import { useEffect, useState, useCallback } from "react";
import "../home/Home.scss";
import "../statistic/Statistic.scss";
import "./Library.scss";
import PrimaryButton from "../../controls/primary-button/PrimaryButton.tsx";
import CardElement from "../../controls/card-element/CardElement.tsx";
import VerticalAccordion from "../../controls/vertical-accordion/VerticalAccordion.tsx";
import Change from "../../../assets/elements/change.svg";
import Open from "../../../assets/elements/open.svg";
import Delete from "../../../assets/elements/delete.svg";
import Modal from "../../controls/modal/Modal.tsx";
import ConfirmDialog from "../../controls/confirm-dialog/ConfirmDialog.tsx";
import DefaultBookCover from "../../../assets/images/books/tri-tovarischa.jpg";
import SearchFilters from "../../controls/search-filters/SearchFilters.tsx";
import Stars from "../../stars/Stars.tsx";
import AddIcon from "../../../assets/elements/add.svg";
import { useLocation, useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import type { RootState } from "../../../redux/store.ts";
import libraryAPI, { type LibraryBook, type LibraryCollection } from '../../../api/libraryService';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import CreateBookModal from "../../controls/create-book-modal/CreateBookModal";
import CreateCollectionModal from "../../controls/create-collection-modal/CreateCollectionModal";
import wishlistService from '../../../api/wishlistService';

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
  const [hasWishlist, setHasWishlist] = useState<boolean>(false);

  // Преобразованные данные для отображения
  const [books, setBooks] = useState<Book[]>([]);
  const [collections, setCollections] = useState<Collection[]>([]);
  const [wishlist, setWishlist] = useState<Book[]>([]);

  // Состояния загрузки
  const [loadingBooks, setLoadingBooks] = useState(true);
  const [loadingCollections, setLoadingCollections] = useState(true);
  const [loadingWishlist, setLoadingWishlist] = useState(true);
  const [loadingError, setLoadingError] = useState<string | null>(null);

  const [isCreateCollectionOpen, setIsCreateCollectionOpen] = useState(false);
  const [isCreateBookOpen, setIsCreateBookOpen] = useState(false);
  
  // Модальные окна и состояния
  const [highlightedBookId, setHighlightedBookId] = useState<string | null>(null);
  const [highlightedCollectionId, setHighlightedCollectionId] = useState<string | null>(null);
  const [isConfirmClearWishlist, setIsConfirmClearWishlist] = useState(false);
  const [isConfirmDeleteBook, setIsConfirmDeleteBook] = useState(false);
  const [bookToDelete, setBookToDelete] = useState<string | null>(null);
  
  // Модальное окно для сообщений
  const [showMessageModal, setShowMessageModal] = useState(false);
  const [messageModalContent, setMessageModalContent] = useState({ title: '', message: '' });

  // Функция для показа сообщений
  const showErrorMessage = (title: string, message: string) => {
    setMessageModalContent({ title, message });
    setShowMessageModal(true);
  };

  // Функция для получения URL изображения книги
  const getBookImageUrl = (book: LibraryBook): string => {
    if (!book.photoLink?.imageData?.uuid || !book.photoLink?.imageData?.extension) {
      return PlaceholderImage;
    }

    const url = `/images/${book.photoLink.imageData.uuid}.${book.photoLink.imageData.extension}`;
    return url;
  };

  // Функция для получения URL изображения коллекции
  const getCollectionImageUrl = (collection: LibraryCollection): string => {
    if (!collection.photoLink?.imageData?.uuid || !collection.photoLink?.imageData?.extension) {
      return PlaceholderImage;
    }

    const url = `/images/${collection.photoLink.imageData.uuid}.${collection.photoLink.imageData.extension}`;
    return url;
  };

  const handleCollectionCreated = (collection: any) => {
    // Обновляем список коллекций
    loadLibraryData();

    // Показываем уведомление или выделяем коллекцию
    setHighlightedCollectionId(collection.collectionId.toString());

    setTimeout(() => {
      setHighlightedCollectionId(null);
    }, 3000);
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
        setLibraryBooks(booksData.value);
        const transformedBooks = booksData.value.map(transformLibraryBookToBook);
        setBooks(transformedBooks);
      } else {
        console.error('Error loading books:', booksData.reason);
      }
      setLoadingBooks(false);

      // Обработка коллекций
      if (collectionsData.status === 'fulfilled') {
        setLibraryCollections(collectionsData.value);
        const transformedCollections = collectionsData.value.map(transformLibraryCollectionToCollection);
        setCollections(transformedCollections);
      } else {
        console.error('Error loading collections:', collectionsData.reason);
      }
      setLoadingCollections(false);

      // Обработка вишлиста
      if (wishlistData.status === 'fulfilled') {
        setWishlistBooks(wishlistData.value.books);
        setWishlistId(wishlistData.value.wishlistId);
        setHasWishlist(true);
        const transformedWishlist = wishlistData.value.books.map(transformLibraryBookToBook);
        setWishlist(transformedWishlist);
      } else {
        console.error('Error loading wishlist:', wishlistData.reason);
        // Проверяем наличие вишлиста через отдельный API
        try {
          const wishlistInfo = await wishlistService.checkWishlist();
          setHasWishlist(wishlistInfo.hasWishlist);
          setWishlistId(wishlistInfo.wishlistId);
        } catch (error) {
          console.error('Error checking wishlist:', error);
          setHasWishlist(false);
        }
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

  // Функции для обработки событий
  const handleAddBookClick = () => {
    setIsCreateBookOpen(true);
  };

  const handleAddCollectionClick = () => {
    setIsCreateCollectionOpen(true);
  };

  const handleClearWishlistClick = async () => {
    if (!hasWishlist) {
      showErrorMessage(
        'Вишлист отсутствует',
        'У вас нет вишлиста. Сначала создайте вишлист, добавив книги на главной странице.'
      );
      return;
    }

    if (wishlist.length === 0) {
      showErrorMessage(
        'Вишлист пуст',
        'Ваш вишлист уже пуст. Добавьте книги на главной странице, чтобы наполнить его.'
      );
      return;
    }

    setIsConfirmClearWishlist(true);
  };

  const handleConfirmClearWishlist = async () => {
    try {
      await wishlistService.clearWishlist();
      setWishlist([]);
      setWishlistBooks([]);
      setIsConfirmClearWishlist(false);
      showErrorMessage(
        'Успех',
        'Вишлист успешно очищен.'
      );
    } catch (error: any) {
      console.error('Error clearing wishlist:', error);
      showErrorMessage(
        'Ошибка',
        'Не удалось очистить вишлист. Попробуйте еще раз.'
      );
    }
  };

  const handleDeleteFromWishlistClick = (id: string) => {
    setBookToDelete(id);
    setIsConfirmDeleteBook(true);
  };

  const handleConfirmDeleteBook = async () => {
    if (!bookToDelete) return;

    try {
      await wishlistService.removeBookFromWishlist(parseInt(bookToDelete));
      setWishlist(prev => prev.filter(book => book.id !== bookToDelete));
      setWishlistBooks(prev => prev.filter(book => book.bookId.toString() !== bookToDelete));
      setBookToDelete(null);
      setIsConfirmDeleteBook(false);
      showErrorMessage(
        'Успех',
        'Книга успешно удалена из вишлиста.'
      );
    } catch (error: any) {
      console.error('Error deleting book from wishlist:', error);
      showErrorMessage(
        'Ошибка',
        'Не удалось удалить книгу из вишлиста. Попробуйте еще раз.'
      );
    }
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
                      buttonLabel={"Открыть"}
                      buttonIconUrl={Open}
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
                          isEditMode: false // Меняем с true на false
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

    if (!hasWishlist) {
      return <p className="no-books-message">У вас нет вишлиста. Начните добавлять книги в вишлист на главной странице.</p>;
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

      <CreateCollectionModal
        open={isCreateCollectionOpen}
        onClose={() => setIsCreateCollectionOpen(false)}
        onCollectionCreated={handleCollectionCreated}
      />

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
    </>
  );
}

export default Library;