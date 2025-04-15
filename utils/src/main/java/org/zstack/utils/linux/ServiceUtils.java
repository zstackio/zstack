package org.zstack.utils.linux;

import org.zstack.utils.Bash;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.List;

public class ServiceUtils {
    private static final CLogger logger = CLoggerImpl.getLogger(ServiceUtils.class);

    /**
     * 创建并安装Linux系统服务
     *
     * @param serviceName 服务名称
     * @param description 服务描述
     * @param documentation 文档URL
     * @param dependencies 依赖的其他服务
     * @param execStart 执行命令
     */
    public static void installService(String serviceName, String description,
                                         String documentation, List<String> dependencies,
                                         String execStart) {
        try {
            StringBuilder serviceContent = new StringBuilder("[Unit]\n");
            serviceContent.append("Description=").append(description).append("\n");

            if (documentation != null && !documentation.isEmpty()) {
                serviceContent.append("Documentation=").append(documentation).append("\n");
            }

            if (dependencies != null && !dependencies.isEmpty()) {
                serviceContent.append("After=").append(String.join(" ", dependencies)).append("\n");
            }

            serviceContent.append("\n[Service]\n");
            serviceContent.append("Type=forking\n");
            serviceContent.append("ExecStart=").append(execStart).append("\n");

            serviceContent.append("\n[Install]\n");
            serviceContent.append("WantedBy=multi-user.target");

            String tmpServicePath = "/tmp/" + serviceName + ".service";
            String servicePath = "/etc/systemd/system/" + serviceName + ".service";

            new Bash() {
                @Override
                protected void scripts() {
                    run("echo '%s' > %s; sudo cp %s %s", serviceContent, tmpServicePath, tmpServicePath, servicePath);
                    run("sudo systemctl enable %s; sudo systemctl daemon-reload", serviceName);
                }
            }.execute();

        } catch (Exception e) {
            logger.debug("Failed to install service: " + e.getMessage(), e);
        }
    }

    /**
     * 启动服务
     */
    public static void startService(String serviceName) {
        new Bash() {
            @Override
            protected void scripts() {
                setE();

                run("sudo systemctl start %s", serviceName);
            }
        }.execute();
    }

    /**
     * 停止服务
     */
    public static void stopService(String serviceName) {
        new Bash() {
            @Override
            protected void scripts() {
                setE();

                run("sudo systemctl stop %s", serviceName);
            }
        }.execute();
    }

    /**
     * 卸载服务
     */
    public static void uninstallService(String serviceName, String scriptPath) {
        new Bash() {
            @Override
            protected void scripts() {
                setE();

                run("sudo systemctl disable %s" , serviceName);
                run("sudo rm /etc/systemd/system/%s.service", serviceName);
                run("sudo rm %s" + scriptPath, scriptPath);
                run("sudo systemctl daemon-reload");
            }
        }.execute();
    }
}
