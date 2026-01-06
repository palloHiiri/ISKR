import { useState, useEffect } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import './AdminChangeEmailModal.scss';
import profileAPI from '../../../api/profileService';

interface AdminChangeEmailModalProps {
  open: boolean;
  onClose: () => void;
  currentEmail: string | null;
  targetUserId: number;
}

function AdminChangeEmailModal({ open, onClose, currentEmail, targetUserId }: AdminChangeEmailModalProps) {
  const [email, setEmail] = useState(currentEmail || '');
  const [confirmation, setConfirmation] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setEmail(currentEmail || '');
  }, [currentEmail]);

  const handleSubmit = async () => {
    if (!confirmation) {
      setError('Подтвердите, что понимаете последствия смены email');
      return;
    }

    if (!email.trim()) {
      setError('Введите email');
      return;
    }

    if (!isValidEmail(email)) {
      setError('Введите корректный email адрес');
      return;
    }

    if (email.trim() === (currentEmail || '')) {
      setError('Новый email совпадает с текущим');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await profileAPI.changeEmailAdmin(targetUserId, email.trim());
      const result = response.data || response;

      if (result.state === 'OK') {
        setSuccess('Email успешно изменен. Пользователь будет заблокирован до подтверждения нового email.');
        setTimeout(() => {
          handleClose();
        }, 2000);
      } else {
        setError(result.message || 'Ошибка при изменении email');
      }
    } catch (err: any) {
      console.error('Error changing email (admin):', err);
      setError(err.response?.data?.message || err.message || 'Ошибка при изменении email');
    } finally {
      setLoading(false);
    }
  };

  const isValidEmail = (email: string) => {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
  };

  const handleClose = () => {
    setEmail(currentEmail || '');
    setConfirmation(false);
    setError(null);
    setSuccess(null);
    setLoading(false);
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose}>
      <div className="admin-change-email-modal">
        <h2 className="admin-modal-title">Смена email (Админ)</h2>

        <div className="admin-warning-message">
          <span className="admin-warning-icon">⚠</span>
          <span>
            После смены email аккаунт пользователя будет заблокирован до подтверждения нового адреса электронной почты.
            Пользователь будет автоматически вылогинен из системы.
          </span>
        </div>

        <div className="admin-current-info">
          <span className="admin-info-label">ID пользователя:</span>
          <span className="admin-info-value">{targetUserId}</span>
        </div>

        <div className="admin-current-info">
          <span className="admin-info-label">Текущий email:</span>
          <span className="admin-info-value">{currentEmail || 'Не установлен'}</span>
        </div>

        <div className="admin-input-group">
          <label htmlFor="admin-email" className="admin-input-label">
            Новый email
          </label>
          <input
            id="admin-email"
            type="email"
            className="admin-email-input"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Введите новый email"
            disabled={loading}
          />
        </div>

        <div className="admin-confirmation-group">
          <label className="admin-confirmation-label">
            <input
              type="checkbox"
              checked={confirmation}
              onChange={(e) => setConfirmation(e.target.checked)}
              disabled={loading}
            />
            <span>
              Я понимаю, что после смены email аккаунт пользователя будет заблокирован до подтверждения нового адреса,
              и пользователь будет автоматически вылогинен из системы.
            </span>
          </label>
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
            label={loading ? "Смена email..." : "Сменить email"}
            onClick={handleSubmit}
            disabled={loading || !email.trim() || !confirmation || email.trim() === (currentEmail || '')}
          />
        </div>
      </div>
    </Modal>
  );
}

export default AdminChangeEmailModal;