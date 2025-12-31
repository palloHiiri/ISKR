import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import goalsReducer from './goalsSlice';
import popularReducer from './popularSlice';
import searchReducer from './searchSlice'; // новый reducer

export const store = configureStore({
  reducer: {
    auth: authReducer,
    goals: goalsReducer,
    popular: popularReducer,
    search: searchReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export default store;