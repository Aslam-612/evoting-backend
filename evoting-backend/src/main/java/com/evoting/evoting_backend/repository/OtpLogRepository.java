package com.evoting.evoting_backend.repository;

import com.evoting.evoting_backend.model.OtpLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpLogRepository extends JpaRepository<OtpLog, Long> {
    Optional<OtpLog> findTopByMobileOrderByCreatedAtDesc(String mobile);
}