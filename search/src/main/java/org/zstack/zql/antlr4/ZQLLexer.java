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
		T__17=18, T__18=19, FILTER_BY=20, OFFSET=21, LIMIT=22, QUERY=23, COUNT=24, 
		ORDER_BY=25, NAMED_AS=26, ORDER_BY_VALUE=27, RESTRICT_BY=28, RETURN_WITH=29, 
		WHERE=30, AND=31, OR=32, ASC=33, DESC=34, BOOLEAN=35, INT=36, FLOAT=37, 
		ID=38, WS=39, STRING=40;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"T__17", "T__18", "FILTER_BY", "OFFSET", "LIMIT", "QUERY", "COUNT", "ORDER_BY", 
		"NAMED_AS", "ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", 
		"OR", "ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", "WS", "STRING", 
		"CHAR", "NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", 
		"'is null'", "'is not null'", "'in'", "'not in'", "'like'", "'not like'", 
		"'('", "')'", "'{'", "'}'", "'filter by'", "'offset'", "'limit'", "'query'", 
		"'count'", "'order by'", "'named as'", null, "'restrict by'", "'return with'", 
		"'where'", "'and'", "'or'", "'asc'", "'desc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, "FILTER_BY", "OFFSET", 
		"LIMIT", "QUERY", "COUNT", "ORDER_BY", "NAMED_AS", "ORDER_BY_VALUE", "RESTRICT_BY", 
		"RETURN_WITH", "WHERE", "AND", "OR", "ASC", "DESC", "BOOLEAN", "INT", 
		"FLOAT", "ID", "WS", "STRING"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2*\u014d\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\3"+
		"\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t"+
		"\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3"+
		"\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3"+
		"\16\3\16\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\25\3\25\3"+
		"\25\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3"+
		"\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3"+
		"\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3"+
		"\33\3\33\3\33\3\33\3\33\3\33\3\33\3\34\3\34\5\34\u00d8\n\34\3\35\3\35"+
		"\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3 \3 \3 \3 \3!\3!\3!\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3"+
		"$\3$\3$\3$\5$\u0111\n$\3%\5%\u0114\n%\3%\3%\3&\3&\3&\6&\u011b\n&\r&\16"+
		"&\u011c\3\'\6\'\u0120\n\'\r\'\16\'\u0121\3(\6(\u0125\n(\r(\16(\u0126\3"+
		"(\3(\3)\3)\7)\u012d\n)\f)\16)\u0130\13)\3)\3)\3)\7)\u0135\n)\f)\16)\u0138"+
		"\13)\3)\5)\u013b\n)\3*\6*\u013e\n*\r*\16*\u013f\3*\6*\u0143\n*\r*\16*"+
		"\u0144\5*\u0147\n*\3+\6+\u014a\n+\r+\16+\u014b\2\2,\3\3\5\4\7\5\t\6\13"+
		"\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'"+
		"\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'"+
		"M(O)Q*S\2U\2\3\2\6\6\2\62;C\\aac|\5\2\13\f\17\17\"\"\3\2$$\3\2))\2\u0157"+
		"\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2"+
		"\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2"+
		"\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2"+
		"\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3"+
		"\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2"+
		"\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\3W\3\2\2\2\5"+
		"Y\3\2\2\2\7[\3\2\2\2\t]\3\2\2\2\13_\3\2\2\2\rb\3\2\2\2\17d\3\2\2\2\21"+
		"g\3\2\2\2\23i\3\2\2\2\25l\3\2\2\2\27t\3\2\2\2\31\u0080\3\2\2\2\33\u0083"+
		"\3\2\2\2\35\u008a\3\2\2\2\37\u008f\3\2\2\2!\u0098\3\2\2\2#\u009a\3\2\2"+
		"\2%\u009c\3\2\2\2\'\u009e\3\2\2\2)\u00a0\3\2\2\2+\u00aa\3\2\2\2-\u00b1"+
		"\3\2\2\2/\u00b7\3\2\2\2\61\u00bd\3\2\2\2\63\u00c3\3\2\2\2\65\u00cc\3\2"+
		"\2\2\67\u00d7\3\2\2\29\u00d9\3\2\2\2;\u00e5\3\2\2\2=\u00f1\3\2\2\2?\u00f7"+
		"\3\2\2\2A\u00fb\3\2\2\2C\u00fe\3\2\2\2E\u0102\3\2\2\2G\u0110\3\2\2\2I"+
		"\u0113\3\2\2\2K\u0117\3\2\2\2M\u011f\3\2\2\2O\u0124\3\2\2\2Q\u013a\3\2"+
		"\2\2S\u0146\3\2\2\2U\u0149\3\2\2\2WX\7=\2\2X\4\3\2\2\2YZ\7\60\2\2Z\6\3"+
		"\2\2\2[\\\7.\2\2\\\b\3\2\2\2]^\7?\2\2^\n\3\2\2\2_`\7#\2\2`a\7?\2\2a\f"+
		"\3\2\2\2bc\7@\2\2c\16\3\2\2\2de\7@\2\2ef\7?\2\2f\20\3\2\2\2gh\7>\2\2h"+
		"\22\3\2\2\2ij\7>\2\2jk\7?\2\2k\24\3\2\2\2lm\7k\2\2mn\7u\2\2no\7\"\2\2"+
		"op\7p\2\2pq\7w\2\2qr\7n\2\2rs\7n\2\2s\26\3\2\2\2tu\7k\2\2uv\7u\2\2vw\7"+
		"\"\2\2wx\7p\2\2xy\7q\2\2yz\7v\2\2z{\7\"\2\2{|\7p\2\2|}\7w\2\2}~\7n\2\2"+
		"~\177\7n\2\2\177\30\3\2\2\2\u0080\u0081\7k\2\2\u0081\u0082\7p\2\2\u0082"+
		"\32\3\2\2\2\u0083\u0084\7p\2\2\u0084\u0085\7q\2\2\u0085\u0086\7v\2\2\u0086"+
		"\u0087\7\"\2\2\u0087\u0088\7k\2\2\u0088\u0089\7p\2\2\u0089\34\3\2\2\2"+
		"\u008a\u008b\7n\2\2\u008b\u008c\7k\2\2\u008c\u008d\7m\2\2\u008d\u008e"+
		"\7g\2\2\u008e\36\3\2\2\2\u008f\u0090\7p\2\2\u0090\u0091\7q\2\2\u0091\u0092"+
		"\7v\2\2\u0092\u0093\7\"\2\2\u0093\u0094\7n\2\2\u0094\u0095\7k\2\2\u0095"+
		"\u0096\7m\2\2\u0096\u0097\7g\2\2\u0097 \3\2\2\2\u0098\u0099\7*\2\2\u0099"+
		"\"\3\2\2\2\u009a\u009b\7+\2\2\u009b$\3\2\2\2\u009c\u009d\7}\2\2\u009d"+
		"&\3\2\2\2\u009e\u009f\7\177\2\2\u009f(\3\2\2\2\u00a0\u00a1\7h\2\2\u00a1"+
		"\u00a2\7k\2\2\u00a2\u00a3\7n\2\2\u00a3\u00a4\7v\2\2\u00a4\u00a5\7g\2\2"+
		"\u00a5\u00a6\7t\2\2\u00a6\u00a7\7\"\2\2\u00a7\u00a8\7d\2\2\u00a8\u00a9"+
		"\7{\2\2\u00a9*\3\2\2\2\u00aa\u00ab\7q\2\2\u00ab\u00ac\7h\2\2\u00ac\u00ad"+
		"\7h\2\2\u00ad\u00ae\7u\2\2\u00ae\u00af\7g\2\2\u00af\u00b0\7v\2\2\u00b0"+
		",\3\2\2\2\u00b1\u00b2\7n\2\2\u00b2\u00b3\7k\2\2\u00b3\u00b4\7o\2\2\u00b4"+
		"\u00b5\7k\2\2\u00b5\u00b6\7v\2\2\u00b6.\3\2\2\2\u00b7\u00b8\7s\2\2\u00b8"+
		"\u00b9\7w\2\2\u00b9\u00ba\7g\2\2\u00ba\u00bb\7t\2\2\u00bb\u00bc\7{\2\2"+
		"\u00bc\60\3\2\2\2\u00bd\u00be\7e\2\2\u00be\u00bf\7q\2\2\u00bf\u00c0\7"+
		"w\2\2\u00c0\u00c1\7p\2\2\u00c1\u00c2\7v\2\2\u00c2\62\3\2\2\2\u00c3\u00c4"+
		"\7q\2\2\u00c4\u00c5\7t\2\2\u00c5\u00c6\7f\2\2\u00c6\u00c7\7g\2\2\u00c7"+
		"\u00c8\7t\2\2\u00c8\u00c9\7\"\2\2\u00c9\u00ca\7d\2\2\u00ca\u00cb\7{\2"+
		"\2\u00cb\64\3\2\2\2\u00cc\u00cd\7p\2\2\u00cd\u00ce\7c\2\2\u00ce\u00cf"+
		"\7o\2\2\u00cf\u00d0\7g\2\2\u00d0\u00d1\7f\2\2\u00d1\u00d2\7\"\2\2\u00d2"+
		"\u00d3\7c\2\2\u00d3\u00d4\7u\2\2\u00d4\66\3\2\2\2\u00d5\u00d8\5C\"\2\u00d6"+
		"\u00d8\5E#\2\u00d7\u00d5\3\2\2\2\u00d7\u00d6\3\2\2\2\u00d88\3\2\2\2\u00d9"+
		"\u00da\7t\2\2\u00da\u00db\7g\2\2\u00db\u00dc\7u\2\2\u00dc\u00dd\7v\2\2"+
		"\u00dd\u00de\7t\2\2\u00de\u00df\7k\2\2\u00df\u00e0\7e\2\2\u00e0\u00e1"+
		"\7v\2\2\u00e1\u00e2\7\"\2\2\u00e2\u00e3\7d\2\2\u00e3\u00e4\7{\2\2\u00e4"+
		":\3\2\2\2\u00e5\u00e6\7t\2\2\u00e6\u00e7\7g\2\2\u00e7\u00e8\7v\2\2\u00e8"+
		"\u00e9\7w\2\2\u00e9\u00ea\7t\2\2\u00ea\u00eb\7p\2\2\u00eb\u00ec\7\"\2"+
		"\2\u00ec\u00ed\7y\2\2\u00ed\u00ee\7k\2\2\u00ee\u00ef\7v\2\2\u00ef\u00f0"+
		"\7j\2\2\u00f0<\3\2\2\2\u00f1\u00f2\7y\2\2\u00f2\u00f3\7j\2\2\u00f3\u00f4"+
		"\7g\2\2\u00f4\u00f5\7t\2\2\u00f5\u00f6\7g\2\2\u00f6>\3\2\2\2\u00f7\u00f8"+
		"\7c\2\2\u00f8\u00f9\7p\2\2\u00f9\u00fa\7f\2\2\u00fa@\3\2\2\2\u00fb\u00fc"+
		"\7q\2\2\u00fc\u00fd\7t\2\2\u00fdB\3\2\2\2\u00fe\u00ff\7c\2\2\u00ff\u0100"+
		"\7u\2\2\u0100\u0101\7e\2\2\u0101D\3\2\2\2\u0102\u0103\7f\2\2\u0103\u0104"+
		"\7g\2\2\u0104\u0105\7u\2\2\u0105\u0106\7e\2\2\u0106F\3\2\2\2\u0107\u0108"+
		"\7v\2\2\u0108\u0109\7t\2\2\u0109\u010a\7w\2\2\u010a\u0111\7g\2\2\u010b"+
		"\u010c\7h\2\2\u010c\u010d\7c\2\2\u010d\u010e\7n\2\2\u010e\u010f\7u\2\2"+
		"\u010f\u0111\7g\2\2\u0110\u0107\3\2\2\2\u0110\u010b\3\2\2\2\u0111H\3\2"+
		"\2\2\u0112\u0114\7/\2\2\u0113\u0112\3\2\2\2\u0113\u0114\3\2\2\2\u0114"+
		"\u0115\3\2\2\2\u0115\u0116\5U+\2\u0116J\3\2\2\2\u0117\u0118\5I%\2\u0118"+
		"\u011a\7\60\2\2\u0119\u011b\5U+\2\u011a\u0119\3\2\2\2\u011b\u011c\3\2"+
		"\2\2\u011c\u011a\3\2\2\2\u011c\u011d\3\2\2\2\u011dL\3\2\2\2\u011e\u0120"+
		"\t\2\2\2\u011f\u011e\3\2\2\2\u0120\u0121\3\2\2\2\u0121\u011f\3\2\2\2\u0121"+
		"\u0122\3\2\2\2\u0122N\3\2\2\2\u0123\u0125\t\3\2\2\u0124\u0123\3\2\2\2"+
		"\u0125\u0126\3\2\2\2\u0126\u0124\3\2\2\2\u0126\u0127\3\2\2\2\u0127\u0128"+
		"\3\2\2\2\u0128\u0129\b(\2\2\u0129P\3\2\2\2\u012a\u012e\7$\2\2\u012b\u012d"+
		"\n\4\2\2\u012c\u012b\3\2\2\2\u012d\u0130\3\2\2\2\u012e\u012c\3\2\2\2\u012e"+
		"\u012f\3\2\2\2\u012f\u0131\3\2\2\2\u0130\u012e\3\2\2\2\u0131\u013b\7$"+
		"\2\2\u0132\u0136\7)\2\2\u0133\u0135\n\5\2\2\u0134\u0133\3\2\2\2\u0135"+
		"\u0138\3\2\2\2\u0136\u0134\3\2\2\2\u0136\u0137\3\2\2\2\u0137\u0139\3\2"+
		"\2\2\u0138\u0136\3\2\2\2\u0139\u013b\7)\2\2\u013a\u012a\3\2\2\2\u013a"+
		"\u0132\3\2\2\2\u013bR\3\2\2\2\u013c\u013e\4c|\2\u013d\u013c\3\2\2\2\u013e"+
		"\u013f\3\2\2\2\u013f\u013d\3\2\2\2\u013f\u0140\3\2\2\2\u0140\u0147\3\2"+
		"\2\2\u0141\u0143\4C\\\2\u0142\u0141\3\2\2\2\u0143\u0144\3\2\2\2\u0144"+
		"\u0142\3\2\2\2\u0144\u0145\3\2\2\2\u0145\u0147\3\2\2\2\u0146\u013d\3\2"+
		"\2\2\u0146\u0142\3\2\2\2\u0147T\3\2\2\2\u0148\u014a\4\62;\2\u0149\u0148"+
		"\3\2\2\2\u014a\u014b\3\2\2\2\u014b\u0149\3\2\2\2\u014b\u014c\3\2\2\2\u014c"+
		"V\3\2\2\2\20\2\u00d7\u0110\u0113\u011c\u0121\u0126\u012e\u0136\u013a\u013f"+
		"\u0144\u0146\u014b\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}