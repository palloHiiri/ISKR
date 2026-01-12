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

export interface CreateCollectionData {
  title: string;
  description?: string;
  confidentiality: 'Public' | 'Private';
  collectionType?: string;
  photoLink?: number | null;
}

export interface UserCollectionsResponse {
  collections: CollectionInfo[];
  batch: number;
  totalPages: number;
  page: number;
  userId: number;
  totalElements: number;
}

export interface LikeStatus {
  isLiked: boolean;
  collectionId: number;
  userId: number;
}

export interface BookInCollectionStatus {
  isInCollection: boolean;
  collectionId: number;
  bookId: number;
}

export const collectionAPI = {
  getCollection: async (collectionId: number, useAuth: boolean = false): Promise<CollectionInfo> => {
    try {
      const endpoint = useAuth ? '/v1/collections/auth' : '/v1/collections';
      const response = await axios.get<ApiResponse<CollectionInfo>>(
        `${OAPI_BASE_URL}${endpoint}`,
        { params: { collectionId } }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось загрузить коллекцию');
    } catch (error: any) {
      console.error('Error fetching collection:', error);

      if (error.response) {
        error.message = error.response.data?.data?.message || error.message;
      }

      throw error;
    }
  },

  getCollectionAdmin: async (collectionId: number): Promise<CollectionInfo> => {
    try {
      const response = await axios.get<ApiResponse<CollectionInfo>>(
        `${OAPI_BASE_URL}/v1/collections/admin`,
        { params: { collectionId } }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось загрузить коллекцию (админ)');
    } catch (error: any) {
      console.error('Error fetching collection (admin):', error);

      if (error.response) {
        error.message = error.response.data?.data?.message || error.message;
      }

      throw error;
    }
  },

  getCollectionBooks: async (
    collectionId: number,
    batch: number = 10,
    page: number = 0,
    useAuth: boolean = false
  ): Promise<CollectionBooksResponse> => {
    try {
      const endpoint = useAuth ? '/v1/collections/books/auth' : '/v1/collections/books';
      const response = await axios.get<ApiResponse<CollectionBooksResponse>>(
        `${OAPI_BASE_URL}${endpoint}`,
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

      if (error.response) {
        error.message = error.response.data?.data?.message || error.message;
      }

      throw error;
    }
  },

  getCollectionBooksAdmin: async (
    collectionId: number,
    batch: number = 10,
    page: number = 0
  ): Promise<CollectionBooksResponse> => {
    try {
      const response = await axios.get<ApiResponse<CollectionBooksResponse>>(
        `${OAPI_BASE_URL}/v1/collections/books/admin`,
        {
          params: { collectionId, batch, page }
        }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось загрузить книги коллекции (админ)');
    } catch (error: any) {
      console.error('Error fetching collection books (admin):', error);

      if (error.response) {
        error.message = error.response.data?.data?.message || error.message;
      }

      throw error;
    }
  },

  getUserCollections: async (): Promise<UserCollectionsResponse> => {
    try {
      const response = await axios.get<ApiResponse<UserCollectionsResponse>>(
        `${OAPI_BASE_URL}/v1/collection`
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось загрузить коллекции пользователя');
    } catch (error: any) {
      console.error('Error fetching user collections:', error);
      throw error;
    }
  },

  createCollection: async (data: CreateCollectionData): Promise<CollectionInfo> => {
    try {
      const response = await axios.post<ApiResponse<CollectionInfo>>(
        `${OAPI_BASE_URL}/v1/collection`,
        {
          title: data.title,
          description: data.description || '',
          confidentiality: data.confidentiality,
          collectionType: data.collectionType || 'Standard',
          photoLink: data.photoLink || null
        },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось создать коллекцию');
    } catch (error: any) {
      console.error('Error creating collection:', error);

      if (error.response?.data?.data?.details) {
        const errorDetails = error.response.data.data.details;
        const errorMessage = errorDetails.message || 'Ошибка при создании коллекции';
        const errorWithDetails = new Error(errorMessage);
        (errorWithDetails as any).response = error.response;
        throw errorWithDetails;
      }

      throw error;
    }
  },

  updateCollection: async (collectionId: number, data: Partial<CreateCollectionData>): Promise<CollectionInfo> => {
    try {
      const response = await axios.put<ApiResponse<CollectionInfo>>(
        `${OAPI_BASE_URL}/v1/collection?collectionId=${collectionId}`,
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

      throw new Error(response.data.data.message || 'Не удалось обновить коллекцию');
    } catch (error: any) {
      console.error('Error updating collection:', error);
      throw error;
    }
  },

  updateCollectionAdmin: async (collectionId: number, data: Partial<CreateCollectionData>): Promise<CollectionInfo> => {
    try {
      const response = await axios.put<ApiResponse<CollectionInfo>>(
        `${OAPI_BASE_URL}/v1/collection/admin?collectionId=${collectionId}`,
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

      throw new Error(response.data.data.message || 'Не удалось обновить коллекцию (админ)');
    } catch (error: any) {
      console.error('Error updating collection (admin):', error);
      throw error;
    }
  },

  deleteCollection: async (collectionId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection?collectionId=${collectionId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить коллекцию');
      }
    } catch (error: any) {
      console.error('Error deleting collection:', error);
      throw error;
    }
  },

  deleteCollectionAdmin: async (collectionId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/admin?collectionId=${collectionId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить коллекцию (админ)');
      }
    } catch (error: any) {
      console.error('Error deleting collection (admin):', error);
      throw error;
    }
  },

  addBookToCollection: async (collectionId: number, bookId: number): Promise<void> => {
    try {
      const response = await axios.post<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/book?collectionId=${collectionId}`,
        { bookId },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось добавить книгу в коллекцию');
      }
    } catch (error: any) {
      console.error('Error adding book to collection:', error);
      throw error;
    }
  },

  addBookToCollectionAdmin: async (collectionId: number, bookId: number): Promise<void> => {
    try {
      const response = await axios.post<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/book/admin?collectionId=${collectionId}`,
        { bookId },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось добавить книгу в коллекцию (админ)');
      }
    } catch (error: any) {
      console.error('Error adding book to collection (admin):', error);
      throw error;
    }
  },

  removeBookFromCollection: async (collectionId: number, bookId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/book?collectionId=${collectionId}&bookId=${bookId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить книгу из коллекции');
      }
    } catch (error: any) {
      console.error('Error removing book from collection:', error);
      throw error;
    }
  },

  removeBookFromCollectionAdmin: async (collectionId: number, bookId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/book/admin?collectionId=${collectionId}&bookId=${bookId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить книгу из коллекции (админ)');
      }
    } catch (error: any) {
      console.error('Error removing book from collection (admin):', error);
      throw error;
    }
  },

  checkBookInCollection: async (collectionId: number, bookId: number): Promise<BookInCollectionStatus> => {
    try {
      const response = await axios.get<ApiResponse<any>>(
        `${OAPI_BASE_URL}/v1/collection/book`,
        {
          params: { collectionId, bookId }
        }
      );

      if (response.data.data.state === 'OK') {
        const key = response.data.data.key;
        return {
          isInCollection: key.exists || false,
          collectionId: key.collectionId || collectionId,
          bookId: key.bookId || bookId
        };
      }

      throw new Error(response.data.data.message || 'Не удалось проверить наличие книги в коллекции');
    } catch (error: any) {
      console.error('Error checking book in collection:', error);
      throw error;
    }
  },

  getLikeStatus: async (collectionId: number): Promise<LikeStatus> => {
    try {
      const response = await axios.get<ApiResponse<LikeStatus>>(
        `${OAPI_BASE_URL}/v1/collection/like?collectionId=${collectionId}`
      );

      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }

      throw new Error(response.data.data.message || 'Не удалось получить статус лайка');
    } catch (error: any) {
      console.error('Error fetching like status:', error);
      throw error;
    }
  },

  likeCollection: async (collectionId: number): Promise<void> => {
    try {
      const response = await axios.post<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/like?collectionId=${collectionId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось поставить лайк');
      }
    } catch (error: any) {
      console.error('Error liking collection:', error);
      throw error;
    }
  },

  unlikeCollection: async (collectionId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/like?collectionId=${collectionId}`
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось убрать лайк');
      }
    } catch (error: any) {
      console.error('Error unliking collection:', error);
      throw error;
    }
  },
  checkBookInCollectionAdmin: async (collectionId: number, bookId: number): Promise<BookInCollectionStatus> => {
    try {
      const response = await axios.get<ApiResponse<any>>(
        `${OAPI_BASE_URL}/v1/collection/book/admin`,
        {
          params: { collectionId, bookId }
        }
      );

      if (response.data.data.state === 'OK') {
        const key = response.data.data.key;
        return {
          isInCollection: key.exists || false,
          collectionId: key.collectionId || collectionId,
          bookId: key.bookId || bookId
        };
      }

      throw new Error(response.data.data.message || 'Не удалось проверить наличие книги в коллекции (админ)');
    } catch (error: any) {
      console.error('Error checking book in collection (admin):', error);
      throw error;
    }
  },

  addCollectionPrivilegeAdmin: async (collectionId: number, data: AddCVPData): Promise<void> => {
    try {
      const response = await axios.post<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/privilege/admin`,
        data,
        {
          params: { collectionId },
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось добавить привилегию (админ)');
      }
    } catch (error: any) {
      console.error('Error adding collection privilege (admin):', error);
      throw error;
    }
  },

  removeCollectionPrivilegeAdmin: async (collectionId: number, userId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/privilege/admin`,
        {
          params: { collectionId, userId }
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить привилегию (админ)');
      }
    } catch (error: any) {
      console.error('Error removing collection privilege (admin):', error);
      throw error;
    }
  },
};

export default collectionAPI;