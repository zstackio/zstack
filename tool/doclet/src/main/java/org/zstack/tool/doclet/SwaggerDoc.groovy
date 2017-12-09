package org.zstack.tool.doclet

import com.sun.javadoc.AnnotationDesc
import com.sun.javadoc.ClassDoc
import com.sun.javadoc.ProgramElementDoc
import com.sun.javadoc.RootDoc
import io.swagger.models.Info
import io.swagger.models.License
import io.swagger.models.Swagger
import io.swagger.util.Yaml

import javax.lang.model.element.AnnotationValue

class SwaggerDoc {
    RootDoc root
    Swagger swagger = new Swagger()

    static final String INFO_DESC = 'info.description'
    static final String INFO_TITLE = 'info.title'
    static final String INFO_VERSION = 'info.version'

    static final String DEFAULT_INFO_DESC = """ZStack API文档"""
    static final String DEFAULT_INFO_TITLE = 'ZStack API文档'
    static final String DEFAULT_INFO_VERSION = '2.3.0'

    static final String API_MESSAGE_NAME = 'org.zstack.header.message.APIMessage'
    static final String API_EVENT_NAME = 'org.zstack.header.message.APIEvent'
    static final String API_REPLY_NAME = 'org.zstack.header.message.APIReply'
    static final String API_QUERY_MESSAGE_NAME = 'org.zstack.header.query.APIQueryMessage'
    static final String API_QUERY_REPLY_NAME = 'org.zstack.header.query.APIQueryReply'
    static final String API_REQUEST_ANNOTATION_NAME = 'org.zstack.header.rest.RestRequest'
    static final String API_RESPONSE_ANNOTATION_NAME = 'org.zstack.header.rest.RestResponse'

    Map<String, ClassDoc> apiRequests = [:]
    Map<String, ClassDoc> apiResponse = [:]
    Map<String, API> apis = [:]

    static class Annotation {
        AnnotationDesc at

        Annotation(AnnotationDesc desc) {
            at = desc
        }

        String getElementValue(String name) {
            def el = at.elementValues().find {
                println("xxxxxxxxxxxx ${it.element().name()} = ${it.value().toString()}")
                it.element().name() == name
            }
            assert el != null : "cannot find element[${name}] in the annotation ${at.annotationType().qualifiedName()}"
            assert !(el.value().value() instanceof Collection) : "value of element[${name}] in the annotation ${at.annotationType().qualifiedName()} " +
                    "is a collection, call getElementValues() instead"
            return el.value().toString()
        }

        String getElementValues(String name) {
            def el = at.elementValues().find { it.element().name() == name }
            assert el != null : "cannot find element[${name}] in the annotation ${at.annotationType().qualifiedName()}"

            return (el.value().value() as AnnotationValue[]).collect { it.toString() }
        }

        static Annotation getAnnotation(ProgramElementDoc doc, String name) {
            AnnotationDesc desc = doc.annotations().find { it.annotationType().qualifiedName() == name }
            assert desc != null : "cannot find annotation[${name}] in ${doc.qualifiedName()}"
            return new Annotation(desc)
        }
    }

    private void createAPI(ClassDoc doc) {
        def restRequest = Annotation.getAnnotation(doc, API_REQUEST_ANNOTATION_NAME)
        def url = restRequest.getElementValue('path')
        def method = restRequest.getElementValue('method')

        apis[url] = new API(
                url: url,
                method: method
        )

        restRequest.getElementValues('optionalPaths').each {
            apis[it] = new API(
                    url: it,
                    method: method
            )
        }
    }

    static class API {
        String url
        String method
    }

    static AnnotationDesc findAnnotation(ProgramElementDoc doc, String annotationName) {
        return doc.annotations().find { it.annotationType().qualifiedName() == annotationName }
    }

    static boolean hasAnnotation(ProgramElementDoc doc, String annotationName) {
        return findAnnotation(doc, annotationName) != null
    }

    private boolean isApiRequest(ClassDoc doc) {
        return isClassOfType(doc, API_MESSAGE_NAME)
    }

    private boolean isApiResponse(ClassDoc doc) {
        return isClassOfType(doc, API_REPLY_NAME) || isClassOfType(doc, API_EVENT_NAME)
    }

    private boolean isClassOfType(ClassDoc doc, String typeName) {
        while (doc != null) {
            if (doc.qualifiedName() == typeName) {
                return true
            }

            doc = doc.superclass()
        }

        return false
    }

    private void parseApi() {
        apiRequests.each { apiName, apiClz ->
            if (apiClz.isAbstract()) {
                return
            }

            if (!hasAnnotation(apiClz, API_REQUEST_ANNOTATION_NAME)) {
                return
            }

            createAPI(apiClz)
        }

        apis.keySet().sort().each { url ->
            println(url)
        }
    }


    SwaggerDoc(RootDoc root) {
        this.root = root

        swagger.info = new Info(
                description: getOption(INFO_DESC, DEFAULT_INFO_DESC),
                title: getOption(INFO_TITLE, DEFAULT_INFO_TITLE),
                version: getOption(INFO_VERSION, DEFAULT_INFO_VERSION),
                license: new License(name: "Apache 2.0", url: "http://www.apache.org/licenses/LICENSE-2.0.html")
        )

        collectApiClassDoc()
        parseApi()
    }

    private void collectApiClassDoc() {
        root.classes().each { clz ->
            if (isApiRequest(clz)) {
                apiRequests[clz.qualifiedName()] = clz
            } else if (isApiResponse(clz)) {
                apiResponse[clz.qualifiedName()] = clz
            }
        }
    }

    private String getOption(String name, String defaultValue) {
        String value = System.getProperty(name)
        return value == null ? defaultValue : value
    }

    void generate(String path) {
        new File(path).write(Yaml.pretty().writeValueAsString(swagger))
    }
}
