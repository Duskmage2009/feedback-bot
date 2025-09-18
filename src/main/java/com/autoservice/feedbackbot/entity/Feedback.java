package com.autoservice.feedbackbot.entity;

import com.autoservice.feedbackbot.enums.Sentiment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    private Integer criticalityLevel;

    @Column(columnDefinition = "TEXT")
    private String solution;

    private LocalDateTime createdAt;

    private String trelloCardId;

    private String googleDocEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_chat_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}