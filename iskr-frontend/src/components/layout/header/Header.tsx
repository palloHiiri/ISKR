import './Header.scss';
import MainLogo from '../../../assets/elements/logo.svg';
import { useDispatch, useSelector } from "react-redux";
import { logout, checkAuth, clearRegistrationSuccess } from "../../../redux/authSlice.ts";
import PrimaryButton from "../../controls/primary-button/PrimaryButton.tsx";
import SecondaryButton from "../../controls/secondary-button/SecondaryButton.tsx";
import { Link, useNavigate, useLocation } from "react-router-dom";
import homeButton from "../../../assets/elements/homeButton.svg";
import libraryButton from "../../../assets/elements/libraryButton.svg";
import statisticsButton from "../../../assets/elements/statisticsButton.svg";
import settingsButton from "../../../assets/elements/settingsButton.svg";
import type { RootState } from '../../../redux/store';
import type { AppDispatch } from '../../../redux/store';
import Modal from "../../controls/modal/Modal.tsx";
import Login from "../../controls/login/Login.tsx";
import ForgotPassword from "../../controls/forgot-password/ForgotPassword.tsx";
import { useEffect, useState } from "react";
import profileButton from "../../../assets/elements/profileButton.svg";
import profileAPI from '../../../api/profileService';

interface HeaderProps {
  showLoginModal?: boolean;
}

function Header({ showLoginModal = false }: HeaderProps) {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const location = useLocation(); // Добавляем useLocation
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const user = useSelector((state: RootState) => state.auth.user);

  const [modalOpen, setModalOpen] = useState(false);
  const [forgotPasswordOpen, setForgotPasswordOpen] = useState(false);
  const [authType, setAuthType] = useState<'login'|'signup'>('login');
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    if (showLoginModal && !isAuthenticated) {
      setAuthType('login');
      setModalOpen(true);
      navigate('.', { replace: true, state: {} });
    }
  }, [showLoginModal, isAuthenticated, navigate]);

  useEffect(() => {
    const checkAuthStatus = async () => {
      const hasAuthCookie = document.cookie.includes('Authorization=') || 
                          document.cookie.includes('access_token=');
      const isStoredAuth = localStorage.getItem('token') || 
                          localStorage.getItem('user');
      
      if (hasAuthCookie || isStoredAuth) {
        try {
          await dispatch(checkAuth()).unwrap();
        } catch (error) {
          console.log('Not authenticated or session expired');
        }
      }
    };
    
    checkAuthStatus();
  }, [dispatch]);

  useEffect(() => {
    if (user) {
      const isUserAdmin = user.username?.toLowerCase().includes('admin') || 
                         user.role === 'admin';
      setIsAdmin(isUserAdmin);
      
      if (user.status === 'banned') {
        console.log('User is banned');
      }
    }
  }, [user]);

  // Проверяем статус пользователя при каждом изменении пути
  useEffect(() => {
    const checkUserStatus = async () => {
      if (isAuthenticated && user?.id) {
        try {
          // Получаем актуальные данные пользователя
          const response = await profileAPI.getUserProfile(user.id);
          
          // Если пользователь забанен - вылогиниваем
          if (response.status === 'banned') {
            // Показываем сообщение о блокировке            
            // Выполняем logout
            dispatch(logout());
            
            // Перенаправляем на главную страницу
            navigate('/', { replace: true });
          }
        } catch (error) {
          console.error('Ошибка при проверке статуса пользователя:', error);
        }
      }
    };

    // Запускаем проверку при каждом изменении пути
    checkUserStatus();
  }, [location.pathname, isAuthenticated, user, dispatch, navigate]);

  // Проверяем, есть ли сообщение о блокировке в state
  useEffect(() => {
    if (location.state?.showBanMessage) {      
      // Очищаем state чтобы сообщение не показывалось повторно
      navigate(location.pathname, { 
        replace: true, 
        state: {} 
      });
    }
  }, [location.state, navigate, location.pathname]);

  const handleSignIn = () => {
    setAuthType('signup');
    setModalOpen(true);
    dispatch(clearRegistrationSuccess());
  };

  const handleLogin = () => {
    setAuthType('login');
    setModalOpen(true);
    dispatch(clearRegistrationSuccess());
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/');
  };

  const handleSwitchType = () => {
    setAuthType(authType === 'login' ? 'signup' : 'login');
    dispatch(clearRegistrationSuccess());
  };

  const handleForgotPassword = () => {
    setModalOpen(false);
    setForgotPasswordOpen(true);
  };

  const handleModalClose = () => {
    setModalOpen(false);
    setAuthType('login');
    dispatch(clearRegistrationSuccess());
  };

  const handleForgotPasswordClose = () => {
    setForgotPasswordOpen(false);
  };

  const handleBackToLogin = () => {
    setForgotPasswordOpen(false);
    setModalOpen(true);
    setAuthType('login');
  };

  const handleAuthSuccess = () => {
    setModalOpen(false);
    setAuthType('login');
  };

  return (
    <>
      <header>
        <div className="header-container">
          <Link to="/">
            <img className="main-logo" src={MainLogo} alt="logo" />
          </Link>
          {isAuthenticated ? (
            <div className="header-buttons">
              <Link to="/" className="header-button">
                <img src={homeButton} alt="Домой"/>
                <span className="header-button-text">Домой</span>
              </Link>
              <Link to="/library" className="header-button">
                <img src={libraryButton} alt="Библиотека"/>
                <span className="header-button-text">Библиотека</span>
              </Link>
              <Link to="/statistic" className="header-button">
                <img src={statisticsButton} alt="Статистика"/>
                <span className="header-button-text">Статистика</span>
              </Link>
              <Link to="/account" className="header-button">
                <img src={profileButton} alt="Профиль" className="profile-avatar-image"/>
                <span className="header-button-text">Профиль</span>
              </Link>
              {isAdmin && (
                <Link to="/admin" className="header-button">
                  <img src={settingsButton} alt="Админ"/>
                  <span className="header-button-text">Панель админа</span>
                </Link>
              )}
              <PrimaryButton label="Выйти" onClick={handleLogout} />
            </div>
          ) : (
            <div className="login-signup-buttons">
              <SecondaryButton label="Зарегистрироваться" onClick={handleSignIn} />
              <PrimaryButton label="Войти" onClick={handleLogin} />
            </div>
          )}
        </div>
      </header>

      {/* Модальное окно входа/регистрации */}
      <Modal
        open={modalOpen}
        onClose={handleModalClose}
        titleId="auth-dialog-title"
        closeOnBackdropClick={true}
      >
        <Login
          type={authType}
          titleId="auth-dialog-title"
          onSubmit={handleAuthSuccess}
          onSwitchType={handleSwitchType}
          onForgotPassword={handleForgotPassword}
        />
      </Modal>

      {/* Модальное окно восстановления пароля */}
      <Modal
        open={forgotPasswordOpen}
        onClose={handleForgotPasswordClose}
        titleId="forgot-password-dialog-title"
        closeOnBackdropClick={true}
      >
        <ForgotPassword
          onClose={handleForgotPasswordClose}
          onBackToLogin={handleBackToLogin}
          titleId="forgot-password-dialog-title"
        />
      </Modal>
    </>
  );
}

export default Header;