package by.step.integration;

import by.step.dto.AuthRequest;
import by.step.dto.RegisterRequest;
import by.step.model.Role;
import by.step.model.User;
import by.step.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
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
        // Очищаем пользователей перед каждым тестом
        userService.getAllUsers().forEach(user ->
                userService.deleteUser(user.getUsername())
        );

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("123");
        registerRequest.setEmail("new@example.com");
        registerRequest.setRole(Role.USER);
    }

    @Test
    @DisplayName("Integration: Full registration and login flow")
    void fullRegistrationAndLoginFlow() throws Exception {
        // 1. Регистрация нового пользователя
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("User registered successfully")))
                .andExpect(jsonPath("$.username", is("newuser")));

        // 2. Логин с новым пользователем
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("newuser");
        loginRequest.setPassword("123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("newuser")));

        // 3. Проверка, что пользователь существует в системе
        User user = userService.findByUsername("newuser");
        assert user != null;
        assert user.getEmail().equals("new@example.com");
    }

    @Test
    @DisplayName("Integration: Duplicate registration should fail")
    void duplicateRegistrationFails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Username already exists" )));
    }

    @Test
    @DisplayName("Integration: Admin can list all users")
    void adminCanListAllUsers() throws Exception {
        // Создаем несколько пользователей
        userService.registerUser("user1", "123", "user1@example.com", Set.of(Role.USER));
        userService.registerUser("admin1", "adminpass", "admin@example.com", Set.of(Role.ADMIN));

        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("admin1", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Админ получает список всех пользователей
        mockMvc.perform(get("/api/admin/users")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.httpBasic("admin1", "adminpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4)))) // including existinguser
                .andExpect(jsonPath("$[*].username", hasItems("user1", "user2", "admin1")));
    }
}