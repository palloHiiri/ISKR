import type { PhotoLink, User, Collection as BaseCollection } from './popular';

export interface ProfileUser extends User {
  registeredDate?: string;
  profileDescription?: string | null;
  birthDate?: string | null;
  emailVerified?: boolean;
  subscriptionsCount?: number;
  collectionsCount?: number;
}

export interface ProfileCollection {
  collectionId: number;
  title: string;
  description: string;
  confidentiality: string;
  collectionType: string;
  photoLink: PhotoLink | null;
  bookCount: number;
}

export interface UserSubscription {
  userId: number;
  username: string;
  nickname: string;
  profileImage: PhotoLink | null;
  subscribersCount: number;
}

export interface UserSubscriber {
  userId: number;
  username: string;
  nickname: string;
  profileImage: PhotoLink | null;
  subscribersCount: number;
}

export interface PaginatedResponse<T> {
  items: T[];
  batch: number;
  totalPages: number;
  page: number;
  totalElements: number;
}