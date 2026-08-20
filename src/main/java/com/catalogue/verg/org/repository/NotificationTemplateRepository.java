package com.catalogue.verg.org.repository;

import com.catalogue.verg.org.entity.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository
        extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate>
    findByTemplateCodeAndIsDeletedFalse(
            String templateCode
    );

    boolean existsByTemplateCodeAndIsDeletedFalse(
            String templateCode
    );

    Optional<NotificationTemplate>
    findByIdAndIsDeletedFalse(Long id);

    @Query("""
            SELECT n
            FROM NotificationTemplate n
            WHERE n.isDeleted = false
            AND (
                LOWER(n.templateCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(n.templateName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(n.templateModule) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(n.notificationChannel) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<NotificationTemplate> searchTemplates(
            @Param("search") String search,
            Pageable pageable
    );

    Page<NotificationTemplate> findByIsDeletedFalse(
            Pageable pageable
    );
}