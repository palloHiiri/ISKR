import { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { checkAuth } from '../../redux/authSlice';
import type { AppDispatch } from '../../redux/store';

export const AuthInitializer = () => {
  const dispatch = useDispatch<AppDispatch>();

  useEffect(() => {
    const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
    
    if (isAuthenticated) {
      dispatch(checkAuth());
    }
  }, [dispatch]);

  return null; 
};