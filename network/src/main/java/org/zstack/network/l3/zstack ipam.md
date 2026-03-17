# ZStack IPAM

ZStack IPAM 负责管理 L3 网络的 IP 地址分配和回收。它提供三种方式:
1. **自动分配**: ZStack云平台根据L3配置的ip range自动分配。
2. **手动分配**: 用户可以在创建虚拟机时指定IP地址。
3. **qga获取**: 通过DHCP服务器动态分配IP地址。

## 自动分配

自动分配需要满足两个条件:
1. L3网络必须配置ip range。
2. L3网络必须enable dhcp服务。这是个历史遗留问题: 扁平网络使用dhcp服务标识是否启用自动分配功能, 其它网络类型不受影响
它根据用户输入的l3网络uuid和可选的ip地址, 按照地址分配算法分配一个可用地址，分配的IP地址包含: ip地址，掩码(或者前缀长度)，网关

### 自动分配算法
- 随机分配: 从可用ip地址池中随机选择一个ip地址分配给虚拟机
- 顺序分配: 从可用ip地址池中按照顺序选择一个ip
- 循环分配: 从可用ip地址池中按照顺序选择一个ip, 分配完最后一个ip后, 从第一个ip重新开始分配

### 当前状况
cloud 5.5版本情况:
1. 扁平网络可以有三种情况: no ip range, ip range without dhcp, ip range with dhcp.
2. 公有网络和VPC网络有两种情况: ip range without dhcp, ip range with dhcp.
3. 管理网和流量网络只有一种情况: ip range without dhcp.

### 工作时机
以下操作会触发自动分配:
1. 创建虚拟机(APICreateVmInstanceMsg)
2. 虚拟机添加网卡(APIAttachL3NetworkToVmMsg, APICreateVmNicMsg)
3. 修改虚拟机IP(APISetVmStaticIpMsg, APIChangeVmNicNetworkMsg)
4. 创建applianceVm(APICreateVpcVRouterMsg, APICreateSlbInstanceMsg, APICreateNfvInstMsg)
5. 创建Vip(APICreateVipMsg)

## 手动分配
手动指定仅仅对虚拟机生效，对于applianceVm不生效。
在前述场景1,2,3的情况下，用户可以指定ip地址. 这又分两种情况：
1. 指定的ip在ip range之内，后端仍然执行的自动分配流量
2. 指定的ip不在ip range之内, 按照手动指定流程分配
   1. 如果指定的ip地址不在l3 cidr之内，必须指定掩码, 网关可选
   2. 如果指定的ip地址在l3 cidr之内，可以不指定掩码, 网关, 如果指定必须和l3 cidr一致

### 工作时机
1. 在5.5.12之前, 扁平网络在两种情况下: no ip range, ip range without dhcp, 允许指定地址不在ip range之内
2. 在5.5.12版本及其以后, 任意网络，都可以通过修改虚拟机IP(APISetVmStaticIpMsg, APIChangeVmNicNetworkMsg) 设置不在ip range之内的地址,

如果指定的ip地址在ip ranges之外, 但是在l3 cidr之内, 则掩码和网关可以不指定, 系统会自动从l3 cidr中获取掩码和网关
如果指定的ip地址在ip cidr之外, 用户输入必须同时输入IP, 掩码或者前缀长度, 如果是默认网卡，必须指定网关, 网关必须在l3 cidr之内


## qga获取
这种方式需要打开全局配置: VmGlobalConfig.ENABLE_VM_INTERNAL_IP_OVERWRITE(默认值是false)
ZStack kvmagent会定期通过qga从云主机内部读取ip地址, 仅在扁平网络在no ip range的情况下会把读出来的ip地址分配给云主机, 其它网络类型不受影响。
其它情况下，qga获取的ip地址如果和虚拟机的ip地址冲突, 则发送报警。

## 配置虚拟机guest OS的IP地址
配置虚拟机guest OS的IP地址有3种方式:
1. **DHCP**: 通过DHCP服务器动态分配IP地址。
2. **Cloud-init**: 在虚拟机创建时，使用Cloud-init工具预配置IP地址。
3. **QGA**: 通过QEMU Guest Agent从虚拟

### DHCP
ZStack会在每个物理机启动分布式dhcp server, 虚拟机启动时候, 通过dhclient获取地址和dns等参数。

### Cloud-init
ZStack会在每个物理机启动分布式userdata server, 虚拟机启动时候, 通过cloud-init获取地址和dns等参数。

### QGA
当虚拟机安装ZStack Guest Agent后，在zstack检测guest agent第一次启动时候，通过qga配置虚拟机的ip地址，dns等参数
当用户在UI手动修改IP(APISetVmStaticIpMsg, APIChangeVmNicNetworkMsg), UI调用后端api, 触发一次配置虚拟机ip地址的过程
qga配置虚拟机的参数包含:
- IP地址
- 掩码或者前缀长度
- 网关
- DNS服务器地址
- mtu
- hostname
用户可以通过全局配置来限制配置的字段: GuestToolsGlobalProperty.GUESTTOOLS_VM_PORT_CONFIGFIELDS来限制

## 网络服务
当网卡地址不在 l3 ip range之内的时候, 又可以分为在l3 cidr之内和在l3 cidr之外两种情况:

### 在l3 cidr之内
这种情况和在l3 ip range之内的情况一样, 网络服务没有影响 

### 在l3 cidr之外 
#### 安全组
1. 安全组的规则不关心网卡ip, 当这种网卡配置了安全组以后, 需要用户小心规则的配置，否则可能满足不了需求

#### DHCP
如果网卡没有ip range, 则没有dhcp服务
如果网卡有ip range, zstack会启动dhcp服务, dnsmasq的配置文件要求指定一个ip cidr
如果网卡的ip地址不在dhcp服务的ip cidr之内, 因此dhcp模块下发配置的时候调多cidr之外的地址

#### Eip
对于扁平网络, eip功能不受影响，可以继续创建。
对于vpc网络, eip的私网地址不在l3 cidr之内, vpc路由器无法路由,网络不通
为了一致性，eip不能绑定ip地址不在l3 cidr之内的网卡, APIGetEipAttachableVmNicsMsg 也不返回ip地址不在l3 cidr之内的网卡

#### Port forwarding
只有vpc网络才有port forwarding功能, 和eip一样, vpc路由器无法路由,网络不通
Port forwarding不能绑定ip地址不在l3 cidr之内的网卡, APIGetPortForwardingAttachableVmNicsMsg 也不返回ip地址不在l3 cidr之内的网卡

#### LoadBalancer
和eip一样,网络不通
APIAddVmNicToLoadBalancerMsg, APIAddBackendServerToServerGroupMsg 不能绑定ip地址不在l3 cidr之内的网卡,
APIGetCandidateVmNicsForLoadBalancerServerGroupMsg, APIGetCandidateVmNicsForLoadBalancerMsg也不返回ip地址不在l3 cidr之内的网卡


## 代码细节

### APISetVmStaticIpMsg 
通过成员字段配置虚拟机的IP地址，掩码，网关等参数。需要完整性校验
- 如果用户输入IP地址，不输入掩码和网关，优先使用网卡上使用的掩码和网关; 
- 继续，如果网卡上没有使用的掩码和网关, 则从l3 cidr中获取掩码和网关
- 继续，如果l3 cidr中也没有掩码和网关，报错
- ipv6和ipv4的逻辑一样
- 
### APIChangeVmNicNetworkMsg
通过system tags来配置虚拟机的IP地址，掩码，网关等参数。需要完整性校验
- 如果配置ip地址，且在l3 cidr之内，掩码和网关从l3 cidr中获取
- 如果配置ip地址，且在l3 cidr之外，必须指定掩码, 网关可选; 如果是默认网卡，必须指定网关
- 如果配置ipv6地址，且在l3 cidr之内，前缀长度和网关从l3 cidr中获取
- 如果配置ipv6地址，且在l3 cidr之外，必须指定前缀长度, 网关可选; 如果是默认网卡，必须指定网关

### APICreateVmInstanceMsg
逻辑和APIChangeVmNicNetworkMsg相同

### APIGetL3NetworkIpStatisticMsg
不统计在ip range之外的ip地址

### APIAddIpRangeMsg
允许给云主机设置不在ip range之内的ip地址, 这样在添加ip range的时候, 可能包含已经分配的ip地址, 此时, 让已分配的地址属于新加入的ip range

### APIAddReservedIpRangeMsg
这个api不仅添加了ReservedIpRangeVO, 还把ReservedIpRangeVO和ip range重叠的ip添加到UsedIpVO, 
vo.setUsedFor(IpAllocatedReason.Reserved.toString());

### APICheckIpAvailabilityMsg 
这个api在5.5.12版本之前, 在扁平网络no dhcp的情况下跳过ip range的检查, 这个功能不变
