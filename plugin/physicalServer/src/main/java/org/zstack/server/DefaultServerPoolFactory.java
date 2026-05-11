package org.zstack.server;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.server.PhysicalServerConstant;
import org.zstack.header.server.ServerPoolState;
import org.zstack.header.server.ServerPoolVO;
import org.zstack.header.server.ServerPoolVO_;

public class DefaultServerPoolFactory {
    @Autowired
    private DatabaseFacade dbf;

    public ServerPoolVO findDefaultPool(String zoneUuid) {
        return Q.New(ServerPoolVO.class)
                .eq(ServerPoolVO_.zoneUuid, zoneUuid)
                .eq(ServerPoolVO_.isDefault, true)
                .find();
    }

    public boolean hasAnyPool(String zoneUuid) {
        return Q.New(ServerPoolVO.class)
                .eq(ServerPoolVO_.zoneUuid, zoneUuid)
                .isExists();
    }

    public ServerPoolVO ensureDefaultPool(String zoneUuid) {
        ServerPoolVO existing = findDefaultPool(zoneUuid);
        if (existing != null) {
            return existing;
        }

        ServerPoolVO vo = new ServerPoolVO();
        vo.setUuid(DigestUtils.md5Hex(zoneUuid + "-default-pool"));
        vo.setName(PhysicalServerConstant.DEFAULT_SERVER_POOL_NAME);
        vo.setDescription("Default server pool created automatically");
        vo.setZoneUuid(zoneUuid);
        vo.setState(ServerPoolState.Enabled);
        vo.setDefault(true);
        return dbf.persistAndRefresh(vo);
    }
}
