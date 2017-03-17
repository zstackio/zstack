package org.zstack.testlib

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import org.zstack.core.Platform
import org.zstack.header.message.APIMessage
import org.zstack.header.message.APISyncCallMessage
import org.zstack.header.rest.RestRequest

import java.lang.reflect.Modifier

/**
 * Created by xing5 on 2017/3/16.
 */
class ApiNotificationGenerator {
    void generate() {
        def src = System.getProperty("src")
        assert src!= null: "please specify the source root by -Dsrc"

        new File(src).traverse {
        }

        /*
        def apiClasses = Platform.getReflections().getSubTypesOf(APIMessage.class).findAll {
            return !Modifier.isAbstract(it.modifiers) && !APISyncCallMessage.isAssignableFrom(it) && it.isAnnotationPresent(RestRequest.class)
        }

        apiClasses.each {

        }

        def s = """\
    public ApiNotification __notification__(APIStartVmInstanceEvent evt) {
        return new ApiNotification() {
            @Override
            public void makeNotifications() {
                ntfy("Starting VM").resource(uuid, VmInstanceVO.class.getSimpleName())
                        .successOrNot(evt).done();
            }
        };
    }
"""
        def newLines = []
        for (int i=0; i<end; i++) {
            newLines.add(lines[i])
        }
        newLines.add(s.split("\n"))
        newLines.add("}")

        println(newLines.join("\n"))
        */
    }
}
