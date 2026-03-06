# VM 网卡 IP 地址支持设置在 L3 IP Range 范围外

## 功能概述

允许用户通过 `APISetVmStaticIpMsg`、`APIChangeVmNicNetworkMsg`、`APIAttachL3NetworkToVmMsg`、`APICreateVmNicMsg` 把网卡地址设置到 L3 IP Range 之外。此功能始终开启，无需全局配置。同时支持为网卡设置自定义 DNS。

> **`enableIpAddressAllocation()` 的含义**：
> - 仅控制 L3 上创建网卡是否执行 IPAM 地址分配过程
> - `enableIPAM=false` 的网络（如 flat/noRange/noDhcp）始终允许设置范围外 IP
> - 即使 `enableIpAddressAllocation() = true`，只要指定地址不在 IP Range 内，就不走 IPAM 分支，走 no-ipam 分支直接创建 `UsedIpVO`（`ipRangeUuid = null`）
>
> **有效网络组合**：
> - Flat 网络（3 种）：no IP range + no DHCP（enableIPAM=false）、IP range + no DHCP、IP range + DHCP
> - 公有网络（2 种）：IP range + no DHCP、IP range + DHCP
> - VPC 网络（1 种）：IP range + DHCP

### 1.1 已删除全局配置 `ALLOW_IP_OUTSIDE_RANGE`

原全局配置 `l3Network.allow.ip.outside.range` 已删除。外范围 IP 始终允许，不再需要开关控制。

### 1.2 已删除 `IsIpAddressInRangesCheckEnabled()`

原有通过 DHCP 服务判断是否允许范围外 IP 的方法已删除。

- `header/.../L3NetworkVO.java` — 删除 `IsIpAddressInRangesCheckEnabled()` 方法
- `header/.../L3NetworkInventory.java` — 删除 `IsIpAddressInRangesCheckEnabled()` 方法

---

## 二、Interceptor 校验修改

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmInstanceApiInterceptor.java`

### 2.1 validate(APIChangeVmNicNetworkMsg)

外范围 IP 始终允许，不再进行 IP Range 范围校验：

```java
checkIpsOccupied(ips, l3Uuid);
```

### 2.2 validateStaticIpCommon

仅保留格式验证和重复 IP 检查，删除 CIDR 范围校验逻辑。

### 2.3 validate(APISetVmStaticIpMsg)

- 删除 `needRangeValidation` 逻辑，外范围 IP 始终允许
- `fillIpv4Parameters` / `fillIpv6Parameters` 无条件调用
- 新增默认网卡网关强制要求：IP 在 CIDR 之外且为默认网卡时，必须指定 gateway

### 2.4 validate(APICreateVmNicMsg) / validate(APIAttachL3NetworkToVmMsg)

删除 IP Range 范围校验。

### 2.5 fillIpv4Parameters / fillIpv6Parameters

新增 `defaultL3NetworkUuid` 参数。当 IP 在所有 CIDR 之外（`matchedRange == null`）时：
- 若为默认网卡，必须指定 gateway，否则报错
- 若非默认网卡，gateway 默认为空字符串

### 2.6 validateDnsAddresses

DNS 地址校验，`APISetVmStaticIpMsg` 和 `APIChangeVmNicNetworkMsg` 均调用：
- 最多 `MAXIMUM_NIC_DNS_NUMBER` 个
- 每个必须是合法 IPv4 或 IPv6 地址

---

## 三、checkIpAvailability 修改

**文件**: `network/src/main/java/org/zstack/network/l3/L3BasicNetwork.java`

- `enableIpAddressAllocation()=false`（flat/noDhcp）时跳过 IP Range 检查（保持 5.5.12 之前行为）
- 删除 `ALLOW_IP_OUTSIDE_RANGE` 判断

> **注意**: 保留 IP 范围在创建时已从正常 IP Range 中分配占位 IP，因此 `checkIpOccupied` 已足够覆盖冲突检测，无需额外的 reserved range 检查。

**文件**: `network/src/main/java/org/zstack/network/l3/IpNotAvailabilityReason.java`

新增 `RESERVED("it is in reserved ip range")` 枚举值。

---

## 四、StaticIpOperator 修改

**文件**: `compute/src/main/java/org/zstack/compute/vm/StaticIpOperator.java`

`fillUpStaticIpInfoToVmNics()` 中：
- 当用户指定的 netmask/gateway/prefix 与 IP Range 不一致时，始终允许使用用户指定值
- 删除 `ALLOW_IP_OUTSIDE_RANGE` 条件分支及其 else-throw 死代码

---

## 五、VmInstanceBase 处理逻辑（无需修改）

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmInstanceBase.java`

`handle(SetVmStaticIpMsg)` 和 `changeVmNicNetwork()` 已通过 `allStaticIpsOutsideRange()` 正确判断：即使 `enableIpAddressAllocation() = true`，只要 IP 全部不在 IP Range 内，就走 no-ipam 分支直接创建 `UsedIpVO`（`ipRangeUuid = null`）。无需额外修改。

---

## 六、范围外 IP 的约束处理

范围外 IP 创建的 `UsedIpVO` 其 `ipRangeUuid = null`，以下模块需针对此情况做特殊处理。

### 6.1 L3 网络 IP 统计排除

**文件**: `network/src/main/java/org/zstack/network/l3/L3NetworkManagerImpl.java`

按 L3Network 和 Zone 统计 UsedIp 时排除 `ipRangeUuid is null` 的记录。

### 6.2 添加 IpRange 时回填孤儿 IP

**文件**: `network/src/main/java/org/zstack/network/l3/NormalIpRangeFactory.java`

添加 IpRange 后，查询 `ipRangeUuid = null` 的 UsedIpVO，如果 IP 在新 Range 内则自动设置 `ipRangeUuid`。

### 6.3 添加 IpRange 时校验特殊地址冲突

**文件**: `network/src/main/java/org/zstack/network/l3/L3NetworkApiInterceptor.java`

添加第一个 IpRange 时，检查 gateway / network address / broadcast address 是否已被 `ipRangeUuid = null` 的 UsedIpVO 占用，若占用则拒绝。

### 6.4 DHCP 跳过范围外 IP

**文件**: `network/src/main/java/org/zstack/network/service/DhcpExtension.java`

- `isDualStackNicInSingleL3Network()`：过滤 `ipRangeUuid = null` 的 IP
- `setDualStackNicOfSingleL3Network()`：过滤 `ipRangeUuid = null` 的 IP
- `makeDhcpStruct()` 主循环：跳过 `ipRangeUuid = null` 的 IP

### 6.5 安全组排除范围外 IP

**文件**: `plugin/securityGroup/.../SecurityGroupManagerImpl.java`

- `getVmIpsBySecurityGroup()`: SQL 添加 `ip.ipRangeUuid is not null` 条件
- `calculateVmNicSecurityGroupTO()` 两个重载: UsedIpVO 查询添加 `.notNull(UsedIpVO_.ipRangeUuid)`

### 6.6 EIP 禁止绑定范围外 IP

**文件**: `plugin/eip/.../EipApiInterceptor.java`

`validate(APIAttachEipMsg)` 和 `validate(APICreateEipMsg)` 中检查 `UsedIpVO.ipRangeUuid`，为 null 则拒绝。

### 6.7 端口转发禁止绑定范围外 IP

**文件**: `plugin/portForwarding/.../PortForwardingApiInterceptor.java`

`validate(APIAttachPortForwardingRuleMsg)` 和 `validate(APICreatePortForwardingRuleMsg)` 中检查。

### 6.8 负载均衡禁止绑定范围外 IP

**文件**: `plugin/loadBalancer/.../LoadBalancerApiInterceptor.java`

`validate(APIAddVmNicToLoadBalancerMsg)` 和 `validate(APIAddBackendServerToServerGroupMsg)` 中检查。

---

## 七、自定义 DNS 功能

### 7.1 系统标签定义

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmSystemTags.java`
```java
public static String STATIC_DNS_L3_UUID_TOKEN = "l3NetworkUuid";
public static String STATIC_DNS_TOKEN = "staticDns";
public static PatternedSystemTag STATIC_DNS = new PatternedSystemTag(
    String.format("staticDns::{%s}::{%s}", STATIC_DNS_L3_UUID_TOKEN, STATIC_DNS_TOKEN),
    VmInstanceVO.class);
```

### 7.2 API 消息新增字段

| 消息 | 新增字段 |
|------|---------|
| `APISetVmStaticIpMsg` | `ip`, `ip6`, `netmask`, `gateway`, `ipv6Gateway`, `ipv6Prefix`, `dnsAddresses` |
| `SetVmStaticIpMsg` | `ip`, `ip6`, `netmask`, `gateway`, `ipv6Gateway`, `ipv6Prefix`, `dnsAddresses` |
| `APIChangeVmNicNetworkMsg` | `staticIp`, `dnsAddresses`（IP/掩码/网关通过 system tag 传递） |
| `ChangeVmNicNetworkMsg` | `staticIp`, `dnsAddresses` |

### 7.3 DNS 操作方法

**文件**: `compute/src/main/java/org/zstack/compute/vm/StaticIpOperator.java`
- `setStaticDns(String vmUuid, String l3Uuid, List<String> dnsAddresses)` — 设置静态 DNS
- `deleteStaticDnsByVmUuidAndL3Uuid(String vmUuid, String l3Uuid)` — 删除静态 DNS

### 7.4 DNS 获取接口

**文件**: `network/src/main/java/org/zstack/network/service/NetworkServiceManager.java`
```java
List<String> getVmNicDns(String vmUuid, String l3NetworkUuid);
```

**文件**: `network/src/main/java/org/zstack/network/service/NetworkServiceManagerImpl.java`

实现逻辑：优先从 `staticDns` 系统标签获取自定义 DNS；若无，回退到 L3 网络 DNS。

### 7.5 VmInstanceBase 处理

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmInstanceBase.java`

- `setIpamStaticIp()` / `setNoIpamStaticIp()`：成功后调用 `setStaticDns()`
- `changeVmNicNetwork()`：`SetStaticIp` 内部类中调用 `setStaticDns()`

### 7.6 GuestTools 集成（未实现）

**文件**: `premium/guesttools/.../GuestToolsManagerImpl.java`

计划使用 `getVmNicDns()` 替代 `getL3NetworkDns()` 以支持自定义 DNS 下发。当前未实现。

---

## 八、测试用例

**已有**:
- `test/.../flat/FlatChangeVmIpOutsideCidrCase.groovy` — 覆盖扁平网络场景
- `test/.../flat/PublicNetworkChangeVmIpOutsideCidrCase.groovy` — 覆盖公有网络场景
- `premium/test-premium/.../VpcChangeVmIpOutsideCidrCase.groovy` — 覆盖 VPC 场景下 EIP/LB/PF/IP 统计等

---

## 九、逻辑总结

| 场景 | `UsedIpVO.ipRangeUuid` | 处理方式 |
|------|------------------------|---------|
| IP 在 Range 内 | 有值 | 正常 IPAM 处理 |
| IP 不在 Range 内 | `null` | no-ipam 分支直接创建 |
| L3 网络 IP 统计 | — | 排除 `null` 记录 |
| DHCP 下发 | — | 跳过 `null` 记录 |
| 安全组计算 | — | 排除 `null` 记录 |
| EIP / LB / PF 绑定 | — | 禁止 `null` 记录 |
| 添加 IpRange | — | 自动回填范围内的孤儿 IP |
