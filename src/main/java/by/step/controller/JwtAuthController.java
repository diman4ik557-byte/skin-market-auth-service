package by.step.controller;

import by.step.config.JwtTokenProvider;
import by.step.dto.AuthRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST контроллер для JWT аутентификации.
 * Предоставляет endpoint для входа с получением JWT токена.
 * Активен только при профиле "jwt".
 *
 * @author Skin Market Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/jwt/auth")
@Profile("jwt")
public class JwtAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword().trim();

        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Username: " + request.getUsername());
        System.out.println("Password: " + request.getPassword());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            System.out.println("Authentication successful!");
        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication.getName());

        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        response.put("type", "Bearer");

        return ResponseEntity.ok(response);
    }
}