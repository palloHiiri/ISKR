import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import searchAPI from '../api/searchService';
import type { Book, Collection, User } from '../types/popular';

interface SearchState {
  query: string;
  types: string[]; 
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
  genres: Array<{id: number, name: string}>;
  loadingGenres: boolean;
  selectedGenreName: string; 
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
  genres: [],
  loadingGenres: false,
  selectedGenreName: 'Все жанры',
};

export const performSearch = createAsyncThunk(
  'search/perform',
  async (params: { reset?: boolean } = {}, { getState, rejectWithValue }) => {
    try {
      const state = getState() as { search: SearchState };
      const { query, types, genre, limit } = state.search;
    
      
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

export const fetchGenres = createAsyncThunk(
  'search/fetchGenres',
  async (_, { rejectWithValue }) => {
    try {
      return await searchAPI.fetchGenres();
    } catch (error: any) {
      return rejectWithValue(error.message || 'Ошибка загрузки жанров');
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
    setGenreName: (state, action: PayloadAction<string>) => {
      state.selectedGenreName = action.payload;
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
      state.genre = null;
      state.selectedGenreName = 'Все жанры';
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
          state.results = { books, users, collections };
        } else {
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
      })
      .addCase(fetchGenres.pending, (state) => {
        state.loadingGenres = true;
      })
      .addCase(fetchGenres.fulfilled, (state, action) => {
        state.loadingGenres = false;
        state.genres = action.payload;
      })
      .addCase(fetchGenres.rejected, (state, action) => {
        state.loadingGenres = false;
        state.error = action.payload as string;
      });
  },
});

export const { setQuery, setTypes, setGenre, setGenreName, resetSearch, increaseLimit, clearSearch } = searchSlice.actions;
export default searchSlice.reducer;