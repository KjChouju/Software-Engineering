const { test, expect } = require('@playwright/test');

test('TC-FE-BMI-001 BMI 计算渲染边界', async ({ page }) => {

  // 登录
  await page.route('**/api/user/login', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, data: { id: 1, userName: '测试用户', userRole: 'user' } })
    });
  });
  await page.goto('/login');
  await page.evaluate(() => localStorage.clear());
  await page.getByPlaceholder('请输入您的账号').fill('user1');
  await page.getByPlaceholder('请输入您的密码').fill('password123');
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL('http://localhost:8080/');

  // 打开健身数据页
  await page.getByRole('link', { name: '健身数据', exact: true }).first().click();
  await expect(page).toHaveURL('http://localhost:8080/data');

  // 打开“添加健身数据”弹窗
  await page.getByRole('button', { name: '添加健身数据' }).click();

  // 拦截 BMI 计算接口，返回边界分类与建议
  await page.route('**/api/fitness/bmi/calculate', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: { bmi: 28.0, category: '偏胖', idealWeightMin: 65, idealWeightMax: 72, healthAdvice: '建议控制体重，增加有氧运动' }
      })
    });
  });

  await page.getByPlaceholder('输入身高').fill('170');
  await page.getByPlaceholder('输入体重').fill('80');
  await page.keyboard.press('Tab');
  await page.waitForSelector('.bmi-result-card .bmi-value .value', { timeout: 5000 });

  // 断言 BMI 结果卡片渲染与建议可见
  await expect(page.locator('.bmi-result-card .bmi-value .value')).toContainText('28');
  await expect(page.locator('.bmi-result-card .health-advice .advice')).toContainText('建议控制体重');
});
