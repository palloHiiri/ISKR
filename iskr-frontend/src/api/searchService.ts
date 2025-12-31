import axios from 'axios';
import { OAPI_BASE_URL, IMAGES_BASE_URL } from '../constants/api';
import type { SearchParams, SearchResponse, SearchResultItem, SearchBookData, SearchCollectionData, SearchUserData } from '../types/search';
import type { Book, Collection, User, PhotoLink, ImageData } from '../types/popular';

// Функция для преобразования объекта в URLSearchParams
const toFormUrlEncoded = (data: Record<string, any>): URLSearchParams => {
  const params = new URLSearchParams();
  Object.keys(data).forEach(key => {
    if (data[key] !== undefined && data[key] !== null) {
      params.append(key, data[key]);
    }
  });
  return params;
};

// Создаем инстанс axios для поиска
const searchApi = axios.create({
  baseURL: OAPI_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
});

// Функция для получения URL изображения из поисковых данных
export const getSearchImageUrl = (imageUuid?: string, imageExtension?: string): string | null => {
  if (!imageUuid || !imageExtension) {
    return null;
  }
  return `${IMAGES_BASE_URL}/${imageUuid}.${imageExtension}`;
};

// Функция для создания объекта PhotoLink из данных поиска
const createPhotoLinkFromSearch = (imageUuid?: string, imageExtension?: string): PhotoLink | null => {
  if (!imageUuid || !imageExtension) {
    return null;
  }
  
  return {
    imglId: 0, // Неизвестно из поиска
    imageData: {
      imgdId: 0, // Неизвестно из поиска
      uuid: imageUuid,
      size: 0, // Неизвестно из поиска
      mimeType: `image/${imageExtension === 'jpg' ? 'jpeg' : imageExtension}`,
      extension: imageExtension
    } as ImageData
  };
};

// Type guards для проверки типа данных
const isBookData = (data: any): data is SearchBookData => {
  return data && typeof data === 'object' && 'title' in data && 'authorIds' in data;
};

const isCollectionData = (data: any): data is SearchCollectionData => {
  return data && typeof data === 'object' && 'title' in data && 'bookCount' in data;
};

const isUserData = (data: any): data is SearchUserData => {
  return data && typeof data === 'object' && 'username' in data && 'email' in data;
};

export const searchAPI = {
  // Основной поиск (POST метод с form-urlencoded)
  search: async (params: SearchParams) => {
    const formData = toFormUrlEncoded(params);
    const response = await searchApi.post<SearchResponse>('/v1/search/query', formData);
    
    const items = response.data.data.items || [];
    const total = response.data.data.total || 0;
    const limit = params.Limit || 10;
    
    // Разделяем результаты по типам
    const books: Book[] = [];
    const users: User[] = [];
    const collections: Collection[] = [];
    
    items.forEach((item: SearchResultItem) => {
      if (item.type === 'book' && isBookData(item.data)) {
        const bookData = item.data as SearchBookData;
        const photoLink = createPhotoLinkFromSearch(bookData.imageUuid, bookData.imageExtension);
        
        books.push({
          bookId: bookData.id,
          title: bookData.title || '',
          subtitle: bookData.subtitle || null,
          isbn: bookData.isbn || '',
          pageCnt: bookData.pageCnt || 0,
          collectionsCount: bookData.collectionsCount || 0,
          averageRating: bookData.averageRating || 0,
          photoLink: photoLink,
          description: bookData.description || '',
          authors: bookData.authors || [],
          genres: bookData.genres || [],
          imageUuid: bookData.imageUuid,
          imageExtension: bookData.imageExtension,
        } as Book);
      } 
      else if (item.type === 'user' && isUserData(item.data)) {
        const userData = item.data as SearchUserData;
        const photoLink = createPhotoLinkFromSearch(userData.imageUuid, userData.imageExtension);
        
        users.push({
          userId: userData.id || 0,
          username: userData.username || '',
          nickname: userData.nickname || '',
          email: userData.email || '',
          status: 'ACTIVE', // По умолчанию
          subscribersCount: userData.subscribersCount || 0,
          profileImage: photoLink
        } as User);
      } 
      else if (item.type === 'collection' && isCollectionData(item.data)) {
        const collectionData = item.data as SearchCollectionData;
        const photoLink = createPhotoLinkFromSearch(collectionData.imageUuid, collectionData.imageExtension);
        
        collections.push({
          collectionId: collectionData.id || 0,
          title: collectionData.title || '',
          description: collectionData.description || '',
          collectionType: collectionData.confidentiality || 'Standard',
          ownerId: collectionData.ownerId || 0,
          ownerNickname: '', // Нет в поиске
          likesCount: collectionData.likesCount || 0,
          bookCount: collectionData.bookCount || 0,
          photoLink: photoLink
        } as Collection);
      }
    });
    
    return {
      books,
      users,
      collections,
      total,
      hasMore: total > items.length
    };
  },
};

export default searchAPI;