package com.fuzis.booksbackend.entity;

import com.fuzis.booksbackend.entity.enumerate.GoalsPeriod;
import com.fuzis.booksbackend.entity.enumerate.GoalsType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_goals", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pgoal_id")
    private Integer pgoalId;

    @Column(name = "user_id")
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period")
    private GoalsPeriod period;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "amount")
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type")
    private GoalsType goalType;
}