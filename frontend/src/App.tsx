import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Navbar } from './components/Navbar';
import { LoginPage } from './pages/LoginPage';
import { StudentDashboard } from './pages/StudentDashboard';
import { AdminDashboard } from './pages/AdminDashboard';
import { MerchantDashboard } from './pages/MerchantDashboard';
import { DeveloperPortal } from './pages/DeveloperPortal';
import { Loader2 } from 'lucide-react';

const MainLayout: React.FC = () => {
  const { user, isLoading } = useAuth();
  const [currentView, setCurrentView] = useState<string>(() => {
    if (typeof window !== 'undefined' && window.location.pathname.startsWith('/dev-portal')) {
      return 'dev-portal';
    }
    return 'dashboard';
  });

  useEffect(() => {
    const handlePopState = () => {
      if (window.location.pathname.startsWith('/dev-portal')) {
        setCurrentView('dev-portal');
      } else {
        setCurrentView('dashboard');
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const handleNavigate = (view: string) => {
    setCurrentView(view);
    if (view === 'dev-portal') {
      if (window.location.pathname !== '/dev-portal') {
        window.history.pushState({}, '', '/dev-portal');
      }
    } else {
      if (window.location.pathname.startsWith('/dev-portal')) {
        window.history.pushState({}, '', '/');
      }
    }
  };

  const isDevPortal = currentView === 'dev-portal' || (typeof window !== 'undefined' && window.location.pathname.startsWith('/dev-portal'));

  // If loading and not directly navigating to dev-portal, show spinner
  if (isLoading && !isDevPortal) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-stone-50 dark:bg-zinc-950">
        <Loader2 className="w-8 h-8 animate-spin text-zinc-900 dark:text-zinc-100" />
      </div>
    );
  }

  // Developer Portal route can be accessed directly with its password gate
  if (isDevPortal) {
    return (
      <div className="min-h-screen bg-stone-50 dark:bg-zinc-950 text-zinc-800 dark:text-zinc-200 flex flex-col font-sans transition-colors antialiased pb-safe selection:bg-zinc-200 dark:selection:bg-zinc-800">
        <Navbar currentView="dev-portal" onNavigate={handleNavigate} />
        <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-4 sm:py-8">
          <DeveloperPortal />
        </main>
        <footer className="border-t border-stone-200 dark:border-zinc-850 py-5 text-center text-xs text-stone-400 dark:text-zinc-500 px-4">
          <p>© 2026 Rajalakshmi Institute of Technology • RITZ Developer Platform</p>
        </footer>
      </div>
    );
  }

  // Direct student access without campus app redirect is restricted
  if (!user) {
    return <LoginPage />;
  }

  const renderContent = () => {
    switch (user.role) {
      case 'SUPER_ADMIN':
      case 'FINANCE_ADMIN':
        return <AdminDashboard currentTab={currentView.replace('admin-', '')} />;
      case 'MERCHANT_ADMIN':
        return <MerchantDashboard />;
      case 'APP_DEVELOPER':
        return <DeveloperPortal />;
      case 'STUDENT':
      default:
        return <StudentDashboard />;
    }
  };

  return (
    <div className="min-h-screen bg-stone-50 dark:bg-zinc-950 text-zinc-800 dark:text-zinc-200 flex flex-col font-sans transition-colors antialiased pb-safe selection:bg-zinc-200 dark:selection:bg-zinc-800">
      <Navbar currentView={currentView} onNavigate={handleNavigate} />
      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-4 sm:py-8">
        {renderContent()}
      </main>
      <footer className="border-t border-stone-200 dark:border-zinc-850 py-5 text-center text-xs text-stone-400 dark:text-zinc-500 px-4">
        <p>© 2026 Rajalakshmi Institute of Technology • RITZ Wallet</p>
      </footer>
    </div>
  );
};

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <MainLayout />
      </AuthProvider>
    </ThemeProvider>
  );
}
