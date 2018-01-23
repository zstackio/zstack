package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/1/26.
 */
public class GetPrimaryStorageFolderListReply extends MessageReply {
    private List<String> folders = new ArrayList<>();

    public List<String> getFolders() {
        return folders;
    }

    public void setFolders(List<String> folders) {
        this.folders = folders;
    }
}
