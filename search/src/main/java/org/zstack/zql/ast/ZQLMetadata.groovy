package org.zstack.zql.ast

import org.zstack.header.core.StaticInit
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.query.ExpandedQueries
import org.zstack.header.query.ExpandedQuery
import org.zstack.header.query.ExpandedQueryAlias
import org.zstack.header.query.ExpandedQueryAliases
import org.zstack.header.rest.APINoSee
import org.zstack.header.search.Inventory
import org.zstack.utils.BeanUtils
import org.zstack.utils.FieldUtils

class ZQLMetadata {
    static class ExpandQueryMetadata {
        Class selfVOClass
        Class targetVOClass
        Class targetInventoryClass
        String selfKeyName
        String targetKeyName
        String name
        boolean hidden
    }

    static class ExpandQueryAliasMetadata {
        String aliasName
        String expandQueryText
    }

    static class InventoryMetadata {
        Class selfInventoryClass
        transient Inventory inventoryAnnotation
        Map<String, ExpandQueryMetadata> expandQueries = [:]
        Map<String, ExpandQueryAliasMetadata> expandQueryAliases = [:]
        Set<String> selfInventoryFieldNames

        boolean hasInventoryField(String fname) {
            return selfInventoryFieldNames.contains(fname)
        }

        void errorIfNoField(String fname) {
            if (!hasInventoryField(fname)) {
                throw new CloudRuntimeException("inventory[${selfInventoryClass}] has no field[${fname}]")
            }
        }

        boolean isUs(String inventoryName) {
            return selfInventoryClass.simpleName.equalsIgnoreCase(inventoryName)
        }

        String fullInventoryName() {
            return selfInventoryClass.name
        }

        String simpleInventoryName() {
            return selfInventoryClass.simpleName
        }
    }

    /**
     * key: the full class name of inventory, e.g. org.zstack.host.HostInventory
     * value: InventoryMetadata
     */
    static Map<String, InventoryMetadata> inventoryMetadata = [:]
    /**
     * key: the full class name of inventory, e.g. org.zstack.host.HostVO
     * value: InventoryMetadata
     */
    static Map<String, InventoryMetadata> inventoryMetadataByVOClassName = [:]

    trait ChainQueryStruct {
        void verify() {
        }
    }

    static InventoryMetadata findInventoryMetadata(String queryName) {
        queryName = "${queryName}inventory"
        def entry = inventoryMetadata.find { it.value.isUs(queryName) }
        assert entry != null : "cannot find inventory with name[${queryName}]"
        return entry.value
    }

    static InventoryMetadata getInventoryMetadataByVOClassName(Class clz) {
        while (clz != Object.class) {
            InventoryMetadata m = ZQLMetadata.inventoryMetadataByVOClassName[clz.name]
            if (m != null) {
                return m
            }

            clz = clz.superclass
        }

        assert false : "cannot find metadata for inventory class[${clz}]"
    }

    static InventoryMetadata getInventoryMetadataByName(String name) {
        InventoryMetadata m = inventoryMetadata[name]
        assert m != null : "cannot find metadata for inventory class[${name}]"
        return m
    }

    static class FieldChainQuery implements ChainQueryStruct {
        InventoryMetadata self
        ExpandQueryMetadata right
        String fieldName

        @Override
        void verify() {
            if (right != null) {
                InventoryMetadata him = getInventoryMetadataByName(right.targetInventoryClass.name)
                if (!him.hasInventoryField(fieldName)) {
                    //throw new OperationFailureException(Platform.argerr("inventory class[%s] not having field[%s]", left.selfInventoryClass, fieldName))
                    throw new CloudRuntimeException("inventory class[${him.selfInventoryClass}] not having field[${fieldName}]")
                }
            } else {
                if (!self.hasInventoryField(fieldName)) {
                    throw new CloudRuntimeException("inventory class[${self.selfInventoryClass}] not having field[${fieldName}]")
                }
            }
        }


        @Override
        String toString() {
            return "FieldChainQuery{" +
                    "self=" + self.selfInventoryClass +
                    ", right=" + right.name +
                    ", fieldName='" + fieldName + '\'' +
                    '}'
        }
    }

    static class ExpandChainQuery implements ChainQueryStruct {
        String selfKey
        InventoryMetadata self
        ExpandQueryMetadata right


        @Override
        String toString() {
            return "ExpandChainQuery{" +
                    "selfKey='" + selfKey + '\'' +
                    ", self=" + self.selfInventoryClass +
                    ", right=" + right.name +
                    '}'
        }
    }

    static List<ChainQueryStruct> createChainQuery(String inventoryName, List<String> nestConditionNames) {
        return new ChainQueryStructGetter(inventoryName: inventoryName, nestConditionNames: nestConditionNames).get()
    }

    private static class ChainQueryStructGetter {
        String inventoryName
        List<String> nestConditionNames

        List<ChainQueryStruct> get() {
            assert !nestConditionNames.isEmpty() : "empty nestConditionNames for inventoryName[${inventoryName}]"

            InventoryMetadata metadata = inventoryMetadata.find { it.key.equalsIgnoreCase(inventoryName) }?.value
            if (metadata == null) {
                throw new CloudRuntimeException("cannot find metadata for query target[${inventoryName}]")
            }

            List<ChainQueryStruct> ret = []

            nestConditionNames = preProcessingNestConditionNames(metadata, nestConditionNames)

            if (nestConditionNames.size() == 1) {
                ret.add(new FieldChainQuery(self: metadata, fieldName: nestConditionNames[0]))
            } else {
                Iterator<String> iterator = nestConditionNames[0..nestConditionNames.size() - 2].iterator()

                InventoryMetadata self = metadata
                ExpandQueryMetadata left = null
                while (iterator.hasNext()) {
                    String expandedQueryName = iterator.next()
                    ExpandQueryMetadata e = self.expandQueries[expandedQueryName]
                    assert e != null: "no expand query[${expandedQueryName}] found on ${self.selfInventoryClass}"
                    ExpandChainQuery em = new ExpandChainQuery(selfKey: left == null ? null : left.targetKeyName, self: self, right: e)
                    self = getInventoryMetadataByName(em.right.targetInventoryClass.name)
                    left = em.right
                    ret.add(em)
                }

                ExpandChainQuery last = ret.last() as ExpandChainQuery
                ret.add(new FieldChainQuery(right: last.right, self: last.self, fieldName: nestConditionNames.last()))
            }

            ret.each { it.verify() }

            return ret
        }

        private List<String> preProcessingNestConditionNames(InventoryMetadata current, List<String> names, boolean removeLast=true) {
            List<String> ret = []

            def toProcessing = names.size() == 1 ? [] : removeLast ? names[0..names.size()-2] : names

            toProcessing.each {
                def alias = current.expandQueryAliases[it]
                if (alias != null) {
                    ret.addAll(preProcessingNestConditionNames(current, alias.expandQueryText.split("\\.") as List, false))
                    return
                }

                def expand = current.expandQueries[it]
                assert expand != null : "invalid nested query condition[${names.join(".")}] on ${current.selfInventoryClass}," +
                            "the expanded target[${current.selfInventoryClass}] have no expanded query[${it}]"

                current = inventoryMetadata[expand.targetInventoryClass.name]
                assert current != null : "unable to find inventory metadata for ${expand.targetInventoryClass}"

                ret.add(it)
            }

            if (removeLast) {
                ret.add(names.last())
            }

            return ret
        }
    }

    private static void fillInventoryMetadata(Class clz,
                                              List<ExpandedQuery> queries, List<ExpandedQueryAlias> aliases,
                                              List<ExpandedQuery> queryForOther, List<ExpandedQueryAlias> aliasForOther) {
        Inventory inventory = clz.getAnnotation(Inventory.class)
        assert inventory : "class[${clz}] not annotated by @Inventory"

        InventoryMetadata metadata = inventoryMetadata[clz.name]
        if (metadata == null) {
            metadata = new InventoryMetadata(
                    inventoryAnnotation: inventory,
                    selfInventoryClass: clz,
                    selfInventoryFieldNames: FieldUtils.getAllFields(clz).findAll {
                        return !it.isAnnotationPresent(APINoSee.class)
                    }.collect { it.name } as Set<String>
            )
        }

        if (queries != null) {
            queries.each {
                if (it.target() != Object.class && it.target() != clz) {
                    assert queryForOther != null : "found ${clz} has an expanded query with target[${it.target()}], but queryForOther == null"
                    queryForOther.add(it)
                    return
                }

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
                        name: it.expandedField(),
                        hidden: it.hidden()
                )

                metadata.expandQueries[emetadata.name] = emetadata
            }
        }

        if (aliases != null) {
            aliases.each {
                if (it.target() != Object.class && it.target() != clz) {
                    assert queryForOther != null : "found ${clz} has an expanded alias with target[${it.target()}], but aliasForOther == null"
                    aliasForOther.add(it)
                    return
                }

                metadata.expandQueryAliases[it.alias()] = new ExpandQueryAliasMetadata(aliasName: it.alias(), expandQueryText: it.expandedField())
            }
        }

        inventoryMetadata[clz.name] = metadata
    }

    @StaticInit
    static void staticInit() {
        List<ExpandedQuery> queryForOther = []
        List<ExpandedQueryAlias> aliasForOther = []

        BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class).findAll{ it.isAnnotationPresent(Inventory.class) }
                .each { clz ->
            ExpandedQueries queries = clz.getAnnotation(ExpandedQueries.class)
            ExpandedQueryAliases aliases = clz.getAnnotation(ExpandedQueryAliases.class)
            fillInventoryMetadata(clz,
                    queries ? queries.value() as List : null,
                    aliases ? aliases.value() as List : null,
                    queryForOther, aliasForOther)
        }

        queryForOther.each {
            Class clz = it.target()
            fillInventoryMetadata(clz, [it], null,  null, null)
        }

        aliasForOther.each {
            Class clz = it.target()
            fillInventoryMetadata(clz, null, [it],  null, null)
        }

        inventoryMetadata.values().each { inventoryMetadataByVOClassName[it.inventoryAnnotation.mappingVOClass().name] = it }
    }
}
