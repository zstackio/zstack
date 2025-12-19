package org.zstack.storage.addon.primary;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageSpaceVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageSpaceVO_;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class ExternalPrimaryStorageSpaceHelper {
    protected String primaryStorageUuid;
    protected String spaceName;
    protected Map<String, ExternalPrimaryStorageSpaceVO> storageSpacesByUrl;

    public ExternalPrimaryStorageSpaceHelper(ExternalPrimaryStorageVO ps) {
        super();
        this.primaryStorageUuid = ps.getUuid();
        this.spaceName = ps.getIdentity();
    }

    public ExternalPrimaryStorageSpaceHelper(String psUuid, String identity) {
        this.primaryStorageUuid = psUuid;
        spaceName = identity;
    }

    public Map<String, ExternalPrimaryStorageSpaceVO> getStorageSpacesByUrl() {
        if (storageSpacesByUrl == null) {
            List<ExternalPrimaryStorageSpaceVO> spaces = Q.New(ExternalPrimaryStorageSpaceVO.class)
                    .eq(ExternalPrimaryStorageSpaceVO_.primaryStorageUuid, primaryStorageUuid)
                    .list();
            if (CollectionUtils.isEmpty(spaces)) {
                storageSpacesByUrl = new HashMap<>();
            } else {
                storageSpacesByUrl = spaces.stream()
                        .collect(Collectors.toMap(ExternalPrimaryStorageSpaceVO::getLocationUrl, it -> it));
                spaceName += (" " + spaces.get(0).getType());
            }
        }
        return storageSpacesByUrl;
    }

    // TODO: add cache for db result
    public String getLocationSpaceUrl(String installUrl) {
        Set<String> spaceUrls = getStorageSpacesByUrl().keySet();
        return spaceUrls.stream().filter(installUrl::startsWith).findFirst()
                .orElseThrow(() -> new OperationFailureException(operr("cannot find storage space for installUrl[%s]", installUrl)));
    }
}
