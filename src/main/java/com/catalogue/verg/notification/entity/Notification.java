package com.catalogue.verg.notification.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {@Index(name = "idx_notification_user_id", columnList = "user_id"), @Index(name = "idx_notification_type", columnList = "notification_type"), @Index(name = "idx_notification_created_at", columnList = "created_at"), @Index(name = "idx_notification_user_read_cleared", columnList = "user_id, marked_as_read, cleared")})
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    @Column(name = "email_id", length = 255)
    private String emailId;
    @Column(name = "phone_no", length = 20)
    private String phoneNo;
    @Column(name = "subject", length = 500)
    private String subject;
    @Column(name = "message_body", nullable = false, length = 10000)
    private String messageBody;
    @Column(name = "notification_type", nullable = false, length = 20)
    private String notificationType;
    @Column(name = "template_module")
    private String templateModule;
    @Column(name = "template_id")
    private Long templateId;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "marked_as_read", nullable = false)
    private Boolean markedAsRead = false;
    @Column(name = "cleared", nullable = false)
    private Boolean cleared = false;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "failure_reason", length = 5000)
    private String failureReason;
}