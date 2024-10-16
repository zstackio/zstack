# ZStack 简介 [http://www.zstack.io](http://www.zstack.io)

ZStack是一款产品化的开源IaaS（基础架构即服务）软件。它面向智能数据中心，通过完善的API统一管理包括计算、存储和网络在内的数据中心资源，提供简单快捷的环境搭建。 5分钟完成安装和部署单台Linux机器上的POC环境；30分钟完成安装和部署多管理节点生产环境（可扩展至数万台物理服务器）。

ZStack通过完善的API管理数据中心资源，构建软件定义数据中心。用户可选择UI界面或命令行工具管理云平台，与ZStack管理节点进行友好交互。 ZStack提供统一配置、统一安装、统一高可用（High Availability）和统一第三方监控的云服务解决方案，使云平台的管理更加便捷、稳定、持久。

## 为什么选择ZStack？

ZStack解决了长期困扰开源IaaS软件用户的两大痛点：复杂性和稳定性。

复杂性：ZStack始终秉承安全可控、全自动化、自管服务、硬件无锁、用户友好查询API的设计理念，为用户提供一款便捷配置、持久运行的云计算软件。

稳定性：ZStack功能架构具备以下特点：

* 全插件系统：添加或移除功能不影响已有代码
* 工作流引擎：出现错误时，任何变更均可回滚
* 瀑布流架构：支持资源的级联操作
* 3套严密的自动化测试系统：确保每个功能的代码质量，并从架构设计层面解决稳定性问题

此外，ZStack具有扩展性：单个管理节点可管理数万台物理服务器、数百万台云主机，处理数千条并发调用API请求，尤其适用于大型公有云平台的搭建。

## 主要优势

#### 弹性

单个管理节点可管理**数万台**物理服务器、数**百万台**云主机，处理**数千万条**并发API请求。

#### 敏捷

各类操作**非常快速**。以创建云主机为例，详见以下性能数据。

<table class="table table-bordered home-table" style="margin-bottom: 0;">
  <tr>
    <th>云主机数量</td>
    <th>时长&nbsp;&nbsp;
        <i class='fa fa-info-circle' style='cursor:help' title="Limited by hardware, this data is from a mixed environment containing real VMs created on nested virtualization hypervisor and simulator VMs, which are created by 100 threads using only one management node. We are 100% sure the performance will get better in the real data center with decent hardware."></i>
    </td>
  </tr>
  <tr>
    <td>1</td>
    <td>0.51 秒</td>
  </tr>
  <tr>
    <td>10</td>
    <td>1.55 秒</td>
  </tr>
  <tr>
    <td>100</td>
    <td>11.33 秒</td>
  </tr>
  <tr>
    <td>1000</td>
    <td>103 秒</td>
  </tr>
  <tr>
    <td>10000</td>
    <td>23 分</td>
  </tr>
</table>

#### 网络功能虚拟化

ZStack默认网络模型基于网络功能虚拟化（**NFV**），为每位租户提供云路由专有网络节点。整个网络模型独立自主、安全可控，用户无需购买特定设备，也无须在计算服务器上部署网络服务器。

#### 全API查询

ZStack支持超过**400万**个查询条件，以及400万阶乘的组合查询条件。用户无须编写临时脚本或登录数据库，直接通过API即可任意查询资源。

     >> QueryVmInstance vmNics.eip.guestIp=16.16.16.16 zone.name=west-coast
     >> QueryHost fields=name,uuid,managementIp hypervisorType=KVM vmInstance.allVolumes.size>=549755813888000 vmInstance.state=Running start=0 limit=10

#### 便捷部署与升级

ZStack安装升级如同安装一个**Java WAR**文件一样简单。用户只需执行一个Bootstrap脚本，即可在**5分钟**内搭建一套POC环境；**30分钟**内搭建一套多管理节点生产环境（包括研读文档时间）。

     >> [root@localhost ~]# curl http://download.zstack.org/install.sh |  bash -s -- -a

#### 全自动化

ZStack云平台由**API统一管理**，全自动化、统一配置。此外，通过无缝透明集成Ansible，可在大规模硬件设备上全自动安装/配置/升级代理程序，全过程无需用户干预。

#### 通用插件系统

ZStack采用与OSGi和Eclipse类似的插件系统方式作为核心架构基础，添加或移除功能不会对核心架构产生任何影响，满足了用户对于开源IaaS软件的鲁棒性需求。

#### 严密的测试系统

ZStack提供**3套全自动化的严密测试系统**，确保每个功能的代码质量。

## 安装和使用

安装ZStack极为便捷。用户可按需选择不同的安装模式安装首套ZStack环境。

* 如需快速尝试，请参阅[快速安装手册](https://www.zstack.io/help/tutorials/quick_install_guide/v5/)。
* 如需部署生产环境，请参阅[手动安装手册](https://www.zstack.io/help/product_manuals/user_guide/v5/)。
* 如需部署多管理节点环境，请参阅[多管理节点安装手册](https://www.zstack.io/help/tutorials/double_mn_ha_solution/v5/)。

## ZStack架构设计

#### 伸缩性

* [ZStack弹性架构揭秘](https://res.zstack.io/assets/pdf/08.pdf)
  -  异构架构
  -  无状态服务架构
  -  无锁架构

#### 插件架构

* [ZStack插件架构](https://res.zstack.io/assets/pdf/09.pdf)
  - 进程内微服务架构
  - 通用插件系统
  - 工作流引擎

#### 资源操作框架

* [标签系统/级联框架/查询API](https://res.zstack.io/assets/pdf/10.pdf)

#### 整体技术架构概述
* [技术架构概述](https://www.zstack.io/help/product_manuals/white_paper/v5/)

## 社区交流
* 加入QQ群，共同探讨和分享对ZStack的建议、使用心得、发展方向等。QQ群号：一群（410185063）、二群（443027683）、三群（741300236）、四群（1046295840）、五群（1071894823）、六群（1012034825）

## 参与贡献
#### 问题反馈
1. 提交Issue或通过QQ群反馈问题
2. 描述如何重现该问题（可选）
3. 可提供解决方案（可选）
4. 提交PR以解决问题（可选）

#### 功能需求
1. 提交Issue或通过QQ群反馈新功能需求及原因
2. 指出这个功能的实现方案（可选）
3. 提出PR实现这个新的功能（可选）

#### 代码贡献
1. 参考[快速编译手册](https://gitee.com/zstackio/zstack-utility/blob/master/zstackbuild/README.md) 准备一个开发环境
2. 提交PR请求根据社区反馈进行完善

感谢以下小伙伴对本仓库的贡献和反馈[社区贡献榜](https://gitee.com/zstackio/zstack/blob/master/CONTRIBUTORS)！

## 许可证

根据Apache许可证2.0版本（"许可证"）授权，为正常使用该服务，请确保许可证与本文件兼容。用户可通过以下链接获得许可证副本：

http://www.apache.org/licenses/LICENSE-2.0

除非适用法律要求或以书面形式约定，该许可证分发的软件将按“原样”提供，无任何明示或暗示的保证或条件。请参阅该许可证，通过特定语言了解具体权限和限制。
