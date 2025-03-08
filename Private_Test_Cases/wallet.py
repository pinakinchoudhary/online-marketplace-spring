import requests
from utils import *
from utils import check_json_exists

walletServiceURL = "http://localhost:8082"

def put_wallet(user_id, action, amount):
    payload = {"action": action, "amount": amount}
    response = requests.put(walletServiceURL + f"/wallets/{user_id}", json=payload)
    print_request('PUT', f'/wallets/{user_id}', payload)
    print_response(response)
    return response

def get_wallet(user_id):
    response = requests.get(f"{walletServiceURL}/wallets/{user_id}")
    print_request('GET', f'/wallets/{user_id}')
    print_response(response)
    return response

def delete_wallet(user_id):
    response = requests.delete(f"{walletServiceURL}/wallets/{user_id}")
    print_request('DELETE', f'/wallets/{user_id}')
    print_response(response)
    return response

def delete_wallets():
    response = requests.delete(walletServiceURL + "/wallets")
    print_request('DELETE', f'/wallets')
    print_response(response)
    return response

def test_get_wallet(user_id, response, exists, balance=None):
    if exists:
        if not check_json_exists(response):
            return False
        
        payload = response.json()

        if not check_field_exists(payload, 'user_id'):
            return False

        if not check_field_type(payload, 'user_id', int):
            return False

        if not check_field_value(payload, 'user_id', user_id):
            return False

        if not check_field_exists(payload, 'balance'):
            return False

        if not check_field_type(payload, 'balance', int):
            return False

        if balance and not check_field_value(payload, 'balance', balance):
            return False

        if not check_fields_count(payload, 2):
            return False
    
        if not check_response_status_code(response, 200):
            return False

        return True
    else:
        if not check_response_status_code(response, 404):
            return False

        return True

def test_put_wallet(user_id, action, amount, old_balance, response):
    if (action == 'debit' and old_balance < amount):
        # Expect insufficient balance => HTTP 400
        if response.status_code == 400:
            print_pass_message(f"Insufficient balance, expected HTTP 400, got HTTP {response.status_code}")
            return True
        else:
            print_fail_message(f"Insufficient balance, expected HTTP 400, got HTTP {response.status_code}")
            return False
    
    if not check_json_exists(response):
        return False
        
    payload = response.json()

    if not check_field_exists(payload, 'user_id'):
        return False

    if not check_field_type(payload, 'user_id', int):
        return False

    if not check_field_value(payload, 'user_id', user_id):
        return False
    
    if not check_field_exists(payload, 'balance'):
        return False

    if not check_field_type(payload, 'balance', int):
        return False

    if not check_fields_count(payload, 2):
        return False
        
    if action == 'credit':
        if payload['balance'] != old_balance + amount:
            print_fail_message(
                f"Field 'balance' incorrectly updated, "
                f"expected {old_balance + amount}, got {payload['balance']}."
            )
            return False
        else:
            print_pass_message(
                f"Field 'balance' correctly updated, expected {old_balance + amount}, "
                f"got {payload['balance']}."
            )

    if action == 'debit':
        if payload['balance'] != old_balance - amount:
            print_fail_message(
                f"Field 'balance' incorrectly updated, "
                f"expected {old_balance - amount}, got {payload['balance']}."
            )
            return False
        else:
            print_pass_message(
                f"Field 'balance' correctly updated, expected {old_balance - amount}, "
                f"got {payload['balance']}."
            )

    if not check_response_status_code(response, 200):
        return False
        
    return True

def test_delete_wallet(response, exists):
    if exists:
        if not check_response_status_code(response, 200):
            return False

        return True
    else:
        if not check_response_status_code(response, 404):
            return False

        return True
