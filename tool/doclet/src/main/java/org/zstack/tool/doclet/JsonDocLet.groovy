package org.zstack.tool.doclet

import com.sun.javadoc.RootDoc
import com.sun.javadoc.Tag
import com.sun.tools.doclets.standard.Standard
import groovy.json.JsonOutput

class JsonDocLet extends Standard  {
    static String fileName = "zstack-doclet-output.json"
    static String outputPath = [System.getProperty("user.home"), fileName].join("/")
    static Map<String, Doc> docs = [:]

    static class Doc {
        String name
        String comment
        Map<String, String> tags
    }

    static class ClassDoc extends Doc {
        Map<String, Doc> fields = [:]
        Map<String, Doc> methods = [:]
    }

    private static Map<String, String> collectTags(Tag[] tags) {
        def ret = [:]
        tags.each {
            ret[it.name()] = it.text()
        }

        return ret
    }

    static boolean start(RootDoc root) {
        String path = System.getProperty("path")
        if (path != null) {
            outputPath = [path, fileName].join("/")
        }

        def dir = new File(outputPath).parentFile
        if (!dir.exists()) {
            dir.mkdirs()
        }

        println("total ${root.classes().length} classes to process")

        root.classes().each { clz ->
            println("processing class ${clz.qualifiedName()}")
            def clzDoc = new ClassDoc(
                    name: clz.qualifiedName(),
                    comment: clz.commentText(),
                    tags: collectTags(clz.tags())
            )

            clz.fields().collect { field ->
                def fieldDoc = new Doc(
                        name: field.name(),
                        comment: field.commentText(),
                        tags: collectTags(field.tags())
                )

                clzDoc.fields[fieldDoc.name] = fieldDoc

                field.annotations().each { at ->
                    def ans = []
                    at.elementValues().each { el ->
                        def an = [:]
                        an["name"] = el.element().name()
                        an["default"] = el.element().defaultValue().toString()
                        an["value"] = el.value().toString()
                        ans.add(an)
                    }
                }
            }

            docs[clzDoc.name] = clzDoc
        }

        new File(outputPath).write(JsonOutput.toJson(docs))
    }
}
