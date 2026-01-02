import './Subscriptions.scss';
import CardElement from "../../controls/card-element/CardElement.tsx";
import Delete from "../../../assets/elements/delete.svg";
import {useLocation, useNavigate, Navigate} from "react-router-dom";
import {useState, useEffect} from "react";
import {useSelector} from "react-redux";
import type {RootState} from "../../../redux/store.ts";
import Modal from "../../controls/modal/Modal.tsx";
import Login from "../../controls/login/Login.tsx";
import ConfirmDialog from "../../controls/confirm-dialog/ConfirmDialog.tsx";
import PrimaryButton from "../../controls/primary-button/PrimaryButton.tsx";
import AddIcon from "../../../assets/elements/add.svg";
import {russianLocalWordConverter} from "../../../utils/russianLocalWordConverter.ts";
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import profileAPI from '../../../api/profileService';
import type { UserSubscription } from '../../../types/profile';
import { getImageUrl } from '../../../api/popularService';
import SecondaryButton from "../../controls/secondary-button/SecondaryButton.tsx";

function Subscriptions() {
  const location = useLocation();
  const navigate = useNavigate();
  
  // Получаем userId и isMine из state
  const userId = location.state?.userId;
  const isMine = location.state?.isMine || false;
  
  // Если userId нет - редирект на главную
  if (!userId) {
    return <Navigate to="/" replace />;
  }

  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const [userFollowStates, setUserFollowStates] = useState<Record<number, boolean>>({});
  const [showLoginModal, setShowLoginModal] = useState(false);
  
  // Состояния для данных
  const [subscriptions, setSubscriptions] = useState<UserSubscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Состояния для модалки отписки
  const [showUnsubscribeModal, setShowUnsubscribeModal] = useState(false);
  const [selectedSubscriptionId, setSelectedSubscriptionId] = useState<number | null>(null);

  // Загрузка подписок - загружаем всех сразу (большой batch)
  useEffect(() => {
    const loadSubscriptions = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Загружаем все подписки сразу (1000 - большое число чтобы охватить всех)
        const subscriptionsData = await profileAPI.getUserSubscriptions(userId, 1000, 0);
        setSubscriptions(subscriptionsData);
      } catch (err: any) {
        console.error('Error loading subscriptions:', err);
        setError(err.message || 'Ошибка загрузки подписок');
      } finally {
        setLoading(false);
      }
    };

    loadSubscriptions();
  }, [userId]);

  // Загрузка профиля для получения username (для заголовка)
  const [profile, setProfile] = useState<{ displayName: string } | null>(null);
  
  useEffect(() => {
    const loadProfile = async () => {
      try {
        const profileData = await profileAPI.getUserProfile(userId);
        setProfile({
          displayName: profileData.nickname || profileData.username || 'Пользователь'
        });
      } catch (err) {
        console.error('Error loading profile for title:', err);
      }
    };
    
    loadProfile();
  }, [userId]);

  const handleUserClick = (subscription: UserSubscription) => {
    navigate('/profile', {
      state: {
        userId: subscription.userId
      }
    });
  };

  const handleUnsubscribe = (subscriptionId: number) => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }
    
    setSelectedSubscriptionId(subscriptionId);
    setShowUnsubscribeModal(true);
  };

  const confirmUnsubscribe = () => {
    if (selectedSubscriptionId !== null) {
      // Здесь должен быть API вызов для отписки
      // Пока просто удаляем из состояния
      setSubscriptions(prev => prev.filter(sub => sub.userId !== selectedSubscriptionId));
    }
    setShowUnsubscribeModal(false);
    setSelectedSubscriptionId(null);
  };

  const handleUserFollow = (subscriptionId: number) => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }
    
    setUserFollowStates(prev => ({
      ...prev,
      [subscriptionId]: !prev[subscriptionId]
    }));
  };

  const getFollowerCount = (subscription: UserSubscription): string => {
    const isFollowed = userFollowStates[subscription.userId] || false;
    const baseCount = subscription.subscribersCount || 0;
    const newCount = isFollowed ? baseCount + 1 : baseCount;
    const formattedCount = newCount.toLocaleString('ru-RU');
    
    return `${formattedCount} ${russianLocalWordConverter(
      newCount,
      'подписчик',
      'подписчика',
      'подписчиков',
      'подписчиков'
    )}`;
  };

  const handleBackClick = () => {
    navigate(-1);
  };

  // Рендер состояний загрузки и ошибок
  const renderLoadingState = () => (
    <div className="loading-state">
      <div className="loading-spinner"></div>
      <p>Загрузка подписок...</p>
    </div>
  );

  const renderErrorState = () => (
    <div className="error-state">
      <p>Ошибка: {error}</p>
      <SecondaryButton 
        label="Вернуться назад" 
        onClick={handleBackClick}
      />
    </div>
  );

  const getTitle = (): string => {
    if (isMine) return 'Мои подписки';
    return `Подписки ${profile?.displayName || 'пользователя'}`;
  };

  const getEmptyMessage = (): string => {
    if (isMine) return 'У вас пока нет подписок.';
    return `У ${profile?.displayName || 'пользователя'} пока нет подписок.`;
  };

  return (
    <main>
      <div className="top-container">
        <div className="container-title-with-button">
          <h2>{getTitle()}</h2>
          <PrimaryButton label="Вернуться назад" onClick={handleBackClick} />
        </div>

        <div className="subscriptions-container container">
          {loading ? (
            renderLoadingState()
          ) : error ? (
            renderErrorState()
          ) : subscriptions.length > 0 ? (
            <div className="subscriptions-list">
              {subscriptions.map((subscription) => {
                const isFollowed = userFollowStates[subscription.userId] || false;
                const displayName = subscription.nickname || subscription.username || 'Пользователь';
                
                return isMine ? (
                  <CardElement
                    key={subscription.userId}
                    title={displayName}
                    description={getFollowerCount(subscription)}
                    imageUrl={getImageUrl(subscription.profileImage) || PlaceholderImage}
                    button={true}
                    buttonLabel="Отписаться"
                    buttonIconUrl={Delete}
                    onClick={() => handleUserClick(subscription)}
                    isAuthenticated={isAuthenticated}
                    onUnauthorized={() => setShowLoginModal(true)}
                    onButtonClick={() => handleUnsubscribe(subscription.userId)}
                  />
                ) : (
                  <CardElement
                    key={subscription.userId}
                    title={displayName}
                    description={getFollowerCount(subscription)}
                    imageUrl={getImageUrl(subscription.profileImage) || PlaceholderImage}
                    button={true}
                    buttonLabel={isFollowed ? "Отписаться" : "Подписаться"}
                    buttonIconUrl={isFollowed ? Delete : AddIcon}
                    onClick={() => handleUserClick(subscription)}
                    onButtonClick={() => handleUserFollow(subscription.userId)}
                    isAuthenticated={isAuthenticated}
                    onUnauthorized={() => setShowLoginModal(true)}
                  />
                );
              })}
            </div>
          ) : (
            <p className="no-subscriptions-message">
              {getEmptyMessage()}
            </p>
          )}
        </div>
      </div>

      <Modal
        open={showLoginModal}
        onClose={() => setShowLoginModal(false)}
      >
        <Login
          type="login"
          onSubmit={() => setShowLoginModal(false)}
        />
      </Modal>

      <Modal open={showUnsubscribeModal} onClose={() => setShowUnsubscribeModal(false)}>
        <ConfirmDialog
          title="Подтверждение отписки"
          message="Вы уверены, что хотите отписаться от этого пользователя?"
          onConfirm={confirmUnsubscribe}
          onCancel={() => setShowUnsubscribeModal(false)}
        />
      </Modal>
    </main>
  );
}

export default Subscriptions;