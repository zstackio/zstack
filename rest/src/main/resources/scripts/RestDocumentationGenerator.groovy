package scripts

import com.google.common.io.Resources
import groovy.json.JsonBuilder
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.springframework.http.HttpMethod
import org.zstack.core.Platform
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.SuppressCredentialCheck
import org.zstack.header.message.APIParam
import org.zstack.header.message.OverriddenApiParams
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.AutoQuery
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestRequest
import org.zstack.header.rest.RestResponse
import org.zstack.rest.RestConstants
import org.zstack.rest.sdk.DocumentGenerator
import org.zstack.rest.sdk.DocumentGenerator.DocMode
import org.zstack.utils.DebugUtils
import org.zstack.utils.FieldUtils
import org.zstack.utils.ShellResult
import org.zstack.utils.ShellUtils
import org.zstack.utils.TypeUtils
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger
import org.zstack.utils.path.PathUtil

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by xing5 on 2016/12/21.
 */
class RestDocumentationGenerator implements DocumentGenerator {
    static CLogger logger = Utils.getLogger(RestDocumentationGenerator.class)

    String rootPath

    Map<String, File> sourceFiles = [:]

    def MUTUAL_FIELDS = [
            "lastOpDate": "最后一次修改时间",
            "createDate": "创建时间",
            "uuid": "资源的UUID，唯一标示该资源",
            "name": "资源名称",
            "description": "资源的详细描述",
            "primaryStorageUuid": "主存储UUID",
            "vmInstanceUuid": "云主机UUID",
            "imageUuid": "镜像UUID",
            "backupStorageUuid": "镜像存储UUID",
            "volumeUuid": "云盘UUID",
            "zoneUuid": "区域UUID",
            "clusterUuid": "集群UUID",
            "hostUuid": "物理机UUID",
            "l2NetworkUuid": "二层网络UUID",
            "l3NetworkUuid": "三层网络UUID",
            "accountUuid": "账户UUID",
            "policyUuid": "权限策略UUID",
            "userUuid": "用户UUID",
            "diskOfferingUuid": "云盘规格UUID",
            "volumeSnapshotUuid": "云盘快照UUID",
            "ipRangeUuid": "IP段UUID",
            "instanceOfferingUuid": "计算规格UUID",
            "vipUuid": "VIP UUID",
            "vmNicUuid": "云主机网卡UUID",
            "networkServiceProviderUuid": "网络服务提供模块UUID",
            "virtualRouterUuid": "云路由UUID",
            "securityGroupUuid": "安全组UUID",
            "eipUuid": "弹性IP UUID",
            "loadBalancerUuid": "负载均衡器UUID",
            "rootVolumeUuid": "根云盘UUID"
    ]

    String CHINESE_CN = "zh_cn"
    String ENGLISH_CN = "en_us"

    String LOCATION_URL = "url"
    String LOCATION_BODY = "body"
    String LOCATION_QUERY = "query"

    String makeFileNameForChinese(Class clz) {
        boolean inner = clz.getEnclosingClass() != null
        String fname
        if (inner) {
            fname = clz.getEnclosingClass().simpleName - ".java" + "_${clz.simpleName}" + "Doc_${CHINESE_CN}.groovy"
        } else {
            fname = clz.simpleName - ".java" + "Doc_${CHINESE_CN}.groovy"
        }

        return fname
    }

    String getSimpleClassNameFromDocTemplateName(String templateName) {
        return templateName - CHINESE_CN - ENGLISH_CN
    }

    def installClosure(ExpandoMetaClass emc, Closure c) {
        c(emc)
    }

    @Override
    void generateDocTemplates(String scanPath, DocMode mode) {
        rootPath = scanPath
        scanJavaSourceFiles()

        String classes = System.getProperty("classes")
        def apiClasses
        if (classes != null) {
            apiClasses = classes.split(",").collect { Class.forName(it) }
        } else {
            apiClasses = Platform.getReflections().getTypesAnnotatedWith(RestRequest.class).findAll { it.isAnnotationPresent(RestRequest.class) }
        }

        apiClasses.each {
            println("generating doc template for class ${it}")
            def tmp = new ApiRequestDocTemplate(it)
            tmp.generateDocFile(mode)
        }

        apiClasses = apiClasses.collect {
            RestRequest at = it.getAnnotation(RestRequest.class)
            return at.responseClass()
        }

        generateResponseDocTemplate(apiClasses as List, mode)
    }

    @Override
    void generateMarkDown(String scanPath, String resultDir) {
        rootPath = scanPath

        scanJavaSourceFiles()

        File dir = new File(resultDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        Set<Class> apiClasses = Platform.getReflections().getTypesAnnotatedWith(RestRequest.class).findAll { it.isAnnotationPresent(RestRequest.class) }
        String specifiedClasses = System.getProperty("classes")

        if (specifiedClasses != null) {
            def classes = specifiedClasses.split(",") as List
            apiClasses = apiClasses.findAll {
                return classes.contains(it.simpleName)
            }
        }

        apiClasses.each {
            def docPath = getDocTemplatePathFromClass(it)
            System.out.println("processing ${docPath}")
            try {
                def md = APIQueryMessage.class.isAssignableFrom(it) ? new QueryMarkDown(docPath) : new MarkDown(docPath)
                File f = new File(PathUtil.join(resultDir, md.doc._category, "${it.canonicalName.replaceAll("\\.", "_")}.md"))
                if (!f.parentFile.exists()) {
                    f.parentFile.mkdirs()
                }

                f.write(md.generate())
                System.out.println("written ${f.absolutePath}")
            } catch (Exception e) {
                if (ignoreError()) {
                    System.out.println("failed to process ${docPath}, ${e.message}")
                    logger.warn(e.message, e)
                } else {
                    throw e
                }
            }
        }

        URL url = Resources.getResource("doc/RestIntroduction_zh_cn.md")
        String context = Resources.toString(url, Charsets.UTF_8)
        new File(PathUtil.join(dir.absolutePath, "RestIntroduction_zh_cn.md")).write(context)
    }

    class RequestParamColumn {
        private String _name
        private String _desc
        private boolean _optional
        private List _values
        private String _since
        private String _type
        private String _location
        private String _enclosedIn

        def enclosedIn(String v) {
            _enclosedIn = v
        }

        def location(String v) {
            _location = v
        }

        def since(String v) {
            _since = v
        }

        def type(String v) {
            _type = v
        }

        def name(String v) {
            _name = v
        }

        def desc(String v) {
            _desc = v
        }

        def optional(boolean v) {
            _optional = v
        }

        def values(Object...args) {
            _values = args as List
        }
    }

    class RequestParamRef extends RequestParam {
        Class refClass
    }

    class RequestParam {
        private List<RequestParamColumn> _cloumns = []
        private String _desc

        def column(Closure c) {
            def rc = c.delegate = new RequestParamColumn()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _cloumns.add(rc)
        }

        def desc(String v) {
            _desc = v
        }
    }

    class Rest {
        private Request _request
        private Response _response

        def response(Closure c) {
            c.delegate = new Response()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _response = c.delegate as Response
        }

        def request(Closure c) {
            c.delegate = new Request()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _request = c.delegate as Request
        }
    }

    class Response {
        private Class _clz
        private String _desc

        def clz(Class v)  {
            _clz = v
        }

        def desc(String v) {
            _desc = v
        }
    }

    class Header {
        String key
        String value
        String comment

        String toString() {
            return "header (${key}: '$value')"
        }
    }

    class Url {
        String url
        String comment

        String toString() {
            return "url \"${url}\""
        }
    }

    class Request {
        private List<Url> _urls = []
        private List<Header> _headers = []
        private String _desc
        private RequestParam _params
        private LinkedHashMap _body
        private Class _clz

        def url(String v, String comment) {
            def u = new Url()
            u.url = v
            u.comment = comment
            _urls.add(u)
        }

        def url(String v) {
            url(v, null)
        }

        def header(Map v) {
            header(v, null)
        }

        def header(Map v, String comment) {
            Map.Entry e = v.entrySet().iterator().next()
            def h = new Header()
            h.key = e.key
            h.value = e.value
            h.comment = comment
            _headers.add(h)
        }

        def desc(String v) {
            _desc = v
        }

        def params(Class v) {
            def p = new RequestParamRef()
            p.refClass = v

            _params = p
        }

        def params(Closure c) {
            c.delegate = new RequestParam()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _params = c.delegate
        }

        def body(Closure c) {
            def j = new JsonBuilder()
            j.call(c)
            _body = JSONObjectUtil.toObject(j.toString(), LinkedHashMap.class)
        }

        def clz(Class v){
            _clz = v
        }
    }

    class DocField {
        String _name
        String _desc
        String _type
        String _since

        def name(String v) {
            _name = v
        }

        def desc(String v) {
            _desc = v
        }

        def type(String v) {
            _type = v
        }

        def since(String v) {
            _since = v
        }
    }

    class DocFieldRef {
        String _name
        String _type
        String _path
        String _desc
        Class _clz
        String _since
        Boolean _overrideDesc

        def since(String v) {
            _since = v
        }

        def name(String v) {
            _name = v
        }

        def type(String v) {
            _type = v
        }

        def path(String v) {
            _path = v
        }

        def desc(String v, Boolean o) {
            _desc = v
            _overrideDesc = o
        }

        def desc(String v) {
            desc(v, null)
        }

        def clz(Class v) {
            _clz = v
        }
    }

    class Doc {
        private String _title
        private String _category
        private String _desc
        private Rest _rest
        private List<DocField> _fields = []
        private List<DocFieldRef> _refs = []

        void merge(Doc n) {
            Request oreq = _rest._request
            Request nreq = n._rest._request

            oreq._urls = nreq._urls
            oreq._headers = nreq._headers
            oreq._clz = nreq._clz

            if (!(oreq._params instanceof RequestParamRef) && oreq._params?._cloumns != null && nreq._params?._cloumns != null) {
                oreq._params._cloumns.each { ocol ->
                    RequestParamColumn ncol = nreq._params._cloumns.find { it._name == ocol._name }
                    if (ncol == null) {
                        // the column is removed
                        return
                    }

                    ocol._enclosedIn = ncol._enclosedIn
                    ocol._location = ncol._location
                    ocol._type = ncol._type
                    ocol._optional = ncol._optional
                }
            }

            Response orsp = _rest._response
            Response nrsp = n._rest._response
            orsp._clz = nrsp._clz
        }

        def title(String v) {
            _title = v
        }

        def category(String v) {
            _category = v
        }

        def desc(String v) {
            _desc = v
        }

        def rest(Closure c) {
            c.delegate = new Rest()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _rest = c.delegate
        }

        def field(Closure c) {
            c.delegate = new DocField()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _fields.add(c.delegate as DocField)
        }

        def ref(Closure c) {
            c.delegate = new DocFieldRef()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            _refs.add(c.delegate as DocFieldRef)
        }
    }

    Doc createDocFromGroobyScript(Script script) {
        ExpandoMetaClass emc = new ExpandoMetaClass(script.getClass(), false, true)

        installClosure(emc, { ExpandoMetaClass e ->
            e.doc = { Closure cDoc ->
                cDoc.delegate = new Doc()
                cDoc.resolveStrategy = DELEGATE_FIRST

                cDoc()

                return cDoc.delegate
            }
        })

        emc.initialize()

        script.setMetaClass(emc)

        return script.run() as Doc
    }

    Doc createDocFromString(String scriptText) {
        Script script = new GroovyShell().parse(scriptText)
        return createDocFromGroobyScript(script)
    }

    Doc createDoc(String docTemplatePath) {
        Script script = new GroovyShell().parse(new File(docTemplatePath))
        return createDocFromGroobyScript(script)
    }

    class QueryMarkDown extends MarkDown {
        QueryMarkDown(String docTemplatePath) {
            super(docTemplatePath)
        }

        @Override
        String params() {
            Class clz = doc._rest._request._clz

            String apiName = StringUtils.removeStart(clz.simpleName, "API")
            apiName = StringUtils.removeEnd(apiName, "Msg")

            return """\
### 可查询字段

运行`zstack-cli`命令行工具，输入`${apiName}`并按Tab键查看所有可查询字段以及可跨表查询的资源名。
"""
        }

        List<String> getQueryConditionExampleOfTheClass(Class clz) {
            try {
                Method m = clz.getMethod("__example__")

                if (!Modifier.isStatic(m.modifiers)) {
                    throw new CloudRuntimeException("__example__() of ${clz.name} must be declared as a static method")
                }

                return m.invoke(null) as List<String>
            } catch (NoSuchMethodException e) {
                throw new CloudRuntimeException("class[${clz.name}] doesn't have static __example__ method", e)
            }
        }

        @Override
        String javaSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            def cols = ["${actionName} action = new ${actionName}();"]
            List<String> conds = getQueryConditionExampleOfTheClass(clz)
            conds = conds.collect { return "\"" + it + "\""}
            cols.add("action.conditions = asList(${conds.join(",")});")
            cols.add("""action.sessionUuid = "${Platform.getUuid()}";""")
            cols.add("${actionName}.Result res = action.call();")

            return """\
```
${cols.join("\n")}
```
"""
        }

        @Override
        String pythonSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            def cols = ["${actionName} action = ${actionName}()"]
            List<String> conds = getQueryConditionExampleOfTheClass(clz)
            conds = conds.collect { return "\"" + it + "\""}
            cols.add("action.conditions = [${conds.join(",")}]")
            cols.add("""action.sessionUuid = "${Platform.getUuid()}\"""")
            cols.add("${actionName}.Result res = action.call()")

            return """\
```
${cols.join("\n")}
```
"""
        }

        String curlExample() {
            if (doc._rest._request._clz == null) {
                return ""
            }

            Class clz = doc._rest._request._clz
            RestRequest at = clz.getAnnotation(RestRequest.class)

            Set<String> paths = []
            if (at.path() != "null") {
                paths.add(at.path())
            }
            paths.addAll(at.optionalPaths())

            List<String> examples = paths.collect {
                def curl = ["curl -H \"Content-Type: application/json\""]
                curl.add("-H \"Authorization: ${RestConstants.HEADER_OAUTH} ${Platform.getUuid()}\"")

                curl.add("-X ${at.method()}")

                if (it.contains("uuid")) {
                    // for "GET /v1/xxx/{uuid}, because the query example
                    // may not have uuid field specified
                    String urlPath = substituteUrl("${RestConstants.API_VERSION}${it}", ["uuid": Platform.getUuid()])
                    curl.add("http://localhost:8080${urlPath}")
                } else {
                    List<String> qstr = getQueryConditionExampleOfTheClass(clz)
                    qstr = qstr.collect {
                        return "q=${it}"
                    }
                    curl.add("http://localhost:8080${RestConstants.API_VERSION}${it}?${qstr.join("&")}")
                }

                return """\
```
${curl.join(" ")}
```
"""
            }


            return """\
### Curl示例

${examples.join("\n")}

"""
        }


        @Override
        String generate() {
            return  """\
## ${doc._category} - ${doc._title}

${doc._desc}

## API请求

${url()}

${headers()}

${requestExample()}

${curlExample()}

${params()}

## API返回

${responseDesc()}

${responseExample()}

## SDK示例

### Java SDK

${javaSdk()}

### Python SDK

${pythonSdk()}
"""
        }
    }

    class MarkDown {
        Doc doc

        MarkDown(String docTemplatePath) {
            doc = createDoc(docTemplatePath)

            if (doc._rest._request._params instanceof RequestParamRef) {
                Class refClass = (doc._rest._request._params as RequestParamRef).refClass
                String docFilePath = getDocTemplatePathFromClass(refClass)
                Doc refDoc = createDoc(docFilePath)

                doc._rest._request._params = refDoc._rest._request._params
            }
        }

        String url() {
            def urls = doc._rest._request._urls
            if (urls == null || urls.isEmpty()) {
                throw new CloudRuntimeException("cannot find urls for the class[${doc._rest._request._clz.name}]")
            }

            def txts = urls.collect {
                return it.comment == null ? it.url : "${it.url} #${it.comment}"
            }

            return """\
### URLs

```
${txts.join("\n")}
```
"""
        }

        String headers() {
            if (doc._rest._request._headers.isEmpty()) {
                return ""
            }

            def hs = []
            doc._rest._request._headers.each { h ->
                if (h.comment != null) {
                    hs.add("${h.key}: ${h.value} #${h.comment}")
                } else {
                    hs.add("${h.key}: ${h.value}")
                }
            }

            return """\
### Headers

```
${hs.join("\n")}
```
"""
        }

        String params() {
            if (doc._rest._request._params == null) {
                return ""
            }

            def cols = doc._rest._request._params?._cloumns

            if (cols == null || cols.isEmpty()) {
                return ""
            }


            def table = ["|名字|类型|位置|描述|可选值|起始版本|"]
            table.add("|---|---|---|---|---|---|")
            cols.each {
                def col = []

                String enclosed = null
                if (it._enclosedIn != "" && it._location == LOCATION_BODY) {
                    enclosed = "包含在`${it._enclosedIn}`结构中"
                }

                col.add(it._optional ? "${it._name} (可选)" : "${it._name}")
                col.add(it._type)

                def loc = it._location == LOCATION_BODY ? "body${enclosed == null ? "" : "(${enclosed})"}" : it._location
                col.add(loc)
                col.add("${it._desc}")
                if (it._values == null || it._values.isEmpty()) {
                    col.add("")
                } else {
                    def vals = it._values.collect { v ->
                        return "<li>${v}</li>"
                    }
                    col.add("<ul>${vals.join("")}</ul>")
                }
                col.add(it._since)

                table.add("|${col.join("|")}|")
            }

            return """\
### 参数列表

${doc._rest._request._params._desc == null ? "" : doc._rest._request._params._desc}

${table.join("\n")}
"""
        }

        LinkedHashMap getApiExampleOfTheClass(Class clz) {
            try {
                Method m = clz.getMethod("__example__")

                if (!Modifier.isStatic(m.modifiers)) {
                    throw new CloudRuntimeException("__example__() of ${clz.name} must be declared as a static method")
                }

                def example = m.invoke(null)

                LinkedHashMap map = JSONObjectUtil.rehashObject(example, LinkedHashMap.class)

                List<Field> apiFields = getApiFieldsOfClass(clz)

                def overridenApiParams = [:]
                OverriddenApiParams oat = clz.getAnnotation(OverriddenApiParams.class)
                if (oat != null) {
                    oat.value().each {
                        overridenApiParams[it.field()] = it.param()
                    }
                }

                List<String> missingField = apiFields.findAll {
                    APIParam at = overridenApiParams.containsKey(it.name) ? overridenApiParams[it.name] : it.getAnnotation(APIParam.class)
                    if (at != null && at.required() && !map.containsKey(it.name)) {
                        return it
                    } else {
                        return null
                    }
                }.collect { it.name }

                if (!missingField.isEmpty()) {
                    throw new CloudRuntimeException("required fields $missingField of ${clz.name} missing in the return value of __example__()")
                }

                def apiFieldNames = apiFields.collect {
                    return it.name
                }

                LinkedHashMap paramMap = [:]
                map.each { k, v ->
                    if (apiFieldNames.contains(k)) {
                        paramMap[k] = v
                    }
                }

                return paramMap
            } catch (NoSuchMethodException e) {
                throw new CloudRuntimeException("class[${clz.name}] doesn't have static __example__ method", e)
            }
        }

        String curlExample() {
            if (doc._rest._request._clz == null) {
                return ""
            }

            Class clz = doc._rest._request._clz
            RestRequest at = clz.getAnnotation(RestRequest.class)

            Set<String> paths = []
            if (at.path() != "null") {
                paths.add(at.path())
            }
            paths.addAll(at.optionalPaths())

            List<String> examples = paths.collect {
                def curl = ["curl -H \"Content-Type: application/json\""]
                if (!clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                    curl.add("-H \"Authorization: ${RestConstants.HEADER_OAUTH} ${Platform.getUuid()}\"")
                }

                boolean queryString = at.method() == HttpMethod.GET || HttpMethod.DELETE

                curl.add("-X ${at.method()}")

                Map allFields = getApiExampleOfTheClass(clz)

                String urlPath = substituteUrl("${RestConstants.API_VERSION}${it}", allFields)

                if (!queryString) {
                    def apiFields = getRequestBody()
                    if (apiFields != null) {
                        curl.add("-d '${JSONObjectUtil.toJsonString(apiFields)}'")
                    }
                    curl.add("http://localhost:8080${urlPath}")
                } else {
                    List<String> queryVars = doc._rest._request._params._cloumns.findAll {
                        return it._location == LOCATION_QUERY
                    }.collect {
                        return it._name
                    }

                    List qstr = []
                    allFields.findAll { k, v ->
                        return queryVars.contains(k)
                    }.each { k, v ->
                        if (v instanceof Collection) {
                            for (vv in v) {
                                qstr.add("${k}=${vv}")
                            }
                        } else if (v instanceof Map) {
                            v.each { kk, vv ->
                                if (vv instanceof Collection) {
                                    for (vvv in vv) {
                                        qstr.add("${k}.${kk}=${vvv}")
                                    }
                                } else {
                                    qstr.add("${k}.${kk}=${vv}")
                                }
                            }
                        } else {
                            qstr.add("${k}=${v}")
                        }
                    }

                    curl.add("http://localhost:8080${urlPath}?${qstr.join("&")}")
                }

                return """\
```
${curl.join(" ")}
```
"""
            }


            return """\
### Curl示例

${examples.join("\n")}

"""

        }

        Map getRequestBody() {
            Class clz = doc._rest._request._clz
            RestRequest at = clz.getAnnotation(RestRequest.class)

            String paramName = null
            if (at.parameterName() != "" && at.parameterName() != "null") {
                paramName = at.parameterName()
            } else if (at.isAction()) {
                paramName = paramNameFromClass(clz)
            }

            if (paramName == null) {
                // no body
                return null
            }

            // the API has a body
            Map apiFields = getApiExampleOfTheClass(clz)
            List<String> urlVars = getVarNamesFromUrl(at.path())
            apiFields = apiFields.findAll { k, v -> !urlVars.contains(k) }
            return [(paramName): apiFields]
        }

        String requestExample() {
            if (doc._rest._request._clz == null) {
                return ""
            }

            def apiFields = getRequestBody()
            if (apiFields == null) {
                return ""
            }

            apiFields["systemTags"] = []
            apiFields["userTags"] = []

            return """\
### Body

```
${JSONObjectUtil.dumpPretty(apiFields)}
```

> 上述示例中`systemTags`、`userTags`字段可以省略。列出是为了表示body中可以包含这两个字段。
"""
        }

        String responseDesc() {
            return doc._rest._response._desc == null ? "" : doc._rest._response._desc
        }

        String responseExample() {
            Class clz = doc._rest._response._clz
            if (clz == null) {
                throw new CloudRuntimeException("${doc._rest} doesn't have 'clz' specified in the response body")
            }

            String docFilePath = getDocTemplatePathFromClass(clz)
            Doc doc = createDoc(docFilePath)
            DataStructMarkDown dmd = new DataStructMarkDown(clz, doc)

            LinkedHashMap map = getApiExampleOfTheClass(clz)
            // the success field is not exposed to Restful APIs
            map.remove("success")

            def cols = []
            if (map.isEmpty()) {
                cols.add("""\
该API成功时返回一个空的JSON结构`{}`，出错时返回的JSON结构包含一个error字段，例如：

```
{
\t"error": {
\t\t"code": "SYS.1001",
\t\t"description": "A message or a operation timeout",
\t\t"details": "Create VM on KVM timeout after 300s"
\t}
}
```


""")
            } else {
                cols.add("""\
### 返回示例

```
${JSONObjectUtil.dumpPretty(map)}
```

${dmd.generate()}

""")
            }

            return cols.join("\n")
        }

        String getSdkActionName() {
            Class clz = doc._rest._request._clz
            String aname = StringUtils.removeStart(clz.simpleName, "API")
            aname = (aname - "Msg") + "Action"
            return aname
        }

        String pythonSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            def apiFields = getApiExampleOfTheClass(doc._rest._request._clz)
            def e = apiFields.find { k, v ->
                return !v.getClass().name.startsWith("java.")
            }

            if (e != null) {
                return "Python SDK未能自动生成"
            }

            def cols = ["${actionName} action = ${actionName}()"]
            cols.addAll(apiFields.collect { k, v ->
                if (v instanceof String) {
                    return """action.${k} = "${v}\""""
                } else {
                    return "action.${k} = ${v}"
                }
            })

            if (!clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                cols.add("""action.sessionUuid = "${Platform.getUuid()}\"""")
            }

            cols.add("${actionName}.Result res = action.call()")

            return """\
```
${cols.join("\n")}
```
"""
        }

        String javaSdk() {
            Class clz = doc._rest._request._clz

            String actionName = getSdkActionName()

            def apiFields = getApiExampleOfTheClass(doc._rest._request._clz)
            def e = apiFields.find { k, v ->
                // don't use v.class here, things break if the v is of type Map
                return !v.getClass().name.startsWith("java.")
            }

            if (e != null) {
                return "Java SDK未能自动生成"
            }

            def cols = ["${actionName} action = new ${actionName}();"]
            cols.addAll(apiFields.collect { k, v ->
                if (v instanceof String) {
                    return """action.${k} = "${v}";"""
                } else if (v instanceof Collection) {
                    Collection c = v as Collection
                    if (c.isEmpty()) {
                        return "action.${k} = new ArrayList();"
                    } else {
                        def item = c[0]

                        if (item instanceof String) {
                            def lst = c.collect {
                                return "\"" + it + "\""
                            }

                            return "action.${k} = asList(${lst.join(",")});"
                        } else {
                            return "action.${k} = asList(${c.join(",")});"
                        }
                    }

                } else {
                    return "action.${k} = ${v};"
                }
            })

            if (!clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                cols.add("""action.sessionUuid = "${Platform.getUuid()}";""")
            }

            cols.add("${actionName}.Result res = action.call();")

            return """\
```
${cols.join("\n")}
```
"""
        }

        String generate() {
            return  """\
## ${doc._category} - ${doc._title}

${doc._desc}

## API请求

${url()}

${headers()}

${requestExample()}

${curlExample()}

${params()}

## API返回

${responseDesc()}

${responseExample()}

## SDK示例

### Java SDK

${javaSdk()}

### Python SDK

${pythonSdk()}
"""
        }
    }

    class DataStructMarkDown {
        Doc doc
        Class clz
        Map<Class, String> resolvedRefs = [:]

        DataStructMarkDown(Class clz, Doc doc) {
            this.clz = clz
            this.doc = doc
        }

        String generate() {
            def tables = []

            def rows = ["|名字|类型|描述|起始版本|"]
            rows.add("|---|---|---|---|")

            doc._fields.each {
                def row = []
                row.add(it._name)
                row.add(it._type)
                row.add(it._desc)
                row.add(it._since)

                rows.add("|${row.join("|")}|")
            }

            doc._refs.each {
                def row = []
                row.add(it._name)
                row.add(it._type)
                if (it._overrideDesc == null) {
                    row.add("详情参考[${it._name}](#${it._path})")
                } else if (it._overrideDesc) {
                    row.add(it._desc)
                } else {
                    row.add("${it._desc}。 详情参考[${it._name}](#${it._path})")
                }
                row.add(it._since)

                rows.add("|${row.join("|")}|")
            }

            tables.add(rows.join("\n"))

            def refs = doc._refs.findAll { clz != it._clz }

            // generate dependent tables

            refs.each {
                String txt = resolvedRefs[it._clz]

                if (txt == null) {
                    String path = getDocTemplatePathFromClass(it._clz)
                    Doc refDoc = createDoc(path)
                    def dmd = new DataStructMarkDown(it._clz, refDoc)
                    dmd.resolvedRefs = resolvedRefs
                    txt = dmd.generate()
                    resolvedRefs[it._clz] = txt
                }

                tables.add("""\

<a name="${it._path}"> **#${it._name}**<a/>

${txt}
""")
            }

            return tables.join("\n")
        }
    }

    String getDocTemplatePathFromClass(Class clz) {
        boolean inner = clz.getEnclosingClass() != null
        String srcFileName = (inner ? clz.getEnclosingClass().simpleName : clz.simpleName) - ".java"

        File srcFile = getSourceFile(srcFileName)
        String docName = makeFileNameForChinese(clz)
        return PathUtil.join(srcFile.parent, docName)
    }

    def PRIMITIVE_TYPES = [
            int.class,
            long.class,
            float .class,
            boolean .class,
            double.class,
            short.class,
            char.class,
            String.class,
            Enum.class,
    ]

    class ApiResponseDocTemplate {
        Set<Class> laterResolveClasses = []

        Class responseClass
        RestResponse at

        List<String> imports = []
        Map<String, Field> fsToAdd = [:]
        List<String> fieldStrings = []

        ApiResponseDocTemplate(Class responseClass) {
            this.responseClass = responseClass

            at = responseClass.getAnnotation(RestResponse.class)
            if (at != null) {
                findFieldsForRestResponse()
            } else {
                findFieldsForNormalClass()
            }
        }

        def findFieldsForNormalClass() {
            List<Field> allFields = FieldUtils.getAllFields(responseClass)
            allFields = allFields.findAll { !it.isAnnotationPresent(APINoSee.class) && !Modifier.isStatic(it.modifiers) }
            allFields.each { fsToAdd[it.name] = it }
        }

        def findFieldsForRestResponse() {
            if (at.allTo() == null && at.fieldsTo().length == 0) {
                return ""
            }

            if (at.allTo() != "") {
                fsToAdd[at.allTo()] = responseClass.getDeclaredField(at.allTo())
            } else if (at.fieldsTo().length == 1 && at.fieldsTo()[0] == "all") {
                findFieldsForNormalClass()
            } else {
                at.fieldsTo().each {
                    def kv = it.split("=")

                    if (kv.length == 1) {
                        fsToAdd[kv[0]] = FieldUtils.getField(kv[0], responseClass)
                    } else {
                        def k = kv[0].trim()
                        def v = kv[1].trim()
                        fsToAdd[k] = FieldUtils.getField(v, responseClass)
                    }
                }
            }

            fieldStrings.add(createRef("error", "${responseClass.canonicalName}.error", "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", ErrorCode.class.simpleName, ErrorCode.class, false))
        }

        String createField(String n, String desc, String type) {
            if (MUTUAL_FIELDS.containsKey(n)) {
                desc = "${desc == null || desc.isEmpty() ? MUTUAL_FIELDS[n] : MUTUAL_FIELDS[n] + "," + desc}"
            }

            return """\tfield {
\t\tname "${n}"
\t\tdesc "${desc == null ? "" : desc}"
\t\ttype "${type}"
\t\tsince "0.6"
\t}"""
        }

        String createRef(String n, String path, String desc, String type, Class clz, Boolean overrideDesc=null) {
            DebugUtils.Assert(!PRIMITIVE_TYPES.contains(clz), "${clz.name} is a primitive class!!!")

            laterResolveClasses.add(clz)
            imports.add("import ${clz.canonicalName}")

            return """\tref {
\t\tname "${n}"
\t\tpath "${path}"
\t\tdesc "${desc}"${overrideDesc != null ? ",${overrideDesc}" : ""}
\t\ttype "${type}"
\t\tsince "0.6"
\t\tclz ${clz.simpleName}.class
\t}"""
        }

        String fields() {
            fsToAdd.each { k, v ->
                System.out.println("generating field ${responseClass.name}.${k} ${v.type.name}")

                if (PRIMITIVE_TYPES.contains(v.type)) {
                    fieldStrings.add(createField(k, "", v.type.simpleName))
                } else if (v.type.name.startsWith("java.")) {
                    if (Collection.class.isAssignableFrom(v.type) || Map.class.isAssignableFrom(v.type)) {
                        Class gtype = FieldUtils.getGenericType(v)

                        if (gtype == null || gtype.name.startsWith("java.")) {
                            fieldStrings.add(createField(k, "", v.type.simpleName))
                        } else {
                            fieldStrings.add(createRef(k, "${responseClass.canonicalName}.${v.name}", null, v.type.simpleName, gtype))
                        }
                    } else {
                        // java time but not primitive, needs import
                        imports.add("import ${v.type.name}")
                        fieldStrings.add(createField(k, "", v.type.simpleName))
                    }
                } else {
                    fieldStrings.add(createRef("${k}", "${responseClass.canonicalName}.${v.name}", null, v.type.simpleName, v.type))
                }
            }

            return fieldStrings.join("\n")
        }

        String generate() {
            String fieldStr = fields()

            return """${responseClass.package}

${imports.join("\n")}

doc {

\ttitle "在这里输入结构的名称"

${fieldStr}
}
"""
        }
    }

    class ApiRequestDocTemplate {
        Class apiClass
        File sourceFile
        RestRequest at

        List<String> imports = []
        static Map<String, String> apiCategories = [:]

        static {
            File apiConfig = PathUtil.findFolderOnClassPath("serviceConfig")
            apiConfig.list().each {
                def xml = new XmlSlurper()
                def f = new File(PathUtil.join(apiConfig.absolutePath, it))
                if (!it.endsWith(".xml")) {
                    return
                }

                def o = xml.parse(f)
                (o.message as List).each {
                    apiCategories[(it.name as String).trim()] = (o.id as String).trim()
                }
            }
        }

        ApiRequestDocTemplate(Class apiClass) {
            this.apiClass = apiClass
            this.sourceFile = getSourceFile(apiClass.simpleName - ".java")
            at = apiClass.getAnnotation(RestRequest.class)
            imports.add("import ${at.responseClass().canonicalName}")
        }

        String headers() {
            if (apiClass.isAnnotationPresent(SuppressCredentialCheck.class)) {
                return ""
            }

            return """header(Authorization: '${RestConstants.HEADER_OAUTH} the-session-uuid')"""
        }

        String getParamName() {
            if (at.parameterName() != "" && at.parameterName() != "null") {
                return at.parameterName()
            } else if (at.isAction()) {
                return paramNameFromClass(apiClass)
            } else {
                return null
            }
        }

        String params() {
            if (APIQueryMessage.class.isAssignableFrom(apiClass)) {
                imports.add("import ${APIQueryMessage.class.canonicalName}")

                return """\t\t\tparams ${APIQueryMessage.class.simpleName}.class"""
            }

            def apiFields = getApiFieldsOfClass(apiClass)

            if (apiFields.isEmpty()) {
                return ""
            }

            String paramName = getParamName()

            List<String> urlVars = getVarNamesFromUrl(at.path())
            def cols = []
            for (Field af : apiFields) {
                APIParam ap = af.getAnnotation(APIParam.class)

                String values = null
                if (ap != null && ap.validValues().length != 0) {
                    def l = []
                    ap.validValues().each {
                        l.add("\"${it}\"")
                    }
                    values = "values (${l.join(",")})"
                }

                String desc = MUTUAL_FIELDS.get(af.name)

                String enclosedIn
                if (af.name == "systemTags" || af.name == "userTags") {
                    enclosedIn = ""
                } else {
                    enclosedIn = paramName == null ? "" : paramName
                }

                def location
                if (urlVars.contains(af.name)) {
                    location = LOCATION_URL
                } else if (at.method() == HttpMethod.GET) {
                    location = LOCATION_QUERY
                } else {
                    location = LOCATION_BODY
                }

                cols.add("""\t\t\t\tcolumn {
\t\t\t\t\tname "${af.name}"
\t\t\t\t\tenclosedIn "${enclosedIn}"
\t\t\t\t\tdesc "${desc == null  ? "" : desc}"
\t\t\t\t\tlocation "${location}"
\t\t\t\t\ttype "${af.type.simpleName}"
\t\t\t\t\toptional ${ap == null ? true : !ap.required()}
\t\t\t\t\tsince "0.6"
\t\t\t\t\t${values == null ? "" : values}
\t\t\t\t}""")
            }

            if (cols.isEmpty()) {
                return ""
            }

            return """\t\t\tparams {

${cols.join("\n")}
\t\t\t}"""
        }

        String imports() {
            return imports.isEmpty() ? "" : imports.join("\n")
        }

        String urls() {
            Set<String> paths = []
            if ("null" != at.path()) {
                paths.add(at.path())
            }

            paths.addAll(at.optionalPaths())
            paths = paths.collect {
                return "${at.method().toString()} ${RestConstants.API_VERSION}${it}"
            }

            return paths.collect {
                return """\
\t\t\turl "${it}"
"""
            }.join("\n")
        }

        String generate(Doc doc) {
            String paramString
            if (doc._rest._request._params instanceof RequestParamRef) {
                imports.add("import ${doc._rest._request._params.refClass.canonicalName}")
                paramString = "\t\t\tparams ${doc._rest._request._params.refClass.simpleName}.class"
            } else {
                List cols = doc._rest._request._params._cloumns.collect {
                    String values = it._values != null && !it._values.isEmpty() ? "values (${it._values.collect { "\"$it\"" }.join(",")})" : ""
                    return """\t\t\t\tcolumn {
\t\t\t\t\tname "${it._name}"
\t\t\t\t\tenclosedIn "${it._enclosedIn}"
\t\t\t\t\tdesc "${it._desc}"
\t\t\t\t\tlocation "${it._location}"
\t\t\t\t\ttype "${it._type}"
\t\t\t\t\toptional ${it._optional}
\t\t\t\t\tsince "${it._since}"
\t\t\t\t\t${values}
\t\t\t\t}"""
                }

                paramString = """\t\t\tparams {

${cols.join("\n")}
\t\t\t}"""
            }

            return """package ${apiClass.package.name}

${imports()}

doc {
    title "${doc._title}"

    category "${doc._category}"

    desc \"\"\"${doc._desc}\"\"\"

    rest {
        request {
${doc._rest._request._urls.collect { "\t\t\t" + it.toString() }.join("\n")}

${doc._rest._request._headers.collect { "\t\t\t" + it.toString() }.join("\n")}


            clz ${doc._rest._request._clz.simpleName}.class

            desc \"\"\"${doc._rest._request._desc}\"\"\"
            
${paramString}
        }

        response {
            clz ${doc._rest._response._clz.simpleName}.class
        }
    }
}"""
        }

        String generate() {
            String paramString = params()

            String title = StringUtils.removeStart(apiClass.simpleName, "API")
            title = StringUtils.removeEnd(title, "Msg")
            String category = apiCategories[apiClass.name]
            category = category == null ? at.category() : category
            if (category == "") {
                category = "未知类别"
            }

            return """package ${apiClass.package.name}

${imports()}

doc {
    title "${title}"

    category "${category}"

    desc \"\"\"在这里填写API描述\"\"\"

    rest {
        request {
${urls()}

            ${headers()}

            clz ${apiClass.simpleName}.class

            desc \"\"\"\"\"\"
            
${paramString}
        }

        response {
            clz ${at.responseClass().simpleName}.class
        }
    }
}"""
        }

        void repair(String docFilePath) {
            if (!new File(docFilePath).exists()) {
                System.out.println("cannot find ${docFilePath}, not way to repair, you need to generate it first")
                return
            }

            Doc oldDoc = createDoc(docFilePath)
            Doc newDoc = createDocFromString(generate())
            oldDoc.merge(newDoc)

            new File(docFilePath).write generate(oldDoc)
            System.out.println("re-written a request doc template ${docFilePath}")
        }

        void generateDocFile(DocMode mode) {
            def docFilePath = getDocTemplatePathFromClass(apiClass)

            if (mode == DocMode.RECREATE_ALL) {
                new File(docFilePath).write generate()
                System.out.println("written a request doc template ${docFilePath}")
            } else if (mode == DocMode.REPAIR) {
                repair(docFilePath)
            } else if (mode == DocMode.CREATE_MISSING) {
                if (!new File(docFilePath).exists()) {
                    new File(docFilePath).write generate()
                    System.out.println("written a request doc template ${docFilePath}")
                } else {
                    System.out.println("${docFilePath} exists, skip it")
                }
            } else {
                throw new CloudRuntimeException("unknown doc mode ${mode}")
            }
        }
    }

    List<Field> getApiFieldsOfClass(Class apiClass) {
        def apiFields = []

        FieldUtils.getAllFields(apiClass).each {
            if (it.isAnnotationPresent(APINoSee.class)) {
                return
            }

            if (Modifier.isStatic(it.modifiers)) {
                return
            }

            apiFields.add(it)
        }

        return apiFields
    }

    void generateResponseDocTemplate(List<Class> aClasses, DocMode mode) {
        Set<Class> resolved = []

        aClasses.each {
            System.out.println("generating response doc template for class[${it.name}]")
            String path = getDocTemplatePathFromClass(it)
            if (new File(path).exists() && mode != DocMode.RECREATE_ALL) {
                System.out.println("${path} exists, skip it")
                return
            }

            Set<Class> todo = []
            Map<String, String> docFiles = [:]
            def tmp = new ApiResponseDocTemplate(it)
            docFiles[path] = tmp.generate()
            todo.addAll(tmp.laterResolveClasses)

            while (!todo.isEmpty()) {
                Set<Class> set = []
                todo.each {
                    def t = new ApiResponseDocTemplate(it)
                    path = getDocTemplatePathFromClass(it)
                    docFiles[path] = t.generate()

                    resolved.add(it)
                    set.addAll(t.laterResolveClasses)
                }

                todo = set - resolved
            }

            docFiles = docFiles.findAll { p, _ ->
                def fname = new File(p).name

                // for pre-defined doc templates, won't generate them
                return [ErrorCode.class].find {
                    return fname.startsWith(it.simpleName)
                } == null
            }

            docFiles.each { k, v ->
                def f = new File(k)
                f.write(v)
                System.out.println("written doc template ${k}")
            }
        }
    }

    File getSourceFile(String n) {
        File f = sourceFiles[n]
        if (f == null) {
            throw new CloudRuntimeException("cannot find source file ${n}.java")
        }

        return f
    }

    List<String> getVarNamesFromUrl(String url) {
        Pattern pattern = Pattern.compile("\\{(.+?)\\}")
        Matcher matcher = pattern.matcher(url)

        List<String> urlVars = []
        while (matcher.find()) {
            urlVars.add(matcher.group(1))
        }

        return urlVars
    }

    String substituteUrl(String url, Map<String, Object> tokens) {
        Pattern pattern = Pattern.compile("\\{(.+?)\\}")
        Matcher matcher = pattern.matcher(url)
        StringBuffer buffer = new StringBuffer()
        while (matcher.find()) {
            String varName = matcher.group(1)
            Object replacement = tokens.get(varName)
            if (replacement == null) {
                throw new CloudRuntimeException(String.format("cannot find value for URL variable[%s]", varName))
            }

            matcher.appendReplacement(buffer, "")
            buffer.append(replacement.toString())
        }

        matcher.appendTail(buffer)
        return buffer.toString()
    }

    def paramNameFromClass(Class clz) {
        def paramName = StringUtils.removeStart(clz.simpleName, "API")
        paramName = StringUtils.removeEnd(paramName, "Msg")
        paramName = StringUtils.uncapitalize(paramName)
        return paramName
    }

    def scanDocTemplateFiles() {
        ShellResult res = ShellUtils.runAndReturn("find ${rootPath} -name '*Doc_*.groovy' -not -path '${rootPath}/sdk/*'")
        List<String> paths = res.stdout.split("\n")
        paths = paths.findAll { !(it - "\n" - "\r" - "\t").trim().isEmpty() }
        return paths
    }

    def scanJavaSourceFiles() {
        ShellResult res = ShellUtils.runAndReturn("find ${rootPath} -name '*.java' -not -path '${rootPath}/sdk/*'")
        List<String> paths = res.stdout.split("\n")
        paths = paths.findAll { !(it - "\n" - "\r" - "\t").trim().isEmpty()}

        paths.each {
            def f = new File(it)
            sourceFiles[f.name - ".java"] = f
        }
    }

    boolean ignoreError() {
        return System.getProperty("ignoreError") != null
    }
}
