import { useState, useEffect } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import './AdminChangeDescriptionModal.scss';
import profileAPI from '../../../api/profileService';

interface AdminChangeDescriptionModalProps {
  open: boolean;
  onClose: () => void;
  currentDescription: string | null;
  onSuccess: (newDescription: string) => void;
  targetUserId: number;
}

function AdminChangeDescriptionModal({ open, onClose, currentDescription, onSuccess, targetUserId }: AdminChangeDescriptionModalProps) {
  const [description, setDescription] = useState(currentDescription || '');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setDescription(currentDescription || '');
  }, [currentDescription]);

  const handleSubmit = async () => {
    if (description.length > 1024) {
      setError('Описание не должно превышать 1024 символа');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await profileAPI.changeProfileDescriptionAdmin(targetUserId, description);
      const result = response.data || response;

      if (result.state === 'OK') {
        setSuccess('Описание профиля успешно изменено');
        setTimeout(() => {
          onSuccess(description);
          handleClose();
        }, 1500);
      } else {
        setError(result.message || 'Ошибка при изменении описания');
      }
    } catch (err: any) {
      console.error('Error changing description (admin):', err);
      setError(err.response?.data?.message || err.message || 'Ошибка при изменении описания');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setDescription(currentDescription || '');
    setError(null);
    setSuccess(null);
    setLoading(false);
    onClose();
  };

  const charactersLeft = 1024 - description.length;

  return (
    <Modal open={open} onClose={handleClose}>
      <div className="admin-change-description-modal">
        <h2 className="admin-modal-title">Редактирование описания профиля (Админ)</h2>

        <div className="admin-current-info">
          <span className="admin-info-label">ID пользователя:</span>
          <span className="admin-info-value">{targetUserId}</span>
        </div>

        <div className="admin-input-group">
          <label htmlFor="admin-description" className="admin-input-label">
            Описание профиля
          </label>
          <textarea
            id="admin-description"
            className="admin-description-input"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Редактировать описание профиля пользователя"
            disabled={loading}
            rows={6}
            maxLength={1024}
          />
          <div className="admin-character-counter">
            {charactersLeft} символов осталось
          </div>
        </div>

        {error && (
          <div className="admin-error-message">
            <span className="admin-error-icon">⚠</span>
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="admin-success-message">
            <span className="admin-success-icon">✓</span>
            <span>{success}</span>
          </div>
        )}

        <div className="admin-modal-actions">
          <SecondaryButton
            label="Отмена"
            onClick={handleClose}
            disabled={loading}
          />
          <PrimaryButton
            label={loading ? "Сохранение..." : "Сохранить"}
            onClick={handleSubmit}
            disabled={loading || description === (currentDescription || '')}
          />
        </div>
      </div>
    </Modal>
  );
}

export default AdminChangeDescriptionModal;