// /src/components/controls/edit-collection-modal/EditCollectionModal.tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import Input from '../input/Input';
import './EditCollectionModal.scss';
import CoverSelectModal from '../cover-select-modal/CoverSelectModal';
import EditAccessModal from '../edit-access-modal/EditAccessModal';
import { useSelector } from 'react-redux';
import type { RootState } from '../../../redux/store';
import collectionAPI, { type CollectionInfo, type CreateCollectionData } from '../../../api/collectionService';
import { getImageUrl } from '../../../api/popularService';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import LockIcon from '../../../assets/elements/lock.svg';

interface EditCollectionModalProps {
  open: boolean;
  onClose: () => void;
  collection: CollectionInfo;
  onCollectionUpdated?: (collection: CollectionInfo) => void;
  isAdmin?: boolean; // Добавляем проп isAdmin
}

type ActiveSubModal = 'cover' | 'confidentiality' | 'access' | null;

function EditCollectionModal({ open, onClose, collection, onCollectionUpdated, isAdmin = false }: EditCollectionModalProps) {
  const navigate = useNavigate();
  const currentUser = useSelector((state: RootState) => state.auth.user);
  
  // Основные поля
  const [title, setTitle] = useState(collection.title);
  const [description, setDescription] = useState(collection.description);
  const [confidentiality, setConfidentiality] = useState(collection.confidentiality);
  
  // Обложка
  const [selectedCoverId, setSelectedCoverId] = useState<number | null>(collection.photoLink?.imglId || null);
  const [coverPreviewUrl, setCoverPreviewUrl] = useState<string | null>(
    collection.photoLink ? getImageUrl(collection.photoLink) : PlaceholderImage
  );
  
  // Активное вложенное модальное окно
  const [activeSubModal, setActiveSubModal] = useState<ActiveSubModal>(null);
  
  // Состояния
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // Сброс формы при открытии
  useEffect(() => {
    if (open) {
      setTitle(collection.title);
      setDescription(collection.description);
      setConfidentiality(collection.confidentiality);
      setSelectedCoverId(collection.photoLink?.imglId || null);
      setCoverPreviewUrl(collection.photoLink ? getImageUrl(collection.photoLink) : PlaceholderImage);
      setError(null);
      setValidationErrors({});
    }
  }, [open, collection]);

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

  const handleUpdateCollection = async () => {
    if (!validateForm()) return;

    setLoading(true);
    setError(null);

    try {
      const updateData: Partial<CreateCollectionData> = {
        title: title.trim(),
        description: description.trim() || '',
        confidentiality: confidentiality as 'Public' | 'Private'
      };

      // Добавляем photoLink только если он изменился
      if (selectedCoverId !== collection.photoLink?.imglId) {
        updateData.photoLink = selectedCoverId;
      }

      // Используем административный метод для администраторов
      let updatedCollection: CollectionInfo;
      if (isAdmin) {
        updatedCollection = await collectionAPI.updateCollectionAdmin(collection.collectionId, updateData);
      } else {
        updatedCollection = await collectionAPI.updateCollection(collection.collectionId, updateData);
      }
      
      // Вызываем callback если передан
      if (onCollectionUpdated) {
        onCollectionUpdated(updatedCollection);
      }
      
      // Закрываем модальное окно
      onClose();
      
    } catch (err: any) {
      console.error('Error updating collection:', err);
      
      if (err.response?.data?.data?.details) {
        const errorDetails = err.response.data.data.details;
        setError(errorDetails.message || 'Ошибка обновления коллекции');
      } else if (err.message) {
        setError(err.message);
      } else {
        setError('Ошибка обновления коллекции');
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

  const handleDeleteCollection = async () => {
    try {
      setLoading(true);
      
      // Используем административный метод для администраторов
      if (isAdmin) {
        await collectionAPI.deleteCollectionAdmin(collection.collectionId);
      } else {
        await collectionAPI.deleteCollection(collection.collectionId);
      }
      
      onClose();
      // Перенаправляем на корень (/)
      navigate('/library');
    } catch (err: any) {
      console.error('Error deleting collection:', err);
      setError(err.message || 'Ошибка удаления коллекции');
    } finally {
      setLoading(false);
    }
  };

  const renderMainContent = () => (
    <>
      <div className="modal-header">
        <h2 className="modal-title">Редактирование коллекции</h2>
      </div>

      <div className="edit-collection-content">
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
          <div className="cover-actions">
            <button
              type="button"
              className="change-cover-btn"
              onClick={() => setActiveSubModal('cover')}
              disabled={loading}
            >
              Изменить обложку
            </button>
            {coverPreviewUrl !== PlaceholderImage && (
              <button
                type="button"
                className="remove-cover-btn"
                onClick={() => {
                  setSelectedCoverId(null);
                  setCoverPreviewUrl(PlaceholderImage);
                }}
                disabled={loading}
              >
                Удалить обложку
              </button>
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
              Конфиденциальность
            </label>
            <div className="confidentiality-options">
              <label className="confidentiality-option">
                <input
                  type="radio"
                  name="confidentiality"
                  value="Public"
                  checked={confidentiality === 'Public'}
                  onChange={(e) => setConfidentiality(e.target.value)}
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
                  onChange={(e) => setConfidentiality(e.target.value)}
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
        <div className="left-actions">
          <button
            type="button"
            className="edit-access-btn"
            onClick={() => setActiveSubModal('access')}
            disabled={loading}
            title="Редактировать доступ"
          >
            <img src={LockIcon} alt="Доступ" />
            Редактировать доступ
          </button>
        </div>
        
        <div className="right-actions">
          <button
            type="button"
            className="delete-collection-btn"
            onClick={handleDeleteCollection}
            disabled={loading}
          >
            Удалить коллекцию
          </button>
          <div className="action-buttons">
            <SecondaryButton
              label="Отмена"
              onClick={onClose}
              disabled={loading}
            />
            <PrimaryButton
              label={loading ? "Сохранение..." : "Сохранить изменения"}
              onClick={handleUpdateCollection}
              disabled={loading}
            />
          </div>
        </div>
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
      case 'access':
        return (
          <EditAccessModal
            open={true}
            onClose={handleSubModalClose}
            collectionId={collection.collectionId}
            collectionTitle={collection.title}
            isAdmin={isAdmin} // Передаем isAdmin в EditAccessModal
          />
        );
      default:
        return renderMainContent();
    }
  };

  return (
    <Modal open={open} onClose={onClose}>
      <div className="edit-collection-modal">
        {renderContent()}
      </div>
    </Modal>
  );
}

export default EditCollectionModal;