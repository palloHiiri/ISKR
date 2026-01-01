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
import BookListModal from '../../controls/book-list-modal/BookListModal.tsx';
import Change from '../../../assets/elements/change-pink.svg';
import Delete from '../../../assets/elements/delete-pink.svg';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import { collectionAPI, type CollectionInfo, type CollectionBook } from '../../../api/collectionService';
import { getImageUrl, getBookImageUrl, formatRating } from '../../../api/popularService';
import { russianLocalWordConverter } from '../../../utils/russianLocalWordConverter.ts';
import LockIcon from '../../../assets/elements/lock.svg'; // Добавим иконку замка

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
  
  const [isEditMode, setEditMode] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showDeleteBookDialog, setShowDeleteBookDialog] = useState(false);
  const [showBookListModal, setShowBookListModal] = useState(false);
  const [selectedBookId, setSelectedBookId] = useState<string | null>(null);
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [collectionInfo, setCollectionInfo] = useState<CollectionInfo | null>(null);
  const [books, setBooks] = useState<BookCardData[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [isForbidden, setIsForbidden] = useState(false); // Новое состояние для 403 ошибки

  const collectionId = parseInt(location.state?.id || '0');

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

        // Загружаем информацию о коллекции и книги параллельно
        const [collectionData, booksData] = await Promise.all([
          collectionAPI.getCollection(collectionId),
          collectionAPI.getCollectionBooks(collectionId, 12, 0)
        ]);

        setCollectionInfo(collectionData);
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
      } catch (err: any) {
        console.error('Error loading collection:', err);
        
        // Проверяем, является ли ошибка 403 (Forbidden)
        if (err.response && err.response.status === 403) {
          setIsForbidden(true);
          setError('Доступ к этой коллекции запрещен');
        } else {
          setError(err.message || 'Ошибка загрузки коллекции');
        }
      } finally {
        setLoading(false);
      }
    };

    loadCollectionData();
  }, [collectionId]);

  // Загрузка дополнительных книг (пагинация)
  const loadMoreBooks = async () => {
    if (!collectionId || !hasMore) return;

    try {
      const nextPage = currentPage + 1;
      const booksData = await collectionAPI.getCollectionBooks(collectionId, 12, nextPage);

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

  const [collectionData, setCollectionData] = useState({
    id: collectionId.toString(),
    name: '',
    isMine: false,
    coverUrl: PlaceholderImage,
  });

  const [originalData, setOriginalData] = useState(collectionData);

  // Обновляем collectionData при загрузке collectionInfo
  useEffect(() => {
    if (collectionInfo) {
      const isMine = currentUser ? collectionInfo.ownerId === currentUser.userId : false;
      
      setCollectionData({
        id: collectionInfo.collectionId.toString(),
        name: collectionInfo.title,
        isMine,
        coverUrl: collectionInfo.photoLink ? getImageUrl(collectionInfo.photoLink) : PlaceholderImage,
      });
    }
  }, [collectionInfo, currentUser]);

  const handleEditMode = (enabled: boolean) => {
    if (enabled) {
      setOriginalData(collectionData);
      setEditMode(true);
    } else {
      setCollectionData(originalData);
      setEditMode(false);
    }
  };

  const handleDeleteCollection = () => {
    setShowDeleteDialog(true);
  };

  const confirmDeleteCollection = () => {
    setShowDeleteDialog(false);
    sessionStorage.setItem('deletedCollectionId', collectionData.id);
    navigate('/library');
  };

  const handleDeleteBook = (bookId: string) => {
    setSelectedBookId(bookId);
    setShowDeleteBookDialog(true);
  };

  const confirmDeleteBook = () => {
    if (selectedBookId) {
      setBooks(prev => prev.filter(book => book.id !== selectedBookId));
    }
    setShowDeleteBookDialog(false);
    setSelectedBookId(null);
  };

  const handleAddBook = () => {
    setShowBookListModal(true);
  };

  const handleBooksSelected = (selectedBooks: BookCardData[]) => {
    setBooks(prev => [...prev, ...selectedBooks]);
    setShowBookListModal(false);
  };

  const handleBookClick = (book: BookCardData) => {
    navigate('/book', {
      state: {
        id: book.id,
        title: book.title,
        description: book.author,
        rating: book.rating,
        coverUrl: book.imageUrl,
        isMine: collectionData.isMine,
        isEditMode: false,
        originalData: book.originalData
      }
    });
  };

  const handleSaveChanges = () => {
    setEditMode(false);

    const updatedCollection = {
      id: collectionData.id,
      name: collectionData.name,
      coverUrl: collectionData.coverUrl,
    };

    sessionStorage.setItem('updatedCollection', JSON.stringify(updatedCollection));
    navigate('/library');
  };

  const handleCoverChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setCollectionData(prev => ({ ...prev, coverUrl: reader.result as string }));
      };
      reader.readAsDataURL(file);
    }
  };

  const handleOwnerClick = () => {
    if (collectionInfo) {
      navigate('/profile', {
        state: {
          userId: collectionInfo.ownerId
        }
      });
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

  // Рендер состояния "доступ запрещен"
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

  if (isForbidden) {
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
          {isEditMode ? (
            <input
              type="text"
              className="collection-title-input"
              value={collectionData.name}
              onChange={(e) => setCollectionData(prev => ({ ...prev, name: e.target.value }))}
              autoFocus
            />
          ) : (
            <h2 className="collection-title">{collectionData.name}</h2>
          )}

          {collectionData.isMine && isAuthenticated && !isEditMode && (
            <div className="collection-actions">
              <button type="button" onClick={() => handleEditMode(!isEditMode)}>
                <img src={Change} alt="Изменить" />
              </button>
            </div>
          )}
        </div>

        {/* Информация о коллекции */}
        <div className="collection-info-section">
          <div className="collection-cover-container">
            {isEditMode ? (
              <div className="collection-cover-edit">
                <img src={collectionData.coverUrl} alt="Обложка коллекции" className="collection-cover-preview" />
                <label htmlFor="cover-upload" className="cover-upload-label">
                  Изменить обложку
                </label>
                <input
                  type="file"
                  id="cover-upload"
                  accept="image/*"
                  onChange={handleCoverChange}
                  className="cover-upload-input"
                />
              </div>
            ) : (
              <img 
                src={collectionData.coverUrl} 
                alt="Обложка коллекции" 
                className="collection-cover"
              />
            )}
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
                <span className="meta-value">{formatLikesCount(collectionInfo.likesCount)}</span>
              </div>
              
              <div className="collection-meta-item clickable" onClick={handleOwnerClick}>
                <span className="meta-label">Владелец:</span>
                <span className="meta-value owner-name">{collectionInfo.ownerNickname}</span>
              </div>
            </div>
          </div>
        </div>

        {collectionData.isMine && isAuthenticated && (
          <div className="collection-buttons">
            {isEditMode ? (
              <>
                <PrimaryButton label="Сохранить изменения" onClick={handleSaveChanges} type="button" />
                <SecondaryButton label="Отмена" onClick={() => handleEditMode(false)} type="button" />
              </>
            ) : (
              <>
                <PrimaryButton label="Добавить книгу" onClick={handleAddBook} type="button" />
                <SecondaryButton label="Удалить коллекцию" onClick={handleDeleteCollection} type="button" />
              </>
            )}
          </div>
        )}

        <div className="collection-books-section">
          <h3 className="books-section-title">Книги в коллекции</h3>
          
          {books.length > 0 ? (
            <>
              <div className="collection-books-list">
                {books.map((book) => (
                  <div key={book.id} className="collection-book-item">
                    {collectionData.isMine && isAuthenticated && !isEditMode && (
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

      <Modal open={showDeleteDialog} onClose={() => setShowDeleteDialog(false)}>
        <ConfirmDialog
          title="Удаление коллекции"
          message="Вы уверены, что хотите удалить эту коллекцию?"
          onConfirm={confirmDeleteCollection}
          onCancel={() => setShowDeleteDialog(false)}
        />
      </Modal>

      <Modal open={showDeleteBookDialog} onClose={() => setShowDeleteBookDialog(false)}>
        <ConfirmDialog
          title="Удаление книги из коллекции"
          message="Вы уверены, что хотите удалить эту книгу из коллекции?"
          onConfirm={confirmDeleteBook}
          onCancel={() => setShowDeleteBookDialog(false)}
        />
      </Modal>

      <Modal open={showBookListModal} onClose={() => setShowBookListModal(false)}>
        <BookListModal
          onBooksSelected={handleBooksSelected}
          selectionMode={true}
        />
      </Modal>
    </main>
  );
}

export default Collection;