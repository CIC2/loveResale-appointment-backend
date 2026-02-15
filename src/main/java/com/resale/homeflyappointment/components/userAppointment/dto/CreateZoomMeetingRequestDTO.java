package com.resale.homeflyappointment.components.userAppointment.dto;

import lombok.Data;

@Data
public class CreateZoomMeetingRequestDTO {
    private String topic; //sales or customer appointment
    private String zoomHostId;
    private String start_time;
    private int duration;
}


