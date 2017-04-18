package org.zstack.header.storage.backup;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListBackupStorageMsg extends APIListMessage {

    public APIListBackupStorageMsg() {
    }

    public APIListBackupStorageMsg(List<String> uuids) {
        super(uuids);
    }

    public static APIListBackupStorageMsg __example__() {
        APIListBackupStorageMsg msg = new APIListBackupStorageMsg();
        return msg;
    }
    
}