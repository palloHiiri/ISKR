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
  resetSearch, 
  increaseLimit, 
  clearSearch,
  performSearch 
} from '../../../redux/searchSlice';
import type { User, Book, Collection } from '../../../types/popular';
import { getBookImageUrl, getUserImageUrl, getCollectionImageUrl, formatRating } from '../../../api/popularService';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import { useDebounce } from '../../../hooks/useDebounce.ts';
import PrimaryButton from '../../controls/primary-button/PrimaryButton.tsx';

function Home() {
  const [localSearchQuery, setLocalSearchQuery] = useState('');
  const [selectedTypes, setSelectedTypes] = useState<string[]>(['books', 'users', 'collections']);
  const [selectedGenre, setSelectedGenre] = useState('Все жанры');
  const [isBookInWishlist, setIsBookInWishlist] = useState(false);
  const [isCollectionFavorited, setIsCollectionFavorited] = useState(false);
  const [userFollowStates, setUserFollowStates] = useState<Record<number, boolean>>({});
  const [showLoadMore, setShowLoadMore] = useState(false);
  
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const [showLoginModal, setShowLoginModal] = useState(false);
  
  const popular = useSelector((state: RootState) => state.popular);
  const search = useSelector((state: RootState) => state.search);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  // Используем дебаунс для поискового запроса
  const debouncedSearchQuery = useDebounce(localSearchQuery, 500);

  // Загружаем популярный контент при монтировании компонента
  useEffect(() => {
    dispatch(fetchAllPopular(12));
  }, [dispatch]);

  // Обработчик изменения поискового запроса
  useEffect(() => {
    if (debouncedSearchQuery.trim()) {
      dispatch(setQuery(debouncedSearchQuery));
      dispatch(resetSearch());
      dispatch(performSearch({ reset: true }));
    } else {
      dispatch(clearSearch());
    }
  }, [debouncedSearchQuery, dispatch]);

  // Обновляем состояние кнопки "Загрузить еще"
  useEffect(() => {
    setShowLoadMore(search.hasMore && search.results.books.length + search.results.users.length + search.results.collections.length > 0);
  }, [search.hasMore, search.results]);

  const handleTypeChange = (type: string) => {
    const newSelectedTypes = selectedTypes.includes(type)
      ? selectedTypes.filter(t => t !== type)
      : [...selectedTypes, type];
    
    setSelectedTypes(newSelectedTypes);
    
    // Преобразуем типы для API: books -> book, users -> user, collections -> collection
    const apiTypes = newSelectedTypes.map(t => {
      if (t === 'books') return 'book';
      if (t === 'users') return 'user';
      if (t === 'collections') return 'collection';
      return t;
    });
    
    dispatch(setTypes(apiTypes));
    
    // Если есть поисковый запрос, выполняем новый поиск
    if (search.query.trim()) {
      dispatch(resetSearch());
      dispatch(performSearch({ reset: true }));
    }
  };

  const handleResetFilters = () => {
    setSelectedTypes(['books', 'users', 'collections']);
    setSelectedGenre('Все жанры');
    dispatch(setTypes(['book', 'user', 'collection']));
    
    // Если есть поисковый запрос, выполняем новый поиск
    if (search.query.trim()) {
      dispatch(resetSearch());
      dispatch(performSearch({ reset: true }));
    }
  };

  const handleSearchChange = (value: string) => {
    setLocalSearchQuery(value);
  };

  // Преобразование данных из API в формат для компонентов - КНИГИ (популярные)
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

  // Преобразование данных поиска - КНИГИ
  const searchBooks = search.results.books.map((book: Book) => {
    let description = '';
    
    // Для книг из поиска используем авторов, если они есть
    if (book.authors && book.authors.length > 0) {
      description = book.authors.join(', ');
    } 
    // Иначе используем описание из API
    else if (book.description) {
      // Ограничиваем длину описания
      description = book.description.length > 80 
        ? book.description.substring(0, 80) + '...' 
        : book.description;
    } 
    // Иначе используем подзаголовок
    else if (book.subtitle) {
      description = book.subtitle;
    } 
    // Иначе информацию о коллекциях
    else if (book.collectionsCount > 0) {
      description = `В ${book.collectionsCount} ${russianLocalWordConverter(
        book.collectionsCount,
        'коллекции',
        'коллекциях',
        'коллекциях',
        'коллекциях'
      )}`;
    } 
    // Дефолтное описание
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

  // Преобразование данных из API в формат для компонентов - КОЛЛЕКЦИИ (популярные)
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
    };
  });

  // Преобразование данных поиска - КОЛЛЕКЦИИ
  const searchCollections = search.results.collections.map((collection: Collection) => {
    let description = '';
    
    // Для коллекций из поиска используем описание или количество книг
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
    };
  });

  // Преобразование данных из API в формат для компонентов - ПОЛЬЗОВАТЕЛИ (популярные)
  const topUsers = popular.users.map((user: User) => {
    return {
      id: user.userId,
      username: user.username,
      nickname: user.nickname,
      followers: user.subscribersCount.toString(),
      avatar: getUserImageUrl(user) || PlaceholderImage,
    };
  });

  // Преобразование данных поиска - ПОЛЬЗОВАТЕЛИ
  const searchUsers = search.results.users.map((user: User) => {
    return {
      id: user.userId,
      username: user.username,
      nickname: user.nickname,
      followers: user.subscribersCount.toString(),
      avatar: getUserImageUrl(user) || PlaceholderImage,
    };
  });

  const handleUserFollow = (userId: number) => {
    setUserFollowStates(prev => ({
      ...prev,
      [userId]: !prev[userId]
    }));
  };

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
        username: user.username,
        subscribersCount: parseInt(user.followers.replace(/\s/g, '')),
        avatarUrl: user.avatar
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
        username: user.username,
        subscribersCount: parseInt(user.followers.replace(/\s/g, '')),
        avatarUrl: user.avatar
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
              onGenreChange={setSelectedGenre}
              onReset={handleResetFilters}
            />

            {selectedTypes.length > 0 && (
              <div className="search-results container">
                <div className="search-results-content">
                  {search.loading ? (
                    renderLoadingState()
                  ) : search.error ? (
                    renderErrorState(search.error)
                  ) : hasSearchResults ? (
                    <>
                      <div className="results-count">
                        Найдено результатов: {search.total}
                      </div>

                      {selectedTypes.includes('books') && search.results.books.length > 0 && (
                        <>
                          {searchBooks.map((book) => (
                            <div key={book.id} className="search-result-row">
                              <div className="search-result-info" onClick={() => handleSearchBookClick(book)} style={{ cursor: 'pointer' }}>
                                <img src={book.cover} alt="Book cover"/>
                                <div>
                                  <p className="search-result-title">{book.title}</p>
                                  <p className="search-result-author">{book.description}</p>
                                  {/* Добавляем отображение рейтинга */}
                                  {book.rating > 0 && (
                                    <div className="search-result-rating">
                                      <Stars count={Math.round(book.rating)}/>
                                      <span className="rating-value">{book.rating.toFixed(1)}</span>
                                    </div>
                                  )}
                                </div>
                              </div>
                              <div className="search-result-actions">
                                <button
                                  className={isBookInWishlist ? 'active' : ''}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleAuthenticatedAction(() => setIsBookInWishlist(!isBookInWishlist));
                                  }}
                                >
                                  <img src={isBookInWishlist ? Delete : AddIcon} alt=""/>
                                  <span>{isBookInWishlist ? 'Удалить из вишлиста' : 'Добавить в вишлист'}</span>
                                </button>
                              </div>
                            </div>
                          ))}
                        </>
                      )}

                      {selectedTypes.includes('users') && search.results.users.length > 0 && (
                        <>
                          {searchUsers.map((user) => {
                            const isFollowed = userFollowStates[user.id] || false;
                            const followerCount = isFollowed 
                              ? parseInt(user.followers.replace(/\s/g, '')) + 1 
                              : parseInt(user.followers.replace(/\s/g, ''));
                            const formattedCount = followerCount.toLocaleString('ru-RU').replace(/,/g, ' ');
                            
                            return (
                              <div key={user.id} className="search-result-row">
                                <div className="search-result-info" onClick={() => handleSearchUserClick(user)} style={{ cursor: 'pointer' }}>
                                  <img src={user.avatar} alt="User avatar"/>
                                  <div>
                                    <p className="search-result-title">{user.username}</p>
                                    <p className="search-result-author">{formattedCount} подписчиков</p>
                                  </div>
                                </div>
                                <div className="search-result-actions">
                                  <button
                                    className={isFollowed ? 'active' : ''}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleAuthenticatedAction(() => handleUserFollow(user.id));
                                    }}
                                  >
                                    <img src={isFollowed ? Delete : AddIcon} alt=""/>
                                    <span>{isFollowed ? 'Отписаться' : 'Подписаться'}</span>
                                  </button>
                                </div>
                              </div>
                            );
                          })}
                        </>
                      )}

                      {selectedTypes.includes('collections') && search.results.collections.length > 0 && (
                        <>
                          {searchCollections.map((collection) => (
                            <div key={collection.id} className="search-result-row">
                              <div className="search-result-info" onClick={() => handleSearchCollectionClick(collection)} style={{ cursor: 'pointer' }}>
                                <img src={collection.cover} alt="Collection cover"/>
                                <div>
                                  <p className="search-result-title">{collection.title}</p>
                                  <p className="search-result-author">{collection.description}</p>
                                </div>
                              </div>
                              <div className="search-result-actions">
                                {collection.booksCount}
                                <button
                                  className={isCollectionFavorited ? 'active' : ''}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleAuthenticatedAction(() => setIsCollectionFavorited(!isCollectionFavorited));
                                  }}
                                >
                                  <img src={isCollectionFavorited ? Delete : AddIcon} alt=""/>
                                  <span>{isCollectionFavorited ? 'Удалить из избранного' : 'Добавить в избранное'}</span>
                                </button>
                              </div>
                            </div>
                          ))}
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
                  ) : localSearchQuery.trim() ? (
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

      {/* Топ книг */}
      <div className="top-container">
        <h2>Топ-12 книг сайта</h2>
        {popular.loading.books ? (
          renderLoadingState()
        ) : popular.error.books ? (
          renderErrorState(popular.error.books)
        ) : topBooks.length > 0 ? (
          <HorizontalSlider>
            {topBooks.map((book) => (
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
              />
            ))}
          </HorizontalSlider>
        ) : (
          <p className="no-data-message">Пока нет данных о книгах</p>
        )}
      </div>

      {/* Топ коллекций */}
      <div className="top-container">
        <h2>Топ-12 коллекций сайта</h2>
        {popular.loading.collections ? (
          renderLoadingState()
        ) : popular.error.collections ? (
          renderErrorState(popular.error.collections)
        ) : topCollections.length > 0 ? (
          <HorizontalSlider>
            {topCollections.map((collection) => (
              <CardElement
                key={collection.id}
                title={collection.title}
                description={collection.description}
                infoDecoration={collection.booksCount}
                imageUrl={collection.cover}
                button={true}
                onClick={handleCollectionClick(collection)}
                buttonLabel={"Добавить в избранное"}
                buttonIconUrl={AddIcon}
                buttonChanged={true}
                buttonChangedIconUrl={Delete}
                buttonChangedLabel={"Удалить из избранного"}
                isAuthenticated={isAuthenticated}
                onUnauthorized={() => setShowLoginModal(true)}
              />
            ))}
          </HorizontalSlider>
        ) : (
          <p className="no-data-message">Пока нет данных о коллекциях</p>
        )}
      </div>

      {/* Топ пользователей */}
      <div className="top-container">
        <h2>Топ-12 пользователей сайта</h2>
        {popular.loading.users ? (
          renderLoadingState()
        ) : popular.error.users ? (
          renderErrorState(popular.error.users)
        ) : topUsers.length > 0 ? (
          <HorizontalSlider>
            {topUsers.map((user) => (
              <CardElement
                key={user.id}
                title={user.username}
                description={`${parseInt(user.followers).toLocaleString('ru-RU')} ${russianLocalWordConverter(
                  parseInt(user.followers),
                  'подписчик',
                  'подписчика',
                  'подписчиков',
                  'подписчиков'
                )}`}
                imageUrl={user.avatar}
                button={true}
                buttonLabel={"Подписаться"}
                onClick={() => handleUserClick(user)}
                buttonIconUrl={AddIcon}
                buttonChanged={true}
                buttonChangedIconUrl={Delete}
                buttonChangedLabel={"Отписаться"}
                onButtonClick={() => handleUserFollow(user.id)}
                isButtonActive={userFollowStates[user.id] || false}
                isAuthenticated={isAuthenticated}
                onUnauthorized={() => setShowLoginModal(true)}
              />
            ))}
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
    </main>
  );
}

export default Home;