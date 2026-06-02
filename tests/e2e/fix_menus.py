import requests

r = requests.post(
    "http://127.0.0.1:48080/api/system/auth/login",
    json={"username": "admin", "password": "admin123"},
)
t = r.json()["data"]["accessToken"]
h = {"Authorization": f"Bearer {t}", "Content-Type": "application/json"}

for mid in [1, 2]:
    resp = requests.put(
        "http://127.0.0.1:48080/api/system/menu/update",
        json={"id": mid, "status": 0},
        headers=h,
    )
    print(f"Fix menu {mid}: {resp.status_code}")

r = requests.get(
    "http://127.0.0.1:48080/api/system/menu/list", headers=h
)
print(f"menu/list records: {len(r.json()['data'])}")

r = requests.get(
    "http://127.0.0.1:48080/api/system/auth/get-permission-info", headers=h
)
d = r.json()["data"]
print(f"perm-info: roles={d['roles']}, menus={len(d['menus'])}")
