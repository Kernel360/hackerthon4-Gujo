import React, { useEffect, useState } from 'react';
import './QuizPlay.css';

interface Question {
  id?: number;
  question: string;
  answers: number[];
}

interface UserRank {
  username: string;
  rank: number;
}

const QuizPlay: React.FC = () => {
  const [quizId, setQuizId] = useState<string>('');
  const [pin, setPin] = useState<string>('');
  const [username, setUsername] = useState<string>('');
  const [isActive, setIsActive] = useState<boolean>(false);
  const [currentQuestion, setCurrentQuestion] = useState<Question | null>(null);
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
  const [score, setScore] = useState<number>(0);
  const [userRank, setUserRank] = useState<UserRank | null>(null);
  const [showResults, setShowResults] = useState<boolean>(false);
  const [eventSource, setEventSource] = useState<EventSource | null>(null);
  const [answerResult, setAnswerResult] = useState<string | null>(null);

  useEffect(() => {
    return () => {
      if (eventSource) {
        eventSource.close();
      }
    };
  }, [eventSource]);

  const handleJoinQuiz = (e: React.FormEvent) => {
    e.preventDefault();

    if (!quizId || !pin || !username) {
      alert('Please fill in all fields');
      return;
    }

    const newEventSource = new EventSource(
      `http://localhost:8080/api/quiz/subscribe?quizId=${quizId}&pin=${pin}&username=${username}`,
      { withCredentials: true }
    );

    setEventSource(newEventSource);

    newEventSource.addEventListener('question', (event) => {
      try {
        console.log('Question received:', event.data);
        const parsedData = JSON.parse(event.data);

        if (typeof parsedData === 'string' && parsedData.includes('subscribe complete')) {
          console.log('Successfully connected to quiz');
          return;
        }

        if (parsedData.question && parsedData.answers) {
          setCurrentQuestion({
            id: parsedData.id || parseInt(event.type, 10), // 서버에서 id 제공 여부 확인
            question: parsedData.question,
            answers: parsedData.answers,
          });
          setSelectedAnswer(null);
          setAnswerResult(null);
        }
      } catch (error) {
        console.error('Error parsing question:', error);
      }
    });

    newEventSource.addEventListener('user-rank', (event) => {
      try {
        const parsedData = JSON.parse(event.data);
        if (parsedData.username && parsedData.rank !== undefined) {
          setUserRank(parsedData);
          setShowResults(true);
        }
      } catch (error) {
        console.error('Error parsing user rank:', error);
      }
    });

    newEventSource.onerror = (error) => {
      console.error('EventSource error:', error);
      if (newEventSource.readyState === EventSource.CLOSED) {
        console.log('Connection closed');
      }
    };

    setIsActive(true);
  };

  const handleAnswerSubmit = async (answerNumber: number) => {
    if (!currentQuestion || selectedAnswer !== null || !currentQuestion.id) {
      console.log('Cannot submit: No question, already answered, or no question ID');
      return;
    }

    setSelectedAnswer(answerNumber);
    setAnswerResult('Submitting...');

    try {
      const url = `http://localhost:8080/api/quiz/${quizId}/answer?username=${encodeURIComponent(username)}&questionId=${currentQuestion.id}&answer=${answerNumber}`;
      console.log('Submitting answer to:', url);

      const response = await fetch(url, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const result = await response.text();
        console.log('Answer response:', result);
        setAnswerResult(result);
        if (!result.toLowerCase().includes('incorrect')) {
          setScore((prev) => prev + 1);
        }
      } else {
        console.error('Response not OK:', response.status, response.statusText);
        setAnswerResult(`Error: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Error submitting answer:', error);
      setAnswerResult('Failed to submit answer');
    }
  };

  if (showResults) {
    return (
      <div className="quiz-results">
        <h2>Quiz Complete!</h2>
        <p>Your Score: {score}</p>
        {userRank && (
          <div className="user-rank-container">
            <h3>Your Ranking</h3>
            <p>Rank: {userRank.rank}</p>
            <p>Username: {userRank.username}</p>
          </div>
        )}
      </div>
    );
  }

  if (!isActive) {
    return (
      <div className="quiz-join">
        <h2>Join Quiz</h2>
        <form onSubmit={handleJoinQuiz}>
          <div className="form-group">
            <label htmlFor="quizId">Quiz ID:</label>
            <input
              id="quizId"
              type="text"
              placeholder="Quiz ID"
              value={quizId}
              onChange={(e) => setQuizId(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="pin">PIN:</label>
            <input
              id="pin"
              type="text"
              placeholder="Enter PIN"
              value={pin}
              onChange={(e) => setPin(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="username">Username:</label>
            <input
              id="username"
              type="text"
              placeholder="Your Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="join-button">Join</button>
        </form>
      </div>
    );
  }

  return (
    <div className="quiz-active">
      {currentQuestion ? (
        <div className="question-container">
          <h2 className="question-title">Question</h2>
          <p className="question-text">{currentQuestion.question}</p>
          <div className="options-container">
            {currentQuestion.answers.map((option) => (
              <button
                key={option}
                className={`answer-button ${selectedAnswer === option ? 'selected' : ''}`}
                onClick={() => handleAnswerSubmit(option)}
                disabled={selectedAnswer !== null}
              >
                Option {option}
              </button>
            ))}
          </div>
          {answerResult && <p className="answer-result">{answerResult}</p>}
        </div>
      ) : (
        <div className="waiting-container">
          <h2>Waiting for question...</h2>
          <p>Get ready!</p>
        </div>
      )}  
      <div className="score-container">
        <p>Current Score: {score}</p>
      </div>
    </div>
  );
};

export default QuizPlay;