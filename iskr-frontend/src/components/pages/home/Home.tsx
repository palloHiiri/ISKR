import Input from "../../controls/input/Input.tsx";
import searchIcon from '../../../assets/elements/search.svg';
import './Home.scss';
import CardElement from "../../controls/card-element/CardElement.tsx";
import AddIcon from '../../../assets/elements/add.svg';
import HorizontalSlider from "../../controls/horizontal-slider/HorizontalSlider.tsx";
import SearchFilters from "../../controls/search-filters/SearchFilters.tsx";
import { useEffect, useState, useCallback } from "react";
import Stars from "../../stars/Stars.tsx";
import Delete from '../../../assets/elements/delete.svg';
import { russianLocalWordConverter } from "../../../utils/russianLocalWordConverter.ts";
import Login from "../../controls/login/Login.tsx";
import Modal from "../../controls/modal/Modal.tsx";
import type { RootState } from "../../../redux/store.ts";
import { useSelector, useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchAllPopular } from '../../../redux/popularSlice';
import { 
  setQuery, 
  setTypes, 
  setGenre,
  setGenreName,
  resetSearch, 
  increaseLimit, 
  clearSearch,
  performSearch,
  fetchGenres
} from '../../../redux/searchSlice';
import type { User, Book, Collection } from '../../../types/popular';
import { getBookImageUrl, getUserImageUrl, getCollectionImageUrl, formatRating } from '../../../api/popularService';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import { useDebounce } from '../../../hooks/useDebounce';
import PrimaryButton from '../../controls/primary-button/PrimaryButton.tsx';
import SecondaryButton from '../../controls/secondary-button/SecondaryButton.tsx';
import profileAPI from '../../../api/profileService';
import collectionAPI from '../../../api/collectionService';
import wishlistService from '../../../api/wishlistService';

function Home() {
  const [localSearchQuery, setLocalSearchQuery] = useState('');
  const [selectedTypes, setSelectedTypes] = useState<string[]>(['books', 'users', 'collections']);
  const [selectedGenre, setSelectedGenre] = useState('Все жанры');
  
  const [bookWishlistStatus, setBookWishlistStatus] = useState<Record<number, boolean>>({});
  const [collectionLikeStatus, setCollectionLikeStatus] = useState<Record<number, boolean>>({});
  const [userFollowStates, setUserFollowStates] = useState<Record<number, boolean>>({});
  
  const [wishlistLoading, setWishlistLoading] = useState<Record<number, boolean>>({});
  const [collectionLikeLoading, setCollectionLikeLoading] = useState<Record<number, boolean>>({});
  const [followLoading, setFollowLoading] = useState<Record<number, boolean>>({});
  
  const [showLoadMore, setShowLoadMore] = useState(false);
  
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const currentUser = useSelector((state: RootState) => state.auth.user);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [showMessageModal, setShowMessageModal] = useState(false);
  const [messageModalContent, setMessageModalContent] = useState({ title: '', message: '' });
  
  const popular = useSelector((state: RootState) => state.popular);
  const search = useSelector((state: RootState) => state.search);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const debouncedSearchQuery = useDebounce(localSearchQuery, 500);

  useEffect(() => {
    dispatch(fetchAllPopular(12));
    dispatch(fetchGenres());
  }, [dispatch]);

  useEffect(() => {
    if (isAuthenticated) {
      loadPopularItemsStatus();
    }
  }, [isAuthenticated, popular]);

  useEffect(() => {
    if (isAuthenticated && search.results) {
      loadSearchItemsStatus();
    }
  }, [isAuthenticated, search.results]);

  useEffect(() => {
    if (debouncedSearchQuery.trim()) {
      dispatch(setQuery(debouncedSearchQuery));
      dispatch(resetSearch());
      dispatch(performSearch({ reset: true }));
    } else {
      dispatch(clearSearch());
    }
  }, [debouncedSearchQuery, dispatch]);

  useEffect(() => {
    setShowLoadMore(search.hasMore && search.results.books.length + search.results.users.length + search.results.collections.length > 0);
  }, [search.hasMore, search.results]);

  const showErrorMessage = (title: string, message: string) => {
    setMessageModalContent({ title, message });
    setShowMessageModal(true);
  };

  const loadPopularItemsStatus = async () => {
    try {
      for (const book of popular.books) {
        try {
          const isInWishlist = await wishlistService.checkBookInWishlist(book.bookId);
          setBookWishlistStatus(prev => ({ ...prev, [book.bookId]: isInWishlist }));
        } catch (error) {
          console.error(`Error checking wishlist for book ${book.bookId}:`, error);
        }
      }

      for (const collection of popular.collections) {
        try {
          const likeStatus = await collectionAPI.getLikeStatus(collection.collectionId);
          setCollectionLikeStatus(prev => ({ ...prev, [collection.collectionId]: likeStatus.isLiked }));
        } catch (error) {
          console.error(`Error checking like status for collection ${collection.collectionId}:`, error);
        }
      }

      for (const user of popular.users) {
        try {
          const isSubscribed = await profileAPI.checkSubscription(user.userId);
          setUserFollowStates(prev => ({ ...prev, [user.userId]: isSubscribed }));
        } catch (error) {
          console.error(`Error checking subscription for user ${user.userId}:`, error);
        }
      }
    } catch (error) {
      console.error('Error loading popular items status:', error);
    }
  };

  const loadSearchItemsStatus = async () => {
    try {
      for (const book of search.results.books) {
        try {
          const isInWishlist = await wishlistService.checkBookInWishlist(book.bookId);
          setBookWishlistStatus(prev => ({ ...prev, [book.bookId]: isInWishlist }));
        } catch (error) {
          console.error(`Error checking wishlist for book ${book.bookId}:`, error);
        }
      }

      for (const collection of search.results.collections) {
        try {
          const likeStatus = await collectionAPI.getLikeStatus(collection.collectionId);
          setCollectionLikeStatus(prev => ({ ...prev, [collection.collectionId]: likeStatus.isLiked }));
        } catch (error) {
          console.error(`Error checking like status for collection ${collection.collectionId}:`, error);
        }
      }

      for (const user of search.results.users) {
        try {
          const isSubscribed = await profileAPI.checkSubscription(user.userId);
          setUserFollowStates(prev => ({ ...prev, [user.userId]: isSubscribed }));
        } catch (error) {
          console.error(`Error checking subscription for user ${user.userId}:`, error);
        }
      }
    } catch (error) {
      console.error('Error loading search items status:', error);
    }
  };

  const handleBookWishlistToggle = async (bookId: number, bookTitle: string) => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }

    setWishlistLoading(prev => ({ ...prev, [bookId]: true }));

    try {
      const wishlistInfo = await wishlistService.checkWishlist();
      if (!wishlistInfo.hasWishlist) {
        showErrorMessage(
          'Отсутствует вишлист',
          'У вас нет вишлиста. Сначала создайте вишлист через создание коллекции в разделе "библиотека".'
        );
        setWishlistLoading(prev => ({ ...prev, [bookId]: false }));
        return;
      }

      const isCurrentlyInWishlist = bookWishlistStatus[bookId];

      if (isCurrentlyInWishlist) {
        await wishlistService.removeBookFromWishlist(bookId);
        setBookWishlistStatus(prev => ({ ...prev, [bookId]: false }));
        console.log(`Книга "${bookTitle}" удалена из вишлиста`);
      } else {
        await wishlistService.addBookToWishlist(bookId);
        setBookWishlistStatus(prev => ({ ...prev, [bookId]: true }));
        console.log(`Книга "${bookTitle}" добавлена в вишлист`);
      }
    } catch (error: any) {
      console.error('Error toggling wishlist:', error);
      
      if (error.response?.data?.data?.state === 'Fail_Conflict') {
        showErrorMessage('Ошибка', 'Эта книга уже находится в вашем вишлисте.');
      } else if (error.response?.data?.data?.state === 'Fail_NotFound') {
        showErrorMessage('Ошибка', 'Книга не найдена.');
      } else {
        showErrorMessage('Ошибка', 'Ошибка при обновлении вишлиста. Попробуйте еще раз.');
      }
    } finally {
      setWishlistLoading(prev => ({ ...prev, [bookId]: false }));
    }
  };

  const handleCollectionLikeToggle = async (collectionId: number, collectionTitle: string, ownerId: number) => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }

    // Проверка: нельзя лайкать свои собственные коллекции
    if (currentUser?.id && ownerId === Number(currentUser.id)) {
      showErrorMessage(
        'Невозможно выполнить действие',
        'Вы не можете добавлять в избранное свои собственные коллекции.'
      );
      return;
    }

    setCollectionLikeLoading(prev => ({ ...prev, [collectionId]: true }));

    try {
      const isCurrentlyLiked = collectionLikeStatus[collectionId];

      if (isCurrentlyLiked) {
        await collectionAPI.unlikeCollection(collectionId);
        setCollectionLikeStatus(prev => ({ ...prev, [collectionId]: false }));
        console.log(`Коллекция "${collectionTitle}" удалена из избранного`);
      } else {
        await collectionAPI.likeCollection(collectionId);
        setCollectionLikeStatus(prev => ({ ...prev, [collectionId]: true }));
        console.log(`Коллекция "${collectionTitle}" добавлена в избранное`);
      }
    } catch (error: any) {
      console.error('Error toggling collection like:', error);
      
      if (error.response?.data?.data?.state === 'Fail_Conflict') {
        showErrorMessage('Ошибка', 'Эта коллекция уже находится в вашем избранном.');
      } else if (error.response?.data?.data?.state === 'Fail_NotFound') {
        showErrorMessage('Ошибка', 'Коллекция не найдена.');
      } else {
        showErrorMessage('Ошибка', 'Ошибка при обновлении избранного. Попробуйте еще раз.');
      }
    } finally {
      setCollectionLikeLoading(prev => ({ ...prev, [collectionId]: false }));
    }
  };

  const handleUserFollowToggle = async (userId: number, userName: string) => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }

    // Проверка: нельзя подписываться на самого себя
    if (currentUser?.id && userId === Number(currentUser.id)) {
      showErrorMessage(
        'Невозможно выполнить действие',
        'Вы не можете подписаться на самого себя.'
      );
      return;
    }

    setFollowLoading(prev => ({ ...prev, [userId]: true }));

    try {
      const isCurrentlyFollowed = userFollowStates[userId];

      if (isCurrentlyFollowed) {
        const response = await profileAPI.unsubscribeFromUser(userId);
        if (response.data?.state === 'OK') {
          setUserFollowStates(prev => ({ ...prev, [userId]: false }));
          console.log(`Отписались от пользователя "${userName}"`);
        }
      } else {
        const response = await profileAPI.subscribeToUser(userId);
        if (response.data?.state === 'OK') {
          setUserFollowStates(prev => ({ ...prev, [userId]: true }));
          console.log(`Подписались на пользователя "${userName}"`);
        }
      }
    } catch (error: any) {
      console.error('Error toggling follow:', error);
      
      if (error.response?.data?.data?.state === 'Fail_Conflict') {
        showErrorMessage('Ошибка', 'Вы уже подписаны на этого пользователя.');
      } else if (error.response?.data?.data?.state === 'Fail_NotFound') {
        showErrorMessage('Ошибка', 'Пользователь не найден.');
      } else {
        showErrorMessage('Ошибка', 'Ошибка при обновлении подписки. Попробуйте еще раз.');
      }
    } finally {
      setFollowLoading(prev => ({ ...prev, [userId]: false }));
    }
  };

  const handleTypeChange = (type: string) => {
    const newSelectedTypes = selectedTypes.includes(type)
      ? selectedTypes.filter(t => t !== type)
      : [...selectedTypes, type];
    
    setSelectedTypes(newSelectedTypes);
    
    const apiTypes = newSelectedTypes.map(t => {
      if (t === 'books') return 'book';
      if (t === 'users') return 'user';
      if (t === 'collections') return 'collection';
      return t;
    });
    
    dispatch(setTypes(apiTypes));
    
    if (search.query.trim()) {
      dispatch(resetSearch());
      dispatch(performSearch({ reset: true }));
    }
  };

  const handleGenreChange = (genreName: string) => {
    setSelectedGenre(genreName);
    
    if (genreName === 'Все жанры') {
      dispatch(setGenre(null));
      dispatch(setGenreName('Все жанры'));
    } else {
      const genre = search.genres.find(g => g.name === genreName);
      if (genre) {
        dispatch(setGenre(genre.id));
        dispatch(setGenreName(genre.name));
      }
    }
    
    if (search.query.trim()) {
      dispatch(resetSearch());
      dispatch(performSearch({ reset: true }));
    }
  };

  const handleResetFilters = () => {
    setSelectedTypes(['books', 'users', 'collections']);
    setSelectedGenre('Все жанры');
    dispatch(setTypes(['book', 'user', 'collection']));
    dispatch(setGenre(null));
    dispatch(setGenreName('Все жанры'));
    
    if (search.query.trim()) {
      dispatch(resetSearch());
      dispatch(performSearch({ reset: true }));
    }
  };

  const handleSearchChange = (value: string) => {
    setLocalSearchQuery(value);
  };

  const topBooks = popular.books.map((book: Book) => {
    let description = '';
    if (book.subtitle) {
      description = book.subtitle;
    } else if (book.collectionsCount > 0) {
      description = `В ${book.collectionsCount} ${russianLocalWordConverter(
        book.collectionsCount,
        'коллекции',
        'коллекциях',
        'коллекциях',
        'коллекциях'
      )}`;
    } else {
      description = 'Нет описания';
    }

    return {
      id: book.bookId,
      title: book.title,
      description: description,
      rating: formatRating(book.averageRating),
      cover: getBookImageUrl(book) || PlaceholderImage,
      originalBook: book,
    };
  });

  const searchBooks = search.results.books.map((book: Book) => {
    let description = '';
    
    if (book.authors && book.authors.length > 0) {
      description = book.authors.join(', ');
    } 
    else if (book.description) {
      description = book.description.length > 80 
        ? book.description.substring(0, 80) + '...' 
        : book.description;
    } 
    else if (book.subtitle) {
      description = book.subtitle;
    } 
    else if (book.collectionsCount > 0) {
      description = `В ${book.collectionsCount} ${russianLocalWordConverter(
        book.collectionsCount,
        'коллекции',
        'коллекциях',
        'коллекциях',
        'коллекциях'
      )}`;
    } 
    else {
      description = 'Нет описания';
    }

    return {
      id: book.bookId,
      title: book.title,
      description: description,
      rating: formatRating(book.averageRating),
      cover: getBookImageUrl(book) || PlaceholderImage,
      originalBook: book,
    };
  });

  const topCollections = popular.collections.map((collection: Collection) => {
    return {
      id: collection.collectionId,
      title: collection.title,
      description: collection.ownerNickname,
      booksCount: `${collection.bookCount} ${russianLocalWordConverter(
        collection.bookCount,
        'книга',
        'книги',
        'книг',
        'книг'
      )}`,
      cover: getCollectionImageUrl(collection) || PlaceholderImage,
      originalCollection: collection,
      ownerId: collection.ownerId,
    };
  });

  const searchCollections = search.results.collections.map((collection: Collection) => {
    let description = '';
    
    if (collection.description) {
      description = collection.description.length > 80 
        ? collection.description.substring(0, 80) + '...' 
        : collection.description;
    } else {
      description = `${collection.bookCount} ${russianLocalWordConverter(
        collection.bookCount,
        'книга',
        'книги',
        'книг',
        'книг'
      )}`;
    }

    return {
      id: collection.collectionId,
      title: collection.title,
      description: description,
      booksCount: `${collection.bookCount} ${russianLocalWordConverter(
        collection.bookCount,
        'книга',
        'книги',
        'книг',
        'книг'
      )}`,
      cover: getCollectionImageUrl(collection) || PlaceholderImage,
      originalCollection: collection,
      ownerId: collection.ownerId,
    };
  });

  const topUsers = popular.users.map((user: User) => {
    const displayName = user.nickname || user.username;

    return {
      id: user.userId,
      username: user.username,
      nickname: user.nickname,
      displayName: displayName,
      followers: user.subscribersCount.toString(),
      avatar: getUserImageUrl(user) || PlaceholderImage,
    };
  });

  const searchUsers = search.results.users.map((user: User) => {
    const displayName = user.nickname || user.username;

    return {
      id: user.userId,
      username: user.username,
      nickname: user.nickname,
      displayName: displayName,
      followers: user.subscribersCount.toString(),
      avatar: getUserImageUrl(user) || PlaceholderImage,
    };
  });

  const getFollowerCount = (userId: number, baseCount: string) => {
    const isFollowed = userFollowStates[userId];
    const numericCount = parseInt(baseCount.replace(/\s/g, ''));
    const newCount = isFollowed ? numericCount + 1 : numericCount;
    const formattedCount = newCount.toLocaleString('ru-RU').replace(/,/g, ' ');
    return `${formattedCount} ${russianLocalWordConverter(newCount, 'подписчик', 'подписчика', 'подписчиков', 'подписчиков')}`;
  };

  const handleAuthenticatedAction = (action: () => void) => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }
    action();
  };

  const handleBookClick = (book: typeof topBooks[0]) => {
    navigate('/book', {
      state: {
        id: book.id.toString(),
        title: book.title,
        description: book.description,
        coverUrl: book.cover,
        rating: book.rating,
        isMine: false,
        isEditMode: false,
        originalData: book.originalBook,
      }
    });
  };

  const handleCollectionClick = (collection: typeof topCollections[0]) => () => {
    navigate('/collection', {
      state: {
        id: collection.id.toString(),
        name: collection.title,
        description: collection.originalCollection.description,
        isMine: false,
        coverUrl: collection.cover,
        owner: collection.description,
        booksCount: collection.originalCollection.bookCount,
        likesCount: collection.originalCollection.likesCount,
        books: topBooks.slice(0, 5).map(book => ({
          id: book.id.toString(),
          title: book.title,
          description: book.description,
          rating: book.rating,
          imageUrl: book.cover
        }))
      }
    });
  };

  const handleUserClick = (user: typeof topUsers[0]) => {
    navigate('/profile', {
      state: {
        userId: user.id
      }
    });
  };

  const handleSearchBookClick = (book: typeof searchBooks[0]) => {
    navigate('/book', {
      state: {
        id: book.id.toString(),
        title: book.title,
        description: book.description,
        coverUrl: book.cover,
        rating: book.rating,
        isMine: false,
        isEditMode: false,
        originalData: book.originalBook,
      }
    });
  };

  const handleSearchUserClick = (user: typeof searchUsers[0]) => {
    navigate('/profile', {
      state: {
        userId: user.id
      }
    });
  };

  const handleSearchCollectionClick = (collection: typeof searchCollections[0]) => {
    navigate('/collection', {
      state: {
        id: collection.id.toString(),
        name: collection.title,
        description: collection.originalCollection.description,
        isMine: false,
        coverUrl: collection.cover,
        owner: collection.description,
        booksCount: collection.originalCollection.bookCount,
        likesCount: collection.originalCollection.likesCount,
        books: topBooks.slice(0, 5).map(book => ({
          id: book.id.toString(),
          title: book.title,
          description: book.description,
          rating: book.rating,
          imageUrl: book.cover
        }))
      }
    });
  };

  const handleLoadMore = () => {
    dispatch(increaseLimit());
    dispatch(performSearch({ reset: false }));
  };

  const renderLoadingState = () => (
    <div className="loading-state">
      <div className="loading-spinner"></div>
      <p>Загрузка...</p>
    </div>
  );

  const renderErrorState = (error: string | null) => (
    <div className="error-state">
      <p>Ошибка загрузки: {error}</p>
    </div>
  );

  const hasSearchResults = search.results.books.length > 0 || 
                          search.results.users.length > 0 || 
                          search.results.collections.length > 0;

  const totalSearchResults = search.results.books.length + 
                            search.results.users.length + 
                            search.results.collections.length;

  const genreOptions = ['Все жанры', ...search.genres.map(genre => genre.name)];

  return (
    <main>
      <div className="search-container">
        <h2>Поиск</h2>
        <Input 
          placeholder="Название книги, автор, коллекция, пользователь..." 
          picture={searchIcon} 
          value={localSearchQuery}
          onChange={handleSearchChange} 
        />

        {localSearchQuery && (
          <>
            <SearchFilters
              selectedTypes={selectedTypes}
              onTypeChange={handleTypeChange}
              selectedGenre={selectedGenre}
              onGenreChange={handleGenreChange}
              onReset={handleResetFilters}
              genres={search.loadingGenres ? ['Загрузка жанров...'] : genreOptions}
              loadingGenres={search.loadingGenres}
            />

            {selectedTypes.length > 0 && (
              <div className="search-results container">
                <div className="search-results-content">
                  {search.loading && (
                    <div className="search-loading">
                      <div className="search-spinner"></div>
                      <p>Поиск...</p>
                    </div>
                  )}

                  {!search.loading && search.error ? (
                    renderErrorState(search.error)
                  ) : !search.loading && hasSearchResults ? (
                    <>
                      <div className="results-count">
                        Найдено результатов: {search.total}
                      </div>

                      {selectedTypes.includes('books') && search.results.books.length > 0 && (
                        <>
                          {searchBooks.map((book) => {
                            const isInWishlist = bookWishlistStatus[book.id] || false;
                            const isLoading = wishlistLoading[book.id] || false;
                            
                            return (
                              <div key={book.id} className="search-result-row">
                                <div className="search-result-info" onClick={() => handleSearchBookClick(book)} style={{ cursor: 'pointer' }}>
                                  <img src={book.cover} alt="Book cover"/>
                                  <div>
                                    <p className="search-result-title">{book.title}</p>
                                    <p className="search-result-author">{book.description}</p>
                                    {book.rating > 0 && (
                                      <div className="search-result-rating">
                                        <Stars count={book.rating} size="small"/>
                                      </div>
                                    )}
                                  </div>
                                </div>
                                <div className="search-result-actions">
                                  <button
                                    className={isInWishlist ? 'active' : ''}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleBookWishlistToggle(book.id, book.title);
                                    }}
                                    disabled={isLoading}
                                  >
                                    {isLoading ? (
                                      <span>Загрузка...</span>
                                    ) : (
                                      <>
                                        <img src={isInWishlist ? Delete : AddIcon} alt=""/>
                                        <span>{isInWishlist ? 'Удалить из вишлиста' : 'Добавить в вишлист'}</span>
                                      </>
                                    )}
                                  </button>
                                </div>
                              </div>
                            );
                          })}
                        </>
                      )}

                      {selectedTypes.includes('users') && search.results.users.length > 0 && (
                        <>
                          {searchUsers.map((user) => {
                            const isFollowed = userFollowStates[user.id] || false;
                            const isLoading = followLoading[user.id] || false;
                            const followerCount = isFollowed 
                              ? parseInt(user.followers.replace(/\s/g, '')) + 1 
                              : parseInt(user.followers.replace(/\s/g, ''));
                            const formattedCount = followerCount.toLocaleString('ru-RU').replace(/,/g, ' ');
                            
                            return (
                              <div key={user.id} className="search-result-row">
                                <div className="search-result-info" onClick={() => handleSearchUserClick(user)} style={{ cursor: 'pointer' }}>
                                  <img src={user.avatar} alt="User avatar"/>
                                  <div>
                                    <p className="search-result-title">{user.displayName}</p>
                                    <p className="search-result-author">{formattedCount} подписчиков</p>
                                  </div>
                                </div>
                                <div className="search-result-actions">
                                  <button
                                    className={isFollowed ? 'active' : ''}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleUserFollowToggle(user.id, user.displayName);
                                    }}
                                    disabled={isLoading}
                                  >
                                    {isLoading ? (
                                      <span>Загрузка...</span>
                                    ) : (
                                      <>
                                        <img src={isFollowed ? Delete : AddIcon} alt=""/>
                                        <span>{isFollowed ? 'Отписаться' : 'Подписаться'}</span>
                                      </>
                                    )}
                                  </button>
                                </div>
                              </div>
                            );
                          })}
                        </>
                      )}

                      {selectedTypes.includes('collections') && search.results.collections.length > 0 && (
                        <>
                          {searchCollections.map((collection) => {
                            const isLiked = collectionLikeStatus[collection.id] || false;
                            const isLoading = collectionLikeLoading[collection.id] || false;
                            const isOwnCollection = currentUser?.id && collection.ownerId === Number(currentUser.id);
                            
                            return (
                              <div key={collection.id} className="search-result-row">
                                <div className="search-result-info" onClick={() => handleSearchCollectionClick(collection)} style={{ cursor: 'pointer' }}>
                                  <img src={collection.cover} alt="Collection cover"/>
                                  <div>
                                    <p className="search-result-title">{collection.title}</p>
                                    <p className="search-result-author">{collection.description}</p>
                                  </div>
                                </div>
                                <div className="search-result-actions">
                                  <span className="books-count">{collection.booksCount}</span>
                                  <button
                                    className={isLiked ? 'active' : ''}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      if (isOwnCollection) {
                                        showErrorMessage(
                                          'Невозможно выполнить действие',
                                          'Вы не можете добавлять в избранное свои собственные коллекции.'
                                        );
                                      } else {
                                        handleCollectionLikeToggle(collection.id, collection.title, collection.ownerId);
                                      }
                                    }}
                                    disabled={isLoading || isOwnCollection}
                                  >
                                    {isLoading ? (
                                      <span>Загрузка...</span>
                                    ) : (
                                      <>
                                        <img src={isLiked ? Delete : AddIcon} alt=""/>
                                        <span>{isLiked ? 'Удалить из избранного' : 'Добавить в избранное'}</span>
                                      </>
                                    )}
                                  </button>
                                </div>
                              </div>
                            );
                          })}
                        </>
                      )}

                      {showLoadMore && (
                        <div className="load-more-container">
                          <PrimaryButton
                            label="Загрузить еще"
                            onClick={handleLoadMore}
                            disabled={search.loading}
                          />
                        </div>
                      )}
                    </>
                  ) : localSearchQuery.trim() && !search.loading ? (
                    <div className="no-results-message">
                      По вашему запросу ничего не найдено
                    </div>
                  ) : null}
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <div className="top-container">
        <h2>Топ-12 книг сайта</h2>
        {popular.loading.books ? (
          renderLoadingState()
        ) : popular.error.books ? (
          renderErrorState(popular.error.books)
        ) : topBooks.length > 0 ? (
          <HorizontalSlider>
            {topBooks.map((book) => {
              const isInWishlist = bookWishlistStatus[book.id] || false;
              
              return (
                <CardElement
                  key={book.id}
                  title={book.title}
                  description={book.description}
                  starsCount={book.rating}
                  imageUrl={book.cover}
                  button={true}
                  buttonLabel={"Добавить в вишлист"}
                  onClick={() => handleBookClick(book)}
                  buttonIconUrl={AddIcon}
                  buttonChanged={true}
                  buttonChangedIconUrl={Delete}
                  buttonChangedLabel={"Удалить из вишлиста"}
                  isAuthenticated={isAuthenticated}
                  onUnauthorized={() => setShowLoginModal(true)}
                  starsSize="small"
                  onButtonClick={() => handleBookWishlistToggle(book.id, book.title)}
                  isButtonActive={isInWishlist}
                />
              );
            })}
          </HorizontalSlider>
        ) : (
          <p className="no-data-message">Пока нет данных о книгах</p>
        )}
      </div>

      <div className="top-container">
        <h2>Топ-12 коллекций сайта</h2>
        {popular.loading.collections ? (
          renderLoadingState()
        ) : popular.error.collections ? (
          renderErrorState(popular.error.collections)
        ) : topCollections.length > 0 ? (
          <HorizontalSlider>
            {topCollections.map((collection) => {
              const isLiked = collectionLikeStatus[collection.id] || false;
              const isOwnCollection = currentUser?.id && collection.ownerId === Number(currentUser.id);
              
              return (
                <CardElement
                  key={collection.id}
                  title={collection.title}
                  description={collection.description}
                  infoDecoration={collection.booksCount}
                  imageUrl={collection.cover}
                  button={!isOwnCollection}
                  onClick={handleCollectionClick(collection)}
                  buttonLabel={"Добавить в избранное"}
                  buttonIconUrl={AddIcon}
                  buttonChanged={true}
                  buttonChangedIconUrl={Delete}
                  buttonChangedLabel={"Удалить из избранного"}
                  isAuthenticated={isAuthenticated}
                  onUnauthorized={() => setShowLoginModal(true)}
                  onButtonClick={() => handleCollectionLikeToggle(collection.id, collection.title, collection.ownerId)}
                  isButtonActive={isLiked}
                />
              );
            })}
          </HorizontalSlider>
        ) : (
          <p className="no-data-message">Пока нет данных о коллекциях</p>
        )}
      </div>

      <div className="top-container">
        <h2>Топ-12 пользователей сайта</h2>
        {popular.loading.users ? (
          renderLoadingState()
        ) : popular.error.users ? (
          renderErrorState(popular.error.users)
        ) : topUsers.length > 0 ? (
          <HorizontalSlider>
            {topUsers.map((user) => {
              const isFollowed = userFollowStates[user.id] || false;
              const followerCount = isFollowed 
                ? parseInt(user.followers.replace(/\s/g, '')) + 1 
                : parseInt(user.followers.replace(/\s/g, ''));
              const formattedCount = followerCount.toLocaleString('ru-RU').replace(/,/g, ' ');
              const description = `${formattedCount} ${russianLocalWordConverter(
                followerCount,
                'подписчик',
                'подписчика',
                'подписчиков',
                'подписчиков'
              )}`;
              const isOwnProfile = currentUser?.id && user.id === Number(currentUser.id);
              
              return (
                <CardElement
                  key={user.id}
                  title={user.displayName}
                  description={description}
                  imageUrl={user.avatar}
                  button={!isOwnProfile}
                  buttonLabel={"Подписаться"}
                  onClick={() => handleUserClick(user)}
                  buttonIconUrl={AddIcon}
                  buttonChanged={true}
                  buttonChangedIconUrl={Delete}
                  buttonChangedLabel={"Отписаться"}
                  onButtonClick={() => handleUserFollowToggle(user.id, user.displayName)}
                  isButtonActive={isFollowed}
                  isAuthenticated={isAuthenticated}
                  onUnauthorized={() => setShowLoginModal(true)}
                />
              );
            })}
          </HorizontalSlider>
        ) : (
          <p className="no-data-message">Пока нет данных о пользователях</p>
        )}
      </div>

      <Modal
        open={showLoginModal}
        onClose={() => setShowLoginModal(false)}
      >
        <Login
          type="login"
          onSubmit={() => setShowLoginModal(false)}
        />
      </Modal>

      <Modal
        open={showMessageModal}
        onClose={() => setShowMessageModal(false)}
      >
        <div className="message-modal-content">
          <h3>{messageModalContent.title}</h3>
          <p>{messageModalContent.message}</p>
          <div className="message-modal-actions">
            <PrimaryButton
              label="OK"
              onClick={() => setShowMessageModal(false)}
            />
          </div>
        </div>
      </Modal>
    </main>
  );
}

export default Home;