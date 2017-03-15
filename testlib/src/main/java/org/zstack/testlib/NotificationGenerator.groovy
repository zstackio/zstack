package org.zstack.testlib

import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.core.notification.Notification
import org.zstack.core.notification.NotificationType
import org.zstack.utils.ShellUtils

/**
 * Created by xing5 on 2017/3/15.
 */
class NotificationGenerator {

    void generate() {
        String src = System.getProperty("src")
        assert src != null: "use -Dsrc= to specify the root of your source code"

        def classes = Platform.getReflections().getTypesAnnotatedWith(Notification.class)
        classes.each { clz ->
            Notification at = clz.getAnnotation(Notification.class)

            String sender = at.sender().isEmpty() ? "NotificationConstant.SYSTEM_SENDER" : "\"${at.sender()}\""

            List methods = []
            at.names().each { name->
                NotificationType.getEnumConstants().each { type ->
                    methods.add("""\
    public static Builder ${name}${type.methodName}_(String content, Object...args) {
        return new Builder(content, args).name("${name}").type(NotificationType.${type});
    }
""")
                }

                methods.add("""\
    public static Builder ${name}Notify_(String content, Object...args) {
        return new Builder(content, args).name("${name}");
    }
""")
            }



            def names = at.names().collect { "\"$it\"" }

            def txt ="""package ${clz.package.name};

import org.zstack.core.notification.Notification;
import org.zstack.core.notification.NotificationBuilder;
import org.zstack.core.notification.NotificationConstant;
import org.zstack.core.notification.NotificationType;

@Notification(
        names = {${names.join(",")}},
        resourceType = "${at.resourceType()}",
        sender = "${at.sender()}"
)
public class ${clz.simpleName} {
    public static class Builder {
        String notificationName;
        String content;
        String sender = ${sender};
        String resourceUuid;
        String resourceType = "${at.resourceType()}";
        Object[] arguments;
        NotificationType type = NotificationType.Info;


        public Builder(String content, Object...args) {
            this.content = content;
            this.arguments = args;
        }

        private Builder name(String name) {
            notificationName = name;
            return this;
        }

        public Builder uuid(String uuid) {
            resourceUuid = uuid;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public void send() {
            new NotificationBuilder().name(notificationName)
                    .resource(resourceUuid, resourceType)
                    .sender(sender)
                    .type(type)
                    .content(content)
                    .arguments(arguments)
                    .send();
        }
    }
    
${methods.join("\n\n")}
}
"""
            def cmd = ShellUtils.runAndReturn("cd $src; find -name ${clz.simpleName}.java; cd - >/dev/null", false)
            cmd.raiseExceptionIfFail()
            def files = cmd.stdout.split("\n") as List
            assert !files.isEmpty(): "cannot find the file ${clz.simpleName}.java in $src"

            files = files.collect { StringUtils.removeEnd(it, "\n").trim() }

            def file
            if (files.size() > 1) {
                def fullName = "${clz.name.replaceAll("\\.", "/")}/${clz.simpleName}.java"
                def found = files.findAll { it.contains(fullName) }
                assert found.size() < 2: "multiple files found matching ${fullName} in\n $out"
                assert found.size() == 1: "cannot find the fine ${fullName} in ${src}"
                file = found[0]
            } else {
                file = files[0]
            }

            new File([src, file].join("/")).write(txt)
        }
    }
}
