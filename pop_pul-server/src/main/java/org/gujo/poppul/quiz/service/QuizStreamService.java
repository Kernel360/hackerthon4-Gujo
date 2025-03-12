    package org.gujo.poppul.quiz.service;

    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.gujo.poppul.answer.entity.Answer;
    import org.gujo.poppul.answer.repository.AnswerStreamRepository;
    import org.gujo.poppul.answer.service.AnswerStreamService;
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
    import java.util.concurrent.ConcurrentHashMap;
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

        HashMap<Long, Integer> pinList = new HashMap<>();
        @Autowired
        private UserService userService;


        // 이벤트 구독 허용, PIN 전달
        public SseEmitter createQuiz(Long quizId) {

            var admin = userService.getCurrentUserName();
            int pin = (int)(Math.random() * 9000) + 1000;
            log.info("pin: " + pin);

            pinList.put(quizId, pin);
            return subscribe(quizId, admin, pin);
        }

        // 문제 출제 시작
        @Transactional
        public void startQuiz(Long quizId) throws InterruptedException {
            Quiz quiz = quizStreamRepository.findById(quizId).orElse(null);
            log.info("Starting quiz {}", quizId);
            if (quiz == null) {
                throw new IllegalArgumentException("Quiz not found for id: " + quizId);
            }

            List<Question> questions = quiz.getQuestionList();
            log.info("Loaded {} questions", questions.size());
            for (Question question : questions) {
                log.info("Question: " + question.getTitle());
            }

            Collection<SseEmitter> sseEmitters = emitterRepository.findByQuizId(quizId);

            // 퀴즈 전송
            for (Question question : quiz.getQuestionList()) {
                try {
                    Map<String,Object> adminData = new HashMap<>();
                    adminData.put("question", question.getTitle());
                    Map<Integer, String> answerMap = new HashMap<>();
                    for (Answer answer : question.getAnswerList()) {
                        answerMap.put(answer.getNo(), answer.getContent());
                    }
                    adminData.put("answers", answerMap);

                    //
                    var admin=userService.getCurrentUserName();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("question/id", question.getTitle()+"/"+question.getId());
                    List<Integer> answerNumbers = new ArrayList<>();
                    for (int i = 1; i <= question.getAnswerList().size(); i++) {
                        answerNumbers.add(i);
                    }
                    userData.put("answers", answerNumbers);

                    for (SseEmitter sseEmitter : sseEmitters) {
                        String username = emitterRepository.findUsernameByEmitter(sseEmitter);
                        if (admin.equals(username)) {
                            log.info("Sending question to admin: " + question.getTitle());
                            sseEmitter.send(SseEmitter.event().name("question").data(adminData));
                        } else {
                            log.info("Sending question to user: " + question.getTitle());
                            sseEmitter.send(SseEmitter.event().name("question").data(userData));
                        }
                    }
                    Thread.sleep(10000); // 10초 대기
                } catch (IOException e) {
                    throw new RuntimeException("연결 오류", e);
                }
            }

            // 퀴즈 종료 후 랭크 화면
            List<Map.Entry<String, Integer>> rank = emitterRepository.getRankedScores(quizId);
            Map<String, Object> rankData = new HashMap<>();
            rankData.put("ranking", rank);

            for (SseEmitter sseEmitter : sseEmitters) {
                String username = emitterRepository.findUsernameByEmitter(sseEmitter);
                var admin = userService.getCurrentUserName();
                if (admin.equals(username)) {
                    try {
                        sseEmitter.send(SseEmitter.event().name("ranking").data(rankData));
                        log.info("Sent ranking to host");
                    } catch (IOException e) {
                        log.error("Error sending ranking to admin", e);
                        emitterRepository.deleteByName(username);
                    }
                }
            }

            for (SseEmitter sseEmitter : sseEmitters) {
                String username = emitterRepository.findUsernameByEmitter(sseEmitter);
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
                    log.error("Error sending rank to user", e);
                    emitterRepository.deleteByName(username);
                }
            }
        }

        public Optional<QuizStreamResponse> getQuizById(Long quizId) {
            Quiz quiz = quizStreamRepository.findById(quizId).orElse(null);
            return Optional.ofNullable(toDto(quiz));
        }

        public SseEmitter subscribe(Long quizId, String username, Integer pin) {
            log.info("pin: " + pin);
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

            var admin = userService.getCurrentUserName();
            if (admin.equals(username)) {
                broadcast(username, "pin: " + pin);
            } else {
                broadcast(username, "subscribe complete, username: " + username);
            }

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    startQuiz(quizId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            return sseEmitter;
        }

        public void broadcast(String username, Object object) {
            SseEmitter sseEmitter = emitterRepository.findByName(username);
            try {
                sseEmitter.send(SseEmitter.event().name("question").data(object));
            } catch (IOException e) {
                emitterRepository.deleteByName(username);
                throw new RuntimeException("연결 오류", e);
            }
        }

        private QuizStreamResponse toDto(Quiz quiz) {
            var questionList = quiz.getQuestionList()
                    .stream().map(questionStreamService::toDto).collect(Collectors.toList());
            return QuizStreamResponse.builder()
                    .id(quiz.getId())
                    .title(quiz.getTitle())
                    .questionList(questionList)
                    .build();
        }

        public void stopQuiz(Long quizId) {
            emitterRepository.clearByQuizId(quizId);
        }
    }