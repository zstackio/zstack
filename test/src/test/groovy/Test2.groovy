import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import org.zstack.core.Platform
import org.zstack.header.message.APIMessage
import org.zstack.header.message.APISyncCallMessage
import org.zstack.header.notification.ApiNotification
import org.zstack.header.rest.RestRequest
import org.zstack.sdk.Param

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 {
    void test() {
        //new NotificationGenerator().generate()
        //new ApiNotificationGenerator().generate()

        def cu = JavaParser.parse(new File("/root/APIStartVmInstanceMsg.java"))
        cu.addImport("org.zstack.header.notification.ApiNotification")

        EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC)
        MethodDeclaration method = new MethodDeclaration(modifiers, new ClassOrInterfaceType("ApiNotification"), "__notification__")
        Parameter param = new Parameter(new ClassOrInterfaceType("APIStartVmInstanceEvent"), "event")
        method.addParameter(param)

        BlockStmt block = new BlockStmt()
        ReturnStmt ret = new ReturnStmt("null")
        block.addStatement(ret)
        method.setBody(block)

        for (TypeDeclaration type : cu.types) {
            if (type instanceof ClassOrInterfaceDeclaration) {
                type.addMember(method)
            }
        }

        println(cu.toString())
    }


    @org.junit.Test
    void test1() {
        def src = System.getProperty("src")
        assert src!= null: "please specify the source root by -Dsrc"

        Map<String, File> sources = [:]
        new File(src).traverse {
            if (it.name.endsWith(".java")) {
                sources[it.absolutePath] = it
            }
        }

        def apiClasses = Platform.getReflections().getSubTypesOf(APIMessage.class).findAll {
            return !java.lang.reflect.Modifier.isAbstract(it.modifiers) && !APISyncCallMessage.isAssignableFrom(it) && it.isAnnotationPresent(RestRequest.class)
        }

        apiClasses.each { clz ->
            if (clz.getMethods().find { it.getReturnType() == ApiNotification.class } != null ) {
                return
            }

            def clzPath = clz.package.name.replaceAll("\\.", "/")
            def key = sources.keySet().find { it.contains(clzPath) }
            assert key != null: "cannot find the source file containing path of $clzPath"

            File file = sources[key]

            def cu = JavaParser.parse(file)
            cu.addImport("org.zstack.header.notification.ApiNotification")
            RestRequest rest = clz.getAnnotation(RestRequest.class)

            EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC)
            MethodDeclaration method = new MethodDeclaration(modifiers, new ClassOrInterfaceType("ApiNotification"), "__notification__")
            Parameter param = new Parameter(new ClassOrInterfaceType(rest.responseClass().simpleName), "event")
            method.addParameter(param)

            BlockStmt block = new BlockStmt()
            ReturnStmt ret = new ReturnStmt("null")
            block.addStatement(ret)
            method.setBody(block)

            for (TypeDeclaration type : cu.types) {
                if (type instanceof ClassOrInterfaceDeclaration) {
                    type.addMember(method)
                }
            }

            new File("/tmp", clz.simpleName + ".java").write(cu.toString())
        }
    }
}
