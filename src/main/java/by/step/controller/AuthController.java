package by.step.controller;

import by.step.dto.AuthRequest;
import by.step.dto.RegisterRequest;
import by.step.model.Role;
import by.step.model.User;
import by.step.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST контроллер для аутентификации и управления пользователями.
 * Предоставляет endpoints для регистрации, входа, получения информации о текущем пользователе
 * и административных операций.
 *
 * @author Skin Market Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Регистрирует нового пользователя в системе.
     *
     * @param request DTO с данными регистрации (username, password, email, role)
     * @return ResponseEntity с информацией о зарегистрированном пользователе
     *         или сообщением об ошибке
     */
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            Role role = request.getRole() != null ? request.getRole() : Role.USER;
            Set<Role> roles = Set.of(role);

            User user = userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    roles
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Выполняет вход пользователя в систему.
     * В ответе возвращается информация о пользователе и инструкция по использованию Basic Auth.
     *
     * @param request DTO с username и password
     * @return ResponseEntity с информацией о пользователе или ошибкой авторизации
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            User user = userService.findByUsername(request.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles());
            response.put("note", "Use Basic Auth with username/password for subsequent requests");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Возвращает информацию о текущем авторизованном пользователе.
     *
     * @return ResponseEntity с username, authorities и статусом аутентификации
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities().toString());
        response.put("authenticated", authentication.isAuthenticated());

        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает список всех пользователей (только для администратора).
     *
     * @return список всех пользователей
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Удаляет пользователя по username (только для администратора).
     *
     * @param username имя пользователя для удаления
     * @return ResponseEntity с подтверждением или ошибкой
     */
    @DeleteMapping("/admin/users/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            userService.deleteUser(username);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted: " + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Возвращает профиль текущего пользователя (доступно для USER и ADMIN).
     *
     * @return ResponseEntity с данными профиля пользователя
     */
    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(auth.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("roles", user.getRoles());

        return ResponseEntity.ok(response);
    }
}