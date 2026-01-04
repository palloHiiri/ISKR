import { useEffect, useState, useCallback } from "react";
import "../home/Home.scss";
import "../statistic/Statistic.scss";
import "./Library.scss";
import Input from "../../controls/input/Input.tsx";
import searchIcon from "../../../assets/elements/search.svg";
import PrimaryButton from "../../controls/primary-button/PrimaryButton.tsx";
import CardElement from "../../controls/card-element/CardElement.tsx";
import VerticalAccordion from "../../controls/vertical-accordion/VerticalAccordion.tsx";
import Change from "../../../assets/elements/change.svg";
import Open from "../../../assets/elements/open.svg";
import Delete from "../../../assets/elements/delete.svg";
import Modal from "../../controls/modal/Modal.tsx";
import ConfirmDialog from "../../controls/confirm-dialog/ConfirmDialog.tsx";
import BookForm, { type BookFormData } from "../../controls/book-form/BookForm.tsx";
import DefaultBookCover from "../../../assets/images/books/tri-tovarischa.jpg";
import CollectionForm, { type CollectionFormData } from "../../controls/collection-form/CollectionForm.tsx";
import SearchFilters from "../../controls/search-filters/SearchFilters.tsx";
import Stars from "../../stars/Stars.tsx";
import AddIcon from "../../../assets/elements/add.svg";
import { useLocation, useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import type { RootState } from "../../../redux/store.ts";
import libraryAPI, { type LibraryBook, type LibraryCollection } from '../../../api/libraryService';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import Avatar1 from '../../../assets/images/users/avatar1.jpg';
import Avatar2 from '../../../assets/images/users/avatar2.jpg';
import Avatar3 from '../../../assets/images/users/avatar3.jpg';
import Avatar4 from '../../../assets/images/users/avatar4.jpg';
import Avatar5 from '../../../assets/images/users/avatar5.jpg';
import Avatar6 from '../../../assets/images/users/avatar6.jpg';
import Avatar7 from '../../../assets/images/users/avatar7.jpg';
import Avatar8 from '../../../assets/images/users/avatar8.jpg';
import CreateBookModal from "../../controls/create-book-modal/CreateBookModal";

// Локальные интерфейсы для компонента
interface Book {
  id: string;
  title: string;
  author: string;
  rating?: number;
  imageUrl: string;
  originalBook?: LibraryBook;
}

interface Collection {
  id: string;
  title: string;
  creator: string;
  booksCount: number;
  imageUrl: string;
  originalCollection?: LibraryCollection;
}

function Library() {
  const { user, isAuthenticated } = useSelector((state: RootState) => state.auth);
  const location = useLocation();
  const navigate = useNavigate();

  // Состояния для данных с бэкенда
  const [libraryBooks, setLibraryBooks] = useState<LibraryBook[]>([]);
  const [libraryCollections, setLibraryCollections] = useState<LibraryCollection[]>([]);
  const [wishlistBooks, setWishlistBooks] = useState<LibraryBook[]>([]);
  const [wishlistId, setWishlistId] = useState<number | undefined>(undefined);

  // Преобразованные данные для отображения
  const [books, setBooks] = useState<Book[]>([]);
  const [collections, setCollections] = useState<Collection[]>([]);
  const [wishlist, setWishlist] = useState<Book[]>([]);

  // Состояния загрузки
  const [loadingBooks, setLoadingBooks] = useState(true);
  const [loadingCollections, setLoadingCollections] = useState(true);
  const [loadingWishlist, setLoadingWishlist] = useState(true);
  const [loadingError, setLoadingError] = useState<string | null>(null);

  // Функция для получения URL изображения книги
  const getBookImageUrl = (book: LibraryBook): string => {
    if (!book.photoLink?.imageData?.uuid || !book.photoLink?.imageData?.extension) {
      console.log(`No image for book: ${book.title}`, book.photoLink);
      return PlaceholderImage;
    }

    const url = `/images/${book.photoLink.imageData.uuid}.${book.photoLink.imageData.extension}`;
    console.log(`Book image URL for ${book.title}:`, url);
    return url;
  };

  // Функция для получения URL изображения коллекции
  const getCollectionImageUrl = (collection: LibraryCollection): string => {
    if (!collection.photoLink?.imageData?.uuid || !collection.photoLink?.imageData?.extension) {
      console.log(`No image for collection: ${collection.title}`, collection.photoLink);
      return PlaceholderImage;
    }

    const url = `/images/${collection.photoLink.imageData.uuid}.${collection.photoLink.imageData.extension}`;
    console.log(`Collection image URL for ${collection.title}:`, url);
    return url;
  };

  // Функция для преобразования LibraryBook в Book
  const transformLibraryBookToBook = (libBook: LibraryBook): Book => {
    const authors = libBook.authors.map(a => a.name).join(', ');
    const rating = libBook.averageRating ? libBook.averageRating / 2 : undefined;

    const imageUrl = getBookImageUrl(libBook);

    return {
      id: libBook.bookId.toString(),
      title: libBook.title,
      author: authors,
      rating: rating,
      imageUrl: imageUrl,
      originalBook: libBook
    };
  };

  // Функция для преобразования LibraryCollection в Collection
  const transformLibraryCollectionToCollection = (libCollection: LibraryCollection): Collection => {
    const imageUrl = getCollectionImageUrl(libCollection);

    return {
      id: libCollection.bcolsId.toString(),
      title: libCollection.title,
      creator: libCollection.ownerNickname,
      booksCount: libCollection.bookCount,
      imageUrl: imageUrl,
      originalCollection: libCollection
    };
  };

  // Загрузка данных с бэкенда
  const loadLibraryData = useCallback(async () => {
    try {
      setLoadingError(null);

      console.log("Начинаем загрузку данных библиотеки...");

      // Загружаем все данные параллельно
      const [booksData, collectionsData, wishlistData] = await Promise.allSettled([
        libraryAPI.getLibraryBooks(),
        libraryAPI.getLibraryCollections(),
        libraryAPI.getWishlist()
      ]);

      // Обработка книг
      if (booksData.status === 'fulfilled') {
        console.log("Книги загружены:", booksData.value);
        setLibraryBooks(booksData.value);
        const transformedBooks = booksData.value.map(transformLibraryBookToBook);
        setBooks(transformedBooks);
      } else {
        console.error('Error loading books:', booksData.reason);
      }
      setLoadingBooks(false);

      // Обработка коллекций
      if (collectionsData.status === 'fulfilled') {
        console.log("Коллекции загружены:", collectionsData.value);
        setLibraryCollections(collectionsData.value);
        const transformedCollections = collectionsData.value.map(transformLibraryCollectionToCollection);
        setCollections(transformedCollections);
      } else {
        console.error('Error loading collections:', collectionsData.reason);
      }
      setLoadingCollections(false);

      // Обработка вишлиста
      if (wishlistData.status === 'fulfilled') {
        console.log("Вишлист загружен:", wishlistData.value.books);
        setWishlistBooks(wishlistData.value.books);
        setWishlistId(wishlistData.value.wishlistId);
        const transformedWishlist = wishlistData.value.books.map(transformLibraryBookToBook);
        setWishlist(transformedWishlist);
      } else {
        console.error('Error loading wishlist:', wishlistData.reason);
      }
      setLoadingWishlist(false);

    } catch (error: any) {
      console.error('Error loading library data:', error);
      setLoadingError(error.message || 'Ошибка загрузки данных библиотеки');
      setLoadingBooks(false);
      setLoadingCollections(false);
      setLoadingWishlist(false);
    }
  }, []);

  // Загрузка данных при монтировании
  useEffect(() => {
    loadLibraryData();
  }, [loadLibraryData]);

  // Модальные окна и состояния
  const [isCreateBookOpen, setIsCreateBookOpen] = useState(false);
  const [highlightedBookId, setHighlightedBookId] = useState<string | null>(null);
  const [isAddCollectionOpen, setIsAddCollectionOpen] = useState(false);
  const [highlightedCollectionId, setHighlightedCollectionId] = useState<string | null>(null);
  const [isConfirmClearWishlist, setIsConfirmClearWishlist] = useState(false);
  const [isConfirmDeleteBook, setIsConfirmDeleteBook] = useState(false);
  const [bookToDelete, setBookToDelete] = useState<string | null>(null);

  // Поиск
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTypes, setSelectedTypes] = useState<string[]>(['books', 'users', 'collections']);
  const [selectedGenre, setSelectedGenre] = useState('Все жанры');
  const [isBookInWishlist, setIsBookInWishlist] = useState(false);
  const [isUserFollowed, setIsUserFollowed] = useState(false);
  const [isCollectionFavorited, setIsCollectionFavorited] = useState(false);

  // Моковые пользователи для поиска (пока нет API)
  const libraryUsers = [
    { id: 1, username: "ghost_67", followers: "12 567", avatar: Avatar1 },
    { id: 2, username: "book_lover", followers: "8 234", avatar: Avatar2 },
    { id: 3, username: "reader_pro", followers: "15 890", avatar: Avatar3 },
    { id: 4, username: "lit_critic", followers: "6 543", avatar: Avatar4 },
    { id: 5, username: "page_turner", followers: "11 234", avatar: Avatar5 },
    { id: 6, username: "bibliophile", followers: "9 876", avatar: Avatar6 },
    { id: 7, username: "story_seeker", followers: "13 456", avatar: Avatar7 },
    { id: 8, username: "word_wizard", followers: "7 890", avatar: Avatar8 },
  ];

  // Функции для обработки событий (пока оставляем моковые, потом заменим на API)
  const handleAddBookClick = () => {
    setIsCreateBookOpen(true);
  };

  const handleBookFormSubmit = (bookData: BookFormData) => {
    const newBook: Book = {
      id: Date.now().toString(),
      title: bookData.title,
      author: bookData.author,
      rating: 0,
      imageUrl: bookData.imageUrl || DefaultBookCover
    };

    setBooks([newBook, ...books]);
    setHighlightedBookId(newBook.id);
    setIsAddBookOpen(false);

    setTimeout(() => {
      setHighlightedBookId(null);
    }, 3000);
  };

  const handleAddCollectionClick = () => {
    setIsAddCollectionOpen(true);
  };

  const handleCollectionFormSubmit = (collectionData: CollectionFormData) => {
    const newCollection: Collection = {
      id: Date.now().toString(),
      title: collectionData.title,
      creator: "user",
      booksCount: collectionData.bookIds.length,
      imageUrl: collectionData.imageUrl || DefaultBookCover
    };

    setCollections([newCollection, ...collections]);
    setHighlightedCollectionId(newCollection.id);
    setIsAddCollectionOpen(false);

    setTimeout(() => {
      setHighlightedCollectionId(null);
    }, 3000);
  };

  const handleClearWishlistClick = () => {
    setIsConfirmClearWishlist(true);
  };

  const handleConfirmClearWishlist = () => {
    setWishlist([]);
    setIsConfirmClearWishlist(false);
  };

  const handleDeleteFromWishlistClick = (id: string) => {
    setBookToDelete(id);
    setIsConfirmDeleteBook(true);
  };

  const handleConfirmDeleteBook = () => {
    if (bookToDelete) {
      setWishlist(wishlist.filter(book => book.id !== bookToDelete));
      setBookToDelete(null);
    }
    setIsConfirmDeleteBook(false);
  };

  // Функции поиска
  const filterSearchResults = () => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return { books: [], users: [], collections: [] };

    const filteredBooks = books.filter(book =>
      book.title.toLowerCase().includes(query) ||
      book.author.toLowerCase().includes(query)
    );

    const filteredUsers = libraryUsers.filter(user =>
      user.username.toLowerCase().includes(query)
    );

    const filteredCollections = collections.filter(collection =>
      collection.title.toLowerCase().includes(query) ||
      collection.creator.toLowerCase().includes(query)
    );

    return {
      books: filteredBooks,
      users: filteredUsers,
      collections: filteredCollections
    };
  };

  const searchResults = filterSearchResults();
  const totalResultsCount = searchResults.books.length + searchResults.users.length + searchResults.collections.length;

  const handleTypeChange = (type: string) => {
    setSelectedTypes(prev =>
      prev.includes(type)
        ? prev.filter(t => t !== type)
        : [...prev, type]
    );
  };

  const handleResetFilters = () => {
    setSelectedTypes(['books', 'users', 'collections']);
    setSelectedGenre('Все жанры');
  };

  const handleSearchBookClick = (book: Book) => {
    navigate('/book', {
      state: {
        id: book.id,
        title: book.title,
        author: book.author,
        coverUrl: book.imageUrl,
        rating: book.rating,
        isMine: isAuthenticated,
        isEditMode: false
      }
    });
  };

  const handleSearchUserClick = (user: typeof libraryUsers[0]) => {
    navigate('/profile', {
      state: {
        username: user.username,
        subscribersCount: parseInt(user.followers.replace(/\s/g, '')),
        avatarUrl: user.avatar
      }
    });
  };

  const handleSearchCollectionClick = (collection: Collection) => {
    navigate('/collection', {
      state: {
        id: collection.id,
        name: collection.title,
        isMine: isAuthenticated,
        coverUrl: collection.imageUrl,
        books: books.slice(0, 5).map(book => ({
          id: book.id,
          title: book.title,
          author: book.author,
          rating: book.rating,
          imageUrl: book.imageUrl
        }))
      }
    });
  };

  // Эффект для обработки обновлений из других страниц
  useEffect(() => {
    if (location.state?.updatedBook) {
      const { id, title, author, rating, imageUrl } = location.state.updatedBook;
      setBooks(prev => prev.map(book =>
        book.id === id ? { ...book, title, author, rating, imageUrl } : book
      ));
    }

    const updatedBookData = sessionStorage.getItem('updatedBook');
    if (updatedBookData) {
      const { id, title, author, rating, imageUrl } = JSON.parse(updatedBookData);
      setBooks(prev => prev.map(book =>
        book.id === id
          ? { ...book, title, author, rating, imageUrl }
          : book
      ));
      sessionStorage.removeItem('updatedBook');
    }

    const deletedBookId = sessionStorage.getItem('deletedBookId');
    if (deletedBookId) {
      setBooks(prev => prev.filter(b => b.id !== deletedBookId));
      sessionStorage.removeItem('deletedBookId');
    }

    const updatedCollectionData = sessionStorage.getItem('updatedCollection');
    if (updatedCollectionData) {
      const { id, name, coverUrl } = JSON.parse(updatedCollectionData);
      setCollections(prev => prev.map(collection =>
        collection.id === id
          ? { ...collection, title: name, imageUrl: coverUrl }
          : collection
      ));
      sessionStorage.removeItem('updatedCollection');
    }

    const deletedCollectionId = sessionStorage.getItem('deletedCollectionId');
    if (deletedCollectionId) {
      setCollections(prev => prev.filter(c => c.id !== deletedCollectionId));
      sessionStorage.removeItem('deletedCollectionId');
    }
  }, [location.state]);

  // Функции для рендеринга состояний загрузки
  const renderLoadingState = (message: string) => (
    <div className="loading-state">
      <div className="loading-spinner"></div>
      <p>{message}</p>
    </div>
  );

  const renderErrorState = (message: string) => (
    <div className="error-state">
      <p>{message}</p>
    </div>
  );

  // Функция для рендеринга раздела книг
  const renderBooksSection = () => {
    if (loadingBooks) {
      return renderLoadingState("Загрузка книг...");
    }

    if (loadingError) {
      return renderErrorState(loadingError);
    }

    if (books.length === 0) {
      return <p className="no-books-message">У вас пока нет добавленных книг.</p>;
    }

    return (
      <VerticalAccordion
        header={
          <div className="statistics-container">
            <div className="statistics-details">
              <div className="my-books-container">
                {books.slice(0, 4).map((book) => (
                  <div
                    key={book.id}
                    className={highlightedBookId === book.id ? 'highlighted-book' : ''}
                  >
                    <CardElement
                      title={book.title}
                      description={book.author}
                      starsCount={book.rating}
                      imageUrl={book.imageUrl}
                      button={true}
                      buttonLabel={"Изменить"}
                      buttonIconUrl={Change}
                      onClick={() => navigate('/book', {
                        state: {
                          id: book.id,
                          title: book.title,
                          author: book.author,
                          rating: book.rating,
                          coverUrl: book.imageUrl,
                          isMine: isAuthenticated,
                          isEditMode: false
                        }
                      })}
                      onButtonClick={() => navigate('/book', {
                        state: {
                          id: book.id,
                          title: book.title,
                          author: book.author,
                          rating: book.rating,
                          coverUrl: book.imageUrl,
                          isMine: isAuthenticated,
                          isEditMode: true
                        }
                      })}
                    />
                  </div>
                ))}
              </div>
            </div>
          </div>
        }
        content={
          books.length > 4 ? (
            <div className="my-books-container">
              {books.slice(4).map((book) => (
                <div
                  key={book.id}
                  className={highlightedBookId === book.id ? 'highlighted-book' : ''}
                >
                  <CardElement
                    title={book.title}
                    description={book.author}
                    starsCount={book.rating}
                    imageUrl={book.imageUrl}
                    button={true}
                    buttonLabel={"Изменить"}
                    buttonIconUrl={Change}
                    onClick={() => navigate('/book', {
                      state: {
                        id: book.id,
                        title: book.title,
                        author: book.author,
                        rating: book.rating,
                        coverUrl: book.imageUrl,
                        isMine: true,
                        isEditMode: false
                      }
                    })}
                    onButtonClick={() => navigate('/book', {
                      state: {
                        id: book.id,
                        title: book.title,
                        author: book.author,
                        rating: book.rating,
                        coverUrl: book.imageUrl,
                        isMine: true,
                        isEditMode: true
                      }
                    })}
                  />
                </div>
              ))}
            </div>
          ) : null
        }
      />
    );
  };

  // Функция для рендеринга раздела коллекций
  const renderCollectionsSection = () => {
    if (loadingCollections) {
      return renderLoadingState("Загрузка коллекций...");
    }

    if (loadingError) {
      return renderErrorState(loadingError);
    }

    if (collections.length === 0) {
      return <p className="no-books-message">У вас пока нет созданных коллекций.</p>;
    }

    return (
      <VerticalAccordion
        header={
          <div className="statistics-container">
            <div className="statistics-details">
              <div className="my-books-container">
                {collections.slice(0, 4).map((collection) => (
                  <div
                    key={collection.id}
                    className={highlightedCollectionId === collection.id ? 'highlighted-book' : ''}
                  >
                    <CardElement
                      title={collection.title}
                      description={collection.creator}
                      infoDecoration={`${collection.booksCount} книг`}
                      imageUrl={collection.imageUrl}
                      button={true}
                      buttonLabel={"Открыть"}
                      buttonIconUrl={Open}
                      onClick={() => navigate('/collection', {
                        state: {
                          id: collection.id,
                          name: collection.title,
                          isMine: isAuthenticated,
                          books: books
                        }
                      })}
                      onButtonClick={() => navigate('/collection', {
                        state: {
                          id: collection.id,
                          name: collection.title,
                          isMine: isAuthenticated,
                          books: books
                        }
                      })}
                    />
                  </div>
                ))}
              </div>
            </div>
          </div>
        }
        content={
          collections.length > 4 ? (
            <div className="my-books-container">
              {collections.slice(4).map((collection) => (
                <div
                  key={collection.id}
                  className={highlightedCollectionId === collection.id ? 'highlighted-book' : ''}
                >
                  <CardElement
                    title={collection.title}
                    description={collection.creator}
                    infoDecoration={`${collection.booksCount} книг`}
                    imageUrl={collection.imageUrl}
                    button={true}
                    buttonLabel={"Открыть"}
                    buttonIconUrl={Open}
                    onClick={() => navigate('/collection', {
                      state: {
                        id: collection.id,
                        name: collection.title,
                        isMine: true,
                        books: books
                      }
                    })}
                    onButtonClick={() => navigate('/collection', {
                      state: {
                        id: collection.id,
                        name: collection.title,
                        isMine: true,
                        books: books
                      }
                    })}
                  />
                </div>
              ))}
            </div>
          ) : null
        }
      />
    );
  };

  // Функция для рендеринга раздела вишлиста
  const renderWishlistSection = () => {
    if (loadingWishlist) {
      return renderLoadingState("Загрузка вишлиста...");
    }

    if (loadingError) {
      return renderErrorState(loadingError);
    }

    if (wishlist.length === 0) {
      return <p className="no-books-message">В вашем вишлисте пока нет книг.</p>;
    }

    return (
      <VerticalAccordion
        header={
          <div className="statistics-container">
            <div className="statistics-details">
              <div className="my-books-container">
                {wishlist.slice(0, 4).map((book) => (
                  <CardElement
                    key={book.id}
                    title={book.title}
                    description={book.author}
                    starsCount={book.rating}
                    imageUrl={book.imageUrl}
                    button={true}
                    buttonLabel={"Удалить"}
                    buttonIconUrl={Delete}
                    onButtonClick={() => handleDeleteFromWishlistClick(book.id)}
                  />
                ))}
              </div>
            </div>
          </div>
        }
        content={
          wishlist.length > 4 ? (
            <div className="my-books-container">
              {wishlist.slice(4).map((book) => (
                <CardElement
                  key={book.id}
                  title={book.title}
                  description={book.author}
                  starsCount={book.rating}
                  imageUrl={book.imageUrl}
                  button={true}
                  buttonLabel={"Удалить"}
                  buttonIconUrl={Delete}
                  onButtonClick={() => handleDeleteFromWishlistClick(book.id)}
                />
              ))}
            </div>
          ) : null
        }
      />
    );
  };

  return (
    <>
      <main>
        <div className="search-container">
          <h2>Поиск по библиотеке</h2>
          <Input placeholder="Название книги, автор, коллекция..." picture={searchIcon} value={searchQuery} onChange={setSearchQuery} />

          {searchQuery && (
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
                    {books.length === 0 && collections.length === 0 && wishlist.length === 0 ? (
                      <div className="no-results-message">
                        У вас в библиотеке пока нет результатов поиска. Пожалуйста, добавьте книги или создайте коллекции, чтобы начать поиск.
                      </div>
                    ) : (
                      <>
                        <div className="results-count">
                          Найдено результатов: {totalResultsCount}
                        </div>

                        {selectedTypes.includes('books') && searchResults.books.length > 0 && (
                          <>
                            {searchResults.books.map((book) => (
                              <div key={book.id} className="search-result-row">
                                <div className="search-result-info" onClick={() => handleSearchBookClick(book)} style={{ cursor: 'pointer' }}>
                                  <img src={book.imageUrl} alt="Book cover" />
                                  <div>
                                    <p className="search-result-title">{book.title}</p>
                                    <p className="search-result-author">{book.author}</p>
                                  </div>
                                </div>
                                <div className="search-result-actions">
                                  <Stars count={book.rating ? Math.round(book.rating) : 0} />
                                  <button
                                    className={isBookInWishlist ? 'active' : ''}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setIsBookInWishlist(!isBookInWishlist);
                                    }}
                                  >
                                    <img src={isBookInWishlist ? Delete : AddIcon} alt="" />
                                    <span>{isBookInWishlist ? 'Удалить из вишлиста' : 'Добавить в вишлист'}</span>
                                  </button>
                                </div>
                              </div>
                            ))}
                          </>
                        )}

                        {selectedTypes.includes('users') && searchResults.users.length > 0 && (
                          <>
                            {searchResults.users.map((user) => (
                              <div key={user.id} className="search-result-row">
                                <div className="search-result-info" onClick={() => handleSearchUserClick(user)} style={{ cursor: 'pointer' }}>
                                  <img src={user.avatar} alt="User avatar" />
                                  <div>
                                    <p className="search-result-title">{user.username}</p>
                                    <p className="search-result-author">{user.followers} подписчиков</p>
                                  </div>
                                </div>
                                <div className="search-result-actions">
                                  <button
                                    className={isUserFollowed ? 'active' : ''}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setIsUserFollowed(!isUserFollowed);
                                    }}
                                  >
                                    <img src={isUserFollowed ? Delete : AddIcon} alt="" />
                                    <span>{isUserFollowed ? 'Отписаться' : 'Подписаться'}</span>
                                  </button>
                                </div>
                              </div>
                            ))}
                          </>
                        )}

                        {selectedTypes.includes('collections') && searchResults.collections.length > 0 && (
                          <>
                            {searchResults.collections.map((collection) => (
                              <div key={collection.id} className="search-result-row">
                                <div className="search-result-info" onClick={() => handleSearchCollectionClick(collection)} style={{ cursor: 'pointer' }}>
                                  <img src={collection.imageUrl} alt="Collection cover" />
                                  <div>
                                    <p className="search-result-title">{collection.title}</p>
                                    <p className="search-result-author">{collection.creator}</p>
                                  </div>
                                </div>
                                <div className="search-result-actions">
                                  {collection.booksCount} книг
                                  <button
                                    className={isCollectionFavorited ? 'active' : ''}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setIsCollectionFavorited(!isCollectionFavorited);
                                    }}
                                  >
                                    <img src={isCollectionFavorited ? Delete : AddIcon} alt="" />
                                    <span>{isCollectionFavorited ? 'Удалить из избранного' : 'Добавить в избранное'}</span>
                                  </button>
                                </div>
                              </div>
                            ))}
                          </>
                        )}

                        {totalResultsCount === 0 && (
                          <div className="no-results-message">
                            По вашему запросу ничего не найдено
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        <div className="top-container">
          <div className="container-title-with-button">
            <h2>Мои книги</h2>
            <PrimaryButton label={"Добавить книгу"} onClick={handleAddBookClick} />
          </div>
          {renderBooksSection()}
        </div>

        <div className="top-container">
          <div className="container-title-with-button">
            <h2>Мои коллекции</h2>
            <PrimaryButton label={"Создать коллекцию"} onClick={handleAddCollectionClick} />
          </div>
          {renderCollectionsSection()}
        </div>

        <div className="top-container">
          <div className="container-title-with-button">
            <h2>Мой вишлист</h2>
            <PrimaryButton label={"Очистить вишлист"} onClick={handleClearWishlistClick} />
          </div>
          {renderWishlistSection()}
        </div>
      </main>

      {/* Модальные окна */}

      <CreateBookModal
        open={isCreateBookOpen}
        onClose={() => setIsCreateBookOpen(false)}
      />


      <Modal open={isAddCollectionOpen} onClose={() => setIsAddCollectionOpen(false)}>
        <CollectionForm
          onSubmit={handleCollectionFormSubmit}
          onCancel={() => setIsAddCollectionOpen(false)}
        />
      </Modal>

      <Modal open={isConfirmClearWishlist} onClose={() => setIsConfirmClearWishlist(false)}>
        <ConfirmDialog
          title="Подтверждение"
          message="Вы уверены, что хотите очистить вишлист?"
          onConfirm={handleConfirmClearWishlist}
          onCancel={() => setIsConfirmClearWishlist(false)}
        />
      </Modal>

      <Modal open={isConfirmDeleteBook} onClose={() => setIsConfirmDeleteBook(false)}>
        <ConfirmDialog
          title="Подтверждение"
          message="Вы уверены, что хотите удалить эту книгу из вишлиста?"
          onConfirm={handleConfirmDeleteBook}
          onCancel={() => setIsConfirmDeleteBook(false)}
        />
      </Modal>
    </>
  );
}

export default Library;