package com.autoservice.feedbackbot.service;

import com.autoservice.feedbackbot.dto.AnalysisResult;
import com.autoservice.feedbackbot.enums.Sentiment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public AnalysisResult analyzeFeedback(String message) {
        try {
            String prompt = createAnalysisPrompt(message);
            String response = callOpenAiApi(prompt);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Error analyzing feedback: {}", e.getMessage());
            return createDefaultAnalysis();
        }
    }

    private String createAnalysisPrompt(String message) {
        return String.format(
                "Analyze this employee feedback from an auto service company. " +
                        "Provide response in JSON format with fields: " +
                        "sentiment (POSITIVE/NEUTRAL/NEGATIVE), " +
                        "criticality (1-5 scale where 1=low, 5=critical), " +
                        "solution (suggested solution in English).\n\n" +
                        "Feedback: \"%s\"\n\n" +
                        "Response (JSON only):", message
        );
    }

    private String callOpenAiApi(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 200,
                "temperature", 0.3
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.get("choices").get(0).get("message").get("content").asText();
        } else {
            throw new RuntimeException("OpenAI API call failed: " + response.getStatusCode());
        }
    }

    private AnalysisResult parseResponse(String response) {
        try {
            // Extract JSON from response if it contains additional text
            String jsonPart = extractJsonFromResponse(response);
            JsonNode json = objectMapper.readTree(jsonPart);

            String sentimentStr = json.get("sentiment").asText();
            int criticality = json.get("criticality").asInt();
            String solution = json.get("solution").asText();

            Sentiment sentiment = Sentiment.valueOf(sentimentStr.toUpperCase());

            return new AnalysisResult(sentiment, criticality, solution);
        } catch (Exception e) {
            log.error("Error parsing OpenAI response: {}", e.getMessage());
            return createDefaultAnalysis();
        }
    }

    private String extractJsonFromResponse(String response) {
        // Find JSON part in the response
        int startIndex = response.indexOf("{");
        int endIndex = response.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        return response;
    }

    private AnalysisResult createDefaultAnalysis() {
        return new AnalysisResult(Sentiment.NEUTRAL, 3, "Review and address the concern raised by employee.");
    }
}