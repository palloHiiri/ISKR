import axios from 'axios';
import { OAPI_BASE_URL } from '../constants/api';
import type { ProfileUser, ProfileCollection, UserSubscription, UserSubscriber, PaginatedResponse } from '../types/profile';
import type { ApiResponse } from '../types/popular';

// Создаем инстанс axios
const api = axios.create({
  baseURL: OAPI_BASE_URL,
  timeout: 10000,
});

export const profileAPI = {
  // Получение данных профиля пользователя
  getUserProfile: async (userId: number): Promise<ProfileUser> => {
    try {
      const response = await api.get<ApiResponse<ProfileUser>>(`/v1/user?userId=${userId}`);
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch user profile');
    } catch (error) {
      console.error('Error fetching user profile:', error);
      throw error;
    }
  },

  // Получение подписчиков пользователя (для Profile.tsx - возвращает массив)
  getUserSubscribers: async (
    userId: number, 
    batch: number = 4, 
    page: number = 0
  ): Promise<UserSubscriber[]> => {
    try {
      const response = await api.get<ApiResponse<{
        subscribers: UserSubscriber[];
        batch: number;
        totalPages: number;
        page: number;
        userId: number;
        totalElements: number;
      }>>(`/v1/user?userId=${userId}/subscribers&batch=${batch}&page=${page}`);
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key.subscribers;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch subscribers');
    } catch (error) {
      console.error('Error fetching subscribers:', error);
      return [];
    }
  },

  // Получение подписчиков пользователя с пагинацией (для Followers.tsx)
  getUserSubscribersPaginated: async (
    userId: number, 
    batch: number = 8, 
    page: number = 0
  ): Promise<PaginatedResponse<UserSubscriber>> => {
    try {
      const response = await api.get<ApiResponse<{
        subscribers: UserSubscriber[];
        batch: number;
        totalPages: number;
        page: number;
        userId: number;
        totalElements: number;
      }>>(`/v1/user?userId=${userId}/subscribers&batch=${batch}&page=${page}`);
      
      if (response.data.data.state === 'OK') {
        const { subscribers, batch: resBatch, totalPages, page: resPage, totalElements } = response.data.data.key;
        return {
          items: subscribers,
          batch: resBatch,
          totalPages,
          page: resPage,
          totalElements
        };
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch subscribers');
    } catch (error) {
      console.error('Error fetching subscribers:', error);
      return { items: [], batch, totalPages: 0, page: 0, totalElements: 0 };
    }
  },

  // Получение подписок пользователя (для Profile.tsx - возвращает массив)
  getUserSubscriptions: async (
    userId: number, 
    batch: number = 4, 
    page: number = 0
  ): Promise<UserSubscription[]> => {
    try {
      const response = await api.get<ApiResponse<{
        subscriptions: UserSubscription[];
        batch: number;
        totalPages: number;
        page: number;
        userId: number;
        totalElements: number;
      }>>(`/v1/user?userId=${userId}/subscriptions&batch=${batch}&page=${page}`);
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key.subscriptions;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch subscriptions');
    } catch (error) {
      console.error('Error fetching subscriptions:', error);
      return [];
    }
  },

  // Получение подписок пользователя с пагинацией (для Subscriptions.tsx)
  getUserSubscriptionsPaginated: async (
    userId: number, 
    batch: number = 8, 
    page: number = 0
  ): Promise<PaginatedResponse<UserSubscription>> => {
    try {
      const response = await api.get<ApiResponse<{
        subscriptions: UserSubscription[];
        batch: number;
        totalPages: number;
        page: number;
        userId: number;
        totalElements: number;
      }>>(`/v1/user?userId=${userId}/subscriptions&batch=${batch}&page=${page}`);
      
      if (response.data.data.state === 'OK') {
        const { subscriptions, batch: resBatch, totalPages, page: resPage, totalElements } = response.data.data.key;
        return {
          items: subscriptions,
          batch: resBatch,
          totalPages,
          page: resPage,
          totalElements
        };
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch subscriptions');
    } catch (error) {
      console.error('Error fetching subscriptions:', error);
      return { items: [], batch, totalPages: 0, page: 0, totalElements: 0 };
    }
  },

  // profileService.ts - добавить новую функцию
changeUsername: async (newUsername: string): Promise<ApiResponse<any>> => {
  try {
    const params = new URLSearchParams();
    params.append('New-Username', newUsername);

    const response = await api.put('/v1/accounts/username', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });

    return response.data;
  } catch (error: any) {
    console.error('Error changing username:', error);
    throw error;
  }
},
uploadImage: async (file: File): Promise<any> => {
  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post('/v1/images/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    // Возвращаем весь ответ, так как структура может быть разной
    return response.data;
  } catch (error) {
    console.error('Error uploading image:', error);
    throw error;
  }
},

// Изменение фото профиля
changeProfileImage: async (imageId: number): Promise<any> => {
  try {
    const response = await api.put(`/v1/accounts/image?New-Image-ID=${imageId}`);
    return response.data;
  } catch (error) {
    console.error('Error changing profile image:', error);
    throw error;
  }
},

changeProfileDescription: async (description: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('New-Description', description);

    const response = await api.put('/v1/accounts/description', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error changing profile description:', error);
    throw error;
  }
},

// Изменение никнейма
changeNickname: async (nickname: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('New-Nickname', nickname);

    const response = await api.put('/v1/accounts/nickname', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error changing nickname:', error);
    throw error;
  }
},

// Изменение email
changeEmail: async (email: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('New-Email', email);

    const response = await api.put('/v1/accounts/email', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error changing email:', error);
    throw error;
  }
},
changePassword: async (newPassword: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('New-Password', newPassword);

    const response = await api.put('/v1/accounts/password', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    return response.data;
  } catch (error) {
    console.error('Error changing password:', error);
    throw error;
  }
},

  // Получение коллекций пользователя
  getUserCollections: async (
    userId: number, 
    batch: number = 4, 
    page: number = 0
  ): Promise<ProfileCollection[]> => {
    try {
      const response = await api.get<ApiResponse<{
        collections: ProfileCollection[];
        batch: number;
        totalPages: number;
        page: number;
        userId: number;
        totalElements: number;
      }>>(`/v1/user?userId=${userId}/collections&batch=${batch}&page=${page}`);
      
      if (response.data.data.state === 'OK') {
        return response.data.data.key.collections;
      }
      
      throw new Error(response.data.data.message || 'Failed to fetch collections');
    } catch (error) {
      console.error('Error fetching collections:', error);
      return [];
    }
  },
  subscribeToUser: async (userOnId: number): Promise<any> => {
  try {
    const response = await api.post(`/v1/subscribe?userOnId=${userOnId}`);
    return response.data;
  } catch (error) {
    console.error('Error subscribing to user:', error);
    throw error;
  }
},

unsubscribeFromUser: async (userOnId: number): Promise<any> => {
  try {
    const response = await api.post(`/v1/unsubscribe?userOnId=${userOnId}`);
    return response.data;
  } catch (error) {
    console.error('Error unsubscribing from user:', error);
    throw error;
  }
},

checkSubscription: async (userOnId: number): Promise<boolean> => {
  try {
    const response = await api.get(`/v1/is-subscriber?userOnId=${userOnId}`);
    
    if (response.data.data.state === 'OK') {
      return response.data.data.key.isSubscriber;
    }
    
    return false;
  } catch (error) {
    console.error('Error checking subscription:', error);
    return false;
  }
},

// === НОВЫЕ МЕТОДЫ ДЛЯ АДМИНИСТРАТОРА ===

// Заблокировать пользователя
banUser: async (userId: number): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('X-User-Change-ID', userId.toString());

    const response = await api.post('/v1/accounts/admin/ban', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    
    return response.data;
  } catch (error: any) {
    console.error('Error banning user:', error);
    
    // Более информативное сообщение об ошибке
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при блокировке пользователя';
    
    throw new Error(errorMessage);
  }
},

// Разблокировать пользователя
unbanUser: async (userId: number): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('X-User-Change-ID', userId.toString());

    const response = await api.post('/v1/accounts/admin/unban', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    
    return response.data;
  } catch (error: any) {
    console.error('Error unbanning user:', error);
    
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при разблокировке пользователя';
    
    throw new Error(errorMessage);
  }
},
changeUsernameAdmin: async (userId: number, newUsername: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('X-User-Change-ID', userId.toString());
    params.append('New-Username', newUsername);

    const response = await api.put('/v1/accounts/admin/username', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    
    return response.data;
  } catch (error: any) {
    console.error('Error changing username (admin):', error);
    
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при изменении имени пользователя';
    
    throw new Error(errorMessage);
  }
},

// Изменить описание профиля (админ)
changeProfileDescriptionAdmin: async (userId: number, description: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('X-User-Change-ID', userId.toString());
    params.append('New-Description', description);

    const response = await api.put('/v1/accounts/admin/description', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    
    return response.data;
  } catch (error: any) {
    console.error('Error changing profile description (admin):', error);
    
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при изменении описания профиля';
    
    throw new Error(errorMessage);
  }
},

// Изменить никнейм (админ)
changeNicknameAdmin: async (userId: number, nickname: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('X-User-Change-ID', userId.toString());
    params.append('New-Nickname', nickname);

    const response = await api.put('/v1/accounts/admin/nickname', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    
    return response.data;
  } catch (error: any) {
    console.error('Error changing nickname (admin):', error);
    
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при изменении никнейма';
    
    throw new Error(errorMessage);
  }
},

// Изменить email (админ)
changeEmailAdmin: async (userId: number, email: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('X-User-Change-ID', userId.toString());
    params.append('New-Email', email);

    const response = await api.put('/v1/accounts/admin/email', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    
    return response.data;
  } catch (error: any) {
    console.error('Error changing email (admin):', error);
    
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при изменении email';
    
    throw new Error(errorMessage);
  }
},

// Изменить пароль (админ)
changePasswordAdmin: async (userId: number, newPassword: string): Promise<any> => {
  try {
    const params = new URLSearchParams();
    params.append('X-User-Change-ID', userId.toString());
    params.append('New-Password', newPassword);

    const response = await api.put('/v1/accounts/admin/password', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
    
    return response.data;
  } catch (error: any) {
    console.error('Error changing password (admin):', error);
    
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при изменении пароля';
    
    throw new Error(errorMessage);
  }
},

// Изменить фото профиля (админ)
changeProfileImageAdmin: async (userId: number, imageId: number): Promise<any> => {
  try {
    const response = await api.put(`/v1/accounts/admin/image?X-User-Change-ID=${userId}&New-Image-ID=${imageId}`);
    return response.data;
  } catch (error: any) {
    console.error('Error changing profile image (admin):', error);
    
    const errorMessage = error.response?.data?.data?.message ||
      error.response?.data?.message ||
      error.message ||
      'Ошибка при изменении фото профиля';
    
    throw new Error(errorMessage);
  }
},
};

export default profileAPI;