// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql1.antlr4;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;

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
		ASC=31, DESC=32, INT=33, FLOAT=34, ID=35, WS=36, STRING=37;
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
		"ASC", "DESC", "INT", "FLOAT", "ID", "WS", "STRING", "CHAR", "NUMBER"
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
		"WHERE", "AND", "OR", "ASC", "DESC", "INT", "FLOAT", "ID", "WS", "STRING"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\'\u0130\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\3\2\3\2\3\3\3\3\3\4"+
		"\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f"+
		"\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25"+
		"\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27\3\27"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\32\3\32\5\32\u00c4\n\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\37\3\37"+
		"\3\37\3 \3 \3 \3 \3!\3!\3!\3!\3!\3\"\5\"\u00f5\n\"\3\"\3\"\3#\3#\3#\6"+
		"#\u00fc\n#\r#\16#\u00fd\3$\6$\u0101\n$\r$\16$\u0102\3%\6%\u0106\n%\r%"+
		"\16%\u0107\3%\3%\3&\3&\6&\u010e\n&\r&\16&\u010f\3&\3&\6&\u0114\n&\r&\16"+
		"&\u0115\3&\6&\u0119\n&\r&\16&\u011a\3&\5&\u011e\n&\3\'\6\'\u0121\n\'\r"+
		"\'\16\'\u0122\3\'\6\'\u0126\n\'\r\'\16\'\u0127\5\'\u012a\n\'\3(\6(\u012d"+
		"\n(\r(\16(\u012e\2\2)\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27"+
		"\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33"+
		"\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M\2O\2\3\2\6\6\2\62;C\\aac|\5\2"+
		"\13\f\17\17\"\"\3\2$$\3\2))\2\u013a\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2"+
		"\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23"+
		"\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2"+
		"\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2"+
		"\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3"+
		"\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2"+
		"\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\3Q\3\2\2\2"+
		"\5S\3\2\2\2\7U\3\2\2\2\tX\3\2\2\2\13Z\3\2\2\2\r]\3\2\2\2\17_\3\2\2\2\21"+
		"b\3\2\2\2\23j\3\2\2\2\25s\3\2\2\2\27v\3\2\2\2\31}\3\2\2\2\33\u0082\3\2"+
		"\2\2\35\u008b\3\2\2\2\37\u008d\3\2\2\2!\u008f\3\2\2\2#\u0091\3\2\2\2%"+
		"\u0093\3\2\2\2\'\u0095\3\2\2\2)\u009f\3\2\2\2+\u00a6\3\2\2\2-\u00ac\3"+
		"\2\2\2/\u00b2\3\2\2\2\61\u00b8\3\2\2\2\63\u00c3\3\2\2\2\65\u00c5\3\2\2"+
		"\2\67\u00d1\3\2\2\29\u00dd\3\2\2\2;\u00e3\3\2\2\2=\u00e7\3\2\2\2?\u00ea"+
		"\3\2\2\2A\u00ee\3\2\2\2C\u00f4\3\2\2\2E\u00f8\3\2\2\2G\u0100\3\2\2\2I"+
		"\u0105\3\2\2\2K\u011d\3\2\2\2M\u0129\3\2\2\2O\u012c\3\2\2\2QR\7\60\2\2"+
		"R\4\3\2\2\2ST\7?\2\2T\6\3\2\2\2UV\7#\2\2VW\7?\2\2W\b\3\2\2\2XY\7@\2\2"+
		"Y\n\3\2\2\2Z[\7@\2\2[\\\7?\2\2\\\f\3\2\2\2]^\7>\2\2^\16\3\2\2\2_`\7>\2"+
		"\2`a\7?\2\2a\20\3\2\2\2bc\7k\2\2cd\7u\2\2de\7\"\2\2ef\7p\2\2fg\7w\2\2"+
		"gh\7n\2\2hi\7n\2\2i\22\3\2\2\2jk\7p\2\2kl\7q\2\2lm\7v\2\2mn\7\"\2\2no"+
		"\7p\2\2op\7w\2\2pq\7n\2\2qr\7n\2\2r\24\3\2\2\2st\7k\2\2tu\7p\2\2u\26\3"+
		"\2\2\2vw\7p\2\2wx\7q\2\2xy\7v\2\2yz\7\"\2\2z{\7k\2\2{|\7p\2\2|\30\3\2"+
		"\2\2}~\7n\2\2~\177\7k\2\2\177\u0080\7m\2\2\u0080\u0081\7g\2\2\u0081\32"+
		"\3\2\2\2\u0082\u0083\7p\2\2\u0083\u0084\7q\2\2\u0084\u0085\7v\2\2\u0085"+
		"\u0086\7\"\2\2\u0086\u0087\7n\2\2\u0087\u0088\7k\2\2\u0088\u0089\7m\2"+
		"\2\u0089\u008a\7g\2\2\u008a\34\3\2\2\2\u008b\u008c\7*\2\2\u008c\36\3\2"+
		"\2\2\u008d\u008e\7.\2\2\u008e \3\2\2\2\u008f\u0090\7+\2\2\u0090\"\3\2"+
		"\2\2\u0091\u0092\7}\2\2\u0092$\3\2\2\2\u0093\u0094\7\177\2\2\u0094&\3"+
		"\2\2\2\u0095\u0096\7h\2\2\u0096\u0097\7k\2\2\u0097\u0098\7n\2\2\u0098"+
		"\u0099\7v\2\2\u0099\u009a\7g\2\2\u009a\u009b\7t\2\2\u009b\u009c\7\"\2"+
		"\2\u009c\u009d\7d\2\2\u009d\u009e\7{\2\2\u009e(\3\2\2\2\u009f\u00a0\7"+
		"q\2\2\u00a0\u00a1\7h\2\2\u00a1\u00a2\7h\2\2\u00a2\u00a3\7u\2\2\u00a3\u00a4"+
		"\7g\2\2\u00a4\u00a5\7v\2\2\u00a5*\3\2\2\2\u00a6\u00a7\7n\2\2\u00a7\u00a8"+
		"\7k\2\2\u00a8\u00a9\7o\2\2\u00a9\u00aa\7k\2\2\u00aa\u00ab\7v\2\2\u00ab"+
		",\3\2\2\2\u00ac\u00ad\7s\2\2\u00ad\u00ae\7w\2\2\u00ae\u00af\7g\2\2\u00af"+
		"\u00b0\7t\2\2\u00b0\u00b1\7{\2\2\u00b1.\3\2\2\2\u00b2\u00b3\7e\2\2\u00b3"+
		"\u00b4\7q\2\2\u00b4\u00b5\7w\2\2\u00b5\u00b6\7p\2\2\u00b6\u00b7\7v\2\2"+
		"\u00b7\60\3\2\2\2\u00b8\u00b9\7q\2\2\u00b9\u00ba\7t\2\2\u00ba\u00bb\7"+
		"f\2\2\u00bb\u00bc\7g\2\2\u00bc\u00bd\7t\2\2\u00bd\u00be\7\"\2\2\u00be"+
		"\u00bf\7d\2\2\u00bf\u00c0\7{\2\2\u00c0\62\3\2\2\2\u00c1\u00c4\5? \2\u00c2"+
		"\u00c4\5A!\2\u00c3\u00c1\3\2\2\2\u00c3\u00c2\3\2\2\2\u00c4\64\3\2\2\2"+
		"\u00c5\u00c6\7t\2\2\u00c6\u00c7\7g\2\2\u00c7\u00c8\7u\2\2\u00c8\u00c9"+
		"\7v\2\2\u00c9\u00ca\7t\2\2\u00ca\u00cb\7k\2\2\u00cb\u00cc\7e\2\2\u00cc"+
		"\u00cd\7v\2\2\u00cd\u00ce\7\"\2\2\u00ce\u00cf\7d\2\2\u00cf\u00d0\7{\2"+
		"\2\u00d0\66\3\2\2\2\u00d1\u00d2\7t\2\2\u00d2\u00d3\7g\2\2\u00d3\u00d4"+
		"\7v\2\2\u00d4\u00d5\7w\2\2\u00d5\u00d6\7t\2\2\u00d6\u00d7\7p\2\2\u00d7"+
		"\u00d8\7\"\2\2\u00d8\u00d9\7y\2\2\u00d9\u00da\7k\2\2\u00da\u00db\7v\2"+
		"\2\u00db\u00dc\7j\2\2\u00dc8\3\2\2\2\u00dd\u00de\7y\2\2\u00de\u00df\7"+
		"j\2\2\u00df\u00e0\7g\2\2\u00e0\u00e1\7t\2\2\u00e1\u00e2\7g\2\2\u00e2:"+
		"\3\2\2\2\u00e3\u00e4\7c\2\2\u00e4\u00e5\7p\2\2\u00e5\u00e6\7f\2\2\u00e6"+
		"<\3\2\2\2\u00e7\u00e8\7q\2\2\u00e8\u00e9\7t\2\2\u00e9>\3\2\2\2\u00ea\u00eb"+
		"\7c\2\2\u00eb\u00ec\7u\2\2\u00ec\u00ed\7e\2\2\u00ed@\3\2\2\2\u00ee\u00ef"+
		"\7f\2\2\u00ef\u00f0\7g\2\2\u00f0\u00f1\7u\2\2\u00f1\u00f2\7e\2\2\u00f2"+
		"B\3\2\2\2\u00f3\u00f5\7/\2\2\u00f4\u00f3\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5"+
		"\u00f6\3\2\2\2\u00f6\u00f7\5O(\2\u00f7D\3\2\2\2\u00f8\u00f9\5C\"\2\u00f9"+
		"\u00fb\7\60\2\2\u00fa\u00fc\5O(\2\u00fb\u00fa\3\2\2\2\u00fc\u00fd\3\2"+
		"\2\2\u00fd\u00fb\3\2\2\2\u00fd\u00fe\3\2\2\2\u00feF\3\2\2\2\u00ff\u0101"+
		"\t\2\2\2\u0100\u00ff\3\2\2\2\u0101\u0102\3\2\2\2\u0102\u0100\3\2\2\2\u0102"+
		"\u0103\3\2\2\2\u0103H\3\2\2\2\u0104\u0106\t\3\2\2\u0105\u0104\3\2\2\2"+
		"\u0106\u0107\3\2\2\2\u0107\u0105\3\2\2\2\u0107\u0108\3\2\2\2\u0108\u0109"+
		"\3\2\2\2\u0109\u010a\b%\2\2\u010aJ\3\2\2\2\u010b\u010d\7$\2\2\u010c\u010e"+
		"\n\4\2\2\u010d\u010c\3\2\2\2\u010e\u010f\3\2\2\2\u010f\u010d\3\2\2\2\u010f"+
		"\u0110\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u011e\7$\2\2\u0112\u0114\7)\2"+
		"\2\u0113\u0112\3\2\2\2\u0114\u0115\3\2\2\2\u0115\u0113\3\2\2\2\u0115\u0116"+
		"\3\2\2\2\u0116\u0118\3\2\2\2\u0117\u0119\n\5\2\2\u0118\u0117\3\2\2\2\u0119"+
		"\u011a\3\2\2\2\u011a\u0118\3\2\2\2\u011a\u011b\3\2\2\2\u011b\u011c\3\2"+
		"\2\2\u011c\u011e\7)\2\2\u011d\u010b\3\2\2\2\u011d\u0113\3\2\2\2\u011e"+
		"L\3\2\2\2\u011f\u0121\4c|\2\u0120\u011f\3\2\2\2\u0121\u0122\3\2\2\2\u0122"+
		"\u0120\3\2\2\2\u0122\u0123\3\2\2\2\u0123\u012a\3\2\2\2\u0124\u0126\4C"+
		"\\\2\u0125\u0124\3\2\2\2\u0126\u0127\3\2\2\2\u0127\u0125\3\2\2\2\u0127"+
		"\u0128\3\2\2\2\u0128\u012a\3\2\2\2\u0129\u0120\3\2\2\2\u0129\u0125\3\2"+
		"\2\2\u012aN\3\2\2\2\u012b\u012d\4\62;\2\u012c\u012b\3\2\2\2\u012d\u012e"+
		"\3\2\2\2\u012e\u012c\3\2\2\2\u012e\u012f\3\2\2\2\u012fP\3\2\2\2\20\2\u00c3"+
		"\u00f4\u00fd\u0102\u0107\u010f\u0115\u011a\u011d\u0122\u0127\u0129\u012e"+
		"\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}