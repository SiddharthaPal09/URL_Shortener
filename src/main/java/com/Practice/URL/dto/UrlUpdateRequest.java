package com.Practice.URL.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UrlUpdateRequest {

    @NotBlank(message = "Original URL cannot be blank")
    @Pattern(regexp = "^https?://\\S+$",
            message = "Original URL must be a valid URL starting with http:// or https://")
    private String originalUrl;

    @Size(max = 100, message = "Title cannot be longer than 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot be longer than 500 characters")
    private String description;

    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;

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

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}
