package com.lifelink.repository;

import com.lifelink.entity.RequestEmailToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestEmailTokenRepository extends JpaRepository<RequestEmailToken, Long> {
    Optional<RequestEmailToken> findByToken(String token);
    Optional<RequestEmailToken> findByTokenAndAction(String token, String action);
}
