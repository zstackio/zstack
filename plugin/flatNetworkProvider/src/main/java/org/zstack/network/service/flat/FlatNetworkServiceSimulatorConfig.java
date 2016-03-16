package org.zstack.network.service.flat;

import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.PrepareDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.ReleaseDhcpCmd;
import org.zstack.network.service.flat.FlatDnsBackend.SetDnsCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.ApplyUserdataCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.ReleaseUserdataCmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 9/19/2015.
 */
public class FlatNetworkServiceSimulatorConfig {
    public List<ApplyDhcpCmd> applyDhcpCmdList = new ArrayList<ApplyDhcpCmd>();
    public List<ReleaseDhcpCmd> releaseDhcpCmds = new ArrayList<ReleaseDhcpCmd>();
    public List<SetDnsCmd> setDnsCmds = new ArrayList<SetDnsCmd>();
    public List<PrepareDhcpCmd> prepareDhcpCmdList = new ArrayList<PrepareDhcpCmd>();
    public List<ApplyUserdataCmd> applyUserdataCmds = new ArrayList<ApplyUserdataCmd>();
    public List<ReleaseUserdataCmd> releaseUserdataCmds = new ArrayList<ReleaseUserdataCmd>();
    public List<FlatDhcpBackend.ConnectCmd> connectCmds = new ArrayList<FlatDhcpBackend.ConnectCmd>();
}
