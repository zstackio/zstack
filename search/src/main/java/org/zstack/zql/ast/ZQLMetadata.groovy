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
        Class selfInventoryClass
        Inventory inventoryAnnotation
        Map<String, ExpandQueryMetadata> expandQueries = [:]
        Map<String, ExpandQueryAliasMetadata> expandQueryAliases = [:]
    }

    /**
     * key: the full class name of inventory, e.g. org.zstack.host.HostInventory
     * value: InventoryMetadata
     */
    static Map<String, InventoryMetadata> inventoryMetadata = [:]

    interface MetadataPair {
    }

    static class SingleFieldMetadataPair implements MetadataPair {
        String fieldName
    }

    static class FieldMetadataPair implements MetadataPair {
        InventoryMetadata left
        String fieldName
    }

    static class ExpandQueryMetadataPair implements MetadataPair {
        InventoryMetadata left
        InventoryMetadata right
    }

    List<MetadataPair> getMetadataPair(String inventoryName, List<String> nestConditionNames) {
        assert !nestConditionNames.isEmpty() : "empty nestConditionNames for inventoryName[${inventoryName}]"

        InventoryMetadata metadata = inventoryMetadata.find { it.key.equalsIgnoreCase(inventoryName) }?.value
        if (metadata == null) {
            throw new CloudRuntimeException("cannot find metadata for query target[${inventoryName}]")
        }

        List<MetadataPair> ret = []

        if (nestConditionNames.size() == 1) {
            return ret.add(new SingleFieldMetadataPair(fieldName: nestConditionNames[0]))
        }

        Iterator<String> iterator = nestConditionNames.iterator()
        InventoryMetadata left = null
        while (iterator.hasNext()) {
            if (left == null) {
                left = getNextInventoryMetadata(metadata, iterator.next())
                continue
            }
        }
    }

    private class MetadataPairGetter {
        String inventoryName
        List<String> nestConditionNames

        List<MetadataPair> get() {
            assert !nestConditionNames.isEmpty() : "empty nestConditionNames for inventoryName[${inventoryName}]"

            InventoryMetadata metadata = inventoryMetadata.find { it.key.equalsIgnoreCase(inventoryName) }?.value
            if (metadata == null) {
                throw new CloudRuntimeException("cannot find metadata for query target[${inventoryName}]")
            }

            List<MetadataPair> ret = []

            nestConditionNames = preProcessingNestConditionNames(metadata, nestConditionNames)

            return ret
        }

        private List<String> preProcessingNestConditionNames(InventoryMetadata current, List<String> names) {
            List<String> ret = []

            names.each {
                def alias = current.expandQueryAliases[it]
                if (alias != null) {
                    ret.addAll(preProcessingNestConditionNames(current, alias.expandQueryText.split("\\.") as List))
                    return
                }

                def expand = current.expandQueries[it]
                assert expand != null : "invalid nested query condition[${names.join(".")}] on ${current.selfInventoryClass}," +
                            "the expanded target[${current.selfInventoryClass}] have no expanded query[${it}]"

                current = inventoryMetadata[expand.targetInventoryClass.name]
                assert current != null : "unable to find inventory metadata for ${expand.targetInventoryClass}"

                ret.add(it)
            }

            return ret
        }
    }

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class).each { clz ->
            Inventory inventory = clz.getAnnotation(Inventory.class)
            InventoryMetadata metadata = new InventoryMetadata(inventoryAnnotation: inventory, selfInventoryClass: clz)

            ExpandedQueries queries = clz.getAnnotation(ExpandedQueries.class)
            if (queries != null) {
                queries.value().each {
                    Class targetInventoryClass = it.inventoryClass()
                    if (!targetInventoryClass.isAnnotationPresent(Inventory.class)) {
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

                    metadata.expandQueries[emetadata.name] = emetadata
                }
            }

            ExpandedQueryAliases aliases = clz.getAnnotation(ExpandedQueryAliases.class)
            if (aliases != null) {
                aliases.value().each {
                    metadata.expandQueryAliases[it.alias()] = new ExpandQueryAliasMetadata(aliasName: it.alias(), expandQueryText: it.expandedField())
                }
            }

            inventoryMetadata[clz.name] = metadata
        }
    }
}
