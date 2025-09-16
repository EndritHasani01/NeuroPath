// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/testsupport/ApiSteps.java
package com.example.adaptivelearningbackend.bcc.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class ApiSteps {
    private final MockMvc mvc;
    private final ObjectMapper om;

    public ApiSteps(MockMvc mvc, ObjectMapper om) {
        this.mvc = mvc; this.om = om;
    }

    public String login(String u, String p) throws Exception {
        var res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\""+u+"\",\"password\":\""+p+"\"}"))
                .andReturn().getResponse();
        assertThat(res.getStatus()).isBetween(200, 201);
        JsonNode node = om.readTree(res.getContentAsString());
        return node.get("token").asText();
    }

    public long firstDomainId(String token) throws Exception {
        var res = mvc.perform(get("/api/learning/domains").header("Authorization","Bearer "+token))
                .andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode arr = om.readTree(res.getContentAsString());
        assertThat(arr.size()).isGreaterThan(0);
        return arr.get(0).get("id").asLong();
    }

    public Map<Long,String> buildAssessmentAnswers(long domainId, String token) throws Exception {
        var res = mvc.perform(get("/api/learning/domains/{id}/assessment-questions", domainId)
                        .header("Authorization","Bearer "+token))
                .andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(200);
        JsonNode arr = om.readTree(res.getContentAsString());
        Map<Long,String> answers = new LinkedHashMap<>();
        for (JsonNode q: arr) {
            long qid = q.get("id").asLong();
            String first = q.get("options").get(0).asText();
            answers.put(qid, first);
        }
        return answers;
    }

    public JsonNode startDomain(long domainId, Map<Long,String> answers, String token) throws Exception {
        Map<String,Object> body = new HashMap<>();
        body.put("domainId", domainId);
        body.put("answers", answers);
        var res = mvc.perform(post("/api/learning/domains/start")
                        .header("Authorization","Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(body)))
                .andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(200);
        return om.readTree(res.getContentAsString());
    }

    public JsonNode nextInsight(long domainId, String token) throws Exception {
        var res = mvc.perform(get("/api/learning/domains/{id}/next-insight", domainId)
                        .header("Authorization","Bearer " + token))
                .andReturn().getResponse();
        if (res.getStatus() == 204) return null;
        assertThat(res.getStatus()).isEqualTo(200);
        return new ObjectMapper().readTree(res.getContentAsString());
    }

    public JsonNode progress(long domainId, String token) throws Exception {
        var res = mvc.perform(get("/api/learning/domains/{id}/progress", domainId)
                        .header("Authorization","Bearer " + token))
                .andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(200);
        return new ObjectMapper().readTree(res.getContentAsString());
    }

    public JsonNode review(long domainId, String token, int expectedStatus) throws Exception {
        var res = mvc.perform(get("/api/learning/domains/{id}/review", domainId)
                        .header("Authorization","Bearer " + token))
                .andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(expectedStatus);
        return res.getContentAsString().isBlank()? null : new ObjectMapper().readTree(res.getContentAsString());
    }
}
