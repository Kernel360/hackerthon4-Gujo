import axios from "axios";
import { Problem, CreateProblemDto, UpdateProblemDto } from "../types/problem";

const API_URL = "http://localhost:8080/api/quiz";

// axios 인스턴스 생성
const axiosInstance = axios.create({
  withCredentials: true, // 쿠키/세션을 요청과 함께 전송
});

export const problemService = {
  // 특정 퀴즈의 모든 문제 조회
  getProblems: async (quizId: number): Promise<Problem[]> => {
    try {
      const response = await axiosInstance.get(
        `${API_URL}/${quizId}/questions`
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching problems:", error);
      throw error;
    }
  },

  // 특정 문제 조회
  getProblem: async (quizId: string, questionId: string): Promise<Problem> => {
    try {
      const response = await axiosInstance.get(
        `${API_URL}/${quizId}/questions/${questionId}`
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching problem:", error);
      throw error;
    }
  },

  // 새로운 문제 생성
  createProblem: async (
    quizId: number,
    problem: CreateProblemDto
  ): Promise<Problem> => {
    try {
      const response = await axiosInstance.post(
        `${API_URL}/${quizId}/questions`,
        problem
      );
      return response.data;
    } catch (error) {
      console.error("Error creating problem:", error);
      throw error;
    }
  },

  // 문제 수정
  updateProblem: async (
    quizId: number,
    questionId: number,
    problem: UpdateProblemDto
  ): Promise<Problem> => {
    try {
      const response = await axiosInstance.put(
        `${API_URL}/${quizId}/questions/${questionId}`,
        problem
      );
      return response.data;
    } catch (error) {
      console.error("Error updating problem:", error);
      throw error;
    }
  },

  // 문제 삭제
  deleteProblem: async (quizId: number, questionId: number): Promise<void> => {
    try {
      console.log(
        `Deleting problem: quizId=${quizId}, problemId=${questionId}`
      ); // 디버깅용 로그 추가
      await axiosInstance.delete(
        `${API_URL}/${quizId}/questions/${questionId}`
      );
    } catch (error) {
      console.error("Error deleting problem:", error);
      throw error;
    }
  },
};
