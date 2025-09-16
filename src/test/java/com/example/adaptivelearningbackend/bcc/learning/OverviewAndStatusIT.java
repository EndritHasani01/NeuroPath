// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/learning/OverviewAndStatusIT.java
package com.example.adaptivelearningbackend.bcc.learning;

import com.example.adaptivelearningbackend.bcc.testsupport.ApiSteps;
import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OverviewAndStatusIT extends ITBase {

    @Test
    void overview_T2_not_started_404() throws Exception {
        var steps = new ApiSteps(mvc, om);
        String token = steps.login("admin","admin");
        long domainId = steps.firstDomainId(token);

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/learning/domains/{id}/overview", domainId)
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse();
        // service throws "Start the domain first"
        assertThat(res.getStatus()).isEqualTo(404);
    }
}
