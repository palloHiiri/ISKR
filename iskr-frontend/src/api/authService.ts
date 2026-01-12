import axios from 'axios';
import { OAPI_BASE_URL, API_ENDPOINTS } from '../constants/api';

const api = axios.create({
  baseURL: OAPI_BASE_URL,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  withCredentials: true,
});

const toFormUrlEncoded = (data: Record<string, any>): URLSearchParams => {
  const params = new URLSearchParams();
  Object.keys(data).forEach(key => {
    if (data[key] !== undefined && data[key] !== null) {
      params.append(key, data[key]);
    }
  });
  return params;
};

export interface LoginData {
  username: string;
  password: string;
}

export interface RegisterData {
  Nickname: string;
  Email: string;
  Username: string;
  Password: string;
}

export interface AuthResponse {
  data: {
    access_token: string;
    expires_in: number;
    refresh_expires_in: number;
    refresh_token: string;
    token_type: string;
    session_state: string;
    scope: string;
    'not-before-policy': number;
  };
  meta: {
    processedBy: string;
    timestamp: string;
    userId: string;
  };
}

export interface RegisterResponse {
  data: {
    state: string;
    message: string;
    key: any;
  };
  meta: {
    processedBy: string;
    timestamp: string;
    userId: string;
  };
}

export interface UserData {
  id: string | number;
  username: string;
  email?: string;
  nickname?: string;
  registered_date?: string;
  profile?: {
    up_id: number;
    user_imgl_id: number | null;
    nickname: string;
    email: string;
    email_verified: boolean;
    profile_description: string;
    birth_date: string | null;
    status: string;
  };
  email_verified?: boolean;
  role?: string;
  status?: string;
  roles?: string[]; 
}

export interface ResetPasswordResponse {
  data: {
    state: string;
    message: string;
    key: any;
  };
  meta: {
    processedBy: string;
    timestamp: string;
    userId: string;
  };
}

export interface ResetPasswordConfirmData {
  Token: string;
  Password: string;
}

export interface RedeemTokenData {
  Token: string;
}

export interface RedeemTokenResponse {
  data: {
    state: string;
    message: string;
    key: any;
  };
  meta: {
    processedBy: string;
    timestamp: string;
    userId: string;
  };
}

export interface RoleData {
  id: string;
  name: string;
  description?: string;
  path: string;
  subGroups: any[];
}

export interface RolesResponse {
  data: RoleData[];
  meta: {
    userId: string;
    timestamp: string;
    processedBy: string;
  };
}

export const authAPI = {
  login: async (credentials: LoginData): Promise<{ user: UserData; token: string }> => {
    const params = toFormUrlEncoded(credentials);
    const response = await api.post(API_ENDPOINTS.LOGIN, params);
    
    const userData = await authAPI.getCurrentUser();
    
    return {
      token: response.data.data.access_token,
      user: userData
    };
  },

  register: async (userData: RegisterData): Promise<RegisterResponse> => {
    const registrationData = {
      Nickname: userData.Nickname,
      Email: userData.Email,
      Username: userData.Username,
      Password: userData.Password,
    };
    
    const params = toFormUrlEncoded(registrationData);
    const response = await api.post(API_ENDPOINTS.REGISTER_USER, params);
    return response.data;
  },

  logout: async (): Promise<void> => {
    try {
      await api.post(API_ENDPOINTS.LOGOUT);
    } catch (error) {
      console.error('Logout error:', error);
    }
  },

  getCurrentUser: async (): Promise<UserData> => {
    try {
      const response = await api.get(API_ENDPOINTS.GET_CURRENT_USER);
      
      const responseData = response.data.data || response.data;
      
      if (responseData.state === 'OK' && responseData.key) {
        const key = responseData.key;
        const profile = key.profile || {};
        
        return {
          id: key.user_id || '',
          username: key.username || '',
          nickname: profile.nickname || key.username,
          email: profile.email || '',
          registered_date: key.registered_date || '',
          profile: profile,
          email_verified: profile.email_verified || false,
          status: profile.status || 'notBanned',
        };
      } else {
        throw new Error(responseData.message || 'Не удалось получить данные пользователя');
      }
    } catch (error: any) {
      console.error('Error getting current user:', error);
      throw error;
    }
  },

  getUserRoles: async (): Promise<RolesResponse> => {
    const response = await api.get(API_ENDPOINTS.GET_USER_ROLES);
    return response.data;
  },

  resetPassword: async (login: string): Promise<ResetPasswordResponse> => {
    const params = toFormUrlEncoded({ login });
    const response = await api.post(API_ENDPOINTS.RESET_PASSWORD, params);
    return response.data;
  },

  resetPasswordConfirm: async (data: ResetPasswordConfirmData): Promise<ResetPasswordResponse> => {
    const params = toFormUrlEncoded(data);
    const response = await api.post(API_ENDPOINTS.RESET_PASSWORD_CONFIRM, params);
    return response.data;
  },

  redeemToken: async (data: RedeemTokenData): Promise<RedeemTokenResponse> => {
    const params = toFormUrlEncoded(data);
    const response = await api.post(API_ENDPOINTS.REDEEM_TOKEN, params);
    return response.data;
  },

  getUserProfile: async (): Promise<UserData> => {
    const response = await api.get(API_ENDPOINTS.GET_USER_PROFILE);
    const responseData = response.data.data || response.data;
    
    if (responseData.state === 'OK' && responseData.key) {
      const key = responseData.key;
      const profile = key.profile || {};
      
      return {
        id: key.user_id || '',
        username: key.username || '',
        nickname: profile.nickname || key.username,
        email: profile.email || '',
        registered_date: key.registered_date || '',
        profile: profile,
        email_verified: profile.email_verified || false,
        status: profile.status || 'notBanned',
      };
    } else {
      throw new Error(responseData.message || 'Не удалось получить профиль пользователя');
    }
  }
};