package com.resale.homeflyappointment.model;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
public class Appointment {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "model_id")
    private Integer modelId;
    @Basic
    @Column(name = "user_id")
    private Integer userId;
    @Basic
    @Column(name = "customer_id")
    private Long customerId;
    @Basic
    @Column(name = "appointment_date")
    private Timestamp appointmentDate;
    @Basic
    @Column(name = "c4c_id")
    private String c4CId;
    @Basic
    @Column(name = "status")
    private String status;
    @Basic
    @Column(name = "type")
    private String type;
    @Basic
    @Column(name = "feedback_message")
    private String feedbackMessage;
    @Basic
    @Column(name = "rating")
    private String rating;

    @Column(columnDefinition = "LONGTEXT")
    private String zoomStartUrl;

    @Column(name = "zoom_meeting_id")
    private String zoomMeetingId;

    @Basic
    @Column(columnDefinition = "LONGTEXT")
    private String zoomUrl;

    @Basic
    @Column(name = "start_time")
    private Timestamp startTime;

    @Basic
    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "rate_1")
    private Integer rate1;
    @Column(name = "rate_2")
    private Integer rate2;
    @Column(name = "rate_3")
    private Integer rate3;
    @Column(name = "rate_4")
    private Integer rate4;
    @Column(name = "comment")
    private String comment;
}


