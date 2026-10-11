package com.Practice.URL.service;

import com.Practice.URL.dto.AnalyticsResponse;
import com.Practice.URL.entity.Url;
import com.Practice.URL.repository.ClickRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    private final UrlService urlService;
    private final ClickRepository clickRepository;
    private final String baseUrl;

    public AnalyticsService(UrlService urlService,
                            ClickRepository clickRepository,
                            @Value("${app.base-url}") String baseUrl) {
        this.urlService = urlService;
        this.clickRepository = clickRepository;
        this.baseUrl = baseUrl;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getUrlAnalytics(Long urlId, String currentUserEmail) {
        Url url = urlService.getOwnedUrl(urlId, currentUserEmail);

        AnalyticsResponse response = new AnalyticsResponse();
        response.setUrlId(url.getId());
        response.setOriginalUrl(url.getOriginalUrl());
        response.setShortUrl(baseUrl + "/" + url.getShortCode());
        response.setCreatedAt(url.getCreatedAt());
        response.setExpiryDate(url.getExpiryDate());
        response.setTotalClicks(clickRepository.countByUrlId(urlId));
        response.setClicksOverTime(buildClicksOverTime(urlId));
        response.setBrowsers(toMap(clickRepository.countGroupedByBrowser(urlId), "Unknown"));
        response.setDevices(toMap(clickRepository.countGroupedByDevice(urlId), "Unknown"));
        response.setOperatingSystems(toMap(clickRepository.countGroupedByOperatingSystem(urlId), "Unknown"));
        response.setReferrers(toMap(clickRepository.countGroupedByReferrer(urlId), "Direct"));
        return response;
    }

    private Map<String, Long> buildClicksOverTime(Long urlId) {
        List<LocalDateTime> clickTimes = clickRepository.findClickedAtByUrlId(urlId);
        Map<String, Long> clicksPerDay = new TreeMap<>();

        for (LocalDateTime clickTime : clickTimes) {
            String day = clickTime.toLocalDate().toString();
            clicksPerDay.put(day, clicksPerDay.getOrDefault(day, 0L) + 1);
        }

        return clicksPerDay;
    }

    private Map<String, Long> toMap(List<Object[]> rows, String nullLabel) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String label = row[0] == null ? nullLabel : row[0].toString();
            result.put(label, (Long) row[1]);
        }
        return result;
    }
}
