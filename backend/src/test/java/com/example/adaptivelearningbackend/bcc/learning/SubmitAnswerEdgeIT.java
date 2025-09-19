// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/learning/SubmitAnswerEdgeIT.java
package com.example.adaptivelearningbackend.bcc.learning;

import com.example.adaptivelearningbackend.bcc.testsupport.ApiSteps;
import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubmitAnswerEdgeIT extends ITBase {

    @Test
    void T1_noJWT_401() throws Exception {
        mvc.perform(post("/api/learning/insights/submit-answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":1,\"selectedAnswer\":\"A\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void T2_question_not_found_404() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        mvc.perform(post("/api/learning/insights/submit-answer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":999999,\"selectedAnswer\":\"A\"}"))
                .andExpect(status().isBadRequest()); // wrapped
    }

    @Test
    void T3_questionId_null_400() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        mvc.perform(post("/api/learning/insights/submit-answer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedAnswer\":\"A\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void T6_timeTaken_null_ok() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        steps.startDomain(domainId, steps.buildAssessmentAnswers(domainId, token), token);
        JsonNode i = steps.nextInsight(domainId, token);
        long qid = i.get("questions").get(0).get("id").asLong();

        mvc.perform(post("/api/learning/insights/submit-answer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"questionId\":"+qid+",\"selectedAnswer\":\"A\"}")))
                .andExpect(status().isOk());
    }
}
