const { test, expect } = require('@playwright/test');

test('TC-FE-EX-003 运动记录表单校验不提交', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });

  let called = false;
  await page.route('**/api/fitness/exercise/add', route => {
    called = true;
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0 }) });
  });

  await page.route('**/api/fitness/exercise/my/list/page', route => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 0, data: { records: [], total: 0 } }) });
  });

  await page.goto('/login');
  await page.getByPlaceholder('请输入您的账号').fill('user1');
  await page.getByPlaceholder('请输入您的密码').fill('password123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('http://localhost:8080/');

  await page.getByRole('link', { name: '健身数据', exact: true }).first().click();
  await expect(page).toHaveURL('http://localhost:8080/data');

  await page.getByRole('button', { name: '添加运动记录' }).click();

  const workoutModal = page.locator('.arco-modal').filter({ hasText: '添加运动记录' });
  await workoutModal.getByRole('button', { name: '确定' }).click();

  await page.waitForTimeout(300);
  expect(called).toBeFalsy();
  await expect(workoutModal).toBeVisible();
});

