const { test, expect } = require('@playwright/test');

test('TC-FE-FD-001 健身数据分页与排序视图一致', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());
  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });

  await page.route('**/api/fitness/data/my/list/page', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          records: [
            { id: 3, weight: 82.0, bodyFat: 18.5, bmi: 24.0, height: 175, dateRecorded: '2025-12-17' },
            { id: 2, weight: 83.1, bodyFat: 18.8, bmi: 24.3, height: 175, dateRecorded: '2025-12-16' },
            { id: 1, weight: 84.0, bodyFat: 19.0, bmi: 24.5, height: 175, dateRecorded: '2025-12-15' }
          ],
          total: 3,
          current: 1,
          pageSize: 10
        }
      })
    });
  });

  await page.route('**/api/fitness/exercise/my/list/page', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { records: [], total: 0 } })
    });
  });

  await page.goto('/login');
  await page.getByPlaceholder('请输入您的账号').fill('user1');
  await page.getByPlaceholder('请输入您的密码').fill('password123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('http://localhost:8080/');

  await page.getByRole('link', { name: '健身数据', exact: true }).first().click();
  await expect(page).toHaveURL('http://localhost:8080/data');
  const weightValue = page.locator('.overview-card').first().locator('.value');
  await expect(weightValue).toContainText('82');
});
