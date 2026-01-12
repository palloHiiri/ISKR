import PrimaryButton from "../../controls/primary-button/PrimaryButton.tsx";
import CardElement from "../../controls/card-element/CardElement.tsx";
import AddIcon from "../../../assets/elements/add.svg";
import Delete from "../../../assets/elements/delete.svg";
import { useState, useEffect, useCallback } from "react";
import { useSelector } from "react-redux";
import type { RootState } from "../../../redux/store.ts";
import ProfileEditMenu from "../../controls/profile-edit-menu/ProfileEditMenu.tsx";
import Modal from "../../controls/modal/Modal.tsx";
import { useNavigate } from "react-router-dom";
import SecondaryButton from "../../controls/secondary-button/SecondaryButton.tsx";
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import './Account.scss';
import { russianLocalWordConverter } from "../../../utils/russianLocalWordConverter.ts";
import profileAPI from '../../../api/profileService';
import type { ProfileUser, ProfileCollection, UserSubscription, UserSubscriber } from '../../../types/profile';
import { getImageUrl, getCollectionImageUrl } from '../../../api/popularService';
import Login from "../../controls/login/Login.tsx";

function Account() {
  const { user: authUser } = useSelector((state: RootState) => state.auth);
  const navigate = useNavigate();
  const [showLoginModal, setShowLoginModal] = useState(false);
  
  const [profile, setProfile] = useState<ProfileUser | null>(null);
  const [subscribers, setSubscribers] = useState<UserSubscriber[]>([]);
  const [subscriptions, setSubscriptions] = useState<UserSubscription[]>([]);
  const [collections, setCollections] = useState<ProfileCollection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditProfileOpen, setIsEditProfileOpen] = useState(false);

  const loadProfileData = useCallback(async () => {
    if (!authUser?.id) {
      setError("Пользователь не авторизован");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const userId = typeof authUser.id === 'string' ? parseInt(authUser.id) : authUser.id;
      
      const [profileData, subscribersData, subscriptionsData, collectionsData] = await Promise.all([
        profileAPI.getUserProfile(userId),
        profileAPI.getUserSubscribers(userId, 6, 0),
        profileAPI.getUserSubscriptions(userId, 6, 0),
        profileAPI.getUserCollections(userId, 4, 0)
      ]);

      setProfile(profileData);
      setSubscribers(subscribersData);
      setSubscriptions(subscriptionsData);
      setCollections(collectionsData);
    } catch (err: any) {
      console.error('Error loading account data:', err);
      setError(err.message || 'Ошибка загрузки данных профиля');
    } finally {
      setLoading(false);
    }
  }, [authUser?.id]);

  useEffect(() => {
    loadProfileData();
  }, [loadProfileData]);

  const handleSubscriberClick = () => {
    if (profile) {
      navigate('/followers', {
        state: {
          userId: profile.userId,
          isMine: true
        }
      });
    }
  };

  const handleProfilePhotoChanged = () => {
  loadProfileData();
};

  const handleSubscriptionsClick = () => {
    if (profile) {
      navigate('/subscriptions', {
        state: {
          userId: profile.userId,
          isMine: true
        }
      });
    }
  };

  const handleCollectionClick = (collection: ProfileCollection) => {
    navigate('/collection', {
      state: {
        id: collection.collectionId,
        name: collection.title,
        description: collection.description,
        isMine: true,
        coverUrl: getCollectionImageUrl(collection as any) || PlaceholderImage,
        owner: profile?.nickname || profile?.username || 'Пользователь',
        booksCount: collection.bookCount,
        likesCount: 0,
        books: []
      }
    });
  };

  const handleFollowerClick = (follower: UserSubscriber | UserSubscription) => {
    navigate('/profile', {
      state: {
        userId: follower.userId
      }
    });
  };

  const handleEditProfile = (): void => {
    setIsEditProfileOpen(true);
  };

  const handleCloseEditProfile = (): void => {
    setIsEditProfileOpen(false);
  };

  const handleUsernameChanged = (newUsername: string) => {
    if (profile) {
      setProfile({
        ...profile,
        username: newUsername
      });
    }
    loadProfileData();
  };

  const handleDescriptionChanged = (newDescription: string) => {
  if (profile) {
    setProfile({
      ...profile,
      profileDescription: newDescription
    });
  }
};

const handleNicknameChanged = (newNickname: string) => {
  if (profile) {
    setProfile({
      ...profile,
      nickname: newNickname
    });
  }
  loadProfileData();
};

  const getAvatarUrl = (): string => {
    if (!profile) return PlaceholderImage;
    
    const imageUrl = profile.profileImage ? 
      getImageUrl(profile.profileImage) : 
      null;
    
    return imageUrl || PlaceholderImage;
  };

  const getDisplayName = (): string => {
    if (!profile) return 'Загрузка...';
    return profile.nickname || profile.username || 'Пользователь';
  };

  const getProfileDescription = (): string | null => {
    if (!profile) return null;
    return profile.profileDescription || null;
  };

  const formatSubscribersCount = (count: number): string => {
    const formattedCount = count.toLocaleString('ru-RU');
    const word = russianLocalWordConverter(
      count,
      'подписчик',
      'подписчика',
      'подписчиков',
      'подписчиков'
    );
    return `${formattedCount} ${word}`;
  };

  const renderLoadingState = () => (
    <div className="loading-state">
      <div className="loading-spinner"></div>
      <p>Загрузка профиля...</p>
    </div>
  );

  const renderErrorState = () => (
    <div className="error-state">
      <p>Ошибка: {error}</p>
      <SecondaryButton 
        label="Вернуться на главную" 
        onClick={() => navigate('/')}
      />
    </div>
  );

  if (loading) {
    return (
      <main>
        <div className="top-container">
          {renderLoadingState()}
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main>
        <div className="top-container">
          {renderErrorState()}
        </div>
      </main>
    );
  }

  if (!profile) {
    return (
      <main>
        <div className="top-container">
          <div className="error-state">
            <p>Профиль не найден</p>
            <SecondaryButton 
              label="Вернуться на главную" 
              onClick={() => navigate('/')}
            />
          </div>
        </div>
      </main>
    );
  }

  const profileDescription = getProfileDescription();

  return (
    <>
      <main>
        <div className="top-container">
          <div className="container-title-with-button">
            <h2>Профиль</h2>
            <PrimaryButton label={"Редактировать"} onClick={handleEditProfile}/>
          </div>

          <div className="profile-info container">
            <div className="profile-info-main">
              <div className="profile-info-panel">
                <span className="profile-info-name">{getDisplayName()}</span>
                
                <div className="profile-avatar-container">
                  <img className="profile-avatar" alt="" src={getAvatarUrl()}/>
                </div>
                
                <div className="profile-info-additional-container">
                  <div className="profile-info-additional clickable" onClick={handleSubscriberClick}>
                    <span className="profile-info-label">{(profile.subscribersCount || 0).toLocaleString('ru-RU')} </span>
                    <span className="profile-info-sublabel">
                      {russianLocalWordConverter(
                        profile.subscribersCount || 0,
                        'подписчик',
                        'подписчика',
                        'подписчиков',
                        'подписчиков'
                      )}
                    </span>
                  </div>
                  <div className="profile-info-additional clickable" onClick={handleSubscriptionsClick}>
                    <span className="profile-info-label">{(profile.subscriptionsCount || 0).toLocaleString('ru-RU')} </span>
                    <span className="profile-info-sublabel">
                      {russianLocalWordConverter(
                        profile.subscriptionsCount || 0,
                        'подписка',
                        'подписки',
                        'подписок',
                        'подписок'
                      )}
                    </span>
                  </div>
                  <div className="profile-info-additional">
                    <span className="profile-info-label">{(profile.collectionsCount || 0).toLocaleString('ru-RU')} </span>
                    <span className="profile-info-sublabel">
                      {russianLocalWordConverter(
                        profile.collectionsCount || 0,
                        'коллекция',
                        'коллекции',
                        'коллекций',
                        'коллекций'
                      )}
                    </span>
                  </div>
                </div>
                
                {/* Описание профиля */}
                {profileDescription && (
                  <div className="profile-description">
                    <span className="profile-description-title">Описание профиля</span>
                    <p>{profileDescription}</p>
                  </div>
                )}
              </div>

              <div className="profile-info-collections">
                <span className="profile-collections-title">Коллекции</span>
                {collections.length > 0 ? (
                  <div className="profile-collections-list">
                    {collections.map((collection) => (
                      <div key={collection.collectionId}>
                        <CardElement
                          title={collection.title}
                          description={getDisplayName()}
                          infoDecoration={`${collection.bookCount} ${russianLocalWordConverter(
                            collection.bookCount,
                            'книга',
                            'книги',
                            'книг',
                            'книг'
                          )}`}
                          imageUrl={getCollectionImageUrl(collection as any) || PlaceholderImage}
                          button={false}  
                          onClick={() => handleCollectionClick(collection)}
                        />
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="no-books-message">У вас пока нет созданных коллекций.</p>
                )}
              </div>
            </div>

            <div className="profile-info-followers">
              <div className="profile-section-header">
                <span className="profile-section-title">Подписчики</span>
                {subscribers.length > 0 && (
                  <SecondaryButton
                    label="Перейти ко всем"
                    onClick={handleSubscriberClick}
                  />
                )}
              </div>
              {subscribers.length > 0 ? (
                <div className="profile-followers-list">
                  {subscribers.map((subscriber) => (
                    <CardElement
                      key={subscriber.userId}
                      title={subscriber.nickname || subscriber.username}
                      description={formatSubscribersCount(subscriber.subscribersCount)}
                      imageUrl={getImageUrl(subscriber.profileImage) || PlaceholderImage}
                      button={false}
                      onClick={() => handleFollowerClick(subscriber)}
                    />
                  ))}
                </div>
              ) : (
                <p className="no-items-message">Нет подписчиков</p>
              )}
            </div>

            <div className="profile-info-subscriptions">
              <div className="profile-section-header">
                <span className="profile-section-title">Подписки</span>
                {subscriptions.length > 0 && (
                  <SecondaryButton
                    label="Перейти ко всем"
                    onClick={handleSubscriptionsClick}
                  />
                )}
              </div>
              {subscriptions.length > 0 ? (
                <div className="profile-subscriptions-list">
                  {subscriptions.map((subscription) => (
                    <CardElement
                      key={subscription.userId}
                      title={subscription.nickname || subscription.username}
                      description={formatSubscribersCount(subscription.subscribersCount)}
                      imageUrl={getImageUrl(subscription.profileImage) || PlaceholderImage}
                      button={false}
                      onClick={() => handleFollowerClick(subscription)}
                    />
                  ))}
                </div>
              ) : (
                <p className="no-items-message">Нет подписок</p>
              )}
            </div>
          </div>
        </div>
      </main>

      {/* Модальное окно редактирования профиля */}
      {isEditProfileOpen && (
        <Modal open={isEditProfileOpen} onClose={handleCloseEditProfile}>
          <ProfileEditMenu
  onClose={handleCloseEditProfile}
  currentUsername={profile?.username || ''}
  currentImageUrl={getAvatarUrl()}
  currentDescription={profile?.profileDescription || null}
  currentNickname={profile?.nickname || null}
  currentEmail={profile?.email || null}
  onUsernameChanged={handleUsernameChanged}
  onProfilePhotoChanged={handleProfilePhotoChanged}
  onDescriptionChanged={handleDescriptionChanged}
  onNicknameChanged={handleNicknameChanged}
/>
        </Modal>
      )}

      <Modal
        open={showLoginModal}
        onClose={() => setShowLoginModal(false)}
      >
        <Login
          type="login"
          onSubmit={() => setShowLoginModal(false)}
        />
      </Modal>
    </>
  );
}

export default Account;