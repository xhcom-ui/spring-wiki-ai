package com.example.activiti.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "leave_application")
public class Leave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String processInstanceId;
    private String applicant;
    private String deptManager;
    private String generalManager;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer days;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
