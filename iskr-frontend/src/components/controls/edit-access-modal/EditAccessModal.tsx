// /src/components/controls/edit-access-modal/EditAccessModal.tsx
import { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import Modal from '../modal/Modal';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import Input from '../input/Input';
import './EditAccessModal.scss';
import cvpAPI, { type CVPStatus, type UserSearchResult, type AddCVPData } from '../../../api/cvpService';
import { getSearchImageUrl } from '../../../api/popularService';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import SearchIcon from '../../../assets/elements/search.svg';
import DeleteIcon from '../../../assets/elements/delete.svg';
import EditIcon from '../../../assets/elements/change.svg';
import type { RootState } from '../../../redux/store';

interface EditAccessModalProps {
  open: boolean;
  onClose: () => void;
  collectionId: number;
  collectionTitle: string;
  isAdmin?: boolean;
}

type UserActionType = 'add' | 'remove' | 'changeStatus';

function EditAccessModal({ open, onClose, collectionId, collectionTitle, isAdmin = false }: EditAccessModalProps) {
  const currentUser = useSelector((state: RootState) => state.auth.user);
  const [privileges, setPrivileges] = useState<CVPStatus[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [selectedUserForAction, setSelectedUserForAction] = useState<{
    userId: number;
    username: string;
    nickname: string;
    actionType: UserActionType;
    currentStatus?: 'Allowed' | 'Disallowed';
  } | null>(null);

  // Загрузка текущих привилегий
  useEffect(() => {
    const loadPrivileges = async () => {
      if (!open) return;
      
      try {
        setLoading(true);
        setError(null);
        
        // Используем административный метод для администраторов
        let data: CVPStatus[];
        if (isAdmin) {
          data = await cvpAPI.getCollectionPrivilegesAdmin(collectionId);
        } else {
          data = await cvpAPI.getCollectionPrivileges(collectionId);
        }
        
        const privilegesArray = Array.isArray(data) ? data : [];
        setPrivileges(privilegesArray);
      } catch (err: any) {
        console.error('Error loading privileges:', err);
        setError('Не удалось загрузить список настроек доступа');
        setPrivileges([]);
      } finally {
        setLoading(false);
      }
    };

    loadPrivileges();
  }, [open, collectionId, isAdmin]);

  // Поиск пользователей
  useEffect(() => {
    const searchUsers = async () => {
      if (!searchQuery.trim()) {
        setSearchResults([]);
        return;
      }

      try {
        setSearching(true);
        const results = await cvpAPI.searchUsers(searchQuery, 10);
        
        const resultsArray = Array.isArray(results) ? results : [];
        setSearchResults(resultsArray);
      } catch (err: any) {
        console.error('Error searching users:', err);
        setSearchResults([]);
      } finally {
        setSearching(false);
      }
    };

    const timer = setTimeout(searchUsers, 500);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  const handleAddOrUpdatePrivilege = async (userId: number, username: string, status: 'Allowed' | 'Disallowed') => {
    try {
      setLoading(true);
      setError(null);
      
      const data: AddCVPData = {
        cvpStatus: status,
        userId
      };
      
      // Используем административный метод для администраторов
      if (isAdmin) {
        await cvpAPI.addCollectionPrivilegeAdmin(collectionId, data);
      } else {
        await cvpAPI.addCollectionPrivilege(collectionId, data);
      }

      // Перезагружаем список привилегий
      let updatedPrivileges: CVPStatus[];
      if (isAdmin) {
        updatedPrivileges = await cvpAPI.getCollectionPrivilegesAdmin(collectionId);
      } else {
        updatedPrivileges = await cvpAPI.getCollectionPrivileges(collectionId);
      }
      
      const privilegesArray = Array.isArray(updatedPrivileges) ? updatedPrivileges : [];
      setPrivileges(privilegesArray);
      
      if (selectedUserForAction?.actionType === 'add') {
        setSearchQuery('');
        setSearchResults([]);
      }
      
      setSuccessMessage(`Настройка доступа для пользователя ${username} успешно обновлена`);
      setTimeout(() => setSuccessMessage(null), 3000);
      setSelectedUserForAction(null);
    } catch (err: any) {
      console.error('Error adding/updating privilege:', err);
      setError(err.message || 'Не удалось обновить настройку доступа');
    } finally {
      setLoading(false);
    }
  };

  const handleRemovePrivilege = async (userId: number, username: string) => {
    try {
      setLoading(true);
      setError(null);
      
      // Используем административный метод для администраторов
      if (isAdmin) {
        await cvpAPI.removeCollectionPrivilegeAdmin(collectionId, userId);
      } else {
        await cvpAPI.removeCollectionPrivilege(collectionId, userId);
      }
      
      // Обновляем локальное состояние
      setPrivileges(prev => prev.filter(p => p.userId !== userId));
      
      setSuccessMessage(`Настройка доступа для пользователя ${username} удалена`);
      setTimeout(() => setSuccessMessage(null), 3000);
      setSelectedUserForAction(null);
    } catch (err: any) {
      console.error('Error removing privilege:', err);
      setError(err.message || 'Не удалось удалить настройку доступа');
    } finally {
      setLoading(false);
    }
  };

  const getUserImageUrl = (user: UserSearchResult | CVPStatus['user']): string => {
    if (!user) return PlaceholderImage;
    
    if ('imageUuid' in user && user.imageUuid && user.imageExtension) {
      return getSearchImageUrl(user.imageUuid, user.imageExtension) || PlaceholderImage;
    }
    return PlaceholderImage;
  };

  const getUserDisplayName = (user: CVPStatus['user'] | undefined, userId: number, username?: string, nickname?: string): string => {
    if (nickname) return nickname;
    if (user && user.nickname) return user.nickname;
    if (username) return username;
    if (user && user.username) return user.username;
    return `Пользователь #${userId}`;
  };

  const getUserUsername = (user: CVPStatus['user'] | undefined, userId: number, username?: string): string => {
    if (username) return `@${username}`;
    if (user && user.username) return `@${user.username}`;
    return `ID: ${userId}`;
  };

  const getStatusText = (status: string): string => {
    return status === 'Allowed' ? 'Разрешить' : 'Запретить';
  };

  const userHasPrivilege = (userId: number): CVPStatus | undefined => {
    return privileges.find(p => p.userId === userId);
  };

  const isCurrentUser = (userId: number): boolean => {
    return currentUser?.id === userId.toString() || currentUser?.userId === userId;
  };

  const renderStatusSelectionModal = () => {
    if (!selectedUserForAction) return null;

    const userPrivilege = userHasPrivilege(selectedUserForAction.userId);
    const isUpdate = userPrivilege !== undefined;

    return (
      <Modal open={true} onClose={() => setSelectedUserForAction(null)}>
        <div className="status-selection-modal-container">
          <div className="status-selection-modal">
            <h3>
              {isUpdate ? 'Изменение настройки доступа' : 'Добавление настройки доступа'}
            </h3>
            <p className="user-info-text">
              Пользователь: {selectedUserForAction.nickname}
            </p>
            
            <div className="status-options">
              <button
                type="button"
                className={`status-option ${!isUpdate || selectedUserForAction.currentStatus === 'Allowed' ? 'active' : ''}`}
                onClick={() => handleAddOrUpdatePrivilege(
                  selectedUserForAction.userId,
                  selectedUserForAction.nickname,
                  'Allowed'
                )}
                disabled={loading}
              >
                <span className="status-icon">✓</span>
                <span className="status-text">
                  <strong>Разрешить просмотр коллекции</strong>
                  <span className="status-description">Пользователь сможет просматривать коллекцию</span>
                </span>
              </button>
              
              <button
                type="button"
                className={`status-option ${isUpdate && selectedUserForAction.currentStatus === 'Disallowed' ? 'active' : ''}`}
                onClick={() => handleAddOrUpdatePrivilege(
                  selectedUserForAction.userId,
                  selectedUserForAction.nickname,
                  'Disallowed'
                )}
                disabled={loading}
              >
                <span className="status-icon">✗</span>
                <span className="status-text">
                  <strong>Запретить просмотр коллекции</strong>
                  <span className="status-description">Пользователь не сможет просматривать коллекцию</span>
                </span>
              </button>
            </div>
            
            {isUpdate && (
              <button
                type="button"
                className="remove-privilege-btn"
                onClick={() => handleRemovePrivilege(
                  selectedUserForAction.userId,
                  selectedUserForAction.nickname
                )}
                disabled={loading}
              >
                Убрать настройку доступа
              </button>
            )}
            
            <div className="modal-actions">
              <SecondaryButton
                label="Отмена"
                onClick={() => setSelectedUserForAction(null)}
                disabled={loading}
              />
            </div>
          </div>
        </div>
      </Modal>
    );
  };

  return (
    <>
      <Modal open={open} onClose={onClose}>
        <div className="edit-access-modal">
          <div className="modal-header">
            <h2 className="modal-title">Управление доступом к коллекции</h2>
            <p className="modal-subtitle">{collectionTitle}</p>
          </div>

          <div className="modal-content">
            {error && (
              <div className="error-message">
                <span className="error-icon">⚠</span>
                <span>{error}</span>
              </div>
            )}

            {successMessage && (
              <div className="success-message">
                <span className="success-icon">✓</span>
                <span>{successMessage}</span>
              </div>
            )}

            {/* Поиск пользователей */}
            <div className="search-section">
              <h3>Добавить настройку доступа</h3>
              <div className="search-input-container">
                <Input
                  type="text"
                  placeholder="Введите имя пользователя для поиска..."
                  value={searchQuery}
                  onChange={setSearchQuery}
                  picture={SearchIcon}
                />
              </div>

              {searching && (
                <div className="loading-state">
                  <div className="loading-spinner"></div>
                  <p>Поиск пользователей...</p>
                </div>
              )}

              {searchResults.length > 0 && (
                <div className="search-results">
                  {searchResults.map(user => {
                    const existingPrivilege = userHasPrivilege(user.id);
                    const isSelf = isCurrentUser(user.id);
                    
                    if (isSelf) {
                      return (
                        <div key={user.id} className="search-result-item disabled">
                          <div className="user-info">
                            <img 
                              src={getUserImageUrl(user)} 
                              alt={user.username} 
                              className="user-avatar"
                            />
                            <div className="user-details">
                              <span className="user-nickname">{user.nickname || user.username}</span>
                              <span className="user-username">@{user.username}</span>
                            </div>
                          </div>
                          <div className="access-info">
                            <span className="access-already">Это вы</span>
                          </div>
                        </div>
                      );
                    }

                    if (existingPrivilege) {
                      return (
                        <div key={user.id} className="search-result-item disabled">
                          <div className="user-info">
                            <img 
                              src={getUserImageUrl(user)} 
                              alt={user.username} 
                              className="user-avatar"
                            />
                            <div className="user-details">
                              <span className="user-nickname">{user.nickname || user.username}</span>
                              <span className="user-username">@{user.username}</span>
                            </div>
                          </div>
                          <div className="access-info">
                            <span className="access-already">Настройка доступа уже есть</span>
                          </div>
                        </div>
                      );
                    }

                    return (
                      <div key={user.id} className="search-result-item">
                        <div className="user-info">
                          <img 
                            src={getUserImageUrl(user)} 
                            alt={user.username} 
                            className="user-avatar"
                          />
                          <div className="user-details">
                            <span className="user-nickname">{user.nickname || user.username}</span>
                            <span className="user-username">@{user.username}</span>
                          </div>
                        </div>
                        <PrimaryButton
                          label="Добавить настройку"
                          onClick={() => setSelectedUserForAction({
                            userId: user.id,
                            username: user.username,
                            nickname: user.nickname || user.username,
                            actionType: 'add'
                          })}
                          disabled={loading}
                        />
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Текущие настройки доступа */}
            <div className="privileges-section">
              <h3>Настройки доступа к коллекции</h3>
              
              {loading && !searching ? (
                <div className="loading-state">
                  <div className="loading-spinner"></div>
                  <p>Загрузка настроек доступа...</p>
                </div>
              ) : privileges.length === 0 ? (
                <p className="no-privileges-message">
                  Нет настроек доступа коллекции
                </p>
              ) : (
                <div className="privileges-list">
                  {privileges.map(privilege => (
                    <div key={privilege.cvpId} className="privilege-item">
                      <div className="user-info">
                        <img 
                          src={getUserImageUrl(privilege)} 
                          alt={privilege.nickname} 
                          className="user-avatar"
                        />
                        <div className="user-details">
                          <span className="user-nickname">
                            {getUserDisplayName(privilege.user, privilege.userId, privilege.username, privilege.nickname)}
                            {isCurrentUser(privilege.userId) && (
                              <span className="self-badge"> (Вы)</span>
                            )}
                          </span>
                          <span className="user-username">
                            {getUserUsername(privilege.user, privilege.userId, privilege.username)}
                          </span>
                          <span className={`user-status ${privilege.status.toLowerCase()}`}>
                            {getStatusText(privilege.status)}
                          </span>
                        </div>
                      </div>
                      <div className="privilege-actions">
                        <button
                          type="button"
                          className="change-status-btn"
                          onClick={() => setSelectedUserForAction({
                            userId: privilege.userId,
                            username: privilege.username,
                            nickname: privilege.nickname,
                            actionType: 'changeStatus',
                            currentStatus: privilege.status
                          })}
                          disabled={loading || isCurrentUser(privilege.userId)}
                          title={isCurrentUser(privilege.userId) ? "Вы не можете изменить свои настройки" : "Изменить настройку"}
                        >
                          <img src={EditIcon} alt="Изменить" />
                        </button>
                        <button
                          type="button"
                          className="remove-access-btn"
                          onClick={() => handleRemovePrivilege(
                            privilege.userId, 
                            privilege.nickname
                          )}
                          disabled={loading || isCurrentUser(privilege.userId)}
                          title={isCurrentUser(privilege.userId) ? "Вы не можете удалить свои настройки" : "Убрать настройку"}
                        >
                          <img src={DeleteIcon} alt="Удалить" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="modal-actions">
            <SecondaryButton
              label="Закрыть"
              onClick={onClose}
              disabled={loading}
            />
          </div>
        </div>
      </Modal>

      {selectedUserForAction && renderStatusSelectionModal()}
    </>
  );
}

export default EditAccessModal;