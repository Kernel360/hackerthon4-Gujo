package org.gujo.poppul.quiz.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.gujo.poppul.answer.entity.Answer;
import org.gujo.poppul.question.entity.Question;
import org.gujo.poppul.question.service.QuestionStreamService;
import org.gujo.poppul.quiz.dto.QuizStreamResponse;
import org.gujo.poppul.quiz.entity.Quiz;
import org.gujo.poppul.quiz.repository.EmitterRepository;
import org.gujo.poppul.quiz.repository.QuizStreamRepository;
import org.gujo.poppul.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class QuizStreamService {

    @Autowired
    private QuizStreamRepository quizStreamRepository;
    @Autowired
    private QuestionStreamService questionStreamService;
    @Autowired
    private EmitterRepository emitterRepository;
    @Autowired
    private UserService userService;

    private final HashMap<Long, Integer> pinList = new HashMap<>();
    private String adminUsername;

    public SseEmitter createQuiz(Long quizId) {
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null");
        }
        adminUsername = userService.getCurrentUserName();
        if (adminUsername == null) {
            adminUsername = "default-admin-" + UUID.randomUUID().toString().substring(0, 8);
            log.warn("No authenticated user found; assigned temporary admin: {}", adminUsername);
        }
        int pin = (int)(Math.random() * 9000) + 1000;
        log.info("pin: {} for quizId: {}", pin, quizId);

        pinList.put(quizId, pin);
        return subscribe(quizId, adminUsername, pin);
    }

    public SseEmitter subscribe(Long quizId, String username, Integer pin) {
        log.info("Subscribing - quizId: {}, username: {}, pin: {}", quizId, username, pin);
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null in subscribe");
        }
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null in subscribe");
        }
        if (pin == null) {
            throw new IllegalArgumentException("Pin cannot be null in subscribe");
        }
        if (!pinList.containsKey(quizId) || !pinList.get(quizId).equals(pin)) {
            throw new IllegalArgumentException("Invalid PIN for quiz ID: " + quizId);
        }

        SseEmitter sseEmitter = emitterRepository.save(quizId, username, pin, new SseEmitter(-1L));
        sseEmitter.onCompletion(() -> {
            log.info("SseEmitter 연결 종료: {}", username);
            emitterRepository.deleteByName(username);
        });
        sseEmitter.onTimeout(() -> {
            log.info("SseEmitter 타임아웃: {}", username);
            emitterRepository.deleteByName(username);
        });

        if (adminUsername != null && adminUsername.equals(username)) {
            broadcast(username, "pin: " + pin);
        } else {
            broadcast(username, "subscribe complete, username: " + username);
        }

        if (adminUsername != null && adminUsername.equals(username)) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    startQuiz(quizId);
                } catch (InterruptedException e) {
                    log.error("Error starting quiz in thread for quizId: {}", quizId, e);
                    throw new RuntimeException(e);
                }
            }).start();
        }

        return sseEmitter;
    }

    @Transactional
    public void startQuiz(Long quizId) throws InterruptedException {
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null in startQuiz");
        }
        Quiz quiz = quizStreamRepository.findById(quizId).orElse(null);
        log.info("Starting quiz {}", quizId);
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz not found for id: " + quizId);
        }

        List<Question> questions = quiz.getQuestionList();
        log.info("Loaded {} questions", questions.size());
        for (Question question : questions) {
            log.info("Question: {}", question.getTitle());
        }

        Collection<SseEmitter> sseEmitters = emitterRepository.findByQuizId(quizId);

        for (Question question : questions) {
            try {
                Map<String, Object> adminData = new HashMap<>();
                adminData.put("question", question.getTitle());
                Map<Integer, String> answerMap = new HashMap<>();
                for (Answer answer : question.getAnswerList()) {
                    answerMap.put(answer.getNo(), answer.getContent());
                }
                adminData.put("answers", answerMap);

                Map<String, Object> userData = new HashMap<>();
                userData.put("id", question.getId()); // Added for non-admin clients
                userData.put("question", question.getTitle());
                List<Integer> answerNumbers = new ArrayList<>();
                for (int i = 1; i <= question.getAnswerList().size(); i++) {
                    answerNumbers.add(i);
                }
                userData.put("answers", answerNumbers);

                for (SseEmitter sseEmitter : sseEmitters) {
                    String emitterUsername = emitterRepository.findUsernameByEmitter(sseEmitter);
                    if (emitterUsername != null) {
                        if (adminUsername.equals(emitterUsername)) {
                            log.info("Sending question to admin {}: {}", emitterUsername, question.getTitle());
                            sseEmitter.send(SseEmitter.event().name("question").data(adminData));
                        } else {
                            log.info("Sending question to user {}: {}", emitterUsername, question.getTitle());
                            sseEmitter.send(SseEmitter.event().name("question").data(userData));
                        }
                    } else {
                        log.warn("No username found for emitter; skipping send");
                    }
                }
                Thread.sleep(10000);
            } catch (IOException e) {
                log.error("Failed to send question for quizId: {}", quizId, e);
                throw new RuntimeException("연결 오류", e);
            }
        }

        List<Map.Entry<String, Integer>> rank = emitterRepository.getRankedScores(quizId);
        Map<String, Object> rankData = new HashMap<>();
        rankData.put("ranking", rank);

        for (SseEmitter sseEmitter : sseEmitters) {
            String username = emitterRepository.findUsernameByEmitter(sseEmitter);
            if (username != null && adminUsername.equals(username)) {
                try {
                    sseEmitter.send(SseEmitter.event().name("ranking").data(rankData));
                    log.info("Sent ranking to admin: {}", username);
                } catch (IOException e) {
                    log.error("Error sending ranking to admin: {}", username, e);
                    emitterRepository.deleteByName(username);
                }
            }
        }

        for (SseEmitter sseEmitter : sseEmitters) {
            String username = emitterRepository.findUsernameByEmitter(sseEmitter);
            if (username != null) {
                int rankIndex = rank.stream()
                        .filter(entry -> entry.getKey().equals(username))
                        .map(rankEntry -> rank.indexOf(rankEntry) + 1)
                        .findFirst()
                        .orElse(-1);

                Map<String, Object> userRankData = new HashMap<>();
                userRankData.put("username", username);
                userRankData.put("rank", rankIndex);

                try {
                    sseEmitter.send(SseEmitter.event().name("user-rank").data(userRankData));
                    log.info("Sent user rank for {}: {}", username, rankIndex);
                } catch (IOException e) {
                    log.error("Error sending rank to user: {}", username, e);
                    emitterRepository.deleteByName(username);
                }
            }
        }
    }

    public Optional<QuizStreamResponse> getQuizById(Long quizId) {
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null in getQuizById");
        }
        Quiz quiz = quizStreamRepository.findById(quizId).orElse(null);
        return Optional.ofNullable(toDto(quiz));
    }

    public void broadcast(String username, Object object) {
        if (username == null) {
            log.warn("Broadcast attempted with null username; skipping");
            return;
        }
        SseEmitter sseEmitter = emitterRepository.findByName(username);
        if (sseEmitter != null) {
            try {
                sseEmitter.send(SseEmitter.event().name("question").data(object));
                log.info("Broadcast sent to {}: {}", username, object);
            } catch (IOException e) {
                log.error("Error broadcasting to {}: {}", username, e.getMessage());
                emitterRepository.deleteByName(username);
                throw new RuntimeException("연결 오류", e);
            }
        } else {
            log.warn("No emitter found for username: {}", username);
        }
    }

    private QuizStreamResponse toDto(Quiz quiz) {
        if (quiz == null) {
            return null;
        }
        var questionList = quiz.getQuestionList()
                .stream()
                .map(questionStreamService::toDto)
                .collect(Collectors.toList());
        return QuizStreamResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .questionList(questionList)
                .build();
    }

    public void stopQuiz(Long quizId) {
        if (quizId == null) {
            throw new IllegalArgumentException("Quiz ID cannot be null in stopQuiz");
        }
        emitterRepository.clearByQuizId(quizId);
        log.info("Stopped quiz and cleared emitters for quizId: {}", quizId);
    }
}