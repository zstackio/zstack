# ZStack SDK Go 开发规范与标准

> **仓库地址**: ssh://git@dev.zstack.io:9022/zstackio/zsphere-go-sdk.git

---

## Quick Start

### 安装

```bash
go get github.com/zstackio/zsphere-sdk-go-v2
```

### 初始化客户端

> **注意**: `DefaultZSConfig` 需要显式传入 context path (如 `"/zstack"`)。

```go
package main

import (
    "fmt"
    "github.com/zstackio/zsphere-sdk-go-v2/pkg/client"
    "github.com/zstackio/zsphere-sdk-go-v2/pkg/param"
)

func main() {
    // 方式一：使用 AccessKey 认证（推荐）
    cli := client.NewZSClient(
        client.DefaultZSConfig("192.168.1.100", "/zstack").
            AccessKey("your-access-key-id", "your-access-key-secret").
            Debug(true),
    )

    // 方式二：使用账号密码认证（明文密码，客户端自动 SHA512 加密）
    cli := client.NewZSClient(
        client.DefaultZSConfig("192.168.1.100", "/zstack").
            Login("admin", "password").  // ✅ 传入明文密码即可
            Debug(true),
    )
}
```

### 查询虚拟机列表

```go
// 查询所有虚拟机
vms, err := cli.QueryVmInstance(ctx, param.NewQueryParam())
if err != nil {
    panic(err)
}

for _, vm := range vms {
    fmt.Printf("VM: %s, UUID: %s, State: %s\n", vm.Name, vm.UUID, vm.State)
}
```

### 创建虚拟机

```go
createParam := param.CreateVmInstanceParam{
    BaseParam: param.BaseParam{},
    Params: param.CreateVmInstanceDetailParam{
        Name:               "my-vm",
        InstanceOfferingUUID: "offering-uuid",
        ImageUUID:          "image-uuid",
        L3NetworkUuids:     []string{"l3-network-uuid"},
        Description:        "Created by Go SDK",
    },
}

vm, err := cli.CreateVmInstance(ctx, createParam)
if err != nil {
    panic(err)
}
fmt.Printf("Created VM: %s\n", vm.UUID)
```

### 获取单个资源

```go
// 根据 UUID 获取虚拟机详情
vm, err := cli.GetVmInstance(ctx, "vm-uuid-here")
if err != nil {
    panic(err)
}
fmt.Printf("VM Name: %s, CPU: %d, Memory: %d\n", vm.Name, vm.CPUNum, vm.MemorySize)
```

### 条件查询

```go
// 使用条件查询
params := param.NewQueryParam().
    AddQ("state=Running").
    AddQ("name~=test").  // 模糊匹配
    Limit(10).
    Start(0)

vms, err := cli.QueryVmInstance(ctx, params)
```

### 删除资源

```go
// 删除虚拟机（宽松模式）
err := cli.DestroyVmInstance(ctx, "vm-uuid", param.Permissive)
if err != nil {
    panic(err)
}
```

### 错误处理

```go
import "github.com/zstackio/zsphere-sdk-go-v2/pkg/errors"

vm, err := cli.GetVmInstance(ctx, "non-existent-uuid")
if err != nil {
    if err == errors.ErrNotFound {
        fmt.Println("VM not found")
    } else {
        fmt.Printf("Error: %v\n", err)
    }
}
```

---

## 1. 项目结构

```
zsphere-sdk-go-v2/
├── pkg/
│   ├── client/          # API 客户端和操作方法
│   ├── param/           # 请求参数结构体
│   ├── view/            # 响应视图结构体
│   ├── errors/          # 错误定义和处理
│   ├── util/            # 通用工具包
│   │   ├── jsonutils/   # JSON 处理
│   │   ├── httputils/   # HTTP 工具
│   │   └── ...          # 其他工具
│   └── integration-test/           # 集成测试
├── go.mod
├── go.sum
└── README.md
```

### 包职责说明

| 包名       | 职责         | 命名规范                     |
|----------|------------|--------------------------|
| `client` | API 操作方法实现 | `{resource}_actions.go`  |
| `param`  | 请求参数定义     | `{resource}_params.go`   |
| `view`   | 响应数据结构     | `{resource}_views.go`    |
| `errors` | 错误类型定义     | `errors.go`, `consts.go` |
| `util`   | 通用工具函数     | 按功能划分子包                  |

---

## 2. 代码规范

### 2.1 文件头部

**所有 Go 文件必须包含版权声明：**

```go
// Copyright (c) ZStack.io, Inc.

package packagename
```

### 2.2 导入顺序

按以下顺序组织导入，组间用空行分隔：

```go
import (
    // 1. 标准库
    "context"
    "fmt"
    "net/http"

    // 2. 第三方库
    "github.com/kataras/golog"

    // 3. 项目内部包
    "github.com/zstackio/zsphere-sdk-go-v2/pkg/errors"
    "github.com/zstackio/zsphere-sdk-go-v2/pkg/param"
    "github.com/zstackio/zsphere-sdk-go-v2/pkg/view"
)
```

---

## 3. 命名规范

### 3.1 文件命名

| 类型      | 格式                      | 示例                       |
|---------|-------------------------|--------------------------|
| Actions | `{resource}_actions.go` | `vm_instance_actions.go` |
| Params  | `{resource}_params.go`  | `vm_instance_params.go`  |
| Views   | `{resource}_views.go`   | `vm_instance_views.go`   |
| Tests   | `{resource}_test.go`    | `vm_instance_test.go`    |

### 3.2 类型命名

```go
// 参数结构体：{Action}{Resource}Param
type CreateVmInstanceParam struct { ... }
type UpdateVmInstanceParam struct { ... }

// 详细参数：{Action}{Resource}DetailParam
type CreateVmInstanceDetailParam struct { ... }

// 视图结构体：{Resource}InventoryView 或 {Resource}View
type VmInstanceInventoryView struct { ... }
type VMConsoleAddressView struct { ... }

// 类型别名
type DeleteMode string
type InstanceType string
```

### 3.3 方法命名

```go
// CRUD 操作
func (cli *ZSClient) Create{Resource}(ctx, params) (*View, error)
func (cli *ZSClient) Query{Resource}(ctx, params *QueryParam) ([]View, error)
func (cli *ZSClient) Get{Resource}(ctx, uuid) (*View, error)
func (cli *ZSClient) Update{Resource}(ctx, uuid, params) (*View, error)
func (cli *ZSClient) Destroy{Resource}(ctx, uuid, deleteMode) error
func (cli *ZSClient) Delete{Resource}(ctx, uuid, deleteMode) error

// 特定操作
func (cli *ZSClient) Start{Resource}(ctx, uuid, params) (*View, error)
func (cli *ZSClient) Stop{Resource}(ctx, uuid, params) (*View, error)
func (cli *ZSClient) Attach{A}To{B}(ctx, aUUID, bUUID) (*View, error)
func (cli *ZSClient) Detach{A}From{B}(ctx, aUUID, bUUID) (*View, error)
```

### 3.4 常量命名

```go
// 使用类型别名定义枚举
type InstanceType string

const (
    UserVm      InstanceType = "UserVm"
    ApplianceVm InstanceType = "ApplianceVm"
)

// 错误常量
const (
    ErrNotFound    = Error("NotFoundError")
    ErrDuplicateId = Error("DuplicateIdError")
)
```

---

## 4. 结构体设计模式

### 4.1 基础结构体嵌入

**View 结构体使用嵌入共享通用字段：**

```go
// 基础信息视图
type BaseInfoView struct {
    UUID        string `json:"uuid"`
    Name        string `json:"name"`
    Description string `json:"description"`
}

// 时间信息视图（使用 ZStackTime 支持特殊格式）
type BaseTimeView struct {
    CreateDate ZStackTime `json:"createDate"`  // ZStack 时间格式："Jan 2, 2006 3:04:05 PM"
    LastOpDate ZStackTime `json:"lastOpDate"`
}

// ZStackTime 自定义类型（仅在 view 包）
type ZStackTime struct {
    time.Time
}

func (t *ZStackTime) UnmarshalJSON(data []byte) error {
    // 支持 ZStack 的 "Oct 28, 2025 2:09:26 PM" 格式
    // 以及标准 RFC3339 格式
}

// 资源视图嵌入基础结构体
type VmInstanceInventoryView struct {
    BaseInfoView
    BaseTimeView

    ZoneUUID    string `json:"zoneUuid"`
    ClusterUUID string `json:"clusterUuid"`
    // ... 其他字段
}
```

### 4.2 参数结构体嵌入

```go
// 基础参数
type BaseParam struct {
    SystemTags []string `json:"systemTags,omitempty"`
    UserTags   []string `json:"userTags,omitempty"`
    RequestIp  string   `json:"requestIp,omitempty"`
}

// 请求参数嵌入基础参数
type CreateVmInstanceParam struct {
    BaseParam
    Params CreateVmInstanceDetailParam `json:"params"`
}

// 详细参数：可选字段使用指针类型
type CreateVmInstanceDetailParam struct {
    Name               string  `json:"name" validate:"required"`        // 必填
    InstanceOfferingUuid string `json:"instanceOfferingUuid" validate:"required"` // 必填
    ImageUuid          string  `json:"imageUuid" validate:"required"`   // 必填
    L3NetworkUuids     []string `json:"l3NetworkUuids" validate:"required"` // 必填
    Description        *string `json:"description,omitempty"`           // 可选，使用指针
    DefaultL3NetworkUuid *string `json:"defaultL3NetworkUuid,omitempty"` // 可选，使用指针
    Strategy           *string `json:"strategy,omitempty"`              // 可选，使用指针
}
```

**使用示例**：

```go
// 只设置必填字段，省略可选字段
params := CreateVmInstanceDetailParam{
    Name: "my-vm",
    InstanceOfferingUuid: "offering-uuid",
    ImageUuid: "image-uuid",
    L3NetworkUuids: []string{"network-uuid"},
    // Description 为 nil，不会发送到服务器
}

// 设置可选字段
desc := "Test VM"
strategy := "InstantStart"
params := CreateVmInstanceDetailParam{
    Name: "my-vm",
    InstanceOfferingUuid: "offering-uuid",
    ImageUuid: "image-uuid",
    L3NetworkUuids: []string{"network-uuid"},
    Description: &desc,      // 使用指针
    Strategy: &strategy,     // 使用指针
}
```

```

### 4.3 Builder 模式 (方法链)

```go
// 配置构建器
func DefaultZSConfig(hostname, contextPath string) *ZSConfig {
    return NewZSConfig(hostname, defaultZStackPort, contextPath)
}

func (config *ZSConfig) AccessKey(id, secret string) *ZSConfig {
    config.accessKeyId = id
    config.accessKeySecret = secret
    config.authType = AuthTypeAccessKey
    return config
}

func (config *ZSConfig) Debug(debug bool) *ZSConfig {
    config.debug = debug
    return config
}

// 使用方式
client := client.NewZSClient(
    client.DefaultZSConfig("10.0.0.1", "/zstack").
        AccessKey("key-id", "key-secret").
        Debug(true),
)

// 查询参数构建器
params := param.NewQueryParam().
    AddQ("name=test").
    Limit(10).
    Start(0)
```

---

## 5. JSON 标签规范

### 5.1 字段标签

```go
type ExampleStruct struct {
    // 必填字段：无 omitempty
    UUID string `json:"uuid"`

    // 可选字段：使用 omitempty
    Description string `json:"description,omitempty"`

    // 指针类型用于区分零值和未设置
    RootDiskSize *int64 `json:"rootDiskSize"`
    CpuNum       *int   `json:"cpuNum"`

    // 时间字段使用 ZStackTime（view 包）或 time.Time（param 包）
    CreateDate ZStackTime `json:"createDate"`  // view 包
    StartTime  time.Time  `json:"startTime"`   // param 包

    // byte 数组使用 []int8（Java byte 是有符号的）
    IpInBinary []int8 `json:"ipInBinary,omitempty"`
}
```

### 5.2 字段注释

**所有导出字段必须有中文注释说明：**

```go
type VmInstanceInventoryView struct {
    UUID             string `json:"uuid"`             // 资源UUID，唯一标识
    ZoneUUID         string `json:"zoneUuid"`         // 区域UUID
    ClusterUUID      string `json:"clusterUuid"`      // 集群UUID
    MemorySize       int64  `json:"memorySize"`       // 内存大小（字节）
    CPUNum           int    `json:"cpuNum"`           // CPU数量
}
```

---

## 6. 错误处理规范

### 6.1 错误定义

```go
// 使用自定义错误类型
type Error string

func (e Error) Error() string {
    return string(e)
}

// 预定义错误常量
const (
    ErrNotFound    = Error("NotFoundError")
    ErrDuplicateId = Error("DuplicateIdError")
    ErrParameter   = Error("ParameterError")
)
```

### 6.2 错误包装

```go
import "github.com/zstackio/zsphere-sdk-go-v2/pkg/errors"

// 使用 Wrap 添加上下文
if err != nil {
    return errors.Wrap(err, "failed to create vm instance")
}

// 使用 Wrapf 格式化上下文
if err != nil {
    return errors.Wrapf(err, "failed to query %s", resource)
}
```

### 6.3 API 方法错误处理

```go
func (cli *ZSClient) GetVmInstance(ctx context.Context, uuid string) (*view.VmInstanceInventoryView, error) {
    var resp view.VmInstanceInventoryView
    if err := cli.Get(ctx, "v1/vm-instances", uuid, nil, &resp); err != nil {
        return nil, err  // 直接返回错误，由调用方处理
    }
    return &resp, nil
}
```

---

## 7. API 方法实现规范

### 7.1 标准方法模板

```go
// {Description} 方法描述
func (cli *ZSClient) {MethodName}(ctx, params...) (*view.{ReturnType}, error) {
    var resp view.{ReturnType}
    if err := cli.{HttpMethod}(ctx, "v1/{resource}", params, &resp); err != nil {
        return nil, err
    }
    return &resp, nil
}
```

### 7.2 完整示例

```go
// CreateVmInstance 创建虚拟机实例
func (cli *ZSClient) CreateVmInstance(ctx context.Context, params param.CreateVmInstanceParam) (*view.VmInstanceInventoryView, error) {
    resp := view.VmInstanceInventoryView{}
    if err := cli.Post(ctx, "v1/vm-instances", params, &resp); err != nil {
        return nil, err
    }
    return &resp, nil
}

// QueryVmInstance 查询虚拟机实例列表
func (cli *ZSClient) QueryVmInstance(ctx context.Context, params *param.QueryParam) ([]view.VmInstanceInventoryView, error) {
    var resp []view.VmInstanceInventoryView
    return resp, cli.List(ctx, "v1/vm-instances", params, &resp)
}

// DestroyVmInstance 删除虚拟机实例
func (cli *ZSClient) DestroyVmInstance(ctx context.Context, uuid string, deleteMode param.DeleteMode) error {
    return cli.Delete(ctx, "v1/vm-instances", uuid, string(deleteMode))
}
```

---

## 8. 测试规范

### 8.1 测试文件位置

测试文件放在 `pkg/integration-test/` 目录下，命名格式：`{resource}_test.go`

### 8.2 测试函数命名

```go
func Test{MethodName}(t *testing.T) {
    // 测试实现
}
```

### 8.3 测试模板

```go
// Copyright (c) ZStack.io, Inc.

package test

import (
    "context"
    "testing"

    "github.com/kataras/golog"

    "github.com/zstackio/zsphere-sdk-go-v2/pkg/param"
    "github.com/zstackio/zsphere-sdk-go-v2/pkg/util/jsonutils"
)

func TestQueryVmInstance(t *testing.T) {
    ctx := context.Background()
    data, err := accessKeyAuthCli.QueryVmInstance(ctx, param.NewQueryParam())
    if err != nil {
        t.Errorf("TestQueryVmInstance: %v", err)
    }
    golog.Info(jsonutils.Marshal(data))
}

func TestGetVmInstance(t *testing.T) {
    ctx := context.Background()
    data, err := accountLoginCli.GetVmInstance(ctx, "uuid-here")
    if err != nil {
        t.Errorf("TestGetVmInstance: %v", err)
    }
    golog.Info(jsonutils.Marshal(data))
}
```

---

## 9. 新增资源开发流程

当需要添加新的 ZStack 资源支持时，按以下步骤进行：

### 步骤 1：定义视图结构体

在 `pkg/view/{resource}_views.go` 中定义：

```go
// Copyright (c) ZStack.io, Inc.

package view

type {Resource}InventoryView struct {
    BaseInfoView
    BaseTimeView

    // 资源特定字段
    Field1 string `json:"field1"` // 字段说明
    Field2 int    `json:"field2"` // 字段说明
}
```

### 步骤 2：定义参数结构体

在 `pkg/param/{resource}_params.go` 中定义：

```go
// Copyright (c) ZStack.io, Inc.

package param

type Create{Resource}Param struct {
    BaseParam
    Params Create{Resource}DetailParam `json:"params"`
}

type Create{Resource}DetailParam struct {
    Name        string `json:"name"`        // 名称
    Description string `json:"description"` // 描述
    // 其他参数
}
```

### 步骤 3：实现 API 方法

在 `pkg/client/{resource}_actions.go` 中实现：

```go
// Copyright (c) ZStack.io, Inc.

package client

import (
    "context"

    "github.com/zstackio/zsphere-sdk-go-v2/pkg/param"
    "github.com/zstackio/zsphere-sdk-go-v2/pkg/view"
)

// Create{Resource} 创建资源
func (cli *ZSClient) Create{Resource}(ctx context.Context, params param.Create{Resource}Param) (*view.{Resource}InventoryView, error) {
    resp := view.{Resource}InventoryView{}
    if err := cli.Post(ctx, "v1/{resources}", params, &resp); err != nil {
        return nil, err
    }
    return &resp, nil
}

// Query{Resource} 查询资源列表
func (cli *ZSClient) Query{Resource}(ctx context.Context, params *param.QueryParam) ([]view.{Resource}InventoryView, error) {
    var resp []view.{Resource}InventoryView
    return resp, cli.List(ctx, "v1/{resources}", params, &resp)
}

// Get{Resource} 获取单个资源
func (cli *ZSClient) Get{Resource}(ctx context.Context, uuid string) (*view.{Resource}InventoryView, error) {
    var resp view.{Resource}InventoryView
    if err := cli.Get(ctx, "v1/{resources}", uuid, nil, &resp); err != nil {
        return nil, err
    }
    return &resp, nil
}

// Destroy{Resource} 删除资源
func (cli *ZSClient) Destroy{Resource}(ctx context.Context, uuid string, deleteMode param.DeleteMode) error {
    return cli.Delete(ctx, "v1/{resources}", uuid, string(deleteMode))
}
```

### 步骤 4：编写测试

在 `pkg/integration-test/{resource}_test.go` 中编写集成测试。

---

## 11. SDK 生成器改进规则 (2026-01)

### 11.1 时间类型处理优化

**规则**: 移除自定义 `ZStackTime` 类型，统一使用 Go 原生 `time.Time`

**原因**:

- 特殊时间格式解析应由独立的 utils 库处理，保持 SDK 代码简洁
- 减少生成代码的复杂度和维护成本
- 用户可根据需要选择时间处理工具

**修改**:

```go
// 旧实现（移除）
type ZStackTime struct { time.Time }
func (t *ZStackTime) UnmarshalJSON(data []byte) error { ... }

// 新实现
type VmInstanceInventoryView struct {
	BaseTimeView
    // ...
}
```

**影响范围**:

- `view` 包所有视图结构体
- `param` 包的参数结构体

---

### 11.2 异步操作方法自动生成

**规则**: 为支持 LongJob 的资源自动生成 `Add{Resource}Async` 方法

**识别机制**:
通过 `@LongJobFor` 注解识别支持异步操作的 API：

```java

@LongJobFor(APIBackupStorageMigrateImageMsg.class)
public class BackupStorageMigrateImageLongJob { ...
}
```

**生成方法模板**:

```go
// AddVmInstanceAsync 异步创建虚拟机
// 返回 LongJob UUID 用于查询执行状态
func (cli *ZSClient) AddVmInstanceAsync(ctx context.Context, params param.CreateVmInstanceParam) (string, error) {
    var resp struct {
        Location string `json:"location"`  // LongJob UUID
    }
    if err := cli.Post(ctx, "v1/vm-instances", params, &resp); err != nil {
        return "", err
    }
    // 从 Location header 提取 LongJob UUID
    return extractLongJobUuid(resp.Location), nil
}
```

**实现要点**:

1. 解析 `@LongJobFor` 注解获取目标 API 类
2. 为目标 API 生成对应的 Async 方法
3. 返回值为 LongJob UUID (string)
4. 用户可通过 `QueryLongJob(uuid)` 查询执行状态

---

### 11.3 资源查询方法增强

**规则**: 每个资源增加 `Get{Resource}(uuid)` 单参数查询方法

**目的**:

- 简化最常见的单资源查询场景
- 区分列表查询 `Query{Resource}(params)` 和单资源查询 `Get{Resource}(uuid)`

**生成规则**:

```go
// Get{Resource} 根据 UUID 获取单个资源
func (cli *ZSClient) Get{Resource}(ctx context.Context, uuid string) (*view.{Resource}InventoryView, error) {
    var resp view.{Resource}InventoryView
    if err := cli.Get(ctx, "v1/{resources}", uuid, nil, &resp); err != nil {
        return nil, err
    }
    return &resp, nil
}
```

**与 Query 的区别**:

- `Get{Resource}(uuid)` - 获取单个资源，返回 `*View`
- `Query{Resource}(params)` - 查询资源列表，返回 `[]View`

---

### 11.4 指针类型优化

**规则**: 可选字段使用指针类型，方便 nil 检查和区分零值

**适用场景**:

```go
type VmInstanceInventoryView struct {
    // 必填字段 - 不使用指针
    UUID string `json:"uuid"`
    Name string `json:"name"`

    // 可选字段 - 使用指针
    Description *string `json:"description,omitempty"`
    ZoneUUID    *string `json:"zoneUuid,omitempty"`

    // 数值字段 - 使用指针区分 0 和未设置
    CPUNum      *int   `json:"cpuNum,omitempty"`
    MemorySize  *int64 `json:"memorySize,omitempty"`
}
```

**判断规则**:

1. 有 `omitempty` 标签的字段使用指针
2. 数值类型（int/int64/float64）如果可选，必须使用指针
3. 字符串类型如果可选，使用指针
4. 必填字段不使用指针

**使用示例**:

```go
vm, _ := cli.GetVmInstance(ctx, "uuid")
if vm.Description != nil {
    fmt.Println(*vm.Description)
}
```

---

### 11.5 基础结构体简化

**规则**: 基础结构体仅包含 `uuid` 和 `name` 字段

**新的 BaseInfoView 定义**:

```go
// BaseInfoView 基础信息视图（仅包含通用标识字段）
type BaseInfoView struct {
    UUID string  `json:"uuid"`           // 资源唯一标识
    Name string `json:"name,omitempty"` // 资源名称
}
```

**继承规则**:

- 资源有 `uuid` 和 `name` 字段 → 继承 `BaseInfoView`
- 资源只有 `uuid` 字段 → 不继承，直接定义 `UUID` 字段
- 资源没有 `uuid` 字段 → 不继承

**移除字段**:

- ❌ `Description` - 移入各资源自己的结构体
- ❌ `CreateDate` / `LastOpDate` - 移除 `BaseTimeView`，各资源自己定义

**示例**:

```go
// 继承 BaseInfoView
type VmInstanceInventoryView struct {
    BaseInfoView
    Description *string    `json:"description,omitempty"`
    CreateDate  time.Time  `json:"createDate,omitempty"`
    // ...
}

// 不继承（资源无 name 字段）
type SessionInventoryView struct {
    UUID       string    `json:"uuid"`
    CreateDate time.Time `json:"createDate,omitempty"`
    // ...
}
```

---

### 11.6 多参数路径支持（Query API）

**规则**: Query API 自动检测 URL 路径中的占位符，支持多参数路径

**背景**:

- 大多数 Query API 使用单一 `{uuid}` 参数
- 部分资源使用复合键，如 GlobalConfig 使用 `{category}/{name}`

**检测逻辑**:

```groovy
// 提取 URL 占位符
def placeholders = extractUrlPlaceholders(apiPath)
// 例如："/global-configurations/{category}/{name}" → ["category", "name"]

if (placeholders.size() >= 2) {
    // 生成多参数方法，使用 GetWithSpec
} else {
    // 生成标准单参数方法
}
```

**生成示例**:

```go
// 单参数：APIQueryVmInstanceMsg (/vm-instances/{uuid})
func (cli *ZSClient) GetVmInstance(ctx context.Context, uuid string) (*view.VmInstanceInventoryView, error) {
    var resp view.VmInstanceInventoryView
    if err := cli.Get(ctx, "v1/vm-instances", uuid, nil, &resp); err != nil {
        return nil, err
    }
    return &resp, nil
}
```

**影响范围**:

- Query API 的 Get 方法生成
- 非-Query API 的 Get/Update/Delete 方法（如果路径有多个占位符）

---

### 11.7 删除操作 URL 处理

**规则**: 删除操作的 URL 路径不包含占位符 `{uuid}`，由业务逻辑自动拼接

**旧实现**:

```go
// ❌ 旧方式
func (cli *ZSClient) Delete(ctx context.Context, path string, uuid string, deleteMode string) error {
    url := strings.Replace(path, "{uuid}", uuid, 1)  // 手动替换占位符
    // ...
}
```

**新实现**:

```go
// ✅ 新方式
func (cli *ZSClient) Delete(ctx context.Context, resource string, uuid string, deleteMode string) error {
    // 自动拼接 URL: /v1/{resource}/{uuid}
    url := fmt.Sprintf("%s/%s", resource, uuid)
    if deleteMode != "" {
        url += "?deleteMode=" + deleteMode
    }
    // ...
}
```

**生成的 action 方法**:

```go
// 旧方式
cli.Delete(ctx, "v1/vm-instances/{uuid}", uuid, deleteMode)

// 新方式（更简洁）
cli.Delete(ctx, "v1/vm-instances", uuid, deleteMode)
```

**影响范围**:

- 所有 DELETE 操作的 actions 方法
- `client.go` 中的 `Delete()` 方法实现

---

### 11.8 客户端代码生成简化

**规则**: `client.go` 不再由生成器生成，使用固定模板文件

**原因**:

- `client.go` 是基础设施代码，逻辑稳定
- 避免每次生成都覆盖手动优化的实现
- 简化生成器逻辑

**实现方式**:

1. 创建 `client.go.template` 固定模板文件
2. 生成器启动时直接复制模板到输出目录
3. 移除 `GoInventory.generateClientFile()` 方法

**模板文件位置**:

```
rest/src/main/resources/scripts/templates/
└── client.go.template  # 固定的 client.go 实现
```

**生成器调用**:

```groovy
private SdkFile copyClientTemplate() {
    def template = new File("templates/client.go.template")
    def sdkFile = new SdkFile()
    sdkFile.subPath = "/pkg/client/"
    sdkFile.fileName = "client.go"
    sdkFile.content = template.text
    return sdkFile
}
```

---

### 11.8 可选字段指针类型支持（2026-01 新增）

**规则**: 支持 `@APIParam(required = false)` 注解，自动将可选字段生成为指针类型

**目的**:

- 区分未设置（nil）和零值（""、0、false）
- 提供更清晰的 API 语义
- 避免误将零值作为有效输入发送到服务器

**判断逻辑**:

```groovy
private boolean isOptionalField(Field field, Map<String, APIParam> apiParamMap) {
    // 1. uuid 和 name 始终必填
    if (field.name in ["uuid", "name"]) {
        return false
    }

    // 2. 检查 @APIParam(required = false)
    if (field.isAnnotationPresent(APIParam.class)) {
        APIParam param = apiParamMap.containsKey(field.name) ?
                apiParamMap.get(field.name) : field.getAnnotation(APIParam.class)
        return !param.required()
    }

    // 3. 没有APIParam注解的字段默认可选
    return true
}
```

**生成规则**:

```groovy
// 基本类型集合
def basicTypes = ["string", "int", "int64", "int32", "float64", "float32", "bool"] as Set

// 可选的基本类型使用指针
if (isOptional && basicTypes.contains(baseType)) {
    return "*" + baseType  // *string, *int64, *bool 等
}

// slice、map、interface{}、struct 本身已支持 nil，不额外添加指针
return baseType
```

**生成示例**:

```go
// UpdateVmInstanceDetailParam
type UpdateVmInstanceDetailParam struct {
    UUID               string   `json:"uuid" validate:"required"`  // 必填
    Name               *string  `json:"name,omitempty"`            // 可选，使用指针
    Description        *string  `json:"description,omitempty"`     // 可选，使用指针
    DefaultL3NetworkUuid *string `json:"defaultL3NetworkUuid,omitempty"` // 可选，使用指针
    CPUNum             *int     `json:"cpuNum,omitempty"`          // 可选，使用指针
}
```

**使用示例**:

```go
// 只更新 name，不更新 description
name := "new-name"
params := UpdateVmInstanceDetailParam{
    UUID: "vm-uuid",
    Name: &name,        // 设置为新值
    // Description 为 nil，不会包含在 JSON 中
}

// 将 description 设置为空字符串（清空）
emptyDesc := ""
params := UpdateVmInstanceDetailParam{
    UUID: "vm-uuid",
    Description: &emptyDesc,  // 发送 "description": "" 到服务器
}
```

**注意事项**:

1. **方法签名一致性**: 修改 `generateParamFieldGeneric` 和 `generateParamFieldType` 后，必须更新所有调用点
2. **Groovy 类型检查**: 使用 `Set.contains()` 而非 `in` 操作符，避免字符串匹配失败
3. **嵌套类型处理**: `generateParamNestedStruct` 也需要构建 `apiParamMap` 并调用 `isOptionalField`

---

## 12. Go 版本和依赖

- **Go 版本**: 1.22.0+
- **主要依赖**:
    - `github.com/kataras/golog` - 日志
    - `github.com/pkg/errors` - 错误处理
    - `github.com/fatih/color` - 终端颜色
    - `github.com/fatih/structs` - 结构体反射

---
