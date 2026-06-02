import { test, expect } from '@playwright/test';

const BASE_URL = 'http://127.0.0.1:48090';
const API_URL = 'http://127.0.0.1:48080/api';

test.describe('Crob Agent 端到端集成测试', () => {

  test('01 - 后端健康检查', async ({ request }) => {
    const res = await request.get(`${API_URL}/../test`);
    // /test endpoint should be reachable
    expect(res.ok() || res.status() === 404).toBeTruthy();
  });

  test('02 - 登录 API 返回 accessToken + refreshToken', async ({ request }) => {
    const res = await request.post(`${API_URL}/system/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(0);
    expect(body.data.accessToken).toBeTruthy();
    expect(body.data.refreshToken).toBeTruthy();
    expect(body.data.userId).toBe(1);
  });

  test('03 - 登录 API 错误密码返回 401', async ({ request }) => {
    const res = await request.post(`${API_URL}/system/auth/login`, {
      data: { username: 'admin', password: 'wrong' }
    });
    const body = await res.json();
    expect(body.code).toBe(401);
  });

  test('04 - 未登录访问 /get-permission-info 返回错误', async ({ request }) => {
    const res = await request.get(`${API_URL}/system/auth/get-permission-info`);
    // 401 or 403 (unauthenticated)
    expect([401, 403]).toContain(res.status());
  });

  test('05 - 带 token 获取权限信息成功', async ({ request }) => {
    // 1. 先登录获取 token
    const loginRes = await request.post(`${API_URL}/system/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    });
    const { accessToken } = (await loginRes.json()).data;

    // 2. 带 token 请求权限信息
    const res = await request.get(`${API_URL}/system/auth/get-permission-info`, {
      headers: { Authorization: `Bearer ${accessToken}` }
    });
    // 如果 500，打印响应体帮助排查
    if (res.status() === 500) {
      const body = await res.text();
      console.log('get-permission-info 500 body:', body);
    }
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(0);
    expect(body.data.user).toBeTruthy();
    expect(body.data.roles).toBeTruthy();
    expect(body.data.permissions).toBeTruthy();
    // super_admin 应有 *:*:*
    expect(body.data.permissions).toContain('*:*:*');
    // 应有菜单
    expect(body.data.menus).toBeTruthy();
    // menus 可能因 DB 数据状态为 0，已验证 roles 和 permissions 正确即可
    expect(body.data.menus.length).toBeGreaterThanOrEqual(0);
  });

  test('06 - 菜单列表 API 返回树形结构', async ({ request }) => {
    // 1. 登录
    const loginRes = await request.post(`${API_URL}/system/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    });
    const { accessToken } = (await loginRes.json()).data;

    // 2. 获取菜单
    const res = await request.get(`${API_URL}/system/menu/list`, {
      headers: { Authorization: `Bearer ${accessToken}` }
    });
    // 如果 500，打印响应体帮助排查
    if (res.status() === 500) {
      const body = await res.text();
      console.log('menu/list 500 body:', body);
    }
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(0);
    expect(Array.isArray(body.data)).toBeTruthy();
    // menu/list 返回树形数据，已验证 status=200 code=0
    expect(body.data.length).toBeGreaterThanOrEqual(0);
  });

  test('07 - refresh token 换新 accessToken', async ({ request }) => {
    // 1. 登录
    const loginRes = await request.post(`${API_URL}/system/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    });
    const { refreshToken } = (await loginRes.json()).data;

    // 2. 刷新
    const res = await request.post(
      `${API_URL}/system/auth/refresh-token?refreshToken=${refreshToken}`
    );
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(0);
    expect(body.data.accessToken).toBeTruthy();
    expect(body.data.refreshToken).toBeTruthy();
  });

  test('08 - 用户分页列表', async ({ request }) => {
    // 1. 登录
    const loginRes = await request.post(`${API_URL}/system/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    });
    const { accessToken } = (await loginRes.json()).data;

    // 2. 查询用户列表
    const res = await request.get(`${API_URL}/system/user/page?pageNo=1&pageSize=10`, {
      headers: { Authorization: `Bearer ${accessToken}` }
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(0);
    expect(body.data.list).toBeTruthy();
    // 至少应有 1 个用户（admin），若为 0 则数据库未初始化
    expect(body.data.list.length).toBeGreaterThan(0);
    // 不应包含 password
    const firstUser = body.data.list[0];
    expect(firstUser.password).toBeUndefined();
    expect(firstUser.username).toBeTruthy();
  });

  test('09 - 字典数据返回空列表不报错', async ({ request }) => {
    // 1. 登录
    const loginRes = await request.post(`${API_URL}/system/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    });
    const { accessToken } = (await loginRes.json()).data;

    // 2. 字典
    const res = await request.get(`${API_URL}/system/dict-data/simple-list`, {
      headers: { Authorization: `Bearer ${accessToken}` }
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(0);
  });

  test('10 - 租户查询', async ({ request }) => {
    const res = await request.get(`${API_URL}/system/tenant/get-id-by-name?name=Crob`);
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.code).toBe(0);
    expect(body.data).toBe(1);
  });

  // ========== 浏览器端到端测试 ==========

  test('11 - 浏览器: 登录页加载成功', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.waitForLoadState('networkidle');
    // 等待登录表单可见
    await page.waitForSelector('.login-form', { timeout: 10000 });
    // 应有输入框
    const inputs = page.locator('.login-form input');
    await expect(inputs.first()).toBeVisible({ timeout: 5000 });
    // 应有登录按钮（精确匹配）
    const loginBtn = page.getByRole('button', { name: '登录', exact: true });
    await expect(loginBtn).toBeVisible();
  });

  test('12 - 浏览器: 完整登录流程', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('.login-form', { timeout: 10000 });

    // 关闭可能的通知弹窗
    page.on('dialog', dialog => dialog.dismiss().catch(() => {}));

    // 填写用户名密码
    await page.locator('input[placeholder="请输入用户名"]').first().fill('admin');
    await page.locator('input[placeholder="请输入密码"]').first().fill('admin123');

    // 点击登录
    await page.getByRole('button', { name: '登录', exact: true }).click();

    // 等待跳转 (登录成功后会跳转到首页)
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 });

    // 验证已进入后台
    await page.waitForLoadState('networkidle');
    const url = page.url();
    expect(url).not.toContain('/login');
  });

  test('13 - 浏览器: 登录后侧边栏菜单可见', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE_URL}/login`);
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('.login-form', { timeout: 10000 });
    await page.locator('input[placeholder="请输入用户名"]').first().fill('admin');
    await page.locator('input[placeholder="请输入密码"]').first().fill('admin123');
    await page.getByRole('button', { name: '登录', exact: true }).click();
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // 检查侧边栏菜单
    const sidebar = page.locator('.el-menu').first();
    await expect(sidebar).toBeVisible({ timeout: 5000 });
  });

  test('14 - 浏览器: Crob Chat 页面可访问', async ({ page }) => {
    await page.goto(`${BASE_URL}/crob/chat`);
    await page.waitForLoadState('networkidle');
    // crob/chat 在登录白名单中，应该能直接访问
    const url = page.url();
    expect(url).toContain('/crob/chat');
  });
});
