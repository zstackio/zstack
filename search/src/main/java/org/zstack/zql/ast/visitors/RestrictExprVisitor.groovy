package org.zstack.zql.ast.visitors

import groovy.text.SimpleTemplateEngine
import org.springframework.beans.factory.annotation.Autowire
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.zstack.core.db.DBGraph
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.EntityMetadata
import org.zstack.zql.ZQLContext
import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.ZQLError
import org.zstack.zql.ast.ZQLMetadata

class RestrictExprVisitor implements ASTVisitor<String, ASTNode.RestrictExpr> {
    private static final OPERATOR_NAME = "__operatorName__"
    private static final VALUE_NAME = "__valueName__"

    @Override
    String visit(ASTNode.RestrictExpr node) {
        String srcTargetName = ZQLContext.peekQueryTargetInventoryName()
        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(srcTargetName)
        ZQLMetadata.InventoryMetadata dst = ZQLMetadata.findInventoryMetadata(node.entity)
        if (dst == null) {
            throw new ZQLError("invalid restrict by clause, inventory[${node.entity}] not found")
        }

        DBGraph.EntityVertex vertex = DBGraph.findVerticesWithSmallestWeight(src.inventoryAnnotation.mappingVOClass(), dst.inventoryAnnotation.mappingVOClass())
        if (vertex == null) {
            throw new ZQLError("invalid restrict by clause, inventory[${node.entity}] has no restriction to inventory[${src.simpleInventoryName()}]")
        }

        String template = makeQueryTemplate(vertex, node.field)
        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).name
        template = "(${src.simpleInventoryName()}.${primaryKey} IN ${template})"
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        return engine.createTemplate(template)
                .make([(OPERATOR_NAME): node.operator,(VALUE_NAME): (node.value as ASTNode).accept(new ValueVisitor())]).toString()
    }

    private String makeQueryTemplate(DBGraph.EntityVertex vertex, String field) {
        if (vertex.next == null) {
            String entity = "${vertex.entityClass.simpleName}_"
            String vo = vertex.entityClass.simpleName
            String key = vertex.previous.dstKey
            return "(SELECT ${entity}.${key} FROM ${vo} ${entity} WHERE ${entity}.${field} \${${OPERATOR_NAME}} \${${VALUE_NAME}})"
        }

        String value = makeQueryTemplate(vertex.next, field)
        String entity = "${vertex.entityClass.simpleName}_"
        String vo = vertex.entityClass.simpleName
        String primaryKey = EntityMetadata.getPrimaryKeyField(vertex.entityClass).name
        return "(SELECT ${entity}.${primaryKey} FROM ${vo} ${entity} WHERE ${entity}.${vertex.srcKey} = ${value})"
    }
}
