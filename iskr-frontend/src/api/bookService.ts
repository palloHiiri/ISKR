// /src/api/bookService.ts
import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

export interface ReviewResponse {
  data: {
    state: string;
    message: string;
    key: Review | null;
  };
  meta: {
    timestamp: string;
    processedBy: string;
    userId: string;
  };
}

// Обновляем интерфейс Review для соответствия ответу сервера
export interface Review {
  reviewId: number;
  user: {
    userId: number;
    username: string;
    registeredDate: string;
    nickname: string;
    profileImage: {
      imglId: number;
      imageData: {
        imgdId: number;
        uuid: string;
        size: number;
        mimeType: string;
        extension: string;
      };
    } | null;
  };
  score: number; // В 10-балльной системе (5 звёзд = 10)
  reviewText: string;
  bookId: number;
}

export interface BookDetail {
  bookId: number;
  isbn: string;
  title: string;
  subtitle: string | null;
  description: string;
  pageCnt: number;
  photoLink: {
    imglId: number;
    imageData: {
      imgdId: number;
      uuid: string;
      size: number;
      mimeType: string;
      extension: string;
    };
  } | null;
  addedBy: {
    userId: number;
    username: string;
    registeredDate: string;
    nickname: string;
    profileImage: {
      imglId: number;
      imageData: {
        imgdId: number;
        uuid: string;
        size: number;
        mimeType: string;
        extension: string;
      };
    } | null;
  };
  authors: Array<{
    authorId: number;
    name: string;
    birthDate: string | null;
    description: string | null;
    realName: string;
  }>;
  genres: Array<{
    genreId: number;
    name: string;
  }>;
  collectionsCount: number;
  averageRating: number;
  reviewsCount: number;
}

export interface Review {
  reviewId: number;
  user: {
    userId: number;
    username: string;
    registeredDate: string;
    nickname: string;
    profileImage: {
      imglId: number;
      imageData: {
        imgdId: number;
        uuid: string;
        size: number;
        mimeType: string;
        extension: string;
      };
    } | null;
  };
  score: number;
  reviewText: string;
}

export interface ReviewsResponse {
  reviews: Review[];
  batch: number;
  totalPages: number;
  page: number;
  bookId: number;
  totalElements: number;
}

export interface UpdateBookData {
  title?: string;
  subtitle?: string | null;
  description?: string | null;
  pageCnt?: number;
  isbn?: string;
  addedBy?: number;
  authorIds?: number[];
  genreIds?: number[];
  photoLink?: number; // Изменено с объекта на число
}

export const bookAPI = {
  // Получение детальной информации о книге
  getBook: async (bookId: number): Promise<BookDetail> => {
    try {
      const response = await axios.get<ApiResponse<BookDetail>>(`${OAPI_BASE_URL}/v1/books`, {
        params: { bookId }
      });

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось загрузить информацию о книге');
    } catch (error: any) {
      console.error('Error fetching book:', error);
      throw error;
    }
  },

  // Получение отзывов о книге
  getBookReviews: async (
    bookId: number,
    batch: number = 10,
    page: number = 0
  ): Promise<ReviewsResponse> => {
    try {
      const response = await axios.get<ApiResponse<ReviewsResponse>>(
        `${OAPI_BASE_URL}/v1/books/reviews`,
        {
          params: { bookId, batch, page }
        }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось загрузить отзывы');
    } catch (error: any) {
      console.error('Error fetching book reviews:', error);
      throw error;
    }
  },

  // Обновление информации о книге
  updateBook: async (bookId: number, data: UpdateBookData): Promise<BookDetail> => {
    try {
      const response = await axios.put<ApiResponse<BookDetail>>(
        `${OAPI_BASE_URL}/v1/book?bookId=${bookId}`,
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

      throw new Error(response.data.data.message || 'Не удалось обновить книгу');
    } catch (error: any) {
      console.error('Error updating book:', error);
      console.error('Request data:', data); // Для отладки

      // Обработка специфичных ошибок
      if (error.response?.data?.data?.details) {
        const errorDetails = error.response.data.data.details;
        const errorMessage = errorDetails.message || 'Ошибка при обновлении книги';
        const errorWithDetails = new Error(errorMessage);
        (errorWithDetails as any).response = error.response;
        throw errorWithDetails;
      }

      throw error;
    }
  },

  // Загрузка изображения для книги
  uploadBookImage: async (file: File): Promise<any> => {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await axios.post(`${OAPI_BASE_URL}/v1/images/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data;
    } catch (error: any) {
      console.error('Error uploading book image:', error);
      throw error;
    }
  },

  // Удаление книги
  deleteBook: async (bookId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/book?bookId=${bookId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить книгу');
      }
    } catch (error: any) {
      console.error('Error deleting book:', error);
      throw error;
    }
  },

  createReview: async (bookId: number, score: number, reviewText: string): Promise<Review> => {
    try {
      const response = await axios.post<ReviewResponse>(
        `${OAPI_BASE_URL}/v1/book/reviews?bookId=${bookId}`,
        { reviewText, score },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state === 'OK' && response.data.data.key) {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось создать отзыв');
    } catch (error: any) {
      console.error('Error creating review:', error);
      throw error;
    }
  },

  updateReview: async (bookId: number, score: number, reviewText: string): Promise<Review> => {
    try {
      const response = await axios.put<ReviewResponse>(
        `${OAPI_BASE_URL}/v1/book/reviews?bookId=${bookId}`,
        { reviewText, score },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state === 'OK' && response.data.data.key) {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось обновить отзыв');
    } catch (error: any) {
      console.error('Error updating review:', error);
      throw error;
    }
  },

  deleteReview: async (bookId: number): Promise<void> => {
    try {
      const response = await axios.delete<ReviewResponse>(
        `${OAPI_BASE_URL}/v1/book/reviews?bookId=${bookId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить отзыв');
      }
    } catch (error: any) {
      console.error('Error deleting review:', error);
      throw error;
    }
  },

  getMyReview: async (bookId: number): Promise<Review | null> => {
    try {
      const response = await axios.get<ReviewResponse>(
        `${OAPI_BASE_URL}/v1/book/reviews/my?bookId=${bookId}`
      );

      if (response.data.data.state === 'OK' && response.data.data.key) {
        return response.data.data.key;
      }

      return null;
    } catch (error: any) {
      // Если отзыв не найден (404), возвращаем null
      if (error.response?.status === 404) {
        return null;
      }
      console.error('Error fetching my review:', error);
      throw error;
    }
  },
  createBook: async (data: {
    title: string;
    subtitle?: string | null;
    description?: string | null;
    pageCnt: number;
    isbn?: string | null;
    addedBy: number;
    authorIds: number[];
    genreIds: number[];
    photoLink?: number | null;
  }): Promise<BookDetail> => {
    try {
      const response = await axios.post<ApiResponse<BookDetail>>(
        `${OAPI_BASE_URL}/v1/book`,
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

      throw new Error(response.data.data.message || 'Не удалось создать книгу');
    } catch (error: any) {
      console.error('Error creating book:', error);
      console.error('Request data:', data);

      if (error.response?.data?.data?.details) {
        const errorDetails = error.response.data.data.details;
        const errorMessage = errorDetails.message || 'Ошибка при создании книги';
        const errorWithDetails = new Error(errorMessage);
        (errorWithDetails as any).response = error.response;
        throw errorWithDetails;
      }

      throw error;
    }
  },
};

export default bookAPI; 