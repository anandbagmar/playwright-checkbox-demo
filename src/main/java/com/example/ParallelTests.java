package com.example;

import com.applitools.eyes.playwright.fluent.Target;
import com.microsoft.playwright.*;

import java.util.concurrent.*;

public class ParallelTests {

    private static final String appName = "The Internet-Parallel";

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(2); // Run 2 tests in parallel

        try {
            ApplitoolsUtil.startBatch();

            Callable<Void> checkboxTask = () -> {
                try (Playwright playwright = Playwright.create()) {
                    testCheckboxNavigation(playwright);
                }
                return null;
            };

            Callable<Void> contextMenuTask = () -> {
                try (Playwright playwright = Playwright.create()) {
                    testContextMenuNavigation(playwright);
                }
                return null;
            };

            Future<Void> future1 = executor.submit(checkboxTask);
            Future<Void> future2 = executor.submit(contextMenuTask);

            try {
                future1.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.err.println("Checkbox test timed out");
            }

            try {
                future2.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.err.println("Context menu test timed out");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
            ApplitoolsUtil.closeRunnerAndBatch(); // Collect results from all threads
        }
    }

    // ---------- Individual Tests ----------

    private static void testCheckboxNavigation(Playwright playwright) {
        Browser browser = launchBrowser(playwright);
        Page page = browser.newPage();

        try {
            ApplitoolsUtil.initEyes(page, appName, "Checkbox Navigation Test", true);
            navigateToHomepage(page);
            openCheckboxesPage(page);
            checkFirstCheckbox(page);
            goBackToHomepage(page);
        } finally {
            ApplitoolsUtil.closeEyes(true);
            browser.close();
        }
    }

    private static void testContextMenuNavigation(Playwright playwright) {
        Browser browser = launchBrowser(playwright);
        Page page = browser.newPage();

        try {
            ApplitoolsUtil.initEyes(page, appName, "Context Menu Navigation Test", true);
            navigateToHomepage(page);
            openContextMenu(page);
            goBackToHomepage(page);
        } finally {
            ApplitoolsUtil.closeEyes(true);
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
        ApplitoolsUtil.getEyes().check("openCheckboxesPage-region", Target.region("text=Checkboxes"));
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
