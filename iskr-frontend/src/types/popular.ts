// Тип для изображения
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

// Тип для пользователя (остается без изменений)
export interface User {
  userId: number;
  username: string;
  nickname: string;
  email: string;
  status: string;
  subscribersCount: number;
  profileImage: PhotoLink | null;
}

// Тип для книги (обновлен по реальным данным API)
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
  authors?: string[]; // Добавляем авторов
  genres?: string[]; // Добавляем жанры
  imageUuid?: string; // Для поиска
  imageExtension?: string; // Для поиска
}

// Тип для коллекции (обновлен по реальным данным API)
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

// Общие типы ответов API
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

// Общий тип ответа API
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