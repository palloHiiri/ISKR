import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

export type ReadingStatusType = 'Planning' | 'Reading' | 'Delayed' | 'GaveUp' | 'Finished';

export interface ReadingStatusResponse {
  brsId: number;
  userId: number;
  bookId: number;
  readingStatus: ReadingStatusType;
  pageRead: number;
  lastReadDate: string | null;
  bookPageCnt: number;
  bookTitle: string;
}

export interface CreateReadingStatusRequest {
  bookId: number;
  readingStatus: ReadingStatusType;
}

export interface UpdateReadingStatusRequest {
  readingStatus: ReadingStatusType;
}

export interface AddProgressRequest {
  pageRead: number;
}

export const readingService = {
  getReadingStatus: async (bookId: number): Promise<ReadingStatusResponse | null> => {
    try {
      const response = await axios.get<ApiResponse<ReadingStatusResponse>>(
        `${OAPI_BASE_URL}/v1/reading/status`,
        { params: { bookId } }
      );
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }
      
      return null;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      console.error('Error getting reading status:', error);
      throw error;
    }
  },

  createReadingStatus: async (bookId: number, readingStatus: ReadingStatusType): Promise<ReadingStatusResponse> => {
    const response = await axios.post<ApiResponse<ReadingStatusResponse>>(
      `${OAPI_BASE_URL}/v1/reading/status`,
      { bookId, readingStatus }
    );
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    
    throw new Error(response.data.data.message || 'Не удалось создать статус чтения');
  },

  updateReadingStatus: async (bookId: number, readingStatus: ReadingStatusType): Promise<ReadingStatusResponse> => {
    const response = await axios.put<ApiResponse<ReadingStatusResponse>>(
      `${OAPI_BASE_URL}/v1/reading/status`,
      { readingStatus },
      { params: { bookId } }
    );
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    
    throw new Error(response.data.data.message || 'Не удалось обновить статус чтения');
  },

  addReadingProgress: async (bookId: number, pageRead: number): Promise<ReadingStatusResponse> => {
    const response = await axios.post<ApiResponse<ReadingStatusResponse>>(
      `${OAPI_BASE_URL}/v1/reading/status/add-progress`,
      { pageRead },
      { params: { bookId } }
    );
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    
    throw new Error(response.data.data.message || 'Не удалось добавить прогресс чтения');
  },
};

export default readingService;