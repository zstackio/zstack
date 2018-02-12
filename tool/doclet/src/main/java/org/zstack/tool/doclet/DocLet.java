package org.zstack.tool.doclet;

import com.sun.javadoc.*;
import com.sun.tools.doclets.standard.Standard;
import groovy.lang.GroovyClassLoader;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.tool.doclet.APIEventDoc.EventField;
import org.zstack.tool.doclet.APIMessageDoc.ParameterDoc;
import org.zstack.tool.doclet.InventoryDoc.InventoryField;
import org.zstack.utils.StringDSL;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.InputStream;
import java.util.*;

/**
 */
public class DocLet extends Standard {
    private static final String INVENTORY_TAG = "inventory";
    private static final String EXAMPLE_TAG = "example";
    private static final String SINCE = "since";
    private static final String DESCRIPTION = "desc";
    private static final String IGNORE = "ignore";
    private static final String CHOICES = "choices";
    private static final String NULLABLE = "nullable";
    private static final String OPTIONAL = "optional";
    private static final String API = "api";
    private static final String MSG = "msg";
    private static final String HTTP_MSG = "httpMsg";
    private static final String CLI = "cli";
    private static final String API_RESULT = "apiResult";
    private static final String RESULT = "result";

    private static Map<String, InventoryDoc> inventories = new HashMap<String, InventoryDoc>();
    private static Map<String, APIMessageDoc> api = new HashMap<String, APIMessageDoc>();
    private static Map<String, APIEventDoc> events = new HashMap<String, APIEventDoc>();

    private static String currentEntityName;

    private static void log(String msg) {
        System.out.println(msg);
    }

    private static Tag getTag(Doc doc, String tagName, boolean exceptionOnMissing) {
        Tag[] tags = doc.tags(tagName);
        if (tags.length == 0 && exceptionOnMissing) {
            throw new RuntimeException(String.format("can not find tag[%s] on field[%s] of %s", tagName, doc.name(), currentEntityName));
        }

        if (tags.length == 0) {
            return null;
        }

        return tags[0];
    }

    private static Tag getTag(Doc doc, String tagName) {
        return getTag(doc, tagName, false);
    }

    private static boolean hasTag(Doc doc, String tagName) {
        Tag[] tags = doc.tags(tagName);
        return tags.length != 0;
    }

    private static List<FieldDoc> getAllFieldDocs(ClassDoc clazz) {
        List<FieldDoc> ret = new ArrayList<FieldDoc>();
        do {
            for (FieldDoc doc : clazz.fields()) {
                if (hasTag(doc, IGNORE)) {
                    continue;
                }

                if (doc.isStatic()) {
                    continue;
                }

                ret.add(doc);
            }
            clazz = clazz.superclass();
        } while (clazz != null && !clazz.qualifiedName().equals(Object.class.getName()));
        return ret;
    }

    private static void handleInventory(ClassDoc clazz) {
        log(String.format("generating doc for inventory[%s]...", clazz));
        currentEntityName = clazz.qualifiedName();

        InventoryDoc doc = new InventoryDoc();

        Tag tag = getTag(clazz, INVENTORY_TAG);
        doc.setName(clazz.simpleTypeName());
        doc.setFullName(clazz.qualifiedTypeName());
        doc.setDescription(tag.text());
        tag = getTag(clazz, EXAMPLE_TAG, true);
        Map example = JSONObjectUtil.toObject(tag.text(), LinkedHashMap.class);
        doc.setExample(JSONObjectUtil.dumpPretty(example));
        tag = getTag(clazz, SINCE, true);
        doc.setSince(tag.text());

        List<FieldDoc> fieldDocs = getAllFieldDocs(clazz);
        for (FieldDoc fieldDoc : fieldDocs) {
            if (hasTag(fieldDoc, IGNORE)) {
                continue;
            }

            InventoryField fdoc = new InventoryField();
            fdoc.setName(fieldDoc.name());
            Tag desc = getTag(fieldDoc, DESCRIPTION, true);
            fdoc.setDescription(desc.text());
            Tag choices = getTag(fieldDoc, CHOICES);
            if (choices != null) {
                fdoc.setChoices(choices.text());
            }
            fdoc.setType(fieldDoc.type().simpleTypeName());
            fdoc.setNullable(hasTag(fieldDoc, NULLABLE));
            Tag tsince = getTag(fieldDoc, SINCE);
            String since = tsince == null ? doc.getSince() : tsince.text();
            fdoc.setSince(since);
            doc.getFields().add(fdoc);
        }

        inventories.put(doc.getName(), doc);
        log(JSONObjectUtil.toJsonString(doc));

    }

    private static String makeDirectoryPath(String dirName) {
        return String.format(String.format("%s/%s/%s", System.getProperty("user.home"), "zstack-rst-doc", dirName));
    }

    private static RestructuredTextWriter getWriter() throws IllegalAccessException, InstantiationException {
        ClassLoader parent = DocLet.class.getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        InputStream in = DocLet.class.getClassLoader().getResourceAsStream("scripts/RestructuredTextWriterImpl.groovy");
        String script = StringDSL.inputStreamToString(in);
        Class writerClass = loader.parseClass(script);
        Object obj = writerClass.newInstance();
        return (RestructuredTextWriter) obj;
    }

    public static boolean start(RootDoc root) {
        ClassDoc[] classes = root.classes();
        for (ClassDoc c : classes) {
            if (hasTag(c, INVENTORY_TAG)) {
                handleInventory(c);
            } else if (hasTag(c, API)) {
                handleApi(c);
            } else if (hasTag(c, API_RESULT)) {
                handleApiEvent(c);
            }
        }

        try {

            RestructuredTextWriter writer = getWriter();
            String inventoryPath = makeDirectoryPath("inventory");
            writer.writeInventory(inventoryPath, inventories);
            String apiPath = makeDirectoryPath("api");
            writer.writeApiMessage(apiPath, api);
            apiPath = makeDirectoryPath("apiEvent");
            writer.writeApiEvent(apiPath, events);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    private static void handleApiEvent(ClassDoc c) {
        log(String.format("Generating doc for api event[%s]...", c));
        currentEntityName = c.qualifiedName();

        APIEventDoc doc = new APIEventDoc();
        doc.setName(c.simpleTypeName());
        doc.setFullName(c.qualifiedTypeName());
        Tag desc = getTag(c, API_RESULT, true);
        doc.setDescription(desc.text());
        Tag tag = getTag(c, EXAMPLE_TAG, true);
        Map example = JSONObjectUtil.toObject(tag.text(), LinkedHashMap.class);
        doc.setExample(JSONObjectUtil.dumpPretty(example));
        tag = getTag(c, SINCE, true);
        doc.setSince(tag.text());

        for (FieldDoc fieldDoc : getAllFieldDocs(c)) {
            EventField edoc = new EventField();
            edoc.setName(fieldDoc.name());
            desc = getTag(fieldDoc, DESCRIPTION, true);
            edoc.setDescription(desc.text());
            Tag choices = getTag(fieldDoc, CHOICES);
            if (choices != null) {
                edoc.setChoices(choices.text());
            }
            edoc.setNullable(hasTag(fieldDoc, NULLABLE));
            Tag tsince = getTag(fieldDoc, SINCE);
            String since = tsince == null ? doc.getSince() : tsince.text();
            edoc.setSince(since);
            doc.getFields().add(edoc);
        }

        events.put(doc.getName(), doc);
    }

    private static void handleApi(ClassDoc c) {
        log(String.format("Generating doc for api[%s]...", c));
        currentEntityName = c.qualifiedName();

        APIMessageDoc doc = new APIMessageDoc();
        doc.setName(c.simpleTypeName());
        doc.setFullName(c.qualifiedTypeName());
        Tag desc = getTag(c, API, true);
        doc.setDescription(desc.text());
        Tag msg = getTag(c, MSG, true);
        Map msgObj = JSONObjectUtil.toObject(msg.text(), LinkedHashMap.class);
        doc.setMessage(JSONObjectUtil.dumpPretty(msgObj));
        msg = getTag(c, HTTP_MSG, true);
        msgObj = JSONObjectUtil.toObject(msg.text(), LinkedHashMap.class);
        doc.setHttpMessage(JSONObjectUtil.dumpPretty(msgObj));
        Tag cli = getTag(c, CLI);
        if (cli != null) {
            doc.setCli(cli.text());
        }
        Tag since = getTag(c, SINCE, true);
        doc.setSince(since.text());
        Tag result = getTag(c, RESULT, true);
        doc.setResult(result.text());
        for (FieldDoc fieldDoc : getAllFieldDocs(c)) {
            ParameterDoc pdoc = new ParameterDoc();
            pdoc.setName(fieldDoc.name());
            desc = getTag(fieldDoc, DESCRIPTION, true);
            pdoc.setDescription(desc.text());
            pdoc.setOptional(hasTag(fieldDoc, OPTIONAL));
            since = getTag(c, SINCE);
            if (since != null) {
                pdoc.setSince(since.text());
            } else {
                pdoc.setSince(doc.getSince());
            }
            Tag choices = getTag(fieldDoc, CHOICES);
            if (choices != null) {
                pdoc.setChoices(choices.text());
            }
            doc.getParameters().add(pdoc);
        }

        log(JSONObjectUtil.toJsonString(doc));
        api.put(doc.getName(), doc);
    }
}
