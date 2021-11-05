package org.zstack.utils;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(name = "Masksensitivepatternlayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class Masksensitivepatternlayout extends AbstractStringLayout {
    private static final long serialVersionUID = 1L;


    public static final String DEFAULT_CONVERSION_PATTERN = "%m%n";

    public static final String TTCC_CONVERSION_PATTERN = "%r [%t] %p %c %x - %m%n";

    public static final String SIMPLE_CONVERSION_PATTERN = "%d [%t] %p %c - %m%n";

    public static final String KEY = "Converter";

    private final List<PatternFormatter> formatters;

    private final String conversionPattern;

    private final Configuration config;

    private final MaskSensitiveRegexReplaces replace;

    private final boolean alwaysWriteExceptions;

    private final boolean noConsoleNoAnsi;


    private Masksensitivepatternlayout(final Configuration config, final MaskSensitiveRegexReplaces replace, final String pattern,
                                       final Charset charset, final boolean alwaysWriteExceptions, final boolean noConsoleNoAnsi,
                                       final String header, final String footer) {
        super(charset, toBytes(header, charset), toBytes(footer, charset));
        this.replace = replace;
        this.conversionPattern = pattern;
        this.config = config;
        this.alwaysWriteExceptions = alwaysWriteExceptions;
        this.noConsoleNoAnsi = noConsoleNoAnsi;
        final PatternParser parser = createPatternParser(config);
        this.formatters = parser.parse(pattern == null ? DEFAULT_CONVERSION_PATTERN : pattern,
                this.alwaysWriteExceptions, this.noConsoleNoAnsi);
    }

    private static byte[] toBytes(final String str, final Charset charset) {
        if (str != null) {
            return str.getBytes(charset != null ? charset : Charset.defaultCharset());
        }
        return null;
    }

    private byte[] strSubstitutorReplace(final byte... b) {
        if (b != null && config != null) {
            return getBytes(config.getStrSubstitutor().replace(new String(b, getCharset())));
        }
        return b;
    }

    @Override
    public byte[] getHeader() {
        return strSubstitutorReplace(super.getHeader());
    }

    @Override
    public byte[] getFooter() {
        return strSubstitutorReplace(super.getFooter());
    }

    public String getConversionPattern() {
        return conversionPattern;
    }


    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("structured", "false");
        result.put("formatType", "conversion");
        result.put("format", conversionPattern);
        return result;
    }


    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        String str = buf.toString();
        if (replace != null) {
            str = replace.format(str);
        }
        return str;
    }


    public static PatternParser createPatternParser(final Configuration config) {
        if (config == null) {
            return new PatternParser(config, KEY, LogEventPatternConverter.class);
        }
        PatternParser parser = config.getComponent(KEY);
        if (parser == null) {
            parser = new PatternParser(config, KEY, LogEventPatternConverter.class);
            config.addComponent(KEY, parser);
            parser = (PatternParser) config.getComponent(KEY);
        }
        return parser;
    }

    @Override
    public String toString() {
        return conversionPattern;
    }


    @PluginFactory
    public static Masksensitivepatternlayout createLayout(
            @PluginAttribute(value = "pattern", defaultString = DEFAULT_CONVERSION_PATTERN) final String pattern,
            @PluginConfiguration final Configuration config,
            @PluginElement("Replaces") final MaskSensitiveRegexReplaces replace,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
            @PluginAttribute(value = "alwaysWriteExceptions", defaultBoolean = true) final boolean alwaysWriteExceptions,
            @PluginAttribute(value = "noConsoleNoAnsi", defaultBoolean = false) final boolean noConsoleNoAnsi,
            @PluginAttribute("header") final String header, @PluginAttribute("footer") final String footer) {
        System.out.println("==============");
        return newBuilder().withPattern(pattern).withConfiguration(config).withRegexReplacement(replace)
                .withCharset(charset).withAlwaysWriteExceptions(alwaysWriteExceptions)
                .withNoConsoleNoAnsi(noConsoleNoAnsi).withHeader(header).withFooter(footer).build();
    }

    public static Masksensitivepatternlayout createDefaultLayout() {
        System.out.println("11111111");
        return newBuilder().build();
    }


    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder implements org.apache.logging.log4j.core.util.Builder<Masksensitivepatternlayout> {

        // FIXME: it seems rather redundant to repeat default values (same goes
        // for field names)
        // perhaps introduce a @PluginBuilderAttribute that has no values of its
        // own and uses reflection?

        @PluginBuilderAttribute
        private String pattern = Masksensitivepatternlayout.DEFAULT_CONVERSION_PATTERN;

        @PluginConfiguration
        private Configuration configuration = null;

        @PluginElement("Replaces")
        private MaskSensitiveRegexReplaces regexReplacement = null;

        // LOG4J2-783 use platform default by default
        @PluginBuilderAttribute
        private Charset charset = Charset.defaultCharset();

        @PluginBuilderAttribute
        private boolean alwaysWriteExceptions = true;

        @PluginBuilderAttribute
        private boolean noConsoleNoAnsi = false;

        @PluginBuilderAttribute
        private String header = null;

        @PluginBuilderAttribute
        private String footer = null;

        private Builder() {
        }

        // TODO: move javadocs from PluginFactory to here

        public Builder withPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder withConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withRegexReplacement(final MaskSensitiveRegexReplaces regexReplacement) {
            this.regexReplacement = regexReplacement;
            return this;
        }

        public Builder withCharset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder withAlwaysWriteExceptions(final boolean alwaysWriteExceptions) {
            this.alwaysWriteExceptions = alwaysWriteExceptions;
            return this;
        }

        public Builder withNoConsoleNoAnsi(final boolean noConsoleNoAnsi) {
            this.noConsoleNoAnsi = noConsoleNoAnsi;
            return this;
        }

        public Builder withHeader(final String header) {
            this.header = header;
            return this;
        }

        public Builder withFooter(final String footer) {
            this.footer = footer;
            return this;
        }

        @Override
        public Masksensitivepatternlayout build() {
            // fall back to DefaultConfiguration
            if (configuration == null) {
                configuration = new DefaultConfiguration();
            }
            return new Masksensitivepatternlayout(configuration, regexReplacement, pattern, charset, alwaysWriteExceptions,
                    noConsoleNoAnsi, header, footer);
        }
    }
}
