import { Problem } from "./problem";

export interface Quiz {
  id: number;
  title: string;
  userId: string;
  description: string;
  problems?: Problem[];
}

export interface CreateQuizDto {
  title: string;
  description: string;
}

export interface UpdateQuizDto {
  title?: string;
  description?: string;
}
