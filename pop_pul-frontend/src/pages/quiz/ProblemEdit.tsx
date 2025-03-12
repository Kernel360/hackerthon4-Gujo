import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Problem, CreateProblemDto } from "../../types/problem";
import { problemService } from "../../services/problemService";

const ProblemEdit: React.FC = () => {
  const { id, questionId } = useParams();
  const navigate = useNavigate();
  const [formData, setFormData] = useState<CreateProblemDto>({
    title: "",
    image: "",
    answerList: [
      { content: "", is_answer: false },
      { content: "", is_answer: false },
      { content: "", is_answer: false },
      { content: "", is_answer: false },
    ],
  });
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    console.log("URL params:", { id, questionId });
    if (!id || !questionId) {
      console.error("Missing URL parameters:", { id, questionId });
      return;
    }
    loadProblem();
  }, [id, questionId]);

  const loadProblem = async () => {
    if (!id || !questionId) return;
    try {
      console.log("Loading problem:", { quizId: id, questionId });
      const problem = await problemService.getProblem(id, questionId);
      setFormData({
        title: problem.title,
        image: problem.image || "",
        answerList: problem.answerList,
      });
    } catch (error) {
      console.error("Error loading problem:", error);
      setError("문제를 불러오는데 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === "answerList" ? prev.answerList : value,
    }));
  };

  const handleAnswerChange = (index: number, value: string) => {
    const newAnswers = [...formData.answerList];
    newAnswers[index] = { ...newAnswers[index], content: value };
    setFormData((prev) => ({
      ...prev,
      answerList: newAnswers,
    }));
  };

  const handleCorrectAnswerChange = (index: number) => {
    const newAnswers = formData.answerList.map((answer, i) => ({
      ...answer,
      is_answer: i === index,
    }));
    setFormData((prev) => ({
      ...prev,
      answerList: newAnswers,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !questionId) return;

    setIsSubmitting(true);
    setError(null);

    try {
      await problemService.updateProblem(
        Number(id),
        Number(questionId),
        formData
      );
      navigate(`/quiz/${id}`);
    } catch (error) {
      console.error("Error updating problem:", error);
      setError("문제 수정에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold mb-6">문제 수정</h1>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label
              htmlFor="title"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              문제
            </label>
            <textarea
              id="title"
              name="title"
              value={formData.title}
              onChange={handleChange}
              required
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="문제를 입력하세요"
            />
          </div>

          <div>
            <label
              htmlFor="image"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              이미지 URL (선택사항)
            </label>
            <input
              type="text"
              id="image"
              name="image"
              value={formData.image}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="이미지 URL을 입력하세요"
            />
          </div>

          <div className="space-y-4">
            <label className="block text-sm font-medium text-gray-700">
              보기
            </label>
            {formData.answerList.map((answer, index) => (
              <div key={index} className="flex items-center space-x-3">
                <input
                  type="radio"
                  name="correctAnswer"
                  checked={answer.is_answer}
                  onChange={() => handleCorrectAnswerChange(index)}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500"
                />
                <input
                  type="text"
                  value={answer.content}
                  onChange={(e) => handleAnswerChange(index, e.target.value)}
                  required
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder={`보기 ${index + 1}`}
                />
              </div>
            ))}
          </div>

          <div className="flex justify-end space-x-3">
            <button
              type="button"
              onClick={() => navigate(`/quiz/${id}`)}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className={`px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 ${
                isSubmitting ? "opacity-50 cursor-not-allowed" : ""
              }`}
            >
              {isSubmitting ? "수정 중..." : "수정 완료"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProblemEdit;
