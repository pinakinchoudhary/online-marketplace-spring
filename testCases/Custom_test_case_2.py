import requests
import sys

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def setup_user_wallet_and_product(user_id, product_id, initial_balance=1000000):
    """Helper function to create a user, initialize a wallet, and set product stock."""
    requests.post(userServiceURL + "/users", json={"id": user_id, "name": "Test User", "email": "testuser@mail.com"})
    requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action": "credit", "amount": initial_balance})

def place_order(user_id, product_id, quantity=1):
    """Helper function to place an order."""
    payload = {"user_id": user_id, "items": [{"product_id": product_id, "quantity": quantity}]}
    response = requests.post(marketplaceServiceURL + "/orders", json=payload)
    if response.status_code == 201:
        return response.json().get("order_id")
    else:
        sys.exit()

def get_order_details(order_id):
    """Helper function to retrieve order details."""
    response = requests.get(marketplaceServiceURL + f"/orders/{order_id}")
    return response

def get_wallet_balance(user_id):
    """Helper function to retrieve the user's wallet balance."""
    response = requests.get(walletServiceURL + f"/wallets/{user_id}")
    return response.json()["balance"]

def get_product_stock(product_id):
    """Helper function to retrieve the product's stock quantity."""
    response = requests.get(marketplaceServiceURL + f"/products/{product_id}")
    return response.json()["stock_quantity"]

def test_cancel_order():
    """Test case: Cancelling a placed order should restore stock and refund user."""
    product_id = 101
    quantity = 2
    setup_user_wallet_and_product(user_id, product_id)
    initial_wallet_balance = get_wallet_balance(user_id)
    initial_stock = get_product_stock(product_id)
    order_id = place_order(user_id, product_id, quantity)
    response = requests.delete(marketplaceServiceURL + f"/orders/{order_id}")
    if response.status_code == 200:
        updated_wallet_balance = get_wallet_balance(user_id)
        updated_stock = get_product_stock(product_id)

        if updated_stock == initial_stock and updated_wallet_balance == initial_wallet_balance:
        else:
            sys.exit()
    else:
        sys.exit()

def test_cancel_nonexistent_order():
    """Test case: Attempting to cancel a non-existent order should return 400."""
    non_existent_order_id = 99999
    response = requests.delete(marketplaceServiceURL + f"/orders/{non_existent_order_id}")

    if response.status_code == 400:
    else:
        sys.exit()

def test_cancel_already_cancelled_order():
    """Test case: Cancelling an already cancelled order should return 400."""
    user_id = 1
    product_id = 101 
    setup_user_wallet_and_product(user_id, product_id)
    order_id = place_order(user_id, product_id)
    requests.delete(marketplaceServiceURL + f"/orders/{order_id}")  # First cancellation
    response = requests.delete(marketplaceServiceURL + f"/orders/{order_id}")

    if response.status_code == 400:
    else:
        sys.exit()

def test_get_cancelled_order():
    """Test case: After cancelling an order, GET request should return order details with status CANCELLED."""
    user_id = 1
    product_id = 101
    setup_user_wallet_and_product(user_id, product_id)
    order_id = place_order(user_id, product_id)
    requests.delete(marketplaceServiceURL + f"/orders/{order_id}")
    response = get_order_details(order_id)
    
    if response.status_code == 200:
        order_data = response.json()
        if order_data["status"] == "CANCELLED":
        else:
            sys.exit()
    else:
        sys.exit()

def test_get_nonexistent_order():
    """Test case: GET request to a non-existent order should return 404."""
    non_existent_order_id = 99999
    response = get_order_details(non_existent_order_id)

    if response.status_code == 404:
    else:
        sys.exit()

if __name__ == "__main__":
    test_cancel_order()
    test_cancel_nonexistent_order()
    test_cancel_already_cancelled_order()
    test_get_cancelled_order()
    test_get_nonexistent_order()

