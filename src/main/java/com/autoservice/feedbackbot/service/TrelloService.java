package com.autoservice.feedbackbot.service;

import com.autoservice.feedbackbot.entity.Feedback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrelloService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${trello.api.key}")
    private String apiKey;

    @Value("${trello.api.token}")
    private String token;

    @Value("${trello.list.id}")
    private String listId;

    private static final String TRELLO_API_URL = "https://api.trello.com/1";

    public String createCriticalFeedbackCard(Feedback feedback) throws Exception {
        String cardName = String.format("Critical Feedback - %s (%s)",
                feedback.getUser().getPosition().getDisplayName(),
                feedback.getUser().getBranch());

        String cardDescription = formatCardDescription(feedback);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("token", token);
        params.add("idList", listId);
        params.add("name", cardName);
        params.add("desc", cardDescription);
        params.add("pos", "top");

        String labelIds = determineLabelIds(feedback);
        if (labelIds != null) {
            params.add("idLabels", labelIds);
        }

        params.add("due", java.time.LocalDateTime.now().plusDays(3).toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                TRELLO_API_URL + "/cards", request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            String cardId = jsonResponse.get("id").asText();
            String cardUrl = jsonResponse.get("url").asText();

            log.info("Created Trello card for critical feedback: {} - {}", cardId, cardUrl);
            return cardId;
        } else {
            throw new RuntimeException("Failed to create Trello card: " + response.getStatusCode());
        }
    }

    private String formatCardDescription(Feedback feedback) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return String.format(
                "**CRITICAL FEEDBACK ALERT**\n\n" +
                        "**Date:** %s\n" +
                        "**Employee Position:** %s\n" +
                        "**Branch:** %s\n" +
                        "**Sentiment:** %s\n" +
                        "**Criticality Level:** %d/5\n\n" +
                        "**Feedback Message:**\n" +
                        "%s\n\n" +
                        "**AI Suggested Solution:**\n" +
                        "%s\n\n" +
                        "**Action Required:** This feedback requires immediate attention due to high criticality level.\n" +
                        "**Deadline:** Please address within 3 business days.",
                feedback.getCreatedAt().format(formatter),
                feedback.getUser().getPosition().getDisplayName(),
                feedback.getUser().getBranch(),
                feedback.getSentiment().toString(),
                feedback.getCriticalityLevel(),
                feedback.getMessage(),
                feedback.getSolution()
        );
    }

    private String determineLabelIds(Feedback feedback) {
        return null;
    }
}