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
		T__17=18, T__18=19, INT=20, FLOAT=21, ID=22, WS=23, STRING=24;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"T__17", "T__18", "INT", "FLOAT", "ID", "WS", "STRING", "CHAR", "NUMBER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'.'", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", "'is null'", 
		"'not null'", "'in'", "'not in'", "'like'", "'not like'", "'and'", "'or'", 
		"'('", "')'", "'query'", "'where'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, "INT", "FLOAT", "ID", 
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\32\u00c5\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\6\3\6"+
		"\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3"+
		"\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r"+
		"\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17"+
		"\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\24\3\24\3\24\3\24\3\24\3\24\3\25\5\25\u008a\n\25\3\25\3\25\3\26"+
		"\3\26\3\26\6\26\u0091\n\26\r\26\16\26\u0092\3\27\6\27\u0096\n\27\r\27"+
		"\16\27\u0097\3\30\6\30\u009b\n\30\r\30\16\30\u009c\3\30\3\30\3\31\3\31"+
		"\6\31\u00a3\n\31\r\31\16\31\u00a4\3\31\3\31\6\31\u00a9\n\31\r\31\16\31"+
		"\u00aa\3\31\6\31\u00ae\n\31\r\31\16\31\u00af\3\31\5\31\u00b3\n\31\3\32"+
		"\6\32\u00b6\n\32\r\32\16\32\u00b7\3\32\6\32\u00bb\n\32\r\32\16\32\u00bc"+
		"\5\32\u00bf\n\32\3\33\6\33\u00c2\n\33\r\33\16\33\u00c3\2\2\34\3\3\5\4"+
		"\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22"+
		"#\23%\24\'\25)\26+\27-\30/\31\61\32\63\2\65\2\3\2\6\6\2\62;C\\aac|\5\2"+
		"\13\f\17\17\"\"\3\2$$\3\2))\2\u00ce\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2"+
		"\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23"+
		"\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2"+
		"\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2"+
		"\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\3\67\3\2\2\2\59\3\2"+
		"\2\2\7;\3\2\2\2\t>\3\2\2\2\13@\3\2\2\2\rC\3\2\2\2\17E\3\2\2\2\21H\3\2"+
		"\2\2\23P\3\2\2\2\25Y\3\2\2\2\27\\\3\2\2\2\31c\3\2\2\2\33h\3\2\2\2\35q"+
		"\3\2\2\2\37u\3\2\2\2!x\3\2\2\2#z\3\2\2\2%|\3\2\2\2\'\u0082\3\2\2\2)\u0089"+
		"\3\2\2\2+\u008d\3\2\2\2-\u0095\3\2\2\2/\u009a\3\2\2\2\61\u00b2\3\2\2\2"+
		"\63\u00be\3\2\2\2\65\u00c1\3\2\2\2\678\7\60\2\28\4\3\2\2\29:\7?\2\2:\6"+
		"\3\2\2\2;<\7#\2\2<=\7?\2\2=\b\3\2\2\2>?\7@\2\2?\n\3\2\2\2@A\7@\2\2AB\7"+
		"?\2\2B\f\3\2\2\2CD\7>\2\2D\16\3\2\2\2EF\7>\2\2FG\7?\2\2G\20\3\2\2\2HI"+
		"\7k\2\2IJ\7u\2\2JK\7\"\2\2KL\7p\2\2LM\7w\2\2MN\7n\2\2NO\7n\2\2O\22\3\2"+
		"\2\2PQ\7p\2\2QR\7q\2\2RS\7v\2\2ST\7\"\2\2TU\7p\2\2UV\7w\2\2VW\7n\2\2W"+
		"X\7n\2\2X\24\3\2\2\2YZ\7k\2\2Z[\7p\2\2[\26\3\2\2\2\\]\7p\2\2]^\7q\2\2"+
		"^_\7v\2\2_`\7\"\2\2`a\7k\2\2ab\7p\2\2b\30\3\2\2\2cd\7n\2\2de\7k\2\2ef"+
		"\7m\2\2fg\7g\2\2g\32\3\2\2\2hi\7p\2\2ij\7q\2\2jk\7v\2\2kl\7\"\2\2lm\7"+
		"n\2\2mn\7k\2\2no\7m\2\2op\7g\2\2p\34\3\2\2\2qr\7c\2\2rs\7p\2\2st\7f\2"+
		"\2t\36\3\2\2\2uv\7q\2\2vw\7t\2\2w \3\2\2\2xy\7*\2\2y\"\3\2\2\2z{\7+\2"+
		"\2{$\3\2\2\2|}\7s\2\2}~\7w\2\2~\177\7g\2\2\177\u0080\7t\2\2\u0080\u0081"+
		"\7{\2\2\u0081&\3\2\2\2\u0082\u0083\7y\2\2\u0083\u0084\7j\2\2\u0084\u0085"+
		"\7g\2\2\u0085\u0086\7t\2\2\u0086\u0087\7g\2\2\u0087(\3\2\2\2\u0088\u008a"+
		"\7/\2\2\u0089\u0088\3\2\2\2\u0089\u008a\3\2\2\2\u008a\u008b\3\2\2\2\u008b"+
		"\u008c\5\65\33\2\u008c*\3\2\2\2\u008d\u008e\5)\25\2\u008e\u0090\7\60\2"+
		"\2\u008f\u0091\5\65\33\2\u0090\u008f\3\2\2\2\u0091\u0092\3\2\2\2\u0092"+
		"\u0090\3\2\2\2\u0092\u0093\3\2\2\2\u0093,\3\2\2\2\u0094\u0096\t\2\2\2"+
		"\u0095\u0094\3\2\2\2\u0096\u0097\3\2\2\2\u0097\u0095\3\2\2\2\u0097\u0098"+
		"\3\2\2\2\u0098.\3\2\2\2\u0099\u009b\t\3\2\2\u009a\u0099\3\2\2\2\u009b"+
		"\u009c\3\2\2\2\u009c\u009a\3\2\2\2\u009c\u009d\3\2\2\2\u009d\u009e\3\2"+
		"\2\2\u009e\u009f\b\30\2\2\u009f\60\3\2\2\2\u00a0\u00a2\7$\2\2\u00a1\u00a3"+
		"\n\4\2\2\u00a2\u00a1\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a2\3\2\2\2\u00a4"+
		"\u00a5\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00b3\7$\2\2\u00a7\u00a9\7)\2"+
		"\2\u00a8\u00a7\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\u00a8\3\2\2\2\u00aa\u00ab"+
		"\3\2\2\2\u00ab\u00ad\3\2\2\2\u00ac\u00ae\n\5\2\2\u00ad\u00ac\3\2\2\2\u00ae"+
		"\u00af\3\2\2\2\u00af\u00ad\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0\u00b1\3\2"+
		"\2\2\u00b1\u00b3\7)\2\2\u00b2\u00a0\3\2\2\2\u00b2\u00a8\3\2\2\2\u00b3"+
		"\62\3\2\2\2\u00b4\u00b6\4c|\2\u00b5\u00b4\3\2\2\2\u00b6\u00b7\3\2\2\2"+
		"\u00b7\u00b5\3\2\2\2\u00b7\u00b8\3\2\2\2\u00b8\u00bf\3\2\2\2\u00b9\u00bb"+
		"\4C\\\2\u00ba\u00b9\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc\u00ba\3\2\2\2\u00bc"+
		"\u00bd\3\2\2\2\u00bd\u00bf\3\2\2\2\u00be\u00b5\3\2\2\2\u00be\u00ba\3\2"+
		"\2\2\u00bf\64\3\2\2\2\u00c0\u00c2\4\62;\2\u00c1\u00c0\3\2\2\2\u00c2\u00c3"+
		"\3\2\2\2\u00c3\u00c1\3\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\66\3\2\2\2\17\2"+
		"\u0089\u0092\u0097\u009c\u00a4\u00aa\u00af\u00b2\u00b7\u00bc\u00be\u00c3"+
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