package com.Practice.URL.service;

import com.Practice.URL.entity.Click;
import com.Practice.URL.entity.Url;
import com.Practice.URL.exception.ProtectedUrlAccessException;
import com.Practice.URL.exception.ResourceNotFoundException;
import com.Practice.URL.repository.ClickRepository;
import com.Practice.URL.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RedirectService {

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAgentParser userAgentParser;

    public RedirectService(UrlRepository urlRepository,
                           ClickRepository clickRepository,
                           PasswordEncoder passwordEncoder,
                           UserAgentParser userAgentParser) {
        this.urlRepository = urlRepository;
        this.clickRepository = clickRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAgentParser = userAgentParser;
    }

    @Transactional
    public Url accessUrl(String shortCode, String password, HttpServletRequest request) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));

        if (!url.isActive()) {
            throw new ResourceNotFoundException("This short URL has been disabled");
        }

        if (url.getExpiryDate() != null && url.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResourceNotFoundException("This short URL has expired");
        }

        if (url.isPasswordProtected()) {
            checkPassword(url, password);
        }

        recordClick(url, request);
        url.setClickCount(url.getClickCount() + 1);
        urlRepository.save(url);

        return url;
    }

    private void checkPassword(Url url, String password) {
        if (password == null || password.isBlank()) {
            throw new ProtectedUrlAccessException(
                    "This short URL is password protected, add ?password=your_password to the link");
        }
        if (!passwordEncoder.matches(password, url.getPasswordHash())) {
            throw new ProtectedUrlAccessException("Incorrect password for this short URL");
        }
    }

    private void recordClick(Url url, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        Click click = new Click();
        click.setUrl(url);
        click.setIpAddress(request.getRemoteAddr());
        click.setReferrer(request.getHeader("Referer"));
        click.setBrowser(userAgentParser.parseBrowser(userAgent));
        click.setDevice(userAgentParser.parseDevice(userAgent));
        click.setOperatingSystem(userAgentParser.parseOperatingSystem(userAgent));

        clickRepository.save(click);
    }
}
