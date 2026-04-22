package by.step.service;

import by.step.model.Role;
import by.step.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService implements UserDetailsService {

    private final List<User> users = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        initTestUsers();
    }

    private void initTestUsers() {
        registerUser("user", "user123", "user@example.com", Set.of(Role.USER));
        registerUser("admin", "admin123", "admin@example.com", Set.of(Role.ADMIN));
        registerUser("artist", "artist123", "artist@example.com", Set.of(Role.ARTIST));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User registerUser(String username, String rawPassword, String email, Set<Role> roles) {
        if (users.stream().anyMatch(u -> u.getUsername().equals(username))) {
            throw new RuntimeException("Username already exists: " + username);
        }
        if (users.stream().anyMatch(u -> u.getEmail().equals(email))) {
            throw new RuntimeException("Email already exists: " + email);
        }

        User user = new User();
        user.setId(idCounter.getAndIncrement());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setRoles(roles);
        user.setEnabled(true);

        users.add(user);
        return user;
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        System.out.println("Password changed for user: " + username);
    }

    public User findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public void deleteUser(String username) {
        users.removeIf(user -> user.getUsername().equals(username));
    }
}