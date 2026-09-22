package com.lifelink.dto;

public record AdminStatsResponse(
        long totalUsers,
        long totalDonors,
        long totalPatients,
        long activeDonors,
        long totalRequests,
        long pendingRequests,
        long acceptedRequests,
        long rejectedRequests,
        long completedRequests,
        long cancelledRequests,
        long totalDonations
) {
}
