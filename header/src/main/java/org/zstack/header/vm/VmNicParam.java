package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.APINoSee;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PythonClassInventory
public class VmNicParam implements Serializable {
    private String l3NetworkUuid;

    private String ip;

    private String ip6;

    private String mac;

    private String netmask;

    private String gateway;

    private String metaData;

    private Integer ipVersion;

    private String driverType;

    private String VmNicType;

    private String state = VmNicState.enable.toString();

    private Long outboundBandwidth;

    private Long inboundBandwidth;

    private Integer multiQueueNum;

    private Boolean isDefaultNic;

    private List<String> sgUuids = new ArrayList<>();

    @APINoSee
    private Map<String, String> IpMap = new HashMap<>(); /* filled for cloned vms more than 2 */

    @APINoSee
    private Map<String, String> Ip6Map = new HashMap<>(); /* filled for cloned vms more than 2 */

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(Long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public Long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(Long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public Integer getMultiQueueNum() {
        return multiQueueNum;
    }

    public void setMultiQueueNum(Integer multiQueueNum) {
        this.multiQueueNum = multiQueueNum;
    }

    public String getIp6() {
        return ip6;
    }

    public void setIp6(String ip6) {
        this.ip6 = ip6;
    }

    public Boolean getDefaultNic() {
        return isDefaultNic;
    }

    public void setDefaultNic(Boolean defaultNic) {
        this.isDefaultNic = defaultNic;
    }

    public String getVmNicType() {
        return VmNicType;
    }

    public void setVmNicType(String vmNicType) {
        VmNicType = vmNicType;
    }

    public List<String> getSgUuids() {
        return sgUuids;
    }

    public void setSgUuids(List<String> sgUuids) {
        this.sgUuids = sgUuids;
    }

    public Map<String, String> getIpMap() {
        return IpMap;
    }

    public void setIpMap(Map<String, String> ipMap) {
        IpMap = ipMap;
    }

    public Map<String, String> getIp6Map() {
        return Ip6Map;
    }

    public void setIp6Map(Map<String, String> ip6Map) {
        Ip6Map = ip6Map;
    }
}
