// /src/api/cvpService.ts
import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ApiResponse } from '../types/popular';
import searchAPI from './searchService';

export interface CVPStatus {
  cvpId: number;
  collectionId: number;
  userId: number;
  username: string;
  nickname: string;
  status: 'Allowed' | 'Disallowed';
  user?: {
    userId: number;
    username: string;
    nickname: string;
    email: string;
  };
}

export interface PrivilegesResponse {
  totalPrivileges: number;
  privileges: CVPStatus[];
  collectionId: number;
}

export interface AddCVPData {
  cvpStatus: 'Allowed' | 'Disallowed';
  userId: number;
}

export interface UserSearchResult {
  id: number;
  username: string;
  nickname: string;
  email: string;
  subscribersCount: number;
  imageUuid?: string;
  imageExtension?: string;
}

export const cvpAPI = {
  // Получить все CVP для коллекции (обычный пользователь)
  getCollectionPrivileges: async (collectionId: number): Promise<CVPStatus[]> => {
    try {
      const response = await axios.get<ApiResponse<PrivilegesResponse>>(
        `${OAPI_BASE_URL}/v1/collection/privilege`,
        { params: { collectionId } }
      );
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key?.privileges || [];
      }
      
      throw new Error(response.data.data.message || 'Не удалось загрузить привилегии доступа');
    } catch (error: any) {
      console.error('Error fetching collection privileges:', error);
      
      if (error.response?.status === 404) {
        return [];
      }
      
      return [];
    }
  },

  // Получить все CVP для коллекции (администратор)
  getCollectionPrivilegesAdmin: async (collectionId: number): Promise<CVPStatus[]> => {
    try {
      const response = await axios.get<ApiResponse<PrivilegesResponse>>(
        `${OAPI_BASE_URL}/v1/collection/privilege/admin`,
        { params: { collectionId } }
      );
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key?.privileges || [];
      }
      
      throw new Error(response.data.data.message || 'Не удалось загрузить привилегии доступа (админ)');
    } catch (error: any) {
      console.error('Error fetching collection privileges (admin):', error);
      
      if (error.response?.status === 404) {
        return [];
      }
      
      return [];
    }
  },

  // Добавить CVP (обычный пользователь)
  addCollectionPrivilege: async (collectionId: number, data: AddCVPData): Promise<void> => {
    try {
      const response = await axios.post<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/privilege`,
        data,
        {
          params: { collectionId },
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось добавить привилегию доступа');
      }
    } catch (error: any) {
      console.error('Error adding collection privilege:', error);
      throw error;
    }
  },

  // Добавить CVP (администратор)
  addCollectionPrivilegeAdmin: async (collectionId: number, data: AddCVPData): Promise<void> => {
    try {
      const response = await axios.post<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/privilege/admin`,
        data,
        {
          params: { collectionId },
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось добавить привилегию доступа (админ)');
      }
    } catch (error: any) {
      console.error('Error adding collection privilege (admin):', error);
      throw error;
    }
  },

  // Удалить CVP (обычный пользователь)
  removeCollectionPrivilege: async (collectionId: number, userId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/privilege`,
        {
          params: { collectionId, userId }
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить привилегию доступа');
      }
    } catch (error: any) {
      console.error('Error removing collection privilege:', error);
      throw error;
    }
  },

  // Удалить CVP (администратор)
  removeCollectionPrivilegeAdmin: async (collectionId: number, userId: number): Promise<void> => {
    try {
      const response = await axios.delete<ApiResponse<void>>(
        `${OAPI_BASE_URL}/v1/collection/privilege/admin`,
        {
          params: { collectionId, userId }
        }
      );

      if (response.data.data.state !== 'OK') {
        throw new Error(response.data.data.message || 'Не удалось удалить привилегию доступа (админ)');
      }
    } catch (error: any) {
      console.error('Error removing collection privilege (admin):', error);
      throw error;
    }
  },

  // Поиск пользователей для добавления CVP
  searchUsers: async (query: string, limit: number = 10): Promise<UserSearchResult[]> => {
    try {
      const searchParams = {
        Query: query,
        Types: 'user',
        Limit: limit
      };

      const response = await searchAPI.search(searchParams);
      
      return response.users?.map(user => ({
        id: user.userId,
        username: user.username,
        nickname: user.nickname || user.username,
        email: user.email,
        subscribersCount: user.subscribersCount || 0,
        imageUuid: user.profileImage?.imageData?.uuid,
        imageExtension: user.profileImage?.imageData?.extension
      })) || [];
    } catch (error: any) {
      console.error('Error searching users:', error);
      return [];
    }
  },
};

export default cvpAPI;