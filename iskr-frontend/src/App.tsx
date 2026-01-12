import {BrowserRouter as Router, Routes, Route, Navigate, useLocation, useNavigate} from 'react-router-dom';
import './App.scss';
import Header from "./components/layout/header/Header.tsx";
import Home from "./components/pages/home/Home.tsx";
import Statistic from "./components/pages/statistic/Statistic.tsx";
import {useDispatch, useSelector} from "react-redux";
import type {RootState} from "./redux/store.ts";
import AdminPage from './components/pages/admin-page/AdminPage.tsx';
import Library from "./components/pages/library/Library.tsx";
import Profile from "./components/pages/profile/Profile.tsx";
import Book from "./components/pages/book/Book.tsx";
import Collection from "./components/pages/collection/Collection.tsx";
import Account from "./components/pages/account/Account.tsx";
import Followers from "./components/pages/followers/Followers.tsx";
import Subscriptions from "./components/pages/subscriptions/Subscriptions.tsx";
import ResetPasswordPage from "./components/pages/reset-password/ResetPasswordPage.tsx";
import ValidateEmailPage from "./components/pages/validate-email/ValidateEmailPage.tsx";
import { logout } from "./redux/authSlice.ts";
import profileAPI from './api/profileService';
import { useEffect, useRef } from 'react';

// Компонент-обертка для передачи состояния в Header
function AppWrapper() {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const user = useSelector((state: RootState) => state.auth.user);

  const showLoginModal = location.state?.showLoginModal || false;

  // Используем useRef для хранения времени последней проверки
  const lastCheckTimeRef = useRef<number>(0);

  // Эффект для проверки статуса пользователя при каждом изменении пути
  useEffect(() => {
    const checkUserStatus = async () => {
      // Проверяем не чаще чем раз в 30 секунд
      const now = Date.now();
      if (now - lastCheckTimeRef.current < 30000) {
        return;
      }

      if (isAuthenticated && user?.id) {
        try {
          lastCheckTimeRef.current = now;
          // Запрашиваем актуальные данные пользователя
          const response = await profileAPI.getUserProfile(user.id);
          
          // Если пользователь забанен, разлогиниваем
          if (response.status === 'banned') {
            // Выполняем logout
            dispatch(logout());
            
            // Перенаправляем на главную страницу
            navigate('/', { replace: true });
            
            // Можно показать сообщение, но в данном случае просто вылогиниваем
          }
        } catch (error) {
          console.error('Ошибка при проверке статуса пользователя:', error);
        }
      }
    };

    // Вызываем проверку при каждом изменении пути
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

  // Проверка статуса при загрузке приложения
  useEffect(() => {
    const checkInitialAuthStatus = async () => {
      const token = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');
      
      if (token && storedUser) {
        try {
          const user = JSON.parse(storedUser);
          const response = await profileAPI.getUserProfile(user.id);
          
          if (response.status === 'banned') {
            // Очищаем данные и показываем сообщение
            localStorage.removeItem('token');
            localStorage.removeItem('user');
          }
        } catch (error) {
          console.error('Ошибка при проверке начального статуса:', error);
        }
      }
    };

    checkInitialAuthStatus();
  }, []);

  return (
    <>
      <Header showLoginModal={showLoginModal} />
      <Routes location={location}>
        <Route path="/" element={<Home />}/>
        <Route path="/statistic" element={isAuthenticated ? <Statistic /> : <Navigate to="/" replace />}/>
        <Route path="/library" element={isAuthenticated ? <Library /> : <Navigate to="/" replace /> }/>
        <Route path="/admin" element={user?.username === 'admin' ? <AdminPage /> : <Navigate to="/" replace /> }/>
        <Route path="/profile" element={<Profile />} />
        <Route path="/book" element={<Book />} />
        <Route path="/collection" element={<Collection />} />
        <Route path="/account" element={isAuthenticated ? <Account /> : <Navigate to="/" replace /> } />
        <Route path="/followers" element={<Followers />} />
        <Route path="/subscriptions" element={<Subscriptions />} />
        <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
        <Route path="/validate-email/:token" element={<ValidateEmailPage />} />
      </Routes>
    </>
  );
}

function App() {
  return (
    <Router>
      <AppWrapper />
    </Router>
  );
}

export default App;