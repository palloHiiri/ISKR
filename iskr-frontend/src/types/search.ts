export interface SearchParams {
  Query: string;
  Types?: string; // "book,user,collection"
  Limit?: number;
  Genre?: number; // ID жанра
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

export interface SearchResultItem {
  id: string;
  type: 'book' | 'user' | 'collection';
  score: number;
  data: SearchBookData | SearchCollectionData | SearchUserData;
  highlights: any;
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