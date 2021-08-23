package org.zstack.core.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.apicost.analyzer.service.MsgLogFinder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.*;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.Constants;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.AbstractBeforeSendMessageReplyInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by lining on 2019/7/13.
 */
public class LogManagerImpl implements Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LogManagerImpl.class);

    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        installValidateExtension();
        installBeforeUpdateExtension();
        installUpdateExtension();

        return true;
    }

    private void installValidateExtension() {
        LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                long valueLong = Long.parseLong(value);
                if (valueLong == 0) {
                    throw new GlobalConfigException("the value cant not be 0");
                }

                if (valueLong < -1) {
                    throw new GlobalConfigException("the value is less than -l");
                }

                if (valueLong > Integer.MAX_VALUE) {
                    throw new GlobalConfigException(String.format("the value must be less than %s", Integer.MAX_VALUE));
                }
            }
        });

        LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                long valueLong = Long.parseLong(value);
                if (valueLong == 0) {
                    throw new GlobalConfigException("the value cant not be 0");
                }

                if (valueLong < -1) {
                    throw new GlobalConfigException("the value is less than -l");
                }

                if (valueLong > Integer.MAX_VALUE) {
                    throw new GlobalConfigException(String.format("the value must be less than %s", Integer.MAX_VALUE));
                }
            }
        });
    }

    private void installBeforeUpdateExtension() {
        LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.installBeforeUpdateExtension(new GlobalConfigBeforeUpdateExtensionPoint() {
            @Override
            public void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue) {
                if (isRetentionSizePropertyConfigured()) {
                    if (!newValue.equals(Integer.toString(LogGlobalProperty.LOG_RETENTION_SIZE_GB))) {
                        throw new GlobalConfigException("The log size has been configured in the zstack.properties");
                    }
                }

                try {
                    Log4jXMLModifier log4jXMLModifier = new Log4jXMLModifier();
                    log4jXMLModifier.validateLog4jXML();
                } catch (Throwable t) {
                    logger.error("can't load log4j2.xml", t);
                    throw new GlobalConfigException("can't load log4j xml file, or log4j xml file has error");
                }
            }
        });

        LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.installBeforeUpdateExtension(new GlobalConfigBeforeUpdateExtensionPoint() {
            @Override
            public void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue) {
                try {
                    Log4jXMLModifier log4jXMLModifier = new Log4jXMLModifier();
                    log4jXMLModifier.validateLog4jXML();
                } catch (Throwable t) {
                    logger.error("can't load log4j2.xml", t);
                    throw new GlobalConfigException("can't load log4j xml file, or log4j xml file has error");
                }
            }
        });
    }

    private void installUpdateExtension() {
        LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                try {
                    Log4jXMLModifier log4jXMLModifier = new Log4jXMLModifier();
                    log4jXMLModifier.modifyLogRetentionDays(Long.parseLong(newConfig.value()));
                } catch (Exception e) {
                    logger.error(String.format("setting log retention time[day=%s] failed, modify log4j xml failed", newConfig.value()), e);
                }
            }
        });

        LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                try {
                    Log4jXMLModifier log4jXMLModifier = new Log4jXMLModifier();
                    log4jXMLModifier.modifyLogRetentionSize(Long.parseLong(newConfig.value()));
                } catch (Exception e) {
                    logger.error(String.format("setting log retention size[%s GB] failed, modify log4j xml failed", newConfig.value()), e);
                }
            }
        });
    }

    @AsyncThread
    private void checkAndSyncLog4jXML() {
        int sizeInDB = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.value(Integer.class);
        int dayInDB = LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.value(Integer.class);

        boolean needSync = false;
        try {
            Log4jXMLModifier log4jXMLModifier = new Log4jXMLModifier();

            boolean flag = log4jXMLModifier.isLogRetentionSizeExpected(sizeInDB);
            if (!flag) {
                needSync = true;
                log4jXMLModifier.setLogRetentionSize(sizeInDB);
            }

            flag = log4jXMLModifier.isLogRetentionDaysExpected(dayInDB);
            if (!flag) {
                needSync = true;
                log4jXMLModifier.setLogRetentionDays(dayInDB);
            }

            if (!needSync) {
                return;
            }

            log4jXMLModifier.updateLog4jXMLFile();
        } catch (Exception e) {
            logger.warn("check log4j2.xml failed", e);
        }
    }

    private void syncLogGlobalConfig() {
        if (!isRetentionSizePropertyConfigured()) {
            return;
        }

        LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.updateValue(LogGlobalProperty.LOG_RETENTION_SIZE_GB);
    }

    private boolean isRetentionSizePropertyConfigured() {
        String value = Integer.toString(LogGlobalProperty.LOG_RETENTION_SIZE_GB);
        if (LogConstant.PROPERTY_LOG_RETENTION_SIZE_GB_DEFAULT_VALUE.equals(value)) {
            return false;
        }

        return true;
    }

    @Override
    public void managementNodeReady() {
        syncLogGlobalConfig();
        checkAndSyncLog4jXML();

        bus.installBeforeSendMessageReplyInterceptor(new AbstractBeforeSendMessageReplyInterceptor() {
            @Override
            public void beforeSendMessageReply(Message msg, MessageReply reply) {
                String THREAD_CONTEXT = "thread-context";
                if (!msg.getHeaders().containsKey(THREAD_CONTEXT))
                    return;

                Map<String, String> threadContext = (Map<String, String>) msg.getHeaders().get(THREAD_CONTEXT);
                String id = threadContext.get(Constants.THREAD_CONTEXT_API);
                String taskName = threadContext.get(Constants.THREAD_CONTEXT_TASK_NAME);
                if (id == null) {
                    return;
                }

                // HTTP调用消息是异步消息，不计算时间
                if (msg.getMessageName().endsWith("HttpCallMsg"))
                    return;

                // 写入步骤开始的时间和应答时间：这里存储msgLog，记录apiId，msgId，msgName，startTime，replyTime, wait, status --huaxin
                long startTime = msg.getCreatedTime();
                long endTime = System.currentTimeMillis();
                BigDecimal wait = BigDecimal.valueOf((endTime - startTime) / 1000.0);

                new MsgLogFinder().save(msg.getId(), msg.getMessageName(), id, taskName,
                        startTime, endTime, wait, MsgLogFinder.NOT_UPDATE);
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
