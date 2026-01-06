// /src/components/pages/collection/Collection.tsx
import './Collection.scss';
import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../../../redux/store.ts';
import CardElement from '../../controls/card-element/CardElement.tsx';
import PrimaryButton from '../../controls/primary-button/PrimaryButton.tsx';
import SecondaryButton from '../../controls/secondary-button/SecondaryButton.tsx';
import Modal from '../../controls/modal/Modal.tsx';
import ConfirmDialog from '../../controls/confirm-dialog/ConfirmDialog.tsx';
import BookSearchModal from '../../controls/book-search-modal/BookSearchModal.tsx';
import EditCollectionModal from '../../controls/edit-collection-modal/EditCollectionModal.tsx';
import Change from '../../../assets/elements/change-pink.svg';
import Delete from '../../../assets/elements/delete-pink.svg';
import HeartEmpty from '../../../assets/elements/heart-empty.svg';
import HeartFilled from '../../../assets/elements/heart-filled.svg';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import { collectionAPI, type CollectionInfo, type CollectionBook } from '../../../api/collectionService';
import { getImageUrl, getBookImageUrl, formatRating } from '../../../api/popularService';
import { russianLocalWordConverter } from '../../../utils/russianLocalWordConverter.ts';
import LockIcon from '../../../assets/elements/lock.svg';
import { selectIsAdmin } from '../../../redux/authSlice';

// Функции для перевода значений
const translateCollectionType = (type: string): string => {
  const translations: Record<string, string> = {
    'Standard': 'Обычная',
    'Liked': 'Понравившиеся',
    'Wishlist': 'Вишлист'
  };
  return translations[type] || type;
};

const translateConfidentiality = (confidentiality: string): string => {
  const translations: Record<string, string> = {
    'Public': 'Публичная',
    'Private': 'Приватная'
  };
  return translations[confidentiality] || confidentiality;
};

interface BookCardData {
  id: string;
  title: string;
  author: string;
  rating?: number;
  imageUrl: string;
  originalData: CollectionBook;
}

function Collection() {
  const location = useLocation();
  const navigate = useNavigate();
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const currentUser = useSelector((state: RootState) => state.auth.user);
  const isAdmin = useSelector(selectIsAdmin);

  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showDeleteBookDialog, setShowDeleteBookDialog] = useState(false);
  const [showAddBookModal, setShowAddBookModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedBookId, setSelectedBookId] = useState<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [collectionInfo, setCollectionInfo] = useState<CollectionInfo | null>(null);
  const [books, setBooks] = useState<BookCardData[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [isForbidden, setIsForbidden] = useState(false);
  const [isLiked, setIsLiked] = useState(false);
  const [likesCount, setLikesCount] = useState(0);

  const collectionId = parseInt(location.state?.id || '0');
  const isOwner = collectionInfo && currentUser ? collectionInfo.ownerId === Number(currentUser.id) : false;

  // Определяем, может ли пользователь редактировать коллекцию (владелец или администратор)
  const canEditCollection = isOwner || (isAdmin && isAuthenticated);

  // Загрузка данных коллекции
  useEffect(() => {
    const loadCollectionData = async () => {
      if (!collectionId) {
        setError('ID коллекции не указан');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        setIsForbidden(false);

        let collectionData: CollectionInfo;
        let booksData: CollectionBooksResponse;

        // Используем административные эндпоинты для администраторов
        if (isAdmin && isAuthenticated) {
          // Загрузка данных коллекции через административные эндпоинты
          [collectionData, booksData] = await Promise.all([
            collectionAPI.getCollectionAdmin(collectionId),
            collectionAPI.getCollectionBooksAdmin(collectionId, 12, 0)
          ]);
        } else {
          // Используем обычные эндпоинты для обычных пользователей
          [collectionData, booksData] = await Promise.all([
            collectionAPI.getCollection(collectionId, isAuthenticated),
            collectionAPI.getCollectionBooks(collectionId, 12, 0, isAuthenticated)
          ]);
        }

        setCollectionInfo(collectionData);
        setLikesCount(collectionData.likesCount);
        setTotalPages(booksData.totalPages);
        setCurrentPage(booksData.page);
        setHasMore(booksData.totalPages > 1);

        // Преобразуем книги в формат для компонента
        const formattedBooks: BookCardData[] = booksData.books.map((book: CollectionBook) => ({
          id: book.bookId.toString(),
          title: book.title,
          author: book.authors.map(a => a.name).join(', '),
          rating: formatRating(book.averageRating),
          imageUrl: getBookImageUrl(book) || PlaceholderImage,
          originalData: book
        }));

        setBooks(formattedBooks);

        // Если пользователь авторизован и не владелец, проверяем лайк
        if (isAuthenticated && collectionData.ownerId !== Number(currentUser?.id)) {
          try {
            const likeStatus = await collectionAPI.getLikeStatus(collectionId);
            setIsLiked(likeStatus.isLiked);
          } catch (err) {
            console.error('Error fetching like status:', err);
          }
        }
      } catch (err: any) {
        console.error('Error loading collection:', err);

        // Проверяем, является ли ошибка ошибкой доступа
        if (err.response && err.response.status === 403) {
          // Для администраторов не должно быть ошибок доступа
          if (isAdmin && isAuthenticated) {
            setError('Администраторская ошибка доступа. Убедитесь в правильности ID коллекции.');
          } else {
            setIsForbidden(true);
            setError('Доступ к этой коллекции запрещен');
          }
        } else {
          setError(err.message || 'Ошибка загрузки коллекции');
        }
      } finally {
        setLoading(false);
      }
    };

    loadCollectionData();
  }, [collectionId, isAuthenticated, currentUser, isAdmin]);

  // Загрузка дополнительных книг (пагинация)
  const loadMoreBooks = async () => {
    if (!collectionId || !hasMore) return;

    try {
      const nextPage = currentPage + 1;
      let booksData: CollectionBooksResponse;

      // Используем административные эндпоинты для администраторов
      if (isAdmin && isAuthenticated) {
        booksData = await collectionAPI.getCollectionBooksAdmin(collectionId, 12, nextPage);
      } else {
        booksData = await collectionAPI.getCollectionBooks(collectionId, 12, nextPage, isAuthenticated);
      }

      const formattedBooks: BookCardData[] = booksData.books.map((book: CollectionBook) => ({
        id: book.bookId.toString(),
        title: book.title,
        author: book.authors.map(a => a.name).join(', '),
        rating: formatRating(book.averageRating),
        imageUrl: getBookImageUrl(book) || PlaceholderImage,
        originalData: book
      }));

      setBooks(prev => [...prev, ...formattedBooks]);
      setCurrentPage(nextPage);
      setHasMore(nextPage < totalPages - 1);
    } catch (err) {
      console.error('Error loading more books:', err);
    }
  };

  // Удаление коллекции
  const handleDeleteCollection = () => {
    setShowDeleteDialog(true);
  };

  const confirmDeleteCollection = async () => {
    try {
      // Используем административный метод для администраторов
      if (isAdmin && isAuthenticated) {
        await collectionAPI.deleteCollectionAdmin(collectionId);
      } else {
        await collectionAPI.deleteCollection(collectionId);
      }

      setShowDeleteDialog(false);
      // Перенаправляем на корень (/)
      navigate('/library');
    } catch (err: any) {
      console.error('Error deleting collection:', err);
      setError('Ошибка удаления коллекции');
      setShowDeleteDialog(false);
    }
  };

  // Удаление книги из коллекции
  const handleDeleteBook = (bookId: string) => {
    setSelectedBookId(bookId);
    setShowDeleteBookDialog(true);
  };

  const confirmDeleteBook = async () => {
    if (!selectedBookId || !collectionId) return;

    try {
      // Используем административный метод для администраторов
      if (isAdmin && isAuthenticated) {
        await collectionAPI.removeBookFromCollectionAdmin(collectionId, parseInt(selectedBookId));
      } else {
        await collectionAPI.removeBookFromCollection(collectionId, parseInt(selectedBookId));
      }

      setBooks(prev => prev.filter(book => book.id !== selectedBookId));
      setShowDeleteBookDialog(false);
      setSelectedBookId(null);
    } catch (err: any) {
      console.error('Error deleting book from collection:', err);
      setError('Ошибка удаления книги из коллекции');
      setShowDeleteBookDialog(false);
    }
  };

  // Обработка добавления книг в коллекцию
  const handleBooksAdded = (bookIds: number[]) => {
    // Перезагружаем список книг
    const loadBooks = async () => {
      try {
        let booksData: CollectionBooksResponse;

        // Используем соответствующие эндпоинты
        if (isAdmin && isAuthenticated) {
          booksData = await collectionAPI.getCollectionBooksAdmin(collectionId, 12, 0);
        } else {
          booksData = await collectionAPI.getCollectionBooks(collectionId, 12, 0, isAuthenticated);
        }

        const formattedBooks: BookCardData[] = booksData.books.map((book: CollectionBook) => ({
          id: book.bookId.toString(),
          title: book.title,
          author: book.authors.map(a => a.name).join(', '),
          rating: formatRating(book.averageRating),
          imageUrl: getBookImageUrl(book) || PlaceholderImage,
          originalData: book
        }));
        setBooks(formattedBooks);
        setTotalPages(booksData.totalPages);
        setCurrentPage(booksData.page);
        setHasMore(booksData.totalPages > 1);
      } catch (err) {
        console.error('Error reloading books:', err);
      }
    };
    loadBooks();
  };

  // Обработка клика по книге
  const handleBookClick = (book: BookCardData) => {
    navigate('/book', {
      state: {
        id: book.id,
        title: book.title,
        author: book.author,
        rating: book.rating,
        coverUrl: book.imageUrl,
        isMine: isOwner,
        isEditMode: false,
        originalData: book.originalData
      }
    });
  };

  // Обработка клика по владельцу
  const handleOwnerClick = () => {
    if (collectionInfo) {
      navigate('/profile', {
        state: {
          userId: collectionInfo.ownerId
        }
      });
    }
  };

  // Обработка лайков
  const handleLikeToggle = async () => {
    if (!isAuthenticated || !collectionId) return;

    try {
      if (isLiked) {
        await collectionAPI.unlikeCollection(collectionId);
        setIsLiked(false);
        setLikesCount(prev => Math.max(0, prev - 1));
      } else {
        await collectionAPI.likeCollection(collectionId);
        setIsLiked(true);
        setLikesCount(prev => prev + 1);
      }
    } catch (err: any) {
      console.error('Error toggling like:', err);
      setError('Ошибка обновления лайка');
    }
  };

  // Обработка обновления коллекции
  const handleCollectionUpdated = async (updatedCollection: CollectionInfo) => {
    try {
      // Обновляем информацию о коллекции
      setCollectionInfo(updatedCollection);
      setLikesCount(updatedCollection.likesCount);
    } catch (err: any) {
      console.error('Error updating collection info:', err);
      setError('Ошибка обновления информации о коллекции');
    }
  };

  // Функция для обновления коллекции (вызывается из EditCollectionModal)
  const handleUpdateCollection = async (data: Partial<CollectionInfo>) => {
    if (!collectionId) return;

    try {
      let updatedCollection: CollectionInfo;

      // Используем административный метод для администраторов
      if (isAdmin && isAuthenticated) {
        updatedCollection = await collectionAPI.updateCollectionAdmin(collectionId, {
          title: data.title,
          description: data.description,
          confidentiality: data.confidentiality as 'Public' | 'Private',
          collectionType: data.collectionType,
          photoLink: data.photoLink?.imglId || null
        });
      } else {
        updatedCollection = await collectionAPI.updateCollection(collectionId, {
          title: data.title,
          description: data.description,
          confidentiality: data.confidentiality as 'Public' | 'Private',
          collectionType: data.collectionType,
          photoLink: data.photoLink?.imglId || null
        });
      }

      setCollectionInfo(updatedCollection);
      setShowEditModal(false);
    } catch (err: any) {
      console.error('Error updating collection:', err);
      setError('Ошибка обновления коллекции');
    }
  };

  // Функция для форматирования количества книг
  const formatBooksCount = (count: number): string => {
    return `${count} ${russianLocalWordConverter(count, 'книга', 'книги', 'книг', 'книг')}`;
  };

  // Функция для форматирования количества лайков
  const formatLikesCount = (count: number): string => {
    return `${count} ${russianLocalWordConverter(count, 'лайк', 'лайка', 'лайков', 'лайков')}`;
  };

  // Рендер состояний загрузки и ошибок
  const renderLoadingState = () => (
    <div className="loading-state">
      <div className="loading-spinner"></div>
      <p>Загрузка коллекции...</p>
    </div>
  );

  const renderErrorState = () => (
    <div className="error-state">
      <p>Ошибка: {error}</p>
      <SecondaryButton
        label="Вернуться назад"
        onClick={() => navigate(-1)}
      />
    </div>
  );

  const renderForbiddenState = () => (
    <div className="forbidden-state">
      <div className="forbidden-icon">
        <img src={LockIcon} alt="Замок" />
      </div>
      <h3>Коллекция недоступна</h3>
      <p className="forbidden-message">
        Эта коллекция является приватной или у вас нет прав для ее просмотра.
      </p>
      <p className="forbidden-hint">
        Если вы считаете, что это ошибка, обратитесь к владельцу коллекции.
      </p>
      <SecondaryButton
        label="Вернуться назад"
        onClick={() => navigate(-1)}
      />
    </div>
  );

  if (loading) {
    return (
      <main>
        <div className="collection-page-container">
          {renderLoadingState()}
        </div>
      </main>
    );
  }

  if (isForbidden && !(isAdmin && isAuthenticated)) {
    return (
      <main>
        <div className="collection-page-container">
          {renderForbiddenState()}
        </div>
      </main>
    );
  }

  if (error || !collectionInfo) {
    return (
      <main>
        <div className="collection-page-container">
          {renderErrorState()}
        </div>
      </main>
    );
  }

  return (
    <main>
      <div className="collection-page-container">
        <button
          className="page-close-button"
          onClick={() => navigate(-1)}
          aria-label="Вернуться назад"
          data-tooltip="Закрыть окно"
        >
          ×
        </button>

        <div className="collection-header">
          <div className="collection-title-section">
            <h2 className="collection-title">{collectionInfo.title}</h2>
            {collectionInfo.confidentiality === 'Private' && (
              <span className="collection-private-badge">
                <img src={LockIcon} alt="Приватная" />
                Приватная
              </span>
            )}
          </div>

          <div className="collection-actions">
            {/* Кнопки редактирования и удаления для владельца или администратора */}
            {canEditCollection && (
              <>
                <button
                  type="button"
                  onClick={() => setShowEditModal(true)}
                  className="collection-action-btn"
                  title="Редактировать коллекцию"
                >
                  <img src={Change} alt="Редактировать" />
                </button>
                <button
                  type="button"
                  onClick={handleDeleteCollection}
                  className="collection-action-btn"
                  title="Удалить коллекцию"
                >
                  <img src={Delete} alt="Удалить" />
                </button>
              </>
            )}

            {/* Кнопка лайка для авторизованных пользователей, которые не являются владельцами */}
            {isAuthenticated && !isOwner && (
              <button
                type="button"
                onClick={handleLikeToggle}
                className={`collection-action-btn like-btn ${isLiked ? 'liked' : ''}`}
                title={isLiked ? "Убрать из понравившегося" : "Добавить в понравившиеся"}
              >
                <img src={isLiked ? HeartFilled : HeartEmpty} alt="Лайк" />
                {isLiked ? 'Убрать из понравившегося' : 'Добавить в понравившиеся'}
              </button>
            )}
          </div>
        </div>

        {/* Информация о коллекции */}
        <div className="collection-info-section">
          <div className="collection-cover-container">
            <img
              src={collectionInfo.photoLink ? getImageUrl(collectionInfo.photoLink) : PlaceholderImage}
              alt="Обложка коллекции"
              className="collection-cover"
            />
          </div>

          <div className="collection-details">
            {collectionInfo.description && (
              <div className="collection-description">
                <h3>Описание</h3>
                <p>{collectionInfo.description}</p>
              </div>
            )}

            <div className="collection-meta">
              <div className="collection-meta-item">
                <span className="meta-label">Тип:</span>
                <span className="meta-value">{translateCollectionType(collectionInfo.collectionType)}</span>
              </div>

              <div className="collection-meta-item">
                <span className="meta-label">Конфиденциальность:</span>
                <span className="meta-value">{translateConfidentiality(collectionInfo.confidentiality)}</span>
              </div>

              <div className="collection-meta-item">
                <span className="meta-label">Книги:</span>
                <span className="meta-value">{formatBooksCount(collectionInfo.booksCount)}</span>
              </div>

              <div className="collection-meta-item">
                <span className="meta-label">Лайки:</span>
                <span className="meta-value">{formatLikesCount(likesCount)}</span>
              </div>

              <div className="collection-meta-item clickable" onClick={handleOwnerClick}>
                <span className="meta-label">Владелец:</span>
                <span className="meta-value owner-name">{collectionInfo.ownerNickname}</span>
              </div>

              {/* Бейдж администратора, если коллекция просматривается администратором */}
              {isAdmin && isAuthenticated && !isOwner && (
                <div className="collection-meta-item admin-view-badge">
                  <span className="meta-label">Режим:</span>
                  <span className="meta-value admin-badge">Администраторский просмотр</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Кнопка добавления книг для владельца или администратора */}
        {canEditCollection && (
          <div className="collection-buttons">
            <PrimaryButton label="Добавить книгу" onClick={() => setShowAddBookModal(true)} type="button" />
          </div>
        )}

        <div className="collection-books-section">
          <h3 className="books-section-title">Книги в коллекции</h3>

          {books.length > 0 ? (
            <>
              <div className="collection-books-list">
                {books.map((book) => (
                  <div key={book.id} className="collection-book-item">
                    {/* Кнопка удаления книги для владельца или администратора */}
                    {canEditCollection && (
                      <button
                        type="button"
                        className="delete-book-button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteBook(book.id);
                        }}
                      >
                        <img src={Delete} alt="Удалить из коллекции" />
                      </button>
                    )}
                    <div onClick={() => handleBookClick(book)}>
                      <CardElement
                        title={book.title}
                        description={book.author}
                        starsCount={book.rating}
                        imageUrl={book.imageUrl}
                        button={false}
                        starsSize="small"
                      />
                    </div>
                  </div>
                ))}
              </div>

              {hasMore && (
                <div className="load-more-container">
                  <PrimaryButton
                    label="Загрузить еще"
                    onClick={loadMoreBooks}
                  />
                </div>
              )}
            </>
          ) : (
            <p className="no-books-message">В этой коллекции пока нет книг</p>
          )}
        </div>
      </div>

      {/* Модальное окно подтверждения удаления коллекции */}
      <Modal open={showDeleteDialog} onClose={() => setShowDeleteDialog(false)}>
        <ConfirmDialog
          title="Удаление коллекции"
          message="Вы уверены, что хотите удалить эту коллекцию? Все книги будут удалены из коллекции, но останутся в системе."
          onConfirm={confirmDeleteCollection}
          onCancel={() => setShowDeleteDialog(false)}
        />
      </Modal>

      {/* Модальное окно подтверждения удаления книги */}
      <Modal open={showDeleteBookDialog} onClose={() => setShowDeleteBookDialog(false)}>
        <ConfirmDialog
          title="Удаление книги из коллекции"
          message="Вы уверены, что хотите удалить эту книгу из коллекции?"
          onConfirm={confirmDeleteBook}
          onCancel={() => setShowDeleteBookDialog(false)}
        />
      </Modal>

      {/* Модальное окно добавления книг */}
      {showAddBookModal && (
        <Modal open={showAddBookModal} onClose={() => setShowAddBookModal(false)}>
          <BookSearchModal
            collectionId={collectionId}
            onClose={() => setShowAddBookModal(false)}
            onBooksAdded={handleBooksAdded}
            isAdmin={isAdmin && isAuthenticated} // Добавляем проп isAdmin
          />
        </Modal>
      )}

      {/* Модальное окно редактирования коллекции */}
      {collectionInfo && (
        <EditCollectionModal
          open={showEditModal}
          onClose={() => setShowEditModal(false)}
          collection={collectionInfo}
          onCollectionUpdated={handleCollectionUpdated}
          isAdmin={isAdmin && isAuthenticated} // Добавляем проп isAdmin
        />
      )}
    </main>
  );
}

export default Collection;