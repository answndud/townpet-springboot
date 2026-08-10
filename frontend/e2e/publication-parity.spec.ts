import { expect, test } from "@playwright/test";

const DEMO_EMAIL = "demo-member-1@townpet.local";
const DEMO_PASSWORD = "townpet-demo-123!";

test("member creates a free-board post and its public direct URL survives reload", async ({ page }, testInfo) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error") browserErrors.push(message.text());
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  const title = `TownPet ${device} 산책 이야기`;
  const body = `${device} 화면에서 작성하고 PostgreSQL에 저장한 자유게시판 글입니다.`;

  await page.goto("/login?next=/posts/new");
  await page.getByLabel("이메일").fill(DEMO_EMAIL);
  await page.getByLabel("비밀번호", { exact: true }).fill(DEMO_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/posts\/new$/);
  await expect(page.getByRole("heading", { name: "새 글 작성" })).toBeVisible();

  await page.getByLabel("제목").fill(title);
  await page.getByLabel("본문").fill(body);
  await page.getByRole("button", { name: "등록", exact: true }).click();

  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+$/);
  await expect(page.getByRole("heading", { name: title })).toBeVisible();
  await expect(page.getByText(body)).toBeVisible();
  await expect(page.getByText("전체 공개")).toBeVisible();

  const directUrl = page.url();
  await page.reload();
  await expect(page).toHaveURL(directUrl);
  await expect(page.getByRole("heading", { name: title })).toBeVisible();
  await expect(page.getByText(body)).toBeVisible();
  expect(browserErrors).toEqual([]);
});
