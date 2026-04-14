package by.step.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimpleController.class)
class SimpleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoint should be accessible without auth")
    void publicHello_Accessible() throws Exception {
        mockMvc.perform(get("/public/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello public!"));
    }

    @Test
    @DisplayName("Hello endpoint requires ADMIN role")
    @WithMockUser(roles = {"USER"})
    void hello_UserForbidden() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Hello endpoint accessible for ADMIN")
    @WithMockUser(roles = {"ADMIN"})
    void hello_AdminSuccess() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello world"));
    }
}