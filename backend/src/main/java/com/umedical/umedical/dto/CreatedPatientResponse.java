package com.umedical.umedical.dto;

public class CreatedPatientResponse {

    private PatientSummaryResponse patient;
    private String username;
    private String temporaryPassword;

    public CreatedPatientResponse(PatientSummaryResponse patient, String username, String temporaryPassword) {
        this.patient = patient;
        this.username = username;
        this.temporaryPassword = temporaryPassword;
    }

    public PatientSummaryResponse getPatient() { return patient; }
    public String getUsername() { return username; }
    public String getTemporaryPassword() { return temporaryPassword; }
}