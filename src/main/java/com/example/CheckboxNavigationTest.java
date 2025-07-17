package com.example;

import com.applitools.eyes.playwright.Eyes;
import com.applitools.eyes.playwright.fluent.Target;
import com.microsoft.playwright.*;

public class CheckboxNavigationTest {

  public static void main(String[] args) {
    Playwright playwright = null;
    Browser browser = null;
    try {
      playwright = Playwright.create();
      browser = launchBrowser(playwright);
      Page page = browser.newPage();

      Eyes eyes = ApplitoolsUtil.initEyes();
      eyes.open(page, "My App", "My Test 1");

      navigateToHomepage(page);
      openCheckboxesPage(page);
      checkFirstCheckbox(page);
      goBackToHomepage(page);

    } finally {
      ApplitoolsUtil.closeEyes();
      ApplitoolsUtil.clearEyes(); // Prevent memory leaks

      ApplitoolsUtil.printTestResults();
      if (browser != null) {
        browser.close();
      }
      if (playwright != null) {
        playwright.close();
      }
      ApplitoolsUtil.closeRunner();
    }
  }

  private static Browser launchBrowser(Playwright playwright) {
    return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
  }

  private static void navigateToHomepage(Page page) {
    page.navigate("https://the-internet.herokuapp.com/");
    ApplitoolsUtil.getEyes().check("Home", Target.window().fully());
  }

  private static void openCheckboxesPage(Page page) {
    page.click("text=Checkboxes");
    ApplitoolsUtil.getEyes().check("openCheckboxesPage", Target.window().fully());
  }

  private static void openContextMenu(Page page) {
    page.click("text=Context Menu");
    ApplitoolsUtil.getEyes().check("openContextMenu", Target.window().fully());
  }

  private static void checkFirstCheckbox(Page page) {
    Locator firstCheckbox = page.locator("form#checkboxes input[type='checkbox']").first();
    if (!firstCheckbox.isChecked()) {
      firstCheckbox.check();
    }
    ApplitoolsUtil.getEyes().check("checkFirstCheckbox", Target.window().fully());
  }

  private static void goBackToHomepage(Page page) {
    page.goBack();
    ApplitoolsUtil.getEyes().check("goBackToHomepage", Target.window().fully());
  }

  private static void pauseForVisualConfirmation(Page page) {
    page.waitForTimeout(2000); // 2 seconds
  }
}
