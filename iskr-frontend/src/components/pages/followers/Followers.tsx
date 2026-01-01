import './Followers.scss';
import CardElement from "../../controls/card-element/CardElement.tsx";
import AddIcon from "../../../assets/elements/add.svg";
import Delete from "../../../assets/elements/delete.svg";
import {useLocation, useNavigate, Navigate} from "react-router-dom";
import {useState, useEffect} from "react";
import {useSelector} from "react-redux";
import type {RootState} from "../../../redux/store.ts";
import Modal from "../../controls/modal/Modal.tsx";
import Login from "../../controls/login/Login.tsx";
import {russianLocalWordConverter} from "../../../utils/russianLocalWordConverter.ts";
import PrimaryButton from "../../controls/primary-button/PrimaryButton.tsx";
import PlaceholderImage from '../../../assets/images/placeholder.jpg';
import profileAPI from '../../../api/profileService';
import type { UserSubscriber, PaginatedResponse } from '../../../types/profile';
import { getImageUrl } from '../../../api/popularService';
import SecondaryButton from "../../controls/secondary-button/SecondaryButton.tsx";

function Followers() {
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
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [userFollowStates, setUserFollowStates] = useState<Record<number, boolean>>({});
  
  // Состояния для данных
  const [followers, setFollowers] = useState<UserSubscriber[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pagination, setPagination] = useState({
    page: 0,
    totalPages: 1,
    totalElements: 0,
    batch: 8
  });

  // Загрузка подписчиков
  useEffect(() => {
    const loadFollowers = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const response = await profileAPI.getUserSubscribersPaginated(userId, pagination.batch, pagination.page);
        
        if (pagination.page === 0) {
          setFollowers(response.items);
        } else {
          setFollowers(prev => [...prev, ...response.items]);
        }
        
        setPagination(prev => ({
          ...prev,
          totalPages: response.totalPages,
          totalElements: response.totalElements
        }));
      } catch (err: any) {
        console.error('Error loading followers:', err);
        setError(err.message || 'Ошибка загрузки подписчиков');
      } finally {
        setLoading(false);
      }
    };

    loadFollowers();
  }, [userId, pagination.page, pagination.batch]);

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

  const handleUserClick = (follower: UserSubscriber) => {
    navigate('/profile', {
      state: {
        userId: follower.userId
      }
    });
  };

  const handleUserFollow = (followerId: number) => {
    if (!isAuthenticated) {
      setShowLoginModal(true);
      return;
    }
    
    setUserFollowStates(prev => ({
      ...prev,
      [followerId]: !prev[followerId]
    }));
  };

  const getFollowerCount = (follower: UserSubscriber): string => {
    const isFollowed = userFollowStates[follower.userId] || false;
    const baseCount = follower.subscribersCount || 0;
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

  const handleLoadMore = () => {
    if (pagination.page < pagination.totalPages - 1) {
      setPagination(prev => ({ ...prev, page: prev.page + 1 }));
    }
  };

  const handleBackClick = () => {
    navigate(-1);
  };

  // Рендер состояний загрузки и ошибок
  const renderLoadingState = () => (
    <div className="loading-state">
      <div className="loading-spinner"></div>
      <p>Загрузка подписчиков...</p>
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
    if (isMine) return 'Мои подписчики';
    return `Подписчики ${profile?.displayName || 'пользователя'}`;
  };

  const getEmptyMessage = (): string => {
    if (isMine) return 'У вас пока нет подписчиков.';
    return `У ${profile?.displayName || 'пользователя'} пока нет подписчиков.`;
  };

  return (
    <main>
      <div className="top-container">
        <div className="container-title-with-button">
          <h2>{getTitle()}</h2>
          <PrimaryButton label="Вернуться назад" onClick={handleBackClick} />
        </div>

        <div className="followers-container container">
          {loading && pagination.page === 0 ? (
            renderLoadingState()
          ) : error ? (
            renderErrorState()
          ) : followers.length > 0 ? (
            <>
              <div className="followers-list">
                {followers.map((follower) => {
                  const isFollowed = userFollowStates[follower.userId] || false;
                  const displayName = follower.nickname || follower.username || 'Пользователь';
                  
                  return (
                    <CardElement
                      key={follower.userId}
                      title={displayName}
                      description={getFollowerCount(follower)}
                      imageUrl={getImageUrl(follower.profileImage) || PlaceholderImage}
                      button={!isMine} // Для своих подписчиков кнопка не показывается
                      buttonLabel={isFollowed ? "Отписаться" : "Подписаться"}
                      buttonIconUrl={isFollowed ? Delete : AddIcon}
                      onClick={() => handleUserClick(follower)}
                      onButtonClick={() => handleUserFollow(follower.userId)}
                      isAuthenticated={isAuthenticated}
                      onUnauthorized={() => setShowLoginModal(true)}
                    />
                  );
                })}
              </div>
              
              {pagination.page < pagination.totalPages - 1 && (
                <div className="load-more-container">
                  <PrimaryButton
                    label="Загрузить еще"
                    onClick={handleLoadMore}
                    disabled={loading}
                  />
                </div>
              )}
              
              {loading && pagination.page > 0 && (
                <div className="loading-more">
                  <div className="loading-spinner-small"></div>
                  <p>Загрузка...</p>
                </div>
              )}
            </>
          ) : (
            <p className="no-followers-message">
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
    </main>
  );
}

export default Followers;