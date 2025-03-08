import requests

from user import post_user, delete_users
from wallet import put_wallet, get_wallet
from marketplace import get_product, delete_user_orders
from utils import print_fail_message, print_pass_message

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"


def main():
    name = "John Doe"
    email = "johndoe@mail.com"
    userId = 101
    productId = 104
    walletAmount = 1000
    add_money_and_check_detail(userId, name, email, productId, walletAmount)


def add_money_and_check_detail(userId, name, email, productId, walletAmount):
    try:
        delete_users()
        new_user = post_user(userId, name, email)
        new_userid = new_user.json()['id']
        put_wallet(new_userid, "credit", walletAmount)
        old_balance = get_wallet(new_userid)
        new_order = {"user_id": new_userid, "items": [{"product_id": productId, "quantity": 2}], }
        # placing an order in the market place
        requests.post(marketplaceServiceURL + "/orders", json=new_order)
        productDetailBeforeOrdering = get_product(productId)
        put_wallet(new_userid, "credit", 2000)
        requests.post(marketplaceServiceURL + "/orders", json=new_order)
        new_balance = get_wallet(new_userid)
        productDetailAfterOrdering = get_product(productId)
        print("Product Detail Before Ordering:", productDetailBeforeOrdering.json()['stock_quantity'])
        print("Product Detail After Ordering:", productDetailAfterOrdering.json()['stock_quantity'])
        print("Old Balance:", old_balance.json()['balance'])
        print("New Balance:", new_balance.json()['balance'])
        if (productDetailBeforeOrdering.json()['stock_quantity'] - productDetailAfterOrdering.json()['stock_quantity'] == 2) and (old_balance.json()['balance'] + 2000 - new_balance.json()['balance'] == productDetailAfterOrdering.json()['price'] * 2 * 0.9):
            print_pass_message("Successful")
        else:
            print_fail_message("Unsuccessful")
        delete_user_orders(101)
    except:
        print("Some Exception Occurred")


if __name__ == "__main__":
    main()
