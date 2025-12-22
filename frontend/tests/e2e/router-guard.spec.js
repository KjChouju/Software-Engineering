const { test, expect } = require('@playwright/test');

test('TC-FE-Auth-002 未登录访问受限重定向到登录', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());
  await page.goto('/profile');
  await expect(page).toHaveURL(/http:\/\/localhost:8080\/login(\?.*)?$/);
  await expect(page.getByRole('button', { name: '登录' })).toBeVisible();
});

test('TC-FE-Router-001 登录后可访问受限页面', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());
  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });
  await page.goto('/login');
  await page.getByPlaceholder('请输入您的账号').fill('user1');
  await page.getByPlaceholder('请输入您的密码').fill('password123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('http://localhost:8080/');
  await page.getByRole('link', { name: '健身排行榜' }).click();
  await expect(page).toHaveURL('http://localhost:8080/ranking');
  await expect(page.getByText('健身排行榜')).toBeVisible();
});
