# VM 网卡 IP 地址支持设置在 L3 IP Range 范围外

## 功能概述

允许用户通过 `APISetVmStaticIpMsg` 和 `APIChangeVmNicNetworkMsg` 把网卡地址设置到 L3 IP Range 之外。通过全局配置 `l3Network.allow.ip.outside.range` 控制是否开放此能力（默认关闭）。同时支持为网卡设置自定义 DNS。

---

## 一、全局配置

**文件**: `network/src/main/java/org/zstack/network/l3/L3NetworkGlobalConfig.java`

```java
@GlobalConfigDef(defaultValue = "false", type = Boolean.class,
        description = "allow setting VM NIC IP address outside L3 network IP ranges")
@GlobalConfigValidation(validValues = {"true", "false"})
public static GlobalConfig ALLOW_IP_OUTSIDE_RANGE = new GlobalConfig(CATEGORY, "allow.ip.outside.range");
```

- `false`（默认）：IP 必须在 L3 IP Range 内（`enableIPAM=false` 的网络除外）
- `true`：允许设置不在 IP Range 内的 IP 地址，全局所有 L3 网络生效

> **`enableIPAM` 与 `allow.ip.outside.range` 的关系**：
> - `enableIPAM`（数据库字段，默认 true）控制 L3 是否启用 IPAM 管理
> - `enableIPAM=false` 的网络（如 flat/noRange/noDhcp）始终允许设置范围外 IP，无需全局配置
> - `enableIPAM=true` 的网络需要全局配置 `allow.ip.outside.range=true` 才能设置范围外 IP
> - `enableIpAddressAllocation()` 仅控制 L3 上创建网卡是否执行 IPAM 地址分配过程，不用于范围校验
> - 即使 `enableIpAddressAllocation() = true`，只要指定地址不在 IP Range 内，就不走 IPAM 分支，走 no-ipam 分支直接创建 `UsedIpVO`（`ipRangeUuid = null`）
>
> **有效网络组合**：
> - Flat 网络（3 种）：no IP range + no DHCP（enableIPAM=false）、IP range + no DHCP、IP range + DHCP
> - 公有网络（2 种）：IP range + no DHCP、IP range + DHCP
> - VPC 网络（1 种）：IP range + DHCP

### 1.1 删除 `IsIpAddressInRangesCheckEnabled()`

原有通过 DHCP 服务判断是否允许范围外 IP 的方法已删除，改为使用全局配置。

- `header/.../L3NetworkVO.java` — 删除 `IsIpAddressInRangesCheckEnabled()` 方法
- `header/.../L3NetworkInventory.java` — 删除 `IsIpAddressInRangesCheckEnabled()` 方法

---

## 二、Interceptor 校验修改

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmInstanceApiInterceptor.java`

### 2.1 validate(APIChangeVmNicNetworkMsg)

`APIChangeVmNicNetworkMsg` 通过 system tag 传递 IP/掩码/网关参数，不使用显式字段。

**staticIp 范围检查放开**：
```java
// 修改前
if (!l3NetworkVO.enableIpAddressAllocation()) { found = true; }
// 修改后
if (!l3NetworkVO.enableIpAddressAllocation()
        || L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.value(Boolean.class)) {
    found = true;
}
```

**system tag 中 staticIp 范围校验**：
```java
// 修改前
if (l3NetworkVO.IsIpAddressInRangesCheckEnabled()) {
// 修改后
if (!L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.value(Boolean.class)) {
```

### 2.2 validateStaticIPv4 / validateStaticIPv6

跳过已有 NIC IP 的范围校验：
```java
// 修改前
if (!l3NetworkVO.IsIpAddressInRangesCheckEnabled()) { continue; }
// 修改后
if (L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.value(Boolean.class)) { continue; }
```

### 2.3 validate(APISetVmStaticIpMsg)

**范围外 IP 拒绝逻辑**：
```java
// 修改前
if (l3NetworkVO.IsIpAddressInRangesCheckEnabled()) {
// 修改后
if (!L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.value(Boolean.class)) {
```

**netmask/gateway 填充分支（IPv4 和 IPv6）**：
```java
// 修改前
if (msg.getIp() != null && !l3NetworkVO.enableIpAddressAllocation()) {
// 修改后
if (msg.getIp() != null && (!l3NetworkVO.enableIpAddressAllocation()
        || L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.value(Boolean.class))) {
```
IPv6 同理。当 IP 不在范围内且用户未指定 netmask/gateway 时，从已有 IpRange 中取默认值；若无 IpRange 则要求用户显式指定。

### 2.4 validateDnsAddresses

新增 DNS 地址校验，`APISetVmStaticIpMsg` 和 `APIChangeVmNicNetworkMsg` 均调用：
- 最多 `MAXIMUM_NIC_DNS_NUMBER` 个
- 每个必须是合法 IPv4 或 IPv6 地址

---

## 三、checkIpAvailability 修改

**文件**: `network/src/main/java/org/zstack/network/l3/L3BasicNetwork.java`

全局配置开启时跳过 IP Range 检查：
```java
if (!self.enableIpAddressAllocation()) {
    inRange = true;
}
// 新增
if (L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.value(Boolean.class)) {
    inRange = true;
}
```

---

## 四、VmInstanceBase 处理逻辑（无需修改）

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmInstanceBase.java`

`handle(SetVmStaticIpMsg)` 和 `changeVmNicNetwork()` 已通过 `allStaticIpsOutsideRange()` 正确判断：即使 `enableIpAddressAllocation() = true`，只要 IP 全部不在 IP Range 内，就走 no-ipam 分支直接创建 `UsedIpVO`（`ipRangeUuid = null`）。无需额外修改。

---

## 五、范围外 IP 的约束处理

范围外 IP 创建的 `UsedIpVO` 其 `ipRangeUuid = null`，以下模块需针对此情况做特殊处理。

### 5.1 L3 网络 IP 统计排除

**文件**: `network/src/main/java/org/zstack/network/l3/L3NetworkManagerImpl.java`

按 L3Network 和 Zone 统计 UsedIp 时排除 `ipRangeUuid is null` 的记录。

### 5.2 添加 IpRange 时回填孤儿 IP

**文件**: `network/src/main/java/org/zstack/network/l3/NormalIpRangeFactory.java`

添加 IpRange 后，查询 `ipRangeUuid = null` 的 UsedIpVO，如果 IP 在新 Range 内则自动设置 `ipRangeUuid`。

### 5.3 添加 IpRange 时校验特殊地址冲突

**文件**: `network/src/main/java/org/zstack/network/l3/L3NetworkApiInterceptor.java`

添加第一个 IpRange 时，检查 gateway / network address / broadcast address 是否已被 `ipRangeUuid = null` 的 UsedIpVO 占用，若占用则拒绝。

### 5.4 DHCP 跳过范围外 IP

**文件**: `network/src/main/java/org/zstack/network/service/DhcpExtension.java`

- `isDualStackNicInSingleL3Network()`：过滤 `ipRangeUuid = null` 的 IP
- `setDualStackNicOfSingleL3Network()`：过滤 `ipRangeUuid = null` 的 IP
- `makeDhcpStruct()` 主循环：跳过 `ipRangeUuid = null` 的 IP

### 5.5 安全组排除范围外 IP

**文件**: `plugin/securityGroup/.../SecurityGroupManagerImpl.java`

SQL 添加 `ip.ipRangeUuid is not null` 条件。

### 5.6 EIP 禁止绑定范围外 IP

**文件**: `plugin/eip/.../EipApiInterceptor.java`

`validate(APIAttachEipMsg)` 和 `validate(APICreateEipMsg)` 中检查 `UsedIpVO.ipRangeUuid`，为 null 则拒绝。

### 5.7 端口转发禁止绑定范围外 IP

**文件**: `plugin/portForwarding/.../PortForwardingApiInterceptor.java`

`validate(APIAttachPortForwardingRuleMsg)` 和 `validate(APICreatePortForwardingRuleMsg)` 中检查。

### 5.8 负载均衡禁止绑定范围外 IP

**文件**: `plugin/loadBalancer/.../LoadBalancerApiInterceptor.java`

`validate(APIAddVmNicToLoadBalancerMsg)` 和 `validate(APIAddBackendServerToServerGroupMsg)` 中检查。

---

## 六、自定义 DNS 功能

### 6.1 系统标签定义

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmSystemTags.java`
```java
public static String STATIC_DNS_L3_UUID_TOKEN = "l3NetworkUuid";
public static String STATIC_DNS_TOKEN = "staticDns";
public static PatternedSystemTag STATIC_DNS = new PatternedSystemTag(
    String.format("staticDns::{%s}::{%s}", STATIC_DNS_L3_UUID_TOKEN, STATIC_DNS_TOKEN),
    VmInstanceVO.class);
```

### 6.2 API 消息新增字段

| 消息 | 新增字段 |
|------|---------|
| `APISetVmStaticIpMsg` | `ip`, `ip6`, `netmask`, `gateway`, `ipv6Gateway`, `ipv6Prefix`, `dnsAddresses` |
| `SetVmStaticIpMsg` | `ip`, `ip6`, `netmask`, `gateway`, `ipv6Gateway`, `ipv6Prefix`, `dnsAddresses` |
| `APIChangeVmNicNetworkMsg` | `staticIp`, `dnsAddresses`（IP/掩码/网关通过 system tag 传递） |
| `ChangeVmNicNetworkMsg` | `staticIp`, `dnsAddresses` |

### 6.3 DNS 操作方法

**文件**: `compute/src/main/java/org/zstack/compute/vm/StaticIpOperator.java`
- `setStaticDns(String vmUuid, String l3Uuid, List<String> dnsAddresses)` — 设置静态 DNS
- `deleteStaticDnsByVmUuidAndL3Uuid(String vmUuid, String l3Uuid)` — 删除静态 DNS

### 6.4 DNS 获取接口

**文件**: `network/src/main/java/org/zstack/network/service/NetworkServiceManager.java`
```java
List<String> getVmNicDns(String vmUuid, String l3NetworkUuid);
```

**文件**: `network/src/main/java/org/zstack/network/service/NetworkServiceManagerImpl.java`

实现逻辑：优先从 `staticDns` 系统标签获取自定义 DNS；若无，回退到 L3 网络 DNS。

### 6.5 VmInstanceBase 处理

**文件**: `compute/src/main/java/org/zstack/compute/vm/VmInstanceBase.java`

- `setIpamStaticIp()` / `setNoIpamStaticIp()`：成功后调用 `setStaticDns()`
- `changeVmNicNetwork()`：`SetStaticIp` 内部类中调用 `setStaticDns()`

### 6.6 GuestTools 集成（未实现）

**文件**: `premium/guesttools/.../GuestToolsManagerImpl.java`

计划使用 `getVmNicDns()` 替代 `getL3NetworkDns()` 以支持自定义 DNS 下发。当前未实现。

---

## 七、测试用例

**已有**:
- `test/.../flat/FlatChangeVmIpOutsideCidrCase.groovy` — 覆盖扁平/公有网络场景

**计划**（未实现）:
- `premium/test-premium/.../VmNicIpOutsideCidrCase.groovy` — 覆盖 VPC 场景下 EIP/LB/PF/IP 统计等

---

## 八、逻辑总结

| 场景 | `UsedIpVO.ipRangeUuid` | 处理方式 |
|------|------------------------|---------|
| IP 在 Range 内 | 有值 | 正常 IPAM 处理 |
| IP 不在 Range 内 | `null` | no-ipam 分支直接创建 |
| L3 网络 IP 统计 | — | 排除 `null` 记录 |
| DHCP 下发 | — | 跳过 `null` 记录 |
| 安全组计算 | — | 包含所有记录（不再排除） |
| EIP / LB / PF 绑定 | — | 禁止 `null` 记录 |
| 添加 IpRange | — | 自动回填范围内的孤儿 IP |
