import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import searchAPI from '../api/searchService';
import type { Book, Collection, User } from '../types/popular';

interface SearchState {
  query: string;
  types: string[]; // ['book', 'user', 'collection']
  genre: number | null;
  results: {
    books: Book[];
    users: User[];
    collections: Collection[];
  };
  loading: boolean;
  error: string | null;
  limit: number;
  hasMore: boolean;
  total: number;
}

const initialState: SearchState = {
  query: '',
  types: ['book', 'user', 'collection'],
  genre: null,
  results: {
    books: [],
    users: [],
    collections: [],
  },
  loading: false,
  error: null,
  limit: 10,
  hasMore: false,
  total: 0,
};

export const performSearch = createAsyncThunk(
  'search/perform',
  async (params: { reset?: boolean } = {}, { getState, rejectWithValue }) => {
    try {
      const state = getState() as { search: SearchState };
      const { query, types, genre, limit } = state.search;
      
      if (!query.trim()) {
        return { books: [], users: [], collections: [], total: 0, hasMore: false };
      }
      
      const searchParams = {
        Query: query,
        Types: types.join(','),
        Limit: limit,
        ...(genre && { Genre: genre })
      };
      
      return await searchAPI.search(searchParams);
    } catch (error: any) {
      return rejectWithValue(error.message || 'Ошибка поиска');
    }
  }
);

const searchSlice = createSlice({
  name: 'search',
  initialState,
  reducers: {
    setQuery: (state, action: PayloadAction<string>) => {
      state.query = action.payload;
    },
    setTypes: (state, action: PayloadAction<string[]>) => {
      state.types = action.payload;
    },
    setGenre: (state, action: PayloadAction<number | null>) => {
      state.genre = action.payload;
    },
    resetSearch: (state) => {
      state.results = { books: [], users: [], collections: [] };
      state.hasMore = false;
      state.total = 0;
      state.limit = 10;
    },
    increaseLimit: (state) => {
      state.limit += 10;
    },
    clearSearch: (state) => {
      state.query = '';
      state.results = { books: [], users: [], collections: [] };
      state.hasMore = false;
      state.total = 0;
      state.limit = 10;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(performSearch.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(performSearch.fulfilled, (state, action) => {
        state.loading = false;
        const { books, users, collections, total, hasMore } = action.payload;
        
        if (state.limit <= 10) {
          // Первый запрос или сброс - заменяем результаты
          state.results = { books, users, collections };
        } else {
          // Пагинация - добавляем к существующим
          state.results.books = [...state.results.books, ...books];
          state.results.users = [...state.results.users, ...users];
          state.results.collections = [...state.results.collections, ...collections];
        }
        
        state.total = total;
        state.hasMore = hasMore;
      })
      .addCase(performSearch.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { setQuery, setTypes, setGenre, resetSearch, increaseLimit, clearSearch } = searchSlice.actions;
export default searchSlice.reducer;