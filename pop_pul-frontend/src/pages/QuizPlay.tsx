import React, { useEffect, useState } from 'react';

interface Question {
    id: number;
    content: string;
    options: string[];
    correctAnswer: string;
}

const QuizPlay: React.FC = () => {
    // Form states
    const [quizId, setQuizId] = useState('');
    const [pin, setPin] = useState('');
    const [username, setUsername] = useState('');

    // Quiz states
    const [currentQuestion, setCurrentQuestion] = useState<Question | null>(null);
    const [answers, setAnswers] = useState<Record<number, string>>({});
    const [score, setScore] = useState(0);
    const [isQuizActive, setIsQuizActive] = useState(false);
    const [timeLeft, setTimeLeft] = useState(30);
    const [questions, setQuestions] = useState<Question[]>([]);
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [showResults, setShowResults] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      try {
        const response = await fetch(`/api/quiz/subscribe?quizId=${quizId}&pin=${pin}&username=${username}`);
        const data = await response.json();
        if (data.success) {
          setIsQuizActive(true);
          // Initialize quiz data
          setQuestions(data.questions);
          setCurrentQuestion(data.questions[0]);
        }
      } catch (error) {
        console.error('Error:', error);
      }
    };

    const handleAnswerSubmit = async (answer: string) => {
      if (!currentQuestion) return;

      setAnswers(prev => ({
        ...prev,
        [currentQuestion.id]: answer
      }));

      try {
        const response = await fetch('/api/quiz/submit-answer', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            quizId,
            questionId: currentQuestion.id,
            answer,
            username
          }),
        });

        const data = await response.json();
        if (data.correct) {
          setScore(prev => prev + 1);
        }

        // Move to next question
        if (currentQuestionIndex < questions.length - 1) {
          setCurrentQuestionIndex(prev => prev + 1);
          setCurrentQuestion(questions[currentQuestionIndex + 1]);
          setTimeLeft(30);
        } else {
          setShowResults(true);
        }
      } catch (error) {
        console.error('Error submitting answer:', error);
      }
    };

    useEffect(() => {
      if (isQuizActive && !showResults) {
        const eventSource = new EventSource(`/api/quiz/stream/${quizId}`);
      
        eventSource.onmessage = (event) => {
          const data = JSON.parse(event.data);
          if (data.type === 'QUESTION_UPDATE') {
            setCurrentQuestion(data.question);
            setTimeLeft(30);
          } else if (data.type === 'QUIZ_END') {
            setShowResults(true);
            eventSource.close();
          }
        };

        return () => eventSource.close();
      }
    }, [isQuizActive, quizId, showResults]);

    useEffect(() => {
      let timer: NodeJS.Timeout;
      if (isQuizActive && timeLeft > 0 && !showResults) {
        timer = setInterval(() => {
          setTimeLeft(prev => prev - 1);
        }, 1000);
      } else if (timeLeft === 0) {
        handleAnswerSubmit(''); // Submit empty answer when time runs out
      }
      return () => clearInterval(timer);
    }, [timeLeft, isQuizActive, showResults]);

    if (showResults) {
      return (
        <div className="quiz-results">
          <h2>Quiz Complete!</h2>
          <p>Your Score: {score} / {questions.length}</p>
          <p>Correct Answers: {score}</p>
        </div>
      );
    }

    return (
      <div className="quiz-container">
        {!isQuizActive ? (
          <div className="quiz-join">
            <h1>Join Quiz</h1>
            <form onSubmit={handleSubmit} className="join-form">
              <div className="form-group">
                <label>Quiz ID:</label>
                <input
                  type="text"
                  value={quizId}
                  onChange={(e) => setQuizId(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label>PIN:</label>
                <input
                  type="text"
                  value={pin}
                  onChange={(e) => setPin(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label>Username:</label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
              <button type="submit">Join Quiz</button>
            </form>
          </div>
        ) : (
          <div className="quiz-active">
            {currentQuestion && (
              <>
                <div className="quiz-header">
                  <div className="timer">Time Left: {timeLeft}s</div>
                  <div className="progress">
                    Question {currentQuestionIndex + 1} of {questions.length}
                  </div>
                </div>
                <div className="question">
                  <h2>{currentQuestion.content}</h2>
                  <div className="options">
                    {currentQuestion.options.map((option, index) => (
                      <button
                        key={index}
                        onClick={() => handleAnswerSubmit(option)}
                        className="option-button"
                      >
                        {option}
                      </button>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    );
};

export default QuizPlay;