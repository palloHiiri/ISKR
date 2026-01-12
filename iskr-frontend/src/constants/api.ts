export const OAPI_BASE_URL = '/oapi'; 
export const IMAGES_BASE_URL = '/images'; 

export const API_ENDPOINTS = {
  LOGIN: '/v1/accounts/login',
  REGISTER_USER: '/v1/accounts/user',
  LOGOUT: '/v1/accounts/logout',
  GET_CURRENT_USER: '/v1/accounts/user',
  GET_USER_PROFILE: '/v1/accounts/profile',
  GET_USER_ROLES: '/v1/accounts/role', 
  RESET_PASSWORD: '/v1/accounts/reset-password',
  RESET_PASSWORD_CONFIRM: '/v1/accounts/reset-password-confirm',
  REDEEM_TOKEN: '/v1/accounts/redeem-token',
  POPULAR_USERS: '/v1/popular/users',
  POPULAR_BOOKS: '/v1/popular/books',
  POPULAR_COLLECTIONS: '/v1/popular/collections',
  SEARCH: '/v1/search/query',
} as const;

export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER: 'user',
  IS_AUTHENTICATED: 'isAuthenticated',
} as const;

export const ERROR_TYPES = {
  AUTHENTICATION_EXCEPTION: 'AuthenticationException',
} as const;

export const ERROR_STATUSES = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  INTERNAL_SERVER_ERROR: 500,
} as const;

export const USER_STATUSES = {
  NOT_BANNED: 'notBanned',
  BANNED: 'banned',
} as const;

export const API_STATES = {
  OK: 'OK',
  FAIL_NOT_FOUND: 'Fail_NotFound',
  FAIL_EXPIRED: 'Fail_Expired',
  FAIL_CONFLICT: 'Fail_Conflict',
  FAIL: 'Fail',
} as const;