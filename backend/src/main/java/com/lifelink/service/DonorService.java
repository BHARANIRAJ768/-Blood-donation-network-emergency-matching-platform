package com.lifelink.service;

import com.lifelink.dto.DonorCardResponse;
import com.lifelink.dto.PageResponse;
import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.Role;
import com.lifelink.repository.UserRepository;
import com.lifelink.util.Haversine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Donor discovery. Donors are ranked by straight-line
 * distance (Haversine). When coordinates are unavailable, matching falls back
 * to district equality.
 */
@Service
public class DonorService {

    private final UserRepository userRepository;
    private final double maxDistanceKm;

    public DonorService(UserRepository userRepository,
                        @Value("${app.max-distance-km:15}") double maxDistanceKm) {
        this.userRepository = userRepository;
        this.maxDistanceKm = maxDistanceKm;
    }

    public record DonorMatch(User donor, Double distanceKm) {
    }

    @Transactional(readOnly = true)
    public PageResponse<DonorCardResponse> search(Long currentUserId, BloodGroup bloodGroup,
                                                  Double latitude, Double longitude,
                                                  int page, int size) {
        List<User> candidates = bloodGroup == null
                ? userRepository.findAllAvailableDonors(Role.DONOR, currentUserId)
                : userRepository.findAvailableDonors(Role.DONOR, bloodGroup, currentUserId);

        User requester = userRepository.findById(currentUserId).orElseThrow();
        Double originLat = latitude != null ? latitude : requester.getLatitude();
        Double originLng = longitude != null ? longitude : requester.getLongitude();

        List<DonorCardResponse> ranked = new ArrayList<>();
        for (User donor : candidates) {
            Double distance = Haversine.distanceKm(originLat, originLng, donor.getLatitude(), donor.getLongitude());
            ranked.add(DonorCardResponse.from(donor, distance.isInfinite() ? null : round(distance)));
        }

        ranked.sort(Comparator
                .comparing(DonorCardResponse::distanceKm,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DonorCardResponse::district, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DonorCardResponse::name));

        int total = ranked.size();
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<DonorCardResponse> content = total == 0 ? List.of() : ranked.subList(from, to);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

     /**
      * Donors that should be alerted about a new request: available, matching
      * blood group, within the configured radius (fallback to same
      * district when coordinates are missing).
      */
    @Transactional(readOnly = true)
    public List<DonorMatch> findMatchingDonors(BloodRequest request) {
        User patient = request.getPatient();
        List<User> candidates = userRepository.findAvailableDonors(Role.DONOR, request.getBloodGroup(), patient.getId());
        List<Long> declined = parseDeclinedBy(request.getDeclinedBy());

        List<DonorMatch> matches = new ArrayList<>();
        for (User donor : candidates) {
            if (declined.contains(donor.getId())) {
                continue;
            }
            Double distance = Haversine.distanceKm(patient.getLatitude(), patient.getLongitude(),
                    donor.getLatitude(), donor.getLongitude());
            boolean withinRadius = !distance.isInfinite() && distance <= maxDistanceKm;
            boolean sameAreaFallback = distance.isInfinite()
                    && sameDistrictIgnoreCase(patient.getDistrict(), donor.getDistrict());
            if (withinRadius || sameAreaFallback) {
                matches.add(new DonorMatch(donor, distance.isInfinite() ? null : round(distance)));
            }
        }
        matches.sort(Comparator.comparing(m -> m.distanceKm() == null ? Double.MAX_VALUE : m.distanceKm()));
        return matches;
    }

    private static boolean sameDistrictIgnoreCase(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static List<Long> parseDeclinedBy(String declinedBy) {
        List<Long> ids = new ArrayList<>();
        if (declinedBy == null || declinedBy.isBlank()) {
            return ids;
        }
        for (String part : declinedBy.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip malformed entries
            }
        }
        return ids;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
