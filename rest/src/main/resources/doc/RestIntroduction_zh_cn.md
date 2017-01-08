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

<a name="X-Job-UUID">**X-Job-UUID**</a>

对于[异步API](#async_api)，可以通过`X-Job-UUID` HTTP Header来指定该API Job的UUID，
例如：
```$xslt
X-Job-UUID: d825b1a26f4e474b8c59306081920ff2
```

如果未指定该HTTP Header，ZStack会自动为API Job生成一个UUID。

>注意：X-Job-UUID必须为一个v4版本的UUID（即随机UUID）字符串去掉连接符`-`。ZStack会验证
X-Job-UUID格式的合法性，并对非法的字符串返回一个400 Bad Request的错误。

<a name="X-Web-Hook">**X-Web-Hook**</a>

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

## HTTP返回码 (HTTP Status Code)

ZStack使用如下返回码：

#### 200

API执行成功。

#### 202

API请求已被ZStack接受，用户需要通过轮询或Web Hook的方式获取API结果。该返回码只在调用异步API时出现。

#### 400

API请求未包含必要的参数或包含了非法的参数。具体信息可以从HTTP Response Body获得。

#### 404

URL不存在，通常是指定了错误的API URL。
如果访问的URL是异步API返回的轮询地址，表示该轮询地址已经过期。

#### 405

API调用使用了错误的HTTP方法，例如在创建一个资源的时候用了`GET`方法而不是`POST`方法。

#### 500

ZStack RESTful终端遭遇了一个内部错误。

#### 503

API所执行的操作引发了一个错误，例如资源不足无法创建虚拟机。错误的具体信息可以从HTTP Response Body。


## API种类

ZStack的API分为同步API和异步API两种：

#### 同步API

所有使用`GET`方法的API都是同步API，调用方收到的HTTP Response中直接包含了API的结果。例如：

```$xslt
GET /v1/zones/f3fa7671894a40f6a73f5bfc7d90c126

{
	"inventory": {
		"uuid": "f3fa7671894a40f6a73f5bfc7d90c126",
		"name": "zone1",
		"description": "test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 6, 2017 3:51:16 AM",
		"lastOpDate": "Jan 6, 2017 3:51:16 AM"
	}
}

```

#### <a name="async_api">异步API</a>

除了登录相关的API外，所有不使用`GET`方法的API都为异步API。用户调用一个异步API成功后会收到202返回码以及
Body中包含的一个轮询地址（location字段），用户需要周期性的GET该轮询地址以获得API的执行结果。例如：

```$xslt
Status Code: 202
  
Body: 

{
	"location": "http://localhost:8989/v1/api-jobs/967a26b7431c49c0b1d50d709ef1aef3"
}
```

通常情况下GET一个轮询地址可以得到三种返回。202返回码表示该API仍在处理中，用户需要继续轮询；200返回码表示API执行成功，
Body中包含API结果；503返回码表示API执行失败，Body中包含错误码。如果收到404返回码，则表示轮询地址已经过期，产生这种结果的原因
可能是用户访问了一个错误的轮询地址，或者太久没有访问该轮询地址（例如超过2天没有访问），该轮询地址已经被删除。

异步API也可以用Web Hook的方式获得结果，具体方法见后面章节。

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

但由于IaaS本身业务的性质，一部分操作更类似于RPC(远程调用)而非CRUD操作，例如启动虚拟机。
根据RESTFul API的一些最佳实践，ZStack将这些操作都归为资源的`actions`子资源，例如启动虚拟机、
停止虚拟机都是对虚拟机的`actions`子资源进行操作。举个例子：

启动虚拟机：

```$xslt
PUT /v1/vm-instances/d46841bd4ebd47f8bf0bed85c3bdf0db/actions

{
    "startVmInstance": {}
}
```

停止虚拟机：

```$xslt
PUT /v1/vm-instances/d46841bd4ebd47f8bf0bed85c3bdf0db/actions

{
    "stopVmInstance": {}
}
```

在上面的例子中，两个操作都访问的是相同的URL */v1/vm-instances/d46841bd4ebd47f8bf0bed85c3bdf0db/actions*，
具体的操作类型由包含在Body中的字段名表示，例如`stopVmInstance`，如果该API包含额外参数，则包含在操作字段名对应的
map中。

资源操作的具体字段名和例子参考每个API的详细文档。

## 示例

在下面的例子里面，我们会创建一个Zone，以展示API使用的基本流程：

#### 登录

使用API的第一步是登录以获取一个Session UUID，以供后续API调用使用。

```
PUT /v1/accounts/login

body:

{
	"logIn": {
		"password": "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86",
		"accountName": "admin"
	}
}
```

这里的密码是用sha512哈希后的结果。API返回如下：

```
status code: 200

body:

 {
 	"inventory": {
 		"uuid": "00d038b699b74e76a01705918d48d939",
 		"accountUuid": "36c27e8ff05c4780bf6d2fa65700f22e",
 		"userUuid": "36c27e8ff05c4780bf6d2fa65700f22e",
 		"expiredDate": "Jan 1, 2017 11:31:06 AM",
 		"createDate": "Jan 1, 2017 9:31:06 AM"
 	}
 }
```

返回内容中包含账户UUID等其他字段，我们需要的session UUID包含在字段`uuid`中：*00d038b699b74e76a01705918d48d939*。

#### 创建Zone

```
POST /v1/zones

headers:

Authorization: OAuth 00d038b699b74e76a01705918d48d939

body:

{
	"params": {
		"name": "Zone1",
		"description": "Test"
	}
}
```

由于创建Zone操作是一个异步API，API返回不是直接的结果，而是一个轮询地址：

```
status code: 202

body:

{
	"location": "http://localhost:8989/v1/api-jobs/d0345d3ddcae485f8170572b15a2b581"
}
```

用户需要周期性的轮询API结果：

```
GET http://localhost:8989/v1/api-jobs/d0345d3ddcae485f8170572b15a2b581

Authorization: OAuth 00d038b699b74e76a01705918d48d939
```

如果API还未执行完成，上述GET操作得到的仍然是202返回码和上述轮询地址。当操作完成时，得到的结果如下：

```
status code: 200

body:

{
	"inventory": {
		"uuid": "f52fe55b64094ceb99b3893a238c4931",
		"name": "Zone1",
		"description": "Test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 1, 2017 9:31:07 AM",
		"lastOpDate": "Jan 1, 2017 9:31:07 AM"
	}
}
```

#### 查询Zone

要获取创建的zone的信息，可以用GET查询：

```
GET /v1/zones/f52fe55b64094ceb99b3893a238c4931

Authorization: OAuth 00d038b699b74e76a01705918d48d939
```

返回：

```
status code: 200

body:

{
	"inventory": {
		"uuid": "f52fe55b64094ceb99b3893a238c4931",
		"name": "Zone1",
		"description": "Test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 1, 2017 9:31:07 AM",
		"lastOpDate": "Jan 1, 2017 9:31:07 AM"
	}
}
```


#### 登出

当所有API调用完毕，我们需要对已登录的session进行登出操作：

```
DELETE /v1/accounts/sessions/00d038b699b74e76a01705918d48d939
```

返回

```
status code: 200
```


## Web Hook

对于异步API使用轮询的方式查询操作结果是一种低效的方式，为此ZStack提供Web Hook的方式主动推送异步API
结果给调用者。

要使用Web Hook功能，调用者只需在HTTP headers中指定[X-Job-UUID](#X-Job-UUID)和[X-Web-Hook](#X-Web-Hook)
即可。上面创建Zone的为例，使用Web Hook的API版本为：

```
POST /v1/zones

headers:

Authorization: OAuth 00d038b699b74e76a01705918d48d939
X-Job-UUID: d0345d3ddcae485f8170572b15a2b581
X-Web-Hook: http://127.0.0.1:8989/rest-webhook

body:

{
	"params": {
		"name": "Zone1",
		"description": "Test"
	}
}
```

API返回仍然是202返回码和一个轮询地址，但调用者无需再轮询。API执行成功后，结果会被推送到*http://127.0.0.1:8989/rest-webhook*:

```
POST http://127.0.0.1:8989/rest-webhook

headers:

X-Job-Success: true
X-Job-UUID: d0345d3ddcae485f8170572b15a2b581

body:

{
	"inventory": {
		"uuid": "f52fe55b64094ceb99b3893a238c4931",
		"name": "Zone1",
		"description": "Test",
		"state": "Enabled",
		"type": "zstack",
		"createDate": "Jan 1, 2017 9:31:07 AM",
		"lastOpDate": "Jan 1, 2017 9:31:07 AM"
	}
}
```

推送的结果之中，*X-Job-Success*指明了API执行成功与否，*X-Job-UUID*包含值跟API调用时的*X-Job-UUID*相同，
调用者可以对应结果和API。