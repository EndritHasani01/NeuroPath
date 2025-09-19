// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/security/JwtConfigIT.java
package com.example.adaptivelearningbackend.bcc.security;

import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JwtConfigIT {

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = {
            "jwt.secret=" + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +  // 64 f's
                    "ffffffffffffffffffffffffffffffff", // +32 f's = 96 total
            "jwt.expiration-ms=0"
    })
    static class ExpiredImmediately extends ITBase {
        @Test
        void T3_expiration_zero_protected_401() throws Exception {
            // login returns token but immediately invalid for protected calls
            var login = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String token = om.readTree(login).get("token").asText();
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/learning/domains")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = {
            "jwt.secret=shortjwt.expiration-ms=86400000", // concatenated/weak
            "jwt.expiration-ms=86400000"
    })
    static class WeakSecretConcatenatedDevLine extends ITBase {
        @Test
        void T1_T2_weak_secret_breaks_login() throws Exception {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                    .andExpect(status().is5xxServerError());
        }
    }
}
