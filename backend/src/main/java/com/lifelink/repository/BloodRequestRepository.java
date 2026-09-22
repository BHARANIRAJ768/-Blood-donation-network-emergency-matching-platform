package com.lifelink.repository;

import com.lifelink.entity.BloodRequest;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<BloodRequest> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    List<BloodRequest> findByStatusAndBloodGroupOrderByCreatedAtDesc(RequestStatus status, BloodGroup bloodGroup);

    Optional<BloodRequest> findByIdAndPatientId(Long id, Long patientId);

    Optional<BloodRequest> findByIdAndDonorId(Long id, Long donorId);

    long countByStatus(RequestStatus status);

    Page<BloodRequest> findByPatientId(Long patientId, Pageable pageable);

    Page<BloodRequest> findByDonorId(Long donorId, Pageable pageable);
}
