package com.revplay.musicplatform.audit.repository;

import com.revplay.musicplatform.audit.entity.AdminAuditLog;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;


@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {


    @Query("SELECT a FROM AdminAuditLog a WHERE " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:performedBy IS NULL OR a.performedBy = :performedBy) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
            "(:toDate IS NULL OR a.timestamp <= :toDate) " +
            "ORDER BY a.timestamp DESC")
    Page<AdminAuditLog> findWithFilters(
            @Param("action") AuditActionType action,
            @Param("performedBy") Long performedBy,
            @Param("entityType") AuditEntityType entityType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
