import requests
from utils import *

userServiceURL = "http://localhost:8080"  # Adjust if needed

######################
# Account Service Calls
######################

def post_user(user_id, name, email):
    """
    POST /users
    Request body -> {
      "id": <int>,
      "name": <str>,
      "email": <str>
    }
    """
    new_user = {
        "id": user_id,
        "name": name,
        "email": email
    }
    print_request('POST', '/users', new_user)
    response = requests.post(userServiceURL + "/users", json=new_user)
    print_response(response)
    return response

def get_user(user_id):
    """
    GET /users/{userId}
    """
    print_request('GET', f'/users/{user_id}')
    response = requests.get(f"{userServiceURL}/users/{user_id}")
    print_response(response)
    return response

def put_user_discount(user_id, discount_availed):
    """
    PUT /users/{userId}
    - The doc mentions a PUT endpoint to update discount_availed = true,
      though not always tested. We'll implement for completeness.
    Request body -> {
      "id": <int>,
      "discount_availed": <bool>
    }
    """
    payload = {
        "id": user_id,
        "discount_availed": discount_availed
    }
    print_request('PUT', f'/users/{user_id}', payload)
    response = requests.put(f"{userServiceURL}/users/{user_id}", json=payload)
    print_response(response)
    return response

def delete_user(user_id):
    """
    DELETE /users/{userId}
    - If user not found, 404
    - Else calls DELETE /marketplace/users/{userId}, DELETE /wallets/{userId}
      then removes user, returns 200
    """
    print_request('DELETE', f'/users/{user_id}')
    response = requests.delete(f"{userServiceURL}/users/{user_id}")
    print_response(response)
    return response

def delete_users():
    """
    DELETE /users
    - Removes all users in the Account Service
    - Also calls DELETE /marketplace, DELETE /wallets
    - Returns 200
    """
    print_request('DELETE', f'/users')
    response = requests.delete(userServiceURL + "/users")
    print_response(response)
    return response

########################
# Test Helper Functions
########################

def test_post_user(user_id, name, email, response):
    """
    After POST /users, we expect:
      - 4 fields in the JSON: {id, name, email, discount_availed}
      - discount_availed = false
      - status code = 201
    """
    if not check_json_exists(response):
        return False
    
    payload = response.json()

    # Check presence & types of fields
    if not check_field_exists(payload, 'id'):
        return False
    if not check_field_type(payload, 'id', int):
        return False
    if not check_field_value(payload, 'id', user_id):
        return False

    if not check_field_exists(payload, 'name'):
        return False
    if not check_field_type(payload, 'name', str):
        return False
    if not check_field_value(payload, 'name', name):
        return False

    if not check_field_exists(payload, 'email'):
        return False
    if not check_field_type(payload, 'email', str):
        return False
    if not check_field_value(payload, 'email', email):
        return False

    if not check_field_exists(payload, 'discount_availed'):
        return False
    if not check_field_type(payload, 'discount_availed', bool):
        return False
    if not check_field_value(payload, 'discount_availed', False):
        return False

    # We expect 4 total fields in the returned JSON
    if not check_fields_count(payload, 4):
        return False

    # Check status code
    if not check_response_status_code(response, 201):
        return False

    return True

def test_get_user(user_id, response, exists, discount_availed=None, name=None, email=None):
    """
    After GET /users/{userId}:
      - If exists=True, expect 200 and 4 fields {id, name, email, discount_availed}
      - If exists=False, expect 404
    Optionally check discount_availed, name, email if provided.
    """
    if not exists:
        # Expect 404
        return check_response_status_code(response, 404)

    # If exists = True
    if not check_response_status_code(response, 200):
        return False

    if not check_json_exists(response):
        return False

    payload = response.json()

    # Must have 4 fields
    if not check_field_exists(payload, 'id'):
        return False
    if not check_field_type(payload, 'id', int):
        return False
    if not check_field_value(payload, 'id', user_id):
        return False

    if not check_field_exists(payload, 'name'):
        return False
    if not check_field_type(payload, 'name', str):
        return False
    if name is not None:  # optionally check name
        if not check_field_value(payload, 'name', name):
            return False

    if not check_field_exists(payload, 'email'):
        return False
    if not check_field_type(payload, 'email', str):
        return False
    if email is not None:  # optionally check email
        if not check_field_value(payload, 'email', email):
            return False

    if not check_field_exists(payload, 'discount_availed'):
        return False
    if not check_field_type(payload, 'discount_availed', bool):
        return False
    if discount_availed is not None:  # optionally check discount_availed
        if not check_field_value(payload, 'discount_availed', discount_availed):
            return False

    if not check_fields_count(payload, 4):
        return False

    return True

def test_put_user_discount(user_id, discount_availed, response, success_expected):
    """
    After PUT /users/{userId} to set discount_availed:
      - If success_expected=True, expect 200, with JSON {id, name, email, discount_availed} updated
      - If success_expected=False, might expect 404 or 400
    """
    if success_expected:
        if not check_response_status_code(response, 200):
            return False
        if not check_json_exists(response):
            return False

        payload = response.json()

        # Must have 4 fields
        if not check_field_exists(payload, 'id'):
            return False
        if not check_field_type(payload, 'id', int):
            return False
        if not check_field_exists(payload, 'name'):
            return False
        if not check_field_exists(payload, 'email'):
            return False
        if not check_field_exists(payload, 'discount_availed'):
            return False
        if not check_field_type(payload, 'discount_availed', bool):
            return False

        if not check_field_value(payload, 'discount_availed', discount_availed):
            return False

        if not check_fields_count(payload, 4):
            return False

        return True
    else:
        # If failure is expected, you might check for 404 or 400, etc.
        # Here's an example checking 404:
        if not (check_response_status_code(response, 404) or check_response_status_code(response, 400)):
            return False
        return True

def test_delete_user(user_id, response, exists):
    """
    DELETE /users/{userId}
      - if exists, expect 200
      - if doesn't exist, expect 404
    """
    if exists:
        return check_response_status_code(response, 200)
    else:
        return check_response_status_code(response, 404)
