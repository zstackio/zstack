package org.zstack.identity.rbac.datatype;

import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.search.Inventory;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Entity {
    private static Map<Class, Entity> allEntities = new HashMap<>();

    private Entity parent;
    private List<Entity> children = new ArrayList<>();
    private Map<String, Field> fields = new HashMap<>();
    private Class entityClass;

    private static void buildEntity(Class clz) {
        if (allEntities.containsKey(clz)) {
            return;
        }

        Entity e = new Entity(clz);
        allEntities.put(clz, e);

        // find children
        BeanUtils.reflections.getSubTypesOf(clz).forEach(arg -> {
            Class c = (Class) arg;
            Entity ce = allEntities.computeIfAbsent(c, x->new Entity(c));
            e.children.add(ce);
            ce.parent = e;
        });
    }

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(APIMessage.class)
                .stream().filter(clz -> !Modifier.isStatic(clz.getModifiers()))
                .forEach(Entity::buildEntity);
        BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class)
                .stream().filter(clz -> !Modifier.isStatic(clz.getModifiers()))
                .forEach(Entity::buildEntity);
    }

    public Entity(Class entityClass) {
        this.entityClass = entityClass;
        FieldUtils.getAllFields(entityClass)
                .stream().filter(f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers()))
                .forEach(f -> {
                    fields.put(f.getName(), f);
                    f.setAccessible(true);
                });
    }

    public Entity getParent() {
        return parent;
    }

    public void setParent(Entity parent) {
        this.parent = parent;
    }

    public List<Entity> getChildren() {
        return children;
    }

    public void setChildren(List<Entity> children) {
        this.children = children;
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    public void setFields(Map<String, Field> fields) {
        this.fields = fields;
    }

    public Class getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class entityClass) {
        this.entityClass = entityClass;
    }

    public static Entity getEntity(Class clz) {
        Entity e = allEntities.get(clz);
        if (e == null) {
            throw new CloudRuntimeException(String.format("cannot find entity for the class[%s]", clz));
        }
        return e;
    }
}
