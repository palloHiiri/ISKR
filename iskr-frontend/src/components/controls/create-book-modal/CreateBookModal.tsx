import { useState, useEffect } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import './CreateBookModal.scss';
import CoverSelectModal from '../cover-select-modal/CoverSelectModal';
import AuthorsSelectModal from '../authors-select-modal/AuthorsSelectModal';
import GenresSelectModal from '../genres-select-modal/GenresSelectModal';
import { useSelector } from 'react-redux';
import type { RootState } from '../../../redux/store';
import bookAPI from '../../../api/bookService';
import { useNavigate } from 'react-router-dom';

interface CreateBookModalProps {
  open: boolean;
  onClose: () => void;
}

interface Author {
  authorId: number;
  name: string;
  realName?: string;
}

interface Genre {
  genreId: number;
  name: string;
}

// Тип для активного вложенного модального окна
type ActiveSubModal = 'cover' | 'authors' | 'genres' | null;

function CreateBookModal({ open, onClose }: CreateBookModalProps) {
  const currentUser = useSelector((state: RootState) => state.auth.user);
  const navigate = useNavigate();
  
  // Основные поля
  const [title, setTitle] = useState('');
  const [subtitle, setSubtitle] = useState('');
  const [isbn, setIsbn] = useState('');
  const [pageCnt, setPageCnt] = useState('');
  const [description, setDescription] = useState('');
  
  // Выбранные элементы
  const [selectedAuthors, setSelectedAuthors] = useState<Author[]>([]);
  const [selectedGenres, setSelectedGenres] = useState<Genre[]>([]);
  const [selectedCoverId, setSelectedCoverId] = useState<number | null>(null);
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(null);
  
  // Активное вложенное модальное окно
  const [activeSubModal, setActiveSubModal] = useState<ActiveSubModal>(null);
  
  // Состояния
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // Сброс формы при открытии
  useEffect(() => {
    if (open) {
      setTitle('');
      setSubtitle('');
      setIsbn('');
      setPageCnt('');
      setDescription('');
      setSelectedAuthors([]);
      setSelectedGenres([]);
      setSelectedCoverId(null);
      setCoverPreviewUrl(null);
      setActiveSubModal(null);
      setError(null);
      setValidationErrors({});
    }
  }, [open]);

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};
    
    if (!title.trim()) {
      errors.title = 'Заголовок обязателен';
    }
    
    const pageCntNum = parseInt(pageCnt, 10);
    if (!pageCnt.trim()) {
      errors.pageCnt = 'Количество страниц обязательно';
    } else if (isNaN(pageCntNum) || pageCntNum <= 0) {
      errors.pageCnt = 'Количество страниц должно быть положительным числом';
    }
    
    if (selectedAuthors.length === 0) {
      errors.authors = 'Выберите хотя бы одного автора';
    }
    
    if (selectedGenres.length === 0) {
      errors.genres = 'Выберите хотя бы один жанр';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateBook = async () => {
    if (!validateForm()) return;
    if (!currentUser?.id) {
      setError('Пользователь не авторизован');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const bookData = {
        title: title.trim(),
        subtitle: subtitle.trim() || null,
        description: description.trim() || null,
        isbn: isbn.trim() || null,
        pageCnt: parseInt(pageCnt, 10),
        addedBy: Number(currentUser.id),
        authorIds: selectedAuthors.map(a => a.authorId),
        genreIds: selectedGenres.map(g => g.genreId),
        photoLink: selectedCoverId || null
      };

      const createdBook = await bookAPI.createBook(bookData);
      
      // Закрываем модальное окно
      onClose();
      
      // Переходим на страницу созданной книги
      navigate('/book', {
        state: { id: createdBook.bookId }
      });
    } catch (err: any) {
      console.error('Error creating book:', err);
      
      if (err.response?.data?.data?.details) {
        const errorDetails = err.response.data.data.details;
        if (errorDetails.state === 'Fail_Conflict') {
          if (errorDetails.message === 'Book with this ISBN already exists') {
            setError('Книга с таким ISBN уже существует');
          } else if (errorDetails.message === 'A book with this title and subtitle combination already exists') {
            setError('Книга с таким заголовком и подзаголовком уже существует');
          } else {
            setError(errorDetails.message || 'Ошибка создания книги');
          }
        } else {
          setError(errorDetails.message || 'Ошибка создания книги');
        }
      } else if (err.message) {
        setError(err.message);
      } else {
        setError('Ошибка создания книги');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCoverSelect = (coverId: number, previewUrl: string) => {
    setSelectedCoverId(coverId);
    setCoverPreviewUrl(previewUrl);
    setActiveSubModal(null);
  };

  const handleAuthorsSelect = (authors: Author[]) => {
    setSelectedAuthors(authors);
    setActiveSubModal(null);
  };

  const handleGenresSelect = (genres: Genre[]) => {
    setSelectedGenres(genres);
    setActiveSubModal(null);
  };

  const renderMainContent = () => (
    <>
      <h2 className="modal-title">Создание новой книги</h2>

      <div className="create-book-content">
        {/* Левая колонка - обложка */}
        <div className="cover-section">
          <div 
            className={`cover-preview ${coverPreviewUrl ? 'has-image' : 'empty'}`}
            onClick={() => setActiveSubModal('cover')}
          >
            {coverPreviewUrl ? (
              <img src={coverPreviewUrl} alt="Обложка" />
            ) : (
              <div className="cover-placeholder">
                <span>Выбрать обложку</span>
              </div>
            )}
          </div>
          {validationErrors.cover && (
            <div className="field-error">{validationErrors.cover}</div>
          )}
        </div>

        {/* Правая колонка - информация */}
        <div className="info-section">
          <div className="form-group">
            <label htmlFor="title" className="form-label required">
              Заголовок *
            </label>
            <input
              id="title"
              type="text"
              className={`form-input ${validationErrors.title ? 'error' : ''}`}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={loading}
              placeholder="Название книги"
            />
            {validationErrors.title && (
              <div className="field-error">{validationErrors.title}</div>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="subtitle" className="form-label">
              Подзаголовок
            </label>
            <input
              id="subtitle"
              type="text"
              className="form-input"
              value={subtitle}
              onChange={(e) => setSubtitle(e.target.value)}
              disabled={loading}
              placeholder="Необязательно"
            />
          </div>

          <div className="form-group">
            <label htmlFor="isbn" className="form-label">
              ISBN
            </label>
            <input
              id="isbn"
              type="text"
              className="form-input"
              value={isbn}
              onChange={(e) => setIsbn(e.target.value)}
              disabled={loading}
              placeholder="Необязательно"
            />
          </div>

          <div className="form-group">
            <label htmlFor="pageCnt" className="form-label required">
              Количество страниц *
            </label>
            <input
              id="pageCnt"
              type="number"
              className={`form-input ${validationErrors.pageCnt ? 'error' : ''}`}
              value={pageCnt}
              onChange={(e) => setPageCnt(e.target.value)}
              disabled={loading}
              min="1"
              placeholder="Например: 300"
            />
            {validationErrors.pageCnt && (
              <div className="field-error">{validationErrors.pageCnt}</div>
            )}
          </div>

          {/* Кнопки выбора авторов и жанров */}
          <div className="selection-buttons">
            <div className="selection-group">
              <label className="selection-label required">
                Авторы
                {selectedAuthors.length > 0 && (
                  <span className="selection-count">({selectedAuthors.length})</span>
                )}
              </label>
              <button
                type="button"
                className={`selection-button ${validationErrors.authors ? 'error' : ''}`}
                onClick={() => setActiveSubModal('authors')}
                disabled={loading}
              >
                {selectedAuthors.length > 0 
                  ? selectedAuthors.map(a => a.name).join(', ')
                  : 'Выбрать авторов'}
              </button>
              {validationErrors.authors && (
                <div className="field-error">{validationErrors.authors}</div>
              )}
            </div>

            <div className="selection-group">
              <label className="selection-label required">
                Жанры
                {selectedGenres.length > 0 && (
                  <span className="selection-count">({selectedGenres.length})</span>
                )}
              </label>
              <button
                type="button"
                className={`selection-button ${validationErrors.genres ? 'error' : ''}`}
                onClick={() => setActiveSubModal('genres')}
                disabled={loading}
              >
                {selectedGenres.length > 0 
                  ? selectedGenres.map(g => g.name).join(', ')
                  : 'Выбрать жанры'}
              </button>
              {validationErrors.genres && (
                <div className="field-error">{validationErrors.genres}</div>
              )}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="description" className="form-label">
              Описание
            </label>
            <textarea
              id="description"
              className="form-textarea"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={loading}
              rows={4}
              placeholder="Описание книги"
            />
          </div>
        </div>
      </div>

      {error && (
        <div className="error-message">
          <span className="error-icon">⚠</span>
          <span>{error}</span>
        </div>
      )}

      <div className="modal-actions">
        <SecondaryButton
          label="Отмена"
          onClick={onClose}
          disabled={loading}
        />
        <PrimaryButton
          label={loading ? "Создание..." : "Создать книгу"}
          onClick={handleCreateBook}
          disabled={loading}
        />
      </div>
    </>
  );

  const handleSubModalClose = () => {
    setActiveSubModal(null);
  };

  // Рендерим контент в зависимости от активного окна
  const renderContent = () => {
    switch (activeSubModal) {
      case 'cover':
        return (
          <CoverSelectModal
            onSelect={handleCoverSelect}
            onClose={handleSubModalClose}
            goBack={handleSubModalClose}
          />
        );
      case 'authors':
        return (
          <AuthorsSelectModal
            onSelect={handleAuthorsSelect}
            onClose={handleSubModalClose}
            goBack={handleSubModalClose}
            initialSelected={selectedAuthors}
          />
        );
      case 'genres':
        return (
          <GenresSelectModal
            onSelect={handleGenresSelect}
            onClose={handleSubModalClose}
            goBack={handleSubModalClose}
            initialSelected={selectedGenres}
          />
        );
      default:
        return renderMainContent();
    }
  };

  return (
    <Modal open={open} onClose={onClose}>
      <div className="create-book-modal">
        {renderContent()}
      </div>
    </Modal>
  );
}

export default CreateBookModal;