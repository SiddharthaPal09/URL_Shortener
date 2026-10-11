package com.Practice.URL.controller;

import com.Practice.URL.entity.Url;
import com.Practice.URL.service.RedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Tag(name = "Redirect")
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    // Regex keeps "/" (the landing page) out of this route; real short codes
    // are alphanumeric and custom aliases allow only [A-Za-z0-9_-].
    @GetMapping("/{shortCode:[A-Za-z0-9_-]+}")
    @Operation(summary = "Follow a short URL and record the click")
    public void redirect(@PathVariable String shortCode,
                        @Parameter(description = "Required only for password protected short URLs")
                        @RequestParam(required = false) String password,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        Url url = redirectService.accessUrl(shortCode, password, request);
        response.sendRedirect(url.getOriginalUrl());
    }
}
