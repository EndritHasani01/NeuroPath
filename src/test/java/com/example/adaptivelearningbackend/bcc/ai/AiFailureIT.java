// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/bcc/ai/AiFailureIT.java
package com.example.adaptivelearningbackend.bcc.ai;

import com.example.adaptivelearningbackend.bcc.testsupport.ApiSteps;
import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class AiFailureIT extends ITBase {

    @BeforeEach
    void forceAiDown() {
        // fallback cases
        wm.stubFor(post(urlPathEqualTo("/api/ai/generate-learning-path"))
                .willReturn(serverError()));
        wm.stubFor(post(urlPathEqualTo("/api/ai/generate-insights"))
                .willReturn(serverError()));
        wm.stubFor(post(urlPathEqualTo("/api/ai/generate-review"))
                .willReturn(serverError()));
    }

    @Test
    void startDomain_T5_python_down_returns_fallback_path() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);

        var body = steps.buildAssessmentAnswers(domainId, token);
        JsonNode lp = steps.startDomain(domainId, body, token);
        assertThat(lp.get("topics").size()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void review_T3_python_down_returns_dummy_review() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);

        // start to ensure progress exists
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        // fast-forward: ask review before threshold to hit 403 path unaffected by AI down
        var res403 = steps.review(domainId, token, 403);
        // now mark completion by answering quickly all insights to reach threshold
        while (true) {
            JsonNode insight = steps.nextInsight(domainId, token);
            if (insight == null) break;
            for (JsonNode q : insight.get("questions")) {
                var body = java.util.Map.of("questionId", q.get("id").asLong(), "selectedAnswer", "A");
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/learning/insights/submit-answer")
                                .header("Authorization", bearer(token))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsBytes(body)))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            }
        }
        var review = steps.review(domainId, token, 200);
        assertThat(review.get("summary").asText()).contains("could not be generated");
    }
}
