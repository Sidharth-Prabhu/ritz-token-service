"""
RITZ Wallet Client SDK for Tillo Point-of-Sale (Python)
Rajalakshmi Institute of Technology (RIT)
"""
import requests
from typing import Dict, Any, Optional

class TilloWalletClient:
    def __init__(self, base_url: str, api_key: str):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.session = requests.Session()
        self.session.headers.update({
            "X-API-KEY": self.api_key,
            "Content-Type": "application/json"
        })

    def get_student_balance(self, student_id: str) -> Dict[str, Any]:
        url = f"{self.base_url}/api/v1/students/{student_id}/balance"
        resp = self.session.get(url)
        resp.raise_for_status()
        return resp.json()

    def validate_balance(self, student_id: str, amount: int) -> Dict[str, Any]:
        url = f"{self.base_url}/api/v1/transactions/validate"
        resp = self.session.post(url, json={"student_id": student_id, "amount": amount})
        resp.raise_for_status()
        return resp.json()

    def debit(self, student_id: str, amount: int, order_ref: str, description: str = "Canteen food purchase") -> Dict[str, Any]:
        url = f"{self.base_url}/api/v1/transactions/debit"
        headers = {"Idempotency-Key": order_ref}
        payload = {
            "student_id": student_id,
            "amount": amount,
            "currency": "RITZ",
            "reference_id": order_ref,
            "merchant_id": "TILLO",
            "description": description
        }
        resp = self.session.post(url, json=payload, headers=headers)
        resp.raise_for_status()
        return resp.json()
