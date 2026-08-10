import { expect, test } from "@playwright/test";

const DEMO_EMAIL = "demo-member-1@townpet.local";
const DEMO_PASSWORD = "townpet-demo-123!";

test("member toggles a publication reaction and keeps the state after reload", async ({ page }, testInfo) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("404 (Not Found)")) {
      browserErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));

  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  const title = `TownPet ${device} reaction 대상 글`;

  await page.goto("/login?next=/posts/new");
  await page.getByLabel("이메일").fill(DEMO_EMAIL);
  await page.getByLabel("비밀번호", { exact: true }).fill(DEMO_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/posts\/new$/);
  await page.getByLabel("제목").fill(title);
  await page.getByLabel("본문").fill("reaction 상태를 검증하는 게시글입니다.");
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page).toHaveURL(/\/posts\/[0-9a-f-]+$/);

  const reaction = page.getByRole("button", { name: /좋아요/ });
  await expect(reaction).toHaveAttribute("aria-pressed", "false");
  await reaction.click();
  await expect(reaction).toHaveAttribute("aria-pressed", "true");
  await expect(reaction).toContainText("좋아요 1");
  await page.reload();
  await expect(page.getByRole("button", { name: /좋아요/ })).toHaveAttribute("aria-pressed", "true");

  await page.getByRole("button", { name: /좋아요/ }).click();
  await expect(page.getByRole("button", { name: /좋아요/ })).toHaveAttribute("aria-pressed", "false");
  expect(browserErrors).toEqual([]);
});
