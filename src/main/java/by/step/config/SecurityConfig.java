package by.step.config;

import by.step.model.Role;
import by.step.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Конфигурация безопасности для auth-сервиса.
 * Определяет цепочки фильтров для разных профилей:
 * - basic: Basic аутентификация
 * - form: Form-based аутентификация
 * - form-custom: Кастомная страница входа
 * - jwt: JWT аутентификация (stateless)
 *
 * @author Skin Market Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;

    /**
     * Настройка безопасности для профиля "basic" (Basic Authentication).
     *
     * @param http HttpSecurity для настройки
     * @return SecurityFilterChain
     */
    @Bean
    @Profile({"basic", "default"})
    public SecurityFilterChain basicFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        // Публичные эндпоинты
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/public/**").permitAll()  // ← всё, что начинается с /public/
                        .requestMatchers("/hello").authenticated()

                        // API эндпоинты
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers("/api/**").authenticated()

                        // Любые другие запросы
                        .anyRequest().authenticated()
                )
                .userDetailsService(userService)
                .httpBasic(httpBasic -> httpBasic.realmName("Demo App"));
        return http.build();
    }

    /**
     * Настройка безопасности для профиля "form" (стандартная форма входа Spring Security).
     *
     * @param http HttpSecurity для настройки
     * @return SecurityFilterChain
     */
    @Bean
    @Profile("form")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Только публичные эндпоинты
                        .requestMatchers("/api/public/**").permitAll()
                        // Все остальные /api/* требуют аутентификации
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/hello").authenticated()
                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/api/user/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                )
                .formLogin(AbstractAuthenticationFilterConfigurer::permitAll  // Используем стандартную страницу Spring Security
                );
        return http.build();
    }

    /**
     * Настройка безопасности для профиля "form-custom" (кастомная страница входа).
     *
     * @param http HttpSecurity для настройки
     * @return SecurityFilterChain
     */
    @Bean
    @Profile("form-custom")
    public SecurityFilterChain formFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
                        .requestMatchers("/api/user/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers("/login", "/dashboard", "/").permitAll()  // Добавляем разрешение для страниц
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")  // URL для обработки формы
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Настройка безопасности для профиля "jwt" (JWT аутентификация, stateless).
     *
     * @param http HttpSecurity для настройки
     * @return SecurityFilterChain
     */
    @Bean
    @Profile("jwt")
    public SecurityFilterChain jwtFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/jwt/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Создаёт бин JwtAuthenticationFilter для профиля "jwt".
     *
     * @return JwtAuthenticationFilter
     */
    @Bean
    @Profile("jwt")
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * Создаёт бин AuthenticationManager.
     *
     * @param authConfig конфигурация аутентификации
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}