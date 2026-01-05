import { useState, useEffect } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import Input from '../input/Input';
import './CreateCollectionModal.scss';
import CoverSelectModal from '../cover-select-modal/CoverSelectModal';
import { useSelector } from 'react-redux';
import type { RootState } from '../../../redux/store';
import collectionAPI, { type CreateCollectionData } from '../../../api/collectionService';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import wishlistService from '../../../api/wishlistService';

interface CreateCollectionModalProps {
  open: boolean;
  onClose: () => void;
  onCollectionCreated?: (collection: any) => void;
}

type ActiveSubModal = 'cover' | null;

function CreateCollectionModal({ open, onClose, onCollectionCreated }: CreateCollectionModalProps) {
  const currentUser = useSelector((state: RootState) => state.auth.user);
  
  // Основные поля
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [confidentiality, setConfidentiality] = useState<'Public' | 'Private'>('Public');
  const [collectionType, setCollectionType] = useState<'Standard' | 'Wishlist' | 'Liked'>('Standard');
  
  // Обложка
  const [selectedCoverId, setSelectedCoverId] = useState<number | null>(null);
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(null);
  
  // Активное вложенное модальное окно
  const [activeSubModal, setActiveSubModal] = useState<ActiveSubModal>(null);
  
  // Состояния
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [hasWishlist, setHasWishlist] = useState<boolean>(false);
  const [checkingWishlist, setCheckingWishlist] = useState(true);

  // Проверка наличия вишлиста при открытии
  useEffect(() => {
    if (open) {
      checkExistingWishlist();
    }
  }, [open]);

  // Сброс формы при открытии
  useEffect(() => {
    if (open) {
      setTitle('');
      setDescription('');
      setConfidentiality('Public');
      setCollectionType('Standard');
      setSelectedCoverId(null);
      setCoverPreviewUrl(null);
      setActiveSubModal(null);
      setError(null);
      setValidationErrors({});
    }
  }, [open]);

  const checkExistingWishlist = async () => {
    setCheckingWishlist(true);
    try {
      const wishlistInfo = await wishlistService.checkWishlist();
      setHasWishlist(wishlistInfo.hasWishlist);
    } catch (error) {
      console.error('Error checking wishlist:', error);
      setHasWishlist(false);
    } finally {
      setCheckingWishlist(false);
    }
  };

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};
    
    if (!title.trim()) {
      errors.title = 'Название коллекции обязательно';
    } else if (title.length > 40) {
      errors.title = 'Название не должно превышать 40 символов';
    }
    
    if (description.length > 1000) {
      errors.description = 'Описание не должно превышать 1000 символов';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateCollection = async () => {
    if (!validateForm()) return;
    if (!currentUser?.id) {
      setError('Пользователь не авторизован');
      return;
    }

    // Проверяем, можно ли создать вишлист
    if (collectionType === 'Wishlist' && hasWishlist) {
      setError('У вас уже есть вишлист. Вишлист можно создать только один раз.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const collectionData: CreateCollectionData = {
        title: title.trim(),
        description: description.trim() || '',
        confidentiality,
        collectionType: collectionType,
        photoLink: selectedCoverId || null
      };

      const createdCollection = await collectionAPI.createCollection(collectionData);
      
      // Закрываем модальное окно
      onClose();
      
      // Вызываем callback если передан
      if (onCollectionCreated) {
        onCollectionCreated(createdCollection);
      }
      
    } catch (err: any) {
      console.error('Error creating collection:', err);
      
      if (err.response?.data?.data?.details) {
        const errorDetails = err.response.data.data.details;
        setError(errorDetails.message || 'Ошибка создания коллекции');
      } else if (err.message) {
        setError(err.message);
      } else {
        setError('Ошибка создания коллекции');
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

  const renderMainContent = () => (
    <>
      <h2 className="modal-title">Создание коллекции</h2>

      <div className="create-collection-content">
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
        </div>

        {/* Правая колонка - информация */}
        <div className="info-section">
          <div className="form-group">
            <label htmlFor="title" className="form-label required">
              Название
            </label>
            <input
              id="title"
              type="text"
              className={`form-input ${validationErrors.title ? 'error' : ''}`}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={loading}
              placeholder="Название коллекции"
              maxLength={40}
            />
            <div className="character-count">
              {title.length}/40 символов
            </div>
            {validationErrors.title && (
              <div className="field-error">{validationErrors.title}</div>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="description" className="form-label">
              Описание
            </label>
            <textarea
              id="description"
              className={`form-textarea ${validationErrors.description ? 'error' : ''}`}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={loading}
              rows={4}
              placeholder="Описание коллекции"
              maxLength={1000}
            />
            <div className="character-count">
              {description.length}/1000 символов
            </div>
            {validationErrors.description && (
              <div className="field-error">{validationErrors.description}</div>
            )}
          </div>

          <div className="form-group">
            <label className="form-label required">
              Тип коллекции
            </label>
            <div className="collection-type-options">
              <label className="collection-type-option">
                <input
                  type="radio"
                  name="collectionType"
                  value="Standard"
                  checked={collectionType === 'Standard'}
                  onChange={(e) => setCollectionType(e.target.value as 'Standard' | 'Wishlist' | 'Liked')}
                  disabled={loading}
                />
                <span className="option-label">
                  <span className="option-title">Обычная коллекция</span>
                  <span className="option-description">Стандартная коллекция для ваших книг</span>
                </span>
              </label>
              <label className="collection-type-option">
                <input
                  type="radio"
                  name="collectionType"
                  value="Wishlist"
                  checked={collectionType === 'Wishlist'}
                  onChange={(e) => setCollectionType(e.target.value as 'Standard' | 'Wishlist' | 'Liked')}
                  disabled={loading || hasWishlist}
                />
                <span className="option-label">
                  <span className="option-title">Вишлист</span>
                  <span className="option-description">Список книг, которые вы хотите прочитать</span>
                  {hasWishlist && (
                    <span className="option-hint">(У вас уже есть вишлист)</span>
                  )}
                </span>
              </label>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label required">
              Конфиденциальность
            </label>
            <div className="confidentiality-options">
              <label className="confidentiality-option">
                <input
                  type="radio"
                  name="confidentiality"
                  value="Public"
                  checked={confidentiality === 'Public'}
                  onChange={(e) => setConfidentiality(e.target.value as 'Public' | 'Private')}
                  disabled={loading}
                />
                <span className="option-label">
                  <span className="option-title">Публичная</span>
                  <span className="option-description">Коллекцию могут просматривать все пользователи</span>
                </span>
              </label>
              <label className="confidentiality-option">
                <input
                  type="radio"
                  name="confidentiality"
                  value="Private"
                  checked={confidentiality === 'Private'}
                  onChange={(e) => setConfidentiality(e.target.value as 'Public' | 'Private')}
                  disabled={loading}
                />
                <span className="option-label">
                  <span className="option-title">Приватная</span>
                  <span className="option-description">Коллекцию можете просматривать только вы</span>
                </span>
              </label>
            </div>
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
          label={loading ? "Создание..." : "Создать коллекцию"}
          onClick={handleCreateCollection}
          disabled={loading || checkingWishlist}
        />
      </div>

      {checkingWishlist && (
        <div className="loading-message">
          <p>Проверка наличия вишлиста...</p>
        </div>
      )}
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
      default:
        return renderMainContent();
    }
  };

  return (
    <Modal open={open} onClose={onClose}>
      <div className="create-collection-modal">
        {renderContent()}
      </div>
    </Modal>
  );
}

export default CreateCollectionModal;