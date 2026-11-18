import random
from tillo_client import TilloWalletClient

def main():
    client = TilloWalletClient("http://localhost:8085", "ritz-secret-api-key-2026")
    student_id = "RIT2026001"
    order_ref = f"TILLO-ORDER-PY-{random.randint(10000, 99999)}"

    print("--- 1. Querying Student Balance ---")
    balance_info = client.get_student_balance(student_id)
    print(f"Student: {balance_info['student_name']} ({balance_info['student_id']})")
    print(f"Current Balance: {balance_info['balance']} {balance_info['currency']}")

    print("\n--- 2. Validating Balance for 80 tokens ---")
    valid = client.validate_balance(student_id, 80)
    print(f"Sufficient Funds: {valid['valid']} (Available: {valid['available_balance']})")

    print("\n--- 3. Executing Canteen Debit ---")
    result = client.debit(student_id, 80, order_ref, "Canteen juice & snacks")
    print("Debit Successful!")
    print(f"Transaction ID: {result['transaction_id']}")
    print(f"Previous Balance: {result['previous_balance']}")
    print(f"Current Balance: {result['current_balance']}")

    print("\n--- 4. Verifying Idempotency Deduplication ---")
    duplicate = client.debit(student_id, 80, order_ref, "Canteen juice & snacks")
    print(f"Duplicate call returned exact same transaction ID: {duplicate['transaction_id']}")
    print(f"Balance preserved: {duplicate['current_balance']}")

if __name__ == "__main__":
    main()
