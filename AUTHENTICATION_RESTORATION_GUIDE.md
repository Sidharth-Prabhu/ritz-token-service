# Authentication Restoration Guide for RITZ Wallet

This guide outlines exactly how the authentication system was temporarily relaxed for Razorpay compliance and onboarding verification, and provides exact, copy-paste steps to restore strict mobile number/JWT authentication once verification is complete.

---

## 1. What Was Temporarily Bypassed & Why

During Razorpay onboarding and website compliance checks, Razorpay review specialists require direct access to the student wallet and checkout experience without being blocked by campus-specific credentials, SMS OTPs, or student ID lookups.

To facilitate this:
1. **Frontend Landing (`frontend/src/context/AuthContext.tsx`)**:
   - Instead of forcing unauthenticated visitors to `LoginPage`, it automatically loads the demo student profile (`RIT2026001` - Aditya Sharma) and renders `StudentDashboard`.
2. **API Endpoints (`SecurityConfig.java` & `WalletController.java`)**:
   - Added public endpoints:
     - `GET /api/v1/wallet/public` (fetches wallet balance directly).
     - `GET /api/v1/wallet/transactions/public` (fetches transactions directly).
     - `POST /api/v1/wallet/recharge/public` (initiates recharge order directly).
     - `GET /api/v1/payments/config` (exposes payment gateway configuration and public key).
     - `POST /api/v1/payments/verify` (verifies Razorpay signature and completes recharge).

---

## 2. Step-by-Step Restoration Instructions

Follow these steps when you are ready to bring back enforced mobile number & JWT authentication.

### Step 2.1: Restore Frontend Authentication Guard

Open [frontend/src/context/AuthContext.tsx](file:///home/ritz/ritz-token-service/frontend/src/context/AuthContext.tsx).

Locate lines 92–104:
```typescript
    if (token) {
      fetchProfile();
    } else {
      // Razorpay Verification Mode: Default to demo student without requiring manual login
      setUser({
        id: 1,
        username: 'RIT2026001',
        fullName: 'Aravind Swaminathan',
        email: 'aravind.s@student.rit.edu.in',
        role: 'STUDENT',
        studentId: 'RIT2026001'
      });
      setIsLoading(false);
    }
```

Change it back to standard behavior (forcing login if no token):
```typescript
    if (token) {
      fetchProfile();
    } else {
      setIsLoading(false);
    }
```

In [frontend/src/services/api.ts](file:///home/ritz/ritz-token-service/frontend/src/services/api.ts), switch authenticated endpoints:
```typescript
  // Student Wallet
  getWallet: () => request<any>('/api/v1/wallet'),
  getWalletTransactions: (page = 0, size = 20) => request<any>(`/api/v1/wallet/transactions?page=${page}&size=${size}`),
  initiateRecharge: (amount: number) =>
    request<any>('/api/v1/wallet/recharge', { method: 'POST', body: JSON.stringify({ amount }) }),
```

Rebuild the frontend bundle and copy to static resources:
```bash
cd /home/ritz/ritz-token-service/frontend
npm run build
rm -rf ../src/main/resources/static/*
cp -r dist/* ../src/main/resources/static/
cd /home/ritz/ritz-token-service
```

---

### Step 2.2: Re-enforce Security Filter Rules

Open [src/main/java/com/rit/token/ritz/config/SecurityConfig.java](file:///home/ritz/ritz-token-service/src/main/java/com/rit/token/ritz/config/SecurityConfig.java).

Locate the `authorizeHttpRequests` block:
```java
    // Public authentication and payment callback endpoints
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/v1/payments/**").permitAll()
    .requestMatchers("/api/v1/wallet/recharge/public").permitAll()
    .requestMatchers("/api/v1/wallet/public").permitAll()
    .requestMatchers("/api/v1/wallet/transactions/public").permitAll()
```

Remove or comment out the public wallet endpoints so they require full JWT authentication:
```java
    // Public authentication and payment callback endpoints
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/v1/payments/webhook").permitAll()
    // Notice: /api/v1/wallet/** is guarded under .authenticated() below
```

---

### Step 2.3: Rebuild and Restart Backend

Run Maven package and restart the service:
```bash
cd /home/ritz/ritz-token-service
./mvnw clean package -DskipTests
sudo systemctl restart ritz-token-service
```

Verify service status:
```bash
systemctl status ritz-token-service
curl -s http://localhost:8085/actuator/health
```

---

## 3. Configuring Live Razorpay Credentials

When your Razorpay Merchant account is approved and ready for live or test transactions:

1. Edit your environment file or `application.properties`:
```properties
# Razorpay Configuration
payment.razorpay.enabled=true
payment.razorpay.key-id=rzp_live_xxxxxxxxxxxxxx
payment.razorpay.key-secret=xxxxxxxxxxxxxxxxxxxxxxxx
payment.razorpay.webhook-secret=xxxxxxxxxxxxxxxxxxxxxxxx
```
*(Or pass them as environment variables: `PAYMENT_RAZORPAY_ENABLED=true`, `PAYMENT_RAZORPAY_KEY_ID=...`, `PAYMENT_RAZORPAY_KEY_SECRET=...`)*

2. Restart the service:
```bash
sudo systemctl restart ritz-token-service
```

When `payment.razorpay.enabled=true`, the frontend dynamically switches from the simulated sandbox modal to the official Razorpay Checkout popup using the configured `key_id`, verifies payment signatures against `payment.razorpay.key-secret`, and executes the double-entry credit ledger update seamlessly.
