import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import './QuizStart.css';

interface Ranking {
  ranking: any[];
}

interface UserRank {
  rank: number;
  username: string;
}

const QuizStart: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [pin, setPin] = useState<string>("");
  const [questionText, setQuestionText] = useState<string>("");
  const [answerOptions, setAnswerOptions] = useState<Record<string, string>>({});
  const [ranking, setRanking] = useState<Ranking | null>(null);
  const [userRank, setUserRank] = useState<UserRank | null>(null);

  useEffect(() => {
    if (!id) return;
    const eventSource = new EventSource(`http://localhost:5173/api/quiz/create?quizId=${id}`, {
      withCredentials: true,
    });

    eventSource.addEventListener('question', (event) => {
      const data = event.data;
      console.log('Question event:', data);

      if (data.includes('pin:')) {
        const pinMatch = data.match(/pin: (\d+)/);
        if (pinMatch) {
          setPin(pinMatch[1]);
        }
      } else {
        try {
          const parsedData = JSON.parse(data);
          if (parsedData.question && parsedData.answers) {
            setQuestionText(parsedData.question);
            setAnswerOptions(parsedData.answers);
          }
        } catch (error) {
          console.error('Error parsing question:', error);
        }
      }
    });

    eventSource.addEventListener('ranking', (event) => {
      try {
        const parsedData: Ranking = JSON.parse(event.data);
        if (parsedData.ranking !== undefined) {
          setRanking(parsedData);
        }
      } catch (error) {
        console.error('Error parsing ranking:', error);
      }
    });

    eventSource.addEventListener('user-rank', (event) => {
      try {
        const parsedData: UserRank = JSON.parse(event.data);
        if (parsedData.rank !== undefined && parsedData.username) {
          setUserRank(parsedData);
        }
      } catch (error) {
        console.error('Error parsing user-rank:', error);
      }
    });

    eventSource.onerror = () => {
      console.error('EventSource error.');
    };

    return () => {
      eventSource.close();
    };
  }, [id]);

  const handleStartQuiz = async () => {
    try {
      const response = await fetch(`http://localhost:5173/api/quiz/start/${id}`, {
        method: 'GET',
        credentials: 'include',
      });
      if (!response.ok) {
        console.error('Failed to start quiz:', response.statusText);
      }
    } catch (error) {
      console.error('Error starting quiz:', error);
    }
  };

  const handleEndQuiz = async () => {
    try {
      const response = await fetch(`http://localhost:5173/api/quiz/start/${id}`, {
        method: 'DELETE',
        credentials: 'include',
      });
      if (response.ok) {
        navigate('/');
      }
    } catch (error) {
      console.error('Error ending quiz:', error);
    }
  };

  return (
    <div className="quiz-container">
      <div className="quiz-content">
        <h1 className="quiz-title">Quiz Room</h1>

          <div className="quizid-display">
            <h2>Quiz ID: {id}</h2>
          </div>
        {pin && (
          <div className="pin-display">
            <h2>Room PIN: {pin}</h2>
          </div>
        )}

        {!ranking && (
          <>
            <button onClick={handleStartQuiz} className="start-button">
              Start Quiz
            </button>
          
              <div className="question-container">
                <h3 className="question-text">{questionText}</h3>
                <div className="answers-grid">
                  {Object.entries(answerOptions).map(([key, value]) => (
                    <div key={key} className="answer-box">
                      {key}: {value}
                    </div>
                  ))}
                </div>
              </div>

          </>
        )}
        {ranking && (
          <>
            <div className="ranking-container">
              <h3>Final Rankings</h3>
              <div className="rankings-list">
                {ranking.ranking.length > 0 ? (
                  ranking.ranking.map((rank, index) => (
                    <div key={index} className="rank-item">
                      {JSON.stringify(rank)}
                    </div>
                  ))
                ) : (
                  <p>No rankings available.</p>
                )}
              </div>
            </div>

            {userRank && (
              <div className="user-rank">
                <h3>Your Result</h3>
                <p>Rank: {userRank.rank}</p>
                <p>Username: {userRank.username}</p>
              </div>
            )}

            <button onClick={handleEndQuiz} className="end-button">
              End Quiz
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default QuizStart;