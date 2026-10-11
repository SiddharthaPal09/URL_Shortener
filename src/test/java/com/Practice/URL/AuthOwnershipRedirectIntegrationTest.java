package com.Practice.URL;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks against the real local database using throwaway test
 * accounts: registration, login, protected APIs, ownership (IDOR), redirects
 * and click tracking, QR generation and dashboard statistics.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthOwnershipRedirectIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final Pattern SHORT_CODE = Pattern.compile("\"shortCode\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    private String register(String name, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extract(TOKEN, result);
    }

    private static String extract(Pattern pattern, MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        Matcher matcher = pattern.matcher(body);
        assertTrue(matcher.find(), "expected pattern in response: " + body);
        return matcher.group(1);
    }

    @Test
    void registrationLoginOwnershipRedirectAndQrWorkTogether() throws Exception {
        long unique = System.nanoTime();
        String emailA = "it-a-" + unique + "@audit.test";
        String emailB = "it-b-" + unique + "@audit.test";
        String tokenA = register("Integration A", emailA, "auditpass1");
        String tokenB = register("Integration B", emailB, "auditpass2");

        // Protected API without token -> 401
        mockMvc.perform(get("/api/urls")).andExpect(status().isUnauthorized());

        // Create a short URL
        MvcResult created = mockMvc.perform(post("/api/urls")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com/it-target\","
                                + "\"customAlias\":\"it-" + unique + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/it-" + unique))
                .andReturn();
        String id = extract(ID, created);
        String shortCode = extract(SHORT_CODE, created);

        // Ownership: user B cannot touch user A's URL
        mockMvc.perform(get("/api/urls/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/urls/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://evil.example.com\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/urls/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Redirect works without authentication and records a click
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://example.com/it-target"));

        mockMvc.perform(get("/api/urls/" + id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(1));

        // Real statistics (not hardcoded)
        MvcResult stats = mockMvc.perform(get("/api/urls/stats")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls").value(1))
                .andExpect(jsonPath("$.totalClicks").value(1))
                .andReturn();
        assertTrue(stats.getResponse().getContentAsString().contains("\"totalUrls\":1"));

        // Analytics and QR for the owned URL
        mockMvc.perform(get("/api/urls/" + id + "/analytics")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1));

        mockMvc.perform(get("/api/urls/" + id + "/qr")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType(MediaType.IMAGE_PNG));

        // Profile updates and never exposes password material
        MvcResult me = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        String meBody = me.getResponse().getContentAsString();
        assertTrue(!meBody.toLowerCase().contains("password"), meBody);

        // Deactivated links stop redirecting
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/urls/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isNotFound());
    }
}
