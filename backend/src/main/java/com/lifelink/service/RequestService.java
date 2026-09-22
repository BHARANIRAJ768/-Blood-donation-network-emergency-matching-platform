package com.lifelink.service;

import com.lifelink.dto.BloodRequestCreateRequest;
import com.lifelink.dto.BloodRequestResponse;
import com.lifelink.dto.MessageResponse;
import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.DonationHistory;
import com.lifelink.entity.Notification;
import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.RequestStatus;
import com.lifelink.enums.Role;
import com.lifelink.exception.BadRequestException;
import com.lifelink.exception.ResourceNotFoundException;
import com.lifelink.entity.RequestEmailToken;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationHistoryRepository;
import com.lifelink.repository.NotificationRepository;
import com.lifelink.repository.RequestEmailTokenRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.util.Haversine;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Manages the full blood request lifecycle:
 * PENDING -> ACCEPTED -> COMPLETED (with REJECTED / CANCELLED branches).
 */
@Service
public class RequestService {

    private final BloodRequestRepository requestRepository;
    private final DonationHistoryRepository historyRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DonorService donorService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final RequestEmailTokenRepository emailTokenRepository;

    public RequestService(BloodRequestRepository requestRepository,
                          DonationHistoryRepository historyRepository,
                          NotificationRepository notificationRepository,
                          UserRepository userRepository,
                          DonorService donorService,
                          NotificationService notificationService,
                          EmailService emailService,
                          SmsService smsService,
                          RequestEmailTokenRepository emailTokenRepository) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.donorService = donorService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.emailTokenRepository = emailTokenRepository;
    }

    // ------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------

    @Transactional
    public BloodRequestResponse create(Long currentUserId, BloodRequestCreateRequest payload) {
        User patient = findUser(currentUserId);
        BloodGroup bloodGroup = BloodGroup.from(payload.bloodGroup());

        BloodRequest request = new BloodRequest();
        request.setPatient(patient);
        request.setBloodGroup(bloodGroup);
        request.setHospitalName(payload.hospitalName().trim());
        request.setHospitalAddress(trimToNull(payload.hospitalAddress()));
        request.setPatientName(payload.patientName().trim());
        request.setPhone(payload.phone().trim());
        request.setUnits(payload.units());
        request.setReason(trimToNull(payload.reason()));
        request.setEmergency(payload.emergency());
        request.setStatus(RequestStatus.PENDING);
        requestRepository.save(request);

        if (payload.donorId() != null) {
            // Targeted flow: must find donor, fail fast if not found or not a donor - ensures mail to receiver
            User donor = userRepository.findById(payload.donorId())
                    .orElseThrow(() -> new BadRequestException("Selected donor not found"));
            if (donor.getRole() != Role.DONOR) {
                throw new BadRequestException("Selected user is not a donor");
            }
            if (donor.getEmail() == null || donor.getEmail().isBlank()) {
                throw new BadRequestException("Selected donor has no registered email");
            }
            Double distance = Haversine.distanceKm(request.getPatient().getLatitude(), request.getPatient().getLongitude(), donor.getLatitude(), donor.getLongitude());
            String title = request.isEmergency() ? "Emergency Blood Request" : "New Blood Request";
            String distanceText = distance == null || distance.isInfinite() ? "your district" : String.format("%.1f km from you", distance);
            String message = request.isEmergency()
                    ? "EMERGENCY! " + request.getPatientName() + " needs " + request.getUnits() + " unit(s) of " + request.getBloodGroup().getValue() + " at " + request.getHospitalName() + ". " + distanceText + "."
                    : request.getPatientName() + " needs " + request.getUnits() + " unit(s) of " + request.getBloodGroup().getValue() + " at " + request.getHospitalName() + ". " + distanceText + ".";
            notificationService.create(donor.getId(), title, message, request.getId());
            // Generate secure tokens for Accept/Decline email links
            String acceptToken = generateEmailToken(request, donor, "ACCEPT");
            String declineToken = generateEmailToken(request, donor, "DECLINE");
            // Send to donor's registered email via Nodemailer/Java (uses donor.getEmail() as receiver mail)
            emailService.sendDonorNotificationMail(donor.getEmail(), donor, request, patient, payload.patientAge(), payload.requiredDate(), distance != null && !distance.isInfinite() ? distance : null, acceptToken, declineToken);
            if (donor.getPhone() != null && !donor.getPhone().isBlank()) {
                String smsText = "VitalDrop: " + request.getPatientName() + " needs " + request.getBloodGroup().getValue() + " blood at " + request.getHospitalName() + " (" + request.getUnits() + " units). Patient: " + request.getPatientName() + " Phone: " + request.getPhone() + ". Please check VitalDrop app.";
                smsService.sendSms(donor.getPhone(), smsText);
            }
        } else {
            alertMatchingDonors(request, patient, payload);
        }
        return BloodRequestResponse.from(request);
    }

    private void alertMatchingDonors(BloodRequest request, User patient, BloodRequestCreateRequest payload) {
        List<DonorService.DonorMatch> matches = donorService.findMatchingDonors(request);
        String title = request.isEmergency() ? "Emergency Blood Request" : "New Blood Request";

        for (DonorService.DonorMatch match : matches) {
            String distanceText = match.distanceKm() == null
                    ? "your district"
                    : String.format("%.1f km from you", match.distanceKm());
            String message = request.isEmergency()
                    ? "EMERGENCY! " + request.getPatientName() + " needs " + request.getUnits()
                    + " unit(s) of " + request.getBloodGroup().getValue() + " at "
                    + request.getHospitalName() + ". " + distanceText + "."
                    : request.getPatientName() + " needs " + request.getUnits()
                    + " unit(s) of " + request.getBloodGroup().getValue() + " at "
                    + request.getHospitalName() + ". " + distanceText + ".";
            notificationService.create(match.donor().getId(), title, message, request.getId());
            // Generate tokens for email links even in broadcast (per donor)
            String acceptToken = generateEmailToken(request, match.donor(), "ACCEPT");
            String declineToken = generateEmailToken(request, match.donor(), "DECLINE");
            emailService.sendDonorNotificationMail(match.donor().getEmail(), match.donor(), request, patient, payload != null ? payload.patientAge() : null, payload != null ? payload.requiredDate() : null, match.distanceKm(), acceptToken, declineToken);
        }
    }

    private void alertMatchingDonors(BloodRequest request) {
        alertMatchingDonors(request, request.getPatient(), null);
    }

    // ------------------------------------------------------------------
    // Donor actions
    // ------------------------------------------------------------------

    @Transactional
    public BloodRequestResponse accept(Long requestId, Long donorId) {
        BloodRequest request = findRequest(requestId);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("This request is no longer accepting donors");
        }

        User donor = findUser(donorId);
        if (donor.getRole() != Role.DONOR) {
            throw new BadRequestException("Only donors can accept blood requests");
        }
        if (!donor.isAvailable()) {
            throw new BadRequestException("You must mark yourself available before accepting a request");
        }
        if (donor.getBloodGroup() != request.getBloodGroup()) {
            throw new BadRequestException("Your blood group does not match this request");
        }

        request.setDonor(donor);
        request.setStatus(RequestStatus.ACCEPTED);
        request.setAcceptedAt(LocalDateTime.now());
        requestRepository.save(request);

        notificationRepository.markAllReadForRequestExcept(request.getId(), donorId);

        int arrival = Haversine.estimatedArrivalMinutes(Haversine.distanceKm(
                request.getPatient().getLatitude(), request.getPatient().getLongitude(),
                donor.getLatitude(), donor.getLongitude()));
        String message = "Your blood request has been accepted.\nDonor: " + donor.getName()
                + "\nPhone: " + donor.getPhone()
                + "\nBlood group: " + donor.getBloodGroup().getValue()
                + "\nEstimated arrival: ~" + arrival + " minutes.";
        notificationService.create(request.getPatient().getId(), "Your request has been accepted", message, request.getId());
        emailService.sendRequestAcceptedToPatient(request.getPatient().getEmail(), donor, request);

        return BloodRequestResponse.from(request);
    }

    @Transactional
    public MessageResponse reject(Long requestId, Long donorId) {
        BloodRequest request = findRequest(requestId);
        User donor = findUser(donorId);

        if (request.getStatus() == RequestStatus.ACCEPTED && request.getDonor() != null
                && request.getDonor().getId().equals(donorId)) {
            request.setStatus(RequestStatus.REJECTED);
            requestRepository.save(request);
            notificationService.create(request.getPatient().getId(), "Donor backed out",
                    "The donor for your request at " + request.getHospitalName()
                            + " had to back out. Your request is marked as rejected.",
                    request.getId());
            return new MessageResponse("You have backed out of this request");
        }

        if (request.getStatus() == RequestStatus.PENDING) {
            request.setDeclinedBy(appendDeclined(request.getDeclinedBy(), donorId));
            requestRepository.save(request);
            notificationRepository.findByReceiverIdAndRequestId(donorId, request.getId())
                    .forEach(n -> {
                        n.setRead(true);
                        notificationRepository.save(n);
                    });
            return new MessageResponse("You have declined this request");
        }

        throw new BadRequestException("This request cannot be declined in its current state");
    }

    // ------------------------------------------------------------------
    // Completion / cancellation
    // ------------------------------------------------------------------

    @Transactional
    public BloodRequestResponse complete(Long requestId, Long currentUserId) {
        BloodRequest request = findRequest(requestId);
        if (request.getStatus() != RequestStatus.ACCEPTED) {
            throw new BadRequestException("Only accepted requests can be completed");
        }
        if (request.getDonor() == null) {
            throw new BadRequestException("No donor is assigned to this request");
        }
        boolean isPatient = request.getPatient().getId().equals(currentUserId);
        boolean isDonor = request.getDonor().getId().equals(currentUserId);
        if (!isPatient && !isDonor) {
            throw new BadRequestException("You are not part of this request");
        }
        if (historyRepository.existsByBloodRequestId(request.getId())) {
            throw new BadRequestException("This donation has already been completed");
        }

        User donor = request.getDonor();
        DonationHistory history = new DonationHistory();
        history.setDonor(donor);
        history.setPatient(request.getPatient());
        history.setBloodRequest(request);
        history.setBloodGroup(request.getBloodGroup());
        history.setHospitalName(request.getHospitalName());
        history.setDonationDate(LocalDate.now());
        historyRepository.save(history);

        donor.setLastDonationDate(LocalDate.now());
        userRepository.save(donor);

        request.setStatus(RequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        requestRepository.save(request);

        notificationService.create(request.getPatient().getId(), "Donation completed",
                "Your blood donation with " + donor.getName() + " has been marked complete. "
                        + "Thank you for using VitalDrop.", request.getId());
        notificationService.create(donor.getId(), "Donation completed",
                "You donated " + request.getUnits() + " unit(s) of " + request.getBloodGroup().getValue()
                        + " to " + request.getPatientName() + " at " + request.getHospitalName()
                        + ". Thank you for saving a life!", request.getId());
        emailService.sendDonationCompleted(request.getPatient().getEmail(), donor, request.getPatient(), request);
        emailService.sendDonationCompleted(donor.getEmail(), donor, request.getPatient(), request);

        return BloodRequestResponse.from(request);
    }

    @Transactional
    public BloodRequestResponse cancel(Long requestId, Long patientId) {
        BloodRequest request = requestRepository.findByIdAndPatientId(requestId, patientId)
                .orElseThrow(() -> new BadRequestException("Request not found for this patient"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only pending requests can be cancelled");
        }
        request.setStatus(RequestStatus.CANCELLED);
        requestRepository.save(request);
        notificationRepository.markAllReadForRequest(request.getId());
        return BloodRequestResponse.from(request);
    }

    // ------------------------------------------------------------------
    // Notification-driven accept / reject
    // ------------------------------------------------------------------

    @Transactional
    public BloodRequestResponse acceptFromNotification(Long notificationId, Long donorId) {
        Notification notification = notificationService.findForUser(notificationId, donorId);
        notification.setRead(true);
        notificationRepository.save(notification);
        if (notification.getRequestId() == null) {
            return null;
        }
        return accept(notification.getRequestId(), donorId);
    }

    @Transactional
    public MessageResponse rejectFromNotification(Long notificationId, Long donorId) {
        Notification notification = notificationService.findForUser(notificationId, donorId);
        notification.setRead(true);
        notificationRepository.save(notification);
        if (notification.getRequestId() == null) {
            return new MessageResponse("Notification cleared");
        }
        return reject(notification.getRequestId(), donorId);
    }

    // ------------------------------------------------------------------
    // Lists
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listForPatient(Long patientId) {
        return requestRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(BloodRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listForDonor(Long donorId) {
        return requestRepository.findByDonorIdOrderByCreatedAtDesc(donorId)
                .stream().map(BloodRequestResponse::from).toList();
    }

    /**
     * PENDING requests matching the donor's blood group, minus ones the donor
     * has already declined. Lets donors browse and accept directly.
     */
    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listAvailableForDonor(Long donorId) {
        User donor = findUser(donorId);
        if (donor.getBloodGroup() == null) {
            return List.of();
        }
        List<Long> declined = parseDeclined(donorId, requestRepository.findByStatusAndBloodGroupOrderByCreatedAtDesc(
                RequestStatus.PENDING, donor.getBloodGroup()));
        return requestRepository.findByStatusAndBloodGroupOrderByCreatedAtDesc(RequestStatus.PENDING, donor.getBloodGroup())
                .stream()
                .filter(r -> !declined.contains(r.getId()))
                .map(BloodRequestResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------
    // Email token handling (Accept/Decline via email link)
    // ------------------------------------------------------------------

    private String generateEmailToken(BloodRequest request, User donor, String action) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        RequestEmailToken emailToken = new RequestEmailToken();
        emailToken.setToken(token);
        emailToken.setRequest(request);
        emailToken.setDonor(donor);
        emailToken.setAction(action);
        emailToken.setUsed(false);
        emailToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        emailTokenRepository.save(emailToken);
        return token;
    }

    @Transactional
    public BloodRequestResponse handleEmailAccept(String token) {
        RequestEmailToken emailToken = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired link"));
        if (emailToken.isUsed()) {
            throw new BadRequestException("This link has already been used");
        }
        if (emailToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This link has expired");
        }
        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);
        return accept(emailToken.getRequest().getId(), emailToken.getDonor().getId());
    }

    @Transactional
    public MessageResponse handleEmailDecline(String token) {
        RequestEmailToken emailToken = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired link"));
        if (emailToken.isUsed()) {
            throw new BadRequestException("This link has already been used");
        }
        if (emailToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This link has expired");
        }
        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);
        return reject(emailToken.getRequest().getId(), emailToken.getDonor().getId());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private BloodRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found"));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String appendDeclined(String existing, Long donorId) {
        StringJoiner joiner = new StringJoiner(",");
        if (existing != null && !existing.isBlank()) {
            joiner.add(existing);
        }
        joiner.add(String.valueOf(donorId));
        return joiner.toString();
    }

    private static List<Long> parseDeclined(Long donorId, List<BloodRequest> requests) {
        List<Long> declinedRequestIds = new ArrayList<>();
        for (BloodRequest request : requests) {
            if (request.getDeclinedBy() != null && request.getDeclinedBy().contains(String.valueOf(donorId))) {
                declinedRequestIds.add(request.getId());
            }
        }
        return declinedRequestIds;
    }
}
