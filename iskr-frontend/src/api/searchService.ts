import axios from 'axios';
import { OAPI_BASE_URL, IMAGES_BASE_URL } from '../constants/api';
import type { SearchParams, SearchResponse, SearchResultItem, SearchBookData, SearchCollectionData, SearchUserData, SearchGenreData, SearchAuthorData } from '../types/search';
import type { Book, Collection, User, PhotoLink, ImageData } from '../types/popular';

const toFormUrlEncoded = (data: Record<string, any>): URLSearchParams => {
  const params = new URLSearchParams();
  Object.keys(data).forEach(key => {
    if (data[key] !== undefined && data[key] !== null) {
      params.append(key, data[key]);
    }
  });
  return params;
};

const searchApi = axios.create({
  baseURL: OAPI_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
});

export const getSearchImageUrl = (imageUuid?: string, imageExtension?: string): string | null => {
  if (!imageUuid || !imageExtension) {
    return null;
  }
  return `${IMAGES_BASE_URL}/${imageUuid}.${imageExtension}`;
};

const createPhotoLinkFromSearch = (imageUuid?: string, imageExtension?: string): PhotoLink | null => {
  if (!imageUuid || !imageExtension) {
    return null;
  }

  return {
    imglId: 0,
    imageData: {
      imgdId: 0,
      uuid: imageUuid,
      size: 0,
      mimeType: `image/${imageExtension === 'jpg' ? 'jpeg' : imageExtension}`,
      extension: imageExtension
    } as ImageData
  };
};

const isBookData = (data: any): data is SearchBookData => {
  return data && typeof data === 'object' && 'title' in data && 'authorIds' in data;
};

const isCollectionData = (data: any): data is SearchCollectionData => {
  return data && typeof data === 'object' && 'title' in data && 'bookCount' in data;
};

const isUserData = (data: any): data is SearchUserData => {
  return data && typeof data === 'object' && 'username' in data && 'email' in data;
};

const isGenreData = (data: any): data is SearchGenreData => {
  return data && typeof data === 'object' && 'id' in data && 'name' in data;
};

const isAuthorData = (data: any): data is SearchAuthorData => {
  return data && typeof data === 'object' && 'id' in data && 'name' in data;
};

export const searchAPI = {
  search: async (params: SearchParams) => {
    const formData = toFormUrlEncoded(params);
    const response = await searchApi.post<SearchResponse>('/v1/search/query', formData);

    const items = response.data.data.items || [];
    const total = response.data.data.total || 0;
    const limit = params.Limit || 10;

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
          status: 'ACTIVE',
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
          ownerNickname: '',
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

  fetchGenres: async (limit: number = 50) => {
    try {
      const params = {
        Query: '',
        Types: 'genre',
        Limit: limit
      };

      const formData = toFormUrlEncoded(params);
      const response = await searchApi.post<SearchResponse>('/v1/search/query', formData);

      const items = response.data.data.items || [];
      const genres: Array<{ id: number, name: string }> = [];

      items.forEach((item: SearchResultItem) => {
        if (item.type === 'genre' && isGenreData(item.data)) {
          const genreData = item.data as SearchGenreData;
          genres.push({
            id: genreData.id,
            name: genreData.name
          });
        }
      });

      return genres;
    } catch (error: any) {
      console.error('Error fetching genres:', error);
      return [];
    }
  },

  searchAuthors: async (query: string, limit: number = 10): Promise<Array<{ id: number, name: string, realName?: string }>> => {
    try {
      const params = {
        Query: query,
        Types: 'author',
        Limit: limit
      };

      const formData = toFormUrlEncoded(params);
      const response = await searchApi.post<SearchResponse>('/v1/search/query', formData);

      const items = response.data.data.items || [];
      const authors: Array<{ id: number, name: string, realName?: string }> = [];

      items.forEach((item: SearchResultItem) => {
        if (item.type === 'author' && isAuthorData(item.data)) {
          const authorData = item.data as SearchAuthorData;
          authors.push({
            id: authorData.id,
            name: authorData.name,
            realName: authorData.realName
          });
        }
      });

      return authors;
    } catch (error: any) {
      console.error('Error searching authors:', error);
      return [];
    }
  },

  searchGenres: async (query: string, limit: number = 10): Promise<Array<{ id: number, name: string }>> => {
    try {
      const params = {
        Query: query,
        Types: 'genre',
        Limit: limit
      };

      const formData = toFormUrlEncoded(params);
      const response = await searchApi.post<SearchResponse>('/v1/search/query', formData);

      const items = response.data.data.items || [];
      const genres: Array<{ id: number, name: string }> = [];

      items.forEach((item: SearchResultItem) => {
        if (item.type === 'genre' && isGenreData(item.data)) {
          const genreData = item.data as SearchGenreData;
          genres.push({
            id: genreData.id,
            name: genreData.name
          });
        }
      });

      return genres;
    } catch (error: any) {
      console.error('Error searching genres:', error);
      return [];
    }
  },
};

export default searchAPI;