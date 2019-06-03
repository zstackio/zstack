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
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, FILTER_BY=23, OFFSET=24, 
		LIMIT=25, QUERY=26, COUNT=27, SUM=28, DISTINCT=29, ORDER_BY=30, GROUP_BY=31, 
		NAMED_AS=32, ORDER_BY_VALUE=33, RESTRICT_BY=34, RETURN_WITH=35, WHERE=36, 
		AND=37, OR=38, ASC=39, DESC=40, BOOLEAN=41, INT=42, FLOAT=43, ID=44, WS=45, 
		STRING=46;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"T__17", "T__18", "T__19", "T__20", "T__21", "FILTER_BY", "OFFSET", "LIMIT", 
		"QUERY", "COUNT", "SUM", "DISTINCT", "ORDER_BY", "GROUP_BY", "NAMED_AS", 
		"ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", "OR", 
		"ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", "WS", "STRING", "CHAR", 
		"NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", 
		"'is null'", "'is not null'", "'in'", "'not in'", "'like'", "'not like'", 
		"'has'", "'not has'", "'('", "')'", "'{'", "'}'", "'by'", "'filter by'", 
		"'offset'", "'limit'", "'query'", "'count'", "'sum'", "'distinct'", "'order by'", 
		"'group by'", "'named as'", null, "'restrict by'", "'return with'", "'where'", 
		"'and'", "'or'", "'asc'", "'desc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, "FILTER_BY", 
		"OFFSET", "LIMIT", "QUERY", "COUNT", "SUM", "DISTINCT", "ORDER_BY", "GROUP_BY", 
		"NAMED_AS", "ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", 
		"OR", "ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", "WS", "STRING"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\60\u017e\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\3\2\3\2\3\3\3\3\3\4\3\4"+
		"\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f"+
		"\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17"+
		"\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21"+
		"\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\24\3\24\3\25"+
		"\3\25\3\26\3\26\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32"+
		"\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3\35"+
		"\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \3!\3!\3"+
		"!\3!\3!\3!\3!\3!\3!\3\"\3\"\5\"\u0109\n\"\3#\3#\3#\3#\3#\3#\3#\3#\3#\3"+
		"#\3#\3#\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3%\3%\3%\3%\3%\3%\3&\3&\3"+
		"&\3&\3\'\3\'\3\'\3(\3(\3(\3(\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3*\3"+
		"*\5*\u0142\n*\3+\5+\u0145\n+\3+\3+\3,\3,\3,\6,\u014c\n,\r,\16,\u014d\3"+
		"-\6-\u0151\n-\r-\16-\u0152\3.\6.\u0156\n.\r.\16.\u0157\3.\3.\3/\3/\7/"+
		"\u015e\n/\f/\16/\u0161\13/\3/\3/\3/\7/\u0166\n/\f/\16/\u0169\13/\3/\5"+
		"/\u016c\n/\3\60\6\60\u016f\n\60\r\60\16\60\u0170\3\60\6\60\u0174\n\60"+
		"\r\60\16\60\u0175\5\60\u0178\n\60\3\61\6\61\u017b\n\61\r\61\16\61\u017c"+
		"\2\2\62\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17"+
		"\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\35"+
		"9\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\2a\2\3\2\6\6\2\62;C\\"+
		"aac|\5\2\13\f\17\17\"\"\3\2$$\3\2))\2\u0188\2\3\3\2\2\2\2\5\3\2\2\2\2"+
		"\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2"+
		"\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2"+
		"\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2"+
		"\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2"+
		"\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2"+
		"\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2"+
		"M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3"+
		"\2\2\2\2[\3\2\2\2\2]\3\2\2\2\3c\3\2\2\2\5e\3\2\2\2\7g\3\2\2\2\ti\3\2\2"+
		"\2\13k\3\2\2\2\rn\3\2\2\2\17p\3\2\2\2\21s\3\2\2\2\23u\3\2\2\2\25x\3\2"+
		"\2\2\27\u0080\3\2\2\2\31\u008c\3\2\2\2\33\u008f\3\2\2\2\35\u0096\3\2\2"+
		"\2\37\u009b\3\2\2\2!\u00a4\3\2\2\2#\u00a8\3\2\2\2%\u00b0\3\2\2\2\'\u00b2"+
		"\3\2\2\2)\u00b4\3\2\2\2+\u00b6\3\2\2\2-\u00b8\3\2\2\2/\u00bb\3\2\2\2\61"+
		"\u00c5\3\2\2\2\63\u00cc\3\2\2\2\65\u00d2\3\2\2\2\67\u00d8\3\2\2\29\u00de"+
		"\3\2\2\2;\u00e2\3\2\2\2=\u00eb\3\2\2\2?\u00f4\3\2\2\2A\u00fd\3\2\2\2C"+
		"\u0108\3\2\2\2E\u010a\3\2\2\2G\u0116\3\2\2\2I\u0122\3\2\2\2K\u0128\3\2"+
		"\2\2M\u012c\3\2\2\2O\u012f\3\2\2\2Q\u0133\3\2\2\2S\u0141\3\2\2\2U\u0144"+
		"\3\2\2\2W\u0148\3\2\2\2Y\u0150\3\2\2\2[\u0155\3\2\2\2]\u016b\3\2\2\2_"+
		"\u0177\3\2\2\2a\u017a\3\2\2\2cd\7=\2\2d\4\3\2\2\2ef\7\60\2\2f\6\3\2\2"+
		"\2gh\7.\2\2h\b\3\2\2\2ij\7?\2\2j\n\3\2\2\2kl\7#\2\2lm\7?\2\2m\f\3\2\2"+
		"\2no\7@\2\2o\16\3\2\2\2pq\7@\2\2qr\7?\2\2r\20\3\2\2\2st\7>\2\2t\22\3\2"+
		"\2\2uv\7>\2\2vw\7?\2\2w\24\3\2\2\2xy\7k\2\2yz\7u\2\2z{\7\"\2\2{|\7p\2"+
		"\2|}\7w\2\2}~\7n\2\2~\177\7n\2\2\177\26\3\2\2\2\u0080\u0081\7k\2\2\u0081"+
		"\u0082\7u\2\2\u0082\u0083\7\"\2\2\u0083\u0084\7p\2\2\u0084\u0085\7q\2"+
		"\2\u0085\u0086\7v\2\2\u0086\u0087\7\"\2\2\u0087\u0088\7p\2\2\u0088\u0089"+
		"\7w\2\2\u0089\u008a\7n\2\2\u008a\u008b\7n\2\2\u008b\30\3\2\2\2\u008c\u008d"+
		"\7k\2\2\u008d\u008e\7p\2\2\u008e\32\3\2\2\2\u008f\u0090\7p\2\2\u0090\u0091"+
		"\7q\2\2\u0091\u0092\7v\2\2\u0092\u0093\7\"\2\2\u0093\u0094\7k\2\2\u0094"+
		"\u0095\7p\2\2\u0095\34\3\2\2\2\u0096\u0097\7n\2\2\u0097\u0098\7k\2\2\u0098"+
		"\u0099\7m\2\2\u0099\u009a\7g\2\2\u009a\36\3\2\2\2\u009b\u009c\7p\2\2\u009c"+
		"\u009d\7q\2\2\u009d\u009e\7v\2\2\u009e\u009f\7\"\2\2\u009f\u00a0\7n\2"+
		"\2\u00a0\u00a1\7k\2\2\u00a1\u00a2\7m\2\2\u00a2\u00a3\7g\2\2\u00a3 \3\2"+
		"\2\2\u00a4\u00a5\7j\2\2\u00a5\u00a6\7c\2\2\u00a6\u00a7\7u\2\2\u00a7\""+
		"\3\2\2\2\u00a8\u00a9\7p\2\2\u00a9\u00aa\7q\2\2\u00aa\u00ab\7v\2\2\u00ab"+
		"\u00ac\7\"\2\2\u00ac\u00ad\7j\2\2\u00ad\u00ae\7c\2\2\u00ae\u00af\7u\2"+
		"\2\u00af$\3\2\2\2\u00b0\u00b1\7*\2\2\u00b1&\3\2\2\2\u00b2\u00b3\7+\2\2"+
		"\u00b3(\3\2\2\2\u00b4\u00b5\7}\2\2\u00b5*\3\2\2\2\u00b6\u00b7\7\177\2"+
		"\2\u00b7,\3\2\2\2\u00b8\u00b9\7d\2\2\u00b9\u00ba\7{\2\2\u00ba.\3\2\2\2"+
		"\u00bb\u00bc\7h\2\2\u00bc\u00bd\7k\2\2\u00bd\u00be\7n\2\2\u00be\u00bf"+
		"\7v\2\2\u00bf\u00c0\7g\2\2\u00c0\u00c1\7t\2\2\u00c1\u00c2\7\"\2\2\u00c2"+
		"\u00c3\7d\2\2\u00c3\u00c4\7{\2\2\u00c4\60\3\2\2\2\u00c5\u00c6\7q\2\2\u00c6"+
		"\u00c7\7h\2\2\u00c7\u00c8\7h\2\2\u00c8\u00c9\7u\2\2\u00c9\u00ca\7g\2\2"+
		"\u00ca\u00cb\7v\2\2\u00cb\62\3\2\2\2\u00cc\u00cd\7n\2\2\u00cd\u00ce\7"+
		"k\2\2\u00ce\u00cf\7o\2\2\u00cf\u00d0\7k\2\2\u00d0\u00d1\7v\2\2\u00d1\64"+
		"\3\2\2\2\u00d2\u00d3\7s\2\2\u00d3\u00d4\7w\2\2\u00d4\u00d5\7g\2\2\u00d5"+
		"\u00d6\7t\2\2\u00d6\u00d7\7{\2\2\u00d7\66\3\2\2\2\u00d8\u00d9\7e\2\2\u00d9"+
		"\u00da\7q\2\2\u00da\u00db\7w\2\2\u00db\u00dc\7p\2\2\u00dc\u00dd\7v\2\2"+
		"\u00dd8\3\2\2\2\u00de\u00df\7u\2\2\u00df\u00e0\7w\2\2\u00e0\u00e1\7o\2"+
		"\2\u00e1:\3\2\2\2\u00e2\u00e3\7f\2\2\u00e3\u00e4\7k\2\2\u00e4\u00e5\7"+
		"u\2\2\u00e5\u00e6\7v\2\2\u00e6\u00e7\7k\2\2\u00e7\u00e8\7p\2\2\u00e8\u00e9"+
		"\7e\2\2\u00e9\u00ea\7v\2\2\u00ea<\3\2\2\2\u00eb\u00ec\7q\2\2\u00ec\u00ed"+
		"\7t\2\2\u00ed\u00ee\7f\2\2\u00ee\u00ef\7g\2\2\u00ef\u00f0\7t\2\2\u00f0"+
		"\u00f1\7\"\2\2\u00f1\u00f2\7d\2\2\u00f2\u00f3\7{\2\2\u00f3>\3\2\2\2\u00f4"+
		"\u00f5\7i\2\2\u00f5\u00f6\7t\2\2\u00f6\u00f7\7q\2\2\u00f7\u00f8\7w\2\2"+
		"\u00f8\u00f9\7r\2\2\u00f9\u00fa\7\"\2\2\u00fa\u00fb\7d\2\2\u00fb\u00fc"+
		"\7{\2\2\u00fc@\3\2\2\2\u00fd\u00fe\7p\2\2\u00fe\u00ff\7c\2\2\u00ff\u0100"+
		"\7o\2\2\u0100\u0101\7g\2\2\u0101\u0102\7f\2\2\u0102\u0103\7\"\2\2\u0103"+
		"\u0104\7c\2\2\u0104\u0105\7u\2\2\u0105B\3\2\2\2\u0106\u0109\5O(\2\u0107"+
		"\u0109\5Q)\2\u0108\u0106\3\2\2\2\u0108\u0107\3\2\2\2\u0109D\3\2\2\2\u010a"+
		"\u010b\7t\2\2\u010b\u010c\7g\2\2\u010c\u010d\7u\2\2\u010d\u010e\7v\2\2"+
		"\u010e\u010f\7t\2\2\u010f\u0110\7k\2\2\u0110\u0111\7e\2\2\u0111\u0112"+
		"\7v\2\2\u0112\u0113\7\"\2\2\u0113\u0114\7d\2\2\u0114\u0115\7{\2\2\u0115"+
		"F\3\2\2\2\u0116\u0117\7t\2\2\u0117\u0118\7g\2\2\u0118\u0119\7v\2\2\u0119"+
		"\u011a\7w\2\2\u011a\u011b\7t\2\2\u011b\u011c\7p\2\2\u011c\u011d\7\"\2"+
		"\2\u011d\u011e\7y\2\2\u011e\u011f\7k\2\2\u011f\u0120\7v\2\2\u0120\u0121"+
		"\7j\2\2\u0121H\3\2\2\2\u0122\u0123\7y\2\2\u0123\u0124\7j\2\2\u0124\u0125"+
		"\7g\2\2\u0125\u0126\7t\2\2\u0126\u0127\7g\2\2\u0127J\3\2\2\2\u0128\u0129"+
		"\7c\2\2\u0129\u012a\7p\2\2\u012a\u012b\7f\2\2\u012bL\3\2\2\2\u012c\u012d"+
		"\7q\2\2\u012d\u012e\7t\2\2\u012eN\3\2\2\2\u012f\u0130\7c\2\2\u0130\u0131"+
		"\7u\2\2\u0131\u0132\7e\2\2\u0132P\3\2\2\2\u0133\u0134\7f\2\2\u0134\u0135"+
		"\7g\2\2\u0135\u0136\7u\2\2\u0136\u0137\7e\2\2\u0137R\3\2\2\2\u0138\u0139"+
		"\7v\2\2\u0139\u013a\7t\2\2\u013a\u013b\7w\2\2\u013b\u0142\7g\2\2\u013c"+
		"\u013d\7h\2\2\u013d\u013e\7c\2\2\u013e\u013f\7n\2\2\u013f\u0140\7u\2\2"+
		"\u0140\u0142\7g\2\2\u0141\u0138\3\2\2\2\u0141\u013c\3\2\2\2\u0142T\3\2"+
		"\2\2\u0143\u0145\7/\2\2\u0144\u0143\3\2\2\2\u0144\u0145\3\2\2\2\u0145"+
		"\u0146\3\2\2\2\u0146\u0147\5a\61\2\u0147V\3\2\2\2\u0148\u0149\5U+\2\u0149"+
		"\u014b\7\60\2\2\u014a\u014c\5a\61\2\u014b\u014a\3\2\2\2\u014c\u014d\3"+
		"\2\2\2\u014d\u014b\3\2\2\2\u014d\u014e\3\2\2\2\u014eX\3\2\2\2\u014f\u0151"+
		"\t\2\2\2\u0150\u014f\3\2\2\2\u0151\u0152\3\2\2\2\u0152\u0150\3\2\2\2\u0152"+
		"\u0153\3\2\2\2\u0153Z\3\2\2\2\u0154\u0156\t\3\2\2\u0155\u0154\3\2\2\2"+
		"\u0156\u0157\3\2\2\2\u0157\u0155\3\2\2\2\u0157\u0158\3\2\2\2\u0158\u0159"+
		"\3\2\2\2\u0159\u015a\b.\2\2\u015a\\\3\2\2\2\u015b\u015f\7$\2\2\u015c\u015e"+
		"\n\4\2\2\u015d\u015c\3\2\2\2\u015e\u0161\3\2\2\2\u015f\u015d\3\2\2\2\u015f"+
		"\u0160\3\2\2\2\u0160\u0162\3\2\2\2\u0161\u015f\3\2\2\2\u0162\u016c\7$"+
		"\2\2\u0163\u0167\7)\2\2\u0164\u0166\n\5\2\2\u0165\u0164\3\2\2\2\u0166"+
		"\u0169\3\2\2\2\u0167\u0165\3\2\2\2\u0167\u0168\3\2\2\2\u0168\u016a\3\2"+
		"\2\2\u0169\u0167\3\2\2\2\u016a\u016c\7)\2\2\u016b\u015b\3\2\2\2\u016b"+
		"\u0163\3\2\2\2\u016c^\3\2\2\2\u016d\u016f\4c|\2\u016e\u016d\3\2\2\2\u016f"+
		"\u0170\3\2\2\2\u0170\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u0178\3\2"+
		"\2\2\u0172\u0174\4C\\\2\u0173\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175"+
		"\u0173\3\2\2\2\u0175\u0176\3\2\2\2\u0176\u0178\3\2\2\2\u0177\u016e\3\2"+
		"\2\2\u0177\u0173\3\2\2\2\u0178`\3\2\2\2\u0179\u017b\4\62;\2\u017a\u0179"+
		"\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u017a\3\2\2\2\u017c\u017d\3\2\2\2\u017d"+
		"b\3\2\2\2\20\2\u0108\u0141\u0144\u014d\u0152\u0157\u015f\u0167\u016b\u0170"+
		"\u0175\u0177\u017c\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}