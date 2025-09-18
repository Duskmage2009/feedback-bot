package com.autoservice.feedbackbot.repository;

import com.autoservice.feedbackbot.entity.Feedback;
import com.autoservice.feedbackbot.enums.Position;
import com.autoservice.feedbackbot.enums.Sentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserBranch(String branch);

    List<Feedback> findByUserPosition(Position position);

    List<Feedback> findByCriticalityLevelGreaterThanEqual(Integer level);

    List<Feedback> findBySentiment(Sentiment sentiment);

    @Query("SELECT f FROM Feedback f WHERE f.user.branch = :branch AND f.user.position = :position")
    List<Feedback> findByBranchAndPosition(@Param("branch") String branch, @Param("position") Position position);

    @Query("SELECT f FROM Feedback f WHERE f.createdAt BETWEEN :start AND :end")
    List<Feedback> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}