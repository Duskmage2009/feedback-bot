package com.autoservice.feedbackbot.controller;

import com.autoservice.feedbackbot.entity.Feedback;
import com.autoservice.feedbackbot.enums.Position;
import com.autoservice.feedbackbot.enums.Sentiment;
import com.autoservice.feedbackbot.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/feedbacks")
    public ResponseEntity<Page<Feedback>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) Position position,
            @RequestParam(required = false) Integer minCriticality,
            @RequestParam(required = false) Sentiment sentiment
    ) {
        Sort sort = Sort.by(sortDir.equals("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Feedback> feedbacks = adminService.getFeedbacks(
                pageRequest, branch, position, minCriticality, sentiment);

        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) Position position,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDateTime start = startDate != null ?
                LocalDateTime.parse(startDate) : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ?
                LocalDateTime.parse(endDate) : LocalDateTime.now();

        Map<String, Object> stats = adminService.getStatistics(branch, position, start, end);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/critical-feedbacks")
    public ResponseEntity<Page<Feedback>> getCriticalFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> criticalFeedbacks = adminService.getCriticalFeedbacks(pageRequest);
        return ResponseEntity.ok(criticalFeedbacks);
    }

    @PostMapping("/feedback/{id}/mark-resolved")
    public ResponseEntity<Void> markFeedbackResolved(@PathVariable Long id) {
        adminService.markFeedbackResolved(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/branches")
    public ResponseEntity<java.util.List<String>> getBranches() {
        java.util.List<String> branches = adminService.getAllBranches();
        return ResponseEntity.ok(branches);
    }
}