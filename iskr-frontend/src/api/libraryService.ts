import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';

const api = axios.create({
  baseURL: OAPI_BASE_URL,
  timeout: 10000,
});

export interface LibraryBook {
  bookId: number;
  isbn: string | null;
  title: string;
  subtitle: string | null;
  description: string | null;
  pageCnt: number | null;
  addedBy: number;
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
  collectionsCount: number | null;
  authors: Array<{
    authorId: number;
    name: string;
    birthDate: string | null;
    description: string | null;
    realName: string | null;
  }>;
  genres: Array<{
    genreId: number;
    name: string;
  }>;
}

export interface LibraryCollection {
  bcolsId: number;
  ownerId: number;
  title: string;
  description: string;
  confidentiality: string;
  bookCollectionType: string;
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
  ownerNickname: string;
  bookCount: number;
}

export interface WishlistResponse {
  wishlistTitle: string;
  books: LibraryBook[];
  count: number;
  wishlistId: number;
}

export const libraryAPI = {
  getLibraryBooks: async (): Promise<LibraryBook[]> => {
    try {
      const response = await api.get<ApiResponse<{ books: LibraryBook[]; count: number }>>('/v1/library-books');
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key.books;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch library books');
    } catch (error) {
      console.error('Error fetching library books:', error);
      throw error;
    }
  },

  getLibraryCollections: async (): Promise<LibraryCollection[]> => {
    try {
      const response = await api.get<ApiResponse<{ collections: LibraryCollection[]; count: number }>>('/v1/library-collections');
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key.collections
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch library collections');
    } catch (error) {
      console.error('Error fetching library collections:', error);
      throw error;
    }
  },

  getWishlist: async (): Promise<{ books: LibraryBook[]; wishlistId?: number }> => {
    try {
      const response = await api.get<ApiResponse<WishlistResponse>>('/v1/library-wishlist');
      
      if (response.data.data.state === 'OK') {
        const key = response.data.data.key;
        
        if (key.count === 0 && response.data.data.message === 'No wishlist found for user') {
          return { books: [], wishlistId: undefined };
        }
        
        return { books: key.books, wishlistId: key.wishlistId };
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch wishlist');
    } catch (error) {
      console.error('Error fetching wishlist:', error);
      throw error;
    }
  },
};

export default libraryAPI;