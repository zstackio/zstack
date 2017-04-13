package org.zstack.core.notification;

/**
 * Created by xing5 on 2017/3/15.
 */
public enum NotificationType {
    Info("Info"),
    Warning("Warn"),
    Error("Error");

    public String methodName;

    NotificationType(String methodName) {
        this.methodName = methodName;
    }
}
