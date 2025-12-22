const { test, expect } = require('@playwright/test');

test('TC-FE-EX-001 新增记录与统计渲染', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });

  const records = [];

  await page.route('**/api/fitness/exercise/my/list/page', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          records,
          total: records.length,
          current: 1,
          pageSize: 100
        }
      })
    });
  });

  await page.route('**/api/fitness/exercise/add', async route => {
    const postData = JSON.parse(route.request().postData() || '{}');
    const id = records.length + 1;
    records.unshift({
      id,
      exerciseType: postData.exerciseType,
      duration: postData.duration,
      caloriesBurned: postData.caloriesBurned,
      dateRecorded: postData.dateRecorded || new Date().toISOString().split('T')[0],
      notes: postData.notes || ''
    });
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id } })
    });
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
  await workoutModal.getByPlaceholder('输入当前体重').fill('70');
  await workoutModal.locator('.arco-select').click();
  await page.getByText('跑步', { exact: true }).first().click();
  await workoutModal.getByPlaceholder('输入运动时长').fill('60');
  await workoutModal.getByPlaceholder('输入运动时长').press('Tab');

  await expect(workoutModal.getByPlaceholder('自动计算')).toHaveValue(/560/);

  await workoutModal.getByRole('button', { name: '确定' }).click();

  await expect(page.locator('.workout-item .workout-info h4').first()).toHaveText('跑步');
  await expect(page.locator('.workout-item .workout-info p').first()).toContainText('60分钟 · 消耗560卡路里');

  const stats = page.locator('.workout-stats .stat-card .stat-number');
  await expect(stats.nth(0)).toHaveText(/^[1-9]\d*|0$/);
  await expect(stats.nth(1)).toHaveText(/^[1-9]\d*|0$/);
  await expect(stats.nth(2)).toHaveText(/560/);
  await expect(stats.nth(3)).toHaveText(/60/);
});
