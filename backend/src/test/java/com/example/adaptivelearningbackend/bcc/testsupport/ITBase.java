// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/bcc/testsupport/ITBase.java
package com.example.adaptivelearningbackend.bcc.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = ITBase.Initializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ITBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper om;

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("adaptive_learning_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    protected static WireMockServer wm;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.profiles.active", () -> "test");
        r.add("logging.level.org.springframework", () -> "WARN");
        r.add("jwt.secret", () -> "f".repeat(96)); // HS512-strong
        r.add("jwt.expiration-ms", () -> "86400000");
        r.add("frontend.origin", () -> "http://localhost:3000");
        // Use wmPort() so WireMock is started if needed and we get a legit port
        r.add("python.service.baseurl", () -> "http://localhost:" + wmPort() + "/api/ai");
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override public void initialize(ConfigurableApplicationContext ctx) {
            if (wm == null) {
                // Do NOT construct ResponseTemplateTransformer manually for WireMock 3.x
                wm = new WireMockServer(options().dynamicPort());
                wm.start();
            }
            TestPropertyValues.of("python.service.baseurl=http://localhost:" + wm.port() + "/api/ai").applyTo(ctx);
        }
    }

    protected static int wmPort() {
        if (wm == null) {
            wm = new WireMockServer(options().dynamicPort());
            wm.start();
        }
        return wm.port();
    }

    @BeforeAll
    void setupAiHealthy() {
        wm.resetAll();

        // Learning Path
        wm.stubFor(post(urlPathEqualTo("/api/ai/generate-learning-path"))
                .willReturn(okJson("""
                    {
                      "domainName": "{{jsonPath request.body '$.domain_name'}}",
                      "topics": ["Foundations","Data Types","Control Flow","Functions","Collections","OOP","Modules","Testing","I/O","Concurrency"]
                    }""").withTransformers("response-template")));

        // Insights generator → 6 insights, each with 2 MC questions
        String insightsJson = """
[
 {{#each (range 1 6) as |i|}}
   {
     "title": "Insight {{i}}",
     "explanation": "Short explanation {{i}}",
     "aiMetadata": { "level": "{{jsonPath request.body '$.level'}}" },
     "questions": [
       {
         "questionType": "MULTIPLE_CHOICE",
         "questionText": "Pick A {{i}}.1",
         "options": ["A","B","C"],
         "correctAnswer": "A",
         "answerFeedbacks": { "A":"Correct!", "B":"Nope", "C":"Nope" }
       },
       {
         "questionType": "MULTIPLE_CHOICE",
         "questionText": "Pick A {{i}}.2",
         "options": ["A","B","C"],
         "correctAnswer": "A",
         "answerFeedbacks": { "A":"Correct!", "B":"Nope", "C":"Nope" }
       }
     ]
   }{{#unless @last}},{{/unless}}
 {{/each}}
 ]
""";

        wm.stubFor(post(urlPathEqualTo("/api/ai/generate-insights"))
                .willReturn(okJson(insightsJson).withTransformers("response-template")));

        // Review
        wm.stubFor(post(urlPathEqualTo("/api/ai/generate-review"))
                .willReturn(okJson("""
                  {
                    "summary":"Review summary",
                    "strengths":["S1"],
                    "weaknesses":["W1"],
                    "revisionQuestions":[]
                  }
                """)));
    }

    @AfterAll
    void teardown() {
        // keep containers alive for parallel classes (no-op)
    }

    protected String bearer(String token) { return "Bearer " + token; }

    protected String basicAuth(String user, String pass) {
        String raw = user + ":" + pass;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
