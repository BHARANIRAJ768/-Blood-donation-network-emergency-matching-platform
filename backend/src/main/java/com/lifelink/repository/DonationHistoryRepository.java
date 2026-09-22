package com.lifelink.repository;

import com.lifelink.entity.DonationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {

    boolean existsByBloodRequestId(Long bloodRequestId);

    long countByDonorId(Long donorId);

    long countByPatientId(Long patientId);

    Optional<DonationHistory> findByBloodRequestId(Long bloodRequestId);

    Page<DonationHistory> findByDonorId(Long donorId, Pageable pageable);

    Page<DonationHistory> findByPatientId(Long patientId, Pageable pageable);
}
