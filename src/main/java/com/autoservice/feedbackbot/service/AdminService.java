package com.autoservice.feedbackbot.service;

import com.autoservice.feedbackbot.entity.Feedback;
import com.autoservice.feedbackbot.enums.Position;
import com.autoservice.feedbackbot.enums.Sentiment;
import com.autoservice.feedbackbot.repository.FeedbackRepository;
import com.autoservice.feedbackbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public Page<Feedback> getFeedbacks(PageRequest pageRequest, String branch,
                                       Position position, Integer minCriticality,
                                       Sentiment sentiment) {

        Specification<Feedback> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (branch != null && !branch.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("branch"), branch));
            }

            if (position != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("position"), position));
            }

            if (minCriticality != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("criticalityLevel"), minCriticality));
            }

            if (sentiment != null) {
                predicates.add(criteriaBuilder.equal(root.get("sentiment"), sentiment));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return feedbackRepository.findAll(spec, pageRequest);
    }

    public Map<String, Object> getStatistics(String branch, Position position,
                                             LocalDateTime start, LocalDateTime end) {

        List<Feedback> feedbacks = getFeedbacksForStats(branch, position, start, end);

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalFeedbacks", feedbacks.size());

        Map<String, Long> sentimentStats = feedbacks.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getSentiment().toString(),
                        Collectors.counting()));
        stats.put("sentimentDistribution", sentimentStats);

        Map<Integer, Long> criticalityStats = feedbacks.stream()
                .collect(Collectors.groupingBy(
                        Feedback::getCriticalityLevel,
                        Collectors.counting()));
        stats.put("criticalityDistribution", criticalityStats);

        if (branch == null) {
            Map<String, Long> branchStats = feedbacks.stream()
                    .collect(Collectors.groupingBy(
                            f -> f.getUser().getBranch(),
                            Collectors.counting()));
            stats.put("branchDistribution", branchStats);
        }

        if (position == null) {
            Map<String, Long> positionStats = feedbacks.stream()
                    .collect(Collectors.groupingBy(
                            f -> f.getUser().getPosition().toString(),
                            Collectors.counting()));
            stats.put("positionDistribution", positionStats);
        }

        long criticalCount = feedbacks.stream()
                .mapToInt(Feedback::getCriticalityLevel)
                .filter(level -> level >= 4)
                .count();
        stats.put("criticalFeedbacksCount", criticalCount);

        double avgCriticality = feedbacks.stream()
                .mapToInt(Feedback::getCriticalityLevel)
                .average()
                .orElse(0.0);
        stats.put("averageCriticality", Math.round(avgCriticality * 100.0) / 100.0);

        return stats;
    }

    public Page<Feedback> getCriticalFeedbacks(PageRequest pageRequest) {
        List<Feedback> criticalFeedbacks = feedbackRepository.findByCriticalityLevelGreaterThanEqual(4);

        int start = Math.min((int) pageRequest.getOffset(), criticalFeedbacks.size());
        int end = Math.min(start + pageRequest.getPageSize(), criticalFeedbacks.size());

        List<Feedback> pageContent = criticalFeedbacks.subList(start, end);

        return new PageImpl<>(pageContent, pageRequest, criticalFeedbacks.size());
    }

    public void markFeedbackResolved(Long feedbackId) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(feedbackId);
        if (feedbackOpt.isPresent()) {
            Feedback feedback = feedbackOpt.get();
            System.out.println("Marked feedback as resolved: " + feedbackId);
        }
    }

    public List<String> getAllBranches() {
        return userRepository.findAll().stream()
                .map(user -> user.getBranch())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<Feedback> getFeedbacksForStats(String branch, Position position,
                                                LocalDateTime start, LocalDateTime end) {
        return feedbackRepository.findAll().stream()
                .filter(f -> f.getCreatedAt().isAfter(start) && f.getCreatedAt().isBefore(end))
                .filter(f -> branch == null || f.getUser().getBranch().equals(branch))
                .filter(f -> position == null || f.getUser().getPosition().equals(position))
                .collect(Collectors.toList());
    }
}