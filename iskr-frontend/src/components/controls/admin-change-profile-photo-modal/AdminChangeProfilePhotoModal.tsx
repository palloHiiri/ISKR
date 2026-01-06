import { useState, useRef, ChangeEvent } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import './AdminChangeProfilePhotoModal.scss';
import profileAPI from '../../../api/profileService';

interface AdminChangeProfilePhotoModalProps {
  open: boolean;
  onClose: () => void;
  currentImageUrl: string;
  onSuccess: () => void;
  targetUserId: number;
}

function AdminChangeProfilePhotoModal({ open, onClose, currentImageUrl, onSuccess, targetUserId }: AdminChangeProfilePhotoModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Проверяем тип файла
    const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'];
    if (!validTypes.includes(file.type)) {
      setError('Неподдерживаемый формат файла. Допустимы: JPEG, PNG, GIF, WebP, BMP');
      return;
    }

    // Проверяем размер файла (например, максимум 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('Файл слишком большой. Максимальный размер: 5MB');
      return;
    }

    setSelectedFile(file);
    setError(null);
    setSuccessMessage(null);

    // Создаем превью
    const reader = new FileReader();
    reader.onload = () => {
      setPreviewUrl(reader.result as string);
    };
    reader.readAsDataURL(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Выберите файл для загрузки');
      return;
    }

    setUploading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      // 1. Загружаем изображение на сервер
      const uploadResponse = await profileAPI.uploadImage(selectedFile);
      
      // Проверяем структуру ответа
      const uploadResult = uploadResponse.data || uploadResponse;
      
      if (uploadResult.state === 'OK') {
        const imageId = uploadResult.key?.imglId;
        
        if (!imageId) {
          setError('Не удалось получить ID загруженного изображения');
          return;
        }
        
        // 2. Обновляем фото профиля с полученным imageId (админский метод)
        const changeResponse = await profileAPI.changeProfileImageAdmin(targetUserId, imageId);
        
        // Проверяем структуру ответа
        const changeResult = changeResponse.data || changeResponse;
        
        if (changeResult.state === 'OK') {
          setSuccessMessage('Фото профиля успешно изменено');
          setTimeout(() => {
            onSuccess();
            handleClose();
          }, 1500);
        } else {
          setError(changeResult.message || 'Ошибка при обновлении фото профиля');
        }
      } else {
        setError(uploadResult.message || 'Ошибка при загрузке изображения');
      }
    } catch (err: any) {
      console.error('Error uploading profile photo (admin):', err);
      
      // Обработка ошибок API
      if (err.response?.data?.state === 'Fail_BadData') {
        setError(err.response.data.message || 'Неподдерживаемый формат файла');
      } else if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err.message) {
        setError(err.message);
      } else {
        setError('Ошибка при загрузке фото профиля');
      }
    } finally {
      setUploading(false);
    }
  };

  const handleClose = () => {
    setSelectedFile(null);
    setPreviewUrl(null);
    setError(null);
    setSuccessMessage(null);
    setUploading(false);
    onClose();
  };

  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };

  return (
    <Modal open={open} onClose={handleClose}>
      <div className="admin-change-profile-photo-modal">
        <h2 className="admin-modal-title">Смена фото профиля (Админ)</h2>
        
        <div className="admin-current-info">
          <span className="admin-info-label">ID пользователя:</span>
          <span className="admin-info-value">{targetUserId}</span>
        </div>
        
        <div className="admin-photo-preview-container">
          <div className="admin-current-photo">
            <span className="admin-preview-label">Текущее фото:</span>
            <img src={currentImageUrl} alt="Текущее фото профиля" className="admin-preview-image" />
          </div>
          
          {previewUrl && (
            <div className="admin-new-photo">
              <span className="admin-preview-label">Новое фото:</span>
              <img src={previewUrl} alt="Новое фото профиля" className="admin-preview-image" />
            </div>
          )}
        </div>
        
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileSelect}
          accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
          style={{ display: 'none' }}
        />
        
        <button 
          className="admin-select-file-button"
          onClick={triggerFileInput}
          disabled={uploading}
        >
          Выбрать файл
        </button>
        
        {selectedFile && (
          <div className="admin-file-info">
            <span className="admin-file-name">{selectedFile.name}</span>
            <span className="admin-file-size">{(selectedFile.size / 1024).toFixed(2)} KB</span>
          </div>
        )}
        
        {error && (
          <div className="admin-error-message">
            <span className="admin-error-icon">⚠</span>
            <span>{error}</span>
          </div>
        )}
        
        {successMessage && (
          <div className="admin-success-message">
            <span className="admin-success-icon">✓</span>
            <span>{successMessage}</span>
          </div>
        )}
        
        <div className="admin-modal-actions">
          <SecondaryButton 
            label="Отмена" 
            onClick={handleClose} 
            disabled={uploading}
          />
          <PrimaryButton 
            label={uploading ? "Загрузка..." : "Сохранить"} 
            onClick={handleUpload} 
            disabled={uploading || !selectedFile}
          />
        </div>
      </div>
    </Modal>
  );
}

export default AdminChangeProfilePhotoModal;