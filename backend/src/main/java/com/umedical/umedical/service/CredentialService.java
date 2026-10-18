package com.umedical.umedical.service;

import com.umedical.umedical.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/** Genera nombres de usuario (a partir del correo) y contrasenas temporales. */
@Service
public class CredentialService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).toLowerCase().replaceAll("[^a-z0-9._-]", ".");
        if (base.length() < 4) {
            base = base + "user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    public String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }
}