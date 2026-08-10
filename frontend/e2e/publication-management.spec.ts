import { expect, test } from "@playwright/test";

const DEMO_EMAIL = "demo-member-1@townpet.local";
const DEMO_PASSWORD = "townpet-demo-123!";

test("author edits and lifecycle-deletes a free-board post", async ({ page }, testInfo) => {
  const browserErrors: string[] = [];
  let expectingDeletedDetail = false;
  page.on("console", (message) => {
    if (
      message.type() === "error" &&
      !(expectingDeletedDetail && message.text().includes("404 (Not Found)"))
    ) {
      browserErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  const originalTitle = `TownPet ${device} 수정 전 글`;
  const editedTitle = `TownPet ${device} 수정 완료 글`;

  await page.goto("/login?next=/posts/new");
  await page.getByLabel("이메일").fill(DEMO_EMAIL);
  await page.getByLabel("비밀번호", { exact: true }).fill(DEMO_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/posts\/new$/);

  await page.getByLabel("제목").fill(originalTitle);
  await page.getByLabel("본문").fill("수정 전 게시글 본문입니다.");
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page.getByRole("heading", { name: originalTitle })).toBeVisible();

  const detailUrl = page.url();
  await page.getByRole("link", { name: "수정", exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+\/edit$/);
  await page.getByLabel("제목").fill(editedTitle);
  await page.getByLabel("본문").fill("낙관적 버전 검사 후 저장한 본문입니다.");
  await page.getByRole("button", { name: "변경 사항 저장" }).click();
  await expect(page).toHaveURL(detailUrl);
  await expect(page.getByRole("heading", { name: editedTitle })).toBeVisible();
  await expect(page.getByText("낙관적 버전 검사 후 저장한 본문입니다.")).toBeVisible();

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "삭제", exact: true }).click();
  await expect(page).toHaveURL(/\/feed$/);
  await expect(page.getByRole("heading", { name: editedTitle })).toHaveCount(0);

  expectingDeletedDetail = true;
  await page.goto(detailUrl);
  await expect(page.getByRole("alert")).toHaveText("존재하지 않거나 삭제된 게시글입니다.");
  expect(browserErrors).toEqual([]);
});
