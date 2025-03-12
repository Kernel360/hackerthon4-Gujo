import { Routes, Route } from "react-router-dom";
import React from "react";
import QuizList from "../pages/quiz/QuizList";
import QuizCreate from "../pages/quiz/QuizCreate";
import QuizDetail from "../pages/quiz/QuizDetail";
import QuizEdit from "../pages/quiz/QuizEdit";
import ProblemCreate from "../pages/quiz/ProblemCreate";
import ProblemEdit from "../pages/quiz/ProblemEdit";

const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<QuizList />} />
      <Route path="/quiz" element={<QuizList />} />
      <Route path="/quiz/create" element={<QuizCreate />} />
      <Route path="/quiz/:id" element={<QuizDetail />} />
      <Route path="/quiz/:id/edit" element={<QuizEdit />} />
      <Route path="/quiz/:id/problems/create" element={<ProblemCreate />} />
      <Route
        path="/quiz/:id/problems/:problemId/edit"
        element={<ProblemEdit />}
      />
    </Routes>
  );
};

export default AppRoutes;
