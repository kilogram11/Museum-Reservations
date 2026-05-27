import requests
import random
import string
import time

# 配置
BASE_URL = "http://localhost:8081"
LOGIN_URL = f"{BASE_URL}/app/user/login"
IDENTITY_SAVE_URL = f"{BASE_URL}/app/identity/save"

# 测试参数
TARGET_MOBILE = "13900000088" # 测试账号手机号
TARGET_CODE = "1234"           # 测试账号验证码
IDENTITY_COUNT = 100           # 需要插入的预约人数

# 禁用 urllib3 警告
requests.packages.urllib3.disable_warnings()

def random_string(length=10):
    return ''.join(random.choices(string.ascii_letters + string.digits, k=length))

def random_cn_name():
    # 简单模拟中文名
    first_names = "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨"
    last_names = "伟芳秀英娜敏静丽强磊军洋刚明"
    return random.choice(first_names) + random.choice(last_names) + random.choice(last_names)

def random_id_card():
    # 生成随机身份证号 (简单模拟，非严格校验)
    # 格式: 110101 YYYY MM DD XXXX
    prefix = "110101"
    year = str(random.randint(1980, 2000))
    month = f"{random.randint(1, 12):02d}"
    day = f"{random.randint(1, 28):02d}"
    suffix = "".join(random.choices(string.digits, k=4))
    return f"{prefix}{year}{month}{day}{suffix}"

def random_mobile():
    return "138" + "".join(random.choices(string.digits, k=8))

class IdentityInserter:
    def __init__(self, mobile, code):
        self.mobile = mobile
        self.code = code
        self.token = None

    def login(self):
        try:
            print(f"Logging in with {self.mobile}...")
            resp = requests.post(LOGIN_URL, json={"mobile": self.mobile, "code": self.code})
            if resp.status_code == 200:
                result = resp.json()
                if result.get("code") == 200:
                    self.token = result["data"]["token"]
                    print("Login success.")
                    return True
                else:
                    print(f"Login failed: {result.get('msg')}")
            else:
                print(f"Login HTTP failed: {resp.status_code}")
            return False
        except Exception as e:
            print(f"Login error: {e}")
            return False

    def add_identities(self, count):
        if not self.token:
            print("No token, cannot proceed.")
            return

        print(f"Starting to add {count} identities...")
        success_count = 0
        headers = {"Token": self.token, "Content-Type": "application/json"}

        for i in range(count):
            name = f"TestVisitor_{i}_{random_string(3)}"
            id_card = random_id_card()
            phone = random_mobile()
            
            data = {
                "identityName": name,
                "identityCard": id_card,
                "identityMobile": phone
            }

            try:
                resp = requests.post(IDENTITY_SAVE_URL, json=data, headers=headers)
                if resp.status_code == 200 and resp.json().get("code") == 200:
                    success_count += 1
                    if (i + 1) % 10 == 0:
                        print(f"Progress: {i + 1}/{count} added.")
                else:
                    print(f"Failed to add index {i}: {resp.text}")
            except Exception as e:
                print(f"Error adding index {i}: {e}")
            
            # 简单延时防止过快
            # time.sleep(0.01)

        print(f"Finished. Successfully added {success_count}/{count} identities.")

if __name__ == "__main__":
    inserter = IdentityInserter(TARGET_MOBILE, TARGET_CODE)
    if inserter.login():
        inserter.add_identities(IDENTITY_COUNT)
