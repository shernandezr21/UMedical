package com.umedical.umedical.service;

/** Acciones que se registran en la bitacora. Son texto (no un enum) para poder agregar nuevas sin migrar la base. */
public final class AuditActions {

    private AuditActions() {
    }

    // Acceso y cambios en informacion clinica
    public static final String DOCUMENTS_VIEWED = "DOCUMENTS_VIEWED";
    public static final String DOCUMENT_CREATED = "DOCUMENT_CREATED";
    public static final String DOCUMENT_FILE_UPLOADED = "DOCUMENT_FILE_UPLOADED";
    public static final String DOCUMENT_FILE_DOWNLOADED = "DOCUMENT_FILE_DOWNLOADED";
    public static final String PATIENT_RECORD_VIEWED = "PATIENT_RECORD_VIEWED";
    public static final String APPOINTMENT_NO_SHOW = "APPOINTMENT_NO_SHOW";

    // Administracion de cuentas
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_ACTIVATED = "USER_ACTIVATED";
    public static final String USER_DEACTIVATED = "USER_DEACTIVATED";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String DOCTOR_ACTIVATED = "DOCTOR_ACTIVATED";
    public static final String DOCTOR_DEACTIVATED = "DOCTOR_DEACTIVATED";

    // Salida de datos
    public static final String REPORT_EXPORTED = "REPORT_EXPORTED";
}
