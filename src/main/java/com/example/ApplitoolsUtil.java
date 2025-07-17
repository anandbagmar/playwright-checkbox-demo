package com.example;

import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.playwright.Eyes;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.playwright.visualgrid.VisualGridRunner;
import com.applitools.eyes.visualgrid.BrowserType;
import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.visualgrid.model.RenderBrowserInfo;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ApplitoolsUtil {

    private static VisualGridRunner runner;

    private static final ThreadLocal<Eyes> threadLocalEyes = new ThreadLocal<>();

    public static Eyes initEyes() {
        if (null == runner) {
            runner = new VisualGridRunner(10);
        }
        Eyes eyes = new Eyes(runner);
        eyes.setConfiguration(loadConfig());
        threadLocalEyes.set(eyes);
        return eyes;
    }

    public static Eyes getEyes() {
        return threadLocalEyes.get();
    }

    public static void closeEyes() {
        Eyes eyes = threadLocalEyes.get();
        if (eyes != null) {
            eyes.closeAsync();
        }
    }

    public static void clearEyes() {
        threadLocalEyes.remove();
    }

    public static void printTestResults() {
        TestResultsSummary summary = runner.getAllTestResults(false);
        System.out.println(summary);
    }

    private static Configuration loadConfig() {
        Yaml yaml = new Yaml();
        try (InputStream in = ApplitoolsUtil.class.getResourceAsStream("/eyes-config.yml")) {
            Map<String, Object> cfg = yaml.load(in);
            Configuration config = new Configuration();
            config.setServerUrl(cfg.get("serverUrl").toString());
            String rawKey = cfg.get("apiKey").toString();
            String resolvedKey = rawKey.replace("${APPLITOOLS_API_KEY}", System.getenv("APPLITOOLS_API_KEY"));
            config.setApiKey(resolvedKey);
            config.setAppName(cfg.get("appName").toString());
            config.setBatch(new BatchInfo(cfg.get("batchName").toString()));

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

            printConfiguration(config);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Applitools config", e);
        }
    }

    private static void printConfiguration(Configuration config) {
        System.out.println("=== Applitools Configuration ===");
        System.out.println("Server URL   : " + config.getServerUrl());
        System.out.println("API Key Set  : " + (config.getApiKey() != null && !config.getApiKey().isEmpty()));
        System.out.println("App Name     : " + config.getAppName());
        System.out.println("Batch Name   : " + (config.getBatch() != null ? config.getBatch().getName() : "N/A"));
        RectangleSize vp = config.getViewportSize();
        System.out.println("Viewport     : " + (vp != null ? vp.getWidth() + "x" + vp.getHeight() : "not set"));
        System.out.println("Match Level  : " + config.getMatchLevel());

        List<RenderBrowserInfo> browsers = config.getBrowsersInfo();
        if (browsers != null && !browsers.isEmpty()) {
            System.out.println("Browsers     :");
            for (RenderBrowserInfo b : browsers) {
                com.applitools.eyes.selenium.BrowserType browserType = b.getBrowserType();
                RectangleSize size = b.getViewportSize();
                System.out.println("  - " + browserType + " (" + size.getWidth() + "x" + size.getHeight() + ")");
            }
        } else {
            System.out.println("Browsers     : [none configured]");
        }

        System.out.println("================================");
    }


    public static void closeRunner() {
        if (runner != null) {
            runner.close();
            runner = null;
        }
    }
}
