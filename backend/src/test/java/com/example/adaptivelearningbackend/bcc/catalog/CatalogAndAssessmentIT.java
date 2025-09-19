// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/catalog/CatalogAndAssessmentIT.java
package com.example.adaptivelearningbackend.bcc.catalog;

import com.example.adaptivelearningbackend.bcc.testsupport.ApiSteps;
import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogAndAssessmentIT extends ITBase {

    @Test
    void domains_T0_validJWT_ok() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        mvc.perform(get("/api/learning/domains").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void domains_T1_noJWT_401() throws Exception {
        mvc.perform(get("/api/learning/domains"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assessment_T0_existingDomain_ok() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);
        mvc.perform(get("/api/learning/domains/{id}/assessment-questions", domainId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThan(0))));
    }

    @Test
    void assessment_T1_domain_not_found_404() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        mvc.perform(get("/api/learning/domains/{id}/assessment-questions", 999999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assessment_T2_noJWT_401() throws Exception {
        mvc.perform(get("/api/learning/domains/{id}/assessment-questions", 1L))
                .andExpect(status().isUnauthorized());
    }
}
