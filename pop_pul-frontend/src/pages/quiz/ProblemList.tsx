import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { problemService } from "../../services/problemService";
import { quizService } from "../../services/quizService";
import { Problem } from "../../types/problem";
import { Quiz } from "../../types/quiz";

const ProblemList: React.FC = () => {
  const { id: quizId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [problems, setProblems] = useState<Problem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadQuizAndProblems();
  }, [quizId]);

  const loadQuizAndProblems = async () => {
    if (!quizId) return;
    setIsLoading(true);
    try {
      const [quizData, problemsData] = await Promise.all([
        quizService.getQuiz(quizId),
        problemService.getProblems(Number(quizId)),
      ]);
      setQuiz(quizData);
      setProblems(problemsData);
      setError(null);
    } catch (error) {
      console.error("Error loading quiz and problems:", error);
      setError("데이터를 불러오는데 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (problemId: number) => {
    if (!quizId || !window.confirm("정말로 이 문제를 삭제하시겠습니까?"))
      return;

    try {
      await problemService.deleteProblem(Number(quizId), problemId);
      setProblems(problems.filter((problem) => problem.id !== problemId));
    } catch (error) {
      console.error("Error deleting problem:", error);
      setError("문제 삭제에 실패했습니다.");
    }
  };

  if (isLoading) return <div className="text-center p-4">로딩 중...</div>;
  if (error) return <div className="text-red-500 text-center p-4">{error}</div>;
  if (!quiz)
    return <div className="text-center p-4">퀴즈를 찾을 수 없습니다.</div>;

  return (
    <div className="container mx-auto p-4">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-2xl font-bold">{quiz.title} - 문제 관리</h1>
            <p className="text-gray-600 mt-1">총 {problems.length}개의 문제</p>
          </div>
          <button
            onClick={() => navigate(`/quiz/${quizId}/problems/create`)}
            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
          >
            새 문제 추가
          </button>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <div className="space-y-4">
          {problems.map((problem, index) => (
            <div
              key={problem.id}
              className="bg-white shadow-lg rounded-lg overflow-hidden"
            >
              <div className="p-6">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold mb-2">
                      문제 {index + 1}. {problem.title}
                    </h3>
                    {problem.image && (
                      <img
                        src={problem.image}
                        alt="문제 이미지"
                        className="mb-4 max-w-md rounded"
                      />
                    )}
                    <div className="space-y-2">
                      {problem.answerList.map((answer, answerIndex) => (
                        <div
                          key={answerIndex}
                          className={`p-2 rounded ${
                            answer.is_answer
                              ? "bg-green-100 border border-green-300"
                              : "bg-gray-50 border border-gray-200"
                          }`}
                        >
                          {answer.content}
                        </div>
                      ))}
                    </div>
                  </div>
                  <div className="flex space-x-2 ml-4">
                    <button
                      onClick={() =>
                        navigate(`/quiz/${quizId}/problems/${problem.id}/edit`)
                      }
                      className="bg-yellow-500 text-white px-3 py-1 rounded hover:bg-yellow-600"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(problem.id)}
                      className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600"
                    >
                      삭제
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}

          {problems.length === 0 && (
            <div className="text-center text-gray-500 py-8">
              아직 등록된 문제가 없습니다.
            </div>
          )}
        </div>

        <div className="mt-6 flex justify-end">
          <button
            onClick={() => navigate(`/quiz/${quizId}`)}
            className="text-gray-600 hover:text-gray-800"
          >
            퀴즈 상세 정보로 돌아가기
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProblemList;
