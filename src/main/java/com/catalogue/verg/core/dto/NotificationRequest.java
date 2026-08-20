package com.catalogue.verg.core.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class NotificationRequest {

    private String receiverId;

    private String templateCode;

    private String templateModule;

    private JsonNode templateVariables;

    private String notificationChannel;

    private String emailId;

    private String phoneNumber;
}
