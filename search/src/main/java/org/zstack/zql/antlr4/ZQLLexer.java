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
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, OFFSET=17, 
		LIMIT=18, QUERY=19, ORDER_BY=20, ORDER_BY_VALUE=21, RESTRICT_BY=22, RETURN_WITH=23, 
		WHERE=24, AND=25, OR=26, ASC=27, DESC=28, INT=29, FLOAT=30, ID=31, WS=32, 
		STRING=33;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "OFFSET", 
		"LIMIT", "QUERY", "ORDER_BY", "ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", 
		"WHERE", "AND", "OR", "ASC", "DESC", "INT", "FLOAT", "ID", "WS", "STRING", 
		"CHAR", "NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'.'", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", "'is null'", 
		"'not null'", "'in'", "'not in'", "'like'", "'not like'", "'('", "','", 
		"')'", "'offset'", "'limit'", "'query'", "'order by'", null, "'restrict by'", 
		"'return with'", "'where'", "'and'", "'or'", "'asc'", "'desc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, "OFFSET", "LIMIT", "QUERY", "ORDER_BY", 
		"ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", "WHERE", "AND", "OR", 
		"ASC", "DESC", "INT", "FLOAT", "ID", "WS", "STRING"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2#\u0114\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\6\3\6\3"+
		"\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3"+
		"\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17"+
		"\3\20\3\20\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25"+
		"\3\25\3\25\3\25\3\25\3\26\3\26\5\26\u00a8\n\26\3\27\3\27\3\27\3\27\3\27"+
		"\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32"+
		"\3\32\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\36"+
		"\5\36\u00d9\n\36\3\36\3\36\3\37\3\37\3\37\6\37\u00e0\n\37\r\37\16\37\u00e1"+
		"\3 \6 \u00e5\n \r \16 \u00e6\3!\6!\u00ea\n!\r!\16!\u00eb\3!\3!\3\"\3\""+
		"\6\"\u00f2\n\"\r\"\16\"\u00f3\3\"\3\"\6\"\u00f8\n\"\r\"\16\"\u00f9\3\""+
		"\6\"\u00fd\n\"\r\"\16\"\u00fe\3\"\5\"\u0102\n\"\3#\6#\u0105\n#\r#\16#"+
		"\u0106\3#\6#\u010a\n#\r#\16#\u010b\5#\u010e\n#\3$\6$\u0111\n$\r$\16$\u0112"+
		"\2\2%\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35"+
		"\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36"+
		";\37= ?!A\"C#E\2G\2\3\2\6\6\2\62;C\\aac|\5\2\13\f\17\17\"\"\3\2$$\3\2"+
		"))\2\u011e\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2"+
		"\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27"+
		"\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2"+
		"\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2"+
		"\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2"+
		"\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\3I\3\2\2\2"+
		"\5K\3\2\2\2\7M\3\2\2\2\tP\3\2\2\2\13R\3\2\2\2\rU\3\2\2\2\17W\3\2\2\2\21"+
		"Z\3\2\2\2\23b\3\2\2\2\25k\3\2\2\2\27n\3\2\2\2\31u\3\2\2\2\33z\3\2\2\2"+
		"\35\u0083\3\2\2\2\37\u0085\3\2\2\2!\u0087\3\2\2\2#\u0089\3\2\2\2%\u0090"+
		"\3\2\2\2\'\u0096\3\2\2\2)\u009c\3\2\2\2+\u00a7\3\2\2\2-\u00a9\3\2\2\2"+
		"/\u00b5\3\2\2\2\61\u00c1\3\2\2\2\63\u00c7\3\2\2\2\65\u00cb\3\2\2\2\67"+
		"\u00ce\3\2\2\29\u00d2\3\2\2\2;\u00d8\3\2\2\2=\u00dc\3\2\2\2?\u00e4\3\2"+
		"\2\2A\u00e9\3\2\2\2C\u0101\3\2\2\2E\u010d\3\2\2\2G\u0110\3\2\2\2IJ\7\60"+
		"\2\2J\4\3\2\2\2KL\7?\2\2L\6\3\2\2\2MN\7#\2\2NO\7?\2\2O\b\3\2\2\2PQ\7@"+
		"\2\2Q\n\3\2\2\2RS\7@\2\2ST\7?\2\2T\f\3\2\2\2UV\7>\2\2V\16\3\2\2\2WX\7"+
		">\2\2XY\7?\2\2Y\20\3\2\2\2Z[\7k\2\2[\\\7u\2\2\\]\7\"\2\2]^\7p\2\2^_\7"+
		"w\2\2_`\7n\2\2`a\7n\2\2a\22\3\2\2\2bc\7p\2\2cd\7q\2\2de\7v\2\2ef\7\"\2"+
		"\2fg\7p\2\2gh\7w\2\2hi\7n\2\2ij\7n\2\2j\24\3\2\2\2kl\7k\2\2lm\7p\2\2m"+
		"\26\3\2\2\2no\7p\2\2op\7q\2\2pq\7v\2\2qr\7\"\2\2rs\7k\2\2st\7p\2\2t\30"+
		"\3\2\2\2uv\7n\2\2vw\7k\2\2wx\7m\2\2xy\7g\2\2y\32\3\2\2\2z{\7p\2\2{|\7"+
		"q\2\2|}\7v\2\2}~\7\"\2\2~\177\7n\2\2\177\u0080\7k\2\2\u0080\u0081\7m\2"+
		"\2\u0081\u0082\7g\2\2\u0082\34\3\2\2\2\u0083\u0084\7*\2\2\u0084\36\3\2"+
		"\2\2\u0085\u0086\7.\2\2\u0086 \3\2\2\2\u0087\u0088\7+\2\2\u0088\"\3\2"+
		"\2\2\u0089\u008a\7q\2\2\u008a\u008b\7h\2\2\u008b\u008c\7h\2\2\u008c\u008d"+
		"\7u\2\2\u008d\u008e\7g\2\2\u008e\u008f\7v\2\2\u008f$\3\2\2\2\u0090\u0091"+
		"\7n\2\2\u0091\u0092\7k\2\2\u0092\u0093\7o\2\2\u0093\u0094\7k\2\2\u0094"+
		"\u0095\7v\2\2\u0095&\3\2\2\2\u0096\u0097\7s\2\2\u0097\u0098\7w\2\2\u0098"+
		"\u0099\7g\2\2\u0099\u009a\7t\2\2\u009a\u009b\7{\2\2\u009b(\3\2\2\2\u009c"+
		"\u009d\7q\2\2\u009d\u009e\7t\2\2\u009e\u009f\7f\2\2\u009f\u00a0\7g\2\2"+
		"\u00a0\u00a1\7t\2\2\u00a1\u00a2\7\"\2\2\u00a2\u00a3\7d\2\2\u00a3\u00a4"+
		"\7{\2\2\u00a4*\3\2\2\2\u00a5\u00a8\5\67\34\2\u00a6\u00a8\59\35\2\u00a7"+
		"\u00a5\3\2\2\2\u00a7\u00a6\3\2\2\2\u00a8,\3\2\2\2\u00a9\u00aa\7t\2\2\u00aa"+
		"\u00ab\7g\2\2\u00ab\u00ac\7u\2\2\u00ac\u00ad\7v\2\2\u00ad\u00ae\7t\2\2"+
		"\u00ae\u00af\7k\2\2\u00af\u00b0\7e\2\2\u00b0\u00b1\7v\2\2\u00b1\u00b2"+
		"\7\"\2\2\u00b2\u00b3\7d\2\2\u00b3\u00b4\7{\2\2\u00b4.\3\2\2\2\u00b5\u00b6"+
		"\7t\2\2\u00b6\u00b7\7g\2\2\u00b7\u00b8\7v\2\2\u00b8\u00b9\7w\2\2\u00b9"+
		"\u00ba\7t\2\2\u00ba\u00bb\7p\2\2\u00bb\u00bc\7\"\2\2\u00bc\u00bd\7y\2"+
		"\2\u00bd\u00be\7k\2\2\u00be\u00bf\7v\2\2\u00bf\u00c0\7j\2\2\u00c0\60\3"+
		"\2\2\2\u00c1\u00c2\7y\2\2\u00c2\u00c3\7j\2\2\u00c3\u00c4\7g\2\2\u00c4"+
		"\u00c5\7t\2\2\u00c5\u00c6\7g\2\2\u00c6\62\3\2\2\2\u00c7\u00c8\7c\2\2\u00c8"+
		"\u00c9\7p\2\2\u00c9\u00ca\7f\2\2\u00ca\64\3\2\2\2\u00cb\u00cc\7q\2\2\u00cc"+
		"\u00cd\7t\2\2\u00cd\66\3\2\2\2\u00ce\u00cf\7c\2\2\u00cf\u00d0\7u\2\2\u00d0"+
		"\u00d1\7e\2\2\u00d18\3\2\2\2\u00d2\u00d3\7f\2\2\u00d3\u00d4\7g\2\2\u00d4"+
		"\u00d5\7u\2\2\u00d5\u00d6\7e\2\2\u00d6:\3\2\2\2\u00d7\u00d9\7/\2\2\u00d8"+
		"\u00d7\3\2\2\2\u00d8\u00d9\3\2\2\2\u00d9\u00da\3\2\2\2\u00da\u00db\5G"+
		"$\2\u00db<\3\2\2\2\u00dc\u00dd\5;\36\2\u00dd\u00df\7\60\2\2\u00de\u00e0"+
		"\5G$\2\u00df\u00de\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00df\3\2\2\2\u00e1"+
		"\u00e2\3\2\2\2\u00e2>\3\2\2\2\u00e3\u00e5\t\2\2\2\u00e4\u00e3\3\2\2\2"+
		"\u00e5\u00e6\3\2\2\2\u00e6\u00e4\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7@\3"+
		"\2\2\2\u00e8\u00ea\t\3\2\2\u00e9\u00e8\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb"+
		"\u00e9\3\2\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00ee\b!"+
		"\2\2\u00eeB\3\2\2\2\u00ef\u00f1\7$\2\2\u00f0\u00f2\n\4\2\2\u00f1\u00f0"+
		"\3\2\2\2\u00f2\u00f3\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f3\u00f4\3\2\2\2\u00f4"+
		"\u00f5\3\2\2\2\u00f5\u0102\7$\2\2\u00f6\u00f8\7)\2\2\u00f7\u00f6\3\2\2"+
		"\2\u00f8\u00f9\3\2\2\2\u00f9\u00f7\3\2\2\2\u00f9\u00fa\3\2\2\2\u00fa\u00fc"+
		"\3\2\2\2\u00fb\u00fd\n\5\2\2\u00fc\u00fb\3\2\2\2\u00fd\u00fe\3\2\2\2\u00fe"+
		"\u00fc\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff\u0100\3\2\2\2\u0100\u0102\7)"+
		"\2\2\u0101\u00ef\3\2\2\2\u0101\u00f7\3\2\2\2\u0102D\3\2\2\2\u0103\u0105"+
		"\4c|\2\u0104\u0103\3\2\2\2\u0105\u0106\3\2\2\2\u0106\u0104\3\2\2\2\u0106"+
		"\u0107\3\2\2\2\u0107\u010e\3\2\2\2\u0108\u010a\4C\\\2\u0109\u0108\3\2"+
		"\2\2\u010a\u010b\3\2\2\2\u010b\u0109\3\2\2\2\u010b\u010c\3\2\2\2\u010c"+
		"\u010e\3\2\2\2\u010d\u0104\3\2\2\2\u010d\u0109\3\2\2\2\u010eF\3\2\2\2"+
		"\u010f\u0111\4\62;\2\u0110\u010f\3\2\2\2\u0111\u0112\3\2\2\2\u0112\u0110"+
		"\3\2\2\2\u0112\u0113\3\2\2\2\u0113H\3\2\2\2\20\2\u00a7\u00d8\u00e1\u00e6"+
		"\u00eb\u00f3\u00f9\u00fe\u0101\u0106\u010b\u010d\u0112\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}