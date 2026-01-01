// /src/api/bookService.ts
import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

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
};

export default bookAPI;