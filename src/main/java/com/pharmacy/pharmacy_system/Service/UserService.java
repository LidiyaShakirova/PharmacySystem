package com.pharmacy.pharmacy_system.Service;

import com.pharmacy.pharmacy_system.Entity.User;
import com.pharmacy.pharmacy_system.Repository.UserRepository;
import com.pharmacy.pharmacy_system.Util.UserSession;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> authenticate(String username, String rawPassword) {
        log.info("Попытка входа: {}", username);

        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .map(user -> {
                 log.info("Успешный вход: {} ({})", username, user.getRole());
                    return user;
                });
    }


    public List<User> findAll() {
        return userRepository.findAll();
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public User createUser(String username, String rawPassword, String role) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        User saved = userRepository.save(user);
        log.info("Создан новый пользователь: {}", username);
        return saved;
    }

    @Transactional
    public User updateUser(Long id, String newUsername, String newRole, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            if (existsByUsername(newUsername)) {
                throw new IllegalArgumentException("Логин уже занят");
            }
            user.setUsername(newUsername);
        }
        if (newRole != null) user.setRole(newRole);
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User current = UserSession.getInstance().getCurrentUser();
        if (current != null && current.getId().equals(id)) {
            throw new IllegalStateException("Нельзя удалить самого себя");
        }
        userRepository.deleteById(id);
        log.info("Удалён пользователь с id {}", id);
    }

    @Transactional
    public void createDefaultAdmin() {
        if (!existsByUsername("admin")) {
            createUser("admin", "123", "ADMIN");
            log.info("Создан администратор по умолчанию: admin/123");
        }
    }
}