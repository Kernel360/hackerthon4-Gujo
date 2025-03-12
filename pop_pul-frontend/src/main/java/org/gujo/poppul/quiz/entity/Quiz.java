package org.gujo.poppul.quiz.entity;
import jakarta.persistence.*;
import lombok.*;
import org.gujo.poppul.question.entity.Question;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "quiz")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title; // 퀴즈 제목

    @Column(name = "user_id")
    @Access(AccessType.FIELD)
    private String user_id;

    @Builder.Default
    @OneToMany(mappedBy = "quiz",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Question> questionList = new ArrayList<>();
} 