package org.zstack.utils;

/**
 * Created by MaJin on 2017-08-23.
 */
public class ProgressUtils {
    public static String getStartFromStage(String stage){
        return stage.split("-")[0];
    }

    public static String getEndFromStage(String stage){
        return stage.split("-")[1];
    }
}
