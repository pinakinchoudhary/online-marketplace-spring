import sys
import requests
import random
from threading import Thread

from user import post_user
from wallet import put_wallet, get_wallet, test_get_wallet
from utils import check_response_status_code, print_fail_message, print_pass_message

WALLET_SERVICE_URL = "http://localhost:8082"

# Global counters
credited_amount = 0
debited_amount = 0

def credit_thread_function(user_id, iterations=50):
    """
    Each thread calls credit random(10..100).
    If response=200, we increment credited_amount.
    """
    global credited_amount
    for _ in range(iterations):
        amount = random.randint(10, 100)
        resp = requests.put(f"{WALLET_SERVICE_URL}/wallets/{user_id}",
                            json={"action": "credit", "amount": amount})
        if resp.status_code == 200:
            credited_amount += amount

def debit_thread_function(user_id, iterations=50):
    """
    Each thread calls debit random(5..50).
    If response=200, we increment debited_amount.
    """
    global debited_amount
    for _ in range(iterations):
        amount = random.randint(5, 50)
        resp = requests.put(f"{WALLET_SERVICE_URL}/wallets/{user_id}",
                            json={"action": "debit", "amount": amount})
        if resp.status_code == 200:
            debited_amount += amount

def main():
    try:
        # Create a user
        user_id = 1001
        resp = post_user(user_id, "Alice Concurrency", "alice@concurrency.com")
        if not check_response_status_code(resp, 201):
            return False

        # Give initial wallet balance
        initial_balance = 1000
        resp = put_wallet(user_id, "credit", initial_balance)
        if not check_response_status_code(resp, 200):
            return False

        # Spawn concurrency threads
        thread1 = Thread(target=credit_thread_function, kwargs={"user_id": user_id, "iterations": 100})
        thread2 = Thread(target=debit_thread_function, kwargs={"user_id": user_id, "iterations": 100})

        thread1.start()
        thread2.start()

        thread1.join()
        thread2.join()

        # Final check of the wallet
        final_expected_balance = initial_balance + credited_amount - debited_amount
        resp = get_wallet(user_id)
        if not test_get_wallet(user_id, resp, final_expected_balance):
            return False

        print_pass_message("Wallet concurrency test passed.")
        return True

    except Exception as e:
        print_fail_message(f"Test crashed with exception: {e}")
        return False

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)
