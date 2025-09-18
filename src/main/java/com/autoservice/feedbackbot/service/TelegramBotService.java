package com.autoservice.feedbackbot.service;

import com.autoservice.feedbackbot.dto.AnalysisResult;
import com.autoservice.feedbackbot.entity.Feedback;
import com.autoservice.feedbackbot.entity.User;
import com.autoservice.feedbackbot.enums.Position;
import com.autoservice.feedbackbot.enums.UserState;
import com.autoservice.feedbackbot.repository.FeedbackRepository;
import com.autoservice.feedbackbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final OpenAiService openAiService;
    private final GoogleDocsService googleDocsService;
    private final TrelloService trelloService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            try {
                processMessage(chatId, messageText);
            } catch (Exception e) {
                log.error("Error processing message: {}", e.getMessage());
                sendMessage(chatId, "Sorry, an error occurred while processing your message.");
            }
        }
    }

    private void processMessage(Long chatId, String messageText) {
        Optional<User> userOptional = userRepository.findById(chatId);

        if (userOptional.isEmpty()) {
            handleNewUser(chatId, messageText);
        } else {
            User user = userOptional.get();
            handleExistingUser(user, messageText);
        }
    }

    private void handleNewUser(Long chatId, String messageText) {
        if (messageText.equals("/start")) {
            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setState(UserState.WAITING_POSITION);
            userRepository.save(newUser);

            sendPositionKeyboard(chatId);
        } else {
            sendMessage(chatId, "Welcome! Please start by typing /start");
        }
    }

    private void handleExistingUser(User user, String messageText) {
        switch (user.getState()) {
            case WAITING_POSITION:
                handlePositionSelection(user, messageText);
                break;
            case WAITING_BRANCH:
                handleBranchSelection(user, messageText);
                break;
            case REGISTERED:
                handleFeedbackMessage(user, messageText);
                break;
            default:
                sendMessage(user.getChatId(), "Please restart with /start command.");
        }
    }

    private void handlePositionSelection(User user, String messageText) {
        try {
            Position position = Position.valueOf(messageText.toUpperCase());
            user.setPosition(position);
            user.setState(UserState.WAITING_BRANCH);
            userRepository.save(user);

            sendMessage(user.getChatId(), "Great! Now please enter your branch name:");
        } catch (IllegalArgumentException e) {
            sendPositionKeyboard(user.getChatId());
        }
    }

    private void handleBranchSelection(User user, String messageText) {
        user.setBranch(messageText);
        user.setState(UserState.REGISTERED);
        userRepository.save(user);

        sendMessage(user.getChatId(),
                String.format("Perfect! You're registered as a %s at %s branch. " +
                                "You can now send your feedback anonymously. " +
                                "Share any complaints, suggestions, or proposals!",
                        user.getPosition().getDisplayName(), user.getBranch()));
    }

    private void handleFeedbackMessage(User user, String messageText) {
        if (messageText.equals("/start")) {
            sendMessage(user.getChatId(), "You're already registered! Send your feedback message.");
            return;
        }

        // Analyze feedback with OpenAI
        AnalysisResult analysis = openAiService.analyzeFeedback(messageText);

        // Save to database
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setMessage(messageText);
        feedback.setSentiment(analysis.getSentiment());
        feedback.setCriticalityLevel(analysis.getCriticality());
        feedback.setSolution(analysis.getSolution());

        feedback = feedbackRepository.save(feedback);

        // Save to Google Docs
        try {
            googleDocsService.addFeedbackToDoc(feedback);
        } catch (Exception e) {
            log.error("Failed to save to Google Docs: {}", e.getMessage());
        }

        // Create Trello card for critical feedback
        if (feedback.getCriticalityLevel() >= 4) {
            try {
                String cardId = trelloService.createCriticalFeedbackCard(feedback);
                feedback.setTrelloCardId(cardId);
                feedbackRepository.save(feedback);
            } catch (Exception e) {
                log.error("Failed to create Trello card: {}", e.getMessage());
            }
        }

        // Send confirmation
        String responseMessage = String.format(
                "Thank you for your feedback! ✅\n\n" +
                        "Analysis:\n" +
                        "• Sentiment: %s\n" +
                        "• Priority Level: %d/5\n" +
                        "• Suggested Solution: %s\n\n" +
                        "Your feedback has been recorded anonymously and will be reviewed by management.",
                analysis.getSentiment().toString().toLowerCase(),
                analysis.getCriticality(),
                analysis.getSolution()
        );

        sendMessage(user.getChatId(), responseMessage);
    }

    private void sendPositionKeyboard(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Please select your position:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        for (Position position : Position.values()) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(position.name()));
            keyboardRows.add(row);
        }

        keyboard.setKeyboard(keyboardRows);
        keyboard.setOneTimeKeyboard(true);
        keyboard.setResizeKeyboard(true);

        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending keyboard: {}", e.getMessage());
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }
}