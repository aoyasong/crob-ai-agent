"""Seed menu data via API"""
import requests
import json

BASE = "http://127.0.0.1:48080/api"

# Login
r = requests.post(f"{BASE}/system/auth/login", json={"username": "admin", "password": "admin123"})
token = r.json()["data"]["accessToken"]
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# Clear existing test data
r = requests.get(f"{BASE}/system/menu/simple-list", headers=headers)
if r.status_code == 200:
    for m in r.json()["data"]:
        requests.delete(f"{BASE}/system/menu/delete?id={m['id']}", headers=headers)

menus = [
    {"id":1,"name":"系统管理","permission":"","type":1,"sort":0,"parentId":0,"path":"/system","icon":"ep:setting","component":"","componentName":"System","status":0,"visible":True,"keepAlive":True,"alwaysShow":True},
    {"id":2,"name":"Crob Agent","permission":"","type":1,"sort":1,"parentId":0,"path":"/crob","icon":"ep:chat-dot-round","component":"","componentName":"Crob","status":0,"visible":True,"keepAlive":True,"alwaysShow":True},
    {"id":10,"name":"用户管理","permission":"system:user:list","type":2,"sort":0,"parentId":1,"path":"user","icon":"ep:user","component":"system/user/index","componentName":"SystemUser","status":0,"visible":True,"keepAlive":True,"alwaysShow":True},
    {"id":11,"name":"角色管理","permission":"system:role:list","type":2,"sort":1,"parentId":1,"path":"role","icon":"ep:user-filled","component":"system/role/index","componentName":"SystemRole","status":0,"visible":True,"keepAlive":True,"alwaysShow":True},
    {"id":12,"name":"菜单管理","permission":"system:menu:list","type":2,"sort":2,"parentId":1,"path":"menu","icon":"ep:menu","component":"system/menu/index","componentName":"SystemMenu","status":0,"visible":True,"keepAlive":True,"alwaysShow":True},
    {"id":20,"name":"Agent对话","permission":"","type":2,"sort":0,"parentId":2,"path":"chat","icon":"ep:chat-line-square","component":"crob/chat/index","componentName":"CrobChat","status":0,"visible":True,"keepAlive":True,"alwaysShow":True},
    {"id":21,"name":"任务列表","permission":"crob:task:list","type":2,"sort":1,"parentId":2,"path":"task","icon":"ep:list","component":"crob/task/index","componentName":"CrobTask","status":0,"visible":True,"keepAlive":True,"alwaysShow":True},
]

for m in menus:
    r = requests.post(f"{BASE}/system/menu/create", json=m, headers=headers)
    status = "OK" if r.status_code == 200 else f"FAIL({r.status_code})"
    print(f"{status} - {m['name']}")
    if r.status_code != 200:
        print(f"  {r.text[:200]}")

# Also insert role-menu associations
print("\n--- Role-Menu ---")
# We need to insert directly via SQL or another API
# The role-menu permission API is at /system/permission/assign-role-menu
# But our stub just returns empty, so we skip for now
