package com.catalogue.verg.core.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class NotificationTemplateRequest {

    private String templateCode;
    private String templateName;
    private String templateModule;
    private String notificationChannel;
    private String subject;
    private String templateContent;
    private JsonNode templateVariables;
    private String description;

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateModule() {
        return templateModule;
    }

    public void setTemplateModule(String templateModule) {
        this.templateModule = templateModule;
    }

    public String getNotificationChannel() {
        return notificationChannel;
    }

    public void setNotificationChannel(String notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public JsonNode getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(JsonNode templateVariables) {
        this.templateVariables = templateVariables;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}