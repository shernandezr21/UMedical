package com.umedical.umedical.dto;

import com.umedical.umedical.dto.ReportSummaryResponse.CountItem;
import com.umedical.umedical.dto.ReportSummaryResponse.DayCount;

import java.time.LocalDate;
import java.util.List;

public class DoctorStatsResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private long totalAppointments;
    private long pending;
    private long completed;
    private long cancelled;
    private long noShow;
    private long unattended;
    private long uniquePatients;
    private long documentsRegistered;
    private List<DayCount> byDay;
    private List<CountItem> byWeekday;

    public DoctorStatsResponse(LocalDate startDate, LocalDate endDate, long totalAppointments, long pending,
                               long completed, long cancelled, long noShow, long unattended, long uniquePatients,
                               long documentsRegistered, List<DayCount> byDay, List<CountItem> byWeekday) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAppointments = totalAppointments;
        this.pending = pending;
        this.completed = completed;
        this.cancelled = cancelled;
        this.noShow = noShow;
        this.unattended = unattended;
        this.uniquePatients = uniquePatients;
        this.documentsRegistered = documentsRegistered;
        this.byDay = byDay;
        this.byWeekday = byWeekday;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getTotalAppointments() { return totalAppointments; }
    public long getPending() { return pending; }
    public long getCompleted() { return completed; }
    public long getCancelled() { return cancelled; }
    public long getNoShow() { return noShow; }
    public long getUnattended() { return unattended; }
    public long getUniquePatients() { return uniquePatients; }
    public long getDocumentsRegistered() { return documentsRegistered; }
    public List<DayCount> getByDay() { return byDay; }
    public List<CountItem> getByWeekday() { return byWeekday; }
}
