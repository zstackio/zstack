package org.zstack.header.host;

import org.zstack.header.message.CancelMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/7/22.
 */
public class CancelHostTasksMsg extends CancelMessage {
    private List<String> searchedMnIds = new ArrayList<>();
    private List<String> hostUuids = new ArrayList<>();

    public List<String> getSearchedMnIds() {
        return searchedMnIds;
    }

    public void setSearchedMnIds(List<String> searchedMnIds) {
        this.searchedMnIds = searchedMnIds;
    }

    public void addSearchedMnId(String mnId) {
        this.searchedMnIds.add(mnId);
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public void addHostUuid(String hostUuid) {
        this.hostUuids.add(hostUuid);
    }
}
