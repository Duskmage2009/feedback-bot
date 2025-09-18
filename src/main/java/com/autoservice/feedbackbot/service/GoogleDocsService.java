package com.autoservice.feedbackbot.service;

import com.autoservice.feedbackbot.entity.Feedback;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDocsService {

    private static final String APPLICATION_NAME = "Employee Feedback Bot";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(DocsScopes.DOCUMENTS);

    @Value("${google.docs.document.id}")
    private String documentId;

    @Value("${google.credentials.file.path}")
    private String credentialsFilePath;

    private Docs docsService;

    @PostConstruct
    public void init() throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredential credential = GoogleCredential
                .fromStream(new FileInputStream(credentialsFilePath))
                .createScoped(SCOPES);

        docsService = new Docs.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void addFeedbackToDoc(Feedback feedback) throws IOException {
        String feedbackText = formatFeedbackText(feedback);

        List<Request> requests = new ArrayList<>();

        // Add the feedback text at the end of the document
        requests.add(new Request()
                .setInsertText(new InsertTextRequest()
                        .setText(feedbackText)
                        .setLocation(new Location().setIndex(1)) // Insert at the beginning
                )
        );

        // Apply formatting
        requests.add(new Request()
                .setUpdateTextStyle(new UpdateTextStyleRequest()
                        .setRange(new Range()
                                .setStartIndex(1)
                                .setEndIndex(feedbackText.length() + 1))
                        .setTextStyle(new TextStyle()
                                .setFontSize(new Dimension().setMagnitude(11.0).setUnit("PT")))
                        .setFields("fontSize")
                )
        );

        BatchUpdateDocumentRequest batchUpdateRequest = new BatchUpdateDocumentRequest()
                .setRequests(requests);

        docsService.documents()
                .batchUpdate(documentId, batchUpdateRequest)
                .execute();

        log.info("Added feedback to Google Doc: {}", feedback.getId());
    }

    private String formatFeedbackText(Feedback feedback) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return String.format(
                "=== FEEDBACK ENTRY ===\n" +
                        "Date: %s\n" +
                        "Position: %s\n" +
                        "Branch: %s\n" +
                        "Sentiment: %s\n" +
                        "Criticality: %d/5\n" +
                        "Message: %s\n" +
                        "Suggested Solution: %s\n" +
                        "Trello Card: %s\n\n",
                feedback.getCreatedAt().format(formatter),
                feedback.getUser().getPosition().getDisplayName(),
                feedback.getUser().getBranch(),
                feedback.getSentiment().toString(),
                feedback.getCriticalityLevel(),
                feedback.getMessage(),
                feedback.getSolution(),
                feedback.getTrelloCardId() != null ? "Created" : "N/A"
        );
    }
}