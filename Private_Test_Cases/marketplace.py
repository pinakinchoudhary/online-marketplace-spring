import requests
from utils import *

MARKETPLACE_SERVICE_URL = "http://localhost:8081"


############################
# Marketplace Endpoints
############################

def get_product(product_id):
    """
    GET /products/{productId}
    """
    print_request('GET', f"{MARKETPLACE_SERVICE_URL}/products/{product_id}")
    response = requests.get(f"{MARKETPLACE_SERVICE_URL}/products/{product_id}")
    print_response(response)
    return response

def delete_marketplace():
    """
    DELETE /marketplace
    - Cancels all 'PLACED' orders and resets product states.
    - Return 200 on success
    """
    print_request('DELETE', f"{MARKETPLACE_SERVICE_URL}/marketplace")
    response = requests.delete(f"{MARKETPLACE_SERVICE_URL}/marketplace")
    print_response(response)
    return response

def post_order(user_id, items):
    """
    POST /orders
    items = [
      {"product_id": XXX, "quantity": YY},
      ...
    ]
    Returns the raw requests.Response.
    """
    payload = {
        "user_id": user_id,
        "items": items
    }
    print_request('POST', f"{MARKETPLACE_SERVICE_URL}/orders", payload)
    response = requests.post(f"{MARKETPLACE_SERVICE_URL}/orders", json=payload)
    print_response(response)
    return response

def delete_order(order_id):
    """
    DELETE /orders/{orderId}
    - If order is 'PLACED', allow cancellation => 200
    - If order not found or already 'CANCELLED'/'DELIVERED', return 400
    """
    print_request('DELETE', f"{MARKETPLACE_SERVICE_URL}/orders/{order_id}")
    response = requests.delete(f"{MARKETPLACE_SERVICE_URL}/orders/{order_id}")
    print_response(response)
    return response

def delete_user_orders(user_id):
    """
    DELETE /marketplace/users/{userId}
    - Cancel any Orders for the given user that are in "PLACED" status, by making the DELETE requests.
    - Return 200 if at least one record was removed, else return 404 if the user had no orders with status "PLACED”.
    """
    print_request('DELETE', f"{MARKETPLACE_SERVICE_URL}/marketplace/users/{user_id}")
    response = requests.delete(f"{MARKETPLACE_SERVICE_URL}/marketplace/users/{user_id}")
    print_response(response)
    return response

def put_order(order_id, status):
    """
    PUT /orders/{orderId}
    - Update the status of the order to 'DELIVERED'.
    - Return 200 on success.
    - Return 400 if status is not 'DELIVERED', order does not exist, or order is not in 'PLACED' status.
    """
    payload = {
        "order_id": order_id,
        "status": status
    }
    print_request('PUT', f"{MARKETPLACE_SERVICE_URL}/orders/{order_id}", payload)
    response = requests.put(f"{MARKETPLACE_SERVICE_URL}/orders/{order_id}", json=payload)
    print_response(response)
    return response

############################
# Test Verification Helpers
############################

def test_get_product_stock(product_id, response, expected_stock=None):
    """
    Checks GET /products/{productId} response
    - Must have 200 if product found
    - Must have 'id' (int) == product_id
    - Must have 'stock_quantity' (int)
    - Optionally check stock_quantity == expected_stock
    """
    if not check_json_exists(response):
        return False

    payload = response.json()

    if not check_field_exists(payload, 'id'):
        return False
    if not check_field_type(payload, 'id', int):
        return False
    if not check_field_value(payload, 'id', product_id):
        return False

    if not check_field_exists(payload, 'stock_quantity'):
        return False
    if not check_field_type(payload, 'stock_quantity', int):
        return False

    if expected_stock is not None:
        actual_stock = payload['stock_quantity']
        if actual_stock != expected_stock:
            print_fail_message(
                f"Product {product_id} stock mismatch. Expected {expected_stock}, got {actual_stock}"
            )
            return False
        else:
            print_pass_message(f"Product {product_id} stock is correct => {actual_stock}")

    # We expect 200 if the product exists
    if not check_response_status_code(response, 200):
        return False

    return True


def test_post_order(user_id, items, response, expect_success=True, expected_total_price=None):
    """
    Verifies POST /orders result.

    If expect_success = True:
      - status_code should be 201
      - response JSON should have:
          'order_id' (int),
          'user_id' (int) == user_id,
          'total_price' (int) matching the sum of item costs (minus any discount if first order),
          'status' = "PLACED",
          'items' = list of objects { 'id', 'product_id', 'quantity' }
    If expected_total_price is given, we check it.

    If expect_success = False:
      - we typically expect a 400 (insufficient stock, invalid user, or insufficient balance).
    """
    if expect_success:
        # Expect 201
        if not check_response_status_code(response, 201):
            return False
        if not check_json_exists(response):
            return False

        payload = response.json()

        # Check 'order_id'
        if not check_field_exists(payload, 'order_id'):
            return False
        if not check_field_type(payload, 'order_id', int):
            return False

        # Check 'user_id'
        if not check_field_exists(payload, 'user_id'):
            return False
        if not check_field_type(payload, 'user_id', int):
            return False
        if not check_field_value(payload, 'user_id', user_id):
            return False

        # Check 'total_price'
        if not check_field_exists(payload, 'total_price'):
            return False
        if not check_field_type(payload, 'total_price', int):
            return False
        if expected_total_price is not None:
            if payload['total_price'] != expected_total_price:
                print_fail_message(
                    f"Expected total_price={expected_total_price}, got {payload['total_price']}"
                )
                return False
            else:
                print_pass_message(
                    f"Order total_price is correct => {payload['total_price']}"
                )

        # Check 'status'
        if not check_field_exists(payload, 'status'):
            return False
        if not check_field_type(payload, 'status', str):
            return False
        if payload['status'] != "PLACED":
            print_fail_message(f"Order status expected 'PLACED', got '{payload['status']}'")
            return False
        else:
            print_pass_message("Order status is 'PLACED' as expected.")

        # Check 'items'
        if not check_field_exists(payload, 'items'):
            return False
        if not isinstance(payload['items'], list):
            print_fail_message("Order 'items' should be a list.")
            return False
        # Optionally verify each item in the array has 'id', 'product_id', 'quantity'
        for itm in payload['items']:
            if 'id' not in itm or 'product_id' not in itm or 'quantity' not in itm:
                print_fail_message(f"Order item missing fields: {itm}")
                return False

        # Count fields? The doc doesn't strictly say how many fields in total,
        # but you can do check_fields_count(payload, 5) if your JSON always has 5 fields:
        #   order_id, user_id, total_price, status, items
        # We'll skip that for flexibility.

        return True

    else:
        # expect_success = False => we expect an error, typically 400
        if not check_response_status_code(response, 400):
            return False
        return True


def test_delete_order(order_id, response, can_cancel=True):
    """
    DELETE /orders/{orderId}
    - If can_cancel = True, we expect 200 (order was in 'PLACED' state).
    - Otherwise (order doesn't exist or is CANCELLED/DELIVERED), doc says return 400.
    """
    if can_cancel:
        return check_response_status_code(response, 200)
    else:
        # we expect 400
        return check_response_status_code(response, 400)


def test_delete_user_orders(user_id, response, expect_success=True):
    """
    Verifies DELETE /marketplace/users/{userId} result.

    If expect_success = True:
        - status_code should be 200
        - response JSON should have:
            'message' (str) indicating success
    If expect_success = False:
        - we typically expect a 404 (no orders with status "PLACED" for the user).
    """
    if expect_success:
        # Expect 200
        if not check_response_status_code(response, 200):
            return False
        if not check_json_exists(response):
            return False

        payload = response.json()

        # Check 'message'
        if not check_field_exists(payload, 'message'):
            return False
        if not check_field_type(payload, 'message', str):
            return False
        if "success" not in payload['message'].lower():
            print_fail_message(f"Expected success message, got '{payload['message']}'")
            return False
        else:
            print_pass_message("Delete user orders success message is as expected.")

        return True

    else:
        # expect_success = False => we expect an error, typically 404
        if not check_response_status_code(response, 404):
            return False
        return True

def test_put_order(order_id, status, response, expect_success=True):
    """
    Verifies PUT /orders/{orderId} result.

    If expect_success = True:
        - status_code should be 200
        - response JSON should have:
            'order_id' (int) == order_id,
            'status' (str) == status
    If expect_success = False:
        - we typically expect a 400 (invalid status, order does not exist, or order is not in 'PLACED' status).
    """
    if expect_success:
        # Expect 200
        if not check_response_status_code(response, 200):
            return False
        if not check_json_exists(response):
            return False

        payload = response.json()

        # Check 'order_id'
        if not check_field_exists(payload, 'order_id'):
            return False
        if not check_field_type(payload, 'order_id', int):
            return False
        if not check_field_value(payload, 'order_id', order_id):
            return False

        # Check 'status'
        if not check_field_exists(payload, 'status'):
            return False
        if not check_field_type(payload, 'status', str):
            return False
        if not check_field_value(payload, 'status', status):
            return False

        return True

    else:
        # expect_success = False => we expect an error, typically 400
        if not check_response_status_code(response, 400):
            return False
        return True

