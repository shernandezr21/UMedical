package com.umedical.umedical.controller;

import com.umedical.umedical.dto.ChangePasswordRequest;
import com.umedical.umedical.security.CustomUserDetails;
import com.umedical.umedical.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Usuario actual", description = "Operaciones sobre la cuenta del usuario autenticado (cualquier rol)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/password")
    @Operation(summary = "Cambia mi contrasena",
            description = "204 si se cambia. 400 si la contrasena actual es incorrecta o la nueva no cumple. "
                    + "El token actual sigue siendo valido.")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }
}