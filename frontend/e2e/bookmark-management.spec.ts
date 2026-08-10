import { expect, test } from "@playwright/test";

test("member toggles a publication bookmark and keeps the state after reload", async ({ page }, testInfo) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("404 (Not Found)")) browserErrors.push(message.text());
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));
  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  await page.goto("/login?next=/posts/new");
  await page.getByLabel("이메일").fill("demo-member-1@townpet.local");
  await page.getByLabel("비밀번호", { exact: true }).fill("townpet-demo-123!");
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/posts\/new$/);
  await page.getByLabel("제목").fill(`TownPet ${device} bookmark 대상 글`);
  await page.getByLabel("본문").fill("bookmark 상태를 검증하는 게시글입니다.");
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+$/);
  const bookmark = page.getByRole("button", { name: /저장/ });
  await expect(bookmark).toHaveAttribute("aria-pressed", "false");
  await bookmark.click();
  await expect(bookmark).toHaveAttribute("aria-pressed", "true");
  await page.reload();
  await expect(page.getByRole("button", { name: /저장/ })).toHaveAttribute("aria-pressed", "true");
  await page.getByRole("button", { name: /저장/ }).click();
  await expect(page.getByRole("button", { name: /저장/ })).toHaveAttribute("aria-pressed", "false");
  await page.reload();
  await expect(page.getByRole("button", { name: /저장/ })).toHaveAttribute("aria-pressed", "false");
  expect(browserErrors).toEqual([]);
});
