package org.zstack.core.aspect;

import org.zstack.core.log.LogSafeGson;
import org.zstack.header.log.HasSensitiveInfo;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;

import java.util.Collections;
import java.util.Set;

/**
 * Created by MaJin on 2019/9/23.
 */
public aspect LogSafeAspect {

    void around(org.zstack.core.thread.ChainTask obj) : target(obj) && execution(void org.zstack.core.thread.ChainTask+.run(..)) {
        if (obj.maskWords.isEmpty()) {
            proceed(obj);
            return;
        }

        try (Utils.MaskWords h = new Utils.MaskWords(obj.maskWords)) {
            proceed(obj);
        }
    }

    Void around(org.zstack.core.thread.Task obj) : target(obj) && execution(Void org.zstack.core.thread.Task+.call()) {
        if (obj.maskWords.isEmpty()) {
            return proceed(obj);
        }

        try (Utils.MaskWords h = new Utils.MaskWords(obj.maskWords)) {
            return proceed(obj);
        }
    }

    Object around(org.zstack.core.thread.Task obj) : target(obj) && execution(Object org.zstack.core.thread.Task+.call()) {
        if (obj.maskWords.isEmpty()) {
            return proceed(obj);
        }

        try (Utils.MaskWords h = new Utils.MaskWords(obj.maskWords)) {
            return proceed(obj);
        }
    }

    void around(org.zstack.header.log.HasSensitiveInfo obj) : args(obj) && execution(void *.handle(HasSensitiveInfo+)) {
        Set<String> maskWords = getValuesToMask(obj);
        if (maskWords.isEmpty()) {
            proceed(obj);
            return;
        }

        try (Utils.MaskWords h = new Utils.MaskWords(maskWords)) {
            proceed(obj);
        }
    }

    private Set<String> getValuesToMask(HasSensitiveInfo msg) {
        try {
            return LogSafeGson.getValuesToMask(msg);
        } catch (Throwable t) {
            return Collections.emptySet();
        }
    }
}
