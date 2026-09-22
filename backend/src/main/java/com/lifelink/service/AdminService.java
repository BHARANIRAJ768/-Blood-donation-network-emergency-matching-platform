package com.lifelink.service;

import com.lifelink.dto.AdminStatsResponse;
import com.lifelink.dto.AdminUserResponse;
import com.lifelink.dto.BloodRequestResponse;
import com.lifelink.dto.HistoryResponse;
import com.lifelink.dto.PageResponse;
import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.User;
import com.lifelink.enums.RequestStatus;
import com.lifelink.enums.Role;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationHistoryRepository;
import com.lifelink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BloodRequestRepository requestRepository;
    private final DonationHistoryRepository historyRepository;

    public AdminService(UserRepository userRepository,
                        BloodRequestRepository requestRepository,
                        DonationHistoryRepository historyRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.DONOR),
                userRepository.countByRole(Role.PATIENT),
                userRepository.countByRoleAndAvailableTrue(Role.DONOR),
                requestRepository.count(),
                requestRepository.countByStatus(RequestStatus.PENDING),
                requestRepository.countByStatus(RequestStatus.ACCEPTED),
                requestRepository.countByStatus(RequestStatus.REJECTED),
                requestRepository.countByStatus(RequestStatus.COMPLETED),
                requestRepository.countByStatus(RequestStatus.CANCELLED),
                historyRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(Role role, String search, int page, int size) {
        List<User> users = role == null ? userRepository.findAll() : userRepository.findByRole(role);
        List<AdminUserResponse> rows = users.stream()
                .map(AdminUserResponse::from)
                .filter(user -> matches(user, search))
                .sorted(Comparator.comparing(AdminUserResponse::createdAt).reversed())
                .toList();
        return paginate(new ArrayList<>(rows), page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<BloodRequestResponse> listRequests(RequestStatus status, String search, int page, int size) {
        List<BloodRequestResponse> rows = requestRepository.findAll().stream()
                .sorted(Comparator.comparing(BloodRequest::getCreatedAt).reversed())
                .map(BloodRequestResponse::from)
                .filter(request -> status == null || request.status() == status)
                .filter(request -> matches(request, search))
                .toList();
        return paginate(new ArrayList<>(rows), page, size);
    }

    private boolean matches(AdminUserResponse user, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.trim().toLowerCase(Locale.ROOT);
        return contains(user.name(), term) || contains(user.email(), term)
                || contains(user.phone(), term)
                || contains(user.district(), term)
                || contains(user.bloodGroup() == null ? null : user.bloodGroup().getValue(), term);
    }

    private boolean matches(BloodRequestResponse request, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.trim().toLowerCase(Locale.ROOT);
        return contains(request.patientName(), term) || contains(request.donorName(), term)
                || contains(request.hospitalName(), term)
                || contains(request.bloodGroup() == null ? null : request.bloodGroup().getValue(), term)
                || contains(request.status() == null ? null : request.status().name(), term);
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private <T> PageResponse<T> paginate(List<T> rows, int page, int size) {
        int total = rows.size();
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<T> content = total == 0 ? List.of() : rows.subList(from, to);
        return new PageResponse<>(content, page, size, total, totalPages);
    }
}
