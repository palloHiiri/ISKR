export interface SearchParams {
  Query: string;
  Types?: string; 
  Limit?: number;
  Genre?: number; 
}

export interface SearchBookData {
  id: number;
  title: string;
  subtitle?: string;
  isbn: string;
  pageCnt: number;
  addedBy: number;
  description: string;
  genreIds: number[];
  authorIds: number[];
  collectionsCount: number;
  genres: string[];
  averageRating: number;
  authors: string[];
  imageUuid?: string;
  imageExtension?: string;
}

export interface SearchCollectionData {
  id: number;
  title: string;
  description: string;
  confidentiality: string;
  likesCount: number;
  bookCount: number;
  ownerId: number;
  imageUuid?: string;
  imageExtension?: string;
}

export interface SearchUserData {
  id: number;
  username: string;
  nickname: string;
  email: string;
  subscribersCount: number;
  imageUuid?: string;
  imageExtension?: string;
}

export interface SearchGenreData {
  id: number;
  name: string;
}

export interface SearchResponse {
  data: {
    query: string;
    total: number;
    limit: number;
    items: SearchResultItem[];
    took: number;
  };
  meta: {
    timestamp: string;
    processedBy: string;
    userId: string;
  };
}

export interface SearchAuthorData {
  id: number;
  name: string;
  realName?: string;
  description?: string;
}

export interface SearchGenreData {
  id: number;
  name: string;
}

export interface SearchResultItem {
  id: string;
  type: 'book' | 'user' | 'collection' | 'author' | 'genre';
  score: number;
  data: SearchBookData | SearchCollectionData | SearchUserData | SearchAuthorData | SearchGenreData;
  highlights: any;
}