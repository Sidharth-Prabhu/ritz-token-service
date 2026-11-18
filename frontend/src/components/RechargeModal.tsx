import React, { useState } from 'react';
import { api } from '../services/api';
import { X, CheckCircle2, CreditCard, ArrowRight, Loader2, Sparkles, AlertCircle } from 'lucide-react';

declare global {
  interface Window {
    Razorpay: any;
  }
}

interface RechargeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  studentId?: string;
  phoneNumber?: string;
  studentName?: string;
}

export const RechargeModal: React.FC<RechargeModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  studentId,
  phoneNumber,
  studentName
}) => {
  const [amount, setAmount] = useState<number>(200);
  const [customAmount, setCustomAmount] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [orderData, setOrderData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState<'SELECT' | 'CHECKOUT' | 'SUCCESS'>('SELECT');

  if (!isOpen) return null;

  const presets = [50, 100, 200, 500, 1000];

  const handleSelectPreset = (val: number) => {
    setAmount(val);
    setCustomAmount('');
    setError(null);
  };

  const handleCustomChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setCustomAmount(val);
    if (val && !isNaN(Number(val))) {
      setAmount(Number(val));
    }
    setError(null);
  };

  const handleInitiateRecharge = async () => {
    if (amount < 10 || amount > 10000) {
      setError('Recharge amount must be between ₹10 and ₹10,000');
      return;
    }

    setIsProcessing(true);
    setError(null);
    try {
      const order = await api.initiateRecharge(amount, phoneNumber, studentId);
      setOrderData(order);

      // Check if Razorpay live/test is enabled and Razorpay script is loaded
      const config = await api.getPaymentConfig();

      if (config.razorpay_enabled && window.Razorpay && order.gatewayOrderId && !order.gatewayOrderId.startsWith('order_sbx_')) {
        // Launch standard Razorpay Checkout Modal
        openRazorpayCheckout(order, config.key_id);
      } else {
        // Fallback to visual checkout / simulated confirmation
        setStep('CHECKOUT');
      }
    } catch (e: any) {
      setError(e.message || 'Failed to create recharge order');
    } finally {
      setIsProcessing(false);
    }
  };

  const openRazorpayCheckout = (order: any, keyId: string) => {
    try {
      const options = {
        key: keyId,
        amount: Math.round(order.amountRupees * 100),
        currency: 'INR',
        name: 'RITZ Campus Wallet',
        description: `Top-up ${order.tokenAmount} RITZ Tokens`,
        order_id: order.gatewayOrderId,
        prefill: {
          name: studentName || order.student_name || 'Student',
          contact: phoneNumber || order.mobile_number || ''
        },
        theme: {
          color: '#2563eb'
        },
        // Enforce UPI only
        config: {
          display: {
            blocks: {
              upi: {
                name: 'Pay via UPI',
                instruments: [
                  {
                    method: 'upi'
                  }
                ]
              }
            },
            sequence: ['block.upi'],
            preferences: {
              show_default_blocks: false
            }
          }
        },
        method: {
          netbanking: false,
          card: false,
          wallet: false,
          upi: true,
          paylater: false
        },
        handler: async (response: any) => {
          setIsProcessing(true);
          try {
            await api.verifyPayment(
              order.orderId,
              response.razorpay_payment_id,
              response.razorpay_signature
            );
            setStep('SUCCESS');
            onSuccess();
          } catch (err: any) {
            setError(err.message || 'Payment signature verification failed.');
            setStep('CHECKOUT');
          } finally {
            setIsProcessing(false);
          }
        },
        modal: {
          ondismiss: () => {
            setIsProcessing(false);
            setStep('CHECKOUT');
          }
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', (resp: any) => {
        setError(`Payment Failed: ${resp.error.description}`);
        setIsProcessing(false);
        setStep('CHECKOUT');
      });
      rzp.open();
    } catch (err: any) {
      console.warn('Failed to open Razorpay modal, falling back to manual confirm', err);
      setStep('CHECKOUT');
    }
  };

  const handleSimulatePayment = async () => {
    if (!orderData) return;
    setIsProcessing(true);
    setError(null);
    try {
      await api.confirmSandboxPayment(orderData.orderId);
      setStep('SUCCESS');
      onSuccess();
    } catch (e: any) {
      setError(e.message || 'Payment confirmation failed');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClose = () => {
    setStep('SELECT');
    setOrderData(null);
    setError(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/40 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="bg-white dark:bg-zinc-900 rounded-t-2xl sm:rounded-2xl max-w-sm w-full p-5 sm:p-6 shadow-xl border border-stone-200 dark:border-zinc-800 relative animate-in fade-in slide-in-from-bottom-4 sm:zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
        
        {/* Mobile Pull Indicator */}
        <div className="w-8 h-1 bg-stone-300 dark:bg-zinc-700 rounded-full mx-auto mb-3 sm:hidden"></div>

        {/* Close Button */}
        <button
          onClick={handleClose}
          className="absolute top-4 right-4 p-1.5 rounded-lg text-stone-400 hover:text-stone-600 dark:hover:text-zinc-300 hover:bg-stone-100 dark:hover:bg-zinc-800 transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        {step === 'SELECT' && (
          <div>
            <div className="mb-3">
              <span className="text-[10px] font-medium tracking-wide text-stone-400 dark:text-zinc-500 uppercase">Wallet Recharge</span>
              <h2 className="text-base font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">Top-up Tokens</h2>
              <p className="text-xs text-stone-500 dark:text-zinc-400 mt-0.5">1 INR = 1 Token. Available instantly across all campus facilities.</p>
            </div>

            {/* Presets */}
            <div className="mt-4">
              <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">Select Amount</label>
              <div className="grid grid-cols-3 gap-1.5 mt-1.5">
                {presets.map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => handleSelectPreset(val)}
                    className={`py-2 rounded-lg border text-xs font-semibold transition-all cursor-pointer ${amount === val && !customAmount ? 'border-zinc-900 dark:border-zinc-100 bg-stone-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100' : 'border-stone-200 dark:border-zinc-700 hover:border-stone-300 dark:hover:border-zinc-600 text-zinc-700 dark:text-zinc-300'}`}
                  >
                    ₹{val}
                  </button>
                ))}
              </div>
            </div>

            {/* Custom Amount */}
            <div className="mt-3">
              <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">Or custom amount (₹10 - ₹10,000)</label>
              <div className="relative mt-1">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-stone-400 font-medium text-xs">₹</span>
                <input
                  type="number"
                  placeholder="Min ₹10 - Max ₹10,000"
                  value={customAmount}
                  onChange={handleCustomChange}
                  className="w-full pl-7 pr-3 py-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-medium text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-900 dark:focus:ring-zinc-100 transition-colors"
                />
              </div>
            </div>

            {error && (
              <div className="mt-3 p-2 rounded-lg bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 text-xs text-red-600 dark:text-red-400 flex items-center space-x-1.5">
                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <button
              onClick={handleInitiateRecharge}
              disabled={isProcessing}
              className="mt-5 w-full py-2.5 bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 font-semibold text-xs rounded-lg shadow-xs flex items-center justify-center space-x-1.5 transition-colors disabled:opacity-50 cursor-pointer active:scale-98"
            >
              {isProcessing ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <><span>Pay ₹{amount} via UPI</span> <ArrowRight className="w-3.5 h-3.5" /></>}
            </button>
          </div>
        )}

        {step === 'CHECKOUT' && orderData && (
          <div>
            <h2 className="text-base font-bold text-zinc-900 dark:text-zinc-100">Confirm Payment</h2>

            <div className="mt-3 bg-stone-50 dark:bg-zinc-800/50 rounded-xl p-3 border border-stone-200 dark:border-zinc-800 space-y-1.5 text-xs">
              <div className="flex justify-between text-stone-500 dark:text-zinc-400">
                <span>Order Ref:</span>
                <span className="font-mono text-zinc-900 dark:text-zinc-100">{orderData.orderId}</span>
              </div>
              <div className="flex justify-between text-stone-500 dark:text-zinc-400">
                <span>Recharge Amount:</span>
                <span className="font-bold text-zinc-900 dark:text-zinc-100">₹{orderData.amountRupees}</span>
              </div>
              <div className="flex justify-between text-stone-500 dark:text-zinc-400">
                <span>Credit Tokens:</span>
                <span className="font-bold text-zinc-900 dark:text-zinc-100">+{orderData.tokenAmount} Tokens</span>
              </div>
            </div>

            {error && (
              <div className="mt-3 p-2 rounded-lg bg-red-50 text-red-600 text-xs">
                {error}
              </div>
            )}

            <div className="mt-5 space-y-2">
              <button
                onClick={handleSimulatePayment}
                disabled={isProcessing}
                className="w-full py-2.5 bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 font-semibold text-xs rounded-lg shadow-xs flex items-center justify-center space-x-1.5 transition-colors disabled:opacity-50 cursor-pointer active:scale-98"
              >
                {isProcessing ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>Confirm Payment (₹{orderData.amountRupees})</span>}
              </button>
              <button
                onClick={() => setStep('SELECT')}
                className="w-full py-2 text-xs text-stone-500 hover:text-stone-700 dark:text-zinc-400 dark:hover:text-zinc-200 font-medium cursor-pointer"
              >
                Cancel
              </button>
            </div>
          </div>
        )}

        {step === 'SUCCESS' && (
          <div className="text-center py-3">
            <div className="w-10 h-10 rounded-full bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto mb-3">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-100">Recharge Successful</h3>
            <p className="text-xs text-stone-500 dark:text-zinc-400 mt-1 max-w-xs mx-auto">
              Your wallet was credited with <span className="font-semibold text-zinc-900 dark:text-zinc-100">{amount} Tokens</span>.
            </p>
            <button
              onClick={handleClose}
              className="mt-5 w-full py-2 bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 font-semibold rounded-lg text-xs transition-colors cursor-pointer"
            >
              Done
            </button>
          </div>
        )}

      </div>
    </div>
  );
};
