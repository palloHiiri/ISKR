import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import popularAPI from '../api/popularService';
import type { User, Book, Collection } from '../types/popular';

interface PopularState {
  books: Book[];
  collections: Collection[];
  users: User[];
  loading: {
    books: boolean;
    collections: boolean;
    users: boolean;
  };
  error: {
    books: string | null;
    collections: string | null;
    users: string | null;
  };
}

const initialState: PopularState = {
  books: [],
  collections: [],
  users: [],
  loading: {
    books: false,
    collections: false,
    users: false,
  },
  error: {
    books: null,
    collections: null,
    users: null,
  },
};

export const fetchPopularBooks = createAsyncThunk(
  'popular/fetchBooks',
  async (limit: number = 12, { rejectWithValue }) => {
    try {
      const books = await popularAPI.getPopularBooks(limit);
      return { books };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch popular books');
    }
  }
);

export const fetchPopularCollections = createAsyncThunk(
  'popular/fetchCollections',
  async (limit: number = 12, { rejectWithValue }) => {
    try {
      const collections = await popularAPI.getPopularCollections(limit);
      return { collections };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch popular collections');
    }
  }
);

export const fetchPopularUsers = createAsyncThunk(
  'popular/fetchUsers',
  async (limit: number = 12, { rejectWithValue }) => {
    try {
      const users = await popularAPI.getPopularUsers(limit);
      return { users };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch popular users');
    }
  }
);

export const fetchAllPopular = createAsyncThunk(
  'popular/fetchAll',
  async (limit: number = 12, { dispatch }) => {
    await Promise.all([
      dispatch(fetchPopularBooks(limit)),
      dispatch(fetchPopularCollections(limit)),
      dispatch(fetchPopularUsers(limit)),
    ]);
  }
);

const popularSlice = createSlice({
  name: 'popular',
  initialState,
  reducers: {
    clearErrors: (state) => {
      state.error = {
        books: null,
        collections: null,
        users: null,
      };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPopularBooks.pending, (state) => {
        state.loading.books = true;
        state.error.books = null;
      })
      .addCase(fetchPopularBooks.fulfilled, (state, action) => {
        state.loading.books = false;
        state.books = action.payload.books;
      })
      .addCase(fetchPopularBooks.rejected, (state, action) => {
        state.loading.books = false;
        state.error.books = action.payload as string;
      })
      
      .addCase(fetchPopularCollections.pending, (state) => {
        state.loading.collections = true;
        state.error.collections = null;
      })
      .addCase(fetchPopularCollections.fulfilled, (state, action) => {
        state.loading.collections = false;
        state.collections = action.payload.collections;
      })
      .addCase(fetchPopularCollections.rejected, (state, action) => {
        state.loading.collections = false;
        state.error.collections = action.payload as string;
      })
      
      .addCase(fetchPopularUsers.pending, (state) => {
        state.loading.users = true;
        state.error.users = null;
      })
      .addCase(fetchPopularUsers.fulfilled, (state, action) => {
        state.loading.users = false;
        state.users = action.payload.users;
      })
      .addCase(fetchPopularUsers.rejected, (state, action) => {
        state.loading.users = false;
        state.error.users = action.payload as string;
      });
  },
});

export const { clearErrors } = popularSlice.actions;
export default popularSlice.reducer;