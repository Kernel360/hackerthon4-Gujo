package org.gujo.poppul.quiz.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gujo.poppul.question.dto.QuestionDto;
import org.gujo.poppul.question.entity.Question;
import org.gujo.poppul.quiz.dto.QuizDto;
import org.gujo.poppul.quiz.entity.Quiz;
import org.gujo.poppul.quiz.exception.QuizNotFoundException;
import org.gujo.poppul.quiz.exception.UnauthorizedAccessException;
import org.gujo.poppul.quiz.repository.QuizRepository;
import org.gujo.poppul.quiz.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final QuizRepository quizRepository;

    // ✅ 퀴즈 생성 (세션 기반 사용자 ID 추가)
    @PostMapping
    public ResponseEntity<QuizDto> addQuiz(@RequestBody QuizDto quizDto, HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId"); // 세션에서 userId 가져오기
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 로그인하지 않은 경우 401 반환
        }

        log.info("퀴즈 생성 요청: 사용자={}, 제목={}", userId, quizDto.getTitle());
        quizDto.setUserId(userId); // DTO에 userId 설정
        Quiz createdQuiz = quizService.createQuiz(quizDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuizDto.fromEntity(createdQuiz));
    }

    // ✅ 퀴즈 수정
    @PutMapping("/{quizId}")
    public ResponseEntity<QuizDto> updateQuiz(@PathVariable Long quizId, @RequestBody QuizDto quizDto, HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("퀴즈 수정 요청: 사용자={}, 퀴즈 ID={}, 제목={}", userId, quizId, quizDto.getTitle());
        Quiz updatedQuiz = quizService.updateQuiz(quizId, quizDto, userId); // 사용자 검증 추가
        return ResponseEntity.ok(QuizDto.fromEntity(updatedQuiz));
    }

    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long quizId, HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 인증되지 않은 사용자
        }

        try {
            log.info("퀴즈 삭제 요청: 사용자={}, 퀴즈 ID={}", userId, quizId);
            quizService.deleteQuiz(quizId, userId); // 서비스에서 삭제 처리
            return ResponseEntity.noContent().build(); // 성공적으로 삭제
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 권한이 없을 때
        } catch (QuizNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 퀴즈를 찾을 수 없을 때
        }
    }


    // ✅ 특정 사용자의 모든 퀴즈 조회
    @GetMapping
    public ResponseEntity<List<QuizDto>> getAllQuizzes(HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("사용자 퀴즈 목록 조회 요청: 사용자={}", userId);
        List<Quiz> quizzes = quizService.getQuizzesByUserId(userId); // 특정 사용자 퀴즈 조회
        List<QuizDto> quizDtos = quizzes.stream()
                .map(QuizDto::fromEntity)
                .toList();
        return ResponseEntity.ok(quizDtos);
    }

    // ✅ 특정 퀴즈 조회 (본인 퀴즈만 조회 가능)
    @GetMapping("/{quizId}")
    public ResponseEntity<QuizDto> getQuiz(@PathVariable Long quizId, HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Quiz quiz = quizService.getQuiz(quizId);
        if (!quiz.getUser_id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 본인 퀴즈가 아니면 403 반환
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
        if (!quiz.getUser_id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Question> questions = quizService.getQuizQuestions(quizId);
        List<QuestionDto> questionDtos = questions.stream()
                .map(QuestionDto::fromEntity)
                .toList();
        return ResponseEntity.ok(questionDtos);
    }

    // ✅ 문제 생성
    @PostMapping("/{quizId}/questions")
    public ResponseEntity<QuestionDto> createQuestion(
            @PathVariable Long quizId,
            @RequestBody QuestionDto questionDto,
            HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 퀴즈 소유자 확인
        Quiz quiz = quizService.getQuiz(quizId);
        if (!quiz.getUser_id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Question createdQuestion = quizService.createQuestion(quizId, questionDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(QuestionDto.fromEntity(createdQuestion));
        } catch (Exception e) {
            log.error("문제 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ 특정 문제 조회
    @GetMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<QuestionDto> getQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Quiz quiz = quizService.getQuiz(quizId);
        if (!quiz.getUser_id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Question question = quizService.getQuestion(quizId, questionId);
        return ResponseEntity.ok(QuestionDto.fromEntity(question));
    }

    // ✅ 문제 수정
    @PutMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<QuestionDto> updateQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @RequestBody QuestionDto questionDto,
            HttpServletRequest request
    ) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Quiz quiz = quizService.getQuiz(quizId);
        if (!quiz.getUser_id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Question updatedQuestion = quizService.updateQuestion(quizId, questionId, questionDto);
        return ResponseEntity.ok(QuestionDto.fromEntity(updatedQuestion));
    }

    // ✅ 문제 삭제
    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            HttpServletRequest request) {
        String userId = (String) request.getSession().getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 퀴즈 소유자 확인
        Quiz quiz = quizService.getQuiz(quizId);
        if (!quiz.getUser_id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            quizService.deleteQuestion(quizId, questionId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("문제 삭제 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
