package com.autoservice.feedbackbot.dto;

import com.autoservice.feedbackbot.enums.Sentiment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private Sentiment sentiment;
    private Integer criticality;
    private String solution;
}