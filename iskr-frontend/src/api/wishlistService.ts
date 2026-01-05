import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

export interface WishlistInfo {
  likesCount: number;
  wishlistTitle: string;
  hasWishlist: boolean;
  confidentiality: string;
  userId: number;
  wishlistId: number;
  booksCount: number;
}

export interface BookInWishlistStatus {
  wishlistTitle: string;
  hasWishlist: boolean;
  confidentiality: string;
  wishlistBooksCount: number;
  existsInWishlist: boolean;
  userId: number;
  wishlistId: number;
  wishlistLikesCount: number;
  bookId: number;
}

export interface WishlistResponse {
  added: boolean;
  userId: number;
  wishlistId: number;
  bookId: number;
}

export interface ClearWishlistResponse {
  booksRemoved: number;
  userId: number;
  wishlistId: number;
  cleared: boolean;
}


export const wishlistService = {
  // Проверка наличия вишлиста у пользователя
  checkWishlist: async (): Promise<WishlistInfo> => {
    const response = await axios.get<ApiResponse<WishlistInfo>>(
      `${OAPI_BASE_URL}/v1/wishlist`
    );
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    throw new Error(response.data.data.message || 'Не удалось проверить вишлист');
  },

  // Проверка, есть ли книга в вишлисте
  checkBookInWishlist: async (bookId: number): Promise<boolean> => {
    const response = await axios.get<ApiResponse<BookInWishlistStatus>>(
      `${OAPI_BASE_URL}/v1/wishlist/is-book-in`,
      { params: { bookId } }
    );
    if (response.data.data.state === 'OK') {
      return response.data.data.key.existsInWishlist;
    }
    throw new Error(response.data.data.message || 'Не удалось проверить наличие книги в вишлисте');
  },

  // Добавление книги в вишлист
  addBookToWishlist: async (bookId: number): Promise<WishlistResponse> => {
    const response = await axios.post<ApiResponse<WishlistResponse>>(
      `${OAPI_BASE_URL}/v1/wishlist`,
      { bookId },
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    throw new Error(response.data.data.message || 'Не удалось добавить книгу в вишлист');
  },

  clearWishlist: async (): Promise<ClearWishlistResponse> => {
    const response = await axios.delete<ApiResponse<ClearWishlistResponse>>(
      `${OAPI_BASE_URL}/v1/wishlist/clear`
    );
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    throw new Error(response.data.data.message || 'Не удалось очистить вишлист');
  },

  // Удаление книги из вишлиста
  removeBookFromWishlist: async (bookId: number): Promise<WishlistResponse> => {
    const response = await axios.delete<ApiResponse<WishlistResponse>>(
      `${OAPI_BASE_URL}/v1/wishlist`,
      { params: { bookId } }
    );
    if (response.data.data.state === 'OK') {
      return response.data.data.key;
    }
    throw new Error(response.data.data.message || 'Не удалось удалить книгу из вишлиста');
  },
};

export default wishlistService;