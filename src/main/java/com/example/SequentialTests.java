package com.example;

import com.applitools.eyes.playwright.fluent.Target;
import com.microsoft.playwright.*;

public class SequentialTests {

    public static void main(String[] args) {
        Playwright playwright = null;

        try {
            ApplitoolsUtil.startBatch(); // Optional final cleanup

            playwright = Playwright.create();

            testCheckboxNavigation(playwright);
            testContextMenuNavigation(playwright);

        } finally {
            if (playwright != null) {
                playwright.close();
            }
            ApplitoolsUtil.closeRunnerAndBatch(); // Optional final cleanup
        }
    }

    // ---------- Individual Tests ----------

    private static void testCheckboxNavigation(Playwright playwright) {
        Browser browser = launchBrowser(playwright);
        Page page = browser.newPage();

        try {
            ApplitoolsUtil.initEyes(page, "The Internet-Sequential", "Checkbox Navigation Test", false);
            navigateToHomepage(page);
            openCheckboxesPage(page);
            checkFirstCheckbox(page);
            goBackToHomepage(page);
        } finally {
            ApplitoolsUtil.closeEyes(false);
            browser.close();
        }
    }

    private static void testContextMenuNavigation(Playwright playwright) {
        Browser browser = launchBrowser(playwright);
        Page page = browser.newPage();

        try {
            ApplitoolsUtil.initEyes(page, "The Internet-Sequential", "Context Menu Navigation Test", false);
            navigateToHomepage(page);
            openContextMenu(page);
            goBackToHomepage(page);
        } finally {
            ApplitoolsUtil.closeEyes(false);
            browser.close();
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
}
