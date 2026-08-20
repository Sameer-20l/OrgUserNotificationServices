package com.catalogue.verg.notification.controller;

import com.catalogue.verg.core.dto.CustomResponse;
import com.catalogue.verg.core.dto.NotificationRequest;
import com.catalogue.verg.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<CustomResponse> sendNotification(
            @RequestBody NotificationRequest request) {

        CustomResponse response =
                notificationService.sendNotification(request);

        return ResponseEntity
                .status(response.getResponseCode())
                .body(response);
    }

    @GetMapping("/portal")
    public ResponseEntity<CustomResponse> getPortalNotifications(
            @RequestParam String userId,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        CustomResponse response =
                notificationService.getPortalNotifications(
                        userId,
                        filter,
                        page,
                        size
                );

        return ResponseEntity
                .status(response.getResponseCode())
                .body(response);
    }
}
