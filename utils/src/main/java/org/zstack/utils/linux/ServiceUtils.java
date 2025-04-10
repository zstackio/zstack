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
     * @param scriptPath 执行脚本路径
     * @param scriptContent 执行脚本内容
     */
    public static void installService(String serviceName, String description,
                                         String documentation, List<String> dependencies,
                                         String scriptPath, String scriptContent) {
        try {
            // 生成服务单元文件内容
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
            serviceContent.append("ExecStart=").append(scriptPath).append("\n");

            serviceContent.append("\n[Install]\n");
            serviceContent.append("WantedBy=multi-user.target");

            // 服务文件路径
            String tmpServicePath = "/tmp/" + serviceName + ".service";
            String servicePath = "/etc/systemd/system/" + serviceName + ".service";

            // 使用Bash执行命令
            new Bash() {
                @Override
                protected void scripts() {
                    // 写入脚本文件
                    run("echo '%s' > %s", scriptContent, scriptPath);
                    // 写入服务文件
                    run("echo '%s' > %s; sudo cp %s %s", serviceContent, tmpServicePath, tmpServicePath, servicePath);
                    // 设置脚本执行权限
                    run("sudo chmod +x %s",scriptPath);
                    // 启用服务并重新加载
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
