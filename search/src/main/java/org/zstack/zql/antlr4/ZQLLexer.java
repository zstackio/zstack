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
			LIMIT=25, QUERY=26, COUNT=27, SUM=28, ORDER_BY=29, GROUP_BY=30, NAMED_AS=31,
			ORDER_BY_VALUE=32, RESTRICT_BY=33, RETURN_WITH=34, WHERE=35, AND=36, OR=37,
			ASC=38, DESC=39, BOOLEAN=40, INT=41, FLOAT=42, ID=43, WS=44, STRING=45;
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
			"QUERY", "COUNT", "SUM", "ORDER_BY", "GROUP_BY", "NAMED_AS", "ORDER_BY_VALUE",
			"RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", "OR", "ASC", "DESC", "BOOLEAN",
			"INT", "FLOAT", "ID", "WS", "STRING", "CHAR", "NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
			null, "';'", "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='",
			"'is null'", "'is not null'", "'in'", "'not in'", "'like'", "'not like'",
			"'has'", "'not has'", "'('", "')'", "'{'", "'}'", "'by'", "'filter by'",
			"'offset'", "'limit'", "'query'", "'count'", "'sum'", "'order by'", "'group by'",
			"'named as'", null, "'restrict by'", "'return with'", "'where'", "'and'",
			"'or'", "'asc'", "'desc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
			null, null, null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null, null, "FILTER_BY",
			"OFFSET", "LIMIT", "QUERY", "COUNT", "SUM", "ORDER_BY", "GROUP_BY", "NAMED_AS",
			"ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", "OR",
			"ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", "WS", "STRING"
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
			"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2/\u0173\b\1\4\2\t"+
					"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
					"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
					"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
					"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
					"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
					",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3"+
					"\6\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13"+
					"\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3"+
					"\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\20"+
					"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22"+
					"\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26"+
					"\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\31"+
					"\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33"+
					"\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35"+
					"\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37"+
					"\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \3!\3!\5!\u00fe\n!\3\""+
					"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3#\3"+
					"#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3%\3%\3%\3%\3&\3&\3&\3\'\3\'\3\'\3\'\3(\3"+
					"(\3(\3(\3(\3)\3)\3)\3)\3)\3)\3)\3)\3)\5)\u0137\n)\3*\5*\u013a\n*\3*\3"+
					"*\3+\3+\3+\6+\u0141\n+\r+\16+\u0142\3,\6,\u0146\n,\r,\16,\u0147\3-\6-"+
					"\u014b\n-\r-\16-\u014c\3-\3-\3.\3.\7.\u0153\n.\f.\16.\u0156\13.\3.\3."+
					"\3.\7.\u015b\n.\f.\16.\u015e\13.\3.\5.\u0161\n.\3/\6/\u0164\n/\r/\16/"+
					"\u0165\3/\6/\u0169\n/\r/\16/\u016a\5/\u016d\n/\3\60\6\60\u0170\n\60\r"+
					"\60\16\60\u0171\2\2\61\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27"+
					"\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33"+
					"\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\2_\2\3\2\6\6\2"+
					"\62;C\\aac|\5\2\13\f\17\17\"\"\3\2$$\3\2))\2\u017d\2\3\3\2\2\2\2\5\3\2"+
					"\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
					"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2"+
					"\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3"+
					"\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3"+
					"\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3"+
					"\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2"+
					"\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2"+
					"Y\3\2\2\2\2[\3\2\2\2\3a\3\2\2\2\5c\3\2\2\2\7e\3\2\2\2\tg\3\2\2\2\13i\3"+
					"\2\2\2\rl\3\2\2\2\17n\3\2\2\2\21q\3\2\2\2\23s\3\2\2\2\25v\3\2\2\2\27~"+
					"\3\2\2\2\31\u008a\3\2\2\2\33\u008d\3\2\2\2\35\u0094\3\2\2\2\37\u0099\3"+
					"\2\2\2!\u00a2\3\2\2\2#\u00a6\3\2\2\2%\u00ae\3\2\2\2\'\u00b0\3\2\2\2)\u00b2"+
					"\3\2\2\2+\u00b4\3\2\2\2-\u00b6\3\2\2\2/\u00b9\3\2\2\2\61\u00c3\3\2\2\2"+
					"\63\u00ca\3\2\2\2\65\u00d0\3\2\2\2\67\u00d6\3\2\2\29\u00dc\3\2\2\2;\u00e0"+
					"\3\2\2\2=\u00e9\3\2\2\2?\u00f2\3\2\2\2A\u00fd\3\2\2\2C\u00ff\3\2\2\2E"+
					"\u010b\3\2\2\2G\u0117\3\2\2\2I\u011d\3\2\2\2K\u0121\3\2\2\2M\u0124\3\2"+
					"\2\2O\u0128\3\2\2\2Q\u0136\3\2\2\2S\u0139\3\2\2\2U\u013d\3\2\2\2W\u0145"+
					"\3\2\2\2Y\u014a\3\2\2\2[\u0160\3\2\2\2]\u016c\3\2\2\2_\u016f\3\2\2\2a"+
					"b\7=\2\2b\4\3\2\2\2cd\7\60\2\2d\6\3\2\2\2ef\7.\2\2f\b\3\2\2\2gh\7?\2\2"+
					"h\n\3\2\2\2ij\7#\2\2jk\7?\2\2k\f\3\2\2\2lm\7@\2\2m\16\3\2\2\2no\7@\2\2"+
					"op\7?\2\2p\20\3\2\2\2qr\7>\2\2r\22\3\2\2\2st\7>\2\2tu\7?\2\2u\24\3\2\2"+
					"\2vw\7k\2\2wx\7u\2\2xy\7\"\2\2yz\7p\2\2z{\7w\2\2{|\7n\2\2|}\7n\2\2}\26"+
					"\3\2\2\2~\177\7k\2\2\177\u0080\7u\2\2\u0080\u0081\7\"\2\2\u0081\u0082"+
					"\7p\2\2\u0082\u0083\7q\2\2\u0083\u0084\7v\2\2\u0084\u0085\7\"\2\2\u0085"+
					"\u0086\7p\2\2\u0086\u0087\7w\2\2\u0087\u0088\7n\2\2\u0088\u0089\7n\2\2"+
					"\u0089\30\3\2\2\2\u008a\u008b\7k\2\2\u008b\u008c\7p\2\2\u008c\32\3\2\2"+
					"\2\u008d\u008e\7p\2\2\u008e\u008f\7q\2\2\u008f\u0090\7v\2\2\u0090\u0091"+
					"\7\"\2\2\u0091\u0092\7k\2\2\u0092\u0093\7p\2\2\u0093\34\3\2\2\2\u0094"+
					"\u0095\7n\2\2\u0095\u0096\7k\2\2\u0096\u0097\7m\2\2\u0097\u0098\7g\2\2"+
					"\u0098\36\3\2\2\2\u0099\u009a\7p\2\2\u009a\u009b\7q\2\2\u009b\u009c\7"+
					"v\2\2\u009c\u009d\7\"\2\2\u009d\u009e\7n\2\2\u009e\u009f\7k\2\2\u009f"+
					"\u00a0\7m\2\2\u00a0\u00a1\7g\2\2\u00a1 \3\2\2\2\u00a2\u00a3\7j\2\2\u00a3"+
					"\u00a4\7c\2\2\u00a4\u00a5\7u\2\2\u00a5\"\3\2\2\2\u00a6\u00a7\7p\2\2\u00a7"+
					"\u00a8\7q\2\2\u00a8\u00a9\7v\2\2\u00a9\u00aa\7\"\2\2\u00aa\u00ab\7j\2"+
					"\2\u00ab\u00ac\7c\2\2\u00ac\u00ad\7u\2\2\u00ad$\3\2\2\2\u00ae\u00af\7"+
					"*\2\2\u00af&\3\2\2\2\u00b0\u00b1\7+\2\2\u00b1(\3\2\2\2\u00b2\u00b3\7}"+
					"\2\2\u00b3*\3\2\2\2\u00b4\u00b5\7\177\2\2\u00b5,\3\2\2\2\u00b6\u00b7\7"+
					"d\2\2\u00b7\u00b8\7{\2\2\u00b8.\3\2\2\2\u00b9\u00ba\7h\2\2\u00ba\u00bb"+
					"\7k\2\2\u00bb\u00bc\7n\2\2\u00bc\u00bd\7v\2\2\u00bd\u00be\7g\2\2\u00be"+
					"\u00bf\7t\2\2\u00bf\u00c0\7\"\2\2\u00c0\u00c1\7d\2\2\u00c1\u00c2\7{\2"+
					"\2\u00c2\60\3\2\2\2\u00c3\u00c4\7q\2\2\u00c4\u00c5\7h\2\2\u00c5\u00c6"+
					"\7h\2\2\u00c6\u00c7\7u\2\2\u00c7\u00c8\7g\2\2\u00c8\u00c9\7v\2\2\u00c9"+
					"\62\3\2\2\2\u00ca\u00cb\7n\2\2\u00cb\u00cc\7k\2\2\u00cc\u00cd\7o\2\2\u00cd"+
					"\u00ce\7k\2\2\u00ce\u00cf\7v\2\2\u00cf\64\3\2\2\2\u00d0\u00d1\7s\2\2\u00d1"+
					"\u00d2\7w\2\2\u00d2\u00d3\7g\2\2\u00d3\u00d4\7t\2\2\u00d4\u00d5\7{\2\2"+
					"\u00d5\66\3\2\2\2\u00d6\u00d7\7e\2\2\u00d7\u00d8\7q\2\2\u00d8\u00d9\7"+
					"w\2\2\u00d9\u00da\7p\2\2\u00da\u00db\7v\2\2\u00db8\3\2\2\2\u00dc\u00dd"+
					"\7u\2\2\u00dd\u00de\7w\2\2\u00de\u00df\7o\2\2\u00df:\3\2\2\2\u00e0\u00e1"+
					"\7q\2\2\u00e1\u00e2\7t\2\2\u00e2\u00e3\7f\2\2\u00e3\u00e4\7g\2\2\u00e4"+
					"\u00e5\7t\2\2\u00e5\u00e6\7\"\2\2\u00e6\u00e7\7d\2\2\u00e7\u00e8\7{\2"+
					"\2\u00e8<\3\2\2\2\u00e9\u00ea\7i\2\2\u00ea\u00eb\7t\2\2\u00eb\u00ec\7"+
					"q\2\2\u00ec\u00ed\7w\2\2\u00ed\u00ee\7r\2\2\u00ee\u00ef\7\"\2\2\u00ef"+
					"\u00f0\7d\2\2\u00f0\u00f1\7{\2\2\u00f1>\3\2\2\2\u00f2\u00f3\7p\2\2\u00f3"+
					"\u00f4\7c\2\2\u00f4\u00f5\7o\2\2\u00f5\u00f6\7g\2\2\u00f6\u00f7\7f\2\2"+
					"\u00f7\u00f8\7\"\2\2\u00f8\u00f9\7c\2\2\u00f9\u00fa\7u\2\2\u00fa@\3\2"+
					"\2\2\u00fb\u00fe\5M\'\2\u00fc\u00fe\5O(\2\u00fd\u00fb\3\2\2\2\u00fd\u00fc"+
					"\3\2\2\2\u00feB\3\2\2\2\u00ff\u0100\7t\2\2\u0100\u0101\7g\2\2\u0101\u0102"+
					"\7u\2\2\u0102\u0103\7v\2\2\u0103\u0104\7t\2\2\u0104\u0105\7k\2\2\u0105"+
					"\u0106\7e\2\2\u0106\u0107\7v\2\2\u0107\u0108\7\"\2\2\u0108\u0109\7d\2"+
					"\2\u0109\u010a\7{\2\2\u010aD\3\2\2\2\u010b\u010c\7t\2\2\u010c\u010d\7"+
					"g\2\2\u010d\u010e\7v\2\2\u010e\u010f\7w\2\2\u010f\u0110\7t\2\2\u0110\u0111"+
					"\7p\2\2\u0111\u0112\7\"\2\2\u0112\u0113\7y\2\2\u0113\u0114\7k\2\2\u0114"+
					"\u0115\7v\2\2\u0115\u0116\7j\2\2\u0116F\3\2\2\2\u0117\u0118\7y\2\2\u0118"+
					"\u0119\7j\2\2\u0119\u011a\7g\2\2\u011a\u011b\7t\2\2\u011b\u011c\7g\2\2"+
					"\u011cH\3\2\2\2\u011d\u011e\7c\2\2\u011e\u011f\7p\2\2\u011f\u0120\7f\2"+
					"\2\u0120J\3\2\2\2\u0121\u0122\7q\2\2\u0122\u0123\7t\2\2\u0123L\3\2\2\2"+
					"\u0124\u0125\7c\2\2\u0125\u0126\7u\2\2\u0126\u0127\7e\2\2\u0127N\3\2\2"+
					"\2\u0128\u0129\7f\2\2\u0129\u012a\7g\2\2\u012a\u012b\7u\2\2\u012b\u012c"+
					"\7e\2\2\u012cP\3\2\2\2\u012d\u012e\7v\2\2\u012e\u012f\7t\2\2\u012f\u0130"+
					"\7w\2\2\u0130\u0137\7g\2\2\u0131\u0132\7h\2\2\u0132\u0133\7c\2\2\u0133"+
					"\u0134\7n\2\2\u0134\u0135\7u\2\2\u0135\u0137\7g\2\2\u0136\u012d\3\2\2"+
					"\2\u0136\u0131\3\2\2\2\u0137R\3\2\2\2\u0138\u013a\7/\2\2\u0139\u0138\3"+
					"\2\2\2\u0139\u013a\3\2\2\2\u013a\u013b\3\2\2\2\u013b\u013c\5_\60\2\u013c"+
					"T\3\2\2\2\u013d\u013e\5S*\2\u013e\u0140\7\60\2\2\u013f\u0141\5_\60\2\u0140"+
					"\u013f\3\2\2\2\u0141\u0142\3\2\2\2\u0142\u0140\3\2\2\2\u0142\u0143\3\2"+
					"\2\2\u0143V\3\2\2\2\u0144\u0146\t\2\2\2\u0145\u0144\3\2\2\2\u0146\u0147"+
					"\3\2\2\2\u0147\u0145\3\2\2\2\u0147\u0148\3\2\2\2\u0148X\3\2\2\2\u0149"+
					"\u014b\t\3\2\2\u014a\u0149\3\2\2\2\u014b\u014c\3\2\2\2\u014c\u014a\3\2"+
					"\2\2\u014c\u014d\3\2\2\2\u014d\u014e\3\2\2\2\u014e\u014f\b-\2\2\u014f"+
					"Z\3\2\2\2\u0150\u0154\7$\2\2\u0151\u0153\n\4\2\2\u0152\u0151\3\2\2\2\u0153"+
					"\u0156\3\2\2\2\u0154\u0152\3\2\2\2\u0154\u0155\3\2\2\2\u0155\u0157\3\2"+
					"\2\2\u0156\u0154\3\2\2\2\u0157\u0161\7$\2\2\u0158\u015c\7)\2\2\u0159\u015b"+
					"\n\5\2\2\u015a\u0159\3\2\2\2\u015b\u015e\3\2\2\2\u015c\u015a\3\2\2\2\u015c"+
					"\u015d\3\2\2\2\u015d\u015f\3\2\2\2\u015e\u015c\3\2\2\2\u015f\u0161\7)"+
					"\2\2\u0160\u0150\3\2\2\2\u0160\u0158\3\2\2\2\u0161\\\3\2\2\2\u0162\u0164"+
					"\4c|\2\u0163\u0162\3\2\2\2\u0164\u0165\3\2\2\2\u0165\u0163\3\2\2\2\u0165"+
					"\u0166\3\2\2\2\u0166\u016d\3\2\2\2\u0167\u0169\4C\\\2\u0168\u0167\3\2"+
					"\2\2\u0169\u016a\3\2\2\2\u016a\u0168\3\2\2\2\u016a\u016b\3\2\2\2\u016b"+
					"\u016d\3\2\2\2\u016c\u0163\3\2\2\2\u016c\u0168\3\2\2\2\u016d^\3\2\2\2"+
					"\u016e\u0170\4\62;\2\u016f\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u016f"+
					"\3\2\2\2\u0171\u0172\3\2\2\2\u0172`\3\2\2\2\20\2\u00fd\u0136\u0139\u0142"+
					"\u0147\u014c\u0154\u015c\u0160\u0165\u016a\u016c\u0171\3\b\2\2";
	public static final ATN _ATN =
			new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}