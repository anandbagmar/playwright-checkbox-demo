package com.example;

import com.microsoft.playwright.*;

public class CheckboxNavigationTest {

  public static void main(String[] args) {
    Playwright playwright = null;
    Browser browser = null;

    try {
      playwright = Playwright.create();
      browser = launchBrowser(playwright);
      Page page = browser.newPage();

      navigateToHomepage(page);
      openCheckboxesPage(page);
      pauseForVisualConfirmation(page);
      checkFirstCheckbox(page);
      pauseForVisualConfirmation(page);
      goBackToHomepage(page);
      pauseForVisualConfirmation(page);

    } finally {
      if (browser != null) {
        browser.close();
      }
      if (playwright != null) {
        playwright.close();
      }
    }
  }

  private static Browser launchBrowser(Playwright playwright) {
    return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
  }

  private static void navigateToHomepage(Page page) {
    page.navigate("https://the-internet.herokuapp.com/");
  }

  private static void openCheckboxesPage(Page page) {
    page.click("text=Checkboxes");
  }

  private static void checkFirstCheckbox(Page page) {
    Locator firstCheckbox = page.locator("form#checkboxes input[type='checkbox']").first();
    if (!firstCheckbox.isChecked()) {
      firstCheckbox.check();
    }
  }

  private static void goBackToHomepage(Page page) {
    page.goBack();
  }

  private static void pauseForVisualConfirmation(Page page) {
    page.waitForTimeout(2000); // 2 seconds
  }
}
