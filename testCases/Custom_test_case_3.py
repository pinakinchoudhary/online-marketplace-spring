import requests
import json

accountServiceURL = "http://localhost:8080"

def main():
    # Test Cases for Account Service
    print("Running Account Service Tests...\n")

    test_create_user_valid()
    test_create_user_invalid_payload()
    test_create_user_duplicate_email()
    test_get_user_existing()
    test_get_user_nonexistent()
    test_delete_user_existing()
    test_delete_user_nonexistent()
    test_delete_all_users()
    test_update_discount_existing()
    test_update_discount_nonexistent()


def test_create_user_valid():
    print("Testing create_user with valid data...")
    user_data = {"id": 1, "name": "Test User", "email": "test@example.com"}  # Provide a unique ID for each test run
    response = requests.post(accountServiceURL + "/users", json=user_data)
    assert response.status_code == 201, f"Expected 201, got {response.status_code}"
    response_data = response.json()
    assert response_data["name"] == "Test User", "Name mismatch"
    assert response_data["email"] == "test@example.com", "Email mismatch"
    print("create_user with valid data passed.\n")


def test_create_user_invalid_payload():
    print("Testing create_user with invalid payload (missing name)...")
    user_data = {"id": 2, "email": "test2@example.com"}
    response = requests.post(accountServiceURL + "/users", json=user_data)
    assert response.status_code == 400, f"Expected 400, got {response.status_code}"
    print("create_user with invalid payload passed.\n")

def test_create_user_duplicate_email():
    print("Testing create_user with duplicate email...")
    user_data = {"id": 3, "name": "Test User 3", "email": "test@example.com"} # Email from test_create_user_valid
    response = requests.post(accountServiceURL + "/users", json=user_data)
    assert response.status_code == 400, f"Expected 400, got {response.status_code}"
    print("create_user with duplicate email passed.\n")


def test_get_user_existing():
    print("Testing get_user with existing user...")
    user_id = 1  # ID from test_create_user_valid
    response = requests.get(accountServiceURL + f"/users/{user_id}")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    response_data = response.json()
    assert response_data["id"] == user_id, "User ID mismatch"
    print("get_user with existing user passed.\n")


def test_get_user_nonexistent():
    print("Testing get_user with nonexistent user...")
    user_id = 999  # Nonexistent ID
    response = requests.get(accountServiceURL + f"/users/{user_id}")
    assert response.status_code == 404, f"Expected 404, got {response.status_code}"
    print("get_user with nonexistent user passed.\n")


def test_delete_user_existing():
    print("Testing delete_user with existing user...")
    user_id = 1  # ID from test_create_user_valid
    response = requests.delete(accountServiceURL + f"/users/{user_id}")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    # Verify deletion by trying to get the user
    get_response = requests.get(accountServiceURL + f"/users/{user_id}")
    assert get_response.status_code == 404, "User should be deleted"
    print("delete_user with existing user passed.\n")

def test_delete_user_nonexistent():
    print("Testing delete_user with nonexistent user...")
    user_id = 999  # Nonexistent ID
    response = requests.delete(accountServiceURL + f"/users/{user_id}")
    assert response.status_code == 404, f"Expected 404, got {response.status_code}"
    print("delete_user with nonexistent user passed.\n")

def test_delete_all_users():
    print("Testing delete_all_users...")
    response = requests.delete(accountServiceURL + "/users")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    print("delete_all_users passed.\n")

def test_update_discount_existing():
    print("Testing update_discount with existing user...")
    # First, create a user (if you don't have a persistent test user)
    user_data = {"id": 4, "name": "Discount User", "email": "discount@example.com"}
    create_response = requests.post(accountServiceURL + "/users", json=user_data)
    assert create_response.status_code == 201, "Failed to create user for discount test"
    user_id = create_response.json()["id"]

    response = requests.put(accountServiceURL + f"/updateDiscount/{user_id}")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    print("update_discount with existing user passed.\n")

def test_update_discount_nonexistent():
    print("Testing update_discount with nonexistent user...")
    user_id = 999  # Nonexistent ID
    response = requests.put(accountServiceURL + f"/updateDiscount/{user_id}")
    assert response.status_code == 404, f"Expected 404, got {response.status_code}"
    print("update_discount with nonexistent user passed.\n")

if __name__ == "__main__":
    main()
