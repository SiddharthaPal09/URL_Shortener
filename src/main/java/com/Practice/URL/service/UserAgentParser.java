package com.Practice.URL.service;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    public String parseBrowser(String userAgent) {
        if (isUnknown(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Edg/") || userAgent.contains("Edge/")) {
            return "Microsoft Edge";
        }
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            return "Opera";
        }
        if (userAgent.contains("Chrome/") || userAgent.contains("CriOS/")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox/") || userAgent.contains("FxiOS/")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari/")) {
            return "Safari";
        }
        return "Other";
    }

    public String parseDevice(String userAgent) {
        if (isUnknown(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("iPad") || userAgent.contains("Tablet")) {
            return "Tablet";
        }
        if (userAgent.contains("Mobile") || userAgent.contains("Android")
                || userAgent.contains("iPhone") || userAgent.contains("iPod")) {
            return "Mobile";
        }
        return "Desktop";
    }

    public String parseOperatingSystem(String userAgent) {
        if (isUnknown(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")
                || userAgent.contains("iPod") || userAgent.contains("CPU OS")) {
            return "iOS";
        }
        if (userAgent.contains("Mac OS X") || userAgent.contains("Macintosh")) {
            return "macOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Other";
    }

    private boolean isUnknown(String userAgent) {
        return userAgent == null || userAgent.isBlank();
    }
}
