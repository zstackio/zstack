package org.zstack.header.core.external.service;

import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.Optional;

/**
 * @author hanyu.liang
 * @date 2024/10/30 17:07
 */
public class ExporterConstant {
    public static final String SYSTEMD_SERVICE_DIR = "/lib/systemd/system/";
    public static final String LOG_DIR = "/var/log/zstack/";
    public static final String PROCESS_EXPORTER_BIN_PATH = Optional
            .ofNullable(PathUtil.findFileOnClassPath("tools/process_exporter"))
            .map(File::getAbsolutePath)
            .orElse(null);

    public static final String ZS_EXPORTER_BIN_PATH = Optional
            .ofNullable(PathUtil.findFileOnClassPath("tools/zstack_service_exporter"))
            .map(File::getAbsolutePath)
            .orElse(null);

    public static final String PROCESS_EXPORTER_SERVICE_PATH = SYSTEMD_SERVICE_DIR + "process_exporter.service";
    public static final String ZS_EXPORTER_SERVICE_PATH = SYSTEMD_SERVICE_DIR + "zstack_service_exporter.service";
    public static final String PROCESS_EXPORTER_LOG_PATH = LOG_DIR + "process_exporter.log";
    public static final String ZS_EXPORTER_LOG_PATH = LOG_DIR + "zstack_service_exporter.log";
    public static final String PROCESS_EXPORTER_CONFIG_PATH = getProcessExporterYamlPath();
    public static final String ZS_EXPORTER_CONFIG_PATH = Optional
            .ofNullable(PathUtil.findFileOnClassPath("zsExporter/zs_exporter_config.yaml"))
            .map(File::getAbsolutePath)
            .orElse(null);
    public static final String ZS_HOST_EXPORTER_CONFIG_PATH = Optional
            .ofNullable(PathUtil.findFileOnClassPath("zsExporter/zs_host_exporter_config.yaml"))
            .map(File::getAbsolutePath)
            .orElse(null);

    private static String getProcessExporterYamlPath() {
        String arch = System.getProperty("os.arch");
        switch (arch) {
            case "aarch64":
                return Optional
                        .ofNullable(PathUtil.findFileOnClassPath("zsExporter/process_exporter_config_aarch64.yaml"))
                        .map(File::getAbsolutePath)
                        .orElse(null);
            default:
                return Optional
                        .ofNullable(PathUtil.findFileOnClassPath("zsExporter/process_exporter_config.yaml"))
                        .map(File::getAbsolutePath)
                        .orElse(null);
        }
    }

    public static boolean isZSExporterInstalled() {
        return ExporterConstant.ZS_EXPORTER_BIN_PATH != null
                && ExporterConstant.ZS_EXPORTER_CONFIG_PATH != null;
    }

    public static boolean isProcessExporterInstalled() {
        return ExporterConstant.PROCESS_EXPORTER_BIN_PATH != null
                && ExporterConstant.PROCESS_EXPORTER_CONFIG_PATH != null;
    }
}
