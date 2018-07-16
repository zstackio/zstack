package org.zstack.header.core.captcha;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Created by kayo on 2018/7/5.
 */
public interface Captcha {
    int getAttemptsForCurrentResource(String targetResourceIdentity);

    void increaseAttemptCount(String targetResourceIdentity);

    void resetAttemptCount(String targetResourceIdentity);

    void generateCaptcha(String targetResourceIdentity, ReturnValueCompletion<CaptchaStruct> completion);

    void refreshCaptcha(String uuid, ReturnValueCompletion<CaptchaStruct> completion);

    CaptchaVO refreshCaptcha(String uuid);

    boolean verifyCaptcha(String uuid, String verifyCode);
}
