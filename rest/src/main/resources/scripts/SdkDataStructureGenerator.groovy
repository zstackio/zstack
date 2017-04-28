package scripts

import org.apache.commons.lang.StringUtils
import org.reflections.Reflections
import org.zstack.core.Platform
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.query.APIQueryReply
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestResponse
import org.zstack.header.rest.SDK
import org.zstack.rest.sdk.SdkFile
import org.zstack.rest.sdk.SdkTemplate
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field

/**
 * Created by xing5 on 2016/12/11.
 */
class SdkDataStructureGenerator implements SdkTemplate {
    CLogger logger = Utils.getLogger(SdkDataStructureGenerator.class)

    Set<Class> responseClasses
    Map<Class, SdkFile> sdkFileMap = [:]
    Set<Class> laterResolvedClasses = []

    Map<String, String> sourceClassMap = [:]

    Reflections reflections = Platform.reflections

    SdkDataStructureGenerator() {
        Reflections reflections = Platform.getReflections()
        responseClasses = reflections.getTypesAnnotatedWith(RestResponse.class)
    }

    @Override
    List<SdkFile> generate() {
        responseClasses.each { c ->
            try {
                generateResponseClass(c)
            } catch (Throwable t) {
                throw new CloudRuntimeException("failed to generate SDK for the class[${c.name}]", t)
            }
        }

        resolveAllClasses()

        def ret = sdkFileMap.values() as List
        ret.add(generateSourceDestClassMap())

        return ret
    }

    def generateSourceDestClassMap() {
        def srcToDst = []
        def dstToSrc = []

        sourceClassMap.each { k, v ->
            srcToDst.add("""\t\t\tput("${k}", "${v}");""")
            dstToSrc.add("""\t\t\tput("${v}", "${k}");""")
        }

        srcToDst.sort()
        dstToSrc.sort()

        SdkFile f = new SdkFile()
        f.fileName = "SourceClassMap.java"
        f.content = """package org.zstack.sdk;

import java.util.HashMap;

public class SourceClassMap {
    final static HashMap<String, String> srcToDstMapping = new HashMap() {
        {
${srcToDst.join("\n")}
        }
    };

    final static HashMap<String, String> dstToSrcMapping = new HashMap() {
        {
${dstToSrc.join("\n")}
        }
    };
}
"""
        return f
    }

    def resolveAllClasses() {
        if (laterResolvedClasses.isEmpty()) {
            return
        }

        Set<Class> toResolve = []
        toResolve.addAll(laterResolvedClasses)

        toResolve.each { Class clz ->
            try {
                resolveClass(clz)
                laterResolvedClasses.remove(clz)
            } catch (Throwable t) {
                throw new CloudRuntimeException("failed to generate SDK for the class[${clz.name}]", t)
            }
        }

        resolveAllClasses()
    }

    def getTargetClassName(Class clz) {
        SDK at = clz.getAnnotation(SDK.class)
        if (at == null || at.sdkClassName().isEmpty()) {
            return clz.simpleName
        }

        return at.sdkClassName()
    }

    def resolveClass(Class clz) {
        if (sdkFileMap.containsKey(clz)) {
            return
        }

        if (!Object.class.isAssignableFrom(clz.superclass)) {
            addToLaterResolvedClassesIfNeed(clz.superclass)
        }

        def output = []

        if (!Enum.class.isAssignableFrom(clz)) {
            for (Field f : clz.getDeclaredFields()) {
                if (f.isAnnotationPresent(APINoSee.class)) {
                    continue
                }

                output.add(makeFieldText(f.name, f))
            }
        } else {
            for (Enum e : clz.getEnumConstants()) {
                output.add("\t${e.name()},")
            }
        }

        SdkFile file = new SdkFile()
        file.fileName = "${getTargetClassName(clz)}.java"
        if (!Enum.class.isAssignableFrom(clz)) {
            file.content = """package org.zstack.sdk;

public class ${getTargetClassName(clz)} ${Object.class == clz.superclass ? "" : "extends " + clz.superclass.simpleName} {

${output.join("\n")}
}
"""
        } else {
            file.content = """package org.zstack.sdk;

public enum ${getTargetClassName(clz)} {
${output.join("\n")}
}
"""
        }

        sourceClassMap[clz.name] = "org.zstack.sdk.${getTargetClassName(clz)}"
        sdkFileMap.put(clz, file)
    }

    def isZStackClass(Class clz) {
        if (clz.name.startsWith("java.") || int.class == clz || long.class == clz
                || short.class == clz || char.class == clz || boolean.class == clz || float.class == clz
                || double.class == clz) {
            return false
        } else if (clz.canonicalName.startsWith("org.zstack")) {
            return true
        } else {
            throw new CloudRuntimeException("${clz.name} is neither JRE class nor ZStack class")
        }
    }

    def addToLaterResolvedClassesIfNeed(Class clz) {
        if (!sdkFileMap.containsKey(clz)) {
            laterResolvedClasses.add(clz)
        }
        Platform.reflections.getSubTypesOf(clz).forEach({ i ->
            if (!sdkFileMap.containsKey(i)) {
                laterResolvedClasses.add(i)
            }
        })
    }

    def generateResponseClass(Class responseClass) {
        logger.debug("generating class: ${responseClass.name}")

        RestResponse at = responseClass.getAnnotation(RestResponse.class)

        def fields = [:]

        def addToFields = { String fname, Field f ->
            if (isZStackClass(f.type)) {
                addToLaterResolvedClassesIfNeed(f.type)
                fields[fname] = f
            } else {
                fields[fname] = f
            }
        }

        if (!at.allTo().isEmpty()) {
            Field f = responseClass.getDeclaredField(at.allTo())
            addToFields(at.allTo(), f)
        } else {
            if (at.fieldsTo().length == 1 && at.fieldsTo()[0] == "all") {
                for (Field f : responseClass.getDeclaredFields()) {
                    addToFields(f.name, f)
                }
            } else {
                at.fieldsTo().each { s ->
                    def ss = s.split("=")

                    def dst, src
                    if (ss.length == 2) {
                        dst = ss[0].trim()
                        src = ss[1].trim()
                    } else {
                        dst = src = ss[0]
                    }

                    Field f = responseClass.getDeclaredField(src)
                    addToFields(dst, f)
                }
            }
        }

        // hack
        if (APIQueryReply.class.isAssignableFrom(responseClass)) {
            addToFields("total", responseClass.superclass.getDeclaredField("total"))
        }
        
        def output = []
        fields.each { String name, Field f ->
            output.add(makeFieldText(name, f))
        }

        def className = responseClass.simpleName
        className = StringUtils.removeStart(className, "API")
        className = StringUtils.removeEnd(className, "Event")
        className = StringUtils.removeEnd(className, "Reply")
        className = StringUtils.capitalize(className)
        className = "${className}Result"

        SdkFile file = new SdkFile()
        file.fileName = "${className}.java"
        file.content = """package org.zstack.sdk;

public class ${className} {
${output.join("\n")}
}
"""
        sdkFileMap[responseClass] = file
    }

    def makeFieldText(String fname, Field field) {
        // zstack type
        if (isZStackClass(field.type) || Enum.class.isAssignableFrom(field.type)) {
            addToLaterResolvedClassesIfNeed(field.type)

            return """\
    public ${getTargetClassName(field.type)} ${fname};
    public void set${StringUtils.capitalize(fname)}(${getTargetClassName(field.type)} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${getTargetClassName(field.type)} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
        }

        // java type
        if (Collection.class.isAssignableFrom(field.type)) {
            Class genericType = FieldUtils.getGenericType(field)

            if (genericType != null) {
                if (isZStackClass(genericType)) {
                    addToLaterResolvedClassesIfNeed(genericType)
                }

                String genericClassName = getTargetClassName(genericType)

                return """\
    public ${field.type.name}<${genericClassName}> ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name}<${genericClassName}> ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name}<${genericClassName}> get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
            } else {
                return """\
    public ${field.type.name} ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
            }
        } else if (Map.class.isAssignableFrom(field.type)) {
            Class genericType = FieldUtils.getGenericType(field)

            if (genericType != null) {
                if (isZStackClass(genericType)) {
                    addToLaterResolvedClassesIfNeed(genericType)
                }

                String genericClassName = getTargetClassName(genericType)

                return """\
    public ${field.type.name}<String, ${genericClassName}> ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name}<String, ${genericClassName}> ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name}<String, ${genericClassName}> get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
            } else {
                return """\
    public ${field.type.name} ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
            }
        } else {
            return """\
    public ${field.type.name} ${fname};
    public void set${StringUtils.capitalize(fname)}(${field.type.name} ${fname}) {
        this.${fname} = ${fname};
    }
    public ${field.type.name} get${StringUtils.capitalize(fname)}() {
        return this.${fname};
    }
"""
        }
    }
}
