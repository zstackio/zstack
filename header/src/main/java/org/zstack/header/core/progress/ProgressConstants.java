package org.zstack.header.core.progress;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 16/12/10.
 */
public class ProgressConstants {
    public final static String PROGRESS_REPORT_PATH = "/progress/report";
    public final static String PROGRESS_START_PATH = "/progress/start";
    public final static String PROGRESS_FINISH_PATH = "/progress/finish";

    public static final String ACTION_CATEGORY = "progress";
    public static final String SERVICE_ID = "core.progress";

    public enum ProgressType {
        AddImage,
        LocalStorageMigrateVolume,
        CreateRootVolumeTemplateFromRootVolume;

        public static boolean contains(String type) {
            List<String> types = new ArrayList<>();
            for (ProgressType value: ProgressType.values()) {
                types.add(value.name());
            }
            return types.contains(type);
        }
    }
}
