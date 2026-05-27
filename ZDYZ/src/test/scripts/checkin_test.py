import threading
import requests
import time
import sys

# 配置
API_URL = "http://localhost:8081/admin/join/checkin"
# 从命令行参数获取 JOIN_ID，如果没有则使用默认值
TARGET_JOIN_ID = sys.argv[1] if len(sys.argv) > 1 else "join_manual_test_001"
HEADERS = {"Content-Type": "application/json"} # 如果有 Token 认证需要加上 Authorization

def send_checkin_request(user_id):
    try:
        data = {"id": TARGET_JOIN_ID}
        # 模拟不同用户/线程同时发起请求
        print(f"Thread-{user_id} sending request...")
        response = requests.post(API_URL, json=data, headers=HEADERS)
        print(f"Thread-{user_id} Response: {response.status_code} - {response.text}")
    except Exception as e:
        print(f"Thread-{user_id} Error: {e}")

if __name__ == "__main__":
    print(f"Testing checkin for ID: {TARGET_JOIN_ID}")
    threads = []
    # 模拟 10 个并发请求
    for i in range(10):
        t = threading.Thread(target=send_checkin_request, args=(i,))
        threads.append(t)
    
    start_time = time.time()
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    print(f"Total time: {time.time() - start_time}s")
