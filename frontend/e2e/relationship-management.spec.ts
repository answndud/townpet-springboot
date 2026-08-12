import { expect, test } from "@playwright/test";

const PASSWORD = "townpet-demo-123!";

async function login(page: import("@playwright/test").Page, email: string) {
  await page.goto("/login?next=/feed");
  await page.getByLabel("이메일").fill(email);
  await page.getByLabel("비밀번호", { exact: true }).fill(PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect.poll(() => new URL(page.url()).pathname).toBe("/feed");
}

async function loginViaApi(page: import("@playwright/test").Page, email: string) {
  const csrf = await page.request.get("/api/v1/auth/csrf");
  const csrfToken = (await csrf.json()).token as string;
  const result = await page.request.post("/api/v1/auth/sessions", {
    headers: { "X-XSRF-TOKEN": csrfToken },
    data: { email, password: PASSWORD },
  });
  expect(result.ok()).toBeTruthy();
}

test("member follows and blocks a different publication author", async ({ page }, testInfo) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("404 (Not Found)")) browserErrors.push(message.text());
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));
  const browser = page.context().browser();
  if (!browser) throw new Error("Browser is required");
  const targetContext = await browser.newContext({ baseURL: "http://127.0.0.1:4173" });
  const targetPage = await targetContext.newPage();
  await loginViaApi(targetPage, "demo-member-2@townpet.local");
  await targetPage.goto("/posts/new");
  await expect(targetPage).toHaveURL(/\/posts\/new$/);
  await expect(targetPage.getByLabel("제목")).toBeVisible();
  await targetPage.getByLabel("제목").fill(`TownPet ${testInfo.project.name} relationship 대상`);
  await targetPage.getByLabel("본문").fill("관계 정책을 검증하는 게시글입니다.");
  await targetPage.getByRole("button", { name: "등록", exact: true }).click();
  await expect(targetPage).toHaveURL(/\/posts\/[0-9a-f-]+$/);
  const targetUrl = targetPage.url();
  await targetContext.close();

  await login(page, "demo-member-1@townpet.local");
  await page.goto(targetUrl);
  const follow = page.getByRole("button", { name: /팔로우/ });
  const block = page.getByRole("button", { name: /차단/ });
  await expect(follow).toHaveAttribute("aria-pressed", "false");
  await follow.click();
  await expect(follow).toHaveAttribute("aria-pressed", "true");
  await page.reload();
  await expect(page.getByRole("button", { name: /팔로우 취소/ })).toHaveAttribute("aria-pressed", "true");
  await block.click();
  await expect(page.getByRole("button", { name: /차단 해제/ })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByRole("button", { name: /팔로우$/ })).toHaveAttribute("aria-pressed", "false");
  await page.getByRole("button", { name: /차단 해제/ }).click();
  await expect(page.getByRole("button", { name: /^차단$/ })).toHaveAttribute("aria-pressed", "false");
  expect(browserErrors).toEqual([]);
});
