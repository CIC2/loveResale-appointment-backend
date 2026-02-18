package com.resale.loveresaleappointment.components.customerAppointment.dto.availableSlots;

import lombok.Data;

import java.util.List;

@Data
public class AppointmentSlotDTO {
    private String startDate;
    private List<String> avlEmps;
    private int addToCalendar;
}

