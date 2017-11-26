package org.zstack.storage.primary.local;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

/**
 * Created by lining on 2017/11/26.
 */
public class LocalStorageUtils {

    public static boolean isOnlyAttachedLocalStorage(String clusterUuid){
        boolean result = SQL.New("select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type != :ptype", String.class)
                .param("cuuid", clusterUuid)
                .param("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .list().isEmpty();

        return result;
    }

    public static boolean isLocalStorage(String psUuid){
        String psType = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .findValue();

        return LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(psType);
    }
}
