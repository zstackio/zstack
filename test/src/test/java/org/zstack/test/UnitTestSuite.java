package org.zstack.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.test.UnitTestSuiteConfig.Import;
import org.zstack.test.UnitTestSuiteConfig.TestCase;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils.ShellRunner;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UnitTestSuite {
    private static CLogger logger = Utils.getLogger(UnitTestSuite.class);

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String REPORT_FOLDER = "unitTestSuiteReport";
    private static final String LOG_FOLDER = "logs";
    private static final String ERR_LOG_FOLDER = "errLogs";
    private static final String SUMMARY = "summary";
    private static final String TIME_SUMMARY = "time";
    private static final String RERUN_FAILURE_CASE = "rerunFailures";
    private static final int DOT_LEN = 50;
    private static int maxCaseNameLen;

    class CaseInfo {
        private Class<?> clazz;
        private volatile boolean success;
        private boolean done;
        private String logFile;
        private long timeout;
        private int index;
        private boolean isFailedByTimeout;
        private int timeCost;

        String caseNameWithIndex() {
            return String.format("%s.%s", index, clazz.getSimpleName());
        }
    }

    private enum Action {
        RUN_CASES,
        LIST_CASES
    }

    private class TestSuite {
        private JAXBContext context;
        private List<UnitTestSuiteConfig> suiteConfigs = new ArrayList<UnitTestSuiteConfig>();
        private List<CaseInfo> testCases = new ArrayList<CaseInfo>();
        private File errLogFolder;
        private Action action;

        private void parseUnitTestSuiteConfig(String configPath) throws JAXBException {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            //UnitTestSuiteConfig config = (UnitTestSuiteConfig) unmarshaller.unmarshal(PathUtil.findFileOnClassPath(configPath, true));
            logger.debug(String.format("parsing unit test suite configuration[%s]", configPath));
            File f = PathUtil.findFileOnClassPath(configPath, true);
            UnitTestSuiteConfig config = (UnitTestSuiteConfig) unmarshaller.unmarshal(f);
            suiteConfigs.add(config);
            for (Import imp : config.getImport()) {
                parseUnitTestSuiteConfig(imp.getResource());
            }
        }

        private void parse() throws JAXBException {
            String configPath = System.getProperty("config");
            if (configPath == null) {
                configPath = "UnitTestSuiteConfig.xml";
            }

            String cases = System.getProperty("cases");
            if (cases != null) {
                configPath = "UnitTestSuiteConfig.xml";
            }


            logger.info(String.format("use configure file: %s", configPath));

            String listCases = System.getProperty("list");
            if (listCases != null) {
                action = Action.LIST_CASES;
            } else {
                action = Action.RUN_CASES;
            }

            context = JAXBContext.newInstance("org.zstack.test");
            parseUnitTestSuiteConfig(configPath);
            parseTestCases();

            if (cases != null) {
                Map<String, CaseInfo> caseMap = new HashMap<String, CaseInfo>();
                for (CaseInfo info : testCases) {
                    caseMap.put(info.clazz.getSimpleName(), info);
                }

                String[] caseNames = cases.split(",");
                DebugUtils.Assert(caseNames.length != 0, String.format("cases cannot be an empty string"));
                List<CaseInfo> casesToRun = new ArrayList<CaseInfo>();
                for (String caseName : caseNames) {
                    CaseInfo info = caseMap.get(caseName);
                    if (info == null) {
                        throw new RuntimeException(String.format("cannot find test case[%s], it's not specified in any XML file contained in UnitTestSuiteConfig.xml", caseName));
                    }
                    casesToRun.add(info);
                }

                testCases = casesToRun;
            }
        }

        private long parseCaseTimeout(TestCase caseConfig, UnitTestSuiteConfig c) {
            if (caseConfig.getTimeout() != null && caseConfig.getTimeout() > 0) {
                return caseConfig.getTimeout();
            }
            if (c.getTimeout() != null && c.getTimeout() > 0) {
                return c.getTimeout();
            }

            return 60;
        }

        private void parseTestCases() {
            for (UnitTestSuiteConfig config : suiteConfigs) {
                for (UnitTestSuiteConfig.TestCase c : config.getTestCase()) {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(c.getClazz());
                    } catch (ClassNotFoundException e) {
                        String err = String.format("Unable to find unit test class[%s], please remove it from UnitTestSuiteConfig.xml or create this unit test case",
                                c.getClazz());
                        logger.warn(err);
                        continue;
                    }

                    String simpleName = clazz.getSimpleName();
                    if (simpleName.length() > maxCaseNameLen) {
                        maxCaseNameLen = simpleName.length();
                    }

                    CaseInfo info = new CaseInfo();
                    info.clazz = clazz;
                    info.timeout = parseCaseTimeout(c, config);
                    info.logFile = PathUtil.join(REPORT_FOLDER, LOG_FOLDER, String.format("%s.log", simpleName));
                    testCases.add(info);

                    if (c.getRepeatTimes() != null && c.getRepeatTimes() > 1) {
                        for (int i = 0; i < c.getRepeatTimes(); i++) {
                            info = new CaseInfo();
                            info.clazz = clazz;
                            info.timeout = parseCaseTimeout(c, config);
                            info.logFile = PathUtil.join(REPORT_FOLDER, LOG_FOLDER, String.format("%s-repeat-%s.log", simpleName, i));
                            testCases.add(info);
                        }
                    }
                }
            }

            maxCaseNameLen += String.valueOf(testCases.size()).length();
        }

        private void prepareLogFolder() throws IOException {
            File reportFolder = new File(REPORT_FOLDER);
            if (reportFolder.exists()) {
                FileUtils.forceDelete(reportFolder);
            }
            File logFolder = new File(PathUtil.join(REPORT_FOLDER, LOG_FOLDER));
            FileUtils.forceMkdir(logFolder);
            errLogFolder = new File(PathUtil.join(REPORT_FOLDER, ERR_LOG_FOLDER));
            FileUtils.forceMkdir(errLogFolder);
        }

        private void runCases() {
            logger.info(String.format("There are total %s test cases to run", testCases.size()));

            int index = 0;
            for (CaseInfo info : testCases) {
                info.index = index++;
                CaseRunner runner = new CaseRunner();
                runner.caseInfo = info;
                try {
                    runner.run();
                } catch (Exception e) {
                    logger.warn(String.format("unable to run test case[%s]", info.clazz.getSimpleName()), e);
                }
            }
        }

        private void mergeErrorCaseLog() throws IOException {
            for (CaseInfo info : testCases) {
                if (!info.done) {
                    continue;
                }

                if (!info.success) {
                    String mvnLog = String.format("target/surefire-reports/%s.txt", info.clazz.getSimpleName());
                    File f = new File(mvnLog);
                    File ourLog = new File(info.logFile);
                    if (f.exists()) {
                        try {
                            String log = FileUtils.readFileToString(f);
                            FileUtils.writeStringToFile(ourLog, log, true);
                        } catch (IOException e) {
                            logger.debug(String.format("Unable to merge Junit error log(%s) to log(%s)", f.getAbsolutePath(), info.logFile), e);
                        }
                    }

                    if (info.isFailedByTimeout) {
                        String err = String.format("\n\n\nTest case didn't complete within %s seconds, terminated by UnitTestSuite", info.timeout);

                        try {
                            FileUtils.writeStringToFile(ourLog, err, true);
                        } catch (IOException e) {
                            logger.debug(String.format("Unable to merge Junit error log(%s) to log(%s)", f.getAbsolutePath(), info.logFile), e);
                        }
                    }

                    FileUtils.copyFileToDirectory(ourLog, errLogFolder);
                }
            }
        }

        private void generateSummary() throws IOException {
            int successCases = 0;
            int failedCases = 0;
            int skipped = 0;
            List<String> failedCaseLogs = new ArrayList<String>();
            List<String> failedCaseNames = new ArrayList<String>();
            for (CaseInfo info : testCases) {
                if (!info.done) {
                    skipped++;
                    continue;
                }

                if (info.success) {
                    successCases++;
                } else {
                    failedCases++;
                    failedCaseLogs.add(info.logFile);
                    failedCaseNames.add(info.clazz.getSimpleName());
                }
            }

            float successRate = successCases;
            successRate = successRate / testCases.size();
            Formatter f = new Formatter();
            if (failedCases > 0) {
                f.format("\n\nFailed cases' log:\n-------------------------------------------------------------------------------");
                for (String log : failedCaseLogs) {
                    File logFile = new File(log);
                    f.format("\n%s", logFile.getAbsolutePath());
                }
            }

            f.format("\n\nTest Summary:\n-------------------------------------------------------------------------------");
            String fmt = "\nTotal cases: %s\tSuccess: %s\tFailed: %s\tSkipped: %s\tPass rate: %s%%";
            f.format(fmt, testCases.size(), successCases, failedCases, skipped, successRate * 100);
            if (failedCases > 0) {
                f.format("\n\nsee error logs in %s", errLogFolder.getAbsolutePath());
            }
            if (!failedCaseNames.isEmpty()) {
                String rerunPath = PathUtil.absPath(PathUtil.join(REPORT_FOLDER, RERUN_FAILURE_CASE));
                String command = String.format("mvn test -Dtest=UnitTestSuite -Dcases=%s", StringUtils.join(failedCaseNames, ","));
                FileUtils.writeStringToFile(new File(rerunPath), command);
                f.format(String.format("\n\nyou can re-run failed cases using command: \n%s", command));
                f.format(String.format("\n\nthe command is stored in %s", rerunPath));
            }
            f.format("\n-------------------------------------------------------------------------------");
            f.format("\n");
            System.out.println(f.toString());
            FileUtils.writeStringToFile(new File(PathUtil.join(REPORT_FOLDER, SUMMARY)), f.toString());
            System.out.flush();
        }

        private void generateReport() throws IOException {
            mergeErrorCaseLog();
            generateTimeSummary();
            generateSummary();
        }


        private void generateTimeSummary() throws IOException {
            List<String> timeCosts = new ArrayList<>();
            for (CaseInfo info : testCases) {
                timeCosts.add(String.format("%s.java %s", info.clazz.getSimpleName(), info.timeCost));
            }

            FileUtils.writeStringToFile(
                    new File(PathUtil.join(REPORT_FOLDER, TIME_SUMMARY)),
                    StringUtils.join(timeCosts, "\n")
            );
        }

        void run() throws Exception {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        generateReport();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }));

            parse();

            if (action == Action.LIST_CASES) {
                listCases();
            } else if (action == Action.RUN_CASES) {
                prepareLogFolder();
                runCases();
            }
        }

        private void listCases() throws IOException {
            List<String> caseNames = testCases.stream().map(info -> info.clazz.getSimpleName()).collect(Collectors.toList());
            String listCases = System.getProperty("list").trim();
            if (listCases.isEmpty()) {
                System.out.println(StringUtils.join(caseNames, "\n"));
            } else {
                FileUtils.writeStringToFile(new File(listCases), StringUtils.join(caseNames, "\n"));
            }
        }
    }

    private String colorResult(boolean ret) {
        return ret ? String.format(ANSI_GREEN + "Success" + ANSI_RESET) : String.format(ANSI_RED + "Failure" + ANSI_RESET);
    }

    private class CaseRunner {
        CaseInfo caseInfo;
        ShellRunner shellRunner;
        Thread timeoutMonitor;
        Thread progressBar;

        void run() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
            if (caseInfo.clazz.isAnnotationPresent(Deprecated.class)) {
                System.out.print(String.format("Skip deprecated case: %s", caseInfo.clazz.getSimpleName()));
                return;
            }

            startProgressBar();
            startTimeoutMonitor();
            shellRunner = new ShellRunner();
            shellRunner.setCommand(String.format("mvn test -Dtest=%s", caseInfo.clazz.getSimpleName()));
            shellRunner.setStdoutFile(caseInfo.logFile);
            shellRunner.setSuppressTraceLog(true);
            shellRunner.setBaseDir(System.getProperty("user.dir"));
            shellRunner.setWithSudo(false);
            ShellResult result = shellRunner.run();
            caseInfo.success = result.isReturnCode(0);
            caseInfo.done = true;
            cleanup();
        }

        private void cleanup() throws InterruptedException {
            TimeUnit.SECONDS.timedJoin(progressBar, 5);
            timeoutMonitor.interrupt();
            Integer pid = shellRunner.obtainUnixPid();
            if (pid != null) {
                ShellRunner cmd = new ShellRunner();
                cmd.setCommand(String.format("kill -9 %s", pid));
                cmd.setSuppressTraceLog(true);
                cmd.run();
            }
        }

        private void startProgressBar() {
            progressBar = new Thread(new Runnable() {
                int dotLen = 1;
                int seconds = 0;

                private void cleanBar(int lastSize) {
                    String fmt = "\r%-" + lastSize + "s";
                    String str = String.format(fmt, "");
                    System.out.print(str);
                }

                private String formatSeconds() {
                    return String.format("%02d:%02d", (seconds % 3600) / 60, (seconds % 60));
                }

                private int printBar() {
                    String fmt = String.format("\r%%-%ss%s%s [ %s ]", maxCaseNameLen, StringUtils.repeat(".", dotLen),
                            StringUtils.repeat(" ", DOT_LEN - dotLen), formatSeconds());
                    String str = String.format(fmt, caseInfo.caseNameWithIndex());
                    System.out.print(str);
                    return str.length();
                }

                @Override
                public void run() {
                    try {
                        int lastSize = 1;
                        cleanBar(lastSize);
                        while (!caseInfo.done) {
                            cleanBar(lastSize);
                            dotLen++;
                            lastSize = printBar();
                            dotLen = dotLen >= 50 ? 1 : dotLen;
                            try {
                                Thread.sleep(1000);
                                seconds++;
                            } catch (InterruptedException e) {
                                logger.debug("", e);
                                break;
                            }
                        }
                        String fmt = "\r%-" + maxCaseNameLen + "s" + StringUtils.repeat(".", DOT_LEN) + String.format(" [ %%-7s %s ]", formatSeconds());
                        System.out.println(String.format(fmt, caseInfo.caseNameWithIndex(), colorResult(caseInfo.success)));
                        caseInfo.timeCost = seconds;
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            });

            progressBar.start();
        }

        private void startTimeoutMonitor() {
            timeoutMonitor = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        try {
                            this.wait(TimeUnit.SECONDS.toMillis(caseInfo.timeout));
                            shellRunner.terminate();
                            caseInfo.isFailedByTimeout = true;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            timeoutMonitor.start();
        }
    }


    @Test
    public void test() throws Exception {
        new TestSuite().run();
    }
}
