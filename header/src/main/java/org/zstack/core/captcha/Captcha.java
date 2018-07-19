package org.zstack.core.captcha;

import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;

/**
 * Created by kayo on 2018/7/5.
 */
public interface Captcha {
    int getAttemptsForCurrentResource(String targetResourceIdentity);

    void increaseAttemptCount(String targetResourceIdentity);

    void resetAttemptCount(String targetResourceIdentity);

    CaptchaStruct getCaptcha(String targetResourceIdentity);

    void generateCaptcha(String targetResourceIdentity);

    void refreshCaptcha(String uuid, ReturnValueCompletion<CaptchaStruct> completion);

    CaptchaVO refreshCaptcha(String uuid);

    boolean verifyCaptcha(String uuid, String verifyCode);

    void removeCaptcha(String targetResourceIdentity, NoErrorCompletion completion);
}
