import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

export interface Author {
  authorId: number;
  name: string;
  birthDate: string | null;
  description: string | null;
  realName: string;
}

export interface Genre {
  genreId: number;
  name: string;
}

export interface CreateAuthorData {
  name: string;
  realName?: string;
  description?: string;
  birthDate?: string | null;
}

export interface UpdateAuthorData {
  name?: string;
  realName?: string | null;
  description?: string | null;
  birthDate?: string | null;
}

export interface CreateGenreData {
  name: string;
}

export const adminAPI = {
  createAuthor: async (data: CreateAuthorData): Promise<Author> => {
    try {
      const response = await axios.post<ApiResponse<Author>>(
        `${OAPI_BASE_URL}/v1/author`,
        data,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось создать автора');
    } catch (error: any) {
      console.error('Error creating author:', error);
      throw error;
    }
  },

  updateAuthor: async (authorId: number, data: UpdateAuthorData): Promise<Author> => {
    try {
      const response = await axios.put<ApiResponse<Author>>(
        `${OAPI_BASE_URL}/v1/author?authorId=${authorId}`,
        data,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось обновить автора');
    } catch (error: any) {
      console.error('Error updating author:', error);
      throw error;
    }
  },

  deleteAuthor: async (authorId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/author?authorId=${authorId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить автора');
      }
    } catch (error: any) {
      console.error('Error deleting author:', error);
      throw error;
    }
  },

  createGenre: async (data: CreateGenreData): Promise<Genre> => {
    try {
      const response = await axios.post<ApiResponse<Genre>>(
        `${OAPI_BASE_URL}/v1/genre`,
        data,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось создать жанр');
    } catch (error: any) {
      console.error('Error creating genre:', error);
      throw error;
    }
  },

  updateGenre: async (genreId: number, name: string): Promise<Genre> => {
    try {
      const response = await axios.put<ApiResponse<Genre>>(
        `${OAPI_BASE_URL}/v1/genre?genreId=${genreId}`,
        { name },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось обновить жанр');
    } catch (error: any) {
      console.error('Error updating genre:', error);
      throw error;
    }
  },

  deleteGenre: async (genreId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/genre?genreId=${genreId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить жанр');
      }
    } catch (error: any) {
      console.error('Error deleting genre:', error);
      throw error;
    }
  },
};

export default adminAPI;