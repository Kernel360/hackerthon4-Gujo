package org.gujo.poppul.answer.controller;

import org.gujo.poppul.quiz.repository.EmitterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class AnswerStreamController {

    @Autowired
    private EmitterRepository emitterRepository;

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
        // Implement your logic to check the answer
        return true; // Placeholder
    }
}