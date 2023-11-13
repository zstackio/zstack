package org.zstack.crypto.securitymachine;

import java.nio.charset.StandardCharsets;

import static org.zstack.core.Platform.*;

/**
 * Created by Wenhao.Zhang on 21/12/27
 */
public class AttachVerifyPair {
    private final String originText;
    // the format of certificate text is binary, can not convert to String
    private final byte[] certificateText;

    public static SecurityMachineResponse<AttachVerifyPair> create(byte[] originText, byte[] certificateText) {
        if (originText == null || certificateText == null) {
            return new SecurityMachineResponse<>(operr("originText or certificateText can not be null"));
        }
        return create(new String(originText, StandardCharsets.UTF_8), certificateText);
    }

    public static SecurityMachineResponse<AttachVerifyPair> create(String originText, byte[] certificateText) {
        if (originText == null || certificateText == null) {
            return new SecurityMachineResponse<>(operr("originText or certificateText can not be null"));
        }
        return new SecurityMachineResponse<>(new AttachVerifyPair(originText, certificateText));
    }

    private AttachVerifyPair(String originText, byte[] certificateText) {
        this.originText = originText;
        this.certificateText = certificateText;
    }

    public String getOriginText() {
        return originText;
    }

    public byte[] getCertificateText() {
        return certificateText;
    }
}
