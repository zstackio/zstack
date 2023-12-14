package org.zstack.header.securitymachine.api.securitymachine;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by LiangHanYu on 2021/11/16 10:57
 */
@RestResponse(fieldsTo = {"all"})
public class APISecurityMachineEncryptEvent extends APIEvent {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public APISecurityMachineEncryptEvent() {

    }

    public APISecurityMachineEncryptEvent(String apiId) {
        super(apiId);
    }

    public static APISecurityMachineEncryptEvent __example__() {
        APISecurityMachineEncryptEvent event = new APISecurityMachineEncryptEvent();
        event.setText("hacks45thai42halo342jato3m2id3j2idol32iced32io");
        return event;
    }
}
