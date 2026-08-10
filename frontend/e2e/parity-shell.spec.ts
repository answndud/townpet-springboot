import { expect, test } from "@playwright/test";

test.describe("TownPet shell parity", () => {
  test("home preserves primary acquisition journeys", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle("TownPet | 우리 동네 반려생활 정보");
    await expect(page.getByRole("heading", { name: "우리 동네 반려생활 정보" })).toBeVisible();
    await expect(page.getByRole("link", { name: "전체 피드" })).toHaveAttribute("href", "/feed/guest");
    await expect(page.getByTestId("header-login-link-home")).toHaveAttribute("href", "/login");
    await expect(page.locator("img[alt=TownPet]")).toBeVisible();
  });

  test("mobile shell keeps the topic row usable", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "관심 주제" })).toBeVisible();
    await expect(page.getByRole("link", { name: "분실/목격" })).toBeVisible();
  });
});
