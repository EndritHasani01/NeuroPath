// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/learning/ReviewAdvanceIT.java
package com.example.adaptivelearningbackend.bcc.learning;

import com.example.adaptivelearningbackend.bcc.testsupport.ApiSteps;
import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewAdvanceIT extends ITBase {

    @Test
    void T5_advance_false_regenerates_same_level() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        // finish level
        while (true) {
            JsonNode insight = steps.nextInsight(domainId, token);
            if (insight == null) break;
            for (JsonNode q : insight.get("questions")) {
                var body = java.util.Map.of("questionId", q.get("id").asLong(), "selectedAnswer", "A");
                mvc.perform(post("/api/learning/insights/submit-answer")
                                .header("Authorization", bearer(token))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsBytes(body)))
                        .andExpect(status().isOk());
            }
        }

        // review available
        steps.review(domainId, token, 200);

        // advance with false → stay on same level after regeneration
        mvc.perform(post("/api/learning/domains/{id}/complete-review", domainId)
                        .param("satisfactoryPerformance","false")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        var ov = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/learning/domains/{id}/overview", domainId)
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse();
        var json = om.readTree(ov.getContentAsString());
        assertThat(json.get("topics").get(0).get("level").asInt()).isEqualTo(1);
    }
}
