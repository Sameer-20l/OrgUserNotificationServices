package com.catalogue.verg.notification.service;

import com.catalogue.verg.core.dto.CustomResponse;
import com.catalogue.verg.core.dto.NotificationTemplateRequest;
import com.catalogue.verg.notification.entity.NotificationTemplate;
import com.catalogue.verg.notification.repository.NotificationTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    public NotificationTemplateService(
            NotificationTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomResponse createTemplate(
            NotificationTemplateRequest request) {

        CustomResponse response = new CustomResponse();

        boolean exists =
                repository.existsByTemplateCodeAndIsDeletedFalse(
                        request.getTemplateCode()
                );

        if (exists) {
            response.setMessage(
                    "Notification template already exists for template code"
            );
            response.setResponseCode(HttpStatus.CONFLICT);

            return response;
        }

        NotificationTemplate template = new NotificationTemplate();

        template.setTemplateModule(
                request.getTemplateModule()
        );

        template.setTemplateCode(
                request.getTemplateCode()
        );

        template.setTemplateName(
                request.getTemplateName()
        );

        template.setNotificationChannel(
                request.getNotificationChannel()
        );

        template.setDescription(
                request.getDescription()
        );

        template.setSubject(
                request.getSubject()
        );

        template.setTemplateContent(
                request.getTemplateContent()
        );

        template.setTemplateVariables(
                request.getTemplateVariables()
        );

        template.setIsEnabled(true);
        template.setIsDeleted(false);

        LocalDateTime now = LocalDateTime.now();

        template.setCreatedAt(now);
        template.setUpdatedAt(now);

        NotificationTemplate savedTemplate =
                repository.save(template);

        response.setMessage(
                "Notification template created successfully"
        );

        response.setResponseCode(
                HttpStatus.CREATED
        );

        response.getResult().put(
                "notificationTemplate",
                savedTemplate
        );

        return response;
    }

    @Transactional
    public CustomResponse updateTemplate(
            Long id,
            NotificationTemplateRequest request) {

        CustomResponse response = new CustomResponse();

        NotificationTemplate template =
                repository.findByIdAndIsDeletedFalse(id)
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
         * Check whether the requested template code
         * belongs to another active template.
         */
        boolean exists =
                repository.existsByTemplateCodeAndIsDeletedFalse(
                        request.getTemplateCode()
                );

        if (exists
                && !template.getTemplateCode()
                .equals(request.getTemplateCode())) {

            response.setMessage(
                    "Notification template already exists for template code"
            );
            response.setResponseCode(
                    HttpStatus.CONFLICT
            );

            return response;
        }

        template.setTemplateModule(
                request.getTemplateModule()
        );

        template.setTemplateCode(
                request.getTemplateCode()
        );

        template.setTemplateName(
                request.getTemplateName()
        );

        template.setNotificationChannel(
                request.getNotificationChannel()
        );

        template.setSubject(
                request.getSubject()
        );

        template.setTemplateContent(
                request.getTemplateContent()
        );

        template.setTemplateVariables(
                request.getTemplateVariables()
        );

        template.setDescription(
                request.getDescription()
        );

        template.setUpdatedAt(
                LocalDateTime.now()
        );

        NotificationTemplate updatedTemplate =
                repository.save(template);

        response.setMessage(
                "Notification template updated successfully"
        );

        response.setResponseCode(
                HttpStatus.OK
        );

        response.getResult().put(
                "notificationTemplate",
                updatedTemplate
        );

        return response;
    }

    @Transactional
    public CustomResponse deleteTemplate(Long id) {

        CustomResponse response = new CustomResponse();

        NotificationTemplate template =
                repository.findByIdAndIsDeletedFalse(id)
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

        template.setIsDeleted(true);
        template.setIsEnabled(false);
        template.setUpdatedAt(
                LocalDateTime.now()
        );

        repository.save(template);

        response.setMessage(
                "Notification template deleted successfully"
        );

        response.setResponseCode(
                HttpStatus.OK
        );

        return response;
    }

    @Transactional(readOnly = true)
    public CustomResponse getTemplateById(Long id) {

        CustomResponse response = new CustomResponse();

        NotificationTemplate template =
                repository.findByIdAndIsDeletedFalse(id)
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

        response.setMessage(
                "Notification template fetched successfully"
        );

        response.setResponseCode(
                HttpStatus.OK
        );

        response.getResult().put(
                "notificationTemplate",
                template
        );

        return response;
    }

    @Transactional(readOnly = true)
    public CustomResponse getTemplates(
            int page,
            int size,
            String search) {

        CustomResponse response = new CustomResponse();

        if (page < 0) {
            response.setMessage("Page cannot be less than 0");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }

        if (size <= 0) {
            response.setMessage("Page size must be greater than 0");
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Page<NotificationTemplate> templatePage;

        if (search == null || search.trim().isEmpty()) {

            templatePage =
                    repository.findByIsDeletedFalse(
                            pageable
                    );

        } else {

            templatePage =
                    repository.searchTemplates(
                            search.trim(),
                            pageable
                    );
        }

        response.setMessage(
                "Notification templates fetched successfully"
        );

        response.setResponseCode(
                HttpStatus.OK
        );

        response.getResult().put(
                "templates",
                templatePage.getContent()
        );

        response.getResult().put(
                "currentPage",
                templatePage.getNumber()
        );

        response.getResult().put(
                "pageSize",
                templatePage.getSize()
        );

        response.getResult().put(
                "totalElements",
                templatePage.getTotalElements()
        );

        response.getResult().put(
                "totalPages",
                templatePage.getTotalPages()
        );

        response.getResult().put(
                "first",
                templatePage.isFirst()
        );

        response.getResult().put(
                "last",
                templatePage.isLast()
        );

        return response;
    }
}