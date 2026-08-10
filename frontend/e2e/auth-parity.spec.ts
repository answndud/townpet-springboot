import { expect, test } from "@playwright/test";

const INITIAL_PASSWORD = "townpet-demo-123!";

test("Credentials journeys use the real JDBC session and account tokens", async ({ page }, testInfo) => {
  const unexpectedBrowserErrors: string[] = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("401 (Unauthorized)")) {
      unexpectedBrowserErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => unexpectedBrowserErrors.push(error.message));

  const device = testInfo.project.name === "mobile" ? "mobile" : "desktop";
  const memberEmail = `e2e-member-${device}@townpet.local`;
  const verificationEmail = `e2e-verify-${device}@townpet.local`;
  const changedPassword = `Townpet-reset-${device}-2026!`;

  await page.goto("/login");
  await page.getByLabel("이메일").fill(memberEmail);
  await page.getByLabel("비밀번호", { exact: true }).fill(INITIAL_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/profile$/);
  await expect(page.getByRole("heading", { name: new RegExp(`e2e-${device}님의 프로필`) })).toBeVisible();

  await page.getByRole("link", { name: "내 동네 설정" }).click();
  await page.getByLabel("대표 동네").selectOption({ label: "서울 마포구" });
  await page.getByLabel("소개 (선택)").fill(`${device} 실제 세션 온보딩`);
  await page.getByRole("button", { name: "반려동물 추가" }).click();
  await page.getByLabel("이름").fill("봄이");
  await page.getByLabel("종류").selectOption("DOG");
  await page.getByRole("button", { name: "설정 저장" }).click();
  await expect(page.getByText("내 동네와 반려동물 정보가 저장되었습니다.")).toBeVisible();

  await page.getByRole("link", { name: "프로필 보기" }).click();
  await page.getByRole("button", { name: "로그아웃" }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto("/profile");
  await expect(page.getByRole("alert")).toHaveText("로그인이 만료되었습니다.");

  await page.goto("/password/reset");
  await page.getByLabel("이메일").fill(memberEmail);
  await page.getByRole("button", { name: "재설정 메일 요청" }).click();
  await expect(page.getByText(/필요한 경우 재설정 메일을 보냈습니다/)).toBeVisible();
  const resetToken = await capturedToken(page, "PASSWORD_RESET", memberEmail);

  await page.goto(`/password/reset?token=${encodeURIComponent(resetToken)}`);
  await page.getByLabel("새 비밀번호", { exact: true }).fill(changedPassword);
  await page.getByLabel("새 비밀번호 확인").fill(changedPassword);
  await page.getByRole("button", { name: "비밀번호 재설정" }).click();
  await expect(page.getByText("비밀번호가 재설정되었습니다.")).toBeVisible();

  await page.goto("/login");
  await page.getByLabel("이메일").fill(memberEmail);
  await page.getByLabel("비밀번호", { exact: true }).fill(changedPassword);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/profile$/);

  await page.goto("/login");
  await page.getByLabel("이메일").fill(verificationEmail);
  await page.getByLabel("비밀번호", { exact: true }).fill(INITIAL_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page.getByRole("alert")).toContainText("이메일 인증 여부");

  await page.goto(`/verify-email?email=${encodeURIComponent(verificationEmail)}`);
  await page.getByRole("button", { name: "인증 메일 요청" }).click();
  await expect(page.getByText(/필요한 경우 인증 메일을 보냈습니다/)).toBeVisible();
  const verificationToken = await capturedToken(page, "EMAIL_VERIFICATION", verificationEmail);

  await page.goto(`/verify-email?token=${encodeURIComponent(verificationToken)}`);
  await page.getByRole("button", { name: "이메일 인증 완료" }).click();
  await expect(page.getByText("이메일 인증이 완료되었습니다.")).toBeVisible();

  await page.goto("/login");
  await page.getByLabel("이메일").fill(verificationEmail);
  await page.getByLabel("비밀번호", { exact: true }).fill(INITIAL_PASSWORD);
  await page.getByRole("button", { name: "이메일로 로그인" }).click();
  await expect(page).toHaveURL(/\/profile$/);
  expect(unexpectedBrowserErrors).toEqual([]);
});

async function capturedToken(page: import("@playwright/test").Page, purpose: string, recipient: string) {
  const response = await page.request.get(
    `/api/_test/account-tokens?purpose=${purpose}&recipient=${encodeURIComponent(recipient)}`,
  );
  expect(response.ok()).toBeTruthy();
  const body = (await response.json()) as { token: string };
  return body.token;
}
