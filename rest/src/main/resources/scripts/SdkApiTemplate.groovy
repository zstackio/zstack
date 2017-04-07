package scripts

import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang.StringUtils
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.SuppressCredentialCheck
import org.zstack.header.message.*
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestRequest
import org.zstack.header.rest.SDK
import org.zstack.rest.sdk.SdkTemplate
import org.zstack.rest.sdk.SdkFile
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field

/**
 * Created by xing5 on 2016/12/9.
 */
class SdkApiTemplate implements SdkTemplate {
    CLogger logger = Utils.getLogger(SdkApiTemplate.class)

    Class apiMessageClass
    RestRequest requestAnnotation

    String resultClassName
    boolean isQueryApi

    SdkApiTemplate(Class apiMessageClass) {
        try {
            this.apiMessageClass = apiMessageClass
            this.requestAnnotation = apiMessageClass.getAnnotation(RestRequest.class)

            String baseName = requestAnnotation.responseClass().simpleName
            baseName = StringUtils.removeStart(baseName, "API")
            baseName = StringUtils.removeEnd(baseName, "Event")
            baseName = StringUtils.removeEnd(baseName, "Reply")

            resultClassName = StringUtils.capitalize(baseName)
            resultClassName = "${resultClassName}Result"

            isQueryApi = APIQueryMessage.class.isAssignableFrom(apiMessageClass)
        } catch (Throwable t) {
            throw new CloudRuntimeException(String.format("failed to make SDK for the class[%s]", apiMessageClass), t)
        }
    }

    def normalizeApiName() {
        def name = StringUtils.removeStart(apiMessageClass.getSimpleName(), "API")
        name = StringUtils.removeEnd(name, "Msg")
        return StringUtils.capitalize(name)
    }

    def generateClassName() {
        return String.format("%sAction", normalizeApiName())
    }

    def generateFields() {
        if (isQueryApi) {
            return ""
        }

        def fields = FieldUtils.getAllFields(apiMessageClass)

        APIMessage msg = (APIMessage)apiMessageClass.newInstance()

        def output = []

        OverriddenApiParams oap = apiMessageClass.getAnnotation(OverriddenApiParams.class)
        Map<String, APIParam> overriden = [:]
        if (oap != null) {
            for (OverriddenApiParam op : oap.value()) {
                overriden.put(op.field(), op.param())
            }
        }

        for (Field f : fields) {
            if (f.isAnnotationPresent(APINoSee.class)) {
                continue
            }

            APIParam apiParam = overriden.containsKey(f.name) ? overriden[f.name] : f.getAnnotation(APIParam.class)

            def annotationFields = []
            if (apiParam != null) {
                annotationFields.add(String.format("required = %s", apiParam.required()))
                if (apiParam.validValues().length > 0) {
                    annotationFields.add(String.format("validValues = {%s}", { ->
                        def vv = []
                        for (String v : apiParam.validValues()) {
                            vv.add("\"${v}\"")
                        }
                        return vv.join(",")
                    }()))
                }
                if (!apiParam.validRegexValues().isEmpty()) {
                    annotationFields.add(String.format("validRegexValues = \"%s\"", StringEscapeUtils.escapeJava(apiParam.validRegexValues())))
                }
                if (apiParam.maxLength() != Integer.MIN_VALUE) {
                    annotationFields.add(String.format("maxLength = %s", apiParam.maxLength()))
                }
                if (apiParam.minLength() != 0) {
                    annotationFields.add(String.format("minLength = %s", apiParam.minLength()))
                }
                annotationFields.add(String.format("nonempty = %s", apiParam.nonempty()))
                annotationFields.add(String.format("nullElements = %s", apiParam.nullElements()))
                annotationFields.add(String.format("emptyString = %s", apiParam.emptyString()))
                if (apiParam.numberRange().length > 0) {
                    def nr = apiParam.numberRange() as List<Long>
                    def ns = []
                    nr.forEach({ n -> return ns.add("${n}L")})

                    annotationFields.add(String.format("numberRange = {%s}", ns.join(",")))
                }
                annotationFields.add(String.format("noTrim = %s", apiParam.noTrim()))
            } else {
                annotationFields.add(String.format("required = false"))
            }

            def fs = """\
    @Param(${annotationFields.join(", ")})
    public ${f.getType().getName()} ${f.getName()}${{ ->
                f.accessible = true
                
                Object val = f.get(msg)
                if (val == null) {
                    return ";"
                }
                
                if (val instanceof String) {
                    return " = \"${StringEscapeUtils.escapeJava(val.toString())}\";"
                } else {
                    return " = ${val.toString()};"
                }
            }()}
"""
            output.add(fs.toString())
        }

        if (!apiMessageClass.isAnnotationPresent(SuppressCredentialCheck.class)) {
            output.add("""\
    @Param(required = true)
    public String sessionId;
""")
        }

        if (!APISyncCallMessage.class.isAssignableFrom(apiMessageClass)) {
            output.add("""\
    public long timeout;
    
    public long pollingInterval;
""")
        }

        return output.join("\n")
    }

    def generateMethods(String path) {
        def ms = []
        ms.add("""\
    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        ${resultClassName} value = res.getResult(${resultClassName}.class);
        ret.value = value == null ? new ${resultClassName}() : value; 

        return ret;
    }
""")

        ms.add("""\
    public Result call() {
        ApiResult res = ZSClient.call(this);
        return makeResult(res);
    }
""")

        ms.add("""\
    public void call(final Completion<Result> completion) {
        ZSClient.call(this, new InternalCompletion() {
            @Override
            public void complete(ApiResult res) {
                completion.complete(makeResult(res));
            }
        });
    }
""")

        ms.add("""\
    Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
""")

        ms.add("""\
    RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "${requestAnnotation.method().name()}";
        info.path = "${path}";
        info.needSession = ${!apiMessageClass.isAnnotationPresent(SuppressCredentialCheck.class)};
        info.needPoll = ${!APISyncCallMessage.class.isAssignableFrom(apiMessageClass)};
        info.parameterName = "${requestAnnotation.isAction() ? StringUtils.uncapitalize(normalizeApiName()) : requestAnnotation.parameterName()}";
        return info;
    }
""")

        return ms.join("\n")
    }

    def generateAction(String clzName, String path) {
        def f = new SdkFile()
        f.fileName = "${clzName}.java"
        f.content = """package org.zstack.sdk;

import java.util.HashMap;
import java.util.Map;

public class ${clzName} extends ${isQueryApi ? "QueryAction" : "AbstractAction"} {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public ${resultClassName} value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

${generateFields()}

${generateMethods(path)}
}
""".toString()

        return f
    }

    def generateAction() {
        SDK sdk = apiMessageClass.getAnnotation(SDK.class)
        if (sdk != null && sdk.actionsMapping().length != 0) {
            def ret = []

            for (String ap : sdk.actionsMapping()) {
                String[] aps = ap.split("=")
                if (aps.length != 2) {
                    throw new CloudRuntimeException("Invalid actionMapping[${ap}] of the class[${apiMessageClass.name}]," +
                            "an action mapping must be in format of actionName=restfulPath")
                }

                String aname = aps[0].trim()
                String restPath = aps[1].trim()

                if (!requestAnnotation.optionalPaths().contains(restPath)) {
                    throw new CloudRuntimeException("Cannot find ${restPath} in the 'optionalPaths' of the @RestPath of " +
                            "the class[${apiMessageClass.name}]")
                }

                aname = StringUtils.capitalize(aname)

                ret.add(generateAction("${aname}Action", restPath))
            }

            return ret
        } else {
            if (requestAnnotation.path() == "null") {
                throw new CloudRuntimeException("'path' is set to 'null' but no @SDK found on the class[${apiMessageClass.name}]")
            }

            return [generateAction(generateClassName(), requestAnnotation.path())]
        }
    }

    @Override
    List<SdkFile> generate() {
        try {
            return generateAction()
        } catch (Exception e) {
            logger.warn("failed to generate SDK for ${apiMessageClass.name}")
            throw e
        }
    }
}
