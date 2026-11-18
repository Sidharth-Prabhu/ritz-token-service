import React, { createContext, useContext, useEffect, useState } from 'react';
import { User, UserRole, AuthResponse } from '../types';
import { api } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<AuthResponse>;
  loginByPhone: (phoneNumber: string, fullName?: string, email?: string, studentId?: string) => Promise<AuthResponse>;
  logout: () => void;
  switchDemoRole: (role: UserRole) => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  token: null,
  isLoading: true,
  login: async () => ({ success: false } as any),
  loginByPhone: async () => ({ success: false } as any),
  logout: () => {},
  switchDemoRole: async () => {},
});

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('ritz_token'));
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchProfile = async () => {
    try {
      const profile = await api.getProfile();
      setUser(profile);
    } catch (e) {
      logout();
    } finally {
      setIsLoading(false);
    }
  };

  const checkUrlParamsForAutoLogin = async () => {
    try {
      const params = new URLSearchParams(window.location.search);
      const phone = params.get('phone') || params.get('phone_number') || params.get('mobile');
      const name = params.get('name') || params.get('full_name');
      const email = params.get('email');
      const studentId = params.get('student_id') || params.get('user_id');
      const authToken = params.get('token') || params.get('auth_token');

      // If token directly passed - always overwrite existing session
      if (authToken) {
        localStorage.setItem('ritz_token', authToken);
        setToken(authToken);
        window.history.replaceState({}, document.title, window.location.pathname);
        const profile = await api.getProfile();
        setUser(profile);
        setIsLoading(false);
        return;
      }

      // If phone passed in URL from external project - always overwrite existing session
      if (phone) {
        setIsLoading(true);
        const res = await api.loginByPhone({
          phone_number: phone,
          full_name: name || undefined,
          email: email || undefined,
          student_id: studentId || undefined
        });

        if (res.success && res.token) {
          localStorage.setItem('ritz_token', res.token);
          setToken(res.token);
          setUser({
            id: res.userId || 0,
            username: res.username,
            fullName: res.fullName,
            email: res.email,
            role: res.role,
            studentId: res.studentId,
            merchantId: res.merchantId,
            applicationId: res.applicationId,
          });
          // Clean query params so URL is neat
          window.history.replaceState({}, document.title, window.location.pathname);
          setIsLoading(false);
          return;
        }
      }
    } catch (err) {
      console.warn('URL auto-login parameter check failed or was not provided:', err);
    }

    if (token) {
      fetchProfile();
    } else {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    checkUrlParamsForAutoLogin();
  }, []);

  const loginByPhone = async (phoneNumber: string, fullName?: string, email?: string, studentId?: string): Promise<AuthResponse> => {
    const res = await api.loginByPhone({
      phone_number: phoneNumber,
      full_name: fullName,
      email: email,
      student_id: studentId
    });

    if (res.success && res.token) {
      localStorage.setItem('ritz_token', res.token);
      setToken(res.token);
      setUser({
        id: res.userId || 0,
        username: res.username,
        fullName: res.fullName,
        email: res.email,
        role: res.role,
        studentId: res.studentId,
        merchantId: res.merchantId,
        applicationId: res.applicationId,
      });
    }
    return res;
  };

  const login = async (username: string, password: string): Promise<AuthResponse> => {
    const res = await api.login({ username, password });
    if (res.success && res.token) {
      localStorage.setItem('ritz_token', res.token);
      setToken(res.token);
      setUser({
        id: res.userId || 0,
        username: res.username,
        fullName: res.fullName,
        email: res.email,
        role: res.role,
        studentId: res.studentId,
        merchantId: res.merchantId,
        applicationId: res.applicationId,
      });
    }
    return res;
  };

  const logout = () => {
    localStorage.removeItem('ritz_token');
    setToken(null);
    setUser(null);
  };

  const switchDemoRole = async (role: UserRole) => {
    setIsLoading(true);
    let u = '';
    let p = '';
    switch (role) {
      case 'STUDENT':
        u = 'RIT2026001';
        p = 'StudentPass2026!';
        break;
      case 'SUPER_ADMIN':
        u = 'superadmin';
        p = 'RitzAdmin2026!';
        break;
      case 'FINANCE_ADMIN':
        u = 'financeadmin';
        p = 'RitzFinance2026!';
        break;
      case 'MERCHANT_ADMIN':
        u = 'tilloadmin';
        p = 'TilloPass2026!';
        break;
      case 'APP_DEVELOPER':
        u = 'developer';
        p = 'RitzDev2026!';
        break;
    }
    try {
      await login(u, p);
    } catch (e) {
      console.error('Demo login failed', e);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider value={{ user, token, isLoading, login, loginByPhone, logout, switchDemoRole }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
