import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { api } from '../services/api';
import { Notification, UserRole } from '../types';
import {
  Wallet,
  Sun,
  Moon,
  Bell,
  LogOut,
  User as UserIcon,
  Shield,
  Layers,
  Store,
  Code2,
  CheckCircle2,
  RefreshCw
} from 'lucide-react';

interface NavbarProps {
  currentView: string;
  onNavigate: (view: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentView, onNavigate }) => {
  const { user, logout, switchDemoRole } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showRoleSwitcher, setShowRoleSwitcher] = useState(false);

  const fetchNotifications = async () => {
    if (!user) return;
    try {
      const data = await api.getNotifications();
      setNotifications(data.notifications || []);
      setUnreadCount(data.unread_count || 0);
    } catch (e) {
      // Ignored
    }
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 15000);
    return () => clearInterval(interval);
  }, [user]);

  const handleMarkAllRead = async () => {
    try {
      await api.markAllNotificationsRead();
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    } catch (e) {
      console.error(e);
    }
  };

  const getRoleBadge = (role: UserRole) => {
    switch (role) {
      case 'SUPER_ADMIN':
        return <span className="bg-red-500/10 text-red-600 dark:text-red-400 border border-red-500/20 text-xs px-2 py-0.5 rounded-full font-semibold">Super Admin</span>;
      case 'FINANCE_ADMIN':
        return <span className="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 text-xs px-2 py-0.5 rounded-full font-semibold">Finance Admin</span>;
      case 'MERCHANT_ADMIN':
        return <span className="bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 text-xs px-2 py-0.5 rounded-full font-semibold">Merchant</span>;
      case 'APP_DEVELOPER':
        return <span className="bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20 text-xs px-2 py-0.5 rounded-full font-semibold">Developer</span>;
      default:
        return <span className="bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20 text-xs px-2 py-0.5 rounded-full font-semibold">Student</span>;
    }
  };

  return (
    <header className="sticky top-0 z-40 bg-white/95 dark:bg-zinc-900/95 backdrop-blur-md border-b border-stone-200 dark:border-zinc-800">
      <div className="max-w-5xl mx-auto px-4 sm:px-6">
        <div className="flex justify-between h-14 items-center">
          
          {/* Logo & Brand */}
          <div className="flex items-center space-x-2.5 cursor-pointer" onClick={() => onNavigate('dashboard')}>
            <div className="w-8 h-8 rounded-lg bg-zinc-900 dark:bg-zinc-100 flex items-center justify-center text-white dark:text-zinc-900 shadow-xs shrink-0">
              <Wallet className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="font-bold text-base tracking-tight text-zinc-900 dark:text-zinc-100">RITZ</span>
                <span className="text-[10px] font-medium px-1.5 py-0.2 rounded bg-stone-100 dark:bg-zinc-800 text-stone-600 dark:text-zinc-400">WALLET</span>
              </div>
              <p className="text-[9px] text-stone-400 dark:text-zinc-500 uppercase tracking-wider font-medium">Rajalakshmi Inst. of Tech.</p>
            </div>
          </div>

          {/* Navigation Links based on role */}
          {user && (
            <nav className="hidden md:flex space-x-1 lg:space-x-2 text-sm font-medium">
              {user.role === 'STUDENT' && (
                <>
                  <button
                    onClick={() => onNavigate('dashboard')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'dashboard' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Wallet
                  </button>
                  <button
                    onClick={() => onNavigate('history')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'history' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    History
                  </button>
                  <button
                    onClick={() => onNavigate('apps')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'apps' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Campus Apps
                  </button>
                </>
              )}

              {(user.role === 'SUPER_ADMIN' || user.role === 'FINANCE_ADMIN') && (
                <>
                  <button
                    onClick={() => onNavigate('admin-overview')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'admin-overview' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Executive Overview
                  </button>
                  <button
                    onClick={() => onNavigate('admin-ledger')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'admin-ledger' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Double-Entry Ledger
                  </button>
                  <button
                    onClick={() => onNavigate('admin-reconciliation')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'admin-reconciliation' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Reconciliation
                  </button>
                  <button
                    onClick={() => onNavigate('admin-wallets')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'admin-wallets' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Wallets & Apps
                  </button>
                  <button
                    onClick={() => onNavigate('admin-reports')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'admin-reports' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Reports & CSV
                  </button>
                </>
              )}

              {user.role === 'MERCHANT_ADMIN' && (
                <>
                  <button
                    onClick={() => onNavigate('merchant-dashboard')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'merchant-dashboard' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Merchant Sales
                  </button>
                  <button
                    onClick={() => onNavigate('merchant-settlements')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'merchant-settlements' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Settlements
                  </button>
                </>
              )}

              {user.role === 'APP_DEVELOPER' && (
                <>
                  <button
                    onClick={() => onNavigate('dev-portal')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'dev-portal' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    API Credentials
                  </button>
                  <button
                    onClick={() => onNavigate('dev-sandbox')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'dev-sandbox' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Live Sandbox
                  </button>
                  <button
                    onClick={() => onNavigate('dev-docs')}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${currentView === 'dev-docs' ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : 'text-slate-600 dark:text-slate-300 hover:text-blue-600'}`}
                  >
                    Integration SDKs
                  </button>
                </>
              )}
            </nav>
          )}

          {/* Right actions: Theme toggle, Demo Persona switcher, Notifications, Profile */}
          <div className="flex items-center space-x-2 sm:space-x-3">
            
            {/* Notifications Bell */}
            {user && (
              <div className="relative">
                <button
                  onClick={() => setShowNotifications(!showNotifications)}
                  className="p-1.5 rounded-lg text-zinc-500 hover:text-zinc-800 dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-stone-100 dark:hover:bg-zinc-800 transition-colors relative"
                  title="Notifications"
                >
                  <Bell className="w-4 h-4" />
                  {unreadCount > 0 && (
                    <span className="absolute top-1 right-1 w-1.5 h-1.5 rounded-full bg-blue-600 ring-2 ring-white dark:ring-zinc-900"></span>
                  )}
                </button>

                {showNotifications && (
                  <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white dark:bg-zinc-900 rounded-xl shadow-xl border border-stone-200 dark:border-zinc-800 z-50 overflow-hidden">
                    <div className="p-3 border-b border-stone-100 dark:border-zinc-800 flex justify-between items-center bg-stone-50/50 dark:bg-zinc-900/50">
                      <div className="flex items-center space-x-2">
                        <span className="font-semibold text-xs text-zinc-900 dark:text-zinc-100">Notifications</span>
                        {unreadCount > 0 && (
                          <span className="bg-stone-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 text-[10px] px-2 py-0.2 rounded-full font-medium">
                            {unreadCount} new
                          </span>
                        )}
                      </div>
                      {unreadCount > 0 && (
                        <button
                          onClick={handleMarkAllRead}
                          className="text-[11px] text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100 font-medium"
                        >
                          Mark all read
                        </button>
                      )}
                    </div>

                    <div className="max-h-80 overflow-y-auto divide-y divide-stone-100 dark:divide-zinc-800/60">
                      {notifications.length === 0 ? (
                        <div className="py-8 text-center text-xs text-stone-400">No notifications</div>
                      ) : (
                        notifications.map((notif) => (
                          <div
                            key={notif.id}
                            className={`p-3 transition-colors ${notif.read ? 'bg-white dark:bg-zinc-900' : 'bg-stone-50 dark:bg-zinc-800/40'}`}
                          >
                            <div className="flex items-start justify-between">
                              <span className="font-medium text-xs text-zinc-900 dark:text-zinc-100">{notif.title}</span>
                              <span className="text-[10px] text-zinc-400">
                                {new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </span>
                            </div>
                            <p className="text-[11px] text-zinc-600 dark:text-zinc-400 mt-0.5 leading-relaxed">{notif.message}</p>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className="p-1.5 rounded-lg text-zinc-500 hover:text-zinc-800 dark:text-zinc-400 dark:hover:text-zinc-200 hover:bg-stone-100 dark:hover:bg-zinc-800 transition-colors"
              title="Toggle Theme"
            >
              {theme === 'dark' ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-zinc-600" />}
            </button>

            {/* User Profile */}
            {user ? (
              <div className="flex items-center space-x-2 pl-2 border-l border-stone-200 dark:border-zinc-800">
                <div className="text-right">
                  <div className="text-xs font-semibold text-zinc-900 dark:text-zinc-100 leading-tight">{user.fullName}</div>
                  <div className="text-[10px] text-zinc-400">{user.studentId || user.username}</div>
                </div>
              </div>
            ) : null}

          </div>
        </div>
      </div>
    </header>
  );
};
