// Generated from ZQL.g4 by ANTLR 4.8

package org.zstack.zql.antlr4;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ZQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, FILTER_BY=24, 
		OFFSET=25, LIMIT=26, QUERY=27, GET=28, COUNT=29, SUM=30, SEARCH=31, DISTINCT=32, 
		ORDER_BY=33, GROUP_BY=34, NAMED_AS=35, ORDER_BY_VALUE=36, RESTRICT_BY=37, 
		RETURN_WITH=38, WHERE=39, FROM=40, AND=41, OR=42, ASC=43, DESC=44, INPUT=45, 
		OUTPUT=46, BOOLEAN=47, INT=48, FLOAT=49, ID=50, WS=51, STRING=52;
	public static final int
		RULE_zqls = 0, RULE_zql = 1, RULE_entity = 2, RULE_field = 3, RULE_multiFields = 4, 
		RULE_operator = 5, RULE_value = 6, RULE_listValue = 7, RULE_logicalOperator = 8, 
		RULE_complexValue = 9, RULE_getQuery = 10, RULE_apiparams = 11, RULE_input = 12, 
		RULE_output = 13, RULE_expr = 14, RULE_equal = 15, RULE_condition = 16, 
		RULE_queryTarget = 17, RULE_function = 18, RULE_queryTargetWithFunction = 19, 
		RULE_orderByExpr = 20, RULE_orderBy = 21, RULE_limit = 22, RULE_offset = 23, 
		RULE_restrictByExpr = 24, RULE_restrictBy = 25, RULE_returnWithExprBlock = 26, 
		RULE_returnWithExpr = 27, RULE_returnWith = 28, RULE_groupByExpr = 29, 
		RULE_groupBy = 30, RULE_subQueryTarget = 31, RULE_subQuery = 32, RULE_filterByExprBlock = 33, 
		RULE_filterByExpr = 34, RULE_filterBy = 35, RULE_namedAsKey = 36, RULE_namedAsValue = 37, 
		RULE_namedAs = 38, RULE_query = 39, RULE_count = 40, RULE_sumByValue = 41, 
		RULE_sumBy = 42, RULE_sum = 43, RULE_search = 44, RULE_keyword = 45, RULE_index = 46;
	private static String[] makeRuleNames() {
		return new String[] {
			"zqls", "zql", "entity", "field", "multiFields", "operator", "value", 
			"listValue", "logicalOperator", "complexValue", "getQuery", "apiparams", 
			"input", "output", "expr", "equal", "condition", "queryTarget", "function", 
			"queryTargetWithFunction", "orderByExpr", "orderBy", "limit", "offset", 
			"restrictByExpr", "restrictBy", "returnWithExprBlock", "returnWithExpr", 
			"returnWith", "groupByExpr", "groupBy", "subQueryTarget", "subQuery", 
			"filterByExprBlock", "filterByExpr", "filterBy", "namedAsKey", "namedAsValue", 
			"namedAs", "query", "count", "sumByValue", "sumBy", "sum", "search", 
			"keyword", "index"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", 
			"'is null'", "'is not null'", "'in'", "'not in'", "'like'", "'not like'", 
			"'has'", "'not has'", "'('", "')'", "'list('", "'{'", "'}'", "'by'", 
			"'filter by'", "'offset'", "'limit'", "'query'", "'getapi'", "'count'", 
			"'sum'", "'search'", "'distinct'", "'order by'", "'group by'", "'named as'", 
			null, "'restrict by'", "'return with'", "'where'", "'from'", "'and'", 
			"'or'", "'asc'", "'desc'", "'api'", "'output'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"FILTER_BY", "OFFSET", "LIMIT", "QUERY", "GET", "COUNT", "SUM", "SEARCH", 
			"DISTINCT", "ORDER_BY", "GROUP_BY", "NAMED_AS", "ORDER_BY_VALUE", "RESTRICT_BY", 
			"RETURN_WITH", "WHERE", "FROM", "AND", "OR", "ASC", "DESC", "INPUT", 
			"OUTPUT", "BOOLEAN", "INT", "FLOAT", "ID", "WS", "STRING"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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

	@Override
	public String getGrammarFileName() { return "ZQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public ZQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ZqlsContext extends ParserRuleContext {
		public List<ZqlContext> zql() {
			return getRuleContexts(ZqlContext.class);
		}
		public ZqlContext zql(int i) {
			return getRuleContext(ZqlContext.class,i);
		}
		public TerminalNode EOF() { return getToken(ZQLParser.EOF, 0); }
		public ZqlsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_zqls; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterZqls(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitZqls(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitZqls(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ZqlsContext zqls() throws RecognitionException {
		ZqlsContext _localctx = new ZqlsContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_zqls);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(94);
			zql();
			setState(99);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(95);
				match(T__0);
				setState(96);
				zql();
				}
				}
				setState(101);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(102);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ZqlContext extends ParserRuleContext {
		public ZqlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_zql; }
	 
		public ZqlContext() { }
		public void copyFrom(ZqlContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class QueryGrammarContext extends ZqlContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public QueryGrammarContext(ZqlContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterQueryGrammar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitQueryGrammar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitQueryGrammar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CountGrammarContext extends ZqlContext {
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public CountGrammarContext(ZqlContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterCountGrammar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitCountGrammar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitCountGrammar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SearchGrammarContext extends ZqlContext {
		public SearchContext search() {
			return getRuleContext(SearchContext.class,0);
		}
		public SearchGrammarContext(ZqlContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSearchGrammar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSearchGrammar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSearchGrammar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SumGrammarContext extends ZqlContext {
		public SumContext sum() {
			return getRuleContext(SumContext.class,0);
		}
		public SumGrammarContext(ZqlContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSumGrammar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSumGrammar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSumGrammar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ZqlContext zql() throws RecognitionException {
		ZqlContext _localctx = new ZqlContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_zql);
		try {
			setState(108);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QUERY:
				_localctx = new QueryGrammarContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(104);
				query();
				}
				break;
			case COUNT:
				_localctx = new CountGrammarContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(105);
				count();
				}
				break;
			case SUM:
				_localctx = new SumGrammarContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(106);
				sum();
				}
				break;
			case SEARCH:
				_localctx = new SearchGrammarContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(107);
				search();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntityContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public EntityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterEntity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitEntity(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitEntity(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityContext entity() throws RecognitionException {
		EntityContext _localctx = new EntityContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_entity);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(ZQLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(ZQLParser.ID, i);
		}
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_field);
		int _la;
		try {
			setState(120);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(112);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				match(ID);
				setState(116); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(114);
					match(T__1);
					setState(115);
					match(ID);
					}
					}
					setState(118); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__1 );
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiFieldsContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(ZQLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(ZQLParser.ID, i);
		}
		public MultiFieldsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiFields; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterMultiFields(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitMultiFields(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitMultiFields(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiFieldsContext multiFields() throws RecognitionException {
		MultiFieldsContext _localctx = new MultiFieldsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_multiFields);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			match(ID);
			setState(125); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(123);
				match(T__2);
				setState(124);
				match(ID);
				}
				}
				setState(127); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__2 );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperatorContext extends ParserRuleContext {
		public OperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OperatorContext operator() throws RecognitionException {
		OperatorContext _localctx = new OperatorContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(129);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(ZQLParser.STRING, 0); }
		public TerminalNode INT() { return getToken(ZQLParser.INT, 0); }
		public TerminalNode FLOAT() { return getToken(ZQLParser.FLOAT, 0); }
		public TerminalNode BOOLEAN() { return getToken(ZQLParser.BOOLEAN, 0); }
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_value);
		int _la;
		try {
			setState(146);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(131);
				match(STRING);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 2);
				{
				setState(132);
				match(INT);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(133);
				match(FLOAT);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(134);
				match(BOOLEAN);
				}
				break;
			case T__17:
				enterOuterAlt(_localctx, 5);
				{
				setState(135);
				match(T__17);
				setState(136);
				value();
				setState(141);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(137);
					match(T__2);
					setState(138);
					value();
					}
					}
					setState(143);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(144);
				match(T__18);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ListValueContext extends ParserRuleContext {
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public ListValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterListValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitListValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitListValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListValueContext listValue() throws RecognitionException {
		ListValueContext _localctx = new ListValueContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_listValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			match(T__19);
			setState(149);
			value();
			setState(154);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(150);
				match(T__2);
				setState(151);
				value();
				}
				}
				setState(156);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(157);
			match(T__18);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LogicalOperatorContext extends ParserRuleContext {
		public TerminalNode AND() { return getToken(ZQLParser.AND, 0); }
		public TerminalNode OR() { return getToken(ZQLParser.OR, 0); }
		public LogicalOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_logicalOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterLogicalOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitLogicalOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitLogicalOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LogicalOperatorContext logicalOperator() throws RecognitionException {
		LogicalOperatorContext _localctx = new LogicalOperatorContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_logicalOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(159);
			_la = _input.LA(1);
			if ( !(_la==AND || _la==OR) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ComplexValueContext extends ParserRuleContext {
		public ComplexValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_complexValue; }
	 
		public ComplexValueContext() { }
		public void copyFrom(ComplexValueContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SubQueryValueContext extends ComplexValueContext {
		public SubQueryContext subQuery() {
			return getRuleContext(SubQueryContext.class,0);
		}
		public SubQueryValueContext(ComplexValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSubQueryValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSubQueryValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSubQueryValue(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SimpleValueContext extends ComplexValueContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public SimpleValueContext(ComplexValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSimpleValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSimpleValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSimpleValue(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ApiGetValueContext extends ComplexValueContext {
		public GetQueryContext getQuery() {
			return getRuleContext(GetQueryContext.class,0);
		}
		public InputContext input() {
			return getRuleContext(InputContext.class,0);
		}
		public OutputContext output() {
			return getRuleContext(OutputContext.class,0);
		}
		public List<ApiparamsContext> apiparams() {
			return getRuleContexts(ApiparamsContext.class);
		}
		public ApiparamsContext apiparams(int i) {
			return getRuleContext(ApiparamsContext.class,i);
		}
		public ApiGetValueContext(ComplexValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterApiGetValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitApiGetValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitApiGetValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComplexValueContext complexValue() throws RecognitionException {
		ComplexValueContext _localctx = new ComplexValueContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_complexValue);
		int _la;
		try {
			setState(204);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				_localctx = new SimpleValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(161);
				value();
				}
				break;
			case 2:
				_localctx = new SubQueryValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(162);
				match(T__17);
				setState(163);
				subQuery();
				setState(164);
				match(T__18);
				}
				break;
			case 3:
				_localctx = new ApiGetValueContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(167);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(166);
					match(T__17);
					}
				}

				setState(169);
				getQuery();
				setState(170);
				match(T__17);
				setState(171);
				input();
				setState(172);
				match(T__2);
				setState(173);
				output();
				setState(178);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(174);
					match(T__2);
					setState(175);
					apiparams();
					}
					}
					setState(180);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(181);
				match(T__18);
				setState(183);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(182);
					match(T__18);
					}
					break;
				}
				}
				break;
			case 4:
				_localctx = new ApiGetValueContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(186);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(185);
					match(T__17);
					}
				}

				setState(188);
				getQuery();
				setState(189);
				match(T__17);
				setState(190);
				output();
				setState(191);
				match(T__2);
				setState(192);
				input();
				setState(197);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(193);
					match(T__2);
					setState(194);
					apiparams();
					}
					}
					setState(199);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(200);
				match(T__18);
				setState(202);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(201);
					match(T__18);
					}
					break;
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GetQueryContext extends ParserRuleContext {
		public TerminalNode GET() { return getToken(ZQLParser.GET, 0); }
		public GetQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_getQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterGetQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitGetQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitGetQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GetQueryContext getQuery() throws RecognitionException {
		GetQueryContext _localctx = new GetQueryContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_getQuery);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			match(GET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ApiparamsContext extends ParserRuleContext {
		public NamedAsKeyContext namedAsKey() {
			return getRuleContext(NamedAsKeyContext.class,0);
		}
		public EqualContext equal() {
			return getRuleContext(EqualContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ListValueContext listValue() {
			return getRuleContext(ListValueContext.class,0);
		}
		public ApiparamsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_apiparams; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterApiparams(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitApiparams(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitApiparams(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ApiparamsContext apiparams() throws RecognitionException {
		ApiparamsContext _localctx = new ApiparamsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_apiparams);
		try {
			setState(216);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(208);
				namedAsKey();
				setState(209);
				equal();
				setState(210);
				value();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(212);
				namedAsKey();
				setState(213);
				equal();
				setState(214);
				listValue();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InputContext extends ParserRuleContext {
		public TerminalNode INPUT() { return getToken(ZQLParser.INPUT, 0); }
		public EqualContext equal() {
			return getRuleContext(EqualContext.class,0);
		}
		public NamedAsValueContext namedAsValue() {
			return getRuleContext(NamedAsValueContext.class,0);
		}
		public InputContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_input; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterInput(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitInput(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitInput(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputContext input() throws RecognitionException {
		InputContext _localctx = new InputContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_input);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(218);
			match(INPUT);
			setState(219);
			equal();
			setState(220);
			namedAsValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OutputContext extends ParserRuleContext {
		public TerminalNode OUTPUT() { return getToken(ZQLParser.OUTPUT, 0); }
		public EqualContext equal() {
			return getRuleContext(EqualContext.class,0);
		}
		public NamedAsValueContext namedAsValue() {
			return getRuleContext(NamedAsValueContext.class,0);
		}
		public OutputContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_output; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterOutput(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitOutput(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitOutput(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OutputContext output() throws RecognitionException {
		OutputContext _localctx = new OutputContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_output);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(222);
			match(OUTPUT);
			setState(223);
			equal();
			setState(224);
			namedAsValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public FieldContext field() {
			return getRuleContext(FieldContext.class,0);
		}
		public OperatorContext operator() {
			return getRuleContext(OperatorContext.class,0);
		}
		public ComplexValueContext complexValue() {
			return getRuleContext(ComplexValueContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			field();
			setState(227);
			operator();
			setState(229);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(228);
				complexValue();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EqualContext extends ParserRuleContext {
		public EqualContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitEqual(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EqualContext equal() throws RecognitionException {
		EqualContext _localctx = new EqualContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_equal);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(231);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConditionContext extends ParserRuleContext {
		public ConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_condition; }
	 
		public ConditionContext() { }
		public void copyFrom(ConditionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NestConditionContext extends ConditionContext {
		public ConditionContext left;
		public LogicalOperatorContext op;
		public ConditionContext right;
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public List<LogicalOperatorContext> logicalOperator() {
			return getRuleContexts(LogicalOperatorContext.class);
		}
		public LogicalOperatorContext logicalOperator(int i) {
			return getRuleContext(LogicalOperatorContext.class,i);
		}
		public NestConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterNestCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitNestCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitNestCondition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SimpleConditionContext extends ConditionContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SimpleConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSimpleCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSimpleCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSimpleCondition(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParenthesisConditionContext extends ConditionContext {
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public ParenthesisConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterParenthesisCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitParenthesisCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitParenthesisCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConditionContext condition() throws RecognitionException {
		return condition(0);
	}

	private ConditionContext condition(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ConditionContext _localctx = new ConditionContext(_ctx, _parentState);
		ConditionContext _prevctx = _localctx;
		int _startState = 32;
		enterRecursionRule(_localctx, 32, RULE_condition, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(239);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__17:
				{
				_localctx = new ParenthesisConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(234);
				match(T__17);
				setState(235);
				condition(0);
				setState(236);
				match(T__18);
				}
				break;
			case ID:
				{
				_localctx = new SimpleConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(238);
				expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(251);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new NestConditionContext(new ConditionContext(_parentctx, _parentState));
					((NestConditionContext)_localctx).left = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_condition);
					setState(241);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(245); 
					_errHandler.sync(this);
					_alt = 1;
					do {
						switch (_alt) {
						case 1:
							{
							{
							setState(242);
							((NestConditionContext)_localctx).op = logicalOperator();
							setState(243);
							((NestConditionContext)_localctx).right = condition(0);
							}
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						setState(247); 
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
					} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
					}
					} 
				}
				setState(253);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class QueryTargetContext extends ParserRuleContext {
		public QueryTargetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryTarget; }
	 
		public QueryTargetContext() { }
		public void copyFrom(QueryTargetContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class WithSingleFieldContext extends QueryTargetContext {
		public EntityContext entity() {
			return getRuleContext(EntityContext.class,0);
		}
		public FieldContext field() {
			return getRuleContext(FieldContext.class,0);
		}
		public WithSingleFieldContext(QueryTargetContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterWithSingleField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitWithSingleField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitWithSingleField(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WithMultiFieldsContext extends QueryTargetContext {
		public EntityContext entity() {
			return getRuleContext(EntityContext.class,0);
		}
		public MultiFieldsContext multiFields() {
			return getRuleContext(MultiFieldsContext.class,0);
		}
		public WithMultiFieldsContext(QueryTargetContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterWithMultiFields(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitWithMultiFields(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitWithMultiFields(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OnlyEntityContext extends QueryTargetContext {
		public EntityContext entity() {
			return getRuleContext(EntityContext.class,0);
		}
		public OnlyEntityContext(QueryTargetContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterOnlyEntity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitOnlyEntity(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitOnlyEntity(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryTargetContext queryTarget() throws RecognitionException {
		QueryTargetContext _localctx = new QueryTargetContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_queryTarget);
		try {
			setState(263);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				_localctx = new OnlyEntityContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(254);
				entity();
				}
				break;
			case 2:
				_localctx = new WithSingleFieldContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(255);
				entity();
				setState(256);
				match(T__1);
				setState(257);
				field();
				}
				break;
			case 3:
				_localctx = new WithMultiFieldsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(259);
				entity();
				setState(260);
				match(T__1);
				setState(261);
				multiFields();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionContext extends ParserRuleContext {
		public TerminalNode DISTINCT() { return getToken(ZQLParser.DISTINCT, 0); }
		public FunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionContext function() throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_function);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			match(DISTINCT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QueryTargetWithFunctionContext extends ParserRuleContext {
		public QueryTargetWithFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_queryTargetWithFunction; }
	 
		public QueryTargetWithFunctionContext() { }
		public void copyFrom(QueryTargetWithFunctionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class WithFunctionContext extends QueryTargetWithFunctionContext {
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public QueryTargetWithFunctionContext queryTargetWithFunction() {
			return getRuleContext(QueryTargetWithFunctionContext.class,0);
		}
		public WithFunctionContext(QueryTargetWithFunctionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterWithFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitWithFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitWithFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WithoutFunctionContext extends QueryTargetWithFunctionContext {
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public WithoutFunctionContext(QueryTargetWithFunctionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterWithoutFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitWithoutFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitWithoutFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryTargetWithFunctionContext queryTargetWithFunction() throws RecognitionException {
		QueryTargetWithFunctionContext _localctx = new QueryTargetWithFunctionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_queryTargetWithFunction);
		try {
			setState(273);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				_localctx = new WithoutFunctionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(267);
				queryTarget();
				}
				break;
			case DISTINCT:
				_localctx = new WithFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(268);
				function();
				setState(269);
				match(T__17);
				setState(270);
				queryTargetWithFunction();
				setState(271);
				match(T__18);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderByExprContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public TerminalNode ORDER_BY_VALUE() { return getToken(ZQLParser.ORDER_BY_VALUE, 0); }
		public OrderByExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderByExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterOrderByExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitOrderByExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitOrderByExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderByExprContext orderByExpr() throws RecognitionException {
		OrderByExprContext _localctx = new OrderByExprContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_orderByExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
			match(ID);
			setState(276);
			match(ORDER_BY_VALUE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderByContext extends ParserRuleContext {
		public TerminalNode ORDER_BY() { return getToken(ZQLParser.ORDER_BY, 0); }
		public List<OrderByExprContext> orderByExpr() {
			return getRuleContexts(OrderByExprContext.class);
		}
		public OrderByExprContext orderByExpr(int i) {
			return getRuleContext(OrderByExprContext.class,i);
		}
		public OrderByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterOrderBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitOrderBy(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitOrderBy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderByContext orderBy() throws RecognitionException {
		OrderByContext _localctx = new OrderByContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_orderBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
			match(ORDER_BY);
			setState(279);
			orderByExpr();
			setState(284);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(280);
				match(T__2);
				setState(281);
				orderByExpr();
				}
				}
				setState(286);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LimitContext extends ParserRuleContext {
		public TerminalNode LIMIT() { return getToken(ZQLParser.LIMIT, 0); }
		public TerminalNode INT() { return getToken(ZQLParser.INT, 0); }
		public LimitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_limit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterLimit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitLimit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitLimit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LimitContext limit() throws RecognitionException {
		LimitContext _localctx = new LimitContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(287);
			match(LIMIT);
			setState(288);
			match(INT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OffsetContext extends ParserRuleContext {
		public TerminalNode OFFSET() { return getToken(ZQLParser.OFFSET, 0); }
		public TerminalNode INT() { return getToken(ZQLParser.INT, 0); }
		public OffsetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_offset; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterOffset(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitOffset(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitOffset(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OffsetContext offset() throws RecognitionException {
		OffsetContext _localctx = new OffsetContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_offset);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(290);
			match(OFFSET);
			setState(291);
			match(INT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RestrictByExprContext extends ParserRuleContext {
		public EntityContext entity() {
			return getRuleContext(EntityContext.class,0);
		}
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public OperatorContext operator() {
			return getRuleContext(OperatorContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public RestrictByExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_restrictByExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterRestrictByExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitRestrictByExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitRestrictByExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RestrictByExprContext restrictByExpr() throws RecognitionException {
		RestrictByExprContext _localctx = new RestrictByExprContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_restrictByExpr);
		int _la;
		try {
			setState(305);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(293);
				entity();
				setState(294);
				match(T__1);
				setState(295);
				match(ID);
				setState(296);
				operator();
				setState(298);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__17) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(297);
					value();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(300);
				match(ID);
				setState(301);
				operator();
				setState(303);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__17) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(302);
					value();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RestrictByContext extends ParserRuleContext {
		public TerminalNode RESTRICT_BY() { return getToken(ZQLParser.RESTRICT_BY, 0); }
		public List<RestrictByExprContext> restrictByExpr() {
			return getRuleContexts(RestrictByExprContext.class);
		}
		public RestrictByExprContext restrictByExpr(int i) {
			return getRuleContext(RestrictByExprContext.class,i);
		}
		public RestrictByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_restrictBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterRestrictBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitRestrictBy(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitRestrictBy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RestrictByContext restrictBy() throws RecognitionException {
		RestrictByContext _localctx = new RestrictByContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_restrictBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(307);
			match(RESTRICT_BY);
			setState(308);
			match(T__17);
			setState(309);
			restrictByExpr();
			setState(314);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(310);
				match(T__2);
				setState(311);
				restrictByExpr();
				}
				}
				setState(316);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(317);
			match(T__18);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReturnWithExprBlockContext extends ParserRuleContext {
		public List<ReturnWithExprBlockContext> returnWithExprBlock() {
			return getRuleContexts(ReturnWithExprBlockContext.class);
		}
		public ReturnWithExprBlockContext returnWithExprBlock(int i) {
			return getRuleContext(ReturnWithExprBlockContext.class,i);
		}
		public ReturnWithExprBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnWithExprBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterReturnWithExprBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitReturnWithExprBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitReturnWithExprBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReturnWithExprBlockContext returnWithExprBlock() throws RecognitionException {
		ReturnWithExprBlockContext _localctx = new ReturnWithExprBlockContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_returnWithExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(319);
			match(T__20);
			setState(324);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__22) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << GET) | (1L << COUNT) | (1L << SUM) | (1L << SEARCH) | (1L << DISTINCT) | (1L << ORDER_BY) | (1L << GROUP_BY) | (1L << NAMED_AS) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << FROM) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << INPUT) | (1L << OUTPUT) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(322);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
				case 1:
					{
					setState(320);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==T__21) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				case 2:
					{
					setState(321);
					returnWithExprBlock();
					}
					break;
				}
				}
				setState(326);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(327);
			match(T__21);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReturnWithExprContext extends ParserRuleContext {
		public ReturnWithExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnWithExpr; }
	 
		public ReturnWithExprContext() { }
		public void copyFrom(ReturnWithExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ReturnWithExprFunctionContext extends ReturnWithExprContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public ReturnWithExprBlockContext returnWithExprBlock() {
			return getRuleContext(ReturnWithExprBlockContext.class,0);
		}
		public ReturnWithExprFunctionContext(ReturnWithExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterReturnWithExprFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitReturnWithExprFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitReturnWithExprFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ReturnWithExprIdContext extends ReturnWithExprContext {
		public List<TerminalNode> ID() { return getTokens(ZQLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(ZQLParser.ID, i);
		}
		public ReturnWithExprIdContext(ReturnWithExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterReturnWithExprId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitReturnWithExprId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitReturnWithExprId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReturnWithExprContext returnWithExpr() throws RecognitionException {
		ReturnWithExprContext _localctx = new ReturnWithExprContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_returnWithExpr);
		int _la;
		try {
			setState(339);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				_localctx = new ReturnWithExprIdContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(329);
				match(ID);
				setState(334);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(330);
					match(T__1);
					setState(331);
					match(ID);
					}
					}
					setState(336);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				_localctx = new ReturnWithExprFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(337);
				match(ID);
				setState(338);
				returnWithExprBlock();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReturnWithContext extends ParserRuleContext {
		public TerminalNode RETURN_WITH() { return getToken(ZQLParser.RETURN_WITH, 0); }
		public List<ReturnWithExprContext> returnWithExpr() {
			return getRuleContexts(ReturnWithExprContext.class);
		}
		public ReturnWithExprContext returnWithExpr(int i) {
			return getRuleContext(ReturnWithExprContext.class,i);
		}
		public ReturnWithContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnWith; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterReturnWith(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitReturnWith(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitReturnWith(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReturnWithContext returnWith() throws RecognitionException {
		ReturnWithContext _localctx = new ReturnWithContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_returnWith);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(341);
			match(RETURN_WITH);
			setState(342);
			match(T__17);
			setState(343);
			returnWithExpr();
			setState(348);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(344);
				match(T__2);
				setState(345);
				returnWithExpr();
				}
				}
				setState(350);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(351);
			match(T__18);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupByExprContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(ZQLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(ZQLParser.ID, i);
		}
		public GroupByExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupByExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterGroupByExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitGroupByExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitGroupByExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupByExprContext groupByExpr() throws RecognitionException {
		GroupByExprContext _localctx = new GroupByExprContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_groupByExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(353);
			match(ID);
			setState(358);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(354);
				match(T__2);
				setState(355);
				match(ID);
				}
				}
				setState(360);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupByContext extends ParserRuleContext {
		public TerminalNode GROUP_BY() { return getToken(ZQLParser.GROUP_BY, 0); }
		public GroupByExprContext groupByExpr() {
			return getRuleContext(GroupByExprContext.class,0);
		}
		public GroupByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterGroupBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitGroupBy(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitGroupBy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupByContext groupBy() throws RecognitionException {
		GroupByContext _localctx = new GroupByContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_groupBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(361);
			match(GROUP_BY);
			setState(362);
			groupByExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubQueryTargetContext extends ParserRuleContext {
		public EntityContext entity() {
			return getRuleContext(EntityContext.class,0);
		}
		public List<TerminalNode> ID() { return getTokens(ZQLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(ZQLParser.ID, i);
		}
		public SubQueryTargetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subQueryTarget; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSubQueryTarget(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSubQueryTarget(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSubQueryTarget(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubQueryTargetContext subQueryTarget() throws RecognitionException {
		SubQueryTargetContext _localctx = new SubQueryTargetContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_subQueryTarget);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(364);
			entity();
			setState(367); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(365);
				match(T__1);
				setState(366);
				match(ID);
				}
				}
				setState(369); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__1 );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubQueryContext extends ParserRuleContext {
		public TerminalNode QUERY() { return getToken(ZQLParser.QUERY, 0); }
		public SubQueryTargetContext subQueryTarget() {
			return getRuleContext(SubQueryTargetContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(ZQLParser.WHERE, 0); }
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public SubQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSubQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSubQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSubQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubQueryContext subQuery() throws RecognitionException {
		SubQueryContext _localctx = new SubQueryContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_subQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(371);
			match(QUERY);
			setState(372);
			subQueryTarget();
			setState(379);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(373);
				match(WHERE);
				setState(375); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(374);
					condition(0);
					}
					}
					setState(377); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterByExprBlockContext extends ParserRuleContext {
		public List<FilterByExprBlockContext> filterByExprBlock() {
			return getRuleContexts(FilterByExprBlockContext.class);
		}
		public FilterByExprBlockContext filterByExprBlock(int i) {
			return getRuleContext(FilterByExprBlockContext.class,i);
		}
		public FilterByExprBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterByExprBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterFilterByExprBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitFilterByExprBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitFilterByExprBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterByExprBlockContext filterByExprBlock() throws RecognitionException {
		FilterByExprBlockContext _localctx = new FilterByExprBlockContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_filterByExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(381);
			match(T__20);
			setState(386);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__22) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << GET) | (1L << COUNT) | (1L << SUM) | (1L << SEARCH) | (1L << DISTINCT) | (1L << ORDER_BY) | (1L << GROUP_BY) | (1L << NAMED_AS) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << FROM) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << INPUT) | (1L << OUTPUT) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(384);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
				case 1:
					{
					setState(382);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==T__21) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				case 2:
					{
					setState(383);
					filterByExprBlock();
					}
					break;
				}
				}
				setState(388);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(389);
			match(T__21);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterByExprContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public FilterByExprBlockContext filterByExprBlock() {
			return getRuleContext(FilterByExprBlockContext.class,0);
		}
		public FilterByExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterByExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterFilterByExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitFilterByExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitFilterByExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterByExprContext filterByExpr() throws RecognitionException {
		FilterByExprContext _localctx = new FilterByExprContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_filterByExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(391);
			match(ID);
			setState(392);
			filterByExprBlock();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterByContext extends ParserRuleContext {
		public TerminalNode FILTER_BY() { return getToken(ZQLParser.FILTER_BY, 0); }
		public List<FilterByExprContext> filterByExpr() {
			return getRuleContexts(FilterByExprContext.class);
		}
		public FilterByExprContext filterByExpr(int i) {
			return getRuleContext(FilterByExprContext.class,i);
		}
		public FilterByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterFilterBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitFilterBy(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitFilterBy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterByContext filterBy() throws RecognitionException {
		FilterByContext _localctx = new FilterByContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_filterBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(394);
			match(FILTER_BY);
			setState(395);
			filterByExpr();
			setState(400);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(396);
				match(T__2);
				setState(397);
				filterByExpr();
				}
				}
				setState(402);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamedAsKeyContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public NamedAsKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedAsKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterNamedAsKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitNamedAsKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitNamedAsKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedAsKeyContext namedAsKey() throws RecognitionException {
		NamedAsKeyContext _localctx = new NamedAsKeyContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_namedAsKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(403);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamedAsValueContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(ZQLParser.STRING, 0); }
		public NamedAsValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedAsValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterNamedAsValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitNamedAsValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitNamedAsValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedAsValueContext namedAsValue() throws RecognitionException {
		NamedAsValueContext _localctx = new NamedAsValueContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_namedAsValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NamedAsContext extends ParserRuleContext {
		public TerminalNode NAMED_AS() { return getToken(ZQLParser.NAMED_AS, 0); }
		public NamedAsValueContext namedAsValue() {
			return getRuleContext(NamedAsValueContext.class,0);
		}
		public NamedAsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedAs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterNamedAs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitNamedAs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitNamedAs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedAsContext namedAs() throws RecognitionException {
		NamedAsContext _localctx = new NamedAsContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_namedAs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(407);
			match(NAMED_AS);
			setState(408);
			namedAsValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QueryContext extends ParserRuleContext {
		public TerminalNode QUERY() { return getToken(ZQLParser.QUERY, 0); }
		public QueryTargetWithFunctionContext queryTargetWithFunction() {
			return getRuleContext(QueryTargetWithFunctionContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(ZQLParser.WHERE, 0); }
		public RestrictByContext restrictBy() {
			return getRuleContext(RestrictByContext.class,0);
		}
		public ReturnWithContext returnWith() {
			return getRuleContext(ReturnWithContext.class,0);
		}
		public GroupByContext groupBy() {
			return getRuleContext(GroupByContext.class,0);
		}
		public OrderByContext orderBy() {
			return getRuleContext(OrderByContext.class,0);
		}
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public OffsetContext offset() {
			return getRuleContext(OffsetContext.class,0);
		}
		public FilterByContext filterBy() {
			return getRuleContext(FilterByContext.class,0);
		}
		public NamedAsContext namedAs() {
			return getRuleContext(NamedAsContext.class,0);
		}
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(410);
			match(QUERY);
			setState(411);
			queryTargetWithFunction();
			setState(418);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(412);
				match(WHERE);
				setState(414); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(413);
					condition(0);
					}
					}
					setState(416); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(421);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(420);
				restrictBy();
				}
			}

			setState(424);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RETURN_WITH) {
				{
				setState(423);
				returnWith();
				}
			}

			setState(427);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP_BY) {
				{
				setState(426);
				groupBy();
				}
			}

			setState(430);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(429);
				orderBy();
				}
			}

			setState(433);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(432);
				limit();
				}
			}

			setState(436);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(435);
				offset();
				}
			}

			setState(439);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FILTER_BY) {
				{
				setState(438);
				filterBy();
				}
			}

			setState(442);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(441);
				namedAs();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CountContext extends ParserRuleContext {
		public TerminalNode COUNT() { return getToken(ZQLParser.COUNT, 0); }
		public QueryTargetWithFunctionContext queryTargetWithFunction() {
			return getRuleContext(QueryTargetWithFunctionContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(ZQLParser.WHERE, 0); }
		public RestrictByContext restrictBy() {
			return getRuleContext(RestrictByContext.class,0);
		}
		public GroupByContext groupBy() {
			return getRuleContext(GroupByContext.class,0);
		}
		public OrderByContext orderBy() {
			return getRuleContext(OrderByContext.class,0);
		}
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public OffsetContext offset() {
			return getRuleContext(OffsetContext.class,0);
		}
		public NamedAsContext namedAs() {
			return getRuleContext(NamedAsContext.class,0);
		}
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public CountContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_count; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterCount(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitCount(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitCount(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CountContext count() throws RecognitionException {
		CountContext _localctx = new CountContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_count);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(444);
			match(COUNT);
			setState(445);
			queryTargetWithFunction();
			setState(452);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(446);
				match(WHERE);
				setState(448); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(447);
					condition(0);
					}
					}
					setState(450); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(455);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(454);
				restrictBy();
				}
			}

			setState(458);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP_BY) {
				{
				setState(457);
				groupBy();
				}
			}

			setState(461);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(460);
				orderBy();
				}
			}

			setState(464);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(463);
				limit();
				}
			}

			setState(467);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(466);
				offset();
				}
			}

			setState(470);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(469);
				namedAs();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SumByValueContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public SumByValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sumByValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSumByValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSumByValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSumByValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SumByValueContext sumByValue() throws RecognitionException {
		SumByValueContext _localctx = new SumByValueContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_sumByValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(472);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SumByContext extends ParserRuleContext {
		public SumByValueContext sumByValue() {
			return getRuleContext(SumByValueContext.class,0);
		}
		public SumByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sumBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSumBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSumBy(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSumBy(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SumByContext sumBy() throws RecognitionException {
		SumByContext _localctx = new SumByContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_sumBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(474);
			match(T__22);
			setState(475);
			sumByValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SumContext extends ParserRuleContext {
		public TerminalNode SUM() { return getToken(ZQLParser.SUM, 0); }
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public SumByContext sumBy() {
			return getRuleContext(SumByContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(ZQLParser.WHERE, 0); }
		public OrderByContext orderBy() {
			return getRuleContext(OrderByContext.class,0);
		}
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public OffsetContext offset() {
			return getRuleContext(OffsetContext.class,0);
		}
		public NamedAsContext namedAs() {
			return getRuleContext(NamedAsContext.class,0);
		}
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public SumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sum; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSum(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSum(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SumContext sum() throws RecognitionException {
		SumContext _localctx = new SumContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_sum);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(477);
			match(SUM);
			setState(478);
			queryTarget();
			setState(479);
			sumBy();
			setState(486);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(480);
				match(WHERE);
				setState(482); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(481);
					condition(0);
					}
					}
					setState(484); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(489);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(488);
				orderBy();
				}
			}

			setState(492);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(491);
				limit();
				}
			}

			setState(495);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(494);
				offset();
				}
			}

			setState(498);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(497);
				namedAs();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SearchContext extends ParserRuleContext {
		public TerminalNode SEARCH() { return getToken(ZQLParser.SEARCH, 0); }
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
		}
		public TerminalNode FROM() { return getToken(ZQLParser.FROM, 0); }
		public IndexContext index() {
			return getRuleContext(IndexContext.class,0);
		}
		public RestrictByContext restrictBy() {
			return getRuleContext(RestrictByContext.class,0);
		}
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public OffsetContext offset() {
			return getRuleContext(OffsetContext.class,0);
		}
		public SearchContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_search; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSearch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSearch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSearch(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SearchContext search() throws RecognitionException {
		SearchContext _localctx = new SearchContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_search);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(500);
			match(SEARCH);
			setState(501);
			keyword();
			setState(504);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FROM) {
				{
				setState(502);
				match(FROM);
				setState(503);
				index();
				}
			}

			setState(507);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(506);
				restrictBy();
				}
			}

			setState(510);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(509);
				limit();
				}
			}

			setState(513);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(512);
				offset();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class KeywordContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(ZQLParser.STRING, 0); }
		public KeywordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyword; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterKeyword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitKeyword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitKeyword(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordContext keyword() throws RecognitionException {
		KeywordContext _localctx = new KeywordContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_keyword);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(515);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IndexContext extends ParserRuleContext {
		public IndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index; }
	 
		public IndexContext() { }
		public void copyFrom(IndexContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SingleIndexContext extends IndexContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public SingleIndexContext(IndexContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterSingleIndex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitSingleIndex(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitSingleIndex(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MultiIndexsContext extends IndexContext {
		public List<TerminalNode> ID() { return getTokens(ZQLParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(ZQLParser.ID, i);
		}
		public MultiIndexsContext(IndexContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterMultiIndexs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitMultiIndexs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitMultiIndexs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IndexContext index() throws RecognitionException {
		IndexContext _localctx = new IndexContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_index);
		int _la;
		try {
			setState(525);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
			case 1:
				_localctx = new SingleIndexContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(517);
				match(ID);
				}
				break;
			case 2:
				_localctx = new MultiIndexsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(518);
				match(ID);
				setState(521); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(519);
					match(T__2);
					setState(520);
					match(ID);
					}
					}
					setState(523); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__2 );
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 16:
			return condition_sempred((ConditionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean condition_sempred(ConditionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\66\u0212\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\3\2\3\2\3\2\7\2d\n\2\f\2\16\2g\13\2\3"+
		"\2\3\2\3\3\3\3\3\3\3\3\5\3o\n\3\3\4\3\4\3\5\3\5\3\5\3\5\6\5w\n\5\r\5\16"+
		"\5x\5\5{\n\5\3\6\3\6\3\6\6\6\u0080\n\6\r\6\16\6\u0081\3\7\3\7\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\7\b\u008e\n\b\f\b\16\b\u0091\13\b\3\b\3\b\5\b"+
		"\u0095\n\b\3\t\3\t\3\t\3\t\7\t\u009b\n\t\f\t\16\t\u009e\13\t\3\t\3\t\3"+
		"\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u00aa\n\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\7\13\u00b3\n\13\f\13\16\13\u00b6\13\13\3\13\3\13\5"+
		"\13\u00ba\n\13\3\13\5\13\u00bd\n\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\7\13\u00c6\n\13\f\13\16\13\u00c9\13\13\3\13\3\13\5\13\u00cd\n\13\5\13"+
		"\u00cf\n\13\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u00db\n\r\3\16"+
		"\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\20\3\20\3\20\5\20\u00e8\n\20\3\21"+
		"\3\21\3\22\3\22\3\22\3\22\3\22\3\22\5\22\u00f2\n\22\3\22\3\22\3\22\3\22"+
		"\6\22\u00f8\n\22\r\22\16\22\u00f9\7\22\u00fc\n\22\f\22\16\22\u00ff\13"+
		"\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\5\23\u010a\n\23\3\24"+
		"\3\24\3\25\3\25\3\25\3\25\3\25\3\25\5\25\u0114\n\25\3\26\3\26\3\26\3\27"+
		"\3\27\3\27\3\27\7\27\u011d\n\27\f\27\16\27\u0120\13\27\3\30\3\30\3\30"+
		"\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\5\32\u012d\n\32\3\32\3\32\3\32"+
		"\5\32\u0132\n\32\5\32\u0134\n\32\3\33\3\33\3\33\3\33\3\33\7\33\u013b\n"+
		"\33\f\33\16\33\u013e\13\33\3\33\3\33\3\34\3\34\3\34\7\34\u0145\n\34\f"+
		"\34\16\34\u0148\13\34\3\34\3\34\3\35\3\35\3\35\7\35\u014f\n\35\f\35\16"+
		"\35\u0152\13\35\3\35\3\35\5\35\u0156\n\35\3\36\3\36\3\36\3\36\3\36\7\36"+
		"\u015d\n\36\f\36\16\36\u0160\13\36\3\36\3\36\3\37\3\37\3\37\7\37\u0167"+
		"\n\37\f\37\16\37\u016a\13\37\3 \3 \3 \3!\3!\3!\6!\u0172\n!\r!\16!\u0173"+
		"\3\"\3\"\3\"\3\"\6\"\u017a\n\"\r\"\16\"\u017b\5\"\u017e\n\"\3#\3#\3#\7"+
		"#\u0183\n#\f#\16#\u0186\13#\3#\3#\3$\3$\3$\3%\3%\3%\3%\7%\u0191\n%\f%"+
		"\16%\u0194\13%\3&\3&\3\'\3\'\3(\3(\3(\3)\3)\3)\3)\6)\u01a1\n)\r)\16)\u01a2"+
		"\5)\u01a5\n)\3)\5)\u01a8\n)\3)\5)\u01ab\n)\3)\5)\u01ae\n)\3)\5)\u01b1"+
		"\n)\3)\5)\u01b4\n)\3)\5)\u01b7\n)\3)\5)\u01ba\n)\3)\5)\u01bd\n)\3*\3*"+
		"\3*\3*\6*\u01c3\n*\r*\16*\u01c4\5*\u01c7\n*\3*\5*\u01ca\n*\3*\5*\u01cd"+
		"\n*\3*\5*\u01d0\n*\3*\5*\u01d3\n*\3*\5*\u01d6\n*\3*\5*\u01d9\n*\3+\3+"+
		"\3,\3,\3,\3-\3-\3-\3-\3-\6-\u01e5\n-\r-\16-\u01e6\5-\u01e9\n-\3-\5-\u01ec"+
		"\n-\3-\5-\u01ef\n-\3-\5-\u01f2\n-\3-\5-\u01f5\n-\3.\3.\3.\3.\5.\u01fb"+
		"\n.\3.\5.\u01fe\n.\3.\5.\u0201\n.\3.\5.\u0204\n.\3/\3/\3\60\3\60\3\60"+
		"\3\60\6\60\u020c\n\60\r\60\16\60\u020d\5\60\u0210\n\60\3\60\2\3\"\61\2"+
		"\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJL"+
		"NPRTVXZ\\^\2\5\3\2\6\23\3\2+,\3\2\30\30\2\u022f\2`\3\2\2\2\4n\3\2\2\2"+
		"\6p\3\2\2\2\bz\3\2\2\2\n|\3\2\2\2\f\u0083\3\2\2\2\16\u0094\3\2\2\2\20"+
		"\u0096\3\2\2\2\22\u00a1\3\2\2\2\24\u00ce\3\2\2\2\26\u00d0\3\2\2\2\30\u00da"+
		"\3\2\2\2\32\u00dc\3\2\2\2\34\u00e0\3\2\2\2\36\u00e4\3\2\2\2 \u00e9\3\2"+
		"\2\2\"\u00f1\3\2\2\2$\u0109\3\2\2\2&\u010b\3\2\2\2(\u0113\3\2\2\2*\u0115"+
		"\3\2\2\2,\u0118\3\2\2\2.\u0121\3\2\2\2\60\u0124\3\2\2\2\62\u0133\3\2\2"+
		"\2\64\u0135\3\2\2\2\66\u0141\3\2\2\28\u0155\3\2\2\2:\u0157\3\2\2\2<\u0163"+
		"\3\2\2\2>\u016b\3\2\2\2@\u016e\3\2\2\2B\u0175\3\2\2\2D\u017f\3\2\2\2F"+
		"\u0189\3\2\2\2H\u018c\3\2\2\2J\u0195\3\2\2\2L\u0197\3\2\2\2N\u0199\3\2"+
		"\2\2P\u019c\3\2\2\2R\u01be\3\2\2\2T\u01da\3\2\2\2V\u01dc\3\2\2\2X\u01df"+
		"\3\2\2\2Z\u01f6\3\2\2\2\\\u0205\3\2\2\2^\u020f\3\2\2\2`e\5\4\3\2ab\7\3"+
		"\2\2bd\5\4\3\2ca\3\2\2\2dg\3\2\2\2ec\3\2\2\2ef\3\2\2\2fh\3\2\2\2ge\3\2"+
		"\2\2hi\7\2\2\3i\3\3\2\2\2jo\5P)\2ko\5R*\2lo\5X-\2mo\5Z.\2nj\3\2\2\2nk"+
		"\3\2\2\2nl\3\2\2\2nm\3\2\2\2o\5\3\2\2\2pq\7\64\2\2q\7\3\2\2\2r{\7\64\2"+
		"\2sv\7\64\2\2tu\7\4\2\2uw\7\64\2\2vt\3\2\2\2wx\3\2\2\2xv\3\2\2\2xy\3\2"+
		"\2\2y{\3\2\2\2zr\3\2\2\2zs\3\2\2\2{\t\3\2\2\2|\177\7\64\2\2}~\7\5\2\2"+
		"~\u0080\7\64\2\2\177}\3\2\2\2\u0080\u0081\3\2\2\2\u0081\177\3\2\2\2\u0081"+
		"\u0082\3\2\2\2\u0082\13\3\2\2\2\u0083\u0084\t\2\2\2\u0084\r\3\2\2\2\u0085"+
		"\u0095\7\66\2\2\u0086\u0095\7\62\2\2\u0087\u0095\7\63\2\2\u0088\u0095"+
		"\7\61\2\2\u0089\u008a\7\24\2\2\u008a\u008f\5\16\b\2\u008b\u008c\7\5\2"+
		"\2\u008c\u008e\5\16\b\2\u008d\u008b\3\2\2\2\u008e\u0091\3\2\2\2\u008f"+
		"\u008d\3\2\2\2\u008f\u0090\3\2\2\2\u0090\u0092\3\2\2\2\u0091\u008f\3\2"+
		"\2\2\u0092\u0093\7\25\2\2\u0093\u0095\3\2\2\2\u0094\u0085\3\2\2\2\u0094"+
		"\u0086\3\2\2\2\u0094\u0087\3\2\2\2\u0094\u0088\3\2\2\2\u0094\u0089\3\2"+
		"\2\2\u0095\17\3\2\2\2\u0096\u0097\7\26\2\2\u0097\u009c\5\16\b\2\u0098"+
		"\u0099\7\5\2\2\u0099\u009b\5\16\b\2\u009a\u0098\3\2\2\2\u009b\u009e\3"+
		"\2\2\2\u009c\u009a\3\2\2\2\u009c\u009d\3\2\2\2\u009d\u009f\3\2\2\2\u009e"+
		"\u009c\3\2\2\2\u009f\u00a0\7\25\2\2\u00a0\21\3\2\2\2\u00a1\u00a2\t\3\2"+
		"\2\u00a2\23\3\2\2\2\u00a3\u00cf\5\16\b\2\u00a4\u00a5\7\24\2\2\u00a5\u00a6"+
		"\5B\"\2\u00a6\u00a7\7\25\2\2\u00a7\u00cf\3\2\2\2\u00a8\u00aa\7\24\2\2"+
		"\u00a9\u00a8\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab\u00ac"+
		"\5\26\f\2\u00ac\u00ad\7\24\2\2\u00ad\u00ae\5\32\16\2\u00ae\u00af\7\5\2"+
		"\2\u00af\u00b4\5\34\17\2\u00b0\u00b1\7\5\2\2\u00b1\u00b3\5\30\r\2\u00b2"+
		"\u00b0\3\2\2\2\u00b3\u00b6\3\2\2\2\u00b4\u00b2\3\2\2\2\u00b4\u00b5\3\2"+
		"\2\2\u00b5\u00b7\3\2\2\2\u00b6\u00b4\3\2\2\2\u00b7\u00b9\7\25\2\2\u00b8"+
		"\u00ba\7\25\2\2\u00b9\u00b8\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba\u00cf\3"+
		"\2\2\2\u00bb\u00bd\7\24\2\2\u00bc\u00bb\3\2\2\2\u00bc\u00bd\3\2\2\2\u00bd"+
		"\u00be\3\2\2\2\u00be\u00bf\5\26\f\2\u00bf\u00c0\7\24\2\2\u00c0\u00c1\5"+
		"\34\17\2\u00c1\u00c2\7\5\2\2\u00c2\u00c7\5\32\16\2\u00c3\u00c4\7\5\2\2"+
		"\u00c4\u00c6\5\30\r\2\u00c5\u00c3\3\2\2\2\u00c6\u00c9\3\2\2\2\u00c7\u00c5"+
		"\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8\u00ca\3\2\2\2\u00c9\u00c7\3\2\2\2\u00ca"+
		"\u00cc\7\25\2\2\u00cb\u00cd\7\25\2\2\u00cc\u00cb\3\2\2\2\u00cc\u00cd\3"+
		"\2\2\2\u00cd\u00cf\3\2\2\2\u00ce\u00a3\3\2\2\2\u00ce\u00a4\3\2\2\2\u00ce"+
		"\u00a9\3\2\2\2\u00ce\u00bc\3\2\2\2\u00cf\25\3\2\2\2\u00d0\u00d1\7\36\2"+
		"\2\u00d1\27\3\2\2\2\u00d2\u00d3\5J&\2\u00d3\u00d4\5 \21\2\u00d4\u00d5"+
		"\5\16\b\2\u00d5\u00db\3\2\2\2\u00d6\u00d7\5J&\2\u00d7\u00d8\5 \21\2\u00d8"+
		"\u00d9\5\20\t\2\u00d9\u00db\3\2\2\2\u00da\u00d2\3\2\2\2\u00da\u00d6\3"+
		"\2\2\2\u00db\31\3\2\2\2\u00dc\u00dd\7/\2\2\u00dd\u00de\5 \21\2\u00de\u00df"+
		"\5L\'\2\u00df\33\3\2\2\2\u00e0\u00e1\7\60\2\2\u00e1\u00e2\5 \21\2\u00e2"+
		"\u00e3\5L\'\2\u00e3\35\3\2\2\2\u00e4\u00e5\5\b\5\2\u00e5\u00e7\5\f\7\2"+
		"\u00e6\u00e8\5\24\13\2\u00e7\u00e6\3\2\2\2\u00e7\u00e8\3\2\2\2\u00e8\37"+
		"\3\2\2\2\u00e9\u00ea\7\6\2\2\u00ea!\3\2\2\2\u00eb\u00ec\b\22\1\2\u00ec"+
		"\u00ed\7\24\2\2\u00ed\u00ee\5\"\22\2\u00ee\u00ef\7\25\2\2\u00ef\u00f2"+
		"\3\2\2\2\u00f0\u00f2\5\36\20\2\u00f1\u00eb\3\2\2\2\u00f1\u00f0\3\2\2\2"+
		"\u00f2\u00fd\3\2\2\2\u00f3\u00f7\f\4\2\2\u00f4\u00f5\5\22\n\2\u00f5\u00f6"+
		"\5\"\22\2\u00f6\u00f8\3\2\2\2\u00f7\u00f4\3\2\2\2\u00f8\u00f9\3\2\2\2"+
		"\u00f9\u00f7\3\2\2\2\u00f9\u00fa\3\2\2\2\u00fa\u00fc\3\2\2\2\u00fb\u00f3"+
		"\3\2\2\2\u00fc\u00ff\3\2\2\2\u00fd\u00fb\3\2\2\2\u00fd\u00fe\3\2\2\2\u00fe"+
		"#\3\2\2\2\u00ff\u00fd\3\2\2\2\u0100\u010a\5\6\4\2\u0101\u0102\5\6\4\2"+
		"\u0102\u0103\7\4\2\2\u0103\u0104\5\b\5\2\u0104\u010a\3\2\2\2\u0105\u0106"+
		"\5\6\4\2\u0106\u0107\7\4\2\2\u0107\u0108\5\n\6\2\u0108\u010a\3\2\2\2\u0109"+
		"\u0100\3\2\2\2\u0109\u0101\3\2\2\2\u0109\u0105\3\2\2\2\u010a%\3\2\2\2"+
		"\u010b\u010c\7\"\2\2\u010c\'\3\2\2\2\u010d\u0114\5$\23\2\u010e\u010f\5"+
		"&\24\2\u010f\u0110\7\24\2\2\u0110\u0111\5(\25\2\u0111\u0112\7\25\2\2\u0112"+
		"\u0114\3\2\2\2\u0113\u010d\3\2\2\2\u0113\u010e\3\2\2\2\u0114)\3\2\2\2"+
		"\u0115\u0116\7\64\2\2\u0116\u0117\7&\2\2\u0117+\3\2\2\2\u0118\u0119\7"+
		"#\2\2\u0119\u011e\5*\26\2\u011a\u011b\7\5\2\2\u011b\u011d\5*\26\2\u011c"+
		"\u011a\3\2\2\2\u011d\u0120\3\2\2\2\u011e\u011c\3\2\2\2\u011e\u011f\3\2"+
		"\2\2\u011f-\3\2\2\2\u0120\u011e\3\2\2\2\u0121\u0122\7\34\2\2\u0122\u0123"+
		"\7\62\2\2\u0123/\3\2\2\2\u0124\u0125\7\33\2\2\u0125\u0126\7\62\2\2\u0126"+
		"\61\3\2\2\2\u0127\u0128\5\6\4\2\u0128\u0129\7\4\2\2\u0129\u012a\7\64\2"+
		"\2\u012a\u012c\5\f\7\2\u012b\u012d\5\16\b\2\u012c\u012b\3\2\2\2\u012c"+
		"\u012d\3\2\2\2\u012d\u0134\3\2\2\2\u012e\u012f\7\64\2\2\u012f\u0131\5"+
		"\f\7\2\u0130\u0132\5\16\b\2\u0131\u0130\3\2\2\2\u0131\u0132\3\2\2\2\u0132"+
		"\u0134\3\2\2\2\u0133\u0127\3\2\2\2\u0133\u012e\3\2\2\2\u0134\63\3\2\2"+
		"\2\u0135\u0136\7\'\2\2\u0136\u0137\7\24\2\2\u0137\u013c\5\62\32\2\u0138"+
		"\u0139\7\5\2\2\u0139\u013b\5\62\32\2\u013a\u0138\3\2\2\2\u013b\u013e\3"+
		"\2\2\2\u013c\u013a\3\2\2\2\u013c\u013d\3\2\2\2\u013d\u013f\3\2\2\2\u013e"+
		"\u013c\3\2\2\2\u013f\u0140\7\25\2\2\u0140\65\3\2\2\2\u0141\u0146\7\27"+
		"\2\2\u0142\u0145\n\4\2\2\u0143\u0145\5\66\34\2\u0144\u0142\3\2\2\2\u0144"+
		"\u0143\3\2\2\2\u0145\u0148\3\2\2\2\u0146\u0144\3\2\2\2\u0146\u0147\3\2"+
		"\2\2\u0147\u0149\3\2\2\2\u0148\u0146\3\2\2\2\u0149\u014a\7\30\2\2\u014a"+
		"\67\3\2\2\2\u014b\u0150\7\64\2\2\u014c\u014d\7\4\2\2\u014d\u014f\7\64"+
		"\2\2\u014e\u014c\3\2\2\2\u014f\u0152\3\2\2\2\u0150\u014e\3\2\2\2\u0150"+
		"\u0151\3\2\2\2\u0151\u0156\3\2\2\2\u0152\u0150\3\2\2\2\u0153\u0154\7\64"+
		"\2\2\u0154\u0156\5\66\34\2\u0155\u014b\3\2\2\2\u0155\u0153\3\2\2\2\u0156"+
		"9\3\2\2\2\u0157\u0158\7(\2\2\u0158\u0159\7\24\2\2\u0159\u015e\58\35\2"+
		"\u015a\u015b\7\5\2\2\u015b\u015d\58\35\2\u015c\u015a\3\2\2\2\u015d\u0160"+
		"\3\2\2\2\u015e\u015c\3\2\2\2\u015e\u015f\3\2\2\2\u015f\u0161\3\2\2\2\u0160"+
		"\u015e\3\2\2\2\u0161\u0162\7\25\2\2\u0162;\3\2\2\2\u0163\u0168\7\64\2"+
		"\2\u0164\u0165\7\5\2\2\u0165\u0167\7\64\2\2\u0166\u0164\3\2\2\2\u0167"+
		"\u016a\3\2\2\2\u0168\u0166\3\2\2\2\u0168\u0169\3\2\2\2\u0169=\3\2\2\2"+
		"\u016a\u0168\3\2\2\2\u016b\u016c\7$\2\2\u016c\u016d\5<\37\2\u016d?\3\2"+
		"\2\2\u016e\u0171\5\6\4\2\u016f\u0170\7\4\2\2\u0170\u0172\7\64\2\2\u0171"+
		"\u016f\3\2\2\2\u0172\u0173\3\2\2\2\u0173\u0171\3\2\2\2\u0173\u0174\3\2"+
		"\2\2\u0174A\3\2\2\2\u0175\u0176\7\35\2\2\u0176\u017d\5@!\2\u0177\u0179"+
		"\7)\2\2\u0178\u017a\5\"\22\2\u0179\u0178\3\2\2\2\u017a\u017b\3\2\2\2\u017b"+
		"\u0179\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u017e\3\2\2\2\u017d\u0177\3\2"+
		"\2\2\u017d\u017e\3\2\2\2\u017eC\3\2\2\2\u017f\u0184\7\27\2\2\u0180\u0183"+
		"\n\4\2\2\u0181\u0183\5D#\2\u0182\u0180\3\2\2\2\u0182\u0181\3\2\2\2\u0183"+
		"\u0186\3\2\2\2\u0184\u0182\3\2\2\2\u0184\u0185\3\2\2\2\u0185\u0187\3\2"+
		"\2\2\u0186\u0184\3\2\2\2\u0187\u0188\7\30\2\2\u0188E\3\2\2\2\u0189\u018a"+
		"\7\64\2\2\u018a\u018b\5D#\2\u018bG\3\2\2\2\u018c\u018d\7\32\2\2\u018d"+
		"\u0192\5F$\2\u018e\u018f\7\5\2\2\u018f\u0191\5F$\2\u0190\u018e\3\2\2\2"+
		"\u0191\u0194\3\2\2\2\u0192\u0190\3\2\2\2\u0192\u0193\3\2\2\2\u0193I\3"+
		"\2\2\2\u0194\u0192\3\2\2\2\u0195\u0196\7\64\2\2\u0196K\3\2\2\2\u0197\u0198"+
		"\7\66\2\2\u0198M\3\2\2\2\u0199\u019a\7%\2\2\u019a\u019b\5L\'\2\u019bO"+
		"\3\2\2\2\u019c\u019d\7\35\2\2\u019d\u01a4\5(\25\2\u019e\u01a0\7)\2\2\u019f"+
		"\u01a1\5\"\22\2\u01a0\u019f\3\2\2\2\u01a1\u01a2\3\2\2\2\u01a2\u01a0\3"+
		"\2\2\2\u01a2\u01a3\3\2\2\2\u01a3\u01a5\3\2\2\2\u01a4\u019e\3\2\2\2\u01a4"+
		"\u01a5\3\2\2\2\u01a5\u01a7\3\2\2\2\u01a6\u01a8\5\64\33\2\u01a7\u01a6\3"+
		"\2\2\2\u01a7\u01a8\3\2\2\2\u01a8\u01aa\3\2\2\2\u01a9\u01ab\5:\36\2\u01aa"+
		"\u01a9\3\2\2\2\u01aa\u01ab\3\2\2\2\u01ab\u01ad\3\2\2\2\u01ac\u01ae\5>"+
		" \2\u01ad\u01ac\3\2\2\2\u01ad\u01ae\3\2\2\2\u01ae\u01b0\3\2\2\2\u01af"+
		"\u01b1\5,\27\2\u01b0\u01af\3\2\2\2\u01b0\u01b1\3\2\2\2\u01b1\u01b3\3\2"+
		"\2\2\u01b2\u01b4\5.\30\2\u01b3\u01b2\3\2\2\2\u01b3\u01b4\3\2\2\2\u01b4"+
		"\u01b6\3\2\2\2\u01b5\u01b7\5\60\31\2\u01b6\u01b5\3\2\2\2\u01b6\u01b7\3"+
		"\2\2\2\u01b7\u01b9\3\2\2\2\u01b8\u01ba\5H%\2\u01b9\u01b8\3\2\2\2\u01b9"+
		"\u01ba\3\2\2\2\u01ba\u01bc\3\2\2\2\u01bb\u01bd\5N(\2\u01bc\u01bb\3\2\2"+
		"\2\u01bc\u01bd\3\2\2\2\u01bdQ\3\2\2\2\u01be\u01bf\7\37\2\2\u01bf\u01c6"+
		"\5(\25\2\u01c0\u01c2\7)\2\2\u01c1\u01c3\5\"\22\2\u01c2\u01c1\3\2\2\2\u01c3"+
		"\u01c4\3\2\2\2\u01c4\u01c2\3\2\2\2\u01c4\u01c5\3\2\2\2\u01c5\u01c7\3\2"+
		"\2\2\u01c6\u01c0\3\2\2\2\u01c6\u01c7\3\2\2\2\u01c7\u01c9\3\2\2\2\u01c8"+
		"\u01ca\5\64\33\2\u01c9\u01c8\3\2\2\2\u01c9\u01ca\3\2\2\2\u01ca\u01cc\3"+
		"\2\2\2\u01cb\u01cd\5> \2\u01cc\u01cb\3\2\2\2\u01cc\u01cd\3\2\2\2\u01cd"+
		"\u01cf\3\2\2\2\u01ce\u01d0\5,\27\2\u01cf\u01ce\3\2\2\2\u01cf\u01d0\3\2"+
		"\2\2\u01d0\u01d2\3\2\2\2\u01d1\u01d3\5.\30\2\u01d2\u01d1\3\2\2\2\u01d2"+
		"\u01d3\3\2\2\2\u01d3\u01d5\3\2\2\2\u01d4\u01d6\5\60\31\2\u01d5\u01d4\3"+
		"\2\2\2\u01d5\u01d6\3\2\2\2\u01d6\u01d8\3\2\2\2\u01d7\u01d9\5N(\2\u01d8"+
		"\u01d7\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9S\3\2\2\2\u01da\u01db\7\64\2\2"+
		"\u01dbU\3\2\2\2\u01dc\u01dd\7\31\2\2\u01dd\u01de\5T+\2\u01deW\3\2\2\2"+
		"\u01df\u01e0\7 \2\2\u01e0\u01e1\5$\23\2\u01e1\u01e8\5V,\2\u01e2\u01e4"+
		"\7)\2\2\u01e3\u01e5\5\"\22\2\u01e4\u01e3\3\2\2\2\u01e5\u01e6\3\2\2\2\u01e6"+
		"\u01e4\3\2\2\2\u01e6\u01e7\3\2\2\2\u01e7\u01e9\3\2\2\2\u01e8\u01e2\3\2"+
		"\2\2\u01e8\u01e9\3\2\2\2\u01e9\u01eb\3\2\2\2\u01ea\u01ec\5,\27\2\u01eb"+
		"\u01ea\3\2\2\2\u01eb\u01ec\3\2\2\2\u01ec\u01ee\3\2\2\2\u01ed\u01ef\5."+
		"\30\2\u01ee\u01ed\3\2\2\2\u01ee\u01ef\3\2\2\2\u01ef\u01f1\3\2\2\2\u01f0"+
		"\u01f2\5\60\31\2\u01f1\u01f0\3\2\2\2\u01f1\u01f2\3\2\2\2\u01f2\u01f4\3"+
		"\2\2\2\u01f3\u01f5\5N(\2\u01f4\u01f3\3\2\2\2\u01f4\u01f5\3\2\2\2\u01f5"+
		"Y\3\2\2\2\u01f6\u01f7\7!\2\2\u01f7\u01fa\5\\/\2\u01f8\u01f9\7*\2\2\u01f9"+
		"\u01fb\5^\60\2\u01fa\u01f8\3\2\2\2\u01fa\u01fb\3\2\2\2\u01fb\u01fd\3\2"+
		"\2\2\u01fc\u01fe\5\64\33\2\u01fd\u01fc\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe"+
		"\u0200\3\2\2\2\u01ff\u0201\5.\30\2\u0200\u01ff\3\2\2\2\u0200\u0201\3\2"+
		"\2\2\u0201\u0203\3\2\2\2\u0202\u0204\5\60\31\2\u0203\u0202\3\2\2\2\u0203"+
		"\u0204\3\2\2\2\u0204[\3\2\2\2\u0205\u0206\7\66\2\2\u0206]\3\2\2\2\u0207"+
		"\u0210\7\64\2\2\u0208\u020b\7\64\2\2\u0209\u020a\7\5\2\2\u020a\u020c\7"+
		"\64\2\2\u020b\u0209\3\2\2\2\u020c\u020d\3\2\2\2\u020d\u020b\3\2\2\2\u020d"+
		"\u020e\3\2\2\2\u020e\u0210\3\2\2\2\u020f\u0207\3\2\2\2\u020f\u0208\3\2"+
		"\2\2\u0210_\3\2\2\2Genxz\u0081\u008f\u0094\u009c\u00a9\u00b4\u00b9\u00bc"+
		"\u00c7\u00cc\u00ce\u00da\u00e7\u00f1\u00f9\u00fd\u0109\u0113\u011e\u012c"+
		"\u0131\u0133\u013c\u0144\u0146\u0150\u0155\u015e\u0168\u0173\u017b\u017d"+
		"\u0182\u0184\u0192\u01a2\u01a4\u01a7\u01aa\u01ad\u01b0\u01b3\u01b6\u01b9"+
		"\u01bc\u01c4\u01c6\u01c9\u01cc\u01cf\u01d2\u01d5\u01d8\u01e6\u01e8\u01eb"+
		"\u01ee\u01f1\u01f4\u01fa\u01fd\u0200\u0203\u020d\u020f";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}