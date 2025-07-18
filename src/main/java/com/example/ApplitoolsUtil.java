package com.example;

import com.applitools.eyes.*;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.playwright.Eyes;
import com.applitools.eyes.playwright.visualgrid.VisualGridRunner;
import com.applitools.eyes.visualgrid.BrowserType;
import com.applitools.eyes.visualgrid.model.DeviceName;
import com.applitools.eyes.visualgrid.model.RenderBrowserInfo;
import com.applitools.eyes.visualgrid.model.ScreenOrientation;
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
        VisualGridRunner runner = getVisualGridRunner(testName, isParallelExecution);

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

    private static void printPrettyTestResults(TestResultsSummary summary) {
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

                if (browserInfo != null) {
                    if (browserInfo.getEmulationInfo() != null && browserInfo.getEmulationInfo().getDeviceName() != null) {
                        sb.append("\tDevice        : ").append(browserInfo.getEmulationInfo().getDeviceName())
                                .append(" [").append(browserInfo.getEmulationInfo().getScreenOrientation()).append("]\n");
                    } else {
                        sb.append("\tBrowser       : ").append(browserInfo.getBrowserType().getName())
                                .append(" @ ").append(browserInfo.getPlatform()).append("\n");

                        RectangleSize vp = browserInfo.getViewportSize();
                        if (vp != null) {
                            sb.append("\tViewport Size : ").append(vp.getWidth()).append("x").append(vp.getHeight()).append("\n");
                        } else {
                            sb.append("\tViewport Size : not set\n");
                        }
                    }
                }

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

    private static void printTestResults() {
        TestResultsSummary summary = threadLocalRunner.get().getAllTestResults(false);
        printPrettyTestResults(summary);
    }

    private static VisualGridRunner getVisualGridRunner(String testName, boolean isParallelExecution) {
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
        return runner;
    }

    private static <E extends Enum<E>> E getEnumIgnoreCase(Class<E> enumClass, String value) {
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("No enum constant in " + enumClass.getSimpleName() + " for value: " + value);
    }

    private static Configuration loadConfig(String testName) {
        Yaml yaml = new Yaml();
        try (InputStream in = ApplitoolsUtil.class.getResourceAsStream("/eyes-config.yml")) {
            Map<String, Object> cfg = yaml.load(in);
            Configuration config = new Configuration();
            config.setServerUrl(cfg.get("serverUrl").toString());
            config.setApiKey(getApplitoolsAPIKey(cfg));
            config.setAppName(cfg.get("appName").toString());
            config.setBatch(batch);

            Map<String, Integer> vp = (Map<String, Integer>) cfg.get("viewport");
            config.setViewportSize(new RectangleSize(vp.get("width"), vp.get("height")));

            String level = cfg.get("matchLevel").toString().toUpperCase();
            config.setMatchLevel(MatchLevel.valueOf(level));

            config.setIsDisabled(getIsDisabled(cfg));
            addProxyDetailsInConfiguration(cfg, config);
            addBrowsersAndDevicesInConfiguration(cfg, config);
            printConfiguration(config, testName);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Applitools config", e);
        }
    }

    private static String getApplitoolsAPIKey(Map<String, Object> cfg) {
        String rawKey = cfg.get("apiKey").toString();
        String resolvedKey = rawKey.replace("${APPLITOOLS_API_KEY}", System.getenv("APPLITOOLS_API_KEY"));
        return resolvedKey;
    }

    private static boolean getIsDisabled(Map<String, Object> cfg) {
        String rawIsDisabled = cfg.get("isDisabled").toString(); // expected: ${DISABLE_APPLITOOLS}
        String envValueForDisableApplitools = System.getenv("DISABLE_APPLITOOLS");
        String resolvedIsDisabled = rawIsDisabled.replace("${DISABLE_APPLITOOLS}", envValueForDisableApplitools != null ? envValueForDisableApplitools : "false");
        boolean isDisabled = Boolean.parseBoolean(resolvedIsDisabled);
        return isDisabled;
    }

    private static void addBrowsersAndDevicesInConfiguration(Map<String, Object> cfg, Configuration config) {
        List<Map<String, Object>> browsers = (List<Map<String, Object>>) cfg.get("browsersInfo");
        for (Map<String, Object> b : browsers) {
            if (b.containsKey("deviceName")) {
                DeviceName device = getEnumIgnoreCase(DeviceName.class, (String) b.get("deviceName"));
                ScreenOrientation orientation = getEnumIgnoreCase(ScreenOrientation.class, (String) b.getOrDefault("screenOrientation", "portrait"));
                config.addDeviceEmulation(device, orientation);
            } else {
                int w = (Integer) b.get("width");
                int h = (Integer) b.get("height");
                BrowserType bt = getEnumIgnoreCase(BrowserType.class, (String) b.get("browserType"));
                config.addBrowser(w, h, bt);
            }
        }
    }

    private static void addProxyDetailsInConfiguration(Map<String, Object> cfg, Configuration config) {
        // Load proxy config if present
        Map<String, String> proxyMap = (Map<String, String>) cfg.get("proxy");
        if (proxyMap != null && proxyMap.get("url") != null && !proxyMap.get("url").isEmpty()) {
            String rawUrl = proxyMap.get("url");
            String rawUser = proxyMap.getOrDefault("username", null);
            String rawPass = proxyMap.getOrDefault("password", null);

            String proxyUser = resolveEnv(rawUser);
            String proxyPass = resolveEnv(rawPass);

            ProxySettings proxy;
            if (proxyUser != null && proxyPass != null) {
                proxy = new ProxySettings(rawUrl, proxyUser, proxyPass);
            } else {
                proxy = new ProxySettings(rawUrl);
            }
            config.setProxy(proxy);
        }
    }

    private static String resolveEnv(String value) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String envKey = value.substring(2, value.length() - 1);
            return System.getenv(envKey);
        }
        return value;
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
        sb.append("\tIs Disabled  : ").append(config.getIsDisabled()).append("\n");
        addProxyInfoToMessage(config, sb);
        addBrowserAndDevicesInfoToMessage(config, sb);

        sb.append("==========================================================");
        System.out.println(sb.toString());
    }

    private static void addProxyInfoToMessage(Configuration config, StringBuilder sb) {
        AbstractProxySettings proxy = config.getProxy();
        if (null != proxy) {
            sb.append("\tProxy        : ").append(proxy).append("\n");
        } else {
            sb.append("\tProxy        : Not Set").append("\n");
        }
    }

    private static void addBrowserAndDevicesInfoToMessage(Configuration config, StringBuilder sb) {
        List<RenderBrowserInfo> browsers = config.getBrowsersInfo();
        if (browsers != null && !browsers.isEmpty()) {
            sb.append("\tBrowsers     :\n");
            for (RenderBrowserInfo b : browsers) {
                if (b.getEmulationInfo() != null && b.getEmulationInfo().getDeviceName() != null) {
                    // Mobile device emulation
                    sb.append("\t  - ").append(b.getEmulationInfo().getDeviceName())
                            .append(" [").append(b.getEmulationInfo().getScreenOrientation()).append("]\n");
                } else if (b.getViewportSize() != null) {
                    // Desktop browser with size
                    RectangleSize size = b.getViewportSize();
                    sb.append("\t  - ").append(b.getBrowserType())
                            .append(" (").append(size.getWidth()).append("x").append(size.getHeight()).append(")\n");
                } else {
                    sb.append("\t  - [Unknown browser/device config]\n");
                }
            }
        } else {
            sb.append("\tBrowsers     : [none configured]\n");
        }
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
