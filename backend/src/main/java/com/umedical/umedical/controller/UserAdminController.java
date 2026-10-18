package com.umedical.umedical.controller;

import com.umedical.umedical.dto.CreateUserRequest;
import com.umedical.umedical.dto.CreatedUserResponse;
import com.umedical.umedical.dto.PageResponse;
import com.umedical.umedical.dto.ResetPasswordResponse;
import com.umedical.umedical.dto.SetActiveRequest;
import com.umedical.umedical.dto.UpdateUserRequest;
import com.umedical.umedical.dto.UserSummaryResponse;
import com.umedical.umedical.security.CustomUserDetails;
import com.umedical.umedical.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios (admin)", description = "Gestion de cuentas por el administrador")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @Operation(summary = "Lista usuarios con busqueda, filtros y paginacion",
            description = "search: nombre, usuario o correo. role: ROLE_ADMIN, ROLE_DOCTOR, ROLE_SECRETARY, ROLE_PATIENT. "
                    + "active: true o false. Maximo 50 por pagina.")
    public ResponseEntity<PageResponse<UserSummaryResponse>> search(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        return ResponseEntity.ok(userAdminService.search(search, role, active, page, size));
    }

    @PostMapping
    @Operation(summary = "Crea un usuario (administrador, secretaria o paciente)",
            description = "Si no se envia username ni password se generan; la contrasena temporal se devuelve una sola vez. "
                    + "400 si el rol es medico o invalido, 409 si el correo o el usuario ya existen.")
    public ResponseEntity<CreatedUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita nombre, correo y telefono de un usuario (el rol no cambia)")
    public ResponseEntity<UserSummaryResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userAdminService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activa o desactiva una cuenta",
            description = "Una cuenta desactivada pierde el acceso al instante. 409 si intentas desactivarte a ti mismo, "
                    + "al ultimo administrador activo, o si el usuario es un medico (se gestiona desde /doctors).")
    public ResponseEntity<UserSummaryResponse> setActive(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody SetActiveRequest request) {
        return ResponseEntity.ok(userAdminService.setActive(currentUser.getId(), id, request.getActive()));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Restablece la contrasena: genera una temporal que se devuelve una sola vez")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.resetPassword(id));
    }
}