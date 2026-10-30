package com.hcl.Inventory.service;

import com.hcl.Inventory.dto.AuthResponseDto;
import com.hcl.Inventory.dto.LoginRequestDto;
import com.hcl.Inventory.dto.RegisterRequestDto;
import com.hcl.Inventory.entity.Role;
import com.hcl.Inventory.entity.User;
import com.hcl.Inventory.repository.UserRepository;
import com.hcl.Inventory.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequestDto dto = new RegisterRequestDto("Admin User", "admin@test.com", "password123", Role.ADMIN);

        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = new User(1L, "Admin User", "admin@test.com", "encodedPassword", Role.ADMIN);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("admin@test.com", "ADMIN")).thenReturn("mockJwtToken");

        AuthResponseDto response = authService.register(dto);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getToken());
        assertEquals(Role.ADMIN, response.getRole());
        assertEquals("Admin User", response.getName());
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequestDto dto = new RegisterRequestDto("Admin User", "admin@test.com", "password123", Role.ADMIN);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(dto));
    }

    @Test
    void testLoginSuccess() {
        LoginRequestDto dto = new LoginRequestDto("admin@test.com", "password123");
        User user = new User(1L, "Admin User", "admin@test.com", "encodedPassword", Role.ADMIN);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("admin@test.com", "ADMIN")).thenReturn("mockJwtToken");

        AuthResponseDto response = authService.login(dto);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getToken());
        assertEquals(Role.ADMIN, response.getRole());
    }

    @Test
    void testLoginInvalidPasswordThrowsException() {
        LoginRequestDto dto = new LoginRequestDto("admin@test.com", "wrongpassword");
        User user = new User(1L, "Admin User", "admin@test.com", "encodedPassword", Role.ADMIN);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(dto));
    }
}
