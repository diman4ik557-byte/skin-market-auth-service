package by.step.controller;

import by.step.dto.AuthRequest;
import by.step.dto.RegisterRequest;
import by.step.model.Role;
import by.step.model.User;
import by.step.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. Указываем конкретно контроллер и наш локальный конфиг
@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {AuthController.class, AuthControllerTest.LocalTestConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // 2. Изолированный конфиг БЕЗ аннотации @EnableWebSecurity
    @Configuration
    static class LocalTestConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        when(userService.registerUser(any(), any(), any(), any()))
                .thenReturn(testUser);
        when(userService.findByUsername(any())).thenReturn(testUser);
    }

    @Test
    void registerUser_Success() throws Exception {
        RegisterRequest rr = new RegisterRequest();
        rr.setUsername("newuser");
        rr.setPassword("password123");
        rr.setEmail("new@example.com");
        rr.setRole(Role.USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rr)))
                .andExpect(status().isCreated());
    }
    @Test
    void getAllUsers_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }
}
