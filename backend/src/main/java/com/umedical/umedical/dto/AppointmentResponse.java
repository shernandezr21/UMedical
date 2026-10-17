package com.umedical.umedical.dto;

import com.umedical.umedical.entity.enumns.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private String specialty;
    private String patientName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private AppointmentStatus status;
    private String reason;
    private String cancellationReason;

    public AppointmentResponse(Long id, Long doctorId, String doctorName, String specialty, String patientName,
                                LocalDate appointmentDate, LocalTime appointmentTime,
                                AppointmentStatus status, String reason, String cancellationReason) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.patientName = patientName;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.status = status;
        this.reason = reason;
        this.cancellationReason = cancellationReason;
    }

    public Long getId() { return id; }
    public Long getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public String getSpecialty() { return specialty; }
    public String getPatientName() { return patientName; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public String getCancellationReason() { return cancellationReason; }
}

// package com.umedical.umedical.dto;

// import com.umedical.umedical.entity.enumns.AppointmentStatus;

// import java.time.LocalDate;
// import java.time.LocalTime;

// public class AppointmentResponse {

//     private Long id;
//     private String doctorName;
//     private String specialty;
//     private String patientName;
//     private LocalDate appointmentDate;
//     private LocalTime appointmentTime;
//     private AppointmentStatus status;
//     private String reason;

//     public AppointmentResponse(Long id, String doctorName, String specialty, String patientName,
//                                 LocalDate appointmentDate, LocalTime appointmentTime,
//                                 AppointmentStatus status, String reason) {
//         this.id = id;
//         this.doctorName = doctorName;
//         this.specialty = specialty;
//         this.patientName = patientName;
//         this.appointmentDate = appointmentDate;
//         this.appointmentTime = appointmentTime;
//         this.status = status;
//         this.reason = reason;
//     }

//     public Long getId() { return id; }
//     public String getDoctorName() { return doctorName; }
//     public String getSpecialty() { return specialty; }
//     public String getPatientName() { return patientName; }
//     public LocalDate getAppointmentDate() { return appointmentDate; }
//     public LocalTime getAppointmentTime() { return appointmentTime; }
//     public AppointmentStatus getStatus() { return status; }
//     public String getReason() { return reason; }
// }