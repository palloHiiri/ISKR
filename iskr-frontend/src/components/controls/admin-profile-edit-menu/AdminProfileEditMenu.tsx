import './AdminProfileEditMenu.scss';
import { useState } from 'react';
import AdminChangeUsernameModal from '../admin-change-username-modal/AdminChangeUsernameModal';
import AdminChangeProfilePhotoModal from '../admin-change-profile-photo-modal/AdminChangeProfilePhotoModal';
import AdminChangeDescriptionModal from '../admin-change-description-modal/AdminChangeDescriptionModal';
import AdminChangeNicknameModal from '../admin-change-nickname-modal/AdminChangeNicknameModal';
import AdminChangeEmailModal from '../admin-change-email-modal/AdminChangeEmailModal';
import AdminChangePasswordModal from '../admin-change-password-modal/AdminChangePasswordModal';

interface AdminProfileEditMenuProps {
  onClose: () => void;
  currentUsername: string;
  currentImageUrl: string;
  currentDescription: string | null;
  currentNickname: string | null;
  currentEmail: string | null;
  onUsernameChanged: (newUsername: string) => void;
  onProfilePhotoChanged: () => void;
  onDescriptionChanged: (newDescription: string) => void;
  onNicknameChanged: (newNickname: string) => void;
  targetUserId: number;
}

function AdminProfileEditMenu({ 
  onClose, 
  currentUsername, 
  currentImageUrl, 
  currentDescription,
  currentNickname,
  currentEmail,
  onUsernameChanged, 
  onProfilePhotoChanged,
  onDescriptionChanged,
  onNicknameChanged,
  targetUserId
}: AdminProfileEditMenuProps) {
  const [activeModal, setActiveModal] = useState<string | null>(null);

  const handleOpenModal = (modalName: string) => {
    setActiveModal(modalName);
  };

  const handleCloseModal = () => {
    setActiveModal(null);
  };

  const handleUsernameSuccess = (newUsername: string) => {
    onUsernameChanged(newUsername);
    handleCloseModal();
  };

  const handleProfilePhotoSuccess = () => {
    onProfilePhotoChanged();
    handleCloseModal();
  };

  const handleDescriptionSuccess = (newDescription: string) => {
    onDescriptionChanged(newDescription);
    handleCloseModal();
  };

  const handleNicknameSuccess = (newNickname: string) => {
    onNicknameChanged(newNickname);
    handleCloseModal();
  };

  return (
    <>
      <div className="admin-profile-edit-menu">
        <h2 className="admin-profile-edit-title">Редактирование профиля пользователя (Админ)</h2>
        
        <div className="admin-profile-edit-options">
          <button 
            className="admin-profile-edit-option"
            onClick={() => handleOpenModal('username')}
          >
            <div className="admin-option-content">
              <span className="admin-option-title">Сменить имя пользователя</span>
              <span className="admin-option-subtitle">Текущее: {currentUsername}</span>
            </div>
            <div className="admin-option-icon">→</div>
          </button>

          <button 
            className="admin-profile-edit-option"
            onClick={() => handleOpenModal('photo')}
          >
            <div className="admin-option-content">
              <span className="admin-option-title">Сменить фото профиля</span>
              <span className="admin-option-subtitle">Загрузить новое изображение</span>
            </div>
            <div className="admin-option-icon">→</div>
          </button>

          <button 
            className="admin-profile-edit-option"
            onClick={() => handleOpenModal('description')}
          >
            <div className="admin-option-content">
              <span className="admin-option-title">Редактировать описание</span>
              <span className="admin-option-subtitle">Изменить информацию о пользователе</span>
            </div>
            <div className="admin-option-icon">→</div>
          </button>

          <button 
            className="admin-profile-edit-option"
            onClick={() => handleOpenModal('nickname')}
          >
            <div className="admin-option-content">
              <span className="admin-option-title">Сменить никнейм</span>
              <span className="admin-option-subtitle">Изменить отображаемое имя</span>
            </div>
            <div className="admin-option-icon">→</div>
          </button>

          <button 
            className="admin-profile-edit-option"
            onClick={() => handleOpenModal('email')}
          >
            <div className="admin-option-content">
              <span className="admin-option-title">Сменить email</span>
              <span className="admin-option-subtitle">Изменить адрес электронной почты</span>
            </div>
            <div className="admin-option-icon">→</div>
          </button>

          <button 
            className="admin-profile-edit-option"
            onClick={() => handleOpenModal('password')}
          >
            <div className="admin-option-content">
              <span className="admin-option-title">Сменить пароль</span>
              <span className="admin-option-subtitle">Установить новый пароль для пользователя</span>
            </div>
            <div className="admin-option-icon">→</div>
          </button>
        </div>

        <button className="admin-profile-edit-close" onClick={onClose}>
          Закрыть
        </button>
      </div>

      {/* Модальные окна для администратора */}
      {activeModal === 'username' && (
        <AdminChangeUsernameModal
          open={true}
          onClose={handleCloseModal}
          currentUsername={currentUsername}
          onSuccess={handleUsernameSuccess}
          targetUserId={targetUserId}
        />
      )}
      
      {activeModal === 'photo' && (
        <AdminChangeProfilePhotoModal
          open={true}
          onClose={handleCloseModal}
          currentImageUrl={currentImageUrl}
          onSuccess={handleProfilePhotoSuccess}
          targetUserId={targetUserId}
        />
      )}
      
      {activeModal === 'description' && (
        <AdminChangeDescriptionModal
          open={true}
          onClose={handleCloseModal}
          currentDescription={currentDescription}
          onSuccess={handleDescriptionSuccess}
          targetUserId={targetUserId}
        />
      )}
      
      {activeModal === 'nickname' && (
        <AdminChangeNicknameModal
          open={true}
          onClose={handleCloseModal}
          currentNickname={currentNickname}
          onSuccess={handleNicknameSuccess}
          targetUserId={targetUserId}
        />
      )}
      
      {activeModal === 'email' && (
        <AdminChangeEmailModal
          open={true}
          onClose={handleCloseModal}
          currentEmail={currentEmail}
          targetUserId={targetUserId}
        />
      )}

      {activeModal === 'password' && (
        <AdminChangePasswordModal
          open={true}
          onClose={handleCloseModal}
          targetUserId={targetUserId}
        />
      )}
    </>
  );
}

export default AdminProfileEditMenu;