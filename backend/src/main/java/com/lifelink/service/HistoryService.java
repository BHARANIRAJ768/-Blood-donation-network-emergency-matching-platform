package com.lifelink.service;

import com.lifelink.dto.HistoryResponse;
import com.lifelink.dto.PageResponse;
import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.DonationHistory;
import com.lifelink.entity.User;
import com.lifelink.enums.RequestStatus;
import com.lifelink.enums.Role;
import com.lifelink.exception.ResourceNotFoundException;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationHistoryRepository;
import com.lifelink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Permanently stores completed donations and surfaces each user's own history
 * (as patient or donor) with status filtering, text search and pagination.
 */
@Service
public class HistoryService {

    private final BloodRequestRepository requestRepository;
    private final DonationHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public HistoryService(BloodRequestRepository requestRepository,
                          DonationHistoryRepository historyRepository,
                          UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryResponse> getMyHistory(Long userId, RequestStatus status, String search,
                                                      int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<Long, LocalDate> donationDates = donationDates();

        List<HistoryResponse> rows = new ArrayList<>();
        for (BloodRequest request : requestRepository.findByPatientIdOrderByCreatedAtDesc(userId)) {
            rows.add(HistoryResponse.from(request, donationDates.get(request.getId()), Role.PATIENT));
        }
        for (BloodRequest request : requestRepository.findByDonorIdOrderByCreatedAtDesc(userId)) {
            rows.add(HistoryResponse.from(request, donationDates.get(request.getId()), Role.DONOR));
        }

        return paginate(filter(rows, status, search), page, size);
    }

    /**
     * Full history used by the admin dashboard.
     */
    @Transactional(readOnly = true)
    public PageResponse<HistoryResponse> getHistory(RequestStatus status, String search, int page, int size) {
        Map<Long, LocalDate> donationDates = donationDates();
        List<HistoryResponse> rows = requestRepository.findAll().stream()
                .map(request -> HistoryResponse.from(request, donationDates.get(request.getId()), null))
                .sorted(Comparator.comparing(HistoryResponse::createdAt).reversed())
                .toList();

        return paginate(filter(rows, status, search), page, size);
    }

    private Map<Long, LocalDate> donationDates() {
        Map<Long, LocalDate> dates = new HashMap<>();
        for (DonationHistory history : historyRepository.findAll()) {
            dates.put(history.getBloodRequest().getId(), history.getDonationDate());
        }
        return dates;
    }

    private List<HistoryResponse> filter(List<HistoryResponse> rows, RequestStatus status, String search) {
        List<HistoryResponse> result = new ArrayList<>();
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        for (HistoryResponse row : rows) {
            if (status != null && row.status() != status) {
                continue;
            }
            if (!term.isEmpty() && !matches(row, term)) {
                continue;
            }
            result.add(row);
        }
        return result;
    }

    private boolean matches(HistoryResponse row, String term) {
        return contains(row.patientName(), term)
                || contains(row.donorName(), term)
                || contains(row.hospitalName(), term)
                || contains(row.bloodGroup() == null ? null : row.bloodGroup().getValue(), term)
                || contains(row.status() == null ? null : row.status().name(), term);
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private PageResponse<HistoryResponse> paginate(List<HistoryResponse> rows, int page, int size) {
        int total = rows.size();
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<HistoryResponse> content = total == 0 ? List.of() : rows.subList(from, to);
        return new PageResponse<>(content, page, size, total, totalPages);
    }
}
