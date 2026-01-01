// /src/api/collectionService.ts
import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

export interface CollectionInfo {
  collectionId: number;
  title: string;
  description: string;
  confidentiality: string;
  collectionType: string;
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
  ownerId: number;
  ownerNickname: string;
  booksCount: number;
  likesCount: number;
  canView: boolean;
}

export interface CollectionBook {
  bookId: number;
  title: string;
  subtitle: string | null;
  isbn: string;
  pageCnt: number;
  description: string;
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
  averageRating: number | null;
  authors: Array<{
    authorId: number;
    name: string;
    realName: string;
  }>;
  genres: Array<{
    genreId: number;
    name: string;
  }>;
}

export interface CollectionBooksResponse {
  canView: boolean;
  books: CollectionBook[];
  batch: number;
  totalPages: number;
  page: number;
  collectionId: number;
  totalElements: number;
}

export const collectionAPI = {
  // Получение информации о коллекции
  getCollection: async (collectionId: number): Promise<CollectionInfo> => {
    try {
      const response = await axios.get<ApiResponse<CollectionInfo>>(`${OAPI_BASE_URL}/v1/collections`, {
        params: { collectionId }
      });
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }
      
      throw new Error(response.data.data.message || 'Не удалось загрузить коллекцию');
    } catch (error: any) {
      console.error('Error fetching collection:', error);
      
      // Пробрасываем ошибку с информацией о статусе
      if (error.response) {
        error.message = error.response.data?.data?.message || error.message;
      }
      
      throw error;
    }
  },

  // Получение книг коллекции
  getCollectionBooks: async (
    collectionId: number, 
    batch: number = 10, 
    page: number = 0
  ): Promise<CollectionBooksResponse> => {
    try {
      const response = await axios.get<ApiResponse<CollectionBooksResponse>>(
        `${OAPI_BASE_URL}/v1/collections/books`,
        {
          params: { collectionId, batch, page }
        }
      );
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }
      
      throw new Error(response.data.data.message || 'Не удалось загрузить книги коллекции');
    } catch (error: any) {
      console.error('Error fetching collection books:', error);
      
      // Пробрасываем ошибку с информацией о статусе
      if (error.response) {
        error.message = error.response.data?.data?.message || error.message;
      }
      
      throw error;
    }
  },
};

export default collectionAPI;