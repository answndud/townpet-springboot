import { expect, test } from "@playwright/test";

const DEMO_EMAIL = "demo-member-1@townpet.local";
const DEMO_PASSWORD = "townpet-demo-123!";

test("member creates, reloads, and deletes a publication comment", async ({ page }, testInfo) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("404 (Not Found)")) {
      browserErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  const title = `TownPet ${device} 댓글 대상 글`;
  const comment = `TownPet ${device} PostgreSQL 댓글입니다.`;

  await page.goto("/login?next=/posts/new");
  await page.getByLabel("이메일").fill(DEMO_EMAIL);
  await page.getByLabel("비밀번호", { exact: true }).fill(DEMO_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/posts\/new$/);
  await page.getByLabel("제목").fill(title);
  await page.getByLabel("본문").fill("댓글 여정을 확인하는 게시글입니다.");
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+$/);

  await page.getByRole("textbox", { name: "댓글" }).fill(comment);
  const submitComment = page.getByRole("button", { name: "댓글 등록" });
  await expect(submitComment).toBeEnabled();
  const createCommentResponse = page.waitForResponse(
    (response) => response.url().includes("/comments") && response.request().method() === "POST",
  );
  await submitComment.click();
  expect((await createCommentResponse).status()).toBe(201);
  await expect(page.getByText(comment)).toBeVisible();
  await page.reload();
  await expect(page.getByText(comment)).toBeVisible();

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "삭제", exact: true }).last().click();
  await expect(page.getByText(comment)).toHaveCount(0);
  expect(browserErrors).toEqual([]);
});
