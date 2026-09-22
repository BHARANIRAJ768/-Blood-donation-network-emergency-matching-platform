package com.lifelink.service;

import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.RequestStatus;
import com.lifelink.enums.Role;
import com.lifelink.exception.BadRequestException;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationHistoryRepository;
import com.lifelink.repository.NotificationRepository;
import com.lifelink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    BloodRequestRepository requestRepository;
    @Mock
    DonationHistoryRepository historyRepository;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    DonorService donorService;
    @Mock
    NotificationService notificationService;
    @Mock
    EmailService emailService;

    @InjectMocks
    RequestService requestService;

    private User donor;
    private User patient;
    private BloodRequest request;

    @BeforeEach
    void setUp() {
        patient = new User();
        patient.setId(1L);
        patient.setRole(Role.PATIENT);
        patient.setEmail("patient@test.com");

        donor = new User();
        donor.setId(2L);
        donor.setRole(Role.DONOR);
        donor.setBloodGroup(BloodGroup.A_POSITIVE);
        donor.setAvailable(true);
        donor.setEmail("donor@test.com");

        request = new BloodRequest();
        request.setId(10L);
        request.setPatient(patient);
        request.setBloodGroup(BloodGroup.A_POSITIVE);
        request.setStatus(RequestStatus.PENDING);
    }

    @Test
    void acceptPendingRequestSetsDonorAndNotifiesPatient() {
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(donor));
        when(requestRepository.save(any())).thenReturn(request);

        var response = requestService.accept(10L, 2L);

        assertEquals(RequestStatus.ACCEPTED, request.getStatus());
        assertEquals(donor, request.getDonor());
        assertNotNull(request.getAcceptedAt());
        assertNotNull(response);
        verify(notificationRepository).markAllReadForRequestExcept(10L, 2L);
        verify(notificationService).create(eq(1L), anyString(), anyString(), eq(10L));
        verify(emailService).sendRequestAcceptedToPatient(eq("patient@test.com"), eq(donor), eq(request));
    }

    @Test
    void acceptRejectsNonPendingRequest() {
        request.setStatus(RequestStatus.ACCEPTED);
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(BadRequestException.class, () -> requestService.accept(10L, 2L));
    }

    @Test
    void acceptRejectsUnavailableDonor() {
        donor.setAvailable(false);
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(donor));

        assertThrows(BadRequestException.class, () -> requestService.accept(10L, 2L));
    }

    @Test
    void acceptRejectsBloodGroupMismatch() {
        donor.setBloodGroup(BloodGroup.B_POSITIVE);
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(donor));

        assertThrows(BadRequestException.class, () -> requestService.accept(10L, 2L));
    }

    @Test
    void rejectPendingRequestRecordsDeclineAndKeepsPending() {
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(donor));
        when(notificationRepository.findByReceiverIdAndRequestId(2L, 10L)).thenReturn(Collections.emptyList());

        var message = requestService.reject(10L, 2L);

        assertEquals(RequestStatus.PENDING, request.getStatus());
        assertTrue(request.getDeclinedBy().contains("2"));
        assertEquals("You have declined this request", message.message());
        verify(requestRepository).save(request);
    }

    @Test
    void completeWritesHistoryAndUpdatesLastDonation() {
        request.setStatus(RequestStatus.ACCEPTED);
        request.setDonor(donor);
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(historyRepository.existsByBloodRequestId(10L)).thenReturn(false);
        when(requestRepository.save(any())).thenReturn(request);

        var response = requestService.complete(10L, 1L);

        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        assertNotNull(request.getCompletedAt());
        assertEquals(LocalDate.now(), donor.getLastDonationDate());

        ArgumentCaptor<com.lifelink.entity.DonationHistory> historyCaptor =
                ArgumentCaptor.forClass(com.lifelink.entity.DonationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(donor, historyCaptor.getValue().getDonor());
        assertEquals(patient, historyCaptor.getValue().getPatient());
        assertEquals(request, historyCaptor.getValue().getBloodRequest());

        verify(notificationService, times(2)).create(anyLong(), anyString(), anyString(), eq(10L));
        verify(emailService, times(2)).sendDonationCompleted(anyString(), eq(donor), eq(patient), eq(request));
    }

    @Test
    void completeRejectsAlreadyCompletedDonation() {
        request.setStatus(RequestStatus.ACCEPTED);
        request.setDonor(donor);
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(historyRepository.existsByBloodRequestId(10L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> requestService.complete(10L, 1L));
    }
}
