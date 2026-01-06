import { useState } from 'react';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import './AdminChangeUsernameModal.scss';
import profileAPI from '../../../api/profileService';

interface AdminChangeUsernameModalProps {
  open: boolean;
  onClose: () => void;
  currentUsername: string;
  onSuccess: (newUsername: string) => void;
  targetUserId: number;
}

function AdminChangeUsernameModal({ open, onClose, currentUsername, onSuccess, targetUserId }: AdminChangeUsernameModalProps) {
  const [newUsername, setNewUsername] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!newUsername.trim()) {
      setError('Введите новое имя пользователя');
      return;
    }

    if (newUsername.trim() === currentUsername) {
      setError('Новое имя пользователя совпадает с текущим');
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      const response = await profileAPI.changeUsernameAdmin(targetUserId, newUsername.trim());
      
      if (response.data.state === 'OK') {
        setSuccess('Имя пользователя успешно изменено');
        setTimeout(() => {
          onSuccess(newUsername.trim());
          handleClose();
        }, 1500);
      } else {
        const details = response.data.details || {};
        
        if (details.state === 'Fail_Conflict') {
          if (details.message === 'Same username') {
            setError('Новое имя пользователя совпадает с текущим');
          } else if (details.message === 'Username already taken') {
            setError('Имя пользователя уже занято другим пользователем');
          } else {
            setError('Ошибка при изменении имени пользователя');
          }
        } else {
          setError(response.data.message || 'Ошибка при изменении имени пользователя');
        }
      }
    } catch (err: any) {
      console.error('Error changing username (admin):', err);
      
      // Обработка ошибок API
      if (err.response?.data?.data?.details) {
        const details = err.response.data.data.details;
        
        if (details.state === 'Fail_Conflict') {
          if (details.message === 'Same username') {
            setError('Новое имя пользователя совпадает с текущим');
          } else if (details.message === 'Username already taken') {
            setError('Имя пользователя уже занято другим пользователем');
          } else {
            setError('Ошибка при изменении имени пользователя');
          }
        } else {
          setError(details.message || 'Ошибка при изменении имени пользователя');
        }
      } else {
        setError(err.message || 'Ошибка при изменении имени пользователя');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setNewUsername('');
    setError(null);
    setSuccess(null);
    setLoading(false);
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose}>
      <div className="admin-change-username-modal">
        <h2 className="admin-modal-title">Смена имени пользователя (Админ)</h2>
        
        <div className="admin-current-info">
          <span className="admin-info-label">ID пользователя:</span>
          <span className="admin-info-value">{targetUserId}</span>
        </div>
        
        <div className="admin-current-info">
          <span className="admin-info-label">Текущее имя пользователя:</span>
          <span className="admin-info-value">{currentUsername}</span>
        </div>
        
        <div className="admin-input-group">
          <label htmlFor="admin-new-username" className="admin-input-label">
            Новое имя пользователя
          </label>
          <input
            id="admin-new-username"
            type="text"
            className="admin-username-input"
            value={newUsername}
            onChange={(e) => setNewUsername(e.target.value)}
            placeholder="Введите новое имя пользователя"
            disabled={loading}
            autoFocus
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
            disabled={loading || !newUsername.trim()}
          />
        </div>
      </div>
    </Modal>
  );
}

export default AdminChangeUsernameModal;