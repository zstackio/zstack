package org.zstack.testlib.tool

import org.apache.logging.log4j.util.Strings
import org.zstack.core.Platform
import org.zstack.header.vo.EO
import org.zstack.header.vo.ResourceVO
import org.zstack.utils.path.PathUtil

import javax.persistence.Entity

class TermsGenerator {
    static String termFileFormat = "terms_%s.properties"

    static String translationFileFormat = "translation_from_%s_to_%s.properties"

    static List<TermTranslatePropertyLine> generateTermsOfZStackVersion(String version) {
        String outputDir = System.getProperty("user.dir")

        Set<Class> resourceVOs = Platform.reflections.getSubTypesOf(ResourceVO.class)
        resourceVOs = resourceVOs.findAll { return it.isAnnotationPresent(Entity.class) && !it.isAnnotationPresent(EO.class) }

        List<TermTranslatePropertyLine> linesFromProperties = loadFileFromResource(PathUtil.join(outputDir, '../../conf', String.format(termFileFormat, version)))

        List<String> termKeysFromResourceConfig = resourceVOs.collect {
            // VmInstanceVO -> VmInstance
            return it.simpleName.replaceAll("VO\$", "").replaceAll("EO\$", "")
        }.sort()

        List<String> missingTerms = termKeysFromResourceConfig.findAll { termKey ->
            return !linesFromProperties.any { it.term == termKey }
        }

        List<TermTranslatePropertyLine> linesToWrite = linesFromProperties + missingTerms.collect {
            return new TermTranslatePropertyLine(it, "")
        }

        String content = linesToWrite.collect { it.toString() }.join("\n")

        new File(PathUtil.join(outputDir, "../../conf", String.format(termFileFormat, version))).write(content)
        return linesToWrite
    }

    static generateTermsFromJpaEntities(String replaceTo) {
        List<TermTranslatePropertyLine> cloudTerms = generateTermsOfZStackVersion("cloud")
        List<TermTranslatePropertyLine> zsvTerms = generateTermsOfZStackVersion("zsv")

        if (replaceTo == null) {
            replaceTo = "cloud"
        }

        Map<String, TermTranslatePropertyLine> replaceMap = [:]
        if (replaceTo == "cloud") {
            cloudTerms.each {
                replaceMap.put(it.term, it)
            }

            zsvTerms.each {
                if (replaceMap.containsKey(it.term)) {
                    replaceMap.get(it.term).replaceTo = it.translation
                }
            }
        } else if (replaceTo == "zsv") {
            zsvTerms.each {
                replaceMap.put(it.term, it)
            }

            cloudTerms.each {
                if (replaceMap.containsKey(it.term)) {
                    replaceMap.get(it.term).replaceTo = it.translation
                }
            }
        }

        String outputDir = System.getProperty("user.dir")
        String content = replaceMap
                .values()
                .findAll { Strings.isNotBlank(it.translation) && Strings.isNotBlank(it.replaceTo) }
                .collect { it.toTranslationString() }
                .join("\n")
        new File(PathUtil.join(outputDir, '../../conf', String.format(translationFileFormat, replaceTo, replaceTo == "cloud" ? "zsv" : "cloud"))).write(content)
    }

    static List<TermTranslatePropertyLine> loadFileFromResource(String fileName) {
        if (!PathUtil.exists(fileName)) {
            return new ArrayList<TermTranslatePropertyLine>()
        }

        InputStream is = new FileInputStream(fileName)
        String content = new String(is.getBytes())
        is.close()

        return content.split("\n").collect {
            String[] tokens = it.split("=")

            String token1
            if (tokens.size() > 1)
                token1 = tokens[1].trim()
            else
                token1 = ""

            TermTranslatePropertyLine line = new TermTranslatePropertyLine(tokens[0].trim(), token1)
            return line
        }
    }

    static class TermTranslatePropertyLine {
        TermTranslatePropertyLine(String term, String translation) {
            this.term = term
            this.translation = translation
        }

        String term
        String translation
        String replaceTo

        String toString() {
            return String.format("%s=%s", term, translation)
        }

        String toTranslationString() {
            return String.format("%s=%s", translation, replaceTo)
        }
    }
}
