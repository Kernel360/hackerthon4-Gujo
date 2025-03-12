package org.gujo.poppul.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gujo.poppul.answer.entity.Answer;
import org.gujo.poppul.question.dto.QuestionDto;
import org.gujo.poppul.question.entity.Question;
import org.gujo.poppul.question.exception.QuestionNotFoundException;
import org.gujo.poppul.question.repository.QuestionRepository;
import org.gujo.poppul.quiz.dto.QuizDto;
import org.gujo.poppul.quiz.entity.Quiz;
import org.gujo.poppul.quiz.exception.QuizNotFoundException;
import org.gujo.poppul.quiz.exception.UnauthorizedAccessException;
import org.gujo.poppul.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    // ✅ 특정 사용자의 모든 퀴즈 조회
    public List<Quiz> getQuizzesByUserId(String userId) {
        return quizRepository.findByUserId(userId);
    }

    // ✅ 특정 퀴즈 조회
    public Quiz getQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));
    }

    @Transactional
    public Quiz createQuiz(QuizDto quizDto) {
        if (quizDto.getUserId() == null || quizDto.getUserId().isEmpty()) {
            throw new RuntimeException("userId는 필수 값입니다.");
        }
        // QuizDto -> Quiz 엔티티로 변환
        Quiz quiz = quizDto.toEntity();
        log.info("Quiz UserId: {}", quiz.getUser_id());  // 로그 추가
        return quizRepository.save(quiz);
    }

    // ✅ 퀴즈 수정 (본인만 수정 가능하도록 userId 검증 추가)
    @Transactional
    public Quiz updateQuiz(Long quizId, QuizDto quizDto, String userId) {
        return quizRepository.findById(quizId).map(quiz -> {
            if (!quiz.getUser_id().equals(userId)) {
                throw new RuntimeException("수정 권한이 없습니다.");
            }
            quiz.setTitle(quizDto.getTitle());
            return quizRepository.save(quiz);
        }).orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));
    }

    // ✅ 퀴즈 삭제 (본인만 삭제 가능하도록 userId 검증 추가)
    public void deleteQuiz(Long quizId, String userId) {
        quizRepository.findById(quizId).ifPresentOrElse(quiz -> {
            if (!quiz.getUser_id().equals(userId)) {
                throw new UnauthorizedAccessException("삭제 권한이 없습니다.");
            }
            quizRepository.deleteById(quizId);
            log.info("퀴즈 삭제 완료: 퀴즈 ID={}", quizId);
        }, () -> {
            throw new QuizNotFoundException("퀴즈를 찾을 수 없습니다.");
        });
    }


    // ✅ 문제 추가
    @Transactional
    public Question addQuestion(Long quizId, QuestionDto questionDto) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));
        Question question = questionDto.toEntity();
        question.setQuiz(quiz);
        return questionRepository.save(question);
    }

    // ✅ 문제 수정
    @Transactional
    public Question updateQuestion(Long quizId, Long questionId, QuestionDto questionDto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Question not found"));

        if (!question.getQuiz().getId().equals(quizId)) {
            throw new IllegalArgumentException("Question does not belong to the specified quiz");
        }

        question.setTitle(questionDto.getTitle());
        question.setImage(questionDto.getImage());

        // 기존 답변 삭제
        question.getAnswerList().clear();

        // 새로운 답변 추가
        questionDto.getAnswerList().forEach(answerDto -> {
            Answer answer = answerDto.toEntity();
            answer.setQuestion(question);
            question.getAnswerList().add(answer);
        });

        return questionRepository.save(question);
    }

    // ✅ 문제 삭제
    public void deleteQuestion(Long quizId, Long questionId) {
        // 퀴즈 존재 여부 확인
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("퀴즈를 찾을 수 없습니다."));

        // 문제가 해당 퀴즈에 속하는지 확인
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("문제를 찾을 수 없습니다."));

        if (!question.getQuiz().getId().equals(quizId)) {
            throw new IllegalArgumentException("해당 퀴즈에 속하지 않는 문제입니다.");
        }

        questionRepository.deleteById(questionId);
    }

    public Question createQuestion(Long quizId, QuestionDto questionDto) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("퀴즈를 찾을 수 없습니다."));

        Question question = questionDto.toEntity();
        question.setQuiz(quiz);

        return questionRepository.save(question);
    }

    // ✅ 특정 퀴즈의 모든 문제 조회
    public List<Question> getQuizQuestions(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException("퀴즈를 찾을 수 없습니다."));
        return questionRepository.findByQuizId(quizId);
    }

    // 특정한 퀴즈의 특정 문제 조회
    public Question getQuestion(Long quizId, Long questionId) {
        return questionRepository.findByQuizIdAndId(quizId, questionId)
                .orElseThrow(() -> new QuestionNotFoundException("문제를 찾을 수 없습니다."));
    }
}