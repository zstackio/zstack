package org.zstack.header.zql;

import java.util.List;

/**
 * BeforeCallZWatchExtensionPoint is an extension point that allows plugins
 * to perform custom operations before calling zwatch.
 */
public interface BeforeCallZWatchExtensionPoint {
    /**
     * Check if this extension supports the given VO class
     * @param voClass the VO class to check
     * @return true if this extension supports the VO class, false otherwise
     */
    boolean supports(Class<?> voClass);

    /**
     * Perform custom operations before calling ZWatch, for example: health-check
     * @param voClass the VO class type
     * @param uuids the list of resource UUIDs to process
     */
    void beforeCallZWatch(Class<?> voClass, List<String> uuids);
}
