import { useState, useEffect } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import './AdminChangeNicknameModal.scss';
import profileAPI from '../../../api/profileService';

interface AdminChangeNicknameModalProps {
  open: boolean;
  onClose: () => void;
  currentNickname: string | null;
  onSuccess: (newNickname: string) => void;
  targetUserId: number;
}

function AdminChangeNicknameModal({ open, onClose, currentNickname, onSuccess, targetUserId }: AdminChangeNicknameModalProps) {
  const [nickname, setNickname] = useState(currentNickname || '');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setNickname(currentNickname || '');
  }, [currentNickname]);

  const handleSubmit = async () => {
    if (!nickname.trim()) {
      setError('Введите никнейм');
      return;
    }

    if (nickname.length > 255) {
      setError('Никнейм не должен превышать 255 символов');
      return;
    }

    if (nickname.trim() === (currentNickname || '')) {
      setError('Новый никнейм совпадает с текущим');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await profileAPI.changeNicknameAdmin(targetUserId, nickname.trim());
      const result = response.data || response;

      if (result.state === 'OK') {
        setSuccess('Никнейм успешно изменен');
        setTimeout(() => {
          onSuccess(nickname.trim());
          handleClose();
        }, 1500);
      } else {
        setError(result.message || 'Ошибка при изменении никнейма');
      }
    } catch (err: any) {
      console.error('Error changing nickname (admin):', err);
      setError(err.response?.data?.message || err.message || 'Ошибка при изменении никнейма');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setNickname(currentNickname || '');
    setError(null);
    setSuccess(null);
    setLoading(false);
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose}>
      <div className="admin-change-nickname-modal">
        <h2 className="admin-modal-title">Смена никнейма (Админ)</h2>

        <div className="admin-current-info">
          <span className="admin-info-label">ID пользователя:</span>
          <span className="admin-info-value">{targetUserId}</span>
        </div>

        <div className="admin-current-info">
          <span className="admin-info-label">Текущий никнейм:</span>
          <span className="admin-info-value">{currentNickname || 'Не установлен'}</span>
        </div>

        <div className="admin-input-group">
          <label htmlFor="admin-nickname" className="admin-input-label">
            Новый никнейм
          </label>
          <input
            id="admin-nickname"
            type="text"
            className="admin-nickname-input"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="Введите новый никнейм"
            disabled={loading}
            maxLength={255}
          />
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
            disabled={loading || !nickname.trim() || nickname.trim() === (currentNickname || '')}
          />
        </div>
      </div>
    </Modal>
  );
}

export default AdminChangeNicknameModal;