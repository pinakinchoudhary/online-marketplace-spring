import requests
import sys

# API URLs
userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def setup_user_and_wallet(user_id, balance=10000):
    requests.post(userServiceURL + "/users", json={"id": user_id, "name": "Test User", "email": "testuser@mail.com"})
    requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action": "credit", "amount": balance})

def test_order_with_zero_quantity():
    user_id = 1 
    product_id = 101
    setup_user_and_wallet(user_id)

    payload = {"user_id": user_id, "items": [{"product_id": product_id, "quantity": 0}]}
    response = requests.post(marketplaceServiceURL + "/orders", json=payload)

    if response.status_code == 400:
        print("Test passed: Order with zero quantity was rejected.")
    else:
        print(f"Test failed: Expected 400, got {response.status_code}.\n Error message: {response.text}")
        sys.exit()

def test_order_with_negative_quantity():
    user_id = 1 
    product_id = 101 
    setup_user_and_wallet(user_id)

    payload = {"user_id": user_id, "items": [{"product_id": product_id, "quantity": -5}]}
    response = requests.post(marketplaceServiceURL + "/orders", json=payload)

    if response.status_code == 400:
        print("Test passed: Order with negative quantity was rejected.")
    else:
        print(f"Test failed: Expected 400, got {response.status_code} \n Error: {response.text}.")
        sys.exit()

if __name__ == "__main__":
    test_order_with_zero_quantity()
    test_order_with_negative_quantity()

