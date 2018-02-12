package scripts

import org.zstack.tool.doclet.APIEventDoc
import org.zstack.tool.doclet.APIMessageDoc
import org.zstack.tool.doclet.InventoryDoc
import org.zstack.tool.doclet.RestructuredTextWriter
import org.zstack.utils.StringDSL

public class RestructuredTextWriterImpl implements RestructuredTextWriter {

    static def makePath(String output) {
        File f = new File(output)
        if (!f.exists()) {
            f.mkdirs()
        }
    }

    static class Line {
        int len
        int maxLength
        def words = []
        int indent

        Line(int maxLen = 60) {
            this.maxLength = maxLen
        }

        boolean isFull() {
            return len >= maxLength
        }

        boolean addWord(String word) {
            words.add(word)
            len += (word.length() + 1)
            return isFull()
        }

        boolean canAdd(String word) {
            return (len + word.length() + 1) <= maxLength
        }

        void indent(int offset) {
            this.indent = offset
        }

        String toString() {
            if (indent != 0) {
                words.add(0, " ".multiply(indent))
            }
            return words.join(" ")
        }
    }

    static class FixedLengthText {
        def lines = []
        String text
        int maxLineLength

        private void wrap() {
            text = text.replaceAll("\n", " ")
            def words = text.split()
            Line line = new Line(maxLineLength)
            lines.add(line)
            words.each { w ->
                if (!line.canAdd(w)) {
                    line = new Line(maxLineLength)
                    lines.add(line)
                }
                line.addWord(w)
            }
        }

        FixedLengthText(String text, int maxLen=60) {
            this.text = text
            this.maxLineLength = maxLen
        }

        String toString() {
            def strs = []
            lines.each { l ->
                strs.add(l.toString())
            }
            return strs.join("\n")
        }

        void indent(int start=0, int offset=0) {
            if (offset == 0) {
                return
            }

            if (lines.size() > start) {
                for (int i=start; i<lines.size(); i++) {
                    lines[i].indent(offset)
                }
            }
        }
    }

    static String wrapText(String text, int indent=0, int maxLength=60) {
        FixedLengthText txt = new FixedLengthText(text, maxLength)
        txt.wrap()
        txt.indent(1, indent)
        return txt.toString()
    }

    static String indentLine(String line, int offset) {
        return " ".multiply(offset) + line;
    }

    static String indentText(String text, int offset) {
        def lst = text.split("\n")
        def ret = []
        lst.each { l ->
            ret.add(indentLine(l, offset))
        }

        return ret.join("\n")
    }

    static String alignText(String text, int offset) {
        def lst = text.split("\n")
        if (lst.size() <= 1) {
            return text
        }

        def ret = []
        ret.add(lst[0])
        for (int i=1; i<lst.size(); i++) {
            ret.add(indentLine(lst[i], offset))
        }

        return ret.join('\n')
    }

    static def titleString(String title) {
        return """\
${title}
${'+'.multiply(title.length())}
"""
    }

    static def makeInventoryDocString(InventoryDoc doc) {
        def fs = []
        for (InventoryDoc.InventoryField f : doc.fields) {
            def fieldTemplate = """\
   * - **${alignText(f.name, 6)}**
     - ${alignText(f.description, 6)}
     - *${alignText(f.nullable.toString(), 6)}*
     - ${alignText(f.choices == null ? '' : f.choices, 6)}
     - ${alignText(f.since, 6)}
"""
            fs.add(fieldTemplate.toString())
        }

        def fields = fs.join("")
        def template = """\
.. _${doc.name}:

${titleString(StringDSL.splitCamelCase(doc.name))}

${trimText(doc.description)}

Fields:
-------

.. list-table::
   :widths: 20 40 10 20 10
   :header-rows: 1

   * - Name
     - Description
     - Nullable
     - Choices
     - Since
${fields}

Examples:
---------

::

${indentText(doc.example, 4)}

Since:
------

${doc.since}
"""
        return template.toString()
    }

    private static String pathJoin(String...paths) {
        String path = paths.join("/");
        return new File(path).getAbsolutePath();
    }

    @Override
    void writeInventory(String outputPath, Map<String, InventoryDoc> docs) {
        makePath(outputPath)
        for (def doc : docs.values()) {
            String inv = makeInventoryDocString(doc)
            String fileName = doc.name.replaceAll(" ", "");
            new File(pathJoin(outputPath, String.format("%s.rst", fileName)))
                    .withWriter { out ->
                out << inv
            }
        }
    }

    @Override
    void writeApiMessage(String outputPath, Map<String, APIMessageDoc> docs) {
        makePath(outputPath)
        for (def doc : docs.values()) {
            String api = makeApiDocString(doc)
            String fileName = doc.name.replaceAll(" ", "");
            new File(pathJoin(outputPath, String.format("%s.rst", fileName)))
                    .withWriter { out ->
                out << api
            }
        }
    }

    @Override
    void writeApiEvent(String outputPath, Map<String, APIEventDoc> docs) {
        makePath(outputPath)
        for (def doc : docs.values()) {
            String inv = makeApiEventDocString(doc)
            String fileName = doc.name.replaceAll(" ", "");
            new File(pathJoin(outputPath, String.format("%s.rst", fileName)))
                    .withWriter { out ->
                out << inv
            }
        }
    }

    static String trimText(String text) {
        def ret = []
        text.split("\n").each {
            it = it.trim()
            ret.add(it)
        }
        return ret.join("\n")
    }

    static String makeApiEventDocString(APIEventDoc doc) {
        def fs = []
        for (APIEventDoc.EventField f : doc.fields) {
            def fieldTemplate = """\
   * - **${alignText(f.name, 6)}**
     - ${alignText(f.description, 6)}
     - *${alignText(f.nullable.toString(), 6)}*
     - ${alignText(f.choices == null ? '' : f.choices, 6)}
     - ${alignText(f.since, 6)}
"""
            fs.add(fieldTemplate.toString())
        }

        def fields = fs.join("")
        def template = """\
.. _${doc.name}:

${titleString(doc.name)}

${trimText(doc.description)}

Fields:
-------

.. list-table::
   :widths: 20 40 10 20 10
   :header-rows: 1

   * - Name
     - Description
     - Nullable
     - Choices
     - Since
${fields}

Examples:
---------

::

${indentText(doc.example, 4)}

Since:
------

${doc.since}
"""
        return template.toString()
    }

    static String makeApiDocString(APIMessageDoc doc) {
        def parameters = []
        for (def p in doc.parameters) {
            def ptmpt = """\
   * - **${alignText(p.name, 6)}**
     - ${alignText(p.description, 6)}
     - *${alignText(p.optional.toString(), 6)}*
     - ${alignText(p.choices == null ? '' : p.choices, 6)}
     - ${alignText(p.since, 6)}
"""
            parameters.add(ptmpt.toString())
        }

        def parameterText = parameters.join("")

        def template = """\
.. _${doc.name}:

${titleString(doc.name)}

Full Name:
----------

${doc.fullName}

Description:
------------

${trimText(doc.description)}

Parameters:
-----------

.. list-table::
   :widths: 20 40 10 20 10
   :header-rows: 1

   * - Name
     - Description
     - Optional
     - Choices
     - Since
${parameterText}

Result:
-------

${doc.result}

Http Message Example:
---------------------

.. note:: This sample is for clients that talk to zstack management server through http post.
          In this case, serviceId is ommited in message.
          Web client, zstack CLI use this mediaType.

::

${indentText(doc.httpMessage, 4)}

Message Example:
----------------

.. note:: This sample is for clients that talk to zstack management server through RabbitMQ message bus.
          In this case, the message must include serviceId 'api.portal' and id which is a stripped uuid4,
          timeout is optional

::

${indentText(doc.message, 4)}


CLI Example:
------------

::

${doc.cli == null ? '' : indentText(doc.cli, 4)}

Since:
------

${doc.since}
"""
        return template.toString()
    }
}