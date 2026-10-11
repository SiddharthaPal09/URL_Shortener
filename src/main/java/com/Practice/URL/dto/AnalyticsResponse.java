package com.Practice.URL.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnalyticsResponse {

    private Long urlId;
    private String originalUrl;
    private String shortUrl;
    private long totalClicks;
    private LocalDateTime createdAt;
    private LocalDateTime expiryDate;
    private Map<String, Long> clicksOverTime = new LinkedHashMap<>();
    private Map<String, Long> browsers = new LinkedHashMap<>();
    private Map<String, Long> devices = new LinkedHashMap<>();
    private Map<String, Long> operatingSystems = new LinkedHashMap<>();
    private Map<String, Long> referrers = new LinkedHashMap<>();

    public Long getUrlId() {
        return urlId;
    }

    public void setUrlId(Long urlId) {
        this.urlId = urlId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Map<String, Long> getClicksOverTime() {
        return clicksOverTime;
    }

    public void setClicksOverTime(Map<String, Long> clicksOverTime) {
        this.clicksOverTime = clicksOverTime;
    }

    public Map<String, Long> getBrowsers() {
        return browsers;
    }

    public void setBrowsers(Map<String, Long> browsers) {
        this.browsers = browsers;
    }

    public Map<String, Long> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, Long> devices) {
        this.devices = devices;
    }

    public Map<String, Long> getOperatingSystems() {
        return operatingSystems;
    }

    public void setOperatingSystems(Map<String, Long> operatingSystems) {
        this.operatingSystems = operatingSystems;
    }

    public Map<String, Long> getReferrers() {
        return referrers;
    }

    public void setReferrers(Map<String, Long> referrers) {
        this.referrers = referrers;
    }
}
