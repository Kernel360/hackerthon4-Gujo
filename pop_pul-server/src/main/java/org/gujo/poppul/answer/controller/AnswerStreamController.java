package org.gujo.poppul.answer.controller;

import org.gujo.poppul.quiz.entity.Quiz;
import org.gujo.poppul.quiz.repository.EmitterRepository;
import org.gujo.poppul.quiz.repository.QuizStreamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class AnswerStreamController {

    @Autowired
    private EmitterRepository emitterRepository;
    @Autowired
    private QuizStreamRepository quizStreamRepository;

    @PostMapping("/{quizId}/answer")
    public String submitAnswer(
            @PathVariable Long quizId,
            @RequestParam String username,
            @RequestParam Long questionId,
            @RequestParam Integer answer) {
        // Your existing logic to validate answer and update score
        boolean isCorrect = checkAnswer(quizId, questionId, answer); // Placeholder
        if (isCorrect) {
            emitterRepository.updateScore(username, quizId); // Assuming 10 points
            return "Correct!";
        }
        return "Incorrect";
    }

    private boolean checkAnswer(Long quizId, Long questionId, Integer answer) {
        if (quizId == null || questionId == null || answer == null) {
            return false; // Invalid input
        }

        // Fetch the Quiz entity
        Quiz quiz = quizStreamRepository.findById(quizId).orElse(null);
        if (quiz == null) {
            return false; // Quiz not found
        }

        // Find the Question by questionId
        return quiz.getQuestionList().stream()
                .filter(question -> question.getId().equals(questionId))
                .findFirst()
                .map(question ->
                        // Check if the submitted answer matches the correct answer’s 'no'
                        question.getAnswerList().stream()
                                .filter(a -> a.is_answer()) // Find the correct answer
                                .anyMatch(a -> a.getNo() == answer) // Match submitted answer
                )
                .orElse(false); // Question not found or no correct answer
    }
}