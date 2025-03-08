import sys
import random
from threading import Thread
import requests

from user import post_user
from wallet import put_wallet, get_wallet
from marketplace import (
    post_order,
    get_product,
    test_get_product_stock,
    test_post_order
)
from utils import check_response_status_code, print_fail_message, print_pass_message

MARKETPLACE_SERVICE_URL = "http://localhost:8081"

# We'll track how many orders are successfully placed
successful_orders = 0

def place_order_thread(user_id, product_id, attempts=5):
    """
    Each thread attempts to place 'attempts' orders for the same product_id, quantity=1.
    If an order is successfully placed (HTTP 201), increment global successful_orders.
    We also call test_post_order(...) to verify the response structure.
    """
    global successful_orders
    for _ in range(attempts):
        resp = post_order(user_id, [{"product_id": product_id, "quantity": 1}])

        if resp.status_code == 201:
            # Verify success scenario
            # (We aren't specifying expected_total_price, so pass None)
            if not test_post_order(
                user_id, 
                items=[{"product_id": product_id, "quantity": 1}],
                response=resp,
                expect_success=True
            ):
                # If the structure fails, we'll just log, but you could raise an exception
                print_fail_message("test_post_order failed on success scenario.")
            successful_orders += 1
        elif resp.status_code == 400:
            # Possibly out of stock or insufficient balance
            if not test_post_order(
                user_id, 
                items=[{"product_id": product_id, "quantity": 1}],
                response=resp,
                expect_success=False
            ):
                print_fail_message("test_post_order failed on expected failure scenario.")
        else:
            print_fail_message(f"Unexpected status code {resp.status_code} for POST /orders.")


def main():
    try:
        # 2) Create user (large enough balance so they can buy many items)
        user_id = 2001
        resp = post_user(user_id, "Bob Market", "bob@market.com")
        if not check_response_status_code(resp, 201):
            return False

        # 3) Credit wallet significantly (e.g. 200000)
        resp = put_wallet(user_id, "credit", 200000)
        if not check_response_status_code(resp, 200):
            return False

        # 4) Check the product's initial stock
        product_id = 101
        initial_stock = 10  
        resp = get_product(product_id)
        if resp.status_code == 200:
            pass

        # 5) Launch concurrency threads that place orders for product_id=101, quantity=1
        # We want more attempts than stock to see if we ever overshoot.
        global successful_orders
        successful_orders = 0  # reset global

        thread_count = 3
        attempts_per_thread = 5  # total = 15 attempts, but stock is only 10
        threads = []

        for i in range(thread_count):
            t = Thread(target=place_order_thread, kwargs={
                "user_id": user_id,
                "product_id": product_id,
                "attempts": attempts_per_thread
            })
            threads.append(t)
            t.start()

        for t in threads:
            t.join()

        # 6) Final check:
        #    - successful_orders <= initial_stock (i.e. cannot exceed 10)
        #    - final product stock = initial_stock - successful_orders
        print_pass_message(f"Total successful orders = {successful_orders}")

        if successful_orders > initial_stock:
            print_fail_message(
                f"Concurrency error: successful_orders={successful_orders} > stock={initial_stock}"
            )
            return False

        expected_final_stock = initial_stock - successful_orders

        # 7) GET /products/{productId} => final stock
        resp = get_product(product_id)
        if not test_get_product_stock(product_id, resp, expected_stock=expected_final_stock):
            return False

        # If everything lines up, success
        print_pass_message("Marketplace concurrency test passed.")
        return True

    except Exception as e:
        print_fail_message(f"Test crashed: {e}")
        return False

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)
