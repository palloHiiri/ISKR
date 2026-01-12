import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

export type GoalPeriod = '1d' | '3d' | 'week' | 'month' | 'quarter' | 'year';
export type GoalType = 'books_read' | 'pages_read';

export interface Goal {
  pgoalId: number;
  userId: number;
  period: GoalPeriod;
  startDate: string;
  amount: number;
  goalType: GoalType;
  currentProgress: number;
  isCompleted: boolean;
  endDate: string;
}

export interface GoalStats {
  totalGoals: number;
  completedGoals: number;
  inProgressGoals: number;
  failedGoals: number;
}

export interface ReadingStats {
  totalBooksRead: number;
  totalPagesRead: number;
  currentlyReadingBooks: number;
  planningToReadBooks: number;
  delayedBooks: number;
  gaveUpBooks: number;
}

export interface CreateGoalRequest {
  period: GoalPeriod;
  amount: number;
  goalType: GoalType;
}

export interface UpdateGoalRequest extends CreateGoalRequest {}

export const goalService = {
  getGoals: async (): Promise<Goal[]> => {
    const response = await axios.get<ApiResponse<{
      totalGoals: number;
      userId: number;
      goals: Goal[];
    }>>(`${OAPI_BASE_URL}/v1/reading/goals`);
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key.goals;
    }
    
    throw new Error(response.data.data.message || 'Не удалось загрузить цели');
  },

  createGoal: async (data: CreateGoalRequest): Promise<Goal> => {
    const response = await axios.post<ApiResponse<Goal>>(
      `${OAPI_BASE_URL}/v1/reading/goals`,
      data
    );
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    
    throw new Error(response.data.data.message || 'Не удалось создать цель');
  },

  updateGoal: async (goalId: number, data: UpdateGoalRequest): Promise<Goal> => {
    const response = await axios.put<ApiResponse<Goal>>(
      `${OAPI_BASE_URL}/v1/reading/goals`,
      data,
      { params: { goalId } }
    );
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    
    throw new Error(response.data.data.message || 'Не удалось обновить цель');
  },

  deleteGoal: async (goalId: number): Promise<void> => {
    const response = await axios.delete<ApiResponse<null>>(
      `${OAPI_BASE_URL}/v1/reading/goals`,
      { params: { goalId } }
    );
    
    if (response.data.data.state !== 'OK') {
      throw new Error(response.data.data.message || 'Не удалось удалить цель');
    }
  },

  getGoalStats: async (): Promise<GoalStats> => {
    const response = await axios.get<ApiResponse<{
      stats: GoalStats;
      userId: number;
    }>>(`${OAPI_BASE_URL}/v1/reading/goals/stats`);
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key.stats;
    }
    
    throw new Error(response.data.data.message || 'Не удалось загрузить статистику целей');
  },

  getReadingStats: async (): Promise<ReadingStats> => {
    const response = await axios.get<ApiResponse<{
      stats: ReadingStats;
      userId: number;
    }>>(`${OAPI_BASE_URL}/v1/reading/stats`);
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key.stats;
    }
    
    throw new Error(response.data.data.message || 'Не удалось загрузить статистику чтения');
  },
};

export const getPeriodDisplayName = (period: GoalPeriod): string => {
  switch (period) {
    case '1d': return '1 день';
    case '3d': return '3 дня';
    case 'week': return 'Неделя';
    case 'month': return 'Месяц';
    case 'quarter': return 'Квартал';
    case 'year': return 'Год';
    default: return period;
  }
};

export const getGoalTypeDisplayName = (type: GoalType): string => {
  switch (type) {
    case 'books_read': return 'Книги';
    case 'pages_read': return 'Страницы';
    default: return type;
  }
};

export const calculateGoalProgress = (goal: Goal): { percentage: number; isOverdue: boolean } => {
  const percentage = Math.min(Math.round((goal.currentProgress / goal.amount) * 100), 100);
  const isOverdue = !goal.isCompleted && new Date(goal.endDate) < new Date();
  
  return { percentage, isOverdue };
};

export default goalService;