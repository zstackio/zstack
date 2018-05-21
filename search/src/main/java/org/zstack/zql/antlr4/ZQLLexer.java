// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql.antlr4;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ZQLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, FILTER_BY=19, OFFSET=20, LIMIT=21, QUERY=22, COUNT=23, ORDER_BY=24, 
		ORDER_BY_VALUE=25, RESTRICT_BY=26, RETURN_WITH=27, WHERE=28, AND=29, OR=30, 
		ASC=31, DESC=32, BOOLEAN=33, INT=34, FLOAT=35, ID=36, WS=37, STRING=38;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"T__17", "FILTER_BY", "OFFSET", "LIMIT", "QUERY", "COUNT", "ORDER_BY", 
		"ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", "OR", 
		"ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", "WS", "STRING", "CHAR", 
		"NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'.'", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", "'is null'", 
		"'not null'", "'in'", "'not in'", "'like'", "'not like'", "'('", "','", 
		"')'", "'{'", "'}'", "'filter by'", "'offset'", "'limit'", "'query'", 
		"'count'", "'order by'", null, "'restrict by'", "'return with'", "'where'", 
		"'and'", "'or'", "'asc'", "'desc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, "FILTER_BY", "OFFSET", "LIMIT", 
		"QUERY", "COUNT", "ORDER_BY", "ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", 
		"WHERE", "AND", "OR", "ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", 
		"WS", "STRING"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public ZQLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ZQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2(\u013b\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\3\2\3\2\3\3\3"+
		"\3\3\4\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\t"+
		"\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3"+
		"\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25"+
		"\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27"+
		"\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\32\3\32\5\32\u00c6\n\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\37"+
		"\3\37\3\37\3 \3 \3 \3 \3!\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\""+
		"\3\"\5\"\u00ff\n\"\3#\5#\u0102\n#\3#\3#\3$\3$\3$\6$\u0109\n$\r$\16$\u010a"+
		"\3%\6%\u010e\n%\r%\16%\u010f\3&\6&\u0113\n&\r&\16&\u0114\3&\3&\3\'\3\'"+
		"\7\'\u011b\n\'\f\'\16\'\u011e\13\'\3\'\3\'\3\'\7\'\u0123\n\'\f\'\16\'"+
		"\u0126\13\'\3\'\5\'\u0129\n\'\3(\6(\u012c\n(\r(\16(\u012d\3(\6(\u0131"+
		"\n(\r(\16(\u0132\5(\u0135\n(\3)\6)\u0138\n)\r)\16)\u0139\2\2*\3\3\5\4"+
		"\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22"+
		"#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C"+
		"#E$G%I&K\'M(O\2Q\2\3\2\6\6\2\62;C\\aac|\5\2\13\f\17\17\"\"\3\2$$\3\2)"+
		")\2\u0145\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2"+
		"\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27"+
		"\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2"+
		"\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2"+
		"\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2"+
		"\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2"+
		"\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\3S\3\2\2\2\5U\3\2\2\2\7W"+
		"\3\2\2\2\tZ\3\2\2\2\13\\\3\2\2\2\r_\3\2\2\2\17a\3\2\2\2\21d\3\2\2\2\23"+
		"l\3\2\2\2\25u\3\2\2\2\27x\3\2\2\2\31\177\3\2\2\2\33\u0084\3\2\2\2\35\u008d"+
		"\3\2\2\2\37\u008f\3\2\2\2!\u0091\3\2\2\2#\u0093\3\2\2\2%\u0095\3\2\2\2"+
		"\'\u0097\3\2\2\2)\u00a1\3\2\2\2+\u00a8\3\2\2\2-\u00ae\3\2\2\2/\u00b4\3"+
		"\2\2\2\61\u00ba\3\2\2\2\63\u00c5\3\2\2\2\65\u00c7\3\2\2\2\67\u00d3\3\2"+
		"\2\29\u00df\3\2\2\2;\u00e5\3\2\2\2=\u00e9\3\2\2\2?\u00ec\3\2\2\2A\u00f0"+
		"\3\2\2\2C\u00fe\3\2\2\2E\u0101\3\2\2\2G\u0105\3\2\2\2I\u010d\3\2\2\2K"+
		"\u0112\3\2\2\2M\u0128\3\2\2\2O\u0134\3\2\2\2Q\u0137\3\2\2\2ST\7\60\2\2"+
		"T\4\3\2\2\2UV\7?\2\2V\6\3\2\2\2WX\7#\2\2XY\7?\2\2Y\b\3\2\2\2Z[\7@\2\2"+
		"[\n\3\2\2\2\\]\7@\2\2]^\7?\2\2^\f\3\2\2\2_`\7>\2\2`\16\3\2\2\2ab\7>\2"+
		"\2bc\7?\2\2c\20\3\2\2\2de\7k\2\2ef\7u\2\2fg\7\"\2\2gh\7p\2\2hi\7w\2\2"+
		"ij\7n\2\2jk\7n\2\2k\22\3\2\2\2lm\7p\2\2mn\7q\2\2no\7v\2\2op\7\"\2\2pq"+
		"\7p\2\2qr\7w\2\2rs\7n\2\2st\7n\2\2t\24\3\2\2\2uv\7k\2\2vw\7p\2\2w\26\3"+
		"\2\2\2xy\7p\2\2yz\7q\2\2z{\7v\2\2{|\7\"\2\2|}\7k\2\2}~\7p\2\2~\30\3\2"+
		"\2\2\177\u0080\7n\2\2\u0080\u0081\7k\2\2\u0081\u0082\7m\2\2\u0082\u0083"+
		"\7g\2\2\u0083\32\3\2\2\2\u0084\u0085\7p\2\2\u0085\u0086\7q\2\2\u0086\u0087"+
		"\7v\2\2\u0087\u0088\7\"\2\2\u0088\u0089\7n\2\2\u0089\u008a\7k\2\2\u008a"+
		"\u008b\7m\2\2\u008b\u008c\7g\2\2\u008c\34\3\2\2\2\u008d\u008e\7*\2\2\u008e"+
		"\36\3\2\2\2\u008f\u0090\7.\2\2\u0090 \3\2\2\2\u0091\u0092\7+\2\2\u0092"+
		"\"\3\2\2\2\u0093\u0094\7}\2\2\u0094$\3\2\2\2\u0095\u0096\7\177\2\2\u0096"+
		"&\3\2\2\2\u0097\u0098\7h\2\2\u0098\u0099\7k\2\2\u0099\u009a\7n\2\2\u009a"+
		"\u009b\7v\2\2\u009b\u009c\7g\2\2\u009c\u009d\7t\2\2\u009d\u009e\7\"\2"+
		"\2\u009e\u009f\7d\2\2\u009f\u00a0\7{\2\2\u00a0(\3\2\2\2\u00a1\u00a2\7"+
		"q\2\2\u00a2\u00a3\7h\2\2\u00a3\u00a4\7h\2\2\u00a4\u00a5\7u\2\2\u00a5\u00a6"+
		"\7g\2\2\u00a6\u00a7\7v\2\2\u00a7*\3\2\2\2\u00a8\u00a9\7n\2\2\u00a9\u00aa"+
		"\7k\2\2\u00aa\u00ab\7o\2\2\u00ab\u00ac\7k\2\2\u00ac\u00ad\7v\2\2\u00ad"+
		",\3\2\2\2\u00ae\u00af\7s\2\2\u00af\u00b0\7w\2\2\u00b0\u00b1\7g\2\2\u00b1"+
		"\u00b2\7t\2\2\u00b2\u00b3\7{\2\2\u00b3.\3\2\2\2\u00b4\u00b5\7e\2\2\u00b5"+
		"\u00b6\7q\2\2\u00b6\u00b7\7w\2\2\u00b7\u00b8\7p\2\2\u00b8\u00b9\7v\2\2"+
		"\u00b9\60\3\2\2\2\u00ba\u00bb\7q\2\2\u00bb\u00bc\7t\2\2\u00bc\u00bd\7"+
		"f\2\2\u00bd\u00be\7g\2\2\u00be\u00bf\7t\2\2\u00bf\u00c0\7\"\2\2\u00c0"+
		"\u00c1\7d\2\2\u00c1\u00c2\7{\2\2\u00c2\62\3\2\2\2\u00c3\u00c6\5? \2\u00c4"+
		"\u00c6\5A!\2\u00c5\u00c3\3\2\2\2\u00c5\u00c4\3\2\2\2\u00c6\64\3\2\2\2"+
		"\u00c7\u00c8\7t\2\2\u00c8\u00c9\7g\2\2\u00c9\u00ca\7u\2\2\u00ca\u00cb"+
		"\7v\2\2\u00cb\u00cc\7t\2\2\u00cc\u00cd\7k\2\2\u00cd\u00ce\7e\2\2\u00ce"+
		"\u00cf\7v\2\2\u00cf\u00d0\7\"\2\2\u00d0\u00d1\7d\2\2\u00d1\u00d2\7{\2"+
		"\2\u00d2\66\3\2\2\2\u00d3\u00d4\7t\2\2\u00d4\u00d5\7g\2\2\u00d5\u00d6"+
		"\7v\2\2\u00d6\u00d7\7w\2\2\u00d7\u00d8\7t\2\2\u00d8\u00d9\7p\2\2\u00d9"+
		"\u00da\7\"\2\2\u00da\u00db\7y\2\2\u00db\u00dc\7k\2\2\u00dc\u00dd\7v\2"+
		"\2\u00dd\u00de\7j\2\2\u00de8\3\2\2\2\u00df\u00e0\7y\2\2\u00e0\u00e1\7"+
		"j\2\2\u00e1\u00e2\7g\2\2\u00e2\u00e3\7t\2\2\u00e3\u00e4\7g\2\2\u00e4:"+
		"\3\2\2\2\u00e5\u00e6\7c\2\2\u00e6\u00e7\7p\2\2\u00e7\u00e8\7f\2\2\u00e8"+
		"<\3\2\2\2\u00e9\u00ea\7q\2\2\u00ea\u00eb\7t\2\2\u00eb>\3\2\2\2\u00ec\u00ed"+
		"\7c\2\2\u00ed\u00ee\7u\2\2\u00ee\u00ef\7e\2\2\u00ef@\3\2\2\2\u00f0\u00f1"+
		"\7f\2\2\u00f1\u00f2\7g\2\2\u00f2\u00f3\7u\2\2\u00f3\u00f4\7e\2\2\u00f4"+
		"B\3\2\2\2\u00f5\u00f6\7v\2\2\u00f6\u00f7\7t\2\2\u00f7\u00f8\7w\2\2\u00f8"+
		"\u00ff\7g\2\2\u00f9\u00fa\7h\2\2\u00fa\u00fb\7c\2\2\u00fb\u00fc\7n\2\2"+
		"\u00fc\u00fd\7u\2\2\u00fd\u00ff\7g\2\2\u00fe\u00f5\3\2\2\2\u00fe\u00f9"+
		"\3\2\2\2\u00ffD\3\2\2\2\u0100\u0102\7/\2\2\u0101\u0100\3\2\2\2\u0101\u0102"+
		"\3\2\2\2\u0102\u0103\3\2\2\2\u0103\u0104\5Q)\2\u0104F\3\2\2\2\u0105\u0106"+
		"\5E#\2\u0106\u0108\7\60\2\2\u0107\u0109\5Q)\2\u0108\u0107\3\2\2\2\u0109"+
		"\u010a\3\2\2\2\u010a\u0108\3\2\2\2\u010a\u010b\3\2\2\2\u010bH\3\2\2\2"+
		"\u010c\u010e\t\2\2\2\u010d\u010c\3\2\2\2\u010e\u010f\3\2\2\2\u010f\u010d"+
		"\3\2\2\2\u010f\u0110\3\2\2\2\u0110J\3\2\2\2\u0111\u0113\t\3\2\2\u0112"+
		"\u0111\3\2\2\2\u0113\u0114\3\2\2\2\u0114\u0112\3\2\2\2\u0114\u0115\3\2"+
		"\2\2\u0115\u0116\3\2\2\2\u0116\u0117\b&\2\2\u0117L\3\2\2\2\u0118\u011c"+
		"\7$\2\2\u0119\u011b\n\4\2\2\u011a\u0119\3\2\2\2\u011b\u011e\3\2\2\2\u011c"+
		"\u011a\3\2\2\2\u011c\u011d\3\2\2\2\u011d\u011f\3\2\2\2\u011e\u011c\3\2"+
		"\2\2\u011f\u0129\7$\2\2\u0120\u0124\7)\2\2\u0121\u0123\n\5\2\2\u0122\u0121"+
		"\3\2\2\2\u0123\u0126\3\2\2\2\u0124\u0122\3\2\2\2\u0124\u0125\3\2\2\2\u0125"+
		"\u0127\3\2\2\2\u0126\u0124\3\2\2\2\u0127\u0129\7)\2\2\u0128\u0118\3\2"+
		"\2\2\u0128\u0120\3\2\2\2\u0129N\3\2\2\2\u012a\u012c\4c|\2\u012b\u012a"+
		"\3\2\2\2\u012c\u012d\3\2\2\2\u012d\u012b\3\2\2\2\u012d\u012e\3\2\2\2\u012e"+
		"\u0135\3\2\2\2\u012f\u0131\4C\\\2\u0130\u012f\3\2\2\2\u0131\u0132\3\2"+
		"\2\2\u0132\u0130\3\2\2\2\u0132\u0133\3\2\2\2\u0133\u0135\3\2\2\2\u0134"+
		"\u012b\3\2\2\2\u0134\u0130\3\2\2\2\u0135P\3\2\2\2\u0136\u0138\4\62;\2"+
		"\u0137\u0136\3\2\2\2\u0138\u0139\3\2\2\2\u0139\u0137\3\2\2\2\u0139\u013a"+
		"\3\2\2\2\u013aR\3\2\2\2\20\2\u00c5\u00fe\u0101\u010a\u010f\u0114\u011c"+
		"\u0124\u0128\u012d\u0132\u0134\u0139\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}