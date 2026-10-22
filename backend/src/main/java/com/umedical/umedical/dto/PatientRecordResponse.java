package com.umedical.umedical.dto;

import com.umedical.umedical.entity.enumns.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Expediente de un paciente visto por un medico: datos del paciente y sus consultas con ese medico. */
public class PatientRecordResponse {

    public static class PatientInfo {
        private Long patientId;
        private String fullName;
        private String email;
        private String phone;
        private LocalDate birthDate;
        private String bloodType;
        private String address;

        public PatientInfo(Long patientId, String fullName, String email, String phone,
                           LocalDate birthDate, String bloodType, String address) {
            this.patientId = patientId;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.birthDate = birthDate;
            this.bloodType = bloodType;
            this.address = address;
        }

        public Long getPatientId() { return patientId; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public LocalDate getBirthDate() { return birthDate; }
        public String getBloodType() { return bloodType; }
        public String getAddress() { return address; }
    }

    public static class Visit {
        private Long appointmentId;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private AppointmentStatus status;
        private String reason;
        private List<DocumentResponse> documents;

        public Visit(Long appointmentId, LocalDate appointmentDate, LocalTime appointmentTime,
                     AppointmentStatus status, String reason, List<DocumentResponse> documents) {
            this.appointmentId = appointmentId;
            this.appointmentDate = appointmentDate;
            this.appointmentTime = appointmentTime;
            this.status = status;
            this.reason = reason;
            this.documents = documents;
        }

        public Long getAppointmentId() { return appointmentId; }
        public LocalDate getAppointmentDate() { return appointmentDate; }
        public LocalTime getAppointmentTime() { return appointmentTime; }
        public AppointmentStatus getStatus() { return status; }
        public String getReason() { return reason; }
        public List<DocumentResponse> getDocuments() { return documents; }
    }

    private PatientInfo patient;
    private List<Visit> visits;

    public PatientRecordResponse(PatientInfo patient, List<Visit> visits) {
        this.patient = patient;
        this.visits = visits;
    }

    public PatientInfo getPatient() { return patient; }
    public List<Visit> getVisits() { return visits; }
}