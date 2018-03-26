package org.zstack.zql.ast

import org.zstack.header.core.StaticInit
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.query.ExpandedQueries
import org.zstack.header.query.ExpandedQueryAliases
import org.zstack.header.search.Inventory
import org.zstack.utils.BeanUtils

class ZQLMetadata {
    static class ExpandQueryMetadata {
        Class selfVOClass
        Class targetVOClass
        Class targetInventoryClass
        String selfKeyName
        String targetKeyName
        String name
    }

    static class ExpandQueryAliasMetadata {
        String aliasName
        String expandQueryText
    }

    static class InventoryMetadata {
        Inventory inventoryAnnotation
        Map<String, ExpandQueryMetadata> expandQueries = [:]
        Map<String, ExpandQueryAliasMetadata> expandQueryAliases = [:]
    }

    static Map<String, InventoryMetadata> inventoryMetadata = [:]

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class).each { clz ->
            Inventory inventory = clz.getAnnotation(Inventory.class)
            InventoryMetadata metadata = new InventoryMetadata(inventoryAnnotation: inventory)

            ExpandedQueries queries = clz.getAnnotation(ExpandedQueries.class)
            if (queries != null) {
                queries.value().each {
                    Class targetInventoryClass = it.inventoryClass()
                    if (targetInventoryClass.isAnnotationPresent(Inventory.class)) {
                        throw new CloudRuntimeException("inventory class[${targetInventoryClass}] is query expanded by ${clz} but not have @Inventory annotation")
                    }

                    ExpandQueryMetadata emetadata = new ExpandQueryMetadata(
                            selfVOClass: inventory.mappingVOClass(),
                            targetVOClass: targetInventoryClass.getAnnotation(Inventory.class).mappingVOClass(),
                            targetInventoryClass: it.inventoryClass(),
                            selfKeyName: it.foreignKey(),
                            targetKeyName: it.expandedInventoryKey(),
                            name: it.expandedField()
                    )

                    metadata[emetadata.name] = emetadata
                }
            }

            ExpandedQueryAliases aliases = clz.getAnnotation(ExpandedQueryAliases.class)
            if (aliases != null) {
                aliases.value().each {
                    metadata[it.alias()] = new ExpandQueryAliasMetadata(aliasName: it.alias(), expandQueryText: it.expandedField())
                }
            }

            inventoryMetadata[clz.simpleName] = metadata
        }
    }
}
