package org.zstack.network.service.virtualrouter;

import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

public class VirtualRouterCommands {
	public static class AgentCommand {
	}
	
	public static class AgentResponse {
		private boolean success = true;
		private String error;
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public String getError() {
			return error;
		}
		public void setError(String error) {
			this.error = error;
		}
	}

    public static class InitCommand extends AgentCommand {
		private String uuid;
        private int restartDnsmasqAfterNumberOfSIGUSER1;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public int getRestartDnsmasqAfterNumberOfSIGUSER1() {
            return restartDnsmasqAfterNumberOfSIGUSER1;
        }

        public void setRestartDnsmasqAfterNumberOfSIGUSER1(int restartDnsmasqAfterNumberOfSIGUSER1) {
            this.restartDnsmasqAfterNumberOfSIGUSER1 = restartDnsmasqAfterNumberOfSIGUSER1;
        }
    }

    public static class InitRsp extends AgentResponse {
    }
	
	public static class NicInfo {
		private String ip;
		private String mac;
		private String gateway;
		private String netmask;
		private boolean isDefaultRoute;
		
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
		public String getGateway() {
			return gateway;
		}
		public void setGateway(String gateway) {
			this.gateway = gateway;
		}
		public String getNetmask() {
			return netmask;
		}
		public void setNetmask(String netmask) {
			this.netmask = netmask;
		}
		public boolean isDefaultRoute() {
			return isDefaultRoute;
		}
		public void setDefaultRoute(boolean isDefaultRoute) {
			this.isDefaultRoute = isDefaultRoute;
		}
	}
	
	public static class ConfigureNicCmd extends AgentCommand {
		private List<NicInfo> nics;

		public List<NicInfo> getNics() {
			return nics;
		}

		public void setNics(List<NicInfo> nics) {
			this.nics = nics;
		}
	}
	
	public static class ConfigureNicRsp extends AgentResponse {
	}

	public static class RemoveNicCmd extends AgentCommand {
		private List<NicInfo> nics;

		public List<NicInfo> getNics() {
			return nics;
		}

		public void setNics(List<NicInfo> nics) {
			this.nics = nics;
		}
	}

	public static class RemoveNicRsp extends AgentResponse {
	}
	
	public static class DhcpInfo {
		private String ip;
		private String mac;
		private String netmask;
		private String gateway;
		private List<String> dns;
		private String hostname;
        private String vrNicMac;
        private String dnsDomain;
        private boolean isDefaultL3Network;
		private Integer mtu;

        public String getDnsDomain() {
            return dnsDomain;
        }

        public void setDnsDomain(String domain) {
            this.dnsDomain = domain;
        }

        public boolean isDefaultL3Network() {
            return isDefaultL3Network;
        }

        public void setDefaultL3Network(boolean isDefaultL3Network) {
            this.isDefaultL3Network = isDefaultL3Network;
        }

        public String getVrNicMac() {
            return vrNicMac;
        }

        public void setVrNicMac(String vrNicMac) {
            this.vrNicMac = vrNicMac;
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
		public List<String> getDns() {
			return dns;
		}
		public void setDns(List<String> dns) {
			this.dns = dns;
		}
		public String getHostname() {
			return hostname;
		}
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}
		public Integer getMtu() {
			return mtu;
		}
		public void setMtu(Integer mtu) {
			this.mtu = mtu;
		}

	}
	
	public static class RemoveDhcpEntryCmd extends AgentCommand {
		private List<DhcpInfo> dhcpEntries;

		public List<DhcpInfo> getDhcpEntries() {
			return dhcpEntries;
		}

		public void setDhcpEntries(List<DhcpInfo> dhcpEntries) {
			this.dhcpEntries = dhcpEntries;
		}
	}
	
	public static class RemoveDhcpEntryRsp extends AgentResponse {
	}
	
	public static class AddDhcpEntryCmd extends AgentCommand {
		private List<DhcpInfo> dhcpEntries;
		private boolean rebuild;

		public List<DhcpInfo> getDhcpEntries() {
			if (dhcpEntries == null) {
				dhcpEntries = new ArrayList<DhcpInfo>();
			}
			return dhcpEntries;
		}

		public void setDhcpEntries(List<DhcpInfo> dhcpEntries) {
			this.dhcpEntries = dhcpEntries;
		}

		public boolean isRebuild() {
			return rebuild;
		}

		public void setRebuild(boolean rebuild) {
			this.rebuild = rebuild;
		}
	}
	
	public static class AddDhcpEntryRsp extends AgentResponse {
	}
	
	public static class SNATInfo {
		private String publicNicMac;
		private String publicIp;
        private String privateNicMac;
        private String privateNicIp;
		private String snatNetmask;

        public String getPrivateNicMac() {
			return privateNicMac;
		}
		public void setPrivateNicMac(String nicMac) {
			this.privateNicMac = nicMac;
		}
		public String getPrivateNicIp() {
			return privateNicIp;
		}
		public void setPrivateNicIp(String nicIp) {
			this.privateNicIp = nicIp;
		}
		public String getSnatNetmask() {
			return snatNetmask;
		}
		public void setSnatNetmask(String snatNetmask) {
			this.snatNetmask = snatNetmask;
		}
		public String getPublicNicMac() {
			return publicNicMac;
		}
		public void setPublicNicMac(String publicNicMac) {
			this.publicNicMac = publicNicMac;
		}
		public String getPublicIp() {
			return publicIp;
		}
		public void setPublicIp(String publicIp) {
			this.publicIp = publicIp;
		}
	}

    public static class SyncSNATCmd extends AgentCommand {
        private List<SNATInfo> snats;

        public List<SNATInfo> getSnats() {
            return snats;
        }

        public void setSnats(List<SNATInfo> snats) {
            this.snats = snats;
        }
    }
    public static class SyncSNATRsp extends AgentResponse {
    }
	
	public static class SetSNATCmd extends AgentCommand {
        private SNATInfo snat;

        public SNATInfo getSnat() {
            return snat;
        }

        public void setSnat(SNATInfo snat) {
            this.snat = snat;
        }
	}
	
	public static class SetSNATRsp extends AgentResponse {
	}

    public static class RemoveSNATCmd extends AgentCommand {
        private List<SNATInfo> natInfo;

        public List<SNATInfo> getNatInfo() {
            return natInfo;
        }

        public void setNatInfo(List<SNATInfo> natInfo) {
            this.natInfo = natInfo;
        }
    }

    public static class RemoveSNATRsp extends AgentResponse {
    }
	
	public static class SyncPortForwardingRuleCmd extends AgentCommand {
	    private List<PortForwardingRuleTO> rules;

        public List<PortForwardingRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<PortForwardingRuleTO> rules) {
            this.rules = rules;
        }
	}
	public static class SyncPortForwardingRuleRsp extends AgentResponse {
	}

	public static class CreatePortForwardingRuleCmd extends AgentCommand {
	    private List<PortForwardingRuleTO> rules;

        public List<PortForwardingRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<PortForwardingRuleTO> rules) {
            this.rules = rules;
        }
	}
	public static class CreatePortForwardingRuleRsp extends AgentResponse {
	}
	
	public static class RevokePortForwardingRuleCmd extends AgentCommand {
	    private List<PortForwardingRuleTO> rules;

        public List<PortForwardingRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<PortForwardingRuleTO> rules) {
            this.rules = rules;
        }
	}
	public static class RevokePortForwardingRuleRsp extends AgentResponse {
	}
	
	public static class DnsInfo {
		private String dnsAddress;
		private String nicMac;

		public String getNicMac() {
			return nicMac;
		}

		public void setNicMac(String nicMac) {
			this.nicMac = nicMac;
		}

		public String getDnsAddress() {
			return dnsAddress;
		}

		public void setDnsAddress(String dnsAddress) {
			this.dnsAddress = dnsAddress;
		}
	}
	
	public static class SetDnsCmd extends AgentCommand {
		private List<DnsInfo> dns;

		public List<DnsInfo> getDns() {
			return dns;
		}

		public void setDns(List<DnsInfo> dns) {
			this.dns = dns;
		}
	}
	
	public static class SetDnsRsp extends AgentResponse {
	}

	public static class SetForwardDnsCmd extends AgentCommand {
		private String dns;
        private String mac;
        private String bridgeName;
        private String nameSpace;
        private List<String> wrongDns;

        public String getNameSpace() {
            return nameSpace;
        }

        public void setNameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getDns() {
			return dns;
		}

		public void setDns(String dns) {
			this.dns = dns;
		}

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public List<String> getWrongDns() {
            return wrongDns;
        }

        public void setWrongDns(List<String> wrongDns) {
            this.wrongDns = wrongDns;
        }
    }

	public static class SetForwardDnsRsp extends AgentResponse {
	}

	public static class RemoveForwardDnsCmd extends AgentCommand {
		private String mac;
		private String bridgeName;
		private String nameSpace;

		public String getNameSpace() {
			return nameSpace;
		}

		public void setNameSpace(String nameSpace) {
			this.nameSpace = nameSpace;
		}

		public String getBridgeName() {
			return bridgeName;
		}

		public void setBridgeName(String bridgeName) {
			this.bridgeName = bridgeName;
		}

		public String getMac() {
			return mac;
		}

		public void setMac(String mac) {
			this.mac = mac;
		}
	}

	public static class RemoveForwardDnsRsp extends AgentResponse {

	}

    public static class RemoveDnsCmd extends AgentCommand {
        private List<DnsInfo> dns;

        public List<DnsInfo> getDns() {
            return dns;
        }

        public void setDns(List<DnsInfo> dns) {
            this.dns = dns;
        }
    }

    public static class RemoveDnsRsp extends AgentResponse {
    }
	
	public static class CreateVipRsp extends AgentResponse {
	}

    public static class VipTO {
        private String ip;
        private String netmask;
        private String gateway;
        private String ownerEthernetMac;
        
        public static VipTO valueOf(VipInventory inv, String ownerMac) {
            VipTO to = new VipTO();
            to.setIp(inv.getIp());
            to.setNetmask(inv.getNetmask());
            to.setGateway(inv.getGateway());
            to.setOwnerEthernetMac(ownerMac);
            return to;
        }
        
        @Override
        public String toString() {
            return JSONObjectUtil.toJsonString(this);
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
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

        public String getOwnerEthernetMac() {
            return ownerEthernetMac;
        }

        public void setOwnerEthernetMac(String ownerEthernetMac) {
            this.ownerEthernetMac = ownerEthernetMac;
        }
    }

	public static class CreateVipCmd extends AgentCommand {
	    private List<VipTO> vips;

        public List<VipTO> getVips() {
            return vips;
        }

        public void setVips(List<VipTO> vips) {
            this.vips = vips;
        }
	}
	
	public static class RemoveVipCmd extends AgentCommand {
	    private List<VipTO> vips;

        public List<VipTO> getVips() {
            return vips;
        }

        public void setVips(List<VipTO> vips) {
            this.vips = vips;
        }
	    
	}

	public static class RemoveVipRsp extends AgentResponse {
	}

    public static class CreateEipCmd extends AgentCommand {
        private EipTO eip;

        public EipTO getEip() {
            return eip;
        }

        public void setEip(EipTO eip) {
            this.eip = eip;
        }
    }

    public static class CreateEipRsp extends AgentResponse {
    }

    public static class RemoveEipCmd extends AgentCommand {
        private EipTO eip;

        public EipTO getEip() {
            return eip;
        }

        public void setEip(EipTO eip) {
            this.eip = eip;
        }
    }

    public static class RemoveEipRsp extends AgentResponse {
    }

    public static class SyncEipCmd extends AgentCommand {
        private List<EipTO> eips;

        public List<EipTO> getEips() {
            return eips;
        }

        public void setEips(List<EipTO> eips) {
            this.eips = eips;
        }
    }

    public static class SyncEipRsp extends AgentResponse {
    }

	public static class PingCmd extends AgentCommand {
		private String uuid;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
	}

	public static class PingRsp extends AgentResponse {
		private String uuid;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
	}
}
