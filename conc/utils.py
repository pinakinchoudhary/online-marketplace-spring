import requests
from http.client import responses
from colorama import Fore, Style

def print_request(method, url, args = {}):
    args_list = []
    for arg in args:
        args_list.append(f"{Style.BRIGHT}{arg}{Style.RESET_ALL}: {args[arg]}")
    args_string = ", ".join(args_list)

    print()
    print(f"=> {Style.BRIGHT}{Fore.BLUE}{method}{Style.RESET_ALL} {url}")
    if (len(args) > 0):
        print(f"   {args}")

def print_response(response):
    status_code = response.status_code
    print(f"<= {Style.BRIGHT}HTTP {status_code} ({responses.get(status_code, '???')}){Style.RESET_ALL}")
    try:
        print(f"   {response.json()}\n")
    except:
        print(f"   {response.text}\n")

def print_fail_message(message):
    print(f"   {Fore.RED}{Style.BRIGHT}Fail{Style.RESET_ALL}: {message}")

def print_pass_message(message):
    print(f"   {Fore.GREEN}{Style.BRIGHT}Pass{Style.RESET_ALL}: {message}")

def check_response_status_code(response, code):
    if response.status_code != code:
        print_fail_message(f"HTTP status code expected {code}, got {response.status_code}")
        return False
    print_pass_message(f"HTTP status code expected {code}, got {response.status_code}")
    return True

def check_json_exists(response):
    content_type_header = response.headers.get('Content-Type', '')
    if 'application/json' not in content_type_header:
        print_fail_message(f"JSON payload doesn't exist (Content-Type: {content_type_header})")
        return False
    print_pass_message(f"JSON payload exists (Content-Type: {content_type_header})")
    return True

def check_field_exists(payload, field):
    if field not in payload:
        print_fail_message(f"Field '{field}' not present in payload")
        return False
    print_pass_message(f"Field '{field}' present in payload")
    return True

def check_field_type(payload, field, expected_type):
    if not isinstance(payload[field], expected_type):
        print_fail_message(f"Field '{field}' not of expected type {expected_type}")
        return False
    print_pass_message(f"Field '{field}' of expected type {expected_type}")
    return True

def check_field_value(payload, field, value):
    if payload[field] != value:
        print_fail_message(f"Field '{field}' incorrect value, expected '{value}', got '{payload[field]}'")
        return False
    print_pass_message(f"Field '{field}' correct value, expected '{value}', got '{payload[field]}'")
    return True

def check_fields_count(payload, expected_count):
    actual_count = len(payload)
    if actual_count != expected_count:
        print_fail_message(f"Field count incorrect, expected {expected_count}, got {actual_count}")
        return False
    print_pass_message(f"Field count correct, expected {expected_count}, got {actual_count}")
    return True