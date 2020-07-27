package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.network.l3.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.network.IPv6Constants;

import java.util.List;

public class NormalIpRangeFactory implements IpRangeFactory {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public IpRangeType getType() {
        return IpRangeType.Normal;
    }

    @Override
    public IpRangeInventory createIpRange(IpRangeInventory ipr, APICreateMessage msg) {
        NormalIpRangeVO vo = new SQLBatchWithReturn<NormalIpRangeVO>() {
            @Override
            protected NormalIpRangeVO scripts() {
                NormalIpRangeVO vo = new NormalIpRangeVO();
                vo.setUuid(ipr.getUuid() == null ? Platform.getUuid() : ipr.getUuid());
                vo.setDescription(ipr.getDescription());
                vo.setEndIp(ipr.getEndIp());
                vo.setGateway(ipr.getGateway());
                vo.setL3NetworkUuid(ipr.getL3NetworkUuid());
                vo.setName(ipr.getName());
                vo.setNetmask(ipr.getNetmask());
                vo.setStartIp(ipr.getStartIp());
                vo.setNetworkCidr(ipr.getNetworkCidr());
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                vo.setIpVersion(ipr.getIpVersion());
                vo.setAddressMode(ipr.getAddressMode());
                vo.setPrefixLen(ipr.getPrefixLen());
                dbf.getEntityManager().persist(vo);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(vo);

                return vo;
            }
        }.execute();

        IpRangeHelper.updateL3NetworkIpversion(ipr);

        final IpRangeInventory finalIpr = NormalIpRangeInventory.valueOf1(vo);
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAddIpRangeExtensionPoint.class), new ForEachFunction<AfterAddIpRangeExtensionPoint>() {
            @Override
            public void run(AfterAddIpRangeExtensionPoint ext) {
                ext.afterAddIpRange(finalIpr, msg.getSystemTags());
            }
        });

        return finalIpr;
    }
}
