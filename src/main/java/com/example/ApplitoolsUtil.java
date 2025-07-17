package com.example;

import com.applitools.eyes.*;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.playwright.Eyes;
import com.applitools.eyes.playwright.visualgrid.VisualGridRunner;
import com.applitools.eyes.visualgrid.BrowserType;
import com.applitools.eyes.visualgrid.model.RenderBrowserInfo;
import com.microsoft.playwright.Page;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ApplitoolsUtil {

    private static BatchInfo batch = null;
    private static final ThreadLocal<Eyes> threadLocalEyes = new ThreadLocal<>();
    private static final ThreadLocal<VisualGridRunner> threadLocalRunner = new ThreadLocal<>();

    public static void initEyes(Page page, String appName, String testName, boolean isParallelExecution) {
        System.out.printf("Initialize Eyes for test '%s' ...%n", testName);
        VisualGridRunner runner = threadLocalRunner.get();
        if (null == runner) {
            if (isParallelExecution) {
                System.out.printf("Initializing VisualGrid Runner for executing test: '%s' in Parallel%n", testName);
            } else {
                System.out.println("Initializing VisualGrid Runner for executing tests in Sequence");
            }
            runner = new VisualGridRunner(10);
            runner.setDontCloseBatches(true);
            threadLocalRunner.set(runner);
        } else {
            System.out.println("VisualGrid Runner is already initialized");
        }

        Eyes eyes = new Eyes(runner);
        System.out.printf("Creating Eyes for '%s'%n", testName);
        eyes.setConfiguration(loadConfig(testName));
        threadLocalEyes.set(eyes);
        eyes.open(page, appName, testName);
    }

    public static Eyes getEyes() {
        return threadLocalEyes.get();
    }

    public static void closeEyes(boolean isParallelExecution) {
        Eyes eyes = threadLocalEyes.get();
        if (eyes != null) {
            String testName = eyes.getTestName();
            eyes.closeAsync();
            printTestResults();
            threadLocalEyes.remove();
            if (isParallelExecution) {
                closeRunner(String.format("Closing Visual Grid runner for test: '%s'%n", testName));
            }
        }
    }

    public static void startBatch() {
        if (null == batch) {
            System.out.println("Starting Batch ...");
            batch = new BatchInfo("Playwright-Java tests");
            batch.setNotifyOnCompletion(false);
        }
    }

    public static void closeRunnerAndBatch() {
        closeRunner("Closing Visual Grid runner");
        closeBatch();
    }

    private static void printTestResults() {
        TestResultsSummary summary = threadLocalRunner.get().getAllTestResults(false);
        printPrettyTestResults(summary);
    }

    public static void printPrettyTestResults(TestResultsSummary summary) {
        int passed = 0, unresolved = 0, failed = 0, exceptions = 0;
        int matches = 0, mismatches = 0, missing = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Applitools Test Results Summary ===\n");

        for (TestResultContainer container : summary.getAllResults()) {
            TestResults result = container.getTestResults();
            RenderBrowserInfo browserInfo = container.getBrowserInfo();

            if (container.getException() != null) {
                exceptions++;
                sb.append("\t❌ Exception occurred during test:\n");
                sb.append("\t").append(container.getException().getMessage()).append("\n");
                sb.append("\t----------------------------------------\n");
                continue;
            }

            if (result != null) {
                if (result.isPassed()) {
                    passed++;
                } else if (result.isNew()) {
                    unresolved++;
                } else if (result.isDifferent() || result.isAborted()) {
                    failed++;
                }

                matches += result.getMatches();
                mismatches += result.getMismatches();
                missing += result.getMissing();

                sb.append("\tTest Name     : ").append(result.getName()).append("\n");
                sb.append("\tBrowser       : ").append(browserInfo.getBrowserType().getName())
                        .append(" @ ").append(browserInfo.getPlatform()).append("\n");
                sb.append("\tViewport Size : ").append(browserInfo.getViewportSize().getWidth())
                        .append("x").append(browserInfo.getViewportSize().getHeight()).append("\n");
                sb.append("\tSteps         : ").append(result.getSteps()).append("\n");
                sb.append("\tMatches       : ").append(result.getMatches()).append("\n");
                sb.append("\tMismatches    : ").append(result.getMismatches()).append("\n");
                sb.append("\tMissing       : ").append(result.getMissing()).append("\n");
                sb.append("\tResult URL    : ").append(result.getUrl()).append("\n");
                sb.append("\t-------------------------------------------------------------------------------\n");
            }
        }

        sb.append("Summary:\n");
        sb.append("\tTotal Tests   : ").append(summary.getAllResults().length).append("\n");
        sb.append("\tPassed        : ").append(passed).append("\n");
        sb.append("\tUnresolved    : ").append(unresolved).append("\n");
        sb.append("\tFailed        : ").append(failed).append("\n");
        sb.append("\tExceptions    : ").append(exceptions).append("\n");
        sb.append("\tMatches       : ").append(matches).append("\n");
        sb.append("\tMismatches    : ").append(mismatches).append("\n");
        sb.append("\tMissing       : ").append(missing).append("\n");
        sb.append("==========================================================");

        System.out.println(sb.toString());
    }

    private static Configuration loadConfig(String testName) {
        Yaml yaml = new Yaml();
        try (InputStream in = ApplitoolsUtil.class.getResourceAsStream("/eyes-config.yml")) {
            Map<String, Object> cfg = yaml.load(in);
            Configuration config = new Configuration();
            config.setServerUrl(cfg.get("serverUrl").toString());
            String rawKey = cfg.get("apiKey").toString();
            String resolvedKey = rawKey.replace("${APPLITOOLS_API_KEY}", System.getenv("APPLITOOLS_API_KEY"));
            config.setApiKey(resolvedKey);
            config.setAppName(cfg.get("appName").toString());
            config.setBatch(batch);

            Map<String, Integer> vp = (Map<String, Integer>) cfg.get("viewport");
            config.setViewportSize(new RectangleSize(vp.get("width"), vp.get("height")));

            String level = cfg.get("matchLevel").toString().toUpperCase();
            config.setMatchLevel(MatchLevel.valueOf(level));

            List<Map<String, Object>> browsers = (List<Map<String, Object>>) cfg.get("browsers");
            for (Map<String, Object> b : browsers) {
                config.addBrowser(
                        (int) b.get("width"),
                        (int) b.get("height"),
                        BrowserType.valueOf(b.get("name").toString())
                );
            }

            printConfiguration(config, testName);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Applitools config", e);
        }
    }

    private static void printConfiguration(Configuration config, String testName) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Applitools Configuration for test: '").append(testName).append("' ===\n");
        sb.append("\tServer URL   : ").append(config.getServerUrl()).append("\n");
        sb.append("\tAPI Key Set  : ").append(config.getApiKey() != null && !config.getApiKey().isEmpty()).append("\n");
        sb.append("\tApp Name     : ").append(config.getAppName()).append("\n");
        sb.append("\tBatch Name   : ").append(config.getBatch() != null ? config.getBatch().getName() : "N/A").append("\n");

        RectangleSize vp = config.getViewportSize();
        sb.append("\tViewport     : ").append(vp != null ? vp.getWidth() + "x" + vp.getHeight() : "not set").append("\n");

        sb.append("\tMatch Level  : ").append(config.getMatchLevel()).append("\n");

        List<RenderBrowserInfo> browsers = config.getBrowsersInfo();
        if (browsers != null && !browsers.isEmpty()) {
            sb.append("\tBrowsers     :\n");
            for (RenderBrowserInfo b : browsers) {
                com.applitools.eyes.selenium.BrowserType browserType = b.getBrowserType();
                RectangleSize size = b.getViewportSize();
                sb.append("\t  - ").append(browserType).append(" (").append(size.getWidth()).append("x").append(size.getHeight()).append(")\n");
            }
        } else {
            sb.append("\tBrowsers     : [none configured]\n");
        }

        sb.append("==========================================================");

        System.out.println(sb.toString());
    }

    private static void closeBatch() {
        System.out.println("Closing Batch");
        if (null != batch) {
            batch.setCompleted(true);
            batch = null;
        }
    }

    private static void closeRunner(String message) {
        System.out.println(message);
        VisualGridRunner runner = threadLocalRunner.get();
        if (runner != null) {
            runner.close();
            threadLocalRunner.remove();
        }
    }

}
