import sys
import requests


from user import post_user, delete_users
from wallet import put_wallet, get_wallet
from marketplace import get_product, delete_user_orders
from utils import  print_fail_message, print_pass_message

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



def add_money_and_check_detail(userId,name, email, productId, walletAmount):
    try:
        delete_users()
        new_user = post_user(userId,name,email)
        new_userid = new_user.json()['id']
        put_wallet(new_userid,"credit",walletAmount)
        product_details_before_ordering = get_product(productId)
        old_wallet_balance = get_wallet(new_userid).json()['balance']
        new_order = {"user_id": new_userid,"items": [{"product_id": productId, "quantity": 2}], }
        # placing an order in the market place
        requests.post(marketplaceServiceURL + "/orders", json=new_order)
        balance_1 = get_product(productId).json()['price']*2*0.9
        new_order_2= {"user_id": new_userid,"items": [{"product_id": productId, "quantity": 2}], }
        requests.post(marketplaceServiceURL+"/orders",json=new_order_2)
        product_details_after_ordering = get_product(productId)
        balance_2 = balance_1 + get_product(productId).json()['price']*2
        new_wallet_balance = get_wallet(new_userid).json()['balance']
        # checking for the correct balance amount in the wallet and the correct quantity
        if (old_wallet_balance - new_wallet_balance == balance_2 and product_details_before_ordering.json()['stock_quantity'] - product_details_after_ordering.json()['stock_quantity'] == 4):
            print_pass_message("Successful")
        else:
            print_fail_message("Unsuccessful")
        delete_user_orders(101)
    except:
        print("Some Exception Occurred")

if __name__ == "__main__":
    main()