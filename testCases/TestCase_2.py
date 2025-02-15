import requests
import sys

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def main():
    name = "John Doe"
    email = "johndoe@mail.com"
    userId = 101
    productId = 104
    walletAmount = 10000
    add_money_and_check_detail(userId,name, email, productId, walletAmount)

def create_user(userId,name, email):
    new_user = {"id":userId,"name": name, "email": email}
    response = requests.post(userServiceURL + "/users", json=new_user)
    return response

def get_wallet(user_id):
    response = requests.get(walletServiceURL + f"/wallets/{user_id}")
    return response

def update_wallet(user_id, action, amount):
    response = requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action":action, "amount":amount})
    return response

def get_product_details(product_id):
    response = requests.get(marketplaceServiceURL + f"/products/{product_id}")
    if response.status_code != 200:
        print("Wrong status code return during get produt details")
        sys.exit()
    return response


def delete_users():
    requests.delete(userServiceURL+f"/users")


def add_money_and_check_detail(userId,name, email, productId, walletAmount):
    try:
        delete_users()
        new_user = create_user(userId,name,email)
        new_userid = new_user.json()['id']
        update_wallet(new_userid,"credit",walletAmount)
        product_details_before_ordering = get_product_details(productId)
        old_wallet_balance = get_wallet(new_userid).json()['balance']
        new_order = {"user_id": new_userid,"items": [{"product_id": productId, "quantity": 2}], }
        # placing an order in the market place
        requests.post(marketplaceServiceURL + "/orders", json=new_order)
        product_details_after_ordering = get_product_details(productId)
        new_wallet_balance = get_wallet(new_userid).json()['balance']
        # checking for the correct balance amount in the wallet and the correct quantity
        if (old_wallet_balance - new_wallet_balance == (product_details_after_ordering.json()['price'] * 2)*0.9) and (product_details_before_ordering.json()['stock_quantity'] - product_details_after_ordering.json()['stock_quantity'] == 2):
            print("Test Passed")
        else:
            print("Test Failed")
    except:
        print("Some Exception Occurred")

if __name__ == "__main__":
    main()