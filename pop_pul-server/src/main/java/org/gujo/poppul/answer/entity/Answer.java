package org.gujo.poppul.answer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.gujo.poppul.question.entity.Question;
import org.gujo.poppul.quiz.entity.Quiz;

@Getter(AccessLevel.PUBLIC)
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "Answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int no;
    private String content;

    @JsonProperty("is_answer")  // JSON 직렬화/역직렬화 시 is_answer로 매핑
    @Column
    private boolean is_answer;

    @ManyToOne()
    @ToString.Exclude
    @JsonIgnore
    @JoinColumn(name = "question_id")
    private Question question;
}