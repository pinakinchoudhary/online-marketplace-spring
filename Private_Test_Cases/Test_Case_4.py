import requests

from user import post_user, delete_users
from wallet import put_wallet, get_wallet
from marketplace import get_product, delete_order, delete_user_orders
from utils import print_fail_message, print_pass_message

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"


def main():
    name = "John Doe"
    email = "johndoe@mail.com"
    userId = 101
    productId = 104
    walletAmount = 2000
    add_money_and_check_detail(userId, name, email, productId, walletAmount)


def add_money_and_check_detail(userId, name, email, productId, walletAmount):
    try:
        delete_user_orders(101)
        delete_users()
        new_user = post_user(userId, name, email)
        new_userid = new_user.json()['id']
        put_wallet(new_userid, "credit", walletAmount)
        new_order = {"user_id": new_userid, "items": [{"product_id": productId, "quantity": 2}], }
        # placing an order in the market place
        response = requests.post(marketplaceServiceURL + "/orders", json=new_order)
        response_2 = requests.post(marketplaceServiceURL + "/orders", json=new_order)
        delete_order(response.json()['order_id'])
        response_3 = requests.post(marketplaceServiceURL + "/orders", json=new_order)
        if response_2.status_code == 400 and response_3.status_code == 201:
            print_pass_message("Successful")
        else:
            print_fail_message("Unsuccessful")
        delete_user_orders(101)
    except:
        print("Some Exception Occurred")


if __name__ == "__main__":
    main()
