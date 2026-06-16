# ZBS vhost — ISO 离线交付 SPDK 镜像（设计文档）

> 状态：设计稿（2026-06-16，第 2 版，按 distro 真实交付链 + MinIO 先例重写）。
> 只描述方案，不含已实现代码。实现跨多仓，见 §4。

## 1. 问题

ZBS vhost 的 SPDK target 是一个**按架构编译的 docker 镜像**（`zbs-vhost-x86_64` / 将来
`zbs-vhost-aarch64`），运行时落到 KVM 主机起 `vhost-user-blk`。当前**开箱即死路**：

- `Zbs.vhost.targetImage` 默认 `zbs-vhost:latest`（无 registry 前缀 → agent
  `image_registry()` 返回 None → docker pull 跳过）。
- `Zbs.vhost.targetImageTar` / `Zbs.vhost.targetImageUrl` 默认空。
- 三个都无效来源 → agent `load_image` 末尾抛 `absent`。

env1/2/3 能跑全靠**手工**把 property 配成内网 registry ref
`172.26.208.212:5000/zbs-vhost-x86_64:<tag>`。客户现场 ISO 装机无此 registry，必须随安装介质离线交付。

### 下载链路索引（code-verified）

```
ZbsStorageController.activateVhostVolume (:160)
  → fillVhostTargetParams (:195)        # 填 image/tar/url 进 VhostActivateCmd
  → VHOST_ACTIVATE_PATH → zbs_storage_plugin.py → zbs_vhost_target.ensure_target
  → load_image(image, image_tar, image_url):
       image_present?  → 有则返回
       image_registry(image)?  → docker pull（仅 registry ref 走；无斜杠 ref 跳过）
       local tar 存在?  → docker load -i <tar>
       image_url?  → download_image(curl -fSL --retry) → docker load
       否则  → 抛 absent
```
**agent 侧 tar/url 兜底已实现且正确，不改 agent。** 缺的是「把 tar 离线交付到主机可达的 url」。

## 2. 各物件现状与归属（全部 verified）

| 物件 | 位置 |
|---|---|
| 镜像源码 | `zstackio/spdk`（branch `zbs-vhost-release`，含 `docker/`） |
| 构建好的镜像 | registry `172.26.208.212:5000/zbs-vhost-<arch>:<tag>`（按 arch） |
| **镜像 tar（docker save）** | **目前不存在任何 repo** → 本方案新建，**不进 git** |
| bin/ISO 组装 | `zstackio/zstack-distro`（`build_zstack_iso.sh` + `mkiso/`，RPM SPECS） |
| MN webapps 部署 | `zstack-utility` 的 `zstack-ctl`：`unzip war -d webapps/zstack`（ctl.py:8424）+ install 时建 `webapps/zstack/static/...` 软链（ctl.py:4396 先例） |

## 3. 方案 —— 照搬 baremetal2 镜像交付先例

distro 已有成熟先例：bm2 的 PXE 镜像 / agent 二进制**不进 git**，放
`minio.zstack.io:9001`，`build_zstack_iso.sh` 构建期 `wget` 进 `mkiso/`
（`get_bm2_pxe_imgs` L644-648 / `get_bm2_instance_agents` L637-641，每 arch 分支都调）。
zbs-vhost 镜像 tar 走**同一条路**。

### ① 镜像 tar 上 MinIO（不进 git）
`zstackio/spdk` 出镜像后（或一次性手工）：
```
docker save 172.26.208.212:5000/zbs-vhost-x86_64:<tag> | gzip > zbs-vhost-x86_64-<tag>.tar.gz
# 上传，按 arch 分目录（仿 bm2）
minio.zstack.io:9001/download/zbs-vhost/x86/zbs-vhost-x86_64-<tag>.tar.gz
minio.zstack.io:9001/download/zbs-vhost/arm/zbs-vhost-aarch64-<tag>.tar.gz
```
体积：save+gzip ≈ 83MB/arch。**镜像分架构，不通用**（SPDK/DPDK 原生二进制；tag 含 arch）。

### ② build_zstack_iso.sh 构建期 wget（zstack-distro）
仿 `get_bm2_pxe_imgs`，加：
```bash
ZBS_VHOST_IMG_X86='http://minio.zstack.io:9001/download/zbs-vhost/x86/zbs-vhost-x86_64-<tag>.tar.gz'
ZBS_VHOST_IMG_ARM='http://minio.zstack.io:9001/download/zbs-vhost/arm/zbs-vhost-aarch64-<tag>.tar.gz'
get_zbs_vhost_image() {
  mkdir -p mkiso/zbs-vhost
  # 按目标 arch 选 link
  wget -c $ZBS_VHOST_IMG_X86 -O mkiso/zbs-vhost/zbs-vhost-x86_64-<tag>.tar.gz
}
```
在各 arch 主流程分支调 `get_zbs_vhost_image`（仿 L1280-1351 调 bm2 那两个函数）。
tar 随 ISO/bin 进介质。

### ③ 安装期落到 MN webapps（zstack-ctl，zstack-utility）
install/upgrade 时把交付 tar 放到 MN 的 8080 可 serve 路径。复用现成 static 机制
（ctl.py:4396 建 `webapps/zstack/static/...` 的先例）：落到
`apache-tomcat/webapps/zstack/static/zbs-vhost/zbs-vhost-<arch>-<tag>.tar.gz`
→ url = `http://{MN IP}:8080/zstack/static/zbs-vhost/<tar>`。
（不放 war 平级独立目录，因 install.sh 不会自动建那种目录；static 下有现成先例。）

### ④ MN 运行时推导 url（zstack repo，本仓可实现+env3 验证）
`ZbsStorageController.fillVhostTargetParams`（:195）：
- `Zbs.vhost.targetImageUrl` 非空 → 用它（保留手工覆盖）。
- 否则扫 `webapps/zstack/static/zbs-vhost/`，存在 tar →
  `cmd.imageUrl = "http://" + Platform.getManagementServerIp() + ":8080/zstack/static/zbs-vhost/" + <tar>`。
  - `Platform.getManagementServerIp()`（本插件 ZbsPrimaryStorageMdsBase.java:115 已用）取当前 MN IP；
    多 MN 各自带 tar，url 指本节点。
  - 运行时推导原因：`@GlobalProperty(defaultValue)` 是编译期常量填不了 IP；安装期写死怕 MN IP 变/多 MN。
- 目录无 tar → 回退现状（registry ref / 报 absent）。

### ⑤ targetImage 默认改无-registry 带 tag ref（zstack repo）
`ZbsGlobalProperty.java:26` `zbs-vhost:latest` → `zbs-vhost-<arch>:<tag>`。无 registry →
跳 pull → 走 ④ 的本地 url `docker load`。`docker save` 时的 repo:tag 必须与此一致
（否则 `image_present` 判不出 / load 后名对不上）。升级 = 换 MinIO tar + 同步改 tag。

## 4. 跨仓改动清单

| 仓 | 改动 | 本会话能否实现+验证 |
|---|---|---|
| `zstack`（本仓） | `fillVhostTargetParams` url 推导（④）+ `ZbsGlobalProperty` 默认值（⑤） | ✅ 可，env3 模拟验证 |
| `zstack-utility` | zstack-ctl install/upgrade 把 tar 落到 `webapps/zstack/static/zbs-vhost/`（③） | ✅ 可改，真验证需走安装流程 |
| `zstack-distro` | `build_zstack_iso.sh` 加 `get_zbs_vhost_image` + 各 arch 调用（②） | ⚠️ 可出 diff，构建验证需 distro 构建环境 |
| MinIO 运维 | 上传 tar（①），spdk 侧 docker save | ⚠️ 仓外操作 |

## 5. 边界 / 风险

- **架构分离**：镜像非全平台通用，tag 含 `-x86_64`/`-aarch64`，MinIO 按 arch 分目录，
  build 按目标 arch 选 link，④⑤ 的默认值/扫描也按 arch（运行时 agent 主机 arch 与镜像须一致）。
- **多 MN**：url 取当前处理请求的 MN IP；每 MN 介质自带 tar。
- **升级 vs ISO**：tar 随 bin（MN 包），不只进 ISO，否则升级后镜像版本与 agent 代码漂移。
- **agent 小坑（follow-up）**：`load_image` 走 url 分支下载的 tmp tar 用完不删
  （`/var/lib/zstack/zbs-vhost-image.tar`）→ 实现阶段顺手在 `docker load` 后清理。
- bm2 先例里 tar 进 `mkiso/` 之后如何被 ks/%post 消费没追到底；我们 MN 落点（③ static）自定义、
  不依赖 bm2 那条消费链。

## 6. 验证（实现阶段）

env3（172.24.194.114）模拟离线现场：
1. 手工放 `zbs-vhost-x86_64-<tag>.tar.gz` 到 MN `apache-tomcat/webapps/zstack/static/zbs-vhost/`。
2. 清空 `Zbs.vhost.targetImageUrl` + 把 `Zbs.vhost.targetImage` 改无-registry `zbs-vhost-x86_64:<tag>`，重启 MN。
3. 某 docker 主机 `docker rmi` 掉镜像，起一个 vhost 根盘 VM。
4. 看 agent 日志：跳过 pull → `download_image` 从 `http://{MN IP}:8080/zstack/static/zbs-vhost/<tar>`
   拉 → `docker load` → 容器起 → VM Running。
5. `curl http://{MN IP}:8080/zstack/static/zbs-vhost/<tar>` 确认 8080 静态服务通。
