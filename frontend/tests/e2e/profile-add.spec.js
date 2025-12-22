const { test, expect } = require('@playwright/test');

test('TC-FE-Profile-001 添加健身数据体脂自动计算与提交', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });

  let added = false;
  await page.route('**/api/fitness/data/add', route => {
    added = true;
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0 }) });
  });

  await page.route('**/api/fitness/data/my/list/page', route => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: { records: [], total: 0 } }) });
  });
  await page.route('**/api/fitness/data/trends?*', route => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: [] }) });
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

  await modal.getByText('男', { exact: true }).click();
  await modal.getByPlaceholder('输入年龄').fill('25');
  await modal.getByPlaceholder('输入年龄').press('Enter');
  await modal.getByPlaceholder('输入身高').fill('175');
  await modal.getByPlaceholder('输入身高').press('Enter');
  await modal.getByPlaceholder('输入体重').fill('70');
  await modal.getByPlaceholder('输入体重').press('Enter');
  await page.waitForTimeout(200);

  await expect(modal.getByPlaceholder('自动计算')).toHaveValue(/[1-4]\d(\.\d)?/);

  await modal.getByRole('button', { name: '确定' }).click();
  await page.waitForTimeout(200);
  expect(added).toBeTruthy();
});

