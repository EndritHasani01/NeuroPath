// FILE: InsightPathAI-Backend/src/test/java/com/example/adaptivelearningbackend/auth/AuthControllerIT.java
package com.example.adaptivelearningbackend.bcc.auth;

import com.example.adaptivelearningbackend.bcc.testsupport.ITBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends ITBase {

    @Test
    void T0_login_base_ok() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void T1_login_unknown_user() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"admin\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void T2_login_wrong_password() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"bad\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void T3_login_missing_field() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_T0_valid_unique_created() throws Exception {
        var body = """
            {"username":"u_%d","email":"e_%d@example.com","password":"secret12","confirmPassword":"secret12"}
            """.formatted(System.nanoTime(), System.nanoTime());
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void register_T1_duplicate_username() throws Exception {
        var body = """
            {"username":"admin","email":"admin2@example.com","password":"secret12","confirmPassword":"secret12"}
            """;
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_T2_invalid_email_format() throws Exception {
        var body = """
            {"username":"x_%d","email":"not-an-email","password":"secret12","confirmPassword":"secret12"}
            """.formatted(System.nanoTime());
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Email should be valid")));
    }

    @Test
    void register_T3_password_mismatch() throws Exception {
        var body = """
            {"username":"y_%d","email":"y_%d@example.com","password":"secret12","confirmPassword":"zzz"}
            """.formatted(System.nanoTime(), System.nanoTime());
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_T4_missing_fields() throws Exception {
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
