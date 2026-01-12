import axios from 'axios';
import { 
  OAPI_BASE_URL, 
  IMAGES_BASE_URL, 
  API_ENDPOINTS 
} from '../constants/api';
import type { 
  Book, 
  Collection, 
  User, 
  PhotoLink, 
  ImageData 
} from '../types/popular';

const api = axios.create({
  baseURL: OAPI_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const getImageUrl = (photoLink: PhotoLink | null | undefined): string | null => {
  if (!photoLink?.imageData?.uuid || !photoLink?.imageData?.extension) {
    return null;
  }
  return `${IMAGES_BASE_URL}/${photoLink.imageData.uuid}.${photoLink.imageData.extension}`;
};

export const getSearchImageUrl = (imageUuid?: string, imageExtension?: string): string | null => {
  if (!imageUuid || !imageExtension) {
    return null;
  }
  return `${IMAGES_BASE_URL}/${imageUuid}.${imageExtension}`;
};

export const getBookImageUrl = (book: any): string | null => {
  if (book.photoLink && book.photoLink.imageData) {
    return getImageUrl(book.photoLink);
  }
  
  if (book.imageUuid && book.imageExtension) {
    return getSearchImageUrl(book.imageUuid, book.imageExtension);
  }
  
  return null;
};

export const getUserImageUrl = (user: any): string | null => {
  if (user.profileImage) {
    return getImageUrl(user.profileImage);
  }
  if (user.profileImage) {
    return getImageUrl(user.profileImage);
  }
  return null;
};

export const getCollectionImageUrl = (collection: Collection): string | null => {
  if (collection.photoLink) {
    return getImageUrl(collection.photoLink);
  }
  return null;
};

export const formatRating = (rating: number | null): number => {
  if (rating === null || rating === undefined) {
    return 0;
  }
  const converted = rating / 2;
  return Math.round(converted * 10) / 10;
};

export const popularAPI = {
  getPopularUsers: async (limit: number = 12): Promise<User[]> => {
    try {
      const response = await api.get(API_ENDPOINTS.POPULAR_USERS, {
        params: { limit }
      });
      
      if (response.data.data.state === 'OK' && response.data.data.key.users) {
        return response.data.data.key.users;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch popular users');
    } catch (error) {
      console.error('Error fetching popular users:', error);
      throw error;
    }
  },

  getPopularBooks: async (limit: number = 12): Promise<Book[]> => {
    try {
      const response = await api.get(API_ENDPOINTS.POPULAR_BOOKS, {
        params: { limit }
      });
      
      if (response.data.data.state === 'OK' && response.data.data.key.books) {
        return response.data.data.key.books;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch popular books');
    } catch (error) {
      console.error('Error fetching popular books:', error);
      return [];
    }
  },

  getPopularCollections: async (limit: number = 12): Promise<Collection[]> => {
    try {
      const response = await api.get(API_ENDPOINTS.POPULAR_COLLECTIONS, {
        params: { limit }
      });
      
      if (response.data.data.state === 'OK' && response.data.data.key.collections) {
        return response.data.data.key.collections;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch popular collections');
    } catch (error) {
      console.error('Error fetching popular collections:', error);
      return [];
    }
  },
};


export default popularAPI;