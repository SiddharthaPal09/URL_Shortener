package com.Practice.URL.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UrlCreateRequest {

    @NotBlank(message = "Original URL cannot be blank")
    @Pattern(regexp = "^https?://\\S+$",
            message = "Original URL must be a valid URL starting with http:// or https://")
    private String originalUrl;

    @Size(max = 100, message = "Title cannot be longer than 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot be longer than 500 characters")
    private String description;

    @Pattern(regexp = "^$|^[A-Za-z0-9_-]{3,30}$",
            message = "Custom alias must be 3 to 30 characters and may only contain letters, numbers, '-' and '_'")
    private String customAlias;

    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;

    @Pattern(regexp = "^$|^[\\s\\S]{4,100}$",
            message = "Password must be between 4 and 100 characters")
    private String password;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
