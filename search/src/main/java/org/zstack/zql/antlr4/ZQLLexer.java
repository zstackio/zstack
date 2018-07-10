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
		T__17=18, T__18=19, T__19=20, FILTER_BY=21, OFFSET=22, LIMIT=23, QUERY=24, 
		COUNT=25, SUM=26, ORDER_BY=27, NAMED_AS=28, ORDER_BY_VALUE=29, RESTRICT_BY=30, 
		RETURN_WITH=31, WHERE=32, AND=33, OR=34, ASC=35, DESC=36, BOOLEAN=37, 
		INT=38, FLOAT=39, ID=40, WS=41, STRING=42;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"T__17", "T__18", "T__19", "FILTER_BY", "OFFSET", "LIMIT", "QUERY", "COUNT", 
		"SUM", "ORDER_BY", "NAMED_AS", "ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", 
		"WHERE", "AND", "OR", "ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", 
		"WS", "STRING", "CHAR", "NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", 
		"'is null'", "'is not null'", "'in'", "'not in'", "'like'", "'not like'", 
		"'('", "')'", "'{'", "'}'", "'by'", "'filter by'", "'offset'", "'limit'", 
		"'query'", "'count'", "'sum'", "'order by'", "'named as'", null, "'restrict by'", 
		"'return with'", "'where'", "'and'", "'or'", "'asc'", "'desc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, "FILTER_BY", "OFFSET", 
		"LIMIT", "QUERY", "COUNT", "SUM", "ORDER_BY", "NAMED_AS", "ORDER_BY_VALUE", 
		"RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", "OR", "ASC", "DESC", "BOOLEAN", 
		"INT", "FLOAT", "ID", "WS", "STRING"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2,\u0158\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\b\3\b"+
		"\3\b\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3"+
		"\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25"+
		"\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27"+
		"\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\34\3\34"+
		"\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35"+
		"\3\35\3\35\3\36\3\36\5\36\u00e3\n\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3!\3!\3"+
		"!\3!\3!\3!\3\"\3\"\3\"\3\"\3#\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3%\3&\3&\3"+
		"&\3&\3&\3&\3&\3&\3&\5&\u011c\n&\3\'\5\'\u011f\n\'\3\'\3\'\3(\3(\3(\6("+
		"\u0126\n(\r(\16(\u0127\3)\6)\u012b\n)\r)\16)\u012c\3*\6*\u0130\n*\r*\16"+
		"*\u0131\3*\3*\3+\3+\7+\u0138\n+\f+\16+\u013b\13+\3+\3+\3+\7+\u0140\n+"+
		"\f+\16+\u0143\13+\3+\5+\u0146\n+\3,\6,\u0149\n,\r,\16,\u014a\3,\6,\u014e"+
		"\n,\r,\16,\u014f\5,\u0152\n,\3-\6-\u0155\n-\r-\16-\u0156\2\2.\3\3\5\4"+
		"\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22"+
		"#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C"+
		"#E$G%I&K\'M(O)Q*S+U,W\2Y\2\3\2\6\6\2\62;C\\aac|\5\2\13\f\17\17\"\"\3\2"+
		"$$\3\2))\2\u0162\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13"+
		"\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2"+
		"\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2"+
		"!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3"+
		"\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2"+
		"\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E"+
		"\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2"+
		"\2\2\2S\3\2\2\2\2U\3\2\2\2\3[\3\2\2\2\5]\3\2\2\2\7_\3\2\2\2\ta\3\2\2\2"+
		"\13c\3\2\2\2\rf\3\2\2\2\17h\3\2\2\2\21k\3\2\2\2\23m\3\2\2\2\25p\3\2\2"+
		"\2\27x\3\2\2\2\31\u0084\3\2\2\2\33\u0087\3\2\2\2\35\u008e\3\2\2\2\37\u0093"+
		"\3\2\2\2!\u009c\3\2\2\2#\u009e\3\2\2\2%\u00a0\3\2\2\2\'\u00a2\3\2\2\2"+
		")\u00a4\3\2\2\2+\u00a7\3\2\2\2-\u00b1\3\2\2\2/\u00b8\3\2\2\2\61\u00be"+
		"\3\2\2\2\63\u00c4\3\2\2\2\65\u00ca\3\2\2\2\67\u00ce\3\2\2\29\u00d7\3\2"+
		"\2\2;\u00e2\3\2\2\2=\u00e4\3\2\2\2?\u00f0\3\2\2\2A\u00fc\3\2\2\2C\u0102"+
		"\3\2\2\2E\u0106\3\2\2\2G\u0109\3\2\2\2I\u010d\3\2\2\2K\u011b\3\2\2\2M"+
		"\u011e\3\2\2\2O\u0122\3\2\2\2Q\u012a\3\2\2\2S\u012f\3\2\2\2U\u0145\3\2"+
		"\2\2W\u0151\3\2\2\2Y\u0154\3\2\2\2[\\\7=\2\2\\\4\3\2\2\2]^\7\60\2\2^\6"+
		"\3\2\2\2_`\7.\2\2`\b\3\2\2\2ab\7?\2\2b\n\3\2\2\2cd\7#\2\2de\7?\2\2e\f"+
		"\3\2\2\2fg\7@\2\2g\16\3\2\2\2hi\7@\2\2ij\7?\2\2j\20\3\2\2\2kl\7>\2\2l"+
		"\22\3\2\2\2mn\7>\2\2no\7?\2\2o\24\3\2\2\2pq\7k\2\2qr\7u\2\2rs\7\"\2\2"+
		"st\7p\2\2tu\7w\2\2uv\7n\2\2vw\7n\2\2w\26\3\2\2\2xy\7k\2\2yz\7u\2\2z{\7"+
		"\"\2\2{|\7p\2\2|}\7q\2\2}~\7v\2\2~\177\7\"\2\2\177\u0080\7p\2\2\u0080"+
		"\u0081\7w\2\2\u0081\u0082\7n\2\2\u0082\u0083\7n\2\2\u0083\30\3\2\2\2\u0084"+
		"\u0085\7k\2\2\u0085\u0086\7p\2\2\u0086\32\3\2\2\2\u0087\u0088\7p\2\2\u0088"+
		"\u0089\7q\2\2\u0089\u008a\7v\2\2\u008a\u008b\7\"\2\2\u008b\u008c\7k\2"+
		"\2\u008c\u008d\7p\2\2\u008d\34\3\2\2\2\u008e\u008f\7n\2\2\u008f\u0090"+
		"\7k\2\2\u0090\u0091\7m\2\2\u0091\u0092\7g\2\2\u0092\36\3\2\2\2\u0093\u0094"+
		"\7p\2\2\u0094\u0095\7q\2\2\u0095\u0096\7v\2\2\u0096\u0097\7\"\2\2\u0097"+
		"\u0098\7n\2\2\u0098\u0099\7k\2\2\u0099\u009a\7m\2\2\u009a\u009b\7g\2\2"+
		"\u009b \3\2\2\2\u009c\u009d\7*\2\2\u009d\"\3\2\2\2\u009e\u009f\7+\2\2"+
		"\u009f$\3\2\2\2\u00a0\u00a1\7}\2\2\u00a1&\3\2\2\2\u00a2\u00a3\7\177\2"+
		"\2\u00a3(\3\2\2\2\u00a4\u00a5\7d\2\2\u00a5\u00a6\7{\2\2\u00a6*\3\2\2\2"+
		"\u00a7\u00a8\7h\2\2\u00a8\u00a9\7k\2\2\u00a9\u00aa\7n\2\2\u00aa\u00ab"+
		"\7v\2\2\u00ab\u00ac\7g\2\2\u00ac\u00ad\7t\2\2\u00ad\u00ae\7\"\2\2\u00ae"+
		"\u00af\7d\2\2\u00af\u00b0\7{\2\2\u00b0,\3\2\2\2\u00b1\u00b2\7q\2\2\u00b2"+
		"\u00b3\7h\2\2\u00b3\u00b4\7h\2\2\u00b4\u00b5\7u\2\2\u00b5\u00b6\7g\2\2"+
		"\u00b6\u00b7\7v\2\2\u00b7.\3\2\2\2\u00b8\u00b9\7n\2\2\u00b9\u00ba\7k\2"+
		"\2\u00ba\u00bb\7o\2\2\u00bb\u00bc\7k\2\2\u00bc\u00bd\7v\2\2\u00bd\60\3"+
		"\2\2\2\u00be\u00bf\7s\2\2\u00bf\u00c0\7w\2\2\u00c0\u00c1\7g\2\2\u00c1"+
		"\u00c2\7t\2\2\u00c2\u00c3\7{\2\2\u00c3\62\3\2\2\2\u00c4\u00c5\7e\2\2\u00c5"+
		"\u00c6\7q\2\2\u00c6\u00c7\7w\2\2\u00c7\u00c8\7p\2\2\u00c8\u00c9\7v\2\2"+
		"\u00c9\64\3\2\2\2\u00ca\u00cb\7u\2\2\u00cb\u00cc\7w\2\2\u00cc\u00cd\7"+
		"o\2\2\u00cd\66\3\2\2\2\u00ce\u00cf\7q\2\2\u00cf\u00d0\7t\2\2\u00d0\u00d1"+
		"\7f\2\2\u00d1\u00d2\7g\2\2\u00d2\u00d3\7t\2\2\u00d3\u00d4\7\"\2\2\u00d4"+
		"\u00d5\7d\2\2\u00d5\u00d6\7{\2\2\u00d68\3\2\2\2\u00d7\u00d8\7p\2\2\u00d8"+
		"\u00d9\7c\2\2\u00d9\u00da\7o\2\2\u00da\u00db\7g\2\2\u00db\u00dc\7f\2\2"+
		"\u00dc\u00dd\7\"\2\2\u00dd\u00de\7c\2\2\u00de\u00df\7u\2\2\u00df:\3\2"+
		"\2\2\u00e0\u00e3\5G$\2\u00e1\u00e3\5I%\2\u00e2\u00e0\3\2\2\2\u00e2\u00e1"+
		"\3\2\2\2\u00e3<\3\2\2\2\u00e4\u00e5\7t\2\2\u00e5\u00e6\7g\2\2\u00e6\u00e7"+
		"\7u\2\2\u00e7\u00e8\7v\2\2\u00e8\u00e9\7t\2\2\u00e9\u00ea\7k\2\2\u00ea"+
		"\u00eb\7e\2\2\u00eb\u00ec\7v\2\2\u00ec\u00ed\7\"\2\2\u00ed\u00ee\7d\2"+
		"\2\u00ee\u00ef\7{\2\2\u00ef>\3\2\2\2\u00f0\u00f1\7t\2\2\u00f1\u00f2\7"+
		"g\2\2\u00f2\u00f3\7v\2\2\u00f3\u00f4\7w\2\2\u00f4\u00f5\7t\2\2\u00f5\u00f6"+
		"\7p\2\2\u00f6\u00f7\7\"\2\2\u00f7\u00f8\7y\2\2\u00f8\u00f9\7k\2\2\u00f9"+
		"\u00fa\7v\2\2\u00fa\u00fb\7j\2\2\u00fb@\3\2\2\2\u00fc\u00fd\7y\2\2\u00fd"+
		"\u00fe\7j\2\2\u00fe\u00ff\7g\2\2\u00ff\u0100\7t\2\2\u0100\u0101\7g\2\2"+
		"\u0101B\3\2\2\2\u0102\u0103\7c\2\2\u0103\u0104\7p\2\2\u0104\u0105\7f\2"+
		"\2\u0105D\3\2\2\2\u0106\u0107\7q\2\2\u0107\u0108\7t\2\2\u0108F\3\2\2\2"+
		"\u0109\u010a\7c\2\2\u010a\u010b\7u\2\2\u010b\u010c\7e\2\2\u010cH\3\2\2"+
		"\2\u010d\u010e\7f\2\2\u010e\u010f\7g\2\2\u010f\u0110\7u\2\2\u0110\u0111"+
		"\7e\2\2\u0111J\3\2\2\2\u0112\u0113\7v\2\2\u0113\u0114\7t\2\2\u0114\u0115"+
		"\7w\2\2\u0115\u011c\7g\2\2\u0116\u0117\7h\2\2\u0117\u0118\7c\2\2\u0118"+
		"\u0119\7n\2\2\u0119\u011a\7u\2\2\u011a\u011c\7g\2\2\u011b\u0112\3\2\2"+
		"\2\u011b\u0116\3\2\2\2\u011cL\3\2\2\2\u011d\u011f\7/\2\2\u011e\u011d\3"+
		"\2\2\2\u011e\u011f\3\2\2\2\u011f\u0120\3\2\2\2\u0120\u0121\5Y-\2\u0121"+
		"N\3\2\2\2\u0122\u0123\5M\'\2\u0123\u0125\7\60\2\2\u0124\u0126\5Y-\2\u0125"+
		"\u0124\3\2\2\2\u0126\u0127\3\2\2\2\u0127\u0125\3\2\2\2\u0127\u0128\3\2"+
		"\2\2\u0128P\3\2\2\2\u0129\u012b\t\2\2\2\u012a\u0129\3\2\2\2\u012b\u012c"+
		"\3\2\2\2\u012c\u012a\3\2\2\2\u012c\u012d\3\2\2\2\u012dR\3\2\2\2\u012e"+
		"\u0130\t\3\2\2\u012f\u012e\3\2\2\2\u0130\u0131\3\2\2\2\u0131\u012f\3\2"+
		"\2\2\u0131\u0132\3\2\2\2\u0132\u0133\3\2\2\2\u0133\u0134\b*\2\2\u0134"+
		"T\3\2\2\2\u0135\u0139\7$\2\2\u0136\u0138\n\4\2\2\u0137\u0136\3\2\2\2\u0138"+
		"\u013b\3\2\2\2\u0139\u0137\3\2\2\2\u0139\u013a\3\2\2\2\u013a\u013c\3\2"+
		"\2\2\u013b\u0139\3\2\2\2\u013c\u0146\7$\2\2\u013d\u0141\7)\2\2\u013e\u0140"+
		"\n\5\2\2\u013f\u013e\3\2\2\2\u0140\u0143\3\2\2\2\u0141\u013f\3\2\2\2\u0141"+
		"\u0142\3\2\2\2\u0142\u0144\3\2\2\2\u0143\u0141\3\2\2\2\u0144\u0146\7)"+
		"\2\2\u0145\u0135\3\2\2\2\u0145\u013d\3\2\2\2\u0146V\3\2\2\2\u0147\u0149"+
		"\4c|\2\u0148\u0147\3\2\2\2\u0149\u014a\3\2\2\2\u014a\u0148\3\2\2\2\u014a"+
		"\u014b\3\2\2\2\u014b\u0152\3\2\2\2\u014c\u014e\4C\\\2\u014d\u014c\3\2"+
		"\2\2\u014e\u014f\3\2\2\2\u014f\u014d\3\2\2\2\u014f\u0150\3\2\2\2\u0150"+
		"\u0152\3\2\2\2\u0151\u0148\3\2\2\2\u0151\u014d\3\2\2\2\u0152X\3\2\2\2"+
		"\u0153\u0155\4\62;\2\u0154\u0153\3\2\2\2\u0155\u0156\3\2\2\2\u0156\u0154"+
		"\3\2\2\2\u0156\u0157\3\2\2\2\u0157Z\3\2\2\2\20\2\u00e2\u011b\u011e\u0127"+
		"\u012c\u0131\u0139\u0141\u0145\u014a\u014f\u0151\u0156\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}