package com.umedical.umedical.dto;

import java.time.LocalDate;
import java.util.List;

public class ReportSummaryResponse {

    public static class CountItem {
        private String label;
        private long count;

        public CountItem(String label, long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() { return label; }
        public long getCount() { return count; }
    }

    public static class DoctorStat {
        private long doctorId;
        private String doctorName;
        private long total;
        private long completed;
        private long cancelled;

        public DoctorStat(long doctorId, String doctorName, long total, long completed, long cancelled) {
            this.doctorId = doctorId;
            this.doctorName = doctorName;
            this.total = total;
            this.completed = completed;
            this.cancelled = cancelled;
        }

        public long getDoctorId() { return doctorId; }
        public String getDoctorName() { return doctorName; }
        public long getTotal() { return total; }
        public long getCompleted() { return completed; }
        public long getCancelled() { return cancelled; }
    }

    public static class DayCount {
        private LocalDate date;
        private long count;

        public DayCount(LocalDate date, long count) {
            this.date = date;
            this.count = count;
        }

        public LocalDate getDate() { return date; }
        public long getCount() { return count; }
    }

    private LocalDate startDate;
    private LocalDate endDate;
    private long totalAppointments;
    private long pending;
    private long completed;
    private long cancelled;
    private long noShow;
    private long unattended;
    private long activeDoctors;
    private long activePatients;
    private List<CountItem> byStatus;
    private List<CountItem> bySpecialty;
    private List<DoctorStat> byDoctor;
    private List<DayCount> byDay;

    public ReportSummaryResponse(LocalDate startDate, LocalDate endDate, long totalAppointments, long pending,
                                 long completed, long cancelled, long noShow, long unattended, long activeDoctors,
                                 long activePatients, List<CountItem> byStatus, List<CountItem> bySpecialty,
                                 List<DoctorStat> byDoctor, List<DayCount> byDay) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAppointments = totalAppointments;
        this.pending = pending;
        this.completed = completed;
        this.cancelled = cancelled;
        this.noShow = noShow;
        this.unattended = unattended;
        this.activeDoctors = activeDoctors;
        this.activePatients = activePatients;
        this.byStatus = byStatus;
        this.bySpecialty = bySpecialty;
        this.byDoctor = byDoctor;
        this.byDay = byDay;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getTotalAppointments() { return totalAppointments; }
    public long getPending() { return pending; }
    public long getCompleted() { return completed; }
    public long getCancelled() { return cancelled; }
    public long getNoShow() { return noShow; }
    public long getUnattended() { return unattended; }
    public long getActiveDoctors() { return activeDoctors; }
    public long getActivePatients() { return activePatients; }
    public List<CountItem> getByStatus() { return byStatus; }
    public List<CountItem> getBySpecialty() { return bySpecialty; }
    public List<DoctorStat> getByDoctor() { return byDoctor; }
    public List<DayCount> getByDay() { return byDay; }
}
