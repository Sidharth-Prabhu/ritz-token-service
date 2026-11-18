import React from 'react';
import { Wallet, ShieldAlert } from 'lucide-react';

export const LoginPage: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col justify-center py-6 sm:py-12 px-4 sm:px-6 lg:px-8 bg-stone-50 dark:bg-zinc-950 transition-colors">
      <div className="w-full max-w-sm mx-auto text-center">
        <div className="w-12 h-12 rounded-xl bg-zinc-900 dark:bg-zinc-100 flex items-center justify-center text-white dark:text-zinc-900 shadow-xs mx-auto mb-3">
          <Wallet className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-100 tracking-tight">RITZ Campus Wallet</h2>
        <p className="text-[11px] text-stone-400 dark:text-zinc-500 uppercase tracking-wider font-medium mt-0.5">
          Rajalakshmi Institute of Technology
        </p>
      </div>

      <div className="mt-5 w-full max-w-sm mx-auto">
        <div className="bg-white dark:bg-zinc-900 py-6 px-5 sm:px-7 rounded-2xl border border-stone-200 dark:border-zinc-800 shadow-xs text-center">
          
          <div className="w-10 h-10 rounded-full bg-stone-100 dark:bg-zinc-800 flex items-center justify-center mx-auto mb-3 text-stone-500 dark:text-zinc-400">
            <ShieldAlert className="w-5 h-5" />
          </div>

          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">Direct Access Restricted</h3>
          <p className="text-xs text-stone-500 dark:text-zinc-400 mt-2 leading-relaxed">
            RITZ Wallet sessions are managed and authenticated directly through integrated campus applications (Tillo Canteen, Campus ERP, Transport).
          </p>

          <div className="mt-5 p-3 rounded-xl bg-stone-50 dark:bg-zinc-800/50 border border-stone-200 dark:border-zinc-800 text-[11px] text-stone-600 dark:text-zinc-400 leading-normal text-left">
            <span className="font-semibold text-zinc-900 dark:text-zinc-200 block mb-1">How to access:</span>
            Open your campus app (e.g. Tillo Canteen) and click <strong>"RITZ Wallet / Top-up"</strong>. You will be authenticated automatically with your active student profile.
          </div>

          <div className="mt-5 pt-4 border-t border-stone-100 dark:border-zinc-800/80">
            <span className="text-[10px] text-stone-400 dark:text-zinc-500">
              Institutional double-entry ledger protection
            </span>
          </div>

        </div>
      </div>
    </div>
  );
};
