import requests

from user import post_user, delete_users
from wallet import put_wallet, get_wallet
from marketplace import get_product, delete_order, put_order, delete_user_orders
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
        delete_users()
        new_user = post_user(userId, name, email)
        new_userid = new_user.json()['id']
        put_wallet(new_userid, "credit", walletAmount)
        productDetailBeforeOrdering = get_product(productId)
        new_order = {"user_id": new_userid, "items": [{"product_id": productId, "quantity": 2}], }
        # placing an order in the market place
        response = requests.post(marketplaceServiceURL + "/orders", json=new_order)
        print(f"Get balance after placing the order: {get_wallet(new_userid).json()['balance']}")
        order_id = response.json()['order_id']
        # Mark the order as delivered
        put_order(order_id, "DELIVERED")
        productDetailAfterOrdering = get_product(productId)
        
        # Attempt to delete the user
        delete_user_orders(101)


        print(f"Balance after deleting the user: {get_wallet(new_userid).json()['balance']}")
        
        # Check if the inventory and wallet are not reverted
        new_balance = get_wallet(new_userid)
        productDetailAfterOrdering = get_product(productId)
        old_balance = walletAmount

        if (productDetailBeforeOrdering.json()['stock_quantity'] - productDetailAfterOrdering.json()['stock_quantity'] == 2) and (old_balance - new_balance.json()['balance'] == productDetailAfterOrdering.json()['price'] * 2 * 0.9):
            print_pass_message("Successful")
        else:
            print(f" new balance: {new_balance.json()['balance']}")
            print(f"Error: {old_balance - new_balance.json()['balance']} \n {productDetailAfterOrdering.json()['price'] * 2 * 0.9}")
            print_fail_message("Unsuccessful")
        delete_user_orders(101)
    except Exception as e:
        print(f"Some Exception Occurred: {e}")

if __name__ == "__main__":
    main()
