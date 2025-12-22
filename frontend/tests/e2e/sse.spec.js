const { test, expect } = require('@playwright/test');

test('TC-FE-AI-001 SSE 事件顺序与错误提示', async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());

  await page.route('**/api/chat/history/list', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          { chatId: 'chat-1', lastMessage: '上次对话', createTime: new Date().toISOString(), updateTime: new Date().toISOString() }
        ],
        message: ''
      })
    });
  });

  await page.route('**/api/chat/history/detail?*', route => {
    const url = new URL(route.request().url());
    const chatId = url.searchParams.get('chatId');
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          chatId,
          messages: [
            { messageType: 'USER', message: '你好', createTime: new Date().toISOString() },
            { messageType: 'ASSISTANT', message: '欢迎回来', createTime: new Date().toISOString() }
          ]
        },
        message: ''
      })
    });
  });

  await page.route('**/ai/keep_app/chat/sse/user?*', route => {
    route.fulfill({
      status: 200,
      headers: { 'Content-Type': 'text/event-stream' },
      body: 'data: 第一段\n\nid: 1\ndata: 第二段\n\nevent: complete\ndata: done\n\n'
    });
  });

  await page.goto('/fitness');
  const input = page.getByPlaceholder('输入您的问题...');
  await input.fill('测试SSE顺序');
  await input.press('Enter');
  await expect(page.locator('.ai-message').last()).toContainText('第一段第二段');

  await page.route('**/ai/keep_app/chat/sse/user?*', route => {
    route.fulfill({
      status: 500,
      headers: { 'Content-Type': 'text/event-stream' },
      body: ''
    });
  });
  await input.fill('测试SSE错误');
  await input.press('Enter');
  await expect(page.locator('.ai-message').last()).toContainText('抱歉，服务器连接出现问题，请稍后再试。');
});
