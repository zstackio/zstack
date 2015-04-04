package org.zstack.core.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class LogFacadeImpl implements LogFacade, Component {
    private LogBackend backend;
    private Map<String, LogBackend> backends = new HashMap<String, LogBackend>();
    private volatile boolean isEnabled;

    @Autowired
    private PluginRegistry pluginRgty;


    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    private void write(String resUuid, LogType type, LogLevel level, String content) {
        if (!isEnabled) {
            return;
        }

        LogVO vo = new LogVO();
        vo.setResourceUuid(resUuid);
        vo.setType(type);
        vo.setLevel(level);
        vo.setContent(content);
        backend.write(vo);
    }

    @Override
    public void info(String resourceUuid, String info) {
        write(resourceUuid, LogType.Text, LogLevel.Info, info);
    }

    @Override
    public void warn(String resourceUuid, String info) {
        write(resourceUuid, LogType.Text, LogLevel.Warn, info);
    }

    @Override
    public void error(String resourceUuid, String info) {
        write(resourceUuid, LogType.Text, LogLevel.Error, info);
    }

    @Override
    public boolean start() {
        for (LogBackend bkd : pluginRgty.getExtensionList(LogBackend.class)) {
            LogBackend old = backends.get(bkd.getLogBackendType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate LogBackend[%s, %s] for type[%s]", old.getClass().getName(), bkd.getClass().getName(), bkd.getLogBackendType()));
            }

            backends.put(bkd.getLogBackendType(), bkd);
        }

        backend = backends.get(LogGlobalProperty.LOG_FACADE_BACKEND_TYPE);
        if (backend == null) {
            throw new CloudRuntimeException(String.format("cannot find LogBackend that has type[%s]", LogGlobalProperty.LOG_FACADE_BACKEND_TYPE));
        }

        isEnabled = LogGlobalConfig.ENABLED.value(boolean.class);
        if (isEnabled) {
            backend.start();
        }

        LogGlobalConfig.ENABLED.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                isEnabled = newConfig.value(boolean.class);
                if (newConfig.value(boolean.class)) {
                    backend.start();
                } else {
                    backend.stop();
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        backend.stop();
        return true;
    }
}
