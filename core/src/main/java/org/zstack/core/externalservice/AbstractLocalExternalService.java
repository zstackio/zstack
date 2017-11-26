package org.zstack.core.externalservice;

import org.zstack.core.Platform;
import org.zstack.utils.ProcessFinder;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public abstract class AbstractLocalExternalService implements LocalExternalService {
    protected static final CLogger logger = Utils.getLogger(AbstractLocalExternalService.class);

    protected abstract String[] getCommandLineKeywords();

    protected ProcessFinder processFinder = new ProcessFinder();

    public Integer getPID() {
        return processFinder.findByCommandLineKeywords(getCommandLineKeywords());
    }

    public void stop() {
        Integer pid = getPID();
        if (pid != null) {
            Platform.killProcess(pid);
        }

        logger.debug(String.format("[External Service] stopped %s", getName()));
    }

    public void restart() {
        stop();
        start();
    }
}
