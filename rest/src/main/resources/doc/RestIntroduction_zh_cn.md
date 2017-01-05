# ZStack RESTful API使用手册

从1.9版本，ZStack开始提供原生RESTful支持，以取代早期的HTTP RPC API。本手册详细描述 Restful
API的使用规范，并提供所有API的详细定义。

## 版本

当前API版本为`v1`，所有API的URL以`/v1`开头，例如:
```$xslt
/v1/vm-instances
```

## HTTP方法 (HTTP Verbs)

当前API支持如下方法：

|方法名|描述|
|---|---|
|GET|获取资源信息。所有的Query API以及读API均使用该方法|
|POST|创建一个资源|
|PUT|修改一个资源。所有对资源的修改操作，以及类RPC调用的操作，例如启动虚拟机，均使用该方法|
|DELETE|删除一个资源|

## 参数

URL、Query String、HTTP body三种方式均可用于传参。每种方式可以单独使用，也可以混合使用，
具体使用哪种传参方式由具体API决定。

**URL传参**

当对某具体资源进行操作时，资源的UUID通过编码到URL的方式进行传参，例如启动一个UUID为
*f97143d60f1042c9badd9a1336d3c105*的虚拟机，URL格式为：
```$xslt
/v1/vm-instances/f97143d60f1042c9badd9a1336d3c105/actions
```

这里UUID编码到URL路径当中。

**Query String传参**

所有使用HTTP `GET`方法的API均使用Query String传参，例如查询所有状态为`Running`的虚拟机，
URL格式为：
```$xslt
/v1/vm-instances?condition=state=Running
```

**HTTP Body传参**

当使用`POST`方法创建一个资源，或`PUT`方法修改一个资源时，除通过URL传参的部分外，剩余参数
均通过HTTP Body传参。例如在指定物理机上启动一个虚拟机：

```$xslt
PUT /v1/vm-instances/f97143d60f1042c9badd9a1336d3c105/actions

{
  "startVmInstance": {
        "hostUuid": "8aef7e3a53b34eedaa05027a919156d9"
   }
}
```

这里虚拟机的UUID通过URL传参，参数*hostUuid*则通过HTTP Body传递。

## HTTP Headers

当前API使用如下自定义HTTP Headers：

**Authorization**

除了少数API外（例如登录API），使用ZStack API前都需要一个会话(session)，在调用API时
通过`Authorization` HTTP Header传递会话UUID。该Header的格式为：

```$xslt
Authorization: OAuth 会话UUID
```

举例：
```$xslt
Authorization: OAuth 34cbfddd470a47d8bdb0727cd2182618
```

>注意：OAuth和会话UUID之间用空格分隔

**X-Job-UUID**

对于[异步API](#async_api)，可以通过`X-Job-UUID` HTTP Header来指定该API Job的UUID，
例如：
```$xslt
X-Job-UUID: d825b1a26f4e474b8c59306081920ff2
```

如果未指定该HTTP Header，ZStack会自动为API Job生成一个UUID。

>注意：X-Job-UUID必须为一个v4版本的UUID（即随机UUID）字符串去掉连接符`-`。ZStack会验证
X-Job-UUID格式的合法性，并对非法的字符串返回一个400 Bad Request的错误。

**X-Web-Hook**

对于[异步API](#async_api)，可以通过`X-Web-Hook` HTTP Header指定一个回调URL用于接收API
返回。通过使用回调URL的方法，调用者可以避免使用轮询去查询一个异步API的执行结果。举例：

```$xslt
X-Web-Hook: http://localhost:5000/api-callback
```

**X-Job-Success**

当使用了`X-Web-Hook`回调的方式获取异步API结果时，ZStack推送给回调URL的HTTP Post请求中会
包含`X-Job-Success` HTTP Header指明该异步API的执行结果是成功还是失败。例如：

```$xslt
X-Job-Success: true
```

当值为*true*时执行成功，为*false*时执行失败。

## API种类

ZStack的API分为同步API和异步API两种：

#### 同步API



## 操作

跟所有的RESTful API类似，绝大多数ZStack API执行的是CRUD(Create, Read, Update, Delete)操作，
以及类RPC操作。

#### 创建资源

所有资源的创建都使用`POST`方法，参数通过HTTP Body传递，例如创建一个虚拟机：

```$xslt
POST /v1/vm-instances

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf

{
	"params": {
		"l3NetworkUuids": ["37a701c7fe4a40758da15593aedd8aff"],
		"defaultL3NetworkUuid": "37a701c7fe4a40758da15593aedd8aff",
		"dataDiskOfferingUuids": [],
		"name": "TestVm",
		"description": "Test",
		"systemTags": [],
		"instanceOfferingUuid": "dd53f94b58924510b0122e40799a4114",
		"type": "UserVm",
		"imageUuid": "cc7b56780879409f98c1f992b75a12b0"
	}
}
```

#### 查询资源

资源的查询使用`GET`方法，查询条件通过Query String传参，例如查询集群*cluster1*中名字**不等于** *web-vm*，
的虚拟机：

```$xslt
GET /v1/vm-instances?condition=name!=web-vm&condition=cluster.name=cluster1

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf
```

如果已知资源的UUID，要直接获取该资源的信息，直接使用`GET`方法不加任何查询条件，例如：

```$xslt
GET /v1/vm-instances/56f0fd314a2647ffb4f9565f6d05858e

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf
```

返回UUID为*56f0fd314a2647ffb4f9565f6d05858e*虚拟机的信息。


#### 删除资源

删除资源使用`DELETE`方法，被删除资源的UUID编码在URL中，例如：

```$xslt
DELETE /v1/vm-instances/56f0fd314a2647ffb4f9565f6d05858e

Authorization： OAuth 0c234e29a2ad4ff4b0d97d4f3b47c6cf
```

删除UUID为`56f0fd314a2647ffb4f9565f6d05858e`的虚拟机。

#### 修改资源与类PRC操作


但由于IaaS本身业务的性质，一部分操作更类似于RPC(远程调用)而不是CRUD操作，例如启动虚拟机。对于这些
类RPC操作，参考RESTful API的一些最佳实践，