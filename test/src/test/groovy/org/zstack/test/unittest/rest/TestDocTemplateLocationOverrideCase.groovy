package org.zstack.test.unittest.rest

import org.junit.Test
import org.objenesis.ObjenesisStd
import org.zstack.header.volume.APIDeleteDataVolumeMsg

class TestDocTemplateLocationOverrideCase {
    @Test
    void locationOverrideSurvivesTwoRepairsWithAndWithoutValues() {
        File root = new File(System.getProperty('zstack.source.root', '..')).canonicalFile
        File script = new File(root, 'rest/src/main/resources/scripts/RestDocumentationGenerator.groovy')
        Class generatorClass = new GroovyClassLoader(getClass().classLoader).parseClass(script)
        // Global configuration discovery is unrelated to parsing, merging and serializing a request doc.
        def generator = new ObjenesisStd().newInstance(generatorClass)
        generator.MUTUAL_FIELDS = [:]
        generator.LOCATION_URL = 'url'
        generator.LOCATION_BODY = 'body'
        generator.LOCATION_QUERY = 'query'
        generator.sourceFiles = [APIDeleteDataVolumeMsg: new File(root,
                'header/src/main/java/org/zstack/header/volume/APIDeleteDataVolumeMsg.java')]
        Class templateClass = generatorClass.classLoader.loadClass('scripts.RestDocumentationGenerator$ApiRequestDocTemplate')
        def template = templateClass.getDeclaredConstructor(generatorClass, Class).newInstance(generator, APIDeleteDataVolumeMsg)
        String generated = template.generate()
        assert generated.contains('name "deleteMode"')

        [false, true].each { boolean values ->
            String canonicalDefault = template.generate(generator.createDocFromString(
                    replaceDeleteMode(generated, null, values).replace('location "query"', 'location "body"')))
            [null, false, true].each { Boolean override ->
                String original = replaceDeleteMode(generated, override, values)
                def originalDoc = generator.createDocFromString(original)
                originalDoc.merge(generator.createDocFromString(generated))
                String once = template.generate(originalDoc)
                String expected = override == true ? 'query' : 'body'
                assert deleteMode(once).contains('location "' + expected + '"' +
                        (override == null ? '' : ', ' + override))
                def reparsed = generator.createDocFromString(once)
                reparsed.merge(generator.createDocFromString(generated))
                String twice = template.generate(reparsed)
                assert twice == once
                if (override == null) {
                    assert once == canonicalDefault
                }
            }
        }
    }

    private static String replaceDeleteMode(String text, Boolean override, boolean values) {
        text.replaceFirst(/(?s)(name "deleteMode".*?location )"body"/, '$1"query"' +
                (override == null ? '' : ', ' + override)).replaceFirst(
                /(?s)(name "deleteMode".*?since "[^"]+")/,
                '$1' + (values ? '\n\t\t\t\t\tvalues ("Permissive","Enforcing")' : ''))
    }

    private static String deleteMode(String text) {
        (text =~ /(?s)name "deleteMode".*?\n\s*}/)[0]
    }
}
