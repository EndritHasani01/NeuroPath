// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/security/CorsIT.java
package com.example.adaptivelearningbackend.bcc.security;

import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

class CorsIT {

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = {
            "frontend.origin=http://localhost:3000"
    })
    static class AllowedOrigin extends ITBase {
        @Test
        void T0_preflight_allowed_origin_ok() throws Exception {
            var res = mvc.perform(options("/api/learning/domains")
                            .header("Origin","http://localhost:3000")
                            .header("Access-Control-Request-Method","GET"))
                    .andReturn().getResponse();
            assertThat(res.getStatus()).isBetween(200, 204);
            assertThat(res.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = {
            "frontend.origin=https://prod.example.com"
    })
    static class DisallowedOrigin extends ITBase {
        @Test
        void T1_preflight_disallowed_origin_no_headers() throws Exception {
            var res = mvc.perform(options("/api/learning/domains")
                            .header("Origin","https://evil.dev")
                            .header("Access-Control-Request-Method","GET"))
                    .andReturn().getResponse();
            assertThat(res.getHeader("Access-Control-Allow-Origin")).isNull();
        }
    }
}
