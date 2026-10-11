package com.Practice.URL.service;

import com.Practice.URL.dto.UrlCreateRequest;
import com.Practice.URL.dto.UrlResponse;
import com.Practice.URL.entity.Url;
import com.Practice.URL.entity.User;
import com.Practice.URL.repository.ClickRepository;
import com.Practice.URL.repository.UrlRepository;
import com.Practice.URL.repository.UserRepository;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that public URLs (short links and QR codes) use the configured
 * base URL, so a production deployment never generates localhost links.
 */
class PublicBaseUrlTest {

    private static final String PROD_BASE_URL = "https://links.example.com";

    private final UrlRepository urlRepository = mock(UrlRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ClickRepository clickRepository = mock(ClickRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UrlService urlService() {
        return new UrlService(urlRepository, userRepository, clickRepository, passwordEncoder, PROD_BASE_URL);
    }

    private User savedUser() {
        User user = new User();
        user.setId(7L);
        user.setName("Base URL Tester");
        user.setEmail("baseurl@example.com");
        user.setPassword("x");
        user.setRole("USER");
        return user;
    }

    private Url storedUrl(User user) {
        Url url = new Url();
        url.setId(11L);
        url.setOriginalUrl("https://example.com/target");
        url.setShortCode("baseurlcheck");
        url.setActive(true);
        url.setClickCount(0);
        url.setUser(user);
        return url;
    }

    @Test
    void createdShortUrlsUseConfiguredBaseUrl() {
        when(userRepository.findByEmail("baseurl@example.com")).thenReturn(java.util.Optional.of(savedUser()));
        when(urlRepository.existsByShortCode("myalias")).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UrlCreateRequest request = new UrlCreateRequest();
        request.setOriginalUrl("https://example.com/target");
        request.setCustomAlias("myalias");

        UrlResponse response = urlService().createUrl(request, "baseurl@example.com");

        assertEquals(PROD_BASE_URL + "/myalias", response.getShortUrl());
        assertTrue(!response.getShortUrl().contains("localhost"),
                "short URL must not point at localhost when a base URL is configured");
    }

    @Test
    void qrCodeEncodesTheConfiguredBaseUrl() throws Exception {
        User user = savedUser();
        Url url = storedUrl(user);
        UrlService urlService = urlService();
        QrCodeService qrCodeService = new QrCodeService(urlService, PROD_BASE_URL);

        when(urlRepository.findById(11L)).thenReturn(java.util.Optional.of(url));

        byte[] png = qrCodeService.generateQrCode(11L, "baseurl@example.com");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result decoded = new MultiFormatReader().decode(bitmap);

        assertEquals(PROD_BASE_URL + "/baseurlcheck", decoded.getText());
    }
}
