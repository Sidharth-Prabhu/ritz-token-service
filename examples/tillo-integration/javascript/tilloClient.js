/**
 * RITZ Wallet Client SDK for Tillo Point-of-Sale (Node.js)
 * Rajalakshmi Institute of Technology (RIT)
 */

class TilloWalletClient {
  constructor(baseUrl, apiKey) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
    this.apiKey = apiKey;
  }

  async getStudentBalance(studentId) {
    const res = await fetch(`${this.baseUrl}/api/v1/students/${studentId}/balance`, {
      method: 'GET',
      headers: {
        'X-API-KEY': this.apiKey,
        'Content-Type': 'application/json'
      }
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `Failed to fetch balance: HTTP ${res.status}`);
    }

    return await res.json();
  }

  async validateBalance(studentId, amount) {
    const res = await fetch(`${this.baseUrl}/api/v1/transactions/validate`, {
      method: 'POST',
      headers: {
        'X-API-KEY': this.apiKey,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ student_id: studentId, amount })
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `Validation failed: HTTP ${res.status}`);
    }

    return await res.json();
  }

  async debit(studentId, amount, orderRef, description = 'Canteen food purchase') {
    const res = await fetch(`${this.baseUrl}/api/v1/transactions/debit`, {
      method: 'POST',
      headers: {
        'X-API-KEY': this.apiKey,
        'Idempotency-Key': orderRef,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        student_id: studentId,
        amount: amount,
        currency: 'RITZ',
        reference_id: orderRef,
        merchant_id: 'TILLO',
        description: description
      })
    });

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(data.error || `Debit failed: HTTP ${res.status}`);
    }

    return data;
  }
}

module.exports = { TilloWalletClient };
