package org.zstack.storage.surfs.backup;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.APIAddBackupStorageMsg;
import org.zstack.storage.surfs.SurfsConstants;

/**
 * Created by zhouhaiping 2017-09-01
 */
@OverriddenApiParams({
        @OverriddenApiParam(field = "url", param = @APIParam(maxLength = 2048, required = false))
})
@RestRequest(
        path = "/backup-storage/surfs",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddBackupStorageEvent.class
)
public class APIAddSurfsBackupStorageMsg extends APIAddBackupStorageMsg {
    @APIParam(nonempty = false, emptyString = false)
    private List<String> nodeUrls;
    
    @APIParam(required = false, maxLength = 255)
    private String poolName;

    public String getUrl() {
        return "not used";
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public List<String> getNodeUrls() {
        return nodeUrls;
    }

    public void setNodeUrls(List<String> nodeUrls) {
        this.nodeUrls = nodeUrls;
    }    
    
    @Override
    public String getType() {
        return SurfsConstants.SURFS_BACKUP_STORAGE_TYPE;
    }
 
    public static APIAddSurfsBackupStorageMsg __example__() {
        APIAddSurfsBackupStorageMsg msg = new APIAddSurfsBackupStorageMsg();
        msg.setNodeUrls(Collections.singletonList("root:password@localhost/?nodePort=7777"));
        msg.setName("surfs");
        return msg;
    }

}
