package com.catalogue.verg.notification.service;

import com.catalogue.verg.core.dto.CustomResponse;
import com.catalogue.verg.core.dto.NotificationRequest;
import com.catalogue.verg.notification.entity.Notification;
import com.catalogue.verg.notification.entity.NotificationTemplate;
import com.catalogue.verg.notification.repository.NotificationRepository;
import com.catalogue.verg.notification.repository.NotificationTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationTemplateRepository templateRepository) {

        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional
    public CustomResponse sendNotification(
            NotificationRequest request) {

        CustomResponse response = new CustomResponse();

        /*
         * Validate receiver
         */
        if (request.getReceiverId() == null
                || request.getReceiverId().trim().isEmpty()) {

            response.setMessage("Receiver ID is required");
            response.setResponseCode(HttpStatus.BAD_REQUEST);

            return response;
        }

        /*
         * Validate template code
         */
        if (request.getTemplateCode() == null
                || request.getTemplateCode().trim().isEmpty()) {

            response.setMessage("Template code is required");
            response.setResponseCode(HttpStatus.BAD_REQUEST);

            return response;
        }

        /*
         * Validate template module
         */
        if (request.getTemplateModule() == null
                || request.getTemplateModule().trim().isEmpty()) {

            response.setMessage("Template module is required");
            response.setResponseCode(HttpStatus.BAD_REQUEST);

            return response;
        }

        /*
         * Validate notification channel
         */
        if (request.getNotificationChannel() == null
                || request.getNotificationChannel().trim().isEmpty()) {

            response.setMessage("Notification channel is required");
            response.setResponseCode(HttpStatus.BAD_REQUEST);

            return response;
        }

        /*
         * Currently only PORTAL notifications are supported.
         */
        if (!"PORTAL".equalsIgnoreCase(
                request.getNotificationChannel().trim())) {

            response.setMessage(
                    "Currently only PORTAL notification channel is supported"
            );

            response.setResponseCode(
                    HttpStatus.BAD_REQUEST
            );

            return response;
        }

        /*
         * Find active template using template code.
         */
        NotificationTemplate template =
                templateRepository
                        .findByTemplateCodeAndIsDeletedFalse(
                                request.getTemplateCode().trim()
                        )
                        .orElse(null);

        if (template == null) {

            response.setMessage(
                    "Notification template not found"
            );

            response.setResponseCode(
                    HttpStatus.NOT_FOUND
            );

            return response;
        }

        /*
         * Validate template module.
         */
        if (!template.getTemplateModule()
                .equalsIgnoreCase(
                        request.getTemplateModule().trim()
                )) {

            response.setMessage(
                    "Template module does not match the template"
            );

            response.setResponseCode(
                    HttpStatus.BAD_REQUEST
            );

            return response;
        }

        /*
         * Validate notification channel.
         */
        if (!template.getNotificationChannel()
                .equalsIgnoreCase(
                        request.getNotificationChannel().trim()
                )) {

            response.setMessage(
                    "Notification channel does not match the template"
            );

            response.setResponseCode(
                    HttpStatus.BAD_REQUEST
            );

            return response;
        }

        /*
         * Replace template variables.
         */
        String messageBody =
                replaceTemplateVariables(
                        template.getTemplateContent(),
                        request.getTemplateVariables()
                );

        /*
         * Create notification.
         */
        Notification notification = new Notification();

        notification.setUserId(
                request.getReceiverId().trim()
        );

        notification.setEmailId(
                request.getEmailId()
        );

        notification.setPhoneNo(
                request.getPhoneNumber()
        );

        notification.setSubject(
                template.getSubject()
        );

        notification.setMessageBody(
                messageBody
        );

        notification.setNotificationType(
                "PORTAL"
        );

        notification.setTemplateId(
                template.getId()
        );

        notification.setTemplateModule(
                template.getTemplateModule()
        );

        /*
         * Since this is a PORTAL notification,
         * successfully storing it means it is available
         * to the user.
         */
        notification.setStatus("SENT");

        notification.setMarkedAsRead(false);
        notification.setCleared(false);

        LocalDateTime now = LocalDateTime.now();

        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setSentAt(now);

        Notification savedNotification =
                notificationRepository.save(notification);

        /*
         * Prepare response.
         */
        response.setMessage(
                "Notification sent successfully"
        );

        response.setResponseCode(
                HttpStatus.CREATED
        );

        response.getResult().put(
                "notification",
                savedNotification
        );

        return response;
    }

    private String replaceTemplateVariables(
            String templateContent,
            com.fasterxml.jackson.databind.JsonNode templateVariables) {

        if (templateContent == null
                || templateVariables == null
                || templateVariables.isNull()) {

            return templateContent;
        }

        String message = templateContent;

        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>>
                fields = templateVariables.fields();

        while (fields.hasNext()) {

            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>
                    field = fields.next();

            String variableName = field.getKey();

            String variableValue =
                    field.getValue().isNull()
                            ? ""
                            : field.getValue().asText();

            message = message.replace(
                    "{{" + variableName + "}}",
                    variableValue
            );
        }

        return message;
    }

    @Transactional(readOnly = true)
    public CustomResponse getPortalNotifications(
            String userId,
            String filter,
            int page,
            int size) {

        CustomResponse response = new CustomResponse();

        if (userId == null || userId.trim().isEmpty()) {

            response.setMessage("User ID is required");
            response.setResponseCode(
                    HttpStatus.BAD_REQUEST
            );

            return response;
        }

        if (page < 0) {

            response.setMessage(
                    "Page cannot be less than 0"
            );

            response.setResponseCode(
                    HttpStatus.BAD_REQUEST
            );

            return response;
        }

        if (size <= 0) {

            response.setMessage(
                    "Page size must be greater than 0"
            );

            response.setResponseCode(
                    HttpStatus.BAD_REQUEST
            );

            return response;
        }

        String notificationFilter =
                filter == null || filter.trim().isEmpty()
                        ? "ALL"
                        : filter.trim().toUpperCase();

        if (!notificationFilter.equals("ALL")
                && !notificationFilter.equals("READ")
                && !notificationFilter.equals("UNREAD")) {

            response.setMessage(
                    "Invalid filter. Allowed values are ALL, READ and UNREAD"
            );

            response.setResponseCode(
                    HttpStatus.BAD_REQUEST
            );

            return response;
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<Notification> notificationPage;

        if ("ALL".equals(notificationFilter)) {

            notificationPage =
                    notificationRepository.findPortalNotifications(
                            userId.trim(),
                            pageable
                    );

        } else {

            boolean readStatus =
                    "READ".equals(notificationFilter);

            notificationPage =
                    notificationRepository
                            .findPortalNotificationsByReadStatus(
                                    userId.trim(),
                                    readStatus,
                                    pageable
                            );
        }

        response.setMessage(
                "Portal notifications fetched successfully"
        );

        response.setResponseCode(
                HttpStatus.OK
        );

        response.getResult().put(
                "notifications",
                notificationPage.getContent()
        );

        response.getResult().put(
                "currentPage",
                notificationPage.getNumber()
        );

        response.getResult().put(
                "pageSize",
                notificationPage.getSize()
        );

        response.getResult().put(
                "totalElements",
                notificationPage.getTotalElements()
        );

        response.getResult().put(
                "totalPages",
                notificationPage.getTotalPages()
        );

        response.getResult().put(
                "first",
                notificationPage.isFirst()
        );

        response.getResult().put(
                "last",
                notificationPage.isLast()
        );

        return response;
    }
}
