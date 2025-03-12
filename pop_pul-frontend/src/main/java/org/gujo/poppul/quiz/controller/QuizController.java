package org.gujo.poppul.quiz.controller;

import org.gujo.poppul.quiz.dto.QuestionDto;
import org.gujo.poppul.quiz.dto.QuizDto;
import org.gujo.poppul.quiz.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // ✅ 특정 퀴즈 조회 (본인 퀴즈만 조회 가능)
    @GetMapping("/{quizId}")
    public ResponseEntity<QuizDto> getQuiz(@PathVariable Long quizId, HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Quiz quiz = quizService.getQuiz(quizId);
        if (!quiz.getUserId().equals(userId)) {  // getUser_id() -> getUserId()
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(QuizDto.fromEntity(quiz));
    }

    // ✅ 특정 퀴즈의 모든 문제 조회
    @GetMapping("/{quizId}/questions")
    public ResponseEntity<List<QuestionDto>> getQuizQuestions(@PathVariable Long quizId, HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Quiz quiz = quizService.getQuiz(quizId);
        if (!quiz.getUserId().equals(userId)) {  // getUser_id() -> getUserId()
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Question> questions = quizService.getQuizQuestions(quizId);
        List<QuestionDto> questionDtos = questions.stream()
                .map(QuestionDto::fromEntity)
                .toList();
        return ResponseEntity.ok(questionDtos);
    }
} 