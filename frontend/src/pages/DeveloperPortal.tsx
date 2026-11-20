import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { RegisteredApplication } from '../types';
import {
  Code2,
  Key,
  RefreshCw,
  Play,
  Copy,
  Check,
  Terminal,
  ShieldCheck,
  Lock,
  PlusCircle,
  AlertTriangle,
  RotateCcw,
  Layers,
  ArrowRight,
  ExternalLink,
  Trash2,
  CheckCircle2,
  Loader2,
  Sparkles
} from 'lucide-react';

export const DeveloperPortal: React.FC = () => {
  // Authentication State
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    return !!localStorage.getItem('ritz_dev_token');
  });
  const [password, setPassword] = useState<string>('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [isAuthenticating, setIsAuthenticating] = useState<boolean>(false);

  // Application Data State
  const [applications, setApplications] = useState<RegisteredApplication[]>([]);
  const [selectedApp, setSelectedApp] = useState<RegisteredApplication | null>(null);
  const [isLoadingApps, setIsLoadingApps] = useState<boolean>(false);
  const [selectedLanguage, setSelectedLanguage] = useState<'javascript' | 'python' | 'java'>('javascript');
  const [copiedSnippet, setCopiedSnippet] = useState<boolean>(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);

  // New Application Modal State
  const [showRegModal, setShowRegModal] = useState<boolean>(false);
  const [newAppId, setNewAppId] = useState<string>('');
  const [newAppName, setNewAppName] = useState<string>('');
  const [newAppDesc, setNewAppDesc] = useState<string>('');
  const [newAppCategory, setNewAppCategory] = useState<string>('CAMPUS_SERVICES');
  const [isSubmittingApp, setIsSubmittingApp] = useState<boolean>(false);
  const [regResult, setRegResult] = useState<any>(null);
  const [regError, setRegError] = useState<string | null>(null);

  // Rotate Key State
  const [rotationResult, setRotationResult] = useState<any>(null);
  const [showRotateModal, setShowRotateModal] = useState<boolean>(false);
  const [isRotating, setIsRotating] = useState<boolean>(false);

  // Interactive Live Sandbox Runner State
  const [testStudentId, setTestStudentId] = useState<string>('');
  const [testAmount, setTestAmount] = useState<number>(50);
  const [testRefId, setTestRefId] = useState<string>('ORDER-' + Math.floor(Math.random() * 90000 + 10000));
  const [testEndpoint, setTestEndpoint] = useState<'debit' | 'balance'>('debit');
  const [sandboxResponse, setSandboxResponse] = useState<any>(null);
  const [sandboxLoading, setSandboxLoading] = useState<boolean>(false);

  const loadApplications = async () => {
    setIsLoadingApps(true);
    try {
      const apps = await api.getApplications();
      setApplications(apps || []);
      if (apps && apps.length > 0 && !selectedApp) {
        setSelectedApp(apps[0]);
      }
    } catch (e: any) {
      if (e.status === 401 || e.status === 403) {
        // Dev token expired or invalid
        localStorage.removeItem('ritz_dev_token');
        setIsAuthenticated(false);
      }
      console.error('Failed to load registered applications:', e);
    } finally {
      setIsLoadingApps(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      loadApplications();
    }
  }, [isAuthenticated]);

  const handlePasswordLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password.trim()) {
      setAuthError('Please enter the developer access password');
      return;
    }

    setIsAuthenticating(true);
    setAuthError(null);

    try {
      const res = await api.loginDevPortal(password);
      if (res && res.token) {
        localStorage.setItem('ritz_dev_token', res.token);
        localStorage.setItem('ritz_token', res.token); // Synchronize active token
        setIsAuthenticated(true);
      } else {
        setAuthError(res?.error || 'Authentication failed');
      }
    } catch (err: any) {
      setAuthError(err.message || 'Incorrect developer portal password');
    } finally {
      setIsAuthenticating(false);
    }
  };

  const handleDevLogout = () => {
    localStorage.removeItem('ritz_dev_token');
    setIsAuthenticated(false);
    setPassword('');
    setApplications([]);
    setSelectedApp(null);
  };

  const handleRegisterApp = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegError(null);
    setIsSubmittingApp(true);
    try {
      const cleanId = newAppId.trim().toUpperCase().replace(/[^A-Z0-9_]/g, '_');
      const res = await api.registerApplication({
        application_id: cleanId,
        application_name: newAppName.trim(),
        description: newAppDesc.trim() || undefined,
        category: newAppCategory,
        scopes: ['TRANSACTION_DEBIT', 'TRANSACTION_CREDIT', 'TRANSACTION_REFUND', 'TRANSACTION_VALIDATE', 'BALANCE_READ', 'MERCHANT_READ']
      });

      setRegResult(res);
      await loadApplications();
      if (res.application_id) {
        const found = applications.find(a => a.applicationId === res.application_id);
        if (found) setSelectedApp(found);
      }
    } catch (e: any) {
      setRegError(e.message || 'Failed to register project application');
    } finally {
      setIsSubmittingApp(false);
    }
  };

  const handleRotateCredentials = async (appId: string) => {
    setIsRotating(true);
    try {
      const res = await api.rotateCredentials(appId);
      setRotationResult(res);
      setShowRotateModal(true);
      await loadApplications();
    } catch (e: any) {
      alert(`Error rotating credentials: ${e.message}`);
    } finally {
      setIsRotating(false);
    }
  };

  const handleRunSandbox = async () => {
    if (!selectedApp) return;
    setSandboxLoading(true);
    setSandboxResponse(null);
    try {
      let res;
      if (testEndpoint === 'debit') {
        res = await api.testDebit(
          {
            phone_number: testStudentId.includes('@') || testStudentId.startsWith('RIT') ? undefined : testStudentId,
            student_id: testStudentId.startsWith('RIT') ? testStudentId : undefined,
            amount: testAmount,
            currency: 'RITZ',
            reference_id: testRefId,
            merchant_id: selectedApp.applicationId,
            description: `Sandbox test debit from ${selectedApp.applicationName}`
          },
          selectedApp.apiKey,
          testRefId
        );
      } else {
        res = await api.testStudentBalanceQuery(testStudentId, selectedApp.apiKey);
      }
      setSandboxResponse(res);
    } catch (e: any) {
      setSandboxResponse({
        success: false,
        status: e.status || 400,
        error: e.message || 'Request failed'
      });
    } finally {
      setSandboxLoading(false);
    }
  };

  const copyToClipboard = (text: string, fieldName: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(fieldName);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const copyCodeSnippet = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedSnippet(true);
    setTimeout(() => setCopiedSnippet(false), 2000);
  };

  // Code Snippets for Integrations
  const activeApiKey = selectedApp ? selectedApp.apiKey : 'rit_live_key_xxxxxxxxxxxxxxxx';
  const activeAppId = selectedApp ? selectedApp.applicationId : 'MY_PROJECT';

  const codeSnippets = {
    javascript: `// Node.js / JavaScript Axios Integration for ${activeAppId}
import axios from 'axios';

const RITZ_BASE_URL = 'https://ritz.rit-services.in';
const API_KEY = '${activeApiKey}';
const API_SECRET = process.env.RITZ_API_SECRET; // Hashed secret generated on registration

const ritzClient = axios.create({
  baseURL: RITZ_BASE_URL,
  headers: {
    'X-API-KEY': API_KEY,
    'X-API-SECRET': API_SECRET,
    'Content-Type': 'application/json'
  }
});

// 1. Check Student Balance by Phone or Student ID
export async function getStudentBalance(phoneOrStudentId) {
  const isPhone = !phoneOrStudentId.startsWith('RIT');
  const endpoint = isPhone 
    ? \`/api/v1/students/phone/\${phoneOrStudentId}/balance\`
    : \`/api/v1/students/\${phoneOrStudentId}/balance\`;
    
  const response = await ritzClient.get(endpoint);
  return response.data; // { balance: 240, can_transact: true, ... }
}

// 2. Deduct Tokens (Debit) with Idempotency Protection
export async function chargeTokens(phoneOrStudentId, tokenAmount, orderRef) {
  const isPhone = !phoneOrStudentId.startsWith('RIT');
  const response = await ritzClient.post(
    '/api/v1/transactions/debit',
    {
      [isPhone ? 'phone_number' : 'student_id']: phoneOrStudentId,
      amount: tokenAmount,
      reference_id: orderRef,
      merchant_id: '${activeAppId}',
      description: 'Campus project service purchase'
    },
    {
      headers: {
        'Idempotency-Key': orderRef
      }
    }
  );
  return response.data; // { success: true, transaction_id: 'RZTXN-...', current_balance: 160 }
}`,

    python: `# Python Requests Integration for ${activeAppId}
import os
import requests

RITZ_BASE_URL = "https://ritz.rit-services.in"
API_KEY = "${activeApiKey}"
API_SECRET = os.getenv("RITZ_API_SECRET") # Store securely in environment

headers = {
    "X-API-KEY": API_KEY,
    "X-API-SECRET": API_SECRET,
    "Content-Type": "application/json"
}

def check_balance(phone_or_student_id: str):
    if phone_or_student_id.startswith("RIT"):
        url = f"{RITZ_BASE_URL}/api/v1/students/{phone_or_student_id}/balance"
    else:
        url = f"{RITZ_BASE_URL}/api/v1/students/phone/{phone_or_student_id}/balance"
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    return response.json()

def debit_wallet(phone_or_student_id: str, amount: int, order_ref: str):
    url = f"{RITZ_BASE_URL}/api/v1/transactions/debit"
    debit_headers = {**headers, "Idempotency-Key": order_ref}
    payload = {
        ("student_id" if phone_or_student_id.startswith("RIT") else "phone_number"): phone_or_student_id,
        "amount": amount,
        "reference_id": order_ref,
        "merchant_id": "${activeAppId}",
        "description": "Campus project purchase"
    }
    response = requests.post(url, json=payload, headers=debit_headers)
    response.raise_for_status()
    return response.json()`,

    java: `// Java 17+ Standard HttpClient Integration for ${activeAppId}
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RitzWalletClient {
    private static final String BASE_URL = "https://ritz.rit-services.in";
    private static final String API_KEY = "${activeApiKey}";
    private static final String API_SECRET = System.getenv("RITZ_API_SECRET");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String debitStudent(String studentPhone, int amount, String orderRef) throws Exception {
        String payload = """
            {
              "phone_number": "%s",
              "amount": %d,
              "reference_id": "%s",
              "merchant_id": "%s",
              "description": "Purchase via %s"
            }
            """.formatted(studentPhone, amount, orderRef, "${activeAppId}", "${activeAppId}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/transactions/debit"))
                .header("X-API-KEY", API_KEY)
                .header("X-API-SECRET", API_SECRET)
                .header("Idempotency-Key", orderRef)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}`
  };

  // If not authenticated, render the Password Gate
  if (!isAuthenticated) {
    return (
      <div className="min-h-[75vh] flex flex-col justify-center items-center py-6 px-4">
        <div className="w-full max-w-sm">
          
          <div className="text-center mb-6">
            <div className="w-12 h-12 rounded-2xl bg-zinc-900 dark:bg-zinc-100 text-white dark:text-zinc-900 flex items-center justify-center mx-auto mb-3 shadow-xs">
              <Lock className="w-5 h-5" />
            </div>
            <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-100">Developer Portal</h2>
            <p className="text-xs text-stone-500 dark:text-zinc-400 mt-1">
              Campus API management and credential provisioning
            </p>
          </div>

          <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-stone-200 dark:border-zinc-800 p-6 shadow-xs">
            <form onSubmit={handlePasswordLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                  Developer Access Password
                </label>
                <div className="relative">
                  <input
                    type="password"
                    required
                    autoFocus
                    placeholder="Enter developer password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full px-3 py-2 text-sm rounded-lg border border-stone-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 placeholder-stone-400 focus:outline-none focus:ring-1 focus:ring-zinc-900 dark:focus:ring-zinc-100 transition-colors"
                  />
                </div>
              </div>

              {authError && (
                <div className="p-2.5 rounded-lg bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 text-xs text-red-700 dark:text-red-400 text-center">
                  {authError}
                </div>
              )}

              <button
                type="submit"
                disabled={isAuthenticating}
                className="w-full py-2.5 px-4 rounded-lg bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 font-semibold text-xs transition-all flex items-center justify-center space-x-1.5 disabled:opacity-50 cursor-pointer active:scale-98"
              >
                {isAuthenticating ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Verifying...</span>
                  </>
                ) : (
                  <>
                    <span>Unlock Developer Portal</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </>
                )}
              </button>
            </form>

            <div className="mt-5 pt-4 border-t border-stone-100 dark:border-zinc-800/80 text-center">
              <span className="text-[10px] text-stone-400 dark:text-zinc-500">
                Institutional API Management • Authorized Access Only
              </span>
            </div>
          </div>

        </div>
      </div>
    );
  }

  // Authenticated Developer Portal View
  return (
    <div className="space-y-6 animate-in fade-in duration-150">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-5 rounded-2xl bg-white dark:bg-zinc-900 border border-stone-200 dark:border-zinc-800 shadow-xs gap-3">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 flex items-center justify-center shrink-0">
            <Code2 className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base sm:text-lg font-bold text-zinc-900 dark:text-zinc-100">Developer Portal</h1>
            <p className="text-xs text-stone-500 dark:text-zinc-400 mt-0.5">
              Manage external project applications, generate secure API keys, and test sandbox requests
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <button
            onClick={() => { setShowRegModal(true); setRegResult(null); setRegError(null); }}
            className="flex-1 sm:flex-initial px-4 py-2 rounded-lg bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 font-semibold text-xs transition-colors flex items-center justify-center space-x-1.5 cursor-pointer shadow-xs active:scale-98"
          >
            <PlusCircle className="w-3.5 h-3.5" />
            <span>Create New Project</span>
          </button>
          <button
            onClick={handleDevLogout}
            className="px-3 py-2 rounded-lg border border-stone-200 dark:border-zinc-700 text-stone-600 dark:text-zinc-400 hover:bg-stone-100 dark:hover:bg-zinc-800 font-medium text-xs transition-colors cursor-pointer"
            title="Lock Developer Portal"
          >
            Lock
          </button>
        </div>
      </div>

      {/* Main Grid: Projects List + Project Details & Sandbox */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        
        {/* Left Column: Registered Project Applications */}
        <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-stone-200 dark:border-zinc-800 p-4 sm:p-5 shadow-xs space-y-3">
          <div className="flex justify-between items-center pb-2 border-b border-stone-100 dark:border-zinc-800">
            <h3 className="font-bold text-xs uppercase tracking-wider text-stone-500 dark:text-zinc-400">Registered Projects</h3>
            <span className="text-[11px] font-medium text-stone-400">{applications.length} Active</span>
          </div>

          {isLoadingApps ? (
            <div className="py-8 text-center text-xs text-stone-400">Loading projects...</div>
          ) : applications.length === 0 ? (
            <div className="py-8 text-center text-xs text-stone-400">
              No applications registered yet. Click "Create New Project" above.
            </div>
          ) : (
            <div className="space-y-2">
              {applications.map((app) => {
                const isSelected = selectedApp?.id === app.id;
                return (
                  <div
                    key={app.id}
                    onClick={() => setSelectedApp(app)}
                    className={`p-3 rounded-xl border transition-all cursor-pointer ${
                      isSelected
                        ? 'border-zinc-900 dark:border-zinc-100 bg-stone-50 dark:bg-zinc-800/60 shadow-xs'
                        : 'border-stone-200 dark:border-zinc-800 hover:border-stone-300 dark:hover:border-zinc-700'
                    }`}
                  >
                    <div className="flex justify-between items-start">
                      <span className="font-bold text-xs text-zinc-900 dark:text-zinc-100">{app.applicationName}</span>
                      <span className={`text-[9px] font-semibold px-1.5 py-0.2 rounded-full ${app.status === 'ACTIVE' ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400' : 'bg-stone-100 text-stone-500'}`}>
                        {app.status}
                      </span>
                    </div>
                    <div className="font-mono text-[10px] text-stone-400 dark:text-zinc-500 mt-1">ID: {app.applicationId}</div>
                    <div className="font-mono text-[10px] text-zinc-600 dark:text-zinc-400 truncate mt-0.5">Key: {app.apiKey}</div>

                    <div className="mt-2.5 pt-2 border-t border-stone-100 dark:border-zinc-800/80 flex justify-between items-center text-[10px]">
                      <span className="text-stone-400">{app.rateLimitPerMinute} req/min</span>
                      <button
                        onClick={(e) => { e.stopPropagation(); handleRotateCredentials(app.applicationId); }}
                        disabled={isRotating}
                        className="text-amber-600 dark:text-amber-400 hover:underline font-medium cursor-pointer"
                      >
                        Rotate Keys
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Columns: Active Project Credentials & Live Sandbox */}
        <div className="lg:col-span-2 space-y-5">
          
          {selectedApp ? (
            <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-stone-200 dark:border-zinc-800 p-5 shadow-xs space-y-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 pb-3 border-b border-stone-100 dark:border-zinc-800">
                <div>
                  <h2 className="text-sm sm:text-base font-bold text-zinc-900 dark:text-zinc-100">
                    {selectedApp.applicationName}
                  </h2>
                  <p className="text-xs text-stone-500 dark:text-zinc-400 mt-0.5">
                    {selectedApp.description || 'Campus REST application integration'}
                  </p>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="font-mono text-xs bg-stone-100 dark:bg-zinc-800 px-2 py-0.5 rounded text-zinc-700 dark:text-zinc-300">
                    {selectedApp.applicationId}
                  </span>
                </div>
              </div>

              {/* Credentials Card */}
              <div className="space-y-3">
                <div>
                  <label className="text-[11px] font-medium text-stone-400 dark:text-zinc-500 uppercase">
                    X-API-KEY
                  </label>
                  <div className="flex items-center space-x-2 mt-1">
                    <input
                      readOnly
                      value={selectedApp.apiKey}
                      className="flex-1 font-mono text-xs p-2 rounded-lg bg-stone-50 dark:bg-zinc-800 border border-stone-200 dark:border-zinc-700 text-zinc-900 dark:text-zinc-100 select-all"
                    />
                    <button
                      onClick={() => copyToClipboard(selectedApp.apiKey, 'apiKey')}
                      className="p-2 rounded-lg border border-stone-200 dark:border-zinc-700 text-stone-600 dark:text-zinc-400 hover:bg-stone-50 dark:hover:bg-zinc-800 transition-colors"
                      title="Copy API Key"
                    >
                      {copiedField === 'apiKey' ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="text-[11px] font-medium text-stone-400 dark:text-zinc-500 uppercase">
                    X-API-SECRET
                  </label>
                  <div className="flex items-center space-x-2 mt-1">
                    <input
                      readOnly
                      value="••••••••••••••••••••••••••••••••••••••••"
                      className="flex-1 font-mono text-xs p-2 rounded-lg bg-stone-50 dark:bg-zinc-800 border border-stone-200 dark:border-zinc-700 text-stone-400 dark:text-zinc-500"
                    />
                    <button
                      onClick={() => handleRotateCredentials(selectedApp.applicationId)}
                      className="px-3 py-2 rounded-lg border border-stone-200 dark:border-zinc-700 text-xs font-medium text-stone-600 dark:text-zinc-300 hover:bg-stone-50 dark:hover:bg-zinc-800 transition-colors"
                    >
                      Generate New Secret
                    </button>
                  </div>
                  <p className="text-[10px] text-stone-400 dark:text-zinc-500 mt-1">
                    The raw API secret is shown once upon project creation or rotation, and stored securely as a one-way BCrypt hash.
                  </p>
                </div>
              </div>

              {/* Redirect URL Generator */}
              <div className="p-3.5 rounded-xl bg-stone-50 dark:bg-zinc-800/40 border border-stone-200 dark:border-zinc-800 space-y-1.5">
                <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                  Student Top-Up Redirect Link
                </span>
                <p className="text-[11px] text-stone-500 dark:text-zinc-400">
                  When a student needs to recharge tokens, redirect them from your project with their mobile number:
                </p>
                <div className="flex items-center space-x-2 mt-1">
                  <input
                    readOnly
                    value={`https://ritz.rit-services.in/?phone=9876543210&name=Student+Name`}
                    className="flex-1 font-mono text-[11px] p-2 rounded-lg bg-white dark:bg-zinc-900 border border-stone-200 dark:border-zinc-700 text-zinc-700 dark:text-zinc-300 select-all"
                  />
                  <button
                    onClick={() => copyToClipboard(`https://ritz.rit-services.in/?phone=9876543210&name=Student+Name`, 'redirectLink')}
                    className="p-2 rounded-lg border border-stone-200 dark:border-zinc-700 text-stone-600 dark:text-zinc-400 hover:bg-stone-100 dark:hover:bg-zinc-800"
                  >
                    {copiedField === 'redirectLink' ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Interactive Live Sandbox */}
              <div className="pt-2 border-t border-stone-100 dark:border-zinc-800 space-y-3">
                <div className="flex justify-between items-center">
                  <div className="flex items-center space-x-2">
                    <Terminal className="w-4 h-4 text-zinc-700 dark:text-zinc-300" />
                    <h3 className="font-bold text-xs text-zinc-900 dark:text-zinc-100 uppercase tracking-wider">Live Sandbox Runner</h3>
                  </div>

                  <div className="flex bg-stone-100 dark:bg-zinc-800 p-0.5 rounded-lg text-xs font-medium">
                    <button
                      onClick={() => setTestEndpoint('debit')}
                      className={`px-2.5 py-1 rounded-md text-xs transition-colors cursor-pointer ${testEndpoint === 'debit' ? 'bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-xs' : 'text-stone-500 hover:text-stone-900 dark:hover:text-zinc-100'}`}
                    >
                      POST Debit
                    </button>
                    <button
                      onClick={() => setTestEndpoint('balance')}
                      className={`px-2.5 py-1 rounded-md text-xs transition-colors cursor-pointer ${testEndpoint === 'balance' ? 'bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-xs' : 'text-stone-500 hover:text-stone-900 dark:hover:text-zinc-100'}`}
                    >
                      GET Balance
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 text-xs">
                  <div>
                    <label className="text-[10px] font-medium text-stone-400 dark:text-zinc-500">Student Phone or ID</label>
                    <input
                      type="text"
                      value={testStudentId}
                      onChange={(e) => setTestStudentId(e.target.value)}
                      placeholder="e.g. 9003627997"
                      className="w-full mt-1 p-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-stone-50 dark:bg-zinc-800 font-mono text-xs text-zinc-900 dark:text-zinc-100"
                    />
                  </div>

                  {testEndpoint === 'debit' && (
                    <div>
                      <label className="text-[10px] font-medium text-stone-400 dark:text-zinc-500">Amount (Tokens)</label>
                      <input
                        type="number"
                        value={testAmount}
                        onChange={(e) => setTestAmount(Number(e.target.value))}
                        className="w-full mt-1 p-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-stone-50 dark:bg-zinc-800 text-xs font-bold text-zinc-900 dark:text-zinc-100"
                      />
                    </div>
                  )}

                  <div>
                    <label className="text-[10px] font-medium text-stone-400 dark:text-zinc-500">Order Reference</label>
                    <input
                      type="text"
                      value={testRefId}
                      onChange={(e) => setTestRefId(e.target.value)}
                      className="w-full mt-1 p-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-stone-50 dark:bg-zinc-800 font-mono text-xs text-zinc-900 dark:text-zinc-100"
                    />
                  </div>
                </div>

                <div className="flex justify-between items-center pt-1">
                  <span className="font-mono text-[10px] text-stone-400">
                    {testEndpoint === 'debit' ? 'POST /api/v1/transactions/debit' : `/api/v1/students/${testStudentId}/balance`}
                  </span>
                  <button
                    onClick={handleRunSandbox}
                    disabled={sandboxLoading}
                    className="px-3.5 py-1.5 bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 rounded-lg text-xs font-semibold flex items-center space-x-1 shadow-xs disabled:opacity-50 transition-colors cursor-pointer"
                  >
                    <Play className="w-3 h-3 fill-current" />
                    <span>{sandboxLoading ? 'Executing...' : 'Run Test'}</span>
                  </button>
                </div>

                {sandboxResponse && (
                  <pre className="p-3 rounded-xl bg-zinc-950 text-emerald-400 font-mono text-[11px] overflow-x-auto border border-zinc-800 max-h-48">
                    {JSON.stringify(sandboxResponse, null, 2)}
                  </pre>
                )}
              </div>

            </div>
          ) : (
            <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-stone-200 dark:border-zinc-800 p-8 text-center text-xs text-stone-400">
              Select or register an application to inspect its API credentials and test requests.
            </div>
          )}

          {/* Integration SDK Code Examples */}
          <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-stone-200 dark:border-zinc-800 p-5 shadow-xs space-y-3">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
              <div>
                <h3 className="font-bold text-xs uppercase tracking-wider text-stone-500 dark:text-zinc-400">Integration Examples</h3>
                <p className="text-xs text-stone-500 dark:text-zinc-400">Ready-to-use code for {activeAppId}</p>
              </div>

              <div className="flex items-center space-x-2">
                <div className="flex bg-stone-100 dark:bg-zinc-800 p-0.5 rounded-lg text-xs font-medium">
                  {(['javascript', 'python', 'java'] as const).map((lang) => (
                    <button
                      key={lang}
                      onClick={() => setSelectedLanguage(lang)}
                      className={`px-2.5 py-1 rounded-md text-xs transition-colors cursor-pointer ${
                        selectedLanguage === lang
                          ? 'bg-white dark:bg-zinc-700 text-zinc-900 dark:text-zinc-100 shadow-xs'
                          : 'text-stone-500 hover:text-stone-900 dark:hover:text-zinc-100'
                      }`}
                    >
                      {lang === 'javascript' ? 'JavaScript' : lang.charAt(0).toUpperCase() + lang.slice(1)}
                    </button>
                  ))}
                </div>

                <button
                  onClick={() => copyCodeSnippet(codeSnippets[selectedLanguage])}
                  className="p-1.5 rounded-lg border border-stone-200 dark:border-zinc-700 text-stone-600 dark:text-zinc-400 hover:bg-stone-100 dark:hover:bg-zinc-800 transition-colors"
                  title="Copy Snippet"
                >
                  {copiedSnippet ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <pre className="p-3.5 rounded-xl bg-zinc-950 text-zinc-200 font-mono text-[11px] overflow-x-auto border border-zinc-800 leading-relaxed">
              {codeSnippets[selectedLanguage]}
            </pre>
          </div>

        </div>

      </div>

      {/* Project Registration Modal */}
      {showRegModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white dark:bg-zinc-900 rounded-2xl max-w-md w-full p-6 shadow-xl border border-stone-200 dark:border-zinc-800 animate-in fade-in zoom-in-95 duration-150">
            <h3 className="font-bold text-base text-zinc-900 dark:text-zinc-100">Register New Campus Project</h3>
            <p className="text-xs text-stone-500 dark:text-zinc-400 mt-1">
              Creates a dedicated project identifier and provisions production API credentials.
            </p>

            {!regResult ? (
              <form onSubmit={handleRegisterApp} className="mt-4 space-y-3.5">
                <div>
                  <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">
                    Application ID (Uppercase identifier, e.g. TILLO, SMARTPRINT, LIBRARY)
                  </label>
                  <input
                    required
                    type="text"
                    value={newAppId}
                    onChange={(e) => setNewAppId(e.target.value.toUpperCase())}
                    placeholder="e.g. EVENT_MANAGEMENT"
                    className="w-full mt-1 p-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs font-mono font-bold text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-900 dark:focus:ring-zinc-100"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">Application Name</label>
                  <input
                    required
                    type="text"
                    value={newAppName}
                    onChange={(e) => setNewAppName(e.target.value)}
                    placeholder="e.g. Campus Event Ticket Booking"
                    className="w-full mt-1 p-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-900 dark:focus:ring-zinc-100"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">Category / Merchant Type</label>
                  <select
                    value={newAppCategory}
                    onChange={(e) => setNewAppCategory(e.target.value)}
                    className="w-full mt-1 p-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs text-zinc-900 dark:text-zinc-100 focus:outline-none focus:ring-1 focus:ring-zinc-900"
                  >
                    <option value="CAMPUS_SERVICES">Campus Services / General</option>
                    <option value="FOOD">Food & Canteen</option>
                    <option value="STATIONERY">Stationery & Printing</option>
                    <option value="TRANSPORT">Transport & Bus</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs font-medium text-zinc-700 dark:text-zinc-300">Description (Optional)</label>
                  <input
                    type="text"
                    value={newAppDesc}
                    onChange={(e) => setNewAppDesc(e.target.value)}
                    placeholder="e.g. Deducts festival ticket tokens from student wallet"
                    className="w-full mt-1 p-2 rounded-lg border border-stone-200 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-xs text-zinc-900 dark:text-zinc-100"
                  />
                </div>

                {regError && (
                  <div className="p-2.5 rounded-lg bg-red-50 text-red-600 text-xs">
                    {regError}
                  </div>
                )}

                <div className="flex space-x-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowRegModal(false)}
                    className="flex-1 py-2 rounded-lg border border-stone-200 dark:border-zinc-700 text-xs font-medium text-stone-600 dark:text-zinc-300 hover:bg-stone-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmittingApp}
                    className="flex-1 py-2 rounded-lg bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 text-xs font-semibold disabled:opacity-50"
                  >
                    {isSubmittingApp ? 'Generating...' : 'Generate Credentials'}
                  </button>
                </div>
              </form>
            ) : (
              <div className="mt-4 space-y-3.5">
                <div className="p-3 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900/60 rounded-xl text-xs text-amber-800 dark:text-amber-300">
                  <div className="font-bold flex items-center space-x-1.5">
                    <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                    <span>Important: Save Your API Secret Now</span>
                  </div>
                  <p className="mt-1 text-[11px] leading-relaxed">
                    The raw API secret is shown only once and cannot be retrieved later. It has been hashed in the PostgreSQL database.
                  </p>
                </div>

                <div className="space-y-2 text-xs">
                  <div>
                    <span className="text-stone-500 font-medium text-[11px]">Application ID:</span>
                    <pre className="mt-0.5 p-2 bg-stone-100 dark:bg-zinc-800 rounded font-mono text-xs text-zinc-900 dark:text-zinc-100 select-all">
                      {regResult.application_id}
                    </pre>
                  </div>
                  <div>
                    <span className="text-stone-500 font-medium text-[11px]">X-API-KEY:</span>
                    <pre className="mt-0.5 p-2 bg-stone-100 dark:bg-zinc-800 rounded font-mono text-xs text-zinc-900 dark:text-zinc-100 select-all">
                      {regResult.api_key}
                    </pre>
                  </div>
                  <div>
                    <span className="text-stone-500 font-medium text-[11px]">X-API-SECRET (Copy Now):</span>
                    <pre className="mt-0.5 p-2 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded font-mono text-xs text-emerald-700 dark:text-emerald-400 select-all font-bold">
                      {regResult.api_secret}
                    </pre>
                  </div>
                </div>

                <button
                  onClick={() => { setShowRegModal(false); setRegResult(null); }}
                  className="w-full py-2 bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 rounded-lg text-xs font-semibold mt-2 cursor-pointer"
                >
                  I Have Safely Saved My Credentials
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Rotate Key Confirmation / Result Modal */}
      {showRotateModal && rotationResult && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white dark:bg-zinc-900 rounded-2xl max-w-md w-full p-6 shadow-xl border border-stone-200 dark:border-zinc-800 animate-in fade-in zoom-in-95 duration-150">
            <h3 className="font-bold text-base text-zinc-900 dark:text-zinc-100">Credentials Rotated</h3>
            <p className="text-xs text-stone-500 dark:text-zinc-400 mt-1">
              Previous credentials for <span className="font-mono font-bold text-zinc-900 dark:text-zinc-100">{rotationResult.application_id}</span> have been invalidated.
            </p>

            <div className="mt-4 space-y-2.5 text-xs">
              <div>
                <span className="text-stone-500 font-medium text-[11px]">New X-API-KEY:</span>
                <pre className="mt-0.5 p-2 bg-stone-100 dark:bg-zinc-800 rounded font-mono text-xs text-zinc-900 dark:text-zinc-100 select-all">
                  {rotationResult.api_key}
                </pre>
              </div>
              <div>
                <span className="text-stone-500 font-medium text-[11px]">New X-API-SECRET (Save Now):</span>
                <pre className="mt-0.5 p-2 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded font-mono text-xs text-emerald-700 dark:text-emerald-400 select-all font-bold">
                  {rotationResult.api_secret}
                </pre>
              </div>
            </div>

            <button
              onClick={() => setShowRotateModal(false)}
              className="mt-5 w-full py-2 bg-zinc-900 hover:bg-zinc-800 dark:bg-zinc-100 dark:hover:bg-white text-white dark:text-zinc-900 rounded-lg text-xs font-semibold cursor-pointer"
            >
              Done
            </button>
          </div>
        </div>
      )}

    </div>
  );
};
