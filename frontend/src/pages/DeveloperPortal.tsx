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
  ShieldAlert,
  Layers,
  CheckCircle2,
  AlertTriangle,
  RotateCcw
} from 'lucide-react';

export const DeveloperPortal: React.FC = () => {
  const [applications, setApplications] = useState<RegisteredApplication[]>([]);
  const [selectedApp, setSelectedApp] = useState<RegisteredApplication | null>(null);
  const [selectedLanguage, setSelectedLanguage] = useState<'javascript' | 'python' | 'java'>('javascript');
  const [copied, setCopied] = useState<boolean>(false);
  
  // Registration Form State
  const [showRegModal, setShowRegModal] = useState<boolean>(false);
  const [newAppId, setNewAppId] = useState<string>('');
  const [newAppName, setNewAppName] = useState<string>('');
  const [newAppDesc, setNewAppDesc] = useState<string>('');
  const [regResult, setRegResult] = useState<any>(null);

  // Live Sandbox Runner State
  const [testStudentId, setTestStudentId] = useState<string>('RIT2026001');
  const [testAmount, setTestAmount] = useState<number>(80);
  const [testRefId, setTestRefId] = useState<string>('TILLO-ORDER-' + Math.floor(Math.random() * 9000 + 1000));
  const [testEndpoint, setTestEndpoint] = useState<'debit' | 'validate' | 'balance'>('debit');
  const [sandboxApiKey, setSandboxApiKey] = useState<string>('ritz-secret-api-key-2026');
  const [sandboxResponse, setSandboxResponse] = useState<any>(null);
  const [sandboxLoading, setSandboxLoading] = useState<boolean>(false);

  const loadApps = async () => {
    try {
      const apps = await api.getApplications();
      setApplications(apps || []);
      if (apps && apps.length > 0 && !selectedApp) {
        setSelectedApp(apps[0]);
        setSandboxApiKey(apps[0].apiKey || 'ritz-secret-api-key-2026');
      }
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadApps();
  }, []);

  const handleRegisterApp = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await api.registerApplication({
        application_id: newAppId,
        application_name: newAppName,
        description: newAppDesc,
        scopes: ['TRANSACTION_DEBIT', 'TRANSACTION_CREDIT', 'TRANSACTION_REFUND', 'TRANSACTION_VALIDATE', 'BALANCE_READ']
      });
      setRegResult(res);
      loadApps();
    } catch (e: any) {
      alert(`Registration error: ${e.message}`);
    }
  };

  const handleRotateKeys = async (appId: string) => {
    if (!confirm('Rotating credentials will immediately invalidate the existing API secret. Proceed?')) return;
    try {
      const res = await api.rotateCredentials(appId);
      setRegResult(res);
      loadApps();
    } catch (e: any) {
      alert(`Rotation error: ${e.message}`);
    }
  };

  const handleRunSandbox = async () => {
    setSandboxLoading(true);
    setSandboxResponse(null);
    try {
      let res;
      if (testEndpoint === 'debit') {
        res = await api.testDebit(
          {
            student_id: testStudentId,
            amount: testAmount,
            currency: 'RITZ',
            reference_id: testRefId,
            merchant_id: 'TILLO',
            description: 'Sandbox canteen test transaction'
          },
          sandboxApiKey,
          testRefId
        );
      } else if (testEndpoint === 'validate') {
        res = await api.testValidateBalance(testStudentId, testAmount, sandboxApiKey);
      } else {
        res = await api.testStudentBalanceQuery(testStudentId, sandboxApiKey);
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

  const copySnippet = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const codeSnippets = {
    javascript: `// Node.js / JavaScript Tillo Integration Example
import axios from 'axios';

const RITZ_BASE_URL = 'http://localhost:8085';
const API_KEY = '${selectedApp ? selectedApp.apiKey : 'rit_live_key_...' }';

async function debitStudentWallet(studentId, amount, orderRef) {
  try {
    const response = await axios.post(
      \`\${RITZ_BASE_URL}/api/v1/transactions/debit\`,
      {
        student_id: studentId,
        amount: amount,
        currency: 'RITZ',
        reference_id: orderRef,
        merchant_id: 'TILLO',
        description: 'Canteen food purchase'
      },
      {
        headers: {
          'X-API-KEY': API_KEY,
          'Idempotency-Key': orderRef,
          'Content-Type': 'application/json'
        }
      }
    );
    console.log('Debit Success:', response.data);
    return response.data;
  } catch (err) {
    console.error('Debit Failed:', err.response?.data || err.message);
    throw err;
  }
}

// Call debit for student RIT2026001 for 80 tokens
debitStudentWallet('RIT2026001', 80, 'TILLO-ORD-1024');`,

    python: `# Python Tillo Integration Example
import requests

RITZ_BASE_URL = "http://localhost:8085"
API_KEY = "${selectedApp ? selectedApp.apiKey : 'rit_live_key_...'}"

def debit_student_wallet(student_id: str, amount: int, order_ref: str):
    url = f"{RITZ_BASE_URL}/api/v1/transactions/debit"
    headers = {
        "X-API-KEY": API_KEY,
        "Idempotency-Key": order_ref,
        "Content-Type": "application/json"
    }
    payload = {
        "student_id": student_id,
        "amount": amount,
        "currency": "RITZ",
        "reference_id": order_ref,
        "merchant_id": "TILLO",
        "description": "Canteen food purchase"
    }
    
    response = requests.post(url, json=payload, headers=headers)
    response.raise_for_status()
    return response.json()

if __name__ == "__main__":
    result = debit_student_wallet("RIT2026001", 80, "TILLO-ORD-1024")
    print("Debit Result:", result)`,

    java: `// Java 17+ HTTP Client Tillo Integration Example
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RitzClient {
    private static final String BASE_URL = "http://localhost:8085";
    private static final String API_KEY = "${selectedApp ? selectedApp.apiKey : 'rit_live_key_...'}";

    public static void main(String[] args) throws Exception {
        String jsonPayload = """
            {
              "student_id": "RIT2026001",
              "amount": 80,
              "currency": "RITZ",
              "reference_id": "TILLO-ORD-1024",
              "merchant_id": "TILLO",
              "description": "Canteen purchase"
            }
            """;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/transactions/debit"))
                .header("X-API-KEY", API_KEY)
                .header("Idempotency-Key", "TILLO-ORD-1024")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}`
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-6 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm gap-4">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-2xl bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20 flex items-center justify-center">
            <Code2 className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-extrabold text-slate-900 dark:text-white">Developer Integration Portal</h1>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Standardized REST APIs and secure credentials for campus application integrations
            </p>
          </div>
        </div>

        <button
          onClick={() => { setShowRegModal(true); setRegResult(null); }}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl text-xs flex items-center space-x-2 shadow-md transition-all"
        >
          <Key className="w-4 h-4" />
          <span>Register Application</span>
        </button>
      </div>

      {/* Main Grid: Applications & Interactive Sandbox */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Applications List */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="font-extrabold text-sm text-slate-900 dark:text-white">Registered Applications</h3>
            <span className="text-xs font-bold text-slate-400">{applications.length} Apps</span>
          </div>

          <div className="space-y-2.5">
            {applications.map((app) => (
              <div
                key={app.id}
                onClick={() => { setSelectedApp(app); setSandboxApiKey(app.apiKey); }}
                className={`p-3.5 rounded-2xl border cursor-pointer transition-all ${selectedApp?.id === app.id ? 'border-blue-600 bg-blue-50/50 dark:bg-blue-950/30' : 'border-slate-200 dark:border-slate-700 hover:border-slate-300'}`}
              >
                <div className="flex justify-between items-start">
                  <span className="font-extrabold text-xs text-slate-900 dark:text-white">{app.applicationName}</span>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                    {app.status}
                  </span>
                </div>
                <div className="font-mono text-[10px] text-slate-400 mt-1">ID: {app.applicationId}</div>
                <div className="font-mono text-[10px] text-blue-600 dark:text-blue-400 mt-1 truncate">Key: {app.apiKey}</div>

                <div className="mt-3 pt-2 border-t border-slate-100 dark:border-slate-800 flex justify-between items-center text-[10px]">
                  <span className="text-slate-400">Rate Limit: {app.rateLimitPerMinute}/min</span>
                  <button
                    onClick={(e) => { e.stopPropagation(); handleRotateKeys(app.applicationId); }}
                    className="font-bold text-amber-600 hover:underline"
                  >
                    Rotate Keys
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Live Interactive Sandbox Console */}
        <div className="lg:col-span-2 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm space-y-4">
          <div className="flex justify-between items-center border-b border-slate-100 dark:border-slate-800 pb-3">
            <div>
              <div className="flex items-center space-x-2">
                <Terminal className="w-4 h-4 text-blue-600" />
                <h3 className="font-extrabold text-sm text-slate-900 dark:text-white">Interactive API Sandbox</h3>
              </div>
              <p className="text-[11px] text-slate-500">Test live debit, validation and balance queries against the real backend</p>
            </div>

            {/* Endpoint Selector */}
            <div className="flex bg-slate-100 dark:bg-slate-800 p-1 rounded-xl text-xs font-semibold">
              <button
                onClick={() => setTestEndpoint('debit')}
                className={`px-3 py-1 rounded-lg ${testEndpoint === 'debit' ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs' : 'text-slate-500'}`}
              >
                Debit
              </button>
              <button
                onClick={() => setTestEndpoint('validate')}
                className={`px-3 py-1 rounded-lg ${testEndpoint === 'validate' ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs' : 'text-slate-500'}`}
              >
                Validate
              </button>
              <button
                onClick={() => setTestEndpoint('balance')}
                className={`px-3 py-1 rounded-lg ${testEndpoint === 'balance' ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs' : 'text-slate-500'}`}
              >
                Balance
              </button>
            </div>
          </div>

          {/* Sandbox Request Inputs */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
            <div>
              <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400">Student ID</label>
              <input
                type="text"
                value={testStudentId}
                onChange={(e) => setTestStudentId(e.target.value)}
                className="w-full mt-1 p-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 font-mono font-bold"
              />
            </div>

            {testEndpoint !== 'balance' && (
              <div>
                <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400">Amount (Tokens)</label>
                <input
                  type="number"
                  value={testAmount}
                  onChange={(e) => setTestAmount(Number(e.target.value))}
                  className="w-full mt-1 p-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 font-bold"
                />
              </div>
            )}

            <div>
              <label className="text-[11px] font-bold text-slate-600 dark:text-slate-400">API Key Used</label>
              <input
                type="text"
                value={sandboxApiKey}
                onChange={(e) => setSandboxApiKey(e.target.value)}
                className="w-full mt-1 p-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 font-mono text-[10px]"
              />
            </div>
          </div>

          <div className="flex justify-between items-center pt-2">
            <span className="font-mono text-[11px] text-slate-500">
              {testEndpoint === 'debit' && 'POST /api/v1/transactions/debit'}
              {testEndpoint === 'validate' && 'POST /api/v1/transactions/validate'}
              {testEndpoint === 'balance' && `GET /api/v1/students/${testStudentId}/balance`}
            </span>
            <button
              onClick={handleRunSandbox}
              disabled={sandboxLoading}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm disabled:opacity-50 transition-all"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>{sandboxLoading ? 'Executing...' : 'Send Live Request'}</span>
            </button>
          </div>

          {/* Sandbox Response Output */}
          <div className="mt-4">
            <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">Live Response Payload</div>
            <pre className="p-4 rounded-2xl bg-slate-950 text-emerald-400 font-mono text-xs overflow-x-auto border border-slate-800 min-h-[140px]">
              {sandboxResponse ? JSON.stringify(sandboxResponse, null, 2) : '// Click "Send Live Request" to test API execution'}
            </pre>
          </div>

        </div>

      </div>

      {/* Code Examples Section */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          <div>
            <h3 className="font-extrabold text-sm text-slate-900 dark:text-white">Integration Code Examples</h3>
            <p className="text-xs text-slate-500">Working implementations for Node.js, Python, and Java</p>
          </div>

          <div className="flex items-center space-x-2">
            <div className="flex bg-slate-100 dark:bg-slate-800 p-1 rounded-xl text-xs font-semibold">
              {(['javascript', 'python', 'java'] as const).map((lang) => (
                <button
                  key={lang}
                  onClick={() => setSelectedLanguage(lang)}
                  className={`px-3 py-1 rounded-lg capitalize transition-all ${selectedLanguage === lang ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-xs' : 'text-slate-500'}`}
                >
                  {lang === 'javascript' ? 'JavaScript / Node' : lang}
                </button>
              ))}
            </div>

            <button
              onClick={() => copySnippet(codeSnippets[selectedLanguage])}
              className="p-2 rounded-xl text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              title="Copy Code"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
        </div>

        <pre className="p-4 rounded-2xl bg-slate-950 text-slate-100 font-mono text-xs overflow-x-auto border border-slate-800 leading-relaxed">
          {codeSnippets[selectedLanguage]}
        </pre>
      </div>

      {/* App Registration Modal */}
      {showRegModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 dark:border-slate-800">
            <h3 className="font-bold text-base text-slate-900 dark:text-white">Register Campus Application</h3>
            <p className="text-xs text-slate-500 mt-1">Generates an API Key and Hashed Secret for REST integration</p>

            {!regResult ? (
              <form onSubmit={handleRegisterApp} className="mt-4 space-y-3">
                <div>
                  <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">Application ID (Uppercase, e.g. TILLO, CAMPUS_ERP)</label>
                  <input
                    required
                    type="text"
                    value={newAppId}
                    onChange={(e) => setNewAppId(e.target.value.toUpperCase())}
                    placeholder="e.g. LIBRARY_PORTAL"
                    className="w-full mt-1 p-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-mono font-bold"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">Application Name</label>
                  <input
                    required
                    type="text"
                    value={newAppName}
                    onChange={(e) => setNewAppName(e.target.value)}
                    placeholder="e.g. Campus Library Printing Service"
                    className="w-full mt-1 p-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-bold"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">Description</label>
                  <input
                    type="text"
                    value={newAppDesc}
                    onChange={(e) => setNewAppDesc(e.target.value)}
                    placeholder="e.g. Deducts print credits from student wallet"
                    className="w-full mt-1 p-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs"
                  />
                </div>

                <div className="flex space-x-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowRegModal(false)}
                    className="flex-1 py-2 rounded-xl text-xs font-semibold bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="flex-1 py-2 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white"
                  >
                    Generate Credentials
                  </button>
                </div>
              </form>
            ) : (
              <div className="mt-4 space-y-3">
                <div className="p-3 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 rounded-xl text-xs text-amber-800 dark:text-amber-300">
                  <div className="font-bold">Important: Save Your API Secret Now</div>
                  <p className="mt-0.5 text-[11px]">The secret is hashed in the database with BCrypt and will never be shown again.</p>
                </div>

                <div className="space-y-2 text-xs">
                  <div>
                    <span className="text-slate-500 font-semibold">API Key:</span>
                    <pre className="mt-1 p-2 bg-slate-100 dark:bg-slate-800 rounded font-mono text-[11px] text-slate-900 dark:text-white select-all">
                      {regResult.api_key}
                    </pre>
                  </div>
                  <div>
                    <span className="text-slate-500 font-semibold">API Secret:</span>
                    <pre className="mt-1 p-2 bg-slate-100 dark:bg-slate-800 rounded font-mono text-[11px] text-emerald-600 dark:text-emerald-400 select-all">
                      {regResult.api_secret}
                    </pre>
                  </div>
                </div>

                <button
                  onClick={() => setShowRegModal(false)}
                  className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold mt-2"
                >
                  I Have Copied The Secret
                </button>
              </div>
            )}
          </div>
        </div>
      )}

    </div>
  );
};
