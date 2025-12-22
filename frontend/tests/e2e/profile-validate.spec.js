const { test, expect } = require('@playwright/test');

test('TC-FE-Profile-002 健身数据表单校验不提交', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });

  let called = false;
  await page.route('**/api/fitness/data/add', route => {
    called = true;
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0 }) });
  });

  await page.goto('/login');
  await page.getByPlaceholder('请输入您的账号').fill('user1');
  await page.getByPlaceholder('请输入您的密码').fill('password123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('http://localhost:8080/');

  await page.getByRole('link', { name: '个人中心' }).click();
  await expect(page).toHaveURL('http://localhost:8080/profile');

  await page.getByRole('button', { name: '记录数据' }).click();
  const modal = page.locator('.arco-modal').filter({ hasText: '添加健身数据' });
  await modal.getByRole('button', { name: '确定' }).click();
  await page.waitForTimeout(200);
  expect(called).toBeFalsy();
  await expect(modal).toBeVisible();
});
