package org.gujo.poppul.question.repository;

import org.gujo.poppul.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuizId(Long quizId);

    // 특정 퀴즈(quizId) 내에서 특정 문제(questionId)를 찾는 메서드
    Optional<Question> findByQuizIdAndId(Long quizId, Long id);
}