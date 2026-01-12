import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import { authAPI, type LoginData, type RegisterData, type UserData, type ResetPasswordConfirmData, type RegisterResponse, type RedeemTokenData, type RoleData, type RolesResponse } from '../api/authService';
import { API_STATES, ERROR_STATUSES } from '../constants/api';

export interface User {
  id: string | number;
  userId?: number; 
  username: string;
  nickname?: string;
  email?: string;
  role?: string;
  email_verified?: boolean;
  status?: string;
  roles?: string[]; 
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
}

interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  isLoading: boolean;
  error: string | null;
  registrationSuccess: boolean;
  emailVerificationSuccess: boolean;
  isAdmin: boolean; 
}

const getStoredUser = (): User | null => {
  try {
    const userStr = localStorage.getItem('user');
    if (!userStr || userStr === 'undefined' || userStr === 'null') {
      return null;
    }
    return JSON.parse(userStr);
  } catch (error) {
    console.error('Error parsing user from localStorage:', error);
    return null;
  }
};

const getStoredToken = (): string | null => {
  const token = localStorage.getItem('token');
  return token && token !== 'undefined' && token !== 'null' ? token : null;
};

const checkIsAdmin = (user: User | null): boolean => {
  if (!user) return false;
  
  if (user.roles && Array.isArray(user.roles)) {
    return user.roles.some(role => role.toLowerCase() === 'admin');
  }
  
  if (user.role) {
    return user.role.toLowerCase() === 'admin';
  }
  
  return false;
};

const initialState: AuthState = {
  isAuthenticated: !!getStoredToken(),
  user: getStoredUser(),
  token: getStoredToken(),
  isLoading: false,
  error: null,
  registrationSuccess: false,
  emailVerificationSuccess: false,
  isAdmin: checkIsAdmin(getStoredUser()), 
};

export const fetchUserRoles = createAsyncThunk(
  'auth/fetchUserRoles',
  async (_, { rejectWithValue }) => {
    try {
      const response = await authAPI.getUserRoles();
      return response;
    } catch (error: any) {
      const errorMessage = error.response?.data?.data?.message ||
        error.response?.data?.message ||
        error.message ||
        'Ошибка при получении ролей пользователя';
      return rejectWithValue(errorMessage);
    }
  }
);

export const login = createAsyncThunk(
  'auth/login',
  async (credentials: LoginData, { rejectWithValue, dispatch }) => {
    try {
      const response = await authAPI.login(credentials);
      
      let roles: RoleData[] = [];
      try {
        const rolesResponse = await authAPI.getUserRoles();
        roles = rolesResponse.data || [];
      } catch (rolesError) {
        console.warn('Не удалось получить роли пользователя:', rolesError);
      }
      
      return {
        ...response,
        roles
      };
    } catch (error: any) {
      let errorMessage = error.response?.data?.data?.message ||
        error.response?.data?.message ||
        error.message ||
        'Ошибка входа';

      if (errorMessage === 'Invalid credentials') {
        errorMessage = 'Неверный логин или пароль';
      } else if (errorMessage === 'Account is not fully set-up') {
        errorMessage = 'Email пользователя не подтвержден или пользователь заблокирован';
      }

      return rejectWithValue(errorMessage);
    }
  }
);

export const signUp = createAsyncThunk(
  'auth/signUp',
  async (userData: RegisterData, { rejectWithValue }) => {
    try {
      const response = await authAPI.register(userData);
      return response;
    } catch (error: any) {
      let errorMessage = 'Ошибка регистрации';

      if (error.response?.status === ERROR_STATUSES.CONFLICT) {
        const details = error.response?.data?.data?.details;
        if (details?.state === API_STATES.FAIL_CONFLICT) {
          if (details?.message?.includes('Username already taken')) {
            errorMessage = 'Имя пользователя уже занято';
          } else if (details?.message?.includes('email already exists')) {
            errorMessage = 'Email уже используется';
          } else {
            errorMessage = details?.message || 'Конфликт данных';
          }
        } else if (details?.state === API_STATES.FAIL) {
          if (details?.message?.includes('duplicate key value violates unique constraint "user_profiles_email_key"')) {
            errorMessage = 'Email уже используется';
          } else {
            errorMessage = details?.message || 'Ошибка базы данных';
          }
        }
      } else if (error.response?.data?.data?.message) {
        errorMessage = error.response.data.data.message;
      }

      return rejectWithValue(errorMessage);
    }
  }
);

export const resetPassword = createAsyncThunk(
  'auth/resetPassword',
  async (login: string, { rejectWithValue }) => {
    try {
      const response = await authAPI.resetPassword(login);
      return response;
    } catch (error: any) {
      let errorMessage = error.response?.data?.data?.message ||
        error.response?.data?.message ||
        error.message ||
        'Ошибка при восстановлении пароля';

      return rejectWithValue(errorMessage);
    }
  }
);

export const resetPasswordConfirm = createAsyncThunk(
  'auth/resetPasswordConfirm',
  async (data: ResetPasswordConfirmData, { rejectWithValue }) => {
    try {
      const response = await authAPI.resetPasswordConfirm(data);

      if (response.data.state === API_STATES.OK) {
        return response;
      } else if (response.data.state === API_STATES.FAIL_NOT_FOUND) {
        return rejectWithValue('Недействительная ссылка для сброса пароля');
      } else if (response.data.state === 'Fail_Expired') {
        return rejectWithValue('Ссылка для сброса пароля просрочена. Запросите новую ссылку.');
      } else {
        return rejectWithValue(response.data.message || 'Ошибка при сбросе пароля');
      }
    } catch (error: any) {
      let errorMessage = error.response?.data?.data?.message ||
        error.response?.data?.message ||
        error.message ||
        'Ошибка при сбросе пароля';

      if (error.response?.status === 404) {
        errorMessage = 'Недействительная ссылка для сброса пароля';
      }

      return rejectWithValue(errorMessage);
    }
  }
);

export const redeemToken = createAsyncThunk(
  'auth/redeemToken',
  async (data: RedeemTokenData, { rejectWithValue }) => {
    try {
      const response = await authAPI.redeemToken(data);

      if (response.data.state === API_STATES.OK) {
        return response;
      } else if (response.data.state === API_STATES.FAIL_NOT_FOUND) {
        return rejectWithValue('Недействительная ссылка для подтверждения email');
      } else {
        return rejectWithValue(response.data.message || 'Ошибка при подтверждении email');
      }
    } catch (error: any) {
      let errorMessage = error.response?.data?.data?.message ||
        error.response?.data?.message ||
        error.message ||
        'Ошибка при подтверждении email';

      if (error.response?.status === 404) {
        errorMessage = 'Недействительная ссылка для подтверждения email';
      }

      return rejectWithValue(errorMessage);
    }
  }
);

export const checkAuth = createAsyncThunk(
  'auth/checkAuth',
  async (_, { rejectWithValue }) => {
    try {
      const userData = await authAPI.getCurrentUser();
      
      let roles: RoleData[] = [];
      try {
        const rolesResponse = await authAPI.getUserRoles();
        roles = rolesResponse.data || [];
      } catch (rolesError) {
        console.warn('Не удалось получить роли пользователя:', rolesError);
      }
      
      return { user: userData, roles };
    } catch (error: any) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');

      const errorMessage = error.response?.status === 404
        ? 'Сессия истекла'
        : 'Не авторизован';

      return rejectWithValue(errorMessage);
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state): void => {
      state.isAuthenticated = false;
      state.user = null;
      state.token = null;
      state.registrationSuccess = false;
      state.emailVerificationSuccess = false;
      state.isAdmin = false;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    },
    setUsername: (state, action: PayloadAction<string>): void => {
      if (state.user) {
        state.user.username = action.payload;
        if (state.user.profile) {
          state.user.profile.nickname = action.payload;
        }
        try {
          localStorage.setItem('user', JSON.stringify(state.user));
        } catch (error) {
          console.error('Error saving user to localStorage:', error);
        }
      }
    },
    clearError: (state) => {
      state.error = null;
    },
    clearRegistrationSuccess: (state) => {
      state.registrationSuccess = false;
    },
    clearEmailVerificationSuccess: (state) => {
      state.emailVerificationSuccess = false;
    },
    updateUserRoles: (state, action: PayloadAction<string[]>) => {
      if (state.user) {
        state.user.roles = action.payload;
        state.isAdmin = action.payload.some(role => role.toLowerCase() === 'admin');
        try {
          localStorage.setItem('user', JSON.stringify(state.user));
        } catch (error) {
          console.error('Error saving user to localStorage:', error);
        }
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.isLoading = false;
        state.isAuthenticated = true;
        const userWithRoles = {
          ...action.payload.user,
          userId: action.payload.user.id,
          roles: action.payload.roles.map((role: RoleData) => role.name)
        };
        state.user = userWithRoles;
        state.token = action.payload.token;
        state.isAdmin = action.payload.roles.some((role: RoleData) => 
          role.name.toLowerCase() === 'admin'
        );

        try {
          if (action.payload.token) {
            localStorage.setItem('token', action.payload.token);
          }
          if (action.payload.user) {
            localStorage.setItem('user', JSON.stringify(userWithRoles));
          }
        } catch (error) {
          console.error('Error saving to localStorage:', error);
        }
      })
      .addCase(login.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })

      .addCase(signUp.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.registrationSuccess = false;
      })
      .addCase(signUp.fulfilled, (state) => {
        state.isLoading = false;
        state.registrationSuccess = true;
        state.error = null;
      })
      .addCase(signUp.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
        state.registrationSuccess = false;
      })

      .addCase(resetPassword.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(resetPassword.fulfilled, (state) => {
        state.isLoading = false;
      })
      .addCase(resetPassword.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })

      .addCase(resetPasswordConfirm.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(resetPasswordConfirm.fulfilled, (state) => {
        state.isLoading = false;
      })
      .addCase(resetPasswordConfirm.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })

      .addCase(redeemToken.pending, (state) => {
        state.isLoading = true;
        state.error = null;
        state.emailVerificationSuccess = false;
      })
      .addCase(redeemToken.fulfilled, (state) => {
        state.isLoading = false;
        state.emailVerificationSuccess = true;
        state.error = null;
      })
      .addCase(redeemToken.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
        state.emailVerificationSuccess = false;
      })

      .addCase(checkAuth.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(checkAuth.fulfilled, (state, action) => {
        state.isLoading = false;
        state.isAuthenticated = true;
        const userWithRoles = {
          ...action.payload.user,
          userId: action.payload.user.id,
          roles: action.payload.roles.map((role: RoleData) => role.name)
        };
        state.user = userWithRoles;
        state.isAdmin = action.payload.roles.some((role: RoleData) => 
          role.name.toLowerCase() === 'admin'
        );

        try {
          if (action.payload.user) {
            localStorage.setItem('user', JSON.stringify(userWithRoles));
          }
        } catch (error) {
          console.error('Error saving user to localStorage:', error);
        }
      })
      .addCase(checkAuth.rejected, (state, action) => {
        state.isLoading = false;
        state.isAuthenticated = false;
        state.user = null;
        state.token = null;
        state.isAdmin = false;
        state.error = action.payload as string;

        try {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
        } catch (error) {
          console.error('Error clearing localStorage:', error);
        }
      })

      .addCase(fetchUserRoles.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchUserRoles.fulfilled, (state, action) => {
        state.isLoading = false;
        if (state.user) {
          state.user.roles = action.payload.data.map((role: RoleData) => role.name);
          state.isAdmin = action.payload.data.some((role: RoleData) => 
            role.name.toLowerCase() === 'admin'
          );
          try {
            localStorage.setItem('user', JSON.stringify(state.user));
          } catch (error) {
            console.error('Error saving user to localStorage:', error);
          }
        }
      })
      .addCase(fetchUserRoles.rejected, (state, action) => {
        state.isLoading = false;
        console.error('Ошибка при получении ролей:', action.payload);
      });
  },
});

export const { 
  logout, 
  setUsername, 
  clearError, 
  clearRegistrationSuccess, 
  clearEmailVerificationSuccess,
  updateUserRoles 
} = authSlice.actions;

export const selectIsAdmin = (state: { auth: AuthState }) => state.auth.isAdmin;
export const selectUserRoles = (state: { auth: AuthState }) => state.auth.user?.roles || [];
export const selectHasRole = (roleName: string) => (state: { auth: AuthState }) => 
  state.auth.user?.roles?.some(role => role.toLowerCase() === roleName.toLowerCase()) || false;

export default authSlice.reducer;