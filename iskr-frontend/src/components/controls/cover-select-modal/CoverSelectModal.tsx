import { useState, useRef } from 'react';
import SecondaryButton from '../secondary-button/SecondaryButton';
import PrimaryButton from '../primary-button/PrimaryButton';
import './CoverSelectModal.scss';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import bookAPI from '../../../api/bookService';

interface CoverSelectModalProps {
  onSelect: (coverId: number, previewUrl: string) => void;
  onClose: () => void;
  goBack: () => void;
}

function CoverSelectModal({ onSelect, onClose, goBack }: CoverSelectModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'];
    if (!validTypes.includes(file.type)) {
      setError('Неподдерживаемый формат файла. Допустимы: JPEG, PNG, GIF, WebP, BMP');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError('Файл слишком большой. Максимальный размер: 5MB');
      return;
    }

    setSelectedFile(file);
    setError(null);

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

    try {
      const uploadResponse = await bookAPI.uploadBookImage(selectedFile);
      const uploadResult = uploadResponse.data || uploadResponse;

      if (uploadResult.state === 'OK') {
        const imageId = uploadResult.key?.imglId;
        
        if (!imageId) {
          setError('Не удалось получить ID загруженного изображения');
          return;
        }

        // Вызываем onSelect с ID обложки и URL превью
        onSelect(imageId, previewUrl!);
      } else {
        setError(uploadResult.message || 'Ошибка при загрузке изображения');
      }
    } catch (err: any) {
      console.error('Error uploading cover:', err);
      
      if (err.response?.data?.state === 'Fail_BadData') {
        setError('Неподдерживаемый формат файла');
      } else if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err.message) {
        setError(err.message);
      } else {
        setError('Ошибка при загрузке обложки');
      }
    } finally {
      setUploading(false);
    }
  };

  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="cover-select-modal">
      <div className="modal-header">
        <button className="back-button" onClick={goBack}>
          ← Назад
        </button>
        <h2 className="modal-title">Выбор обложки</h2>
      </div>
      
      <div className="modal-content">
        <div className="cover-preview-container">
          <img
            src={previewUrl || PlaceholderImage}
            alt="Предпросмотр обложки"
            className="cover-preview"
          />
        </div>
        
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileSelect}
          accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
          style={{ display: 'none' }}
        />
        
        <button 
          className="select-file-button"
          onClick={triggerFileInput}
          disabled={uploading}
        >
          Выбрать файл
        </button>
        
        {selectedFile && (
          <div className="file-info">
            <span className="file-name">{selectedFile.name}</span>
            <span className="file-size">{(selectedFile.size / 1024).toFixed(2)} KB</span>
          </div>
        )}
        
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
            disabled={uploading}
          />
          <PrimaryButton 
            label={uploading ? "Загрузка..." : "Выбрать обложку"} 
            onClick={handleUpload} 
            disabled={uploading || !selectedFile}
          />
        </div>
      </div>
    </div>
  );
}

export default CoverSelectModal;