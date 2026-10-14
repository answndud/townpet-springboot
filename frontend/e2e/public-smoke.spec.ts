import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/v1/members/me", route => route.fulfill({status:401,contentType:"application/json",body:"{}"}));
  await page.route("**/api/v1/discovery/**", route => route.fulfill({contentType:"application/json",body:JSON.stringify({items:[],page:{hasNext:false,nextCursor:null}})}));
});

test("navigation supports hover, keyboard and separate mobile account actions", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("link",{name:"TownPet 홈으로 이동",exact:true})).toHaveText("TownPet");
  for (const label of ["동물 게시판", "공통게시판"]) {
    const trigger = page.getByRole("button",{name:label,exact:true});
    const menu = page.getByRole("menu",{name:label + " 바로가기",exact:true});
    await trigger.hover();
    await expect(trigger).toHaveAttribute("aria-expanded","true");
    const bounds = await trigger.boundingBox();
    await page.mouse.move(bounds!.x + bounds!.width / 2, bounds!.y + bounds!.height + 3);
    await expect(menu).toBeVisible();
    await menu.getByRole("menuitem").first().hover();
    await expect(menu).toBeVisible();
    await page.mouse.move(0,0);
    await expect(trigger).toHaveAttribute("aria-expanded","false");
    await expect(menu).toBeHidden();
    await trigger.press("ArrowDown");
    await expect(menu.getByRole("menuitem").first()).toBeFocused();
    await menu.getByRole("menuitem").first().press("Escape");
    await expect(trigger).toBeFocused();
    await expect(menu).toBeHidden();
  }
  await page.setViewportSize({width:390,height:844});
  await page.route("**/api/v1/local-resources*", route => route.fulfill({contentType:"application/json",body:"[]"}));
  await page.goto("/");
  await page.getByRole("button",{name:"메뉴",exact:true}).click();
  const navigation = page.getByRole("navigation",{name:"모바일 주요 이동"});
  await expect(navigation.getByRole("link",{name:"로그인",exact:true})).toHaveCount(0);
  await expect(navigation.getByRole("link",{name:"내 프로필",exact:true})).toHaveCount(0);
  await expect(page.getByTestId("header-login-link-home")).toBeVisible();
  await navigation.getByRole("link",{name:"지역 가이드",exact:true}).click();
  await expect(page.getByRole("heading",{name:"우리 동네 반려생활 가이드"})).toBeVisible();
  await expect(page.getByRole("button",{name:"메뉴",exact:true})).toHaveAttribute("aria-expanded","false");
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBeTruthy();
});

test("anonymous member-write entry redirects to login with its return path", async ({ page }) => {
  await page.goto("/marketplace/new");
  await expect(page.getByRole("heading",{name:"로그인",exact:true})).toBeVisible();
  expect(new URL(page.url()).searchParams.get("next")).toBe("/marketplace/new");
});
