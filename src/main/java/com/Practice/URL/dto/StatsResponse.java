package com.Practice.URL.dto;

public class StatsResponse {

    private long totalUrls;
    private long totalClicks;
    private long activeUrls;
    private long expiredUrls;

    public StatsResponse() {
    }

    public StatsResponse(long totalUrls, long totalClicks, long activeUrls, long expiredUrls) {
        this.totalUrls = totalUrls;
        this.totalClicks = totalClicks;
        this.activeUrls = activeUrls;
        this.expiredUrls = expiredUrls;
    }

    public long getTotalUrls() {
        return totalUrls;
    }

    public void setTotalUrls(long totalUrls) {
        this.totalUrls = totalUrls;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public long getActiveUrls() {
        return activeUrls;
    }

    public void setActiveUrls(long activeUrls) {
        this.activeUrls = activeUrls;
    }

    public long getExpiredUrls() {
        return expiredUrls;
    }

    public void setExpiredUrls(long expiredUrls) {
        this.expiredUrls = expiredUrls;
    }
}
