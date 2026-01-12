export interface ImageData {
  imgdId: number;
  uuid: string;
  size: number;
  mimeType: string;
  extension: string;
}

export interface PhotoLink {
  imglId: number;
  imageData: ImageData;
}

export interface User {
  userId: number;
  username: string;
  nickname: string;
  email: string;
  status: string;
  subscribersCount: number;
  profileImage: PhotoLink | null;
}

export interface Book {
  bookId: number;
  title: string;
  subtitle: string | null;
  isbn: string;
  pageCnt: number;
  collectionsCount: number;
  averageRating: number | null;
  photoLink: PhotoLink | null;
  description?: string;
  authors?: string[]; 
  genres?: string[]; 
  imageUuid?: string; 
  imageExtension?: string; 
}

export interface Collection {
  collectionId: number;
  title: string;
  description: string;
  collectionType: string;
  ownerId: number;
  ownerNickname: string;
  likesCount: number;
  bookCount: number;
  photoLink: PhotoLink | null;
}

export interface PopularResponse<T> {
  data: {
    state: string;
    message: string;
    key: {
      limit: number;
      count: number;
      books?: T[];
      collections?: T[];
      users?: T[];
    };
  };
  meta: {
    timestamp: string;
    processedBy: string;
    userId: string;
  };
}

export interface ApiResponse<T> {
  data: {
    state: string;
    message: string;
    key: T;
  };
  meta: {
    timestamp: string;
    processedBy: string;
    userId: string;
  };
}