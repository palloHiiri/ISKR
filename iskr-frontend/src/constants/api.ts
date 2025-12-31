// Базовые URL для API
export const OAPI_BASE_URL = '/oapi'; // Проксируется через Vite
export const IMAGES_BASE_URL = '/images'; // Базовый URL для изображений

// Endpoints
export const API_ENDPOINTS = {
  LOGIN: '/v1/accounts/login',
  REGISTER_USER: '/v1/accounts/user',
  LOGOUT: '/v1/accounts/logout',
  GET_CURRENT_USER: '/v1/accounts/user',
  GET_USER_PROFILE: '/v1/accounts/profile',
  RESET_PASSWORD: '/v1/accounts/reset-password',
  RESET_PASSWORD_CONFIRM: '/v1/accounts/reset-password-confirm',
  REDEEM_TOKEN: '/v1/accounts/redeem-token',
  POPULAR_USERS: '/v1/popular/users',
  POPULAR_BOOKS: '/v1/popular/books',
  POPULAR_COLLECTIONS: '/v1/popular/collections',
  SEARCH: '/v1/search/query',
} as const;

// Ключи для localStorage
export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER: 'user',
  IS_AUTHENTICATED: 'isAuthenticated',
} as const;

// Типы ошибок
export const ERROR_TYPES = {
  AUTHENTICATION_EXCEPTION: 'AuthenticationException',
} as const;

// Статусы ошибок
export const ERROR_STATUSES = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  INTERNAL_SERVER_ERROR: 500,
} as const;

// Типы статусов пользователя
export const USER_STATUSES = {
  NOT_BANNED: 'notBanned',
  BANNED: 'banned',
} as const;

// Состояния ответов API
export const API_STATES = {
  OK: 'OK',
  FAIL_NOT_FOUND: 'Fail_NotFound',
  FAIL_EXPIRED: 'Fail_Expired',
  FAIL_CONFLICT: 'Fail_Conflict',
  FAIL: 'Fail',
} as const;