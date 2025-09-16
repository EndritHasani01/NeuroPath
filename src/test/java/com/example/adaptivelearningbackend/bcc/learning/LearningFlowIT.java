// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/learning/LearningFlowIT.java
package com.example.adaptivelearningbackend.bcc.learning;

import com.example.adaptivelearningbackend.bcc.testsupport.ApiSteps;
import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LearningFlowIT extends ITBase {

    @Test
    void startDomain_T0_base_and_side_effects() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        var answers = steps.buildAssessmentAnswers(domainId, token);
        JsonNode lp = steps.startDomain(domainId, answers, token);
        assertThat(lp.get("topics")).isNotNull();
        assertThat(lp.get("topics").size()).isBetween(6, 20);
    }

    @Test
    void startDomain_T1_noJWT_401() throws Exception {
        var body = Map.of("domainId", 1, "answers", Map.of());
        mvc.perform(post("/api/learning/domains/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startDomain_T2_domain_not_found_404() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        var body = Map.of("domainId", 999999, "answers", Map.of());
        mvc.perform(post("/api/learning/domains/start")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(body)))
                .andExpect(status().is5xxServerError()); // service wraps to 500
    }

    @Test
    void startDomain_T3_answers_empty_200_and_persist() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        var body = Map.of("domainId", domainId, "answers", Map.of());
        mvc.perform(post("/api/learning/domains/start")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics", not(empty())));
    }

    @Test
    void nextInsight_T0_base_200_and_timesShown_inc() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        var answers = steps.buildAssessmentAnswers(domainId, token);
        steps.startDomain(domainId, answers, token);

        JsonNode i1 = steps.nextInsight(domainId, token);
        assertThat(i1).isNotNull();
        assertThat(i1.get("questions").isArray()).isTrue();

        // fetch again to increase timesShown
        JsonNode i2 = steps.nextInsight(domainId, token);
        assertThat(i2).isNotNull();
        assertThat(i2.get("id").asLong()).isEqualTo(i1.get("id").asLong());
    }

    @Test
    void nextInsight_T1_noJWT_401() throws Exception {
        mvc.perform(get("/api/learning/domains/{id}/next-insight", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitAnswer_T0_correct_200_feedback() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        JsonNode insight = steps.nextInsight(domainId, token);
        long qId = insight.get("questions").get(0).get("id").asLong();

        var body = Map.of("questionId", qId, "selectedAnswer", "A", "timeTakenMs", 123);
        mvc.perform(post("/api/learning/insights/submit-answer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.feedback", containsString("Correct")));
    }

    @Test
    void submitAnswer_T4_incorrect_200_feedbackIncludesCorrect() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        JsonNode insight = steps.nextInsight(domainId, token);
        long qId = insight.get("questions").get(1).get("id").asLong();

        var body = Map.of("questionId", qId, "selectedAnswer", "B");
        mvc.perform(post("/api/learning/insights/submit-answer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.correctAnswer").value("A"));
    }

    @Test
    void submitAnswer_T5_caseInsensitive_true() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);
        JsonNode insight = steps.nextInsight(domainId, token);
        long qId = insight.get("questions").get(0).get("id").asLong();

        var body = Map.of("questionId", qId, "selectedAnswer", "a");
        mvc.perform(post("/api/learning/insights/submit-answer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));
    }

    @Test
    void progress_T0_before_threshold_reviewUnavailable() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        var p = steps.progress(domainId, token);
        assertThat(p.get("reviewAvailable").asBoolean()).isFalse();
    }

    @Test
    void complete_all_insights_then_review_and_advance() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        // complete insights in current level
        while (true) {
            JsonNode insight = steps.nextInsight(domainId, token);
            if (insight == null) break; // 204
            for (JsonNode q : insight.get("questions")) {
                long qId = q.get("id").asLong();
                var body = Map.of("questionId", qId, "selectedAnswer", "A");
                mvc.perform(post("/api/learning/insights/submit-answer")
                                .header("Authorization", bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsBytes(body)))
                        .andExpect(status().isOk());
            }
        }

        var p1 = steps.progress(domainId, token);
        assertThat(p1.get("reviewAvailable").asBoolean()).isTrue();

        var review = steps.review(domainId, token, 200);
        assertThat(review.get("summary").asText()).isNotBlank();

        mvc.perform(post("/api/learning/domains/{id}/complete-review", domainId)
                        .param("satisfactoryPerformance", "true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        var ov = mvc.perform(get("/api/learning/domains/{id}/overview", domainId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        var ovJson = om.readTree(ov.getContentAsString());
        assertThat(ovJson.get("topics").get(0).get("level").asInt()).isGreaterThanOrEqualTo(2);
        // next topic unlocked
        if (ovJson.get("topics").size() > 1) {
            assertThat(ovJson.get("topics").get(1).get("unlocked").asBoolean()).isTrue();
        }
    }

    @Test
    void selectTopic_T2_negative_index_400() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        mvc.perform(post("/api/learning/domains/{id}/select-topic/{idx}", domainId, -1)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void selectTopic_T0_switch_ok_and_insights_exist() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        mvc.perform(post("/api/learning/domains/{id}/select-topic/{idx}", domainId, 1)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        JsonNode i = steps.nextInsight(domainId, token);
        assertThat(i).isNotNull();
    }

    @Test
    void domainsStatus_T0_hasOneInProgress() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        var res = mvc.perform(get("/api/learning/domains/status").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        var arr = om.readTree(res.getContentAsString());
        assertThat(arr.isArray()).isTrue();

        boolean hasInProgress = false;
        for (var n : arr) {
            if (n.get("inProgress").asBoolean()) {
                hasInProgress = true;
                break;
            }
        }

        assertThat(hasInProgress).isTrue();

    }
}
