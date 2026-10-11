package com.Practice.URL.controller;

import com.Practice.URL.dto.AnalyticsResponse;
import com.Practice.URL.dto.UrlCreateRequest;
import com.Practice.URL.dto.StatsResponse;
import com.Practice.URL.dto.UrlResponse;
import com.Practice.URL.dto.UrlUpdateRequest;
import com.Practice.URL.service.AnalyticsService;
import com.Practice.URL.service.QrCodeService;
import com.Practice.URL.service.UrlService;
import com.Practice.URL.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/urls")
@Tag(name = "URL Management")
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final QrCodeService qrCodeService;
    private final CurrentUser currentUser;

    public UrlController(UrlService urlService,
                        AnalyticsService analyticsService,
                        QrCodeService qrCodeService,
                        CurrentUser currentUser) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
        this.qrCodeService = qrCodeService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Create a short URL")
    public ResponseEntity<UrlResponse> createUrl(@Valid @RequestBody UrlCreateRequest request) {
        UrlResponse response = urlService.createUrl(request, currentUser.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics for the current user")
    public StatsResponse getStats() {
        return urlService.getStats(currentUser.email());
    }

    @GetMapping
    @Operation(summary = "List all short URLs of the current user")
    public List<UrlResponse> getMyUrls() {
        return urlService.getUserUrls(currentUser.email());
    }

    @GetMapping("/search")
    @Operation(summary = "Search own short URLs by keyword")
    public List<UrlResponse> searchUrls(@RequestParam String keyword) {
        return urlService.searchUrls(keyword, currentUser.email());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one short URL by id")
    public UrlResponse getUrl(@PathVariable Long id) {
        return urlService.getUrl(id, currentUser.email());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a short URL")
    public UrlResponse updateUrl(@PathVariable Long id,
                                 @Valid @RequestBody UrlUpdateRequest request) {
        return urlService.updateUrl(id, request, currentUser.email());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a short URL")
    public ResponseEntity<Void> deleteUrl(@PathVariable Long id) {
        urlService.deleteUrl(id, currentUser.email());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a short URL")
    public UrlResponse deactivateUrl(@PathVariable Long id) {
        return urlService.deactivateUrl(id, currentUser.email());
    }

    @GetMapping("/{id}/analytics")
    @Operation(summary = "Click analytics for a short URL")
    public AnalyticsResponse getAnalytics(@PathVariable Long id) {
        return analyticsService.getUrlAnalytics(id, currentUser.email());
    }

    @GetMapping("/{id}/qr")
    @Operation(summary = "QR code image for a short URL")
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        byte[] image = qrCodeService.generateQrCode(id, currentUser.email());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}
