# ZStack SDK 生成器开发指南

> 本文档总结了 Go SDK 生成器的实现逻辑和经验，用于指导其他语言 SDK 生成器的开发。

---

## 1. 整体架构

### 1.1 核心组件

| 组件            | 文件                     | 职责                                        |
|---------------|------------------------|-------------------------------------------|
| API 模板生成器     | `GoApiTemplate.groovy` | 生成 client actions、param 文件、response views |
| Inventory 生成器 | `GoInventory.groovy`   | 生成 view 结构体、基础文件、client.go                |
| 入口调用          | `RestServer.java`      | 扫描 API 类并调用生成器                            |

### 1.2 生成的文件结构

```
sdk/
├── pkg/
│   ├── client/                    # API 操作方法
│   │   ├── client.go              # 客户端基类
│   │   ├── {resource}_actions.go  # 各资源的 CRUD 方法
│   │   └── ...
│   ├── param/                     # 请求参数
│   │   ├── base_params.go         # 基础参数结构
│   │   ├── base_param_types.go    # 嵌套类型定义
│   │   └── {resource}_params.go   # 各资源的参数
│   ├── view/                      # 响应视图
│   │   ├── base_views.go          # 基础视图结构
│   │   ├── {resource}_views.go    # @Inventory 类生成的视图
│   │   ├── {resource}_additional_views.go  # 被引用但无 @Inventory 的类
│   │   └── {resource}_response_views.go    # API 响应类型
│   └── errors/
│       └── errors.go              # 错误定义
└── go.mod
```

---

## 2. 生成流程

### 2.1 整体流程 (RestServer.java)

```
1. 扫描所有带 @RestRequest 注解的 API Message 类
2. 创建 GoInventory 实例（单例，共享状态）
3. 对每个 API 类：
   a. 创建 GoApiTemplate 实例
   b. 调用 generate() 生成 param、action、response view 文件
4. 调用 GoInventory.generate() 生成：
   a. @Inventory 类的 view 文件
   b. 被引用的 additional view 文件
   c. param 嵌套类型文件
   d. 基础文件（client.go, base_params.go, errors.go）
5. 写入所有文件到输出目录
```

### 2.2 GoApiTemplate 生成流程

```groovy
List<SdkFile> generate() {
    // 1. 解析 API 类信息
    String clzName = parseApiName()        // APICreateVmInstanceMsg -> CreateVmInstance
    String actionType = parseActionType()  // Create, Query, Update, Delete, ...
    String httpMethod = getHttpMethod()    // GET, POST, PUT, DELETE

    // 2. 确定视图类型
    if (isQueryMessage) {
        viewClass = findInventoryClassFromResponse()  // 从 inventories 字段获取
    } else {
        viewClass = responseClass  // 直接使用 responseClass
    }

    // 3. 生成文件
    if (!isQueryMessage) {
        generateParamFile()      // {action}_params.go
    }
    generateActionFile()         // {action}_actions.go
    generateResponseViewFile()   // {action}_response_views.go
}
```

### 2.3 GoInventory 生成流程

```groovy
List<SdkFile> generate() {
    // 1. 生成 @Inventory 类的 view 文件
    generateViewFiles()

    // 2. 生成被引用但无 @Inventory 的类
    generateAdditionalViewFiles()

    // 3. 生成 param 包的嵌套类型
    generateParamNestedTypesFile()

    // 4. 生成基础文件
    generateBaseViewFile()
    generateBaseParamFile()
    generateClientFile()
    generateErrorsFile()
}
```

---

## 3. 类型映射规则

### 3.1 Java → Go 基本类型映射

**注意**: 对于可选字段（`@APIParam(required = false)`），基本类型会生成为指针类型（如 `*string`, `*int64`），以便区分未设置（nil）和零值。

| Java 类型                       | Go 类型（必填）                                 | Go 类型（可选）                                   |
|-------------------------------|-------------------------------------------|-------------------------------------------|
| `String`, `char`, `Character` | `string`                                  | `*string`                                 |
| `int`, `Integer`              | `int`                                     | `*int`                                    |
| `long`, `Long`                | `int64`                                   | `*int64`                                  |
| `short`, `Short`              | `int16`                                   | `*int16`                                  |
| `byte`, `Byte`                | `int8`                                    | `*int8`                                   |
| `float`, `Float`              | `float32`                                 | `*float32`                                |
| `double`, `Double`            | `float64`                                 | `*float64`                                |
| `boolean`, `Boolean`          | `bool`                                    | `*bool`                                   |
| `Date`, `Timestamp`           | `time.Time` (param) / `ZStackTime` (view) | `*time.Time` (param) / `*ZStackTime` (view) |
| `Enum`                        | `string`                                  | `*string`                                 |
| `byte[]`                      | `[]int8` (Java byte 是有符号 -128~127)        | `[]int8`                                   |
| `List<T>`                     | `[]T`                                     | `[]T`                                      |
| `Map<String, V>`              | `map[string]V`                            | `map[string]V`                             |
| 其他复杂类型                        | 生成对应的 struct                              | -                                           |
| 无法识别的类型                       | `interface{}`                             | `interface{}`                              |

### 3.2 上下文感知的类型映射

**重要**: `mapJavaTypeToGoType()` 需要根据上下文返回不同类型：

- **param 包**（请求参数）: `Date/Timestamp` → `time.Time`
- **view 包**（响应解析）: `Date/Timestamp` → `ZStackTime`（自定义类型，支持 ZStack 特殊格式）

```groovy
private String mapJavaTypeToGoType(Class javaType, boolean forView) {
    switch (javaType) {
        case Date.class:
        case java.sql.Timestamp.class:
            return forView ? "ZStackTime" : "time.Time"
            // ...
    }
}
```

**ZStackTime 实现**（仅在 view 包）:

```go
type ZStackTime struct { time.Time }

func (t *ZStackTime) UnmarshalJSON(data []byte) error {
    // 支持多种格式："Jan 2, 2006 3:04:05 PM", "Jan 2, 2006 15:04:05", RFC3339
}
```

### 3.3 类型处理决策树

```
处理 Java 类型 fieldType:
│
├─ 是基本类型？ → 映射到 Go 基本类型（注意 forView 参数）
│   └─ 是可选字段 (@APIParam(required=false))？ → 添加 * 指针前缀
│
├─ 是数组类型 (isArray)？ → `[]` + 递归处理元素类型（slice本身可nil，不额外加*）
│
├─ 集合类型 (Collection)？ → `[]` + 处理泛型参数（slice本身可nil）
│
├─ 是 Map 类型？ → `map[string]` + 处理 value 泛型（map本身可nil）
│
├─ 是枚举？ → `string`
│   └─ 是可选字段？ → `*string`
│
├─ 有 @Inventory 注解？ → 生成对应 View struct（struct可nil，不额外加*）
│
├─ 是可生成的复杂类 (isGeneratableClass)？
│   ├─ 在 view 包 → 添加到 additionalClasses
│   └─ 在 param 包 → 添加到 paramNestedTypes
│
└─ 其他 → `interface{}`（已支持nil）
```

### 3.4 数组类型特殊处理

**byte[] 映射为 []int8 而非 []byte**:

```groovy
if (javaType.isArray()) {
    Class componentType = javaType.getComponentType()
    if (componentType == byte.class || componentType == Byte.class) {
        // Java byte 是有符号类型 (-128 到 127)
        // Go 的 byte 是 uint8 (0-255)
        // Go 的 int8 是有符号 (-128 到 127)
        return "[]int8"  // ⚠️ 不是 []byte
    }
}
```

**实际案例**: `ipInBinary` 字段存储负数值（如 -84），如果用 `[]byte` (即 `[]uint8`) 会导致 JSON 解析错误。

### 3.5 可生成类的判断 (isGeneratableClass)

```groovy
boolean isGeneratableClass(Class clz) {
    if (clz == null) return false
    if (clz.isPrimitive()) return false      // 排除基本类型
    if (clz.isEnum()) return false           // 排除枚举
    if (clz.isInterface()) return false      // 排除接口
    if (clz.isArray()) return false          // 排除数组类型
    if (clz.name.startsWith("java.")) return false   // 排除 Java 标准库
    if (clz.name.startsWith("javax.")) return false  // 排除 javax
    if (clz.name.startsWith("[")) return false       // 排除数组内部表示
    return true
}
```

---

## 4. 关键问题和解决方案

### 4.1 递归类型（自引用）

**问题**：类似 `ErrorCode` 包含 `cause: ErrorCode` 字段，Go 会报 "invalid recursive type"

**解决**：检测自引用并使用指针类型

```groovy
// 追踪当前正在生成的类
private Class currentGeneratingClass = null

String generateViewStruct(Class clazz, String structName) {
    Class previousClass = currentGeneratingClass
    currentGeneratingClass = clazz
    try {
        // 生成字段...
    } finally {
        currentGeneratingClass = previousClass
    }
}

String generateFieldType(Field field, Type type) {
    // 检测自引用
    boolean isSelfReference = (currentGeneratingClass != null && fieldType == currentGeneratingClass)
    String pointerPrefix = isSelfReference ? "*" : ""
    return pointerPrefix + getViewStructName(fieldType)
}
```

**生成结果**：

```go
type ErrorCodeView struct {
    Code    string         `json:"code"`
    Cause   *ErrorCodeView `json:"cause,omitempty"`  // 使用指针
}
```

### 4.2 类型重复生成

**问题**：同一个类可能同时是 `@Inventory` 类又被其他类引用

**解决**：使用 `generatedViewStructs` 集合追踪已生成的类型

```groovy
Set<String> generatedViewStructs = new HashSet<>()

void generateStruct(Class clz) {
    String structName = getViewStructName(clz)
    if (!generatedViewStructs.contains(structName)) {
        generatedViewStructs.add(structName)
        // 实际生成...
    }
}
```

### 4.3 依赖发现（嵌套类型）

**问题**：生成 struct A 时发现引用了 struct B，B 又引用 C...需要递归发现所有依赖

**解决**：使用迭代方式（非递归）处理新发现的类型

```groovy
def classesToProcess = new ArrayList<Class>(initialClasses)
int index = 0

while (index < classesToProcess.size()) {
    Class clz = classesToProcess.get(index)

    // 生成 struct（会发现新的依赖类型，添加到 additionalClasses）
    generateViewStruct(clz, structName)

    // 检查是否有新发现的类型
    additionalClasses.each { Class newClz ->
        if (!classesToProcess.contains(newClz)) {
            classesToProcess.add(newClz)  // 添加到处理队列
        }
    }
    index++
}
```

### 4.4 发现阶段与生成阶段分离

**问题**：发现阶段调用 `generateViewStruct` 可能污染状态，导致实际生成阶段跳过

**解决**：使用单独地集合追踪发现阶段处理的类

```groovy
// 发现阶段：只收集类，不标记为已生成
def discoveredClasses = new HashSet<Class>()
while (index < classesToProcess.size()) {
    Class clz = classesToProcess.get(index)
    if (!discoveredClasses.contains(clz) && !generatedViewStructs.contains(structName)) {
        discoveredClasses.add(clz)
        generateViewStruct(clz, structName)  // 触发依赖发现
        // ...
    }
    index++
}

// 生成阶段：实际写入文件
discoveredClasses.each { Class clz ->
    String structName = getViewStructName(clz)
    if (!generatedViewStructs.contains(structName)) {
        generatedViewStructs.add(structName)
        content.append(generateViewStruct(clz, structName))
    }
}
```

### 4.5 HTTP 方法与导入处理

**问题**：DELETE 方法不返回 view 类型，但仍导入 view 包会报 "imported and not used"

**解决**：根据 HTTP 方法和 action 类型决定导入

```groovy
// 判断是否为删除类操作
boolean isDeleteAction = ["Delete", "Destroy", "Remove", "Expunge", "Detach", "Cleanup"]
        .contains(actionType) || httpMethod == "DELETE"

// 生成导入
content.append("import (\n")
content.append("\t\"pkg/param\"\n")
if (!isDeleteAction) {
    content.append("\t\"pkg/view\"\n")  // 只有非删除操作才导入 view
}
content.append(")\n")
```

### 4.6 可选字段指针类型处理

**问题**：Go中如何区分未设置字段和零值（如空字符串""、0、false）？

**解决**：对可选字段使用指针类型，nil表示未设置，非nil表示已设置

```groovy
/**
 * 判断字段是否为可选字段
 */
private boolean isOptionalField(Field field, Map<String, APIParam> apiParamMap) {
    // 1. uuid 和 name 始终必填
    if (field.name in ["uuid", "name"]) {
        return false
    }

    // 2. 检查 @APIParam(required = false)
    if (field.isAnnotationPresent(APIParam.class)) {
        APIParam param = apiParamMap.containsKey(field.name) ?
                apiParamMap.get(field.name) : field.getAnnotation(APIParam.class)
        if (!param.required()) {
            return true
        }
    }

    // 3. 没有APIParam注解的字段默认可选
    if (!field.isAnnotationPresent(APIParam.class)) {
        return true
    }

    return false
}

/**
 * 生成字段类型（支持可选字段指针）
 */
private String generateParamFieldType(Field field, Type type,
                                      Map<String, APIParam> apiParamMap,
                                      boolean isOptional) {
    String baseType = mapJavaTypeToGoType(fieldType)

    // 基本类型集合
    def basicTypes = ["string", "int", "int64", "int32",
                      "float64", "float32", "bool"] as Set

    // 对可选的基本类型使用指针
    if (isOptional && !baseType.startsWith("[") &&
            !baseType.startsWith("map[") &&
            !baseType.equals("interface{}") &&
            basicTypes.contains(baseType)) {
        return "*" + baseType
    }

    return baseType
}
```

**生成结果**：

```go
type UpdateVmInstanceDetailParam struct {
    UUID        string  `json:"uuid" validate:"required"`  // 必填
    Name        *string `json:"name,omitempty"`            // 可选，使用指针
    Description *string `json:"description,omitempty"`     // 可选，使用指针
    CPUNum      *int    `json:"cpuNum,omitempty"`          // 可选，使用指针
}
```

**使用示例**：

```go
// 只更新 name，不更新 description
name := "new-name"
params := UpdateVmInstanceDetailParam{
    UUID: "vm-uuid",
    Name: &name,  // 设置为新值
    // Description 为 nil，不会发送到服务器
}
```

### 4.7 多参数路径处理（Query API）

**问题**：部分Query API使用复合键而非单一uuid，如GlobalConfig使用`{category}/{name}`

**解决**：自动检测URL占位符数量，多参数时使用`GetWithSpec`方法

```groovy
/**
 * 提取URL中的占位符
 */
private List<String> extractUrlPlaceholders(String url) {
    def placeholders = []
    def matcher = (url =~ /\{([^}]+)\}/)
    while (matcher.find()) {
        placeholders.add(matcher.group(1))
    }
    return placeholders
}

/**
 * 为Query API生成Get方法
 */
private String generateGetMethodForQuery(String apiPath, String viewStructName) {
    def placeholders = extractUrlPlaceholders(apiPath)
    String cleanPath = removePlaceholders(apiPath)

    // 多参数路径：使用 GetWithSpec
    if (placeholders.size() >= 2) {
        String params = placeholders.collect { "${it} string" }.join(", ")
        String firstParam = placeholders[0]
        def remainingPlaceholders = placeholders.drop(1)
        String spec = buildSpecPath(remainingPlaceholders)

        return """
func (cli *ZSClient) Get${resourceName}(ctx context.Context, ${params}) (*view.${viewStructName}, error) {
    var resp view.${viewStructName}
    err := cli.GetWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, "", nil, &resp)
    if err != nil {
        return nil, err
    }
    return &resp, nil
}
"""
    }

    // 单参数路径：标准 Get 方法
    return """
func (cli *ZSClient) Get${resourceName}(ctx context.Context, uuid string) (*view.${viewStructName}, error) {
    var resp view.${viewStructName}
    if err := cli.Get(ctx, "${cleanPath}", uuid, nil, &resp); err != nil {
        return nil, err
    }
    return &resp, nil
}
"""
}
```

**生成结果**：

```go
// APIGetGlobalConfigOptionsMsg
func (cli *ZSClient) GetGlobalConfigOptions(ctx context.Context, category string, name string) (*view.GetGlobalConfigOptionsView, error) {
	var resp view.GetGlobalConfigOptionsView
	err := cli.GetWithSpec(ctx, "v1/global-configurations", category, fmt.Sprintf("%s", name), "", nil, &resp)
	if err != nil {
		return nil, err
	}
	return &resp, nil
}

// APIQueryVmInstanceMsg
func (cli *ZSClient) GetVmInstance(ctx context.Context, uuid string) (*view.VmInstanceInventoryView, error) {
	var resp view.VmInstanceInventoryView
	if err := cli.Get(ctx, "v1/vm-instances", uuid, nil, &resp); err != nil {
		return nil, err
	}
	return &resp, nil
}
```

### 4.8 Groovy类型检查陷阱

**问题**：Groovy的`in`操作符对字符串列表检查不可靠

```groovy
// ❌ 错误：可能失败
if (baseType in ["string", "int", "int64"]) {
    return "*" + baseType
}
```

**解决**：使用Set的`contains()`方法

```groovy
// ✅ 正确：稳定可靠
def basicTypes = ["string", "int", "int64", "int32",
                  "float64", "float32", "bool"] as Set
if (basicTypes.contains(baseType)) {
    return "*" + baseType
}
```

### 4.9 方法签名一致性

**问题**：修改方法签名后，所有调用点必须同步更新，否则Groovy会报`MissingMethodException`

**案例**：`generateParamFieldGeneric`从单个参数改为三个参数

```groovy
// 旧签名
private String generateParamFieldGeneric(Field field) { ... }

// 新签名（添加apiParamMap和isOptional）
private String generateParamFieldGeneric(Field field,
                                         Map<String, APIParam> apiParamMap,
                                         boolean isOptional) { ... }
```

**必须更新所有调用点**：

```groovy
// generateParamStruct 中的调用
boolean isOptional = isOptionalField(field, apiParamMap)
String fieldType = generateParamFieldGeneric(field, apiParamMap, isOptional)

// generateParamNestedStruct 中的调用
def apiParamMap = new HashMap<String, APIParam>()
if (clazz.isAnnotationPresent(OverriddenApiParams.class)) {
    for (OverriddenApiParam oap : clazz.getAnnotation(OverriddenApiParams.class).value()) {
        apiParamMap.put(oap.field(), oap.param())
    }
}
boolean isOptional = isOptionalField(field, apiParamMap)
String fieldType = generateParamFieldGeneric(field, apiParamMap, isOptional)
```

**错误示例**：

```
groovy.lang.MissingMethodException: No signature of method:
  scripts.GoInventory.generateParamFieldGeneric() is applicable for
  argument types: (java.lang.reflect.Field)
```

### 4.10 模块路径一致性

**问题**：生成的 import 路径必须与 `go.mod` 中的模块名一致

**配置**：

```groovy
// 模块路径配置
String modulePath = "dev.zstack.io/ye.zou/zsphere-go-sdk"

// 生成导入时使用
content.append("\t\"${modulePath}/pkg/param\"\n")
content.append("\t\"${modulePath}/pkg/view\"\n")
```

---

## 5. 命名规则

### 5.1 API 名称解析

```groovy
// APICreateVmInstanceMsg -> CreateVmInstance
String parseApiName(String className) {
    return className
            .replaceFirst("^API", "")
            .replaceFirst("Msg\$", "")
}
```

### 5.2 View 结构体命名

```groovy
String getViewStructName(Class clz) {
    String name = clz.simpleName

    if (name.endsWith("Inventory")) {
        return name.replace("Inventory", "InventoryView")
        // VmInstanceInventory -> VmInstanceInventoryView
    }
    if (name.endsWith("Reply")) {
        return name.replace("Reply", "View").replaceAll("^API", "")
        // APIQueryVmInstanceReply -> QueryVmInstanceView
    }
    return name + "View"
    // ErrorCode -> ErrorCodeView
}
```

### 5.3 Param 结构体命名

```groovy
String getParamStructName(Class clz) {
    String name = clz.simpleName
    if (name.endsWith("Inventory")) {
        return name.replace("Inventory", "") + "Param"
        // AttributeInventory -> AttributeParam
    }
    return name + "Param"
    // SecurityGroupRuleAO -> SecurityGroupRuleAOParam
}
```

### 5.4 文件名（snake_case）

```groovy
String toSnakeCase(String name) {
    return name.replaceAll('([a-z])([A-Z])', '$1_$2').toLowerCase()
}
// CreateVmInstance -> create_vm_instance
// IAM2Project -> iam2_project
```

---

## 6. 特殊情况处理

### 6.1 Query API 的 Inventory 类型查找

```groovy
Class findQueryInventoryClass() {
    // 1. 从 Response 类的 inventories 字段获取泛型参数
    Field inventoriesField = responseClass.getDeclaredField("inventories")
    Type genericType = inventoriesField.getGenericType()
    if (genericType instanceof ParameterizedType) {
        return (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0]
    }

    // 2. 回退：根据 API 名称匹配 Inventory 类
    String expectedName = resourceName + "Inventory"
    for (Class inv : knownInventoryClasses) {
        if (inv.simpleName == expectedName) return inv
    }

    return null
}
```

### 6.2 RestResponse 注解处理

```groovy
if (clazz.isAnnotationPresent(RestResponse.class)) {
    RestResponse at = clazz.getAnnotation(RestResponse.class)

    // allTo：将所有字段映射到指定字段
    if (at.allTo() != "") {
        fieldMap = [at.allTo(): findField(clazz, at.allTo())]
    }

    // fieldsTo：字段重命名
    // "newName=oldName" 或 "fieldName"
    for (String fieldsTo : at.fieldsTo()) {
        String[] split = fieldsTo.split("=")
        String outputName = split[0]
        String fieldName = split.length == 1 ? split[0] : split[1]
        fieldMap[outputName] = findField(clazz, fieldName)
    }
}
```

### 6.3 字段跳过规则

```groovy
// 跳过静态字段
if (Modifier.isStatic(field.modifiers)) continue

// 跳过 @APINoSee 注解的字段
if (field.isAnnotationPresent(APINoSee.class)) continue

// 跳过 View 中的 error 字段（由基类处理）
if (fieldName == 'error' && structName.endsWith('View')) continue

// Param 中跳过基础字段
def skipFields = ['systemTags', 'userTags', 'requestIp', 'session',
                  'timeout', 'id', 'serviceId', 'creatingAccountUuid']
if (skipFields.contains(fieldName)) continue
```

---

## 7. 最佳实践总结

### 7.1 架构设计

1. **单例共享生成器**：Inventory 生成器应为单例，跨 API 共享状态
2. **两阶段处理**：先发现所有依赖类型，再统一生成
3. **去重机制**：使用 Set 追踪已生成的类型名称
4. **迭代而非递归**：处理嵌套依赖时使用队列，避免栈溢出

### 7.2 类型处理

1. **完整的类型映射表**：覆盖所有基本类型和常用类型
2. **可选字段指针类型**：基本类型的可选字段使用`*`前缀（slice/map/interface{}除外）
3. **数组类型特殊处理**：检查 `isArray()` 和内部表示 `[`；注意`byte[]` → `[]int8`（非`[]byte`）
4. **递归类型使用指针**：避免 Go 编译器报错
5. **类型检查使用Set.contains()**：避免Groovy `in`操作符的不可靠行为
6. **兜底使用 interface{}**：无法识别的类型降级处理

### 7.3 方法签名管理

1. **方法签名修改时同步更新所有调用点**：Groovy方法重载严格匹配参数类型和数量
2. **使用明确的参数传递**：避免依赖默认值或可选参数
3. **重构时使用grep搜索**：确保找到所有调用点

### 7.4 导入管理

1. **按需导入**：根据实际使用决定是否导入包
2. **避免未使用导入**：特别是 DELETE 操作不需要 view 包
3. **时间包特殊处理**：使用 `var _ = time.Now` 避免未使用警告

### 7.5 调试支持

1. **详细日志**：记录生成过程中的关键决策
2. **类型追踪**：记录发现和生成的类型数量
3. **跳过记录**：记录因缺少信息而跳过的 API

---

## 8. Go SDK 客户端使用指南

### 8.1 认证配置

#### 密码登陆（自动 SHA512 加密）

```go
package main

import (
    "context"
	"fmt"
	"time"

	"dev.zstack.io/ye.zou/zstack-go-sdk/pkg/client"
)

func main() {
	// 创建客户端配置
	config := &client.ZSConfig{
		Hostname: "172.20.1.164",
		Port:     8080,
		ContextPath: "/zstack",
		AuthType: client.AuthTypeUsernamePassword,
		Username: "admin",
		Password: "password123",  // ✅ 明文密码，客户端自动 SHA512 加密
		Timeout:  30 * time.Second,
	}

	cli := client.NewZSClient(config)
	ctx := context.Background()
	// 开始使用 API
	params := &param.QueryVmInstanceParam{}
	vms, err := cli.QueryVmInstance(ctx, params)
	if err != nil {
		fmt.Printf("查询失败: %v\n", err)
		return
	}

	fmt.Printf("找到 %d 个 VM\n", len(vms))
}
```

#### Access Key 认证（推荐）

```go
config := &client.ZSConfig{
	Hostname:       "172.20.1.164",
	Port:           8080,
	ContextPath:    "/zstack",
	AuthType:       client.AuthTypeAccessKey,
	AccessKeyId:    "your-access-key-id",
	AccessKeySecret: "your-access-key-secret",
	Timeout:        30 * time.Second,
}
```

### 8.2 常见问题

**Q: API 返回 `status code 400 (Authorization: )`，说明什么？**

A: 说明 Authorization 请求头为空，通常是以下原因：

- ✗ 密码错误（客户端会自动 SHA512 加密，传入明文即可）
- ✗ Access Key 配置错误
- ✗ 客户端配置中 AuthType 设置不正确

**Q: 密码需要手动加密吗？**

A: **不需要**。`NewZSClient()` 内部会自动使用 SHA512 加密密码，直接传入明文即可：

```go
config.Password = "password123"  // ✅ 明文，自动加密
// ❌ 不要：config.Password = hashPasswordSHA512("password123")
```

**Q: 如何验证加密后的密码？**

```bash
echo -n "password123" | sha512sum
# 输出: b109f3bbbc244eb8... (128位十六进制)
```

---

## 9. 扩展到其他语言

### 9.1 需要调整的部分

| 方面   | Go               | Java/Kotlin           | TypeScript           | Python                 |
|------|------------------|-----------------------|----------------------|------------------------|
| 类型系统 | 静态强类型            | 静态强类型                 | 动态类型+类型注解            | 动态类型                   |
| 空值处理 | 指针 `*T`          | `Optional<T>`         | `T \| null`          | `Optional[T]`          |
| 集合类型 | `[]T`, `map[K]V` | `List<T>`, `Map<K,V>` | `T[]`, `Record<K,V>` | `list[T]`, `dict[K,V]` |
| 时间类型 | `time.Time`      | `Instant`, `Date`     | `Date`               | `datetime`             |
| 枚举   | `string` + 常量    | `enum`                | `enum` / 字符串联合       | `Enum`                 |
| 包/模块 | `package`        | `package`             | `export/import`      | `import`               |

### 9.2 通用处理流程

```
1. 扫描 @RestRequest API 类
2. 解析 API 信息（名称、方法、路径、响应类型）
3. 生成请求参数类型
4. 生成响应视图类型
5. 生成 API 客户端方法
6. 生成基础设施代码（客户端、错误处理、工具函数）
7. 处理依赖类型（递归发现和生成）
```

### 9.3 注意事项

- **保持命名一致性**：跨语言 SDK 的 API 名称应保持一致
- **类型安全优先**：尽可能使用强类型而非 any/interface{}
- **文档生成**：从 Java 注释生成目标语言的文档注释
- **版本同步**：SDK 版本应与 ZStack 版本对应

---

## 10. 附录：完整的字段处理伪代码

```
function processField(field, apiParamMap):
    type = field.type

    // 0. 判断字段是否可选
    isOptional = isOptionalField(field, apiParamMap)

    // 1. 基本类型映射
    if (type in PRIMITIVE_TYPE_MAP):
        baseType = PRIMITIVE_TYPE_MAP[type]
        // 可选的基本类型使用指针
        if (isOptional && type in BASIC_TYPES):
            return "*" + baseType
        return baseType

    // 2. 数组类型
    if (type.isArray()):
        elementType = type.componentType
        return "[]" + processType(elementType)

    // 3. 泛型集合
    if (type is ParameterizedType):
        rawType = type.rawType
        if (rawType == Collection):
            return "[]" + processType(type.typeArguments[0])
        if (rawType == Map):
            return "map[string]" + processType(type.typeArguments[1])

    // 4. 枚举
    if (type.isEnum()):
        return "string"

    // 5. 递归类型检查
    if (type == currentGeneratingClass):
        return "*" + getStructName(type)  // 使用指针

    // 6. 可生成的复杂类
    if (isGeneratableClass(type)):
        addToPendingGeneration(type)
        return getStructName(type)

    // 7. 兜底
    return "interface{}"

function isOptionalField(field, apiParamMap):
    // uuid 和 name 始终必填
    if (field.name in ["uuid", "name"]):
        return false

    // 检查 @APIParam(required = false)
    if (field.hasAnnotation(APIParam)):
        param = apiParamMap.get(field.name) or field.getAnnotation(APIParam)
        if (param.required == false):
            return true
        return false

    // 没有注解的字段默认可选
    return true
```
