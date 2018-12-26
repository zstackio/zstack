package org.zstack.header.core.trash;

import org.zstack.header.rest.SDK;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/26.
 */
@SDK
public class CleanTrashResult {
    private List<String> resourceUuids = new ArrayList<>();
    private Long size = 0L;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
