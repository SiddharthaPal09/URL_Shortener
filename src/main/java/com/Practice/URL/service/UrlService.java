package com.Practice.URL.service;

import com.Practice.URL.dto.StatsResponse;
import com.Practice.URL.dto.UrlCreateRequest;
import com.Practice.URL.dto.UrlResponse;
import com.Practice.URL.dto.UrlUpdateRequest;
import com.Practice.URL.entity.Url;
import com.Practice.URL.entity.User;
import com.Practice.URL.exception.ConflictException;
import com.Practice.URL.exception.ForbiddenException;
import com.Practice.URL.exception.ResourceNotFoundException;
import com.Practice.URL.repository.ClickRepository;
import com.Practice.URL.repository.UrlRepository;
import com.Practice.URL.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UrlService {

    private static final String SHORT_CODE_CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final ClickRepository clickRepository;
    private final PasswordEncoder passwordEncoder;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();

    public UrlService(UrlRepository urlRepository,
                      UserRepository userRepository,
                      ClickRepository clickRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.clickRepository = clickRepository;
        this.passwordEncoder = passwordEncoder;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public UrlResponse createUrl(UrlCreateRequest request, String currentUserEmail) {
        User user = getCurrentUser(currentUserEmail);
        String shortCode = resolveShortCode(request.getCustomAlias());

        Url url = new Url();
        url.setOriginalUrl(request.getOriginalUrl());
        url.setShortCode(shortCode);
        url.setTitle(request.getTitle());
        url.setDescription(request.getDescription());
        url.setExpiryDate(request.getExpiryDate());
        url.setActive(true);
        url.setClickCount(0);
        url.setUser(user);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            url.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            url.setPasswordProtected(true);
        }

        try {
            urlRepository.saveAndFlush(url);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Short code '" + shortCode + "' is already in use");
        }

        return toResponse(url);
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(String currentUserEmail) {
        User user = getCurrentUser(currentUserEmail);
        List<Url> urls = urlRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

        LocalDateTime now = LocalDateTime.now();
        long totalClicks = 0;
        long activeUrls = 0;
        long expiredUrls = 0;

        for (Url url : urls) {
            totalClicks += url.getClickCount();
            boolean expired = url.getExpiryDate() != null && url.getExpiryDate().isBefore(now);
            if (expired) {
                expiredUrls++;
            } else if (url.isActive()) {
                activeUrls++;
            }
        }

        return new StatsResponse(urls.size(), totalClicks, activeUrls, expiredUrls);
    }

    @Transactional(readOnly = true)
    public List<UrlResponse> getUserUrls(String currentUserEmail) {
        User user = getCurrentUser(currentUserEmail);
        List<Url> urls = urlRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        return toResponseList(urls);
    }

    @Transactional(readOnly = true)
    public List<UrlResponse> searchUrls(String keyword, String currentUserEmail) {
        User user = getCurrentUser(currentUserEmail);
        String pattern = "%" + keyword.toLowerCase() + "%";
        List<Url> urls = urlRepository.searchByUserId(user.getId(), pattern);
        return toResponseList(urls);
    }

    @Transactional(readOnly = true)
    public UrlResponse getUrl(Long id, String currentUserEmail) {
        Url url = getOwnedUrl(id, currentUserEmail);
        return toResponse(url);
    }

    @Transactional
    public UrlResponse updateUrl(Long id, UrlUpdateRequest request, String currentUserEmail) {
        Url url = getOwnedUrl(id, currentUserEmail);
        url.setOriginalUrl(request.getOriginalUrl());
        url.setTitle(request.getTitle());
        url.setDescription(request.getDescription());
        url.setExpiryDate(request.getExpiryDate());
        urlRepository.save(url);
        return toResponse(url);
    }

    @Transactional
    public void deleteUrl(Long id, String currentUserEmail) {
        Url url = getOwnedUrl(id, currentUserEmail);
        clickRepository.deleteAllByUrlId(url.getId());
        urlRepository.delete(url);
    }

    @Transactional
    public UrlResponse deactivateUrl(Long id, String currentUserEmail) {
        Url url = getOwnedUrl(id, currentUserEmail);
        url.setActive(false);
        urlRepository.save(url);
        return toResponse(url);
    }

    @Transactional(readOnly = true)
    public Url getOwnedUrl(Long id, String currentUserEmail) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id " + id));

        if (!url.getUser().getEmail().equals(currentUserEmail)) {
            throw new ForbiddenException("You do not own this URL");
        }

        return url;
    }

    private String resolveShortCode(String customAlias) {
        if (customAlias != null && !customAlias.isBlank()) {
            if (urlRepository.existsByShortCode(customAlias)) {
                throw new ConflictException("Custom alias '" + customAlias + "' is already in use");
            }
            return customAlias;
        }

        String shortCode = generateRandomShortCode();
        while (urlRepository.existsByShortCode(shortCode)) {
            shortCode = generateRandomShortCode();
        }
        return shortCode;
    }

    private String generateRandomShortCode() {
        StringBuilder builder = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = random.nextInt(SHORT_CODE_CHARACTERS.length());
            builder.append(SHORT_CODE_CHARACTERS.charAt(index));
        }
        return builder.toString();
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<UrlResponse> toResponseList(List<Url> urls) {
        List<UrlResponse> responses = new ArrayList<>();
        for (Url url : urls) {
            responses.add(toResponse(url));
        }
        return responses;
    }

    private UrlResponse toResponse(Url url) {
        UrlResponse response = new UrlResponse();
        response.setId(url.getId());
        response.setOriginalUrl(url.getOriginalUrl());
        response.setShortCode(url.getShortCode());
        response.setShortUrl(baseUrl + "/" + url.getShortCode());
        response.setTitle(url.getTitle());
        response.setDescription(url.getDescription());
        response.setPasswordProtected(url.isPasswordProtected());
        response.setExpiryDate(url.getExpiryDate());
        response.setActive(url.isActive());
        response.setClickCount(url.getClickCount());
        response.setCreatedAt(url.getCreatedAt());
        response.setUpdatedAt(url.getUpdatedAt());
        return response;
    }
}
