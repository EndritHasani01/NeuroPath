// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/learning/NextInsightEdgeIT.java
package com.example.adaptivelearningbackend.bcc.learning;

import com.example.adaptivelearningbackend.entity.TopicProgress;
import com.example.adaptivelearningbackend.entity.UserDomainProgress;
import com.example.adaptivelearningbackend.repository.TopicProgressRepository;
import com.example.adaptivelearningbackend.repository.UserDomainProgressRepository;
import com.example.adaptivelearningbackend.bcc.testsupport.ApiSteps;
import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NextInsightEdgeIT extends ITBase {

    @Autowired UserDomainProgressRepository udpRepo;
    @Autowired TopicProgressRepository tpRepo;

    @Test
    void T4_insightsNotGenerated_flag_false_regenerates_then_200() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        UserDomainProgress udp = udpRepo.findByUserIdAndDomainId(
                udpRepo.findByUserId(udpRepo.findByUserId(1L).isEmpty()? 1L : udpRepo.findByUserId(1L).get(0).getUser().getId()).get(0).getUser().getId(),
                domainId).orElseThrow();

        String currentTopic = om.readTree(
                udp.getLearningPathJson()).get("topics").get(0).asText();

        TopicProgress tp = tpRepo.findByUserDomainProgressIdAndTopicNameAndLevel(udp.getId(), currentTopic, 1).orElseThrow();
        tp.setInsightsGenerated(false);
        tpRepo.save(tp);

        JsonNode i = steps.nextInsight(domainId, token);
        assertThat(i).isNotNull();
    }

    @Test
    void T5_allInsightsCompleted_returns_204() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);

        // finish them
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

        mvc.perform(get("/api/learning/domains/{id}/next-insight", domainId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }
}
