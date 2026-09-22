package com.lifelink.repository;

import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;
import com.lifelink.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    long countByRole(Role role);

    long countByRoleAndAvailableTrue(Role role);

    List<User> findByRoleAndAvailableTrueAndBloodGroupAndIdNot(Role role, BloodGroup bloodGroup, Long excludeId);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.available = true AND u.bloodGroup = :bloodGroup AND u.id <> :excludeId ORDER BY u.district")
    List<User> findAvailableDonors(@Param("role") Role role,
                                   @Param("bloodGroup") BloodGroup bloodGroup,
                                   @Param("excludeId") Long excludeId);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.available = true AND u.id <> :excludeId")
    List<User> findAllAvailableDonors(@Param("role") Role role, @Param("excludeId") Long excludeId);
}
