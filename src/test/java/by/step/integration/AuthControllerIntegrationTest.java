package by.step.integration;

import by.step.dto.AuthRequest;
import by.step.dto.RegisterRequest;
import by.step.model.Role;
import by.step.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("basic")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        userService.getAllUsers().forEach(user ->
                userService.deleteUser(user.getUsername())
        );

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("user123");
        registerRequest.setEmail("new@example.com");
        registerRequest.setRole(Role.USER);
    }

    @Test
    @DisplayName("Integration: Full registration and login flow")
    void fullRegistrationAndLoginFlow() throws Exception {
        // 1. Регистрация
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.username").value("newuser"));

        // 2. Логин с базовой авторизацией
        mockMvc.perform(get("/api/me")
                        .with(httpBasic("newuser", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));

    }

    @Test
    @DisplayName("Integration: Duplicate registration should fail")
    void duplicateRegistrationFails() throws Exception {
        // Первая регистрация
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Вторая регистрация
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists: newuser"));
    }

    @Test
    @DisplayName("Integration: Admin can list all users")
    void adminCanListAllUsers() throws Exception {
        // Создание пользователя администратора
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setUsername("admin");
        adminRequest.setPassword("admin123");
        adminRequest.setEmail("admin@example.com");
        adminRequest.setRole(Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isCreated());

        // Создание тестового пользователя
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Получение всех пользователей с базовой авторизацией
        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("admin", "admin123"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}