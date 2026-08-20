package com.catalogue.verg.org.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates", uniqueConstraints = {@UniqueConstraint(name = "uk_notification_template_code", columnNames = {"template_code"})})
@Data
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "template_module", nullable = false, length = 100)
    private String templateModule;
    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;
    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;
    @Column(name = "notification_channel", nullable = false, length = 30)
    private String notificationChannel;
    @Column(name = "subject", length = 500)
    private String subject;
    @Column(name = "template_content", nullable = false, length = 10000)
    private String templateContent;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_variables", columnDefinition = "jsonb")
    private JsonNode templateVariables;
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(
            name = "description",
            length = 1000
    )
    private String description;
}