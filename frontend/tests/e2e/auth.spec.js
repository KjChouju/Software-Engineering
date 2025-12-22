const { test, expect } = require('@playwright/test');

test('TC-FE-Auth-001 登录表单校验与成功', async ({ page }) => {
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
  await expect(page.getByRole('link', { name: '个人中心' })).toBeVisible();
});

