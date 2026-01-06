import { useState } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import './AdminChangePasswordModal.scss';
import profileAPI from '../../../api/profileService';

interface AdminChangePasswordModalProps {
  open: boolean;
  onClose: () => void;
  targetUserId: number;
}

function AdminChangePasswordModal({ open, onClose, targetUserId }: AdminChangePasswordModalProps) {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!newPassword.trim()) {
      setError('Введите новый пароль');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Пароли не совпадают');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await profileAPI.changePasswordAdmin(targetUserId, newPassword.trim());
      const result = response.data || response;

      if (result.state === 'OK') {
        setSuccess('Пароль успешно изменен');
        setTimeout(() => {
          handleClose();
        }, 1500);
      } else {
        setError(result.message || 'Ошибка при изменении пароля');
      }
    } catch (err: any) {
      console.error('Error changing password (admin):', err);
      setError(err.response?.data?.message || err.message || 'Ошибка при изменении пароля');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setNewPassword('');
    setConfirmPassword('');
    setError(null);
    setSuccess(null);
    setLoading(false);
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose}>
      <div className="admin-change-password-modal">
        <h2 className="admin-modal-title">Смена пароля (Админ)</h2>

        <div className="admin-current-info">
          <span className="admin-info-label">ID пользователя:</span>
          <span className="admin-info-value">{targetUserId}</span>
        </div>

        <div className="admin-input-group">
          <label htmlFor="admin-new-password" className="admin-input-label">
            Новый пароль
          </label>
          <input
            id="admin-new-password"
            type="password"
            className="admin-password-input"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="Введите новый пароль"
            disabled={loading}
            autoComplete="new-password"
          />
        </div>

        <div className="admin-input-group">
          <label htmlFor="admin-confirm-password" className="admin-input-label">
            Подтверждение пароля
          </label>
          <input
            id="admin-confirm-password"
            type="password"
            className="admin-password-input"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Повторите новый пароль"
            disabled={loading}
            autoComplete="new-password"
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
            disabled={loading || !newPassword.trim() || !confirmPassword.trim()}
          />
        </div>
      </div>
    </Modal>
  );
}

export default AdminChangePasswordModal;