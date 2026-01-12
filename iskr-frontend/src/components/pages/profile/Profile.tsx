import PrimaryButton from "../../controls/primary-button/PrimaryButton.tsx";
import CardElement from "../../controls/card-element/CardElement.tsx";
import AddIcon from "../../../assets/elements/add.svg";
import { useLocation, useNavigate, Navigate } from "react-router-dom";
import VerticalAccordion from "../../controls/vertical-accordion/VerticalAccordion.tsx";
import Delete from "../../../assets/elements/delete.svg";
import { useState, useEffect, useRef } from "react";
import type { RootState } from "../../../redux/store.ts";
import { useSelector, useDispatch } from "react-redux";
import Login from "../../controls/login/Login.tsx";
import Modal from "../../controls/modal/Modal.tsx";
import './Profile.scss';
import { russianLocalWordConverter } from "../../../utils/russianLocalWordConverter.ts";
import SecondaryButton from "../../controls/secondary-button/SecondaryButton.tsx";
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import profileAPI from '../../../api/profileService';
import type { ProfileUser, ProfileCollection, UserSubscription, UserSubscriber } from '../../../types/profile';
import { getImageUrl, getCollectionImageUrl } from '../../../api/popularService';
import { selectIsAdmin } from '../../../redux/authSlice';
import AdminProfileEditMenu from '../../controls/admin-profile-edit-menu/AdminProfileEditMenu.tsx';
import { logout } from '../../../redux/authSlice.ts';

// Создадим хук для проверки статуса пользователя
function useUserStatusChecker() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const user = useSelector((state: RootState) => state.auth.user);
  
  // Используем ref для хранения времени последней проверки
  const lastCheckTimeRef = useRef<number>(0);
  
  useEffect(() => {
    const checkStatus = async () => {
      // Проверяем не чаще чем раз в 30 секунд
      const now = Date.now();
      if (now - lastCheckTimeRef.current < 30000) return;
      
      if (isAuthenticated && user?.id) {
        try {
          lastCheckTimeRef.current = now;
          const response = await profileAPI.getUserProfile(user.id);
          
          if (response.status === 'banned') {
            dispatch(logout());
            navigate('/', { 
              replace: true,
              state: { 
                showBanMessage: true,
                message: 'Ваш аккаунт был заблокирован.' 
              }
            });
          }
        } catch (error) {
          console.error('Ошибка проверки статуса:', error);
        }
      }
    };
    
    checkStatus();
  }, [location.pathname, isAuthenticated, user, dispatch, navigate]);
}

function Profile() {
  const location = useLocation();
  const navigate = useNavigate();

  // Получаем userId из state или из location.state (для обратной совместимости)
  const userId = location.state?.userId || location.state?.id;

  // Если userId нет - редирект на главную
  if (!userId) {
    return <Navigate to="/" replace />;
  }

  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const currentUser = useSelector((state: RootState) => state.auth.user);
  const isAdmin = useSelector(selectIsAdmin);

  const [showLoginModal, setShowLoginModal] = useState(false);

  // Состояния для данных
  const [profile, setProfile] = useState<ProfileUser | null>(null);
  const [subscribers, setSubscribers] = useState<UserSubscriber[]>([]);
  const [subscriptions, setSubscriptions] = useState<UserSubscription[]>([]);
  const [collections, setCollections] = useState<ProfileCollection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [isCheckingSubscription, setIsCheckingSubscription] = useState(false);
  const [currentSubscribersCount, setCurrentSubscribersCount] = useState(0);
  const [subscriptionLoading, setSubscriptionLoading] = useState(false);
  const [subscriptionError, setSubscriptionError] = useState<string | null>(null);

  // Состояния для админских операций
  const [banLoading, setBanLoading] = useState(false);
  const [banError, setBanError] = useState<string | null>(null);
  const [isAdminEditOpen, setIsAdminEditOpen] = useState(false);

  // Используем хук для проверки статуса текущего пользователя
  useUserStatusChecker();

  // Проверяем, заблокирован ли пользователь
  const isBanned = profile?.status === 'banned';

  // Проверяем, является ли этот профиль профилем текущего пользователя
  const isOwnProfile = currentUser?.id === userId;

  // Проверяем, может ли текущий пользователь выполнять админские действия
  const canPerformAdminActions = isAdmin && !isOwnProfile && isAuthenticated;

  // Загрузка данных профиля и проверка подписки
  useEffect(() => {
    const loadProfileData = async () => {
      try {
        setLoading(true);
        setError(null);

        // Загружаем данные профиля
        const [profileData, subscribersData, subscriptionsData, collectionsData] = await Promise.all([
          profileAPI.getUserProfile(userId),
          profileAPI.getUserSubscribers(userId, 6, 0),
          profileAPI.getUserSubscriptions(userId, 6, 0),
          profileAPI.getUserCollections(userId, 4, 0)
        ]);

        setProfile(profileData);
        setCurrentSubscribersCount(profileData.subscribersCount || 0);
        setSubscribers(subscribersData);
        setSubscriptions(subscriptionsData);
        setCollections(collectionsData);

        // Если пользователь авторизован и это не его профиль - проверяем подписку
        if (isAuthenticated && !isOwnProfile && !isBanned) {
          await checkUserSubscription();
        }
      } catch (err: any) {
        console.error('Error loading profile:', err);
        setError(err.message || 'Ошибка загрузки профиля');
      } finally {
        setLoading(false);
      }
    };

    loadProfileData();
  }, [userId, isAuthenticated]);

  // Функция для проверки подписки
  const checkUserSubscription = async () => {
    if (!isAuthenticated || isOwnProfile || isBanned) return;

    try {
      setIsCheckingSubscription(true);
      const isSubscribedResult = await profileAPI.checkSubscription(userId);
      setIsSubscribed(isSubscribedResult);
    } catch (err: any) {
      console.error('Error checking subscription:', err);
    } finally {
      setIsCheckingSubscription(false);
    }
  };

  // Обработчик подписки/отписки
  const handleSubscribeProfile = async () => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }

    if (isBanned || isOwnProfile || subscriptionLoading) {
      return;
    }

    setSubscriptionLoading(true);
    setSubscriptionError(null);

    try {
      if (isSubscribed) {
        // Отписываемся
        const response = await profileAPI.unsubscribeFromUser(userId);

        if (response.data?.state === 'OK') {
          setIsSubscribed(false);
          setCurrentSubscribersCount(prev => prev - 1);
        } else {
          throw new Error(response.data?.message || 'Ошибка отписки');
        }
      } else {
        // Подписываемся
        const response = await profileAPI.subscribeToUser(userId);

        if (response.data?.state === 'OK') {
          setIsSubscribed(true);
          setCurrentSubscribersCount(prev => prev + 1);
        } else {
          throw new Error(response.data?.message || 'Ошибка подписки');
        }
      }
    } catch (err: any) {
      console.error('Subscription error:', err);

      // Обрабатываем специфичные ошибки
      if (err.response?.data?.data?.details?.state === 'Fail_Conflict') {
        setSubscriptionError('Вы уже подписаны на этого пользователя');
      } else if (err.response?.data?.data?.details?.state === 'Fail_NotFound') {
        setSubscriptionError('Подписка не найдена');
      } else {
        setSubscriptionError(err.message || 'Ошибка при выполнении операции');
      }
    } finally {
      setSubscriptionLoading(false);
    }
  };

  // Обработчик блокировки/разблокировки пользователя (для администратора)
  const handleBanUser = async () => {
    if (!canPerformAdminActions || banLoading) return;

    setBanLoading(true);
    setBanError(null);

    try {
      if (isBanned) {
        // Разблокировать пользователя
        const response = await profileAPI.unbanUser(userId);

        if (response.data?.state === 'OK') {
          // Обновляем статус пользователя в локальном состоянии
          setProfile(prev => prev ? { ...prev, status: 'notBanned' } : null);
        } else {
          throw new Error(response.data?.message || 'Ошибка разблокировки');
        }
      } else {
        // Заблокировать пользователя
        const response = await profileAPI.banUser(userId);

        if (response.data?.state === 'OK') {
          // Обновляем статус пользователя в локальном состоянии
          setProfile(prev => prev ? { ...prev, status: 'banned' } : null);
          // Также сбрасываем подписку, если пользователь был подписан
          setIsSubscribed(false);
        } else {
          throw new Error(response.data?.message || 'Ошибка блокировки');
        }
      }
    } catch (err: any) {
      console.error('Ban/Unban error:', err);
      setBanError(err.message || 'Ошибка при выполнении операции');
    } finally {
      setBanLoading(false);
    }
  };

  // Обработчики для админского редактирования профиля
  const handleAdminEditProfile = (): void => {
    setIsAdminEditOpen(true);
  };

  const handleCloseAdminEditProfile = (): void => {
    setIsAdminEditOpen(false);
  };

  const handleAdminUsernameChanged = (newUsername: string) => {
    if (profile) {
      setProfile({
        ...profile,
        username: newUsername
      });
    }
    // Перезагружаем данные профиля
    loadProfileData();
  };

  const handleAdminProfilePhotoChanged = () => {
    // Перезагружаем данные профиля, чтобы обновилось фото
    loadProfileData();
  };

  const handleAdminDescriptionChanged = (newDescription: string) => {
    if (profile) {
      setProfile({
        ...profile,
        profileDescription: newDescription
      });
    }
  };

  const handleAdminNicknameChanged = (newNickname: string) => {
    if (profile) {
      setProfile({
        ...profile,
        nickname: newNickname
      });
    }
    // Обновляем отображаемое имя
    loadProfileData();
  };

  // Функция для перезагрузки данных профиля
  const loadProfileData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Загружаем данные профиля
      const [profileData, subscribersData, subscriptionsData, collectionsData] = await Promise.all([
        profileAPI.getUserProfile(userId),
        profileAPI.getUserSubscribers(userId, 6, 0),
        profileAPI.getUserSubscriptions(userId, 6, 0),
        profileAPI.getUserCollections(userId, 4, 0)
      ]);

      setProfile(profileData);
      setCurrentSubscribersCount(profileData.subscribersCount || 0);
      setSubscribers(subscribersData);
      setSubscriptions(subscriptionsData);
      setCollections(collectionsData);
    } catch (err: any) {
      console.error('Error loading profile:', err);
      setError(err.message || 'Ошибка загрузки профиля');
    } finally {
      setLoading(false);
    }
  };

  const getFormattedSubscribersCount = (): string => {
    return currentSubscribersCount.toLocaleString('ru-RU').replace(/,/g, ' ');
  };

  const getSubscribersWord = (count: number): string => {
    return russianLocalWordConverter(count, 'подписчик', 'подписчика', 'подписчиков', 'подписчиков');
  };

  const handleFollowerClick = (follower: UserSubscriber | UserSubscription) => {
    navigate('/profile', {
      state: {
        userId: follower.userId
      }
    });
  };

  const handleSubscriberClick = () => {
    navigate('/followers', {
      state: {
        userId: profile?.userId,
        isMine: false
      }
    });
  };

  const handleSubscriptionsClick = () => {
    navigate('/subscriptions', {
      state: {
        userId: profile?.userId,
        isMine: false
      }
    });
  };

  const handleCollectionClick = (collection: ProfileCollection) => {
    navigate('/collection', {
      state: {
        id: collection.collectionId,
        name: collection.title,
        description: collection.description,
        isMine: false,
        coverUrl: getCollectionImageUrl(collection as any) || PlaceholderImage,
        owner: profile?.nickname || profile?.username || 'Пользователь',
        booksCount: collection.bookCount,
        likesCount: 0,
        books: []
      }
    });
  };

  // Получаем URL аватара
  const getAvatarUrl = (): string => {
    if (!profile) return PlaceholderImage;

    const imageUrl = profile.profileImage ?
      getImageUrl(profile.profileImage) :
      null;

    return imageUrl || PlaceholderImage;
  };

  // Получаем отображаемое имя
  const getDisplayName = (): string => {
    if (!profile) return 'Загрузка...';
    return profile.nickname || profile.username || 'Пользователь';
  };

  // Получаем описание профиля
  const getProfileDescription = (): string | null => {
    if (!profile) return null;
    return profile.profileDescription || null;
  };

  // Рендер состояний загрузки и ошибок
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

  // Функция для форматирования количества подписчиков с правильным склонением
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
    return <Navigate to="/" replace />;
  }

  const profileDescription = getProfileDescription();

  return (
    <main>
      <div className="top-container">
        <div className="container-title-with-button">
          <h2>Профиль</h2>

          <div className="profile-action-buttons">
            {/* Кнопка подписки/отписки для обычных пользователей */}
            {isAuthenticated && !isBanned && !isOwnProfile && (
              <div className="subscription-button-wrapper">
                {subscriptionError && (
                  <div className="subscription-error-message">
                    {subscriptionError}
                  </div>
                )}
                {subscriptionLoading || isCheckingSubscription ? (
                  <div className="subscription-loading">
                    <div className="small-loading-spinner"></div>
                  </div>
                ) : isSubscribed ? (
                  <SecondaryButton
                    label={"Отписаться"}
                    onClick={handleSubscribeProfile}
                    disabled={subscriptionLoading}
                  />
                ) : (
                  <PrimaryButton
                    label={"Подписаться"}
                    onClick={handleSubscribeProfile}
                    disabled={subscriptionLoading}
                  />
                )}
              </div>
            )}

            {/* Кнопки для администратора */}
            {canPerformAdminActions && (
              <>
                <div className="admin-ban-button-wrapper">
                  {banError && (
                    <div className="admin-error-message">
                      {banError}
                    </div>
                  )}
                  {banLoading ? (
                    <div className="admin-loading">
                      <div className="small-loading-spinner"></div>
                    </div>
                  ) : isBanned ? (
                    <PrimaryButton
                      label={"Разблокировать"}
                      onClick={handleBanUser}
                      disabled={banLoading}
                      style={{ backgroundColor: '#4CAF50' }}
                    />
                  ) : (
                    <PrimaryButton
                      label={"Заблокировать"}
                      onClick={handleBanUser}
                      disabled={banLoading}
                      style={{ backgroundColor: '#f44336' }}
                    />
                  )}
                </div>

                <SecondaryButton
                  label={"Редактировать профиль"}
                  onClick={handleAdminEditProfile}
                  style={{
                    backgroundColor: 'transparent',
                    borderColor: '#457b9d',
                    color: '#457b9d'
                  }}
                />
              </>
            )}
          </div>
        </div>

        {/* Баннер заблокированного пользователя */}
        {isBanned && (
          <div className="banned-banner">
            <div className="banned-banner-content">
              <span className="banned-banner-icon">🚫</span>
              <span className="banned-banner-text">Пользователь заблокирован</span>
            </div>
          </div>
        )}

        <div className="profile-info container">
          <div className="profile-info-main">
            <div className="profile-info-panel">
              <span className="profile-info-name">{getDisplayName()}</span>

              <div className="profile-avatar-container">
                <img className="profile-avatar" alt="" src={getAvatarUrl()} />
                {isBanned && <div className="profile-avatar-overlay"></div>}
              </div>

              <div className="profile-info-additional-container">
                <div className="profile-info-additional clickable" onClick={handleSubscriberClick}>
                  <span className="profile-info-label">{getFormattedSubscribersCount()} </span>
                  <span className="profile-info-sublabel">{getSubscribersWord(currentSubscribersCount)}</span>
                </div>
                <div className="profile-info-additional clickable" onClick={handleSubscriptionsClick}>
                  <span className="profile-info-label">{(profile.subscriptionsCount || 0).toLocaleString('ru-RU')} </span>
                  <span className="profile-info-sublabel">подписок</span>
                </div>
                <div className="profile-info-additional">
                  <span className="profile-info-label">{(profile.collectionsCount || 0).toLocaleString('ru-RU')} </span>
                  <span className="profile-info-sublabel">коллекций</span>
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
                <VerticalAccordion
                  header={
                    <div className="profile-collections-header">
                      {collections.slice(0, 4).map((collection) => (
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
                            button={true}
                            buttonLabel={"Добавить в избранное"}
                            onClick={() => handleCollectionClick(collection)}
                            buttonIconUrl={AddIcon}
                            buttonChanged={true}
                            buttonChangedIconUrl={Delete}
                            buttonChangedLabel={"Удалить из избранного"}
                            isAuthenticated={isAuthenticated}
                            onUnauthorized={() => setShowLoginModal(true)}
                          />
                        </div>
                      ))}
                    </div>
                  }
                  content={
                    collections.slice(4).length > 0 ? (
                      <div>
                        {collections.slice(4).map((collection) => (
                          <div key={collection.collectionId} onClick={() => handleCollectionClick(collection)}>
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
                              button={true}
                              buttonLabel={"Добавить в избранное"}
                              buttonIconUrl={AddIcon}
                              buttonChanged={true}
                              buttonChangedIconUrl={Delete}
                              buttonChangedLabel={"Удалить из избранного"}
                              isAuthenticated={isAuthenticated}
                              onUnauthorized={() => setShowLoginModal(true)}
                            />
                          </div>
                        ))}
                      </div>
                    ) : null
                  }
                />
              ) : (
                <p className="no-books-message">У пользователя пока нет коллекций.</p>
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

      {/* Модальное окно для админского редактирования профиля */}
      {isAdminEditOpen && (
        <Modal open={isAdminEditOpen} onClose={handleCloseAdminEditProfile}>
          <AdminProfileEditMenu
            onClose={handleCloseAdminEditProfile}
            currentUsername={profile?.username || ''}
            currentImageUrl={getAvatarUrl()}
            currentDescription={profile?.profileDescription || null}
            currentNickname={profile?.nickname || null}
            currentEmail={profile?.email || null}
            onUsernameChanged={handleAdminUsernameChanged}
            onProfilePhotoChanged={handleAdminProfilePhotoChanged}
            onDescriptionChanged={handleAdminDescriptionChanged}
            onNicknameChanged={handleAdminNicknameChanged}
            targetUserId={userId}
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
    </main>
  );
}

export default Profile;