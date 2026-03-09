// Generated from /home/test/modernize/search/src/main/antlr4/ZQL.g4 by ANTLR 4.13.0

package org.zstack.zql.antlr4;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class ZQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, INNER=30, LEFT=31, RIGHT=32, 
		JOIN=33, ON=34, FILTER_BY=35, OFFSET=36, LIMIT=37, QUERY=38, GET=39, COUNT=40, 
		SUM=41, SEARCH=42, ORDER_BY=43, GROUP_BY=44, NAMED_AS=45, ORDER_BY_VALUE=46, 
		RESTRICT_BY=47, RETURN_WITH=48, WHERE=49, FROM=50, AND=51, OR=52, ASC=53, 
		DESC=54, INPUT=55, OUTPUT=56, BOOLEAN=57, INT=58, FLOAT=59, ID=60, WS=61, 
		STRING=62;
	public static final int
		RULE_zqls = 0, RULE_zql = 1, RULE_entity = 2, RULE_field = 3, RULE_multiFields = 4, 
		RULE_operator = 5, RULE_value = 6, RULE_listValue = 7, RULE_logicalOperator = 8, 
		RULE_complexValue = 9, RULE_getQuery = 10, RULE_apiparams = 11, RULE_input = 12, 
		RULE_output = 13, RULE_expr = 14, RULE_joinExpr = 15, RULE_exprAtom = 16, 
		RULE_equal = 17, RULE_condition = 18, RULE_queryTarget = 19, RULE_function = 20, 
		RULE_queryTargetWithFunction = 21, RULE_orderByExpr = 22, RULE_orderBy = 23, 
		RULE_limit = 24, RULE_offset = 25, RULE_restrictByExpr = 26, RULE_restrictBy = 27, 
		RULE_returnWithExprBlock = 28, RULE_returnWithExpr = 29, RULE_returnWith = 30, 
		RULE_groupByExpr = 31, RULE_groupBy = 32, RULE_subQueryTarget = 33, RULE_subQuery = 34, 
		RULE_filterByExprBlock = 35, RULE_filterByExpr = 36, RULE_filterBy = 37, 
		RULE_namedAsKey = 38, RULE_namedAsValue = 39, RULE_namedAs = 40, RULE_joinClause = 41, 
		RULE_query = 42, RULE_count = 43, RULE_sumByValue = 44, RULE_sumBy = 45, 
		RULE_sum = 46, RULE_search = 47, RULE_keyword = 48, RULE_index = 49, RULE_mathOperator = 50;
	private static String[] makeRuleNames() {
		return new String[] {
			"zqls", "zql", "entity", "field", "multiFields", "operator", "value", 
			"listValue", "logicalOperator", "complexValue", "getQuery", "apiparams", 
			"input", "output", "expr", "joinExpr", "exprAtom", "equal", "condition", 
			"queryTarget", "function", "queryTargetWithFunction", "orderByExpr", 
			"orderBy", "limit", "offset", "restrictByExpr", "restrictBy", "returnWithExprBlock", 
			"returnWithExpr", "returnWith", "groupByExpr", "groupBy", "subQueryTarget", 
			"subQuery", "filterByExprBlock", "filterByExpr", "filterBy", "namedAsKey", 
			"namedAsValue", "namedAs", "joinClause", "query", "count", "sumByValue", 
			"sumBy", "sum", "search", "keyword", "index", "mathOperator"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", 
			"'is null'", "'is not null'", "'in'", "'not in'", "'like'", "'not like'", 
			"'has'", "'not has'", "'('", "')'", "'list('", "'{'", "'}'", "'by'", 
			"'*'", "'/'", "'%'", "'+'", "'-'", "'--'", "'inner'", "'left'", "'right'", 
			"'join'", "'on'", "'filter by'", "'offset'", "'limit'", "'query'", "'getapi'", 
			"'count'", "'sum'", "'search'", "'order by'", "'group by'", "'named as'", 
			null, "'restrict by'", "'return with'", "'where'", "'from'", "'and'", 
			"'or'", "'asc'", "'desc'", "'api'", "'output'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, "INNER", "LEFT", "RIGHT", "JOIN", 
			"ON", "FILTER_BY", "OFFSET", "LIMIT", "QUERY", "GET", "COUNT", "SUM", 
			"SEARCH", "ORDER_BY", "GROUP_BY", "NAMED_AS", "ORDER_BY_VALUE", "RESTRICT_BY", 
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(102);
			zql();
			setState(107);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(103);
				match(T__0);
				setState(104);
				zql();
				}
				}
				setState(109);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(110);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
			setState(116);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QUERY:
				_localctx = new QueryGrammarContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(112);
				query();
				}
				break;
			case COUNT:
				_localctx = new CountGrammarContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				count();
				}
				break;
			case SUM:
				_localctx = new SumGrammarContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(114);
				sum();
				}
				break;
			case SEARCH:
				_localctx = new SearchGrammarContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(115);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(118);
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

	@SuppressWarnings("CheckReturnValue")
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
		try {
			int _alt;
			setState(128);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(120);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(121);
				match(ID);
				setState(124); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(122);
						match(T__1);
						setState(123);
						match(ID);
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(126); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	@SuppressWarnings("CheckReturnValue")
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
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(130);
			match(ID);
			setState(133); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(131);
					match(T__2);
					setState(132);
					match(ID);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(135); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(137);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 262128L) != 0)) ) {
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(154);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(139);
				match(STRING);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 2);
				{
				setState(140);
				match(INT);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(141);
				match(FLOAT);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(142);
				match(BOOLEAN);
				}
				break;
			case T__17:
				enterOuterAlt(_localctx, 5);
				{
				setState(143);
				match(T__17);
				setState(144);
				value();
				setState(149);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(145);
					match(T__2);
					setState(146);
					value();
					}
					}
					setState(151);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(152);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(156);
			match(T__19);
			setState(157);
			value();
			setState(162);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(158);
				match(T__2);
				setState(159);
				value();
				}
				}
				setState(164);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(165);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(167);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
			setState(212);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				_localctx = new SimpleValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(169);
				value();
				}
				break;
			case 2:
				_localctx = new SubQueryValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(170);
				match(T__17);
				setState(171);
				subQuery();
				setState(172);
				match(T__18);
				}
				break;
			case 3:
				_localctx = new ApiGetValueContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(175);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(174);
					match(T__17);
					}
				}

				setState(177);
				getQuery();
				setState(178);
				match(T__17);
				setState(179);
				input();
				setState(180);
				match(T__2);
				setState(181);
				output();
				setState(186);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(182);
					match(T__2);
					setState(183);
					apiparams();
					}
					}
					setState(188);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(189);
				match(T__18);
				setState(191);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(190);
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
				setState(194);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(193);
					match(T__17);
					}
				}

				setState(196);
				getQuery();
				setState(197);
				match(T__17);
				setState(198);
				output();
				setState(199);
				match(T__2);
				setState(200);
				input();
				setState(205);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(201);
					match(T__2);
					setState(202);
					apiparams();
					}
					}
					setState(207);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(208);
				match(T__18);
				setState(210);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(209);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(214);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(224);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(216);
				namedAsKey();
				setState(217);
				equal();
				setState(218);
				value();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(220);
				namedAsKey();
				setState(221);
				equal();
				setState(222);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(226);
			match(INPUT);
			setState(227);
			equal();
			setState(228);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(230);
			match(OUTPUT);
			setState(231);
			equal();
			setState(232);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(234);
			field();
			setState(235);
			operator();
			setState(237);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(236);
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

	@SuppressWarnings("CheckReturnValue")
	public static class JoinExprContext extends ParserRuleContext {
		public QueryTargetContext left;
		public QueryTargetContext right;
		public OperatorContext operator() {
			return getRuleContext(OperatorContext.class,0);
		}
		public List<QueryTargetContext> queryTarget() {
			return getRuleContexts(QueryTargetContext.class);
		}
		public QueryTargetContext queryTarget(int i) {
			return getRuleContext(QueryTargetContext.class,i);
		}
		public JoinExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterJoinExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitJoinExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitJoinExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinExprContext joinExpr() throws RecognitionException {
		JoinExprContext _localctx = new JoinExprContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_joinExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(239);
			((JoinExprContext)_localctx).left = queryTarget();
			setState(240);
			operator();
			setState(241);
			((JoinExprContext)_localctx).right = queryTarget();
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExprAtomContext extends ParserRuleContext {
		public ExprAtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprAtom; }
	 
		public ExprAtomContext() { }
		public void copyFrom(ExprAtomContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionCallExpressionAtomContext extends ExprAtomContext {
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public FunctionCallExpressionAtomContext(ExprAtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterFunctionCallExpressionAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitFunctionCallExpressionAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitFunctionCallExpressionAtom(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ColumnNameExprAtomContext extends ExprAtomContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public ColumnNameExprAtomContext(ExprAtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterColumnNameExprAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitColumnNameExprAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitColumnNameExprAtom(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MathExprAtomContext extends ExprAtomContext {
		public ExprAtomContext left;
		public ExprAtomContext right;
		public MathOperatorContext mathOperator() {
			return getRuleContext(MathOperatorContext.class,0);
		}
		public List<ExprAtomContext> exprAtom() {
			return getRuleContexts(ExprAtomContext.class);
		}
		public ExprAtomContext exprAtom(int i) {
			return getRuleContext(ExprAtomContext.class,i);
		}
		public MathExprAtomContext(ExprAtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterMathExprAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitMathExprAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitMathExprAtom(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RelationshipEntityExprAtomContext extends ExprAtomContext {
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public RelationshipEntityExprAtomContext(ExprAtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterRelationshipEntityExprAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitRelationshipEntityExprAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitRelationshipEntityExprAtom(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NestedExprAtomContext extends ExprAtomContext {
		public List<ExprAtomContext> exprAtom() {
			return getRuleContexts(ExprAtomContext.class);
		}
		public ExprAtomContext exprAtom(int i) {
			return getRuleContext(ExprAtomContext.class,i);
		}
		public NestedExprAtomContext(ExprAtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterNestedExprAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitNestedExprAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitNestedExprAtom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprAtomContext exprAtom() throws RecognitionException {
		return exprAtom(0);
	}

	private ExprAtomContext exprAtom(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprAtomContext _localctx = new ExprAtomContext(_ctx, _parentState);
		ExprAtomContext _prevctx = _localctx;
		int _startState = 32;
		enterRecursionRule(_localctx, 32, RULE_exprAtom, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(262);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				_localctx = new ColumnNameExprAtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(244);
				match(ID);
				}
				break;
			case 2:
				{
				_localctx = new RelationshipEntityExprAtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(245);
				queryTarget();
				}
				break;
			case 3:
				{
				_localctx = new FunctionCallExpressionAtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(246);
				function();
				setState(247);
				match(T__17);
				setState(248);
				queryTarget();
				setState(249);
				match(T__18);
				}
				break;
			case 4:
				{
				_localctx = new NestedExprAtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(251);
				match(T__17);
				setState(252);
				exprAtom(0);
				setState(257);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(253);
					match(T__2);
					setState(254);
					exprAtom(0);
					}
					}
					setState(259);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(260);
				match(T__18);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(270);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new MathExprAtomContext(new ExprAtomContext(_parentctx, _parentState));
					((MathExprAtomContext)_localctx).left = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_exprAtom);
					setState(264);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(265);
					mathOperator();
					setState(266);
					((MathExprAtomContext)_localctx).right = exprAtom(2);
					}
					} 
				}
				setState(272);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 34, RULE_equal);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class JoinConditionContext extends ConditionContext {
		public JoinExprContext joinExpr() {
			return getRuleContext(JoinExprContext.class,0);
		}
		public JoinConditionContext(ConditionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterJoinCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitJoinCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitJoinCondition(this);
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
		int _startState = 36;
		enterRecursionRule(_localctx, 36, RULE_condition, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				_localctx = new ParenthesisConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(276);
				match(T__17);
				setState(277);
				condition(0);
				setState(278);
				match(T__18);
				}
				break;
			case 2:
				{
				_localctx = new SimpleConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(280);
				expr();
				}
				break;
			case 3:
				{
				_localctx = new JoinConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(281);
				joinExpr();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(294);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new NestConditionContext(new ConditionContext(_parentctx, _parentState));
					((NestConditionContext)_localctx).left = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_condition);
					setState(284);
					if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
					setState(288); 
					_errHandler.sync(this);
					_alt = 1;
					do {
						switch (_alt) {
						case 1:
							{
							{
							setState(285);
							((NestConditionContext)_localctx).op = logicalOperator();
							setState(286);
							((NestConditionContext)_localctx).right = condition(0);
							}
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						setState(290); 
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
					} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
					}
					} 
				}
				setState(296);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 38, RULE_queryTarget);
		try {
			setState(306);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				_localctx = new OnlyEntityContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(297);
				entity();
				}
				break;
			case 2:
				_localctx = new WithSingleFieldContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(298);
				entity();
				setState(299);
				match(T__1);
				setState(300);
				field();
				}
				break;
			case 3:
				_localctx = new WithMultiFieldsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(302);
				entity();
				setState(303);
				match(T__1);
				setState(304);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
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
		enterRule(_localctx, 40, RULE_function);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(308);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class WithFunctionContext extends QueryTargetWithFunctionContext {
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public QueryTargetWithFunctionContext queryTargetWithFunction() {
			return getRuleContext(QueryTargetWithFunctionContext.class,0);
		}
		public List<JoinClauseContext> joinClause() {
			return getRuleContexts(JoinClauseContext.class);
		}
		public JoinClauseContext joinClause(int i) {
			return getRuleContext(JoinClauseContext.class,i);
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
	@SuppressWarnings("CheckReturnValue")
	public static class WithoutFunctionContext extends QueryTargetWithFunctionContext {
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public List<JoinClauseContext> joinClause() {
			return getRuleContexts(JoinClauseContext.class);
		}
		public JoinClauseContext joinClause(int i) {
			return getRuleContext(JoinClauseContext.class,i);
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
		enterRule(_localctx, 42, RULE_queryTargetWithFunction);
		int _la;
		try {
			setState(327);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				_localctx = new WithoutFunctionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(310);
				queryTarget();
				setState(314);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 7516192768L) != 0)) {
					{
					{
					setState(311);
					joinClause();
					}
					}
					setState(316);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				_localctx = new WithFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(317);
				function();
				setState(318);
				match(T__17);
				setState(319);
				queryTargetWithFunction();
				setState(320);
				match(T__18);
				setState(324);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 7516192768L) != 0)) {
					{
					{
					setState(321);
					joinClause();
					}
					}
					setState(326);
					_errHandler.sync(this);
					_la = _input.LA(1);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OrderByExprContext extends ParserRuleContext {
		public ExprAtomContext exprAtom() {
			return getRuleContext(ExprAtomContext.class,0);
		}
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
		enterRule(_localctx, 44, RULE_orderByExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(329);
			exprAtom(0);
			setState(330);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 46, RULE_orderBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(332);
			match(ORDER_BY);
			setState(333);
			orderByExpr();
			setState(338);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(334);
				match(T__2);
				setState(335);
				orderByExpr();
				}
				}
				setState(340);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 48, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(341);
			match(LIMIT);
			setState(342);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 50, RULE_offset);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(344);
			match(OFFSET);
			setState(345);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 52, RULE_restrictByExpr);
		int _la;
		try {
			setState(359);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(347);
				entity();
				setState(348);
				match(T__1);
				setState(349);
				match(ID);
				setState(350);
				operator();
				setState(352);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 5620492334958641152L) != 0)) {
					{
					setState(351);
					value();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(354);
				match(ID);
				setState(355);
				operator();
				setState(357);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 5620492334958641152L) != 0)) {
					{
					setState(356);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 54, RULE_restrictBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(361);
			match(RESTRICT_BY);
			setState(362);
			match(T__17);
			setState(363);
			restrictByExpr();
			setState(368);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(364);
				match(T__2);
				setState(365);
				restrictByExpr();
				}
				}
				setState(370);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(371);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 56, RULE_returnWithExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(373);
			match(T__20);
			setState(378);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 9223372036850581502L) != 0)) {
				{
				setState(376);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
				case 1:
					{
					setState(374);
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
					setState(375);
					returnWithExprBlock();
					}
					break;
				}
				}
				setState(380);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(381);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 58, RULE_returnWithExpr);
		int _la;
		try {
			setState(393);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				_localctx = new ReturnWithExprIdContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(383);
				match(ID);
				setState(388);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(384);
					match(T__1);
					setState(385);
					match(ID);
					}
					}
					setState(390);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				_localctx = new ReturnWithExprFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(391);
				match(ID);
				setState(392);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 60, RULE_returnWith);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(395);
			match(RETURN_WITH);
			setState(396);
			match(T__17);
			setState(397);
			returnWithExpr();
			setState(402);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(398);
				match(T__2);
				setState(399);
				returnWithExpr();
				}
				}
				setState(404);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(405);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 62, RULE_groupByExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(407);
			match(ID);
			setState(412);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(408);
				match(T__2);
				setState(409);
				match(ID);
				}
				}
				setState(414);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 64, RULE_groupBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(415);
			match(GROUP_BY);
			setState(416);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 66, RULE_subQueryTarget);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(418);
			entity();
			setState(421); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(419);
				match(T__1);
				setState(420);
				match(ID);
				}
				}
				setState(423); 
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 68, RULE_subQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(425);
			match(QUERY);
			setState(426);
			subQueryTarget();
			setState(433);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(427);
				match(WHERE);
				setState(429); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(428);
					condition(0);
					}
					}
					setState(431); 
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 70, RULE_filterByExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(435);
			match(T__20);
			setState(440);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 9223372036850581502L) != 0)) {
				{
				setState(438);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
				case 1:
					{
					setState(436);
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
					setState(437);
					filterByExprBlock();
					}
					break;
				}
				}
				setState(442);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(443);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 72, RULE_filterByExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(445);
			match(ID);
			setState(446);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 74, RULE_filterBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(448);
			match(FILTER_BY);
			setState(449);
			filterByExpr();
			setState(454);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(450);
				match(T__2);
				setState(451);
				filterByExpr();
				}
				}
				setState(456);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 76, RULE_namedAsKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(457);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 78, RULE_namedAsValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(459);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 80, RULE_namedAs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(461);
			match(NAMED_AS);
			setState(462);
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

	@SuppressWarnings("CheckReturnValue")
	public static class JoinClauseContext extends ParserRuleContext {
		public JoinClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_joinClause; }
	 
		public JoinClauseContext() { }
		public void copyFrom(JoinClauseContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class JoinTableContext extends JoinClauseContext {
		public TerminalNode JOIN() { return getToken(ZQLParser.JOIN, 0); }
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public TerminalNode ON() { return getToken(ZQLParser.ON, 0); }
		public TerminalNode INNER() { return getToken(ZQLParser.INNER, 0); }
		public TerminalNode LEFT() { return getToken(ZQLParser.LEFT, 0); }
		public TerminalNode RIGHT() { return getToken(ZQLParser.RIGHT, 0); }
		public List<ConditionContext> condition() {
			return getRuleContexts(ConditionContext.class);
		}
		public ConditionContext condition(int i) {
			return getRuleContext(ConditionContext.class,i);
		}
		public JoinTableContext(JoinClauseContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterJoinTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitJoinTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitJoinTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JoinClauseContext joinClause() throws RecognitionException {
		JoinClauseContext _localctx = new JoinClauseContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_joinClause);
		int _la;
		try {
			_localctx = new JoinTableContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(464);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 7516192768L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(465);
			match(JOIN);
			setState(466);
			queryTarget();
			setState(467);
			match(ON);
			setState(469); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(468);
				condition(0);
				}
				}
				setState(471); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__17 || _la==ID );
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 84, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(473);
			match(QUERY);
			setState(474);
			queryTargetWithFunction();
			setState(481);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(475);
				match(WHERE);
				setState(477); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(476);
					condition(0);
					}
					}
					setState(479); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(484);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(483);
				restrictBy();
				}
			}

			setState(487);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RETURN_WITH) {
				{
				setState(486);
				returnWith();
				}
			}

			setState(490);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP_BY) {
				{
				setState(489);
				groupBy();
				}
			}

			setState(493);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(492);
				orderBy();
				}
			}

			setState(496);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(495);
				limit();
				}
			}

			setState(499);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(498);
				offset();
				}
			}

			setState(502);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FILTER_BY) {
				{
				setState(501);
				filterBy();
				}
			}

			setState(505);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(504);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 86, RULE_count);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(507);
			match(COUNT);
			setState(508);
			queryTargetWithFunction();
			setState(515);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(509);
				match(WHERE);
				setState(511); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(510);
					condition(0);
					}
					}
					setState(513); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(518);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(517);
				restrictBy();
				}
			}

			setState(521);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP_BY) {
				{
				setState(520);
				groupBy();
				}
			}

			setState(524);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(523);
				orderBy();
				}
			}

			setState(527);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(526);
				limit();
				}
			}

			setState(530);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(529);
				offset();
				}
			}

			setState(533);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(532);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 88, RULE_sumByValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(535);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 90, RULE_sumBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(537);
			match(T__22);
			setState(538);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 92, RULE_sum);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(540);
			match(SUM);
			setState(541);
			queryTarget();
			setState(542);
			sumBy();
			setState(549);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(543);
				match(WHERE);
				setState(545); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(544);
					condition(0);
					}
					}
					setState(547); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(552);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(551);
				orderBy();
				}
			}

			setState(555);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(554);
				limit();
				}
			}

			setState(558);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(557);
				offset();
				}
			}

			setState(561);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(560);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 94, RULE_search);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(563);
			match(SEARCH);
			setState(564);
			keyword();
			setState(567);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FROM) {
				{
				setState(565);
				match(FROM);
				setState(566);
				index();
				}
			}

			setState(570);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(569);
				restrictBy();
				}
			}

			setState(573);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(572);
				limit();
				}
			}

			setState(576);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(575);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 96, RULE_keyword);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(578);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 98, RULE_index);
		int _la;
		try {
			setState(588);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,74,_ctx) ) {
			case 1:
				_localctx = new SingleIndexContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(580);
				match(ID);
				}
				break;
			case 2:
				_localctx = new MultiIndexsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(581);
				match(ID);
				setState(584); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(582);
					match(T__2);
					setState(583);
					match(ID);
					}
					}
					setState(586); 
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

	@SuppressWarnings("CheckReturnValue")
	public static class MathOperatorContext extends ParserRuleContext {
		public MathOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mathOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).enterMathOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ZQLListener ) ((ZQLListener)listener).exitMathOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ZQLVisitor ) return ((ZQLVisitor<? extends T>)visitor).visitMathOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MathOperatorContext mathOperator() throws RecognitionException {
		MathOperatorContext _localctx = new MathOperatorContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_mathOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(590);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1056964608L) != 0)) ) {
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 16:
			return exprAtom_sempred((ExprAtomContext)_localctx, predIndex);
		case 18:
			return condition_sempred((ConditionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean exprAtom_sempred(ExprAtomContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean condition_sempred(ConditionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 3);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001>\u0251\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u0002"+
		"2\u00072\u0001\u0000\u0001\u0000\u0001\u0000\u0005\u0000j\b\u0000\n\u0000"+
		"\f\u0000m\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0003\u0001u\b\u0001\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0004\u0003}\b\u0003\u000b"+
		"\u0003\f\u0003~\u0003\u0003\u0081\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0004\u0004\u0086\b\u0004\u000b\u0004\f\u0004\u0087\u0001\u0005"+
		"\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006\u0094\b\u0006\n\u0006"+
		"\f\u0006\u0097\t\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u009b\b\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u00a1\b\u0007"+
		"\n\u0007\f\u0007\u00a4\t\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b"+
		"\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003\t\u00b0\b\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0005\t\u00b9\b\t\n"+
		"\t\f\t\u00bc\t\t\u0001\t\u0001\t\u0003\t\u00c0\b\t\u0001\t\u0003\t\u00c3"+
		"\b\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0005\t\u00cc"+
		"\b\t\n\t\f\t\u00cf\t\t\u0001\t\u0001\t\u0003\t\u00d3\b\t\u0003\t\u00d5"+
		"\b\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u00e1\b\u000b\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0003\u000e\u00ee\b\u000e\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0005\u0010\u0100\b\u0010\n\u0010\f\u0010\u0103"+
		"\t\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u0107\b\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0005\u0010\u010d\b\u0010\n\u0010"+
		"\f\u0010\u0110\t\u0010\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0003\u0012"+
		"\u011b\b\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0004\u0012"+
		"\u0121\b\u0012\u000b\u0012\f\u0012\u0122\u0005\u0012\u0125\b\u0012\n\u0012"+
		"\f\u0012\u0128\t\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013"+
		"\u0133\b\u0013\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0005\u0015"+
		"\u0139\b\u0015\n\u0015\f\u0015\u013c\t\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0005\u0015\u0143\b\u0015\n\u0015\f\u0015"+
		"\u0146\t\u0015\u0003\u0015\u0148\b\u0015\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0005\u0017\u0151"+
		"\b\u0017\n\u0017\f\u0017\u0154\t\u0017\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0003\u001a\u0161\b\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0003\u001a\u0166\b\u001a\u0003\u001a\u0168\b\u001a\u0001"+
		"\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0005\u001b\u016f"+
		"\b\u001b\n\u001b\f\u001b\u0172\t\u001b\u0001\u001b\u0001\u001b\u0001\u001c"+
		"\u0001\u001c\u0001\u001c\u0005\u001c\u0179\b\u001c\n\u001c\f\u001c\u017c"+
		"\t\u001c\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0005"+
		"\u001d\u0183\b\u001d\n\u001d\f\u001d\u0186\t\u001d\u0001\u001d\u0001\u001d"+
		"\u0003\u001d\u018a\b\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0005\u001e\u0191\b\u001e\n\u001e\f\u001e\u0194\t\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0005\u001f\u019b"+
		"\b\u001f\n\u001f\f\u001f\u019e\t\u001f\u0001 \u0001 \u0001 \u0001!\u0001"+
		"!\u0001!\u0004!\u01a6\b!\u000b!\f!\u01a7\u0001\"\u0001\"\u0001\"\u0001"+
		"\"\u0004\"\u01ae\b\"\u000b\"\f\"\u01af\u0003\"\u01b2\b\"\u0001#\u0001"+
		"#\u0001#\u0005#\u01b7\b#\n#\f#\u01ba\t#\u0001#\u0001#\u0001$\u0001$\u0001"+
		"$\u0001%\u0001%\u0001%\u0001%\u0005%\u01c5\b%\n%\f%\u01c8\t%\u0001&\u0001"+
		"&\u0001\'\u0001\'\u0001(\u0001(\u0001(\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0004)\u01d6\b)\u000b)\f)\u01d7\u0001*\u0001*\u0001*\u0001*\u0004*\u01de"+
		"\b*\u000b*\f*\u01df\u0003*\u01e2\b*\u0001*\u0003*\u01e5\b*\u0001*\u0003"+
		"*\u01e8\b*\u0001*\u0003*\u01eb\b*\u0001*\u0003*\u01ee\b*\u0001*\u0003"+
		"*\u01f1\b*\u0001*\u0003*\u01f4\b*\u0001*\u0003*\u01f7\b*\u0001*\u0003"+
		"*\u01fa\b*\u0001+\u0001+\u0001+\u0001+\u0004+\u0200\b+\u000b+\f+\u0201"+
		"\u0003+\u0204\b+\u0001+\u0003+\u0207\b+\u0001+\u0003+\u020a\b+\u0001+"+
		"\u0003+\u020d\b+\u0001+\u0003+\u0210\b+\u0001+\u0003+\u0213\b+\u0001+"+
		"\u0003+\u0216\b+\u0001,\u0001,\u0001-\u0001-\u0001-\u0001.\u0001.\u0001"+
		".\u0001.\u0001.\u0004.\u0222\b.\u000b.\f.\u0223\u0003.\u0226\b.\u0001"+
		".\u0003.\u0229\b.\u0001.\u0003.\u022c\b.\u0001.\u0003.\u022f\b.\u0001"+
		".\u0003.\u0232\b.\u0001/\u0001/\u0001/\u0001/\u0003/\u0238\b/\u0001/\u0003"+
		"/\u023b\b/\u0001/\u0003/\u023e\b/\u0001/\u0003/\u0241\b/\u00010\u0001"+
		"0\u00011\u00011\u00011\u00011\u00041\u0249\b1\u000b1\f1\u024a\u00031\u024d"+
		"\b1\u00012\u00012\u00012\u0000\u0002 $3\u0000\u0002\u0004\u0006\b\n\f"+
		"\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:"+
		"<>@BDFHJLNPRTVXZ\\^`bd\u0000\u0005\u0001\u0000\u0004\u0011\u0001\u0000"+
		"34\u0001\u0000\u0016\u0016\u0001\u0000\u001e \u0001\u0000\u0018\u001d"+
		"\u0273\u0000f\u0001\u0000\u0000\u0000\u0002t\u0001\u0000\u0000\u0000\u0004"+
		"v\u0001\u0000\u0000\u0000\u0006\u0080\u0001\u0000\u0000\u0000\b\u0082"+
		"\u0001\u0000\u0000\u0000\n\u0089\u0001\u0000\u0000\u0000\f\u009a\u0001"+
		"\u0000\u0000\u0000\u000e\u009c\u0001\u0000\u0000\u0000\u0010\u00a7\u0001"+
		"\u0000\u0000\u0000\u0012\u00d4\u0001\u0000\u0000\u0000\u0014\u00d6\u0001"+
		"\u0000\u0000\u0000\u0016\u00e0\u0001\u0000\u0000\u0000\u0018\u00e2\u0001"+
		"\u0000\u0000\u0000\u001a\u00e6\u0001\u0000\u0000\u0000\u001c\u00ea\u0001"+
		"\u0000\u0000\u0000\u001e\u00ef\u0001\u0000\u0000\u0000 \u0106\u0001\u0000"+
		"\u0000\u0000\"\u0111\u0001\u0000\u0000\u0000$\u011a\u0001\u0000\u0000"+
		"\u0000&\u0132\u0001\u0000\u0000\u0000(\u0134\u0001\u0000\u0000\u0000*"+
		"\u0147\u0001\u0000\u0000\u0000,\u0149\u0001\u0000\u0000\u0000.\u014c\u0001"+
		"\u0000\u0000\u00000\u0155\u0001\u0000\u0000\u00002\u0158\u0001\u0000\u0000"+
		"\u00004\u0167\u0001\u0000\u0000\u00006\u0169\u0001\u0000\u0000\u00008"+
		"\u0175\u0001\u0000\u0000\u0000:\u0189\u0001\u0000\u0000\u0000<\u018b\u0001"+
		"\u0000\u0000\u0000>\u0197\u0001\u0000\u0000\u0000@\u019f\u0001\u0000\u0000"+
		"\u0000B\u01a2\u0001\u0000\u0000\u0000D\u01a9\u0001\u0000\u0000\u0000F"+
		"\u01b3\u0001\u0000\u0000\u0000H\u01bd\u0001\u0000\u0000\u0000J\u01c0\u0001"+
		"\u0000\u0000\u0000L\u01c9\u0001\u0000\u0000\u0000N\u01cb\u0001\u0000\u0000"+
		"\u0000P\u01cd\u0001\u0000\u0000\u0000R\u01d0\u0001\u0000\u0000\u0000T"+
		"\u01d9\u0001\u0000\u0000\u0000V\u01fb\u0001\u0000\u0000\u0000X\u0217\u0001"+
		"\u0000\u0000\u0000Z\u0219\u0001\u0000\u0000\u0000\\\u021c\u0001\u0000"+
		"\u0000\u0000^\u0233\u0001\u0000\u0000\u0000`\u0242\u0001\u0000\u0000\u0000"+
		"b\u024c\u0001\u0000\u0000\u0000d\u024e\u0001\u0000\u0000\u0000fk\u0003"+
		"\u0002\u0001\u0000gh\u0005\u0001\u0000\u0000hj\u0003\u0002\u0001\u0000"+
		"ig\u0001\u0000\u0000\u0000jm\u0001\u0000\u0000\u0000ki\u0001\u0000\u0000"+
		"\u0000kl\u0001\u0000\u0000\u0000ln\u0001\u0000\u0000\u0000mk\u0001\u0000"+
		"\u0000\u0000no\u0005\u0000\u0000\u0001o\u0001\u0001\u0000\u0000\u0000"+
		"pu\u0003T*\u0000qu\u0003V+\u0000ru\u0003\\.\u0000su\u0003^/\u0000tp\u0001"+
		"\u0000\u0000\u0000tq\u0001\u0000\u0000\u0000tr\u0001\u0000\u0000\u0000"+
		"ts\u0001\u0000\u0000\u0000u\u0003\u0001\u0000\u0000\u0000vw\u0005<\u0000"+
		"\u0000w\u0005\u0001\u0000\u0000\u0000x\u0081\u0005<\u0000\u0000y|\u0005"+
		"<\u0000\u0000z{\u0005\u0002\u0000\u0000{}\u0005<\u0000\u0000|z\u0001\u0000"+
		"\u0000\u0000}~\u0001\u0000\u0000\u0000~|\u0001\u0000\u0000\u0000~\u007f"+
		"\u0001\u0000\u0000\u0000\u007f\u0081\u0001\u0000\u0000\u0000\u0080x\u0001"+
		"\u0000\u0000\u0000\u0080y\u0001\u0000\u0000\u0000\u0081\u0007\u0001\u0000"+
		"\u0000\u0000\u0082\u0085\u0005<\u0000\u0000\u0083\u0084\u0005\u0003\u0000"+
		"\u0000\u0084\u0086\u0005<\u0000\u0000\u0085\u0083\u0001\u0000\u0000\u0000"+
		"\u0086\u0087\u0001\u0000\u0000\u0000\u0087\u0085\u0001\u0000\u0000\u0000"+
		"\u0087\u0088\u0001\u0000\u0000\u0000\u0088\t\u0001\u0000\u0000\u0000\u0089"+
		"\u008a\u0007\u0000\u0000\u0000\u008a\u000b\u0001\u0000\u0000\u0000\u008b"+
		"\u009b\u0005>\u0000\u0000\u008c\u009b\u0005:\u0000\u0000\u008d\u009b\u0005"+
		";\u0000\u0000\u008e\u009b\u00059\u0000\u0000\u008f\u0090\u0005\u0012\u0000"+
		"\u0000\u0090\u0095\u0003\f\u0006\u0000\u0091\u0092\u0005\u0003\u0000\u0000"+
		"\u0092\u0094\u0003\f\u0006\u0000\u0093\u0091\u0001\u0000\u0000\u0000\u0094"+
		"\u0097\u0001\u0000\u0000\u0000\u0095\u0093\u0001\u0000\u0000\u0000\u0095"+
		"\u0096\u0001\u0000\u0000\u0000\u0096\u0098\u0001\u0000\u0000\u0000\u0097"+
		"\u0095\u0001\u0000\u0000\u0000\u0098\u0099\u0005\u0013\u0000\u0000\u0099"+
		"\u009b\u0001\u0000\u0000\u0000\u009a\u008b\u0001\u0000\u0000\u0000\u009a"+
		"\u008c\u0001\u0000\u0000\u0000\u009a\u008d\u0001\u0000\u0000\u0000\u009a"+
		"\u008e\u0001\u0000\u0000\u0000\u009a\u008f\u0001\u0000\u0000\u0000\u009b"+
		"\r\u0001\u0000\u0000\u0000\u009c\u009d\u0005\u0014\u0000\u0000\u009d\u00a2"+
		"\u0003\f\u0006\u0000\u009e\u009f\u0005\u0003\u0000\u0000\u009f\u00a1\u0003"+
		"\f\u0006\u0000\u00a0\u009e\u0001\u0000\u0000\u0000\u00a1\u00a4\u0001\u0000"+
		"\u0000\u0000\u00a2\u00a0\u0001\u0000\u0000\u0000\u00a2\u00a3\u0001\u0000"+
		"\u0000\u0000\u00a3\u00a5\u0001\u0000\u0000\u0000\u00a4\u00a2\u0001\u0000"+
		"\u0000\u0000\u00a5\u00a6\u0005\u0013\u0000\u0000\u00a6\u000f\u0001\u0000"+
		"\u0000\u0000\u00a7\u00a8\u0007\u0001\u0000\u0000\u00a8\u0011\u0001\u0000"+
		"\u0000\u0000\u00a9\u00d5\u0003\f\u0006\u0000\u00aa\u00ab\u0005\u0012\u0000"+
		"\u0000\u00ab\u00ac\u0003D\"\u0000\u00ac\u00ad\u0005\u0013\u0000\u0000"+
		"\u00ad\u00d5\u0001\u0000\u0000\u0000\u00ae\u00b0\u0005\u0012\u0000\u0000"+
		"\u00af\u00ae\u0001\u0000\u0000\u0000\u00af\u00b0\u0001\u0000\u0000\u0000"+
		"\u00b0\u00b1\u0001\u0000\u0000\u0000\u00b1\u00b2\u0003\u0014\n\u0000\u00b2"+
		"\u00b3\u0005\u0012\u0000\u0000\u00b3\u00b4\u0003\u0018\f\u0000\u00b4\u00b5"+
		"\u0005\u0003\u0000\u0000\u00b5\u00ba\u0003\u001a\r\u0000\u00b6\u00b7\u0005"+
		"\u0003\u0000\u0000\u00b7\u00b9\u0003\u0016\u000b\u0000\u00b8\u00b6\u0001"+
		"\u0000\u0000\u0000\u00b9\u00bc\u0001\u0000\u0000\u0000\u00ba\u00b8\u0001"+
		"\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000\u00bb\u00bd\u0001"+
		"\u0000\u0000\u0000\u00bc\u00ba\u0001\u0000\u0000\u0000\u00bd\u00bf\u0005"+
		"\u0013\u0000\u0000\u00be\u00c0\u0005\u0013\u0000\u0000\u00bf\u00be\u0001"+
		"\u0000\u0000\u0000\u00bf\u00c0\u0001\u0000\u0000\u0000\u00c0\u00d5\u0001"+
		"\u0000\u0000\u0000\u00c1\u00c3\u0005\u0012\u0000\u0000\u00c2\u00c1\u0001"+
		"\u0000\u0000\u0000\u00c2\u00c3\u0001\u0000\u0000\u0000\u00c3\u00c4\u0001"+
		"\u0000\u0000\u0000\u00c4\u00c5\u0003\u0014\n\u0000\u00c5\u00c6\u0005\u0012"+
		"\u0000\u0000\u00c6\u00c7\u0003\u001a\r\u0000\u00c7\u00c8\u0005\u0003\u0000"+
		"\u0000\u00c8\u00cd\u0003\u0018\f\u0000\u00c9\u00ca\u0005\u0003\u0000\u0000"+
		"\u00ca\u00cc\u0003\u0016\u000b\u0000\u00cb\u00c9\u0001\u0000\u0000\u0000"+
		"\u00cc\u00cf\u0001\u0000\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000"+
		"\u00cd\u00ce\u0001\u0000\u0000\u0000\u00ce\u00d0\u0001\u0000\u0000\u0000"+
		"\u00cf\u00cd\u0001\u0000\u0000\u0000\u00d0\u00d2\u0005\u0013\u0000\u0000"+
		"\u00d1\u00d3\u0005\u0013\u0000\u0000\u00d2\u00d1\u0001\u0000\u0000\u0000"+
		"\u00d2\u00d3\u0001\u0000\u0000\u0000\u00d3\u00d5\u0001\u0000\u0000\u0000"+
		"\u00d4\u00a9\u0001\u0000\u0000\u0000\u00d4\u00aa\u0001\u0000\u0000\u0000"+
		"\u00d4\u00af\u0001\u0000\u0000\u0000\u00d4\u00c2\u0001\u0000\u0000\u0000"+
		"\u00d5\u0013\u0001\u0000\u0000\u0000\u00d6\u00d7\u0005\'\u0000\u0000\u00d7"+
		"\u0015\u0001\u0000\u0000\u0000\u00d8\u00d9\u0003L&\u0000\u00d9\u00da\u0003"+
		"\"\u0011\u0000\u00da\u00db\u0003\f\u0006\u0000\u00db\u00e1\u0001\u0000"+
		"\u0000\u0000\u00dc\u00dd\u0003L&\u0000\u00dd\u00de\u0003\"\u0011\u0000"+
		"\u00de\u00df\u0003\u000e\u0007\u0000\u00df\u00e1\u0001\u0000\u0000\u0000"+
		"\u00e0\u00d8\u0001\u0000\u0000\u0000\u00e0\u00dc\u0001\u0000\u0000\u0000"+
		"\u00e1\u0017\u0001\u0000\u0000\u0000\u00e2\u00e3\u00057\u0000\u0000\u00e3"+
		"\u00e4\u0003\"\u0011\u0000\u00e4\u00e5\u0003N\'\u0000\u00e5\u0019\u0001"+
		"\u0000\u0000\u0000\u00e6\u00e7\u00058\u0000\u0000\u00e7\u00e8\u0003\""+
		"\u0011\u0000\u00e8\u00e9\u0003N\'\u0000\u00e9\u001b\u0001\u0000\u0000"+
		"\u0000\u00ea\u00eb\u0003\u0006\u0003\u0000\u00eb\u00ed\u0003\n\u0005\u0000"+
		"\u00ec\u00ee\u0003\u0012\t\u0000\u00ed\u00ec\u0001\u0000\u0000\u0000\u00ed"+
		"\u00ee\u0001\u0000\u0000\u0000\u00ee\u001d\u0001\u0000\u0000\u0000\u00ef"+
		"\u00f0\u0003&\u0013\u0000\u00f0\u00f1\u0003\n\u0005\u0000\u00f1\u00f2"+
		"\u0003&\u0013\u0000\u00f2\u001f\u0001\u0000\u0000\u0000\u00f3\u00f4\u0006"+
		"\u0010\uffff\uffff\u0000\u00f4\u0107\u0005<\u0000\u0000\u00f5\u0107\u0003"+
		"&\u0013\u0000\u00f6\u00f7\u0003(\u0014\u0000\u00f7\u00f8\u0005\u0012\u0000"+
		"\u0000\u00f8\u00f9\u0003&\u0013\u0000\u00f9\u00fa\u0005\u0013\u0000\u0000"+
		"\u00fa\u0107\u0001\u0000\u0000\u0000\u00fb\u00fc\u0005\u0012\u0000\u0000"+
		"\u00fc\u0101\u0003 \u0010\u0000\u00fd\u00fe\u0005\u0003\u0000\u0000\u00fe"+
		"\u0100\u0003 \u0010\u0000\u00ff\u00fd\u0001\u0000\u0000\u0000\u0100\u0103"+
		"\u0001\u0000\u0000\u0000\u0101\u00ff\u0001\u0000\u0000\u0000\u0101\u0102"+
		"\u0001\u0000\u0000\u0000\u0102\u0104\u0001\u0000\u0000\u0000\u0103\u0101"+
		"\u0001\u0000\u0000\u0000\u0104\u0105\u0005\u0013\u0000\u0000\u0105\u0107"+
		"\u0001\u0000\u0000\u0000\u0106\u00f3\u0001\u0000\u0000\u0000\u0106\u00f5"+
		"\u0001\u0000\u0000\u0000\u0106\u00f6\u0001\u0000\u0000\u0000\u0106\u00fb"+
		"\u0001\u0000\u0000\u0000\u0107\u010e\u0001\u0000\u0000\u0000\u0108\u0109"+
		"\n\u0001\u0000\u0000\u0109\u010a\u0003d2\u0000\u010a\u010b\u0003 \u0010"+
		"\u0002\u010b\u010d\u0001\u0000\u0000\u0000\u010c\u0108\u0001\u0000\u0000"+
		"\u0000\u010d\u0110\u0001\u0000\u0000\u0000\u010e\u010c\u0001\u0000\u0000"+
		"\u0000\u010e\u010f\u0001\u0000\u0000\u0000\u010f!\u0001\u0000\u0000\u0000"+
		"\u0110\u010e\u0001\u0000\u0000\u0000\u0111\u0112\u0005\u0004\u0000\u0000"+
		"\u0112#\u0001\u0000\u0000\u0000\u0113\u0114\u0006\u0012\uffff\uffff\u0000"+
		"\u0114\u0115\u0005\u0012\u0000\u0000\u0115\u0116\u0003$\u0012\u0000\u0116"+
		"\u0117\u0005\u0013\u0000\u0000\u0117\u011b\u0001\u0000\u0000\u0000\u0118"+
		"\u011b\u0003\u001c\u000e\u0000\u0119\u011b\u0003\u001e\u000f\u0000\u011a"+
		"\u0113\u0001\u0000\u0000\u0000\u011a\u0118\u0001\u0000\u0000\u0000\u011a"+
		"\u0119\u0001\u0000\u0000\u0000\u011b\u0126\u0001\u0000\u0000\u0000\u011c"+
		"\u0120\n\u0003\u0000\u0000\u011d\u011e\u0003\u0010\b\u0000\u011e\u011f"+
		"\u0003$\u0012\u0000\u011f\u0121\u0001\u0000\u0000\u0000\u0120\u011d\u0001"+
		"\u0000\u0000\u0000\u0121\u0122\u0001\u0000\u0000\u0000\u0122\u0120\u0001"+
		"\u0000\u0000\u0000\u0122\u0123\u0001\u0000\u0000\u0000\u0123\u0125\u0001"+
		"\u0000\u0000\u0000\u0124\u011c\u0001\u0000\u0000\u0000\u0125\u0128\u0001"+
		"\u0000\u0000\u0000\u0126\u0124\u0001\u0000\u0000\u0000\u0126\u0127\u0001"+
		"\u0000\u0000\u0000\u0127%\u0001\u0000\u0000\u0000\u0128\u0126\u0001\u0000"+
		"\u0000\u0000\u0129\u0133\u0003\u0004\u0002\u0000\u012a\u012b\u0003\u0004"+
		"\u0002\u0000\u012b\u012c\u0005\u0002\u0000\u0000\u012c\u012d\u0003\u0006"+
		"\u0003\u0000\u012d\u0133\u0001\u0000\u0000\u0000\u012e\u012f\u0003\u0004"+
		"\u0002\u0000\u012f\u0130\u0005\u0002\u0000\u0000\u0130\u0131\u0003\b\u0004"+
		"\u0000\u0131\u0133\u0001\u0000\u0000\u0000\u0132\u0129\u0001\u0000\u0000"+
		"\u0000\u0132\u012a\u0001\u0000\u0000\u0000\u0132\u012e\u0001\u0000\u0000"+
		"\u0000\u0133\'\u0001\u0000\u0000\u0000\u0134\u0135\u0005<\u0000\u0000"+
		"\u0135)\u0001\u0000\u0000\u0000\u0136\u013a\u0003&\u0013\u0000\u0137\u0139"+
		"\u0003R)\u0000\u0138\u0137\u0001\u0000\u0000\u0000\u0139\u013c\u0001\u0000"+
		"\u0000\u0000\u013a\u0138\u0001\u0000\u0000\u0000\u013a\u013b\u0001\u0000"+
		"\u0000\u0000\u013b\u0148\u0001\u0000\u0000\u0000\u013c\u013a\u0001\u0000"+
		"\u0000\u0000\u013d\u013e\u0003(\u0014\u0000\u013e\u013f\u0005\u0012\u0000"+
		"\u0000\u013f\u0140\u0003*\u0015\u0000\u0140\u0144\u0005\u0013\u0000\u0000"+
		"\u0141\u0143\u0003R)\u0000\u0142\u0141\u0001\u0000\u0000\u0000\u0143\u0146"+
		"\u0001\u0000\u0000\u0000\u0144\u0142\u0001\u0000\u0000\u0000\u0144\u0145"+
		"\u0001\u0000\u0000\u0000\u0145\u0148\u0001\u0000\u0000\u0000\u0146\u0144"+
		"\u0001\u0000\u0000\u0000\u0147\u0136\u0001\u0000\u0000\u0000\u0147\u013d"+
		"\u0001\u0000\u0000\u0000\u0148+\u0001\u0000\u0000\u0000\u0149\u014a\u0003"+
		" \u0010\u0000\u014a\u014b\u0005.\u0000\u0000\u014b-\u0001\u0000\u0000"+
		"\u0000\u014c\u014d\u0005+\u0000\u0000\u014d\u0152\u0003,\u0016\u0000\u014e"+
		"\u014f\u0005\u0003\u0000\u0000\u014f\u0151\u0003,\u0016\u0000\u0150\u014e"+
		"\u0001\u0000\u0000\u0000\u0151\u0154\u0001\u0000\u0000\u0000\u0152\u0150"+
		"\u0001\u0000\u0000\u0000\u0152\u0153\u0001\u0000\u0000\u0000\u0153/\u0001"+
		"\u0000\u0000\u0000\u0154\u0152\u0001\u0000\u0000\u0000\u0155\u0156\u0005"+
		"%\u0000\u0000\u0156\u0157\u0005:\u0000\u0000\u01571\u0001\u0000\u0000"+
		"\u0000\u0158\u0159\u0005$\u0000\u0000\u0159\u015a\u0005:\u0000\u0000\u015a"+
		"3\u0001\u0000\u0000\u0000\u015b\u015c\u0003\u0004\u0002\u0000\u015c\u015d"+
		"\u0005\u0002\u0000\u0000\u015d\u015e\u0005<\u0000\u0000\u015e\u0160\u0003"+
		"\n\u0005\u0000\u015f\u0161\u0003\f\u0006\u0000\u0160\u015f\u0001\u0000"+
		"\u0000\u0000\u0160\u0161\u0001\u0000\u0000\u0000\u0161\u0168\u0001\u0000"+
		"\u0000\u0000\u0162\u0163\u0005<\u0000\u0000\u0163\u0165\u0003\n\u0005"+
		"\u0000\u0164\u0166\u0003\f\u0006\u0000\u0165\u0164\u0001\u0000\u0000\u0000"+
		"\u0165\u0166\u0001\u0000\u0000\u0000\u0166\u0168\u0001\u0000\u0000\u0000"+
		"\u0167\u015b\u0001\u0000\u0000\u0000\u0167\u0162\u0001\u0000\u0000\u0000"+
		"\u01685\u0001\u0000\u0000\u0000\u0169\u016a\u0005/\u0000\u0000\u016a\u016b"+
		"\u0005\u0012\u0000\u0000\u016b\u0170\u00034\u001a\u0000\u016c\u016d\u0005"+
		"\u0003\u0000\u0000\u016d\u016f\u00034\u001a\u0000\u016e\u016c\u0001\u0000"+
		"\u0000\u0000\u016f\u0172\u0001\u0000\u0000\u0000\u0170\u016e\u0001\u0000"+
		"\u0000\u0000\u0170\u0171\u0001\u0000\u0000\u0000\u0171\u0173\u0001\u0000"+
		"\u0000\u0000\u0172\u0170\u0001\u0000\u0000\u0000\u0173\u0174\u0005\u0013"+
		"\u0000\u0000\u01747\u0001\u0000\u0000\u0000\u0175\u017a\u0005\u0015\u0000"+
		"\u0000\u0176\u0179\b\u0002\u0000\u0000\u0177\u0179\u00038\u001c\u0000"+
		"\u0178\u0176\u0001\u0000\u0000\u0000\u0178\u0177\u0001\u0000\u0000\u0000"+
		"\u0179\u017c\u0001\u0000\u0000\u0000\u017a\u0178\u0001\u0000\u0000\u0000"+
		"\u017a\u017b\u0001\u0000\u0000\u0000\u017b\u017d\u0001\u0000\u0000\u0000"+
		"\u017c\u017a\u0001\u0000\u0000\u0000\u017d\u017e\u0005\u0016\u0000\u0000"+
		"\u017e9\u0001\u0000\u0000\u0000\u017f\u0184\u0005<\u0000\u0000\u0180\u0181"+
		"\u0005\u0002\u0000\u0000\u0181\u0183\u0005<\u0000\u0000\u0182\u0180\u0001"+
		"\u0000\u0000\u0000\u0183\u0186\u0001\u0000\u0000\u0000\u0184\u0182\u0001"+
		"\u0000\u0000\u0000\u0184\u0185\u0001\u0000\u0000\u0000\u0185\u018a\u0001"+
		"\u0000\u0000\u0000\u0186\u0184\u0001\u0000\u0000\u0000\u0187\u0188\u0005"+
		"<\u0000\u0000\u0188\u018a\u00038\u001c\u0000\u0189\u017f\u0001\u0000\u0000"+
		"\u0000\u0189\u0187\u0001\u0000\u0000\u0000\u018a;\u0001\u0000\u0000\u0000"+
		"\u018b\u018c\u00050\u0000\u0000\u018c\u018d\u0005\u0012\u0000\u0000\u018d"+
		"\u0192\u0003:\u001d\u0000\u018e\u018f\u0005\u0003\u0000\u0000\u018f\u0191"+
		"\u0003:\u001d\u0000\u0190\u018e\u0001\u0000\u0000\u0000\u0191\u0194\u0001"+
		"\u0000\u0000\u0000\u0192\u0190\u0001\u0000\u0000\u0000\u0192\u0193\u0001"+
		"\u0000\u0000\u0000\u0193\u0195\u0001\u0000\u0000\u0000\u0194\u0192\u0001"+
		"\u0000\u0000\u0000\u0195\u0196\u0005\u0013\u0000\u0000\u0196=\u0001\u0000"+
		"\u0000\u0000\u0197\u019c\u0005<\u0000\u0000\u0198\u0199\u0005\u0003\u0000"+
		"\u0000\u0199\u019b\u0005<\u0000\u0000\u019a\u0198\u0001\u0000\u0000\u0000"+
		"\u019b\u019e\u0001\u0000\u0000\u0000\u019c\u019a\u0001\u0000\u0000\u0000"+
		"\u019c\u019d\u0001\u0000\u0000\u0000\u019d?\u0001\u0000\u0000\u0000\u019e"+
		"\u019c\u0001\u0000\u0000\u0000\u019f\u01a0\u0005,\u0000\u0000\u01a0\u01a1"+
		"\u0003>\u001f\u0000\u01a1A\u0001\u0000\u0000\u0000\u01a2\u01a5\u0003\u0004"+
		"\u0002\u0000\u01a3\u01a4\u0005\u0002\u0000\u0000\u01a4\u01a6\u0005<\u0000"+
		"\u0000\u01a5\u01a3\u0001\u0000\u0000\u0000\u01a6\u01a7\u0001\u0000\u0000"+
		"\u0000\u01a7\u01a5\u0001\u0000\u0000\u0000\u01a7\u01a8\u0001\u0000\u0000"+
		"\u0000\u01a8C\u0001\u0000\u0000\u0000\u01a9\u01aa\u0005&\u0000\u0000\u01aa"+
		"\u01b1\u0003B!\u0000\u01ab\u01ad\u00051\u0000\u0000\u01ac\u01ae\u0003"+
		"$\u0012\u0000\u01ad\u01ac\u0001\u0000\u0000\u0000\u01ae\u01af\u0001\u0000"+
		"\u0000\u0000\u01af\u01ad\u0001\u0000\u0000\u0000\u01af\u01b0\u0001\u0000"+
		"\u0000\u0000\u01b0\u01b2\u0001\u0000\u0000\u0000\u01b1\u01ab\u0001\u0000"+
		"\u0000\u0000\u01b1\u01b2\u0001\u0000\u0000\u0000\u01b2E\u0001\u0000\u0000"+
		"\u0000\u01b3\u01b8\u0005\u0015\u0000\u0000\u01b4\u01b7\b\u0002\u0000\u0000"+
		"\u01b5\u01b7\u0003F#\u0000\u01b6\u01b4\u0001\u0000\u0000\u0000\u01b6\u01b5"+
		"\u0001\u0000\u0000\u0000\u01b7\u01ba\u0001\u0000\u0000\u0000\u01b8\u01b6"+
		"\u0001\u0000\u0000\u0000\u01b8\u01b9\u0001\u0000\u0000\u0000\u01b9\u01bb"+
		"\u0001\u0000\u0000\u0000\u01ba\u01b8\u0001\u0000\u0000\u0000\u01bb\u01bc"+
		"\u0005\u0016\u0000\u0000\u01bcG\u0001\u0000\u0000\u0000\u01bd\u01be\u0005"+
		"<\u0000\u0000\u01be\u01bf\u0003F#\u0000\u01bfI\u0001\u0000\u0000\u0000"+
		"\u01c0\u01c1\u0005#\u0000\u0000\u01c1\u01c6\u0003H$\u0000\u01c2\u01c3"+
		"\u0005\u0003\u0000\u0000\u01c3\u01c5\u0003H$\u0000\u01c4\u01c2\u0001\u0000"+
		"\u0000\u0000\u01c5\u01c8\u0001\u0000\u0000\u0000\u01c6\u01c4\u0001\u0000"+
		"\u0000\u0000\u01c6\u01c7\u0001\u0000\u0000\u0000\u01c7K\u0001\u0000\u0000"+
		"\u0000\u01c8\u01c6\u0001\u0000\u0000\u0000\u01c9\u01ca\u0005<\u0000\u0000"+
		"\u01caM\u0001\u0000\u0000\u0000\u01cb\u01cc\u0005>\u0000\u0000\u01ccO"+
		"\u0001\u0000\u0000\u0000\u01cd\u01ce\u0005-\u0000\u0000\u01ce\u01cf\u0003"+
		"N\'\u0000\u01cfQ\u0001\u0000\u0000\u0000\u01d0\u01d1\u0007\u0003\u0000"+
		"\u0000\u01d1\u01d2\u0005!\u0000\u0000\u01d2\u01d3\u0003&\u0013\u0000\u01d3"+
		"\u01d5\u0005\"\u0000\u0000\u01d4\u01d6\u0003$\u0012\u0000\u01d5\u01d4"+
		"\u0001\u0000\u0000\u0000\u01d6\u01d7\u0001\u0000\u0000\u0000\u01d7\u01d5"+
		"\u0001\u0000\u0000\u0000\u01d7\u01d8\u0001\u0000\u0000\u0000\u01d8S\u0001"+
		"\u0000\u0000\u0000\u01d9\u01da\u0005&\u0000\u0000\u01da\u01e1\u0003*\u0015"+
		"\u0000\u01db\u01dd\u00051\u0000\u0000\u01dc\u01de\u0003$\u0012\u0000\u01dd"+
		"\u01dc\u0001\u0000\u0000\u0000\u01de\u01df\u0001\u0000\u0000\u0000\u01df"+
		"\u01dd\u0001\u0000\u0000\u0000\u01df\u01e0\u0001\u0000\u0000\u0000\u01e0"+
		"\u01e2\u0001\u0000\u0000\u0000\u01e1\u01db\u0001\u0000\u0000\u0000\u01e1"+
		"\u01e2\u0001\u0000\u0000\u0000\u01e2\u01e4\u0001\u0000\u0000\u0000\u01e3"+
		"\u01e5\u00036\u001b\u0000\u01e4\u01e3\u0001\u0000\u0000\u0000\u01e4\u01e5"+
		"\u0001\u0000\u0000\u0000\u01e5\u01e7\u0001\u0000\u0000\u0000\u01e6\u01e8"+
		"\u0003<\u001e\u0000\u01e7\u01e6\u0001\u0000\u0000\u0000\u01e7\u01e8\u0001"+
		"\u0000\u0000\u0000\u01e8\u01ea\u0001\u0000\u0000\u0000\u01e9\u01eb\u0003"+
		"@ \u0000\u01ea\u01e9\u0001\u0000\u0000\u0000\u01ea\u01eb\u0001\u0000\u0000"+
		"\u0000\u01eb\u01ed\u0001\u0000\u0000\u0000\u01ec\u01ee\u0003.\u0017\u0000"+
		"\u01ed\u01ec\u0001\u0000\u0000\u0000\u01ed\u01ee\u0001\u0000\u0000\u0000"+
		"\u01ee\u01f0\u0001\u0000\u0000\u0000\u01ef\u01f1\u00030\u0018\u0000\u01f0"+
		"\u01ef\u0001\u0000\u0000\u0000\u01f0\u01f1\u0001\u0000\u0000\u0000\u01f1"+
		"\u01f3\u0001\u0000\u0000\u0000\u01f2\u01f4\u00032\u0019\u0000\u01f3\u01f2"+
		"\u0001\u0000\u0000\u0000\u01f3\u01f4\u0001\u0000\u0000\u0000\u01f4\u01f6"+
		"\u0001\u0000\u0000\u0000\u01f5\u01f7\u0003J%\u0000\u01f6\u01f5\u0001\u0000"+
		"\u0000\u0000\u01f6\u01f7\u0001\u0000\u0000\u0000\u01f7\u01f9\u0001\u0000"+
		"\u0000\u0000\u01f8\u01fa\u0003P(\u0000\u01f9\u01f8\u0001\u0000\u0000\u0000"+
		"\u01f9\u01fa\u0001\u0000\u0000\u0000\u01faU\u0001\u0000\u0000\u0000\u01fb"+
		"\u01fc\u0005(\u0000\u0000\u01fc\u0203\u0003*\u0015\u0000\u01fd\u01ff\u0005"+
		"1\u0000\u0000\u01fe\u0200\u0003$\u0012\u0000\u01ff\u01fe\u0001\u0000\u0000"+
		"\u0000\u0200\u0201\u0001\u0000\u0000\u0000\u0201\u01ff\u0001\u0000\u0000"+
		"\u0000\u0201\u0202\u0001\u0000\u0000\u0000\u0202\u0204\u0001\u0000\u0000"+
		"\u0000\u0203\u01fd\u0001\u0000\u0000\u0000\u0203\u0204\u0001\u0000\u0000"+
		"\u0000\u0204\u0206\u0001\u0000\u0000\u0000\u0205\u0207\u00036\u001b\u0000"+
		"\u0206\u0205\u0001\u0000\u0000\u0000\u0206\u0207\u0001\u0000\u0000\u0000"+
		"\u0207\u0209\u0001\u0000\u0000\u0000\u0208\u020a\u0003@ \u0000\u0209\u0208"+
		"\u0001\u0000\u0000\u0000\u0209\u020a\u0001\u0000\u0000\u0000\u020a\u020c"+
		"\u0001\u0000\u0000\u0000\u020b\u020d\u0003.\u0017\u0000\u020c\u020b\u0001"+
		"\u0000\u0000\u0000\u020c\u020d\u0001\u0000\u0000\u0000\u020d\u020f\u0001"+
		"\u0000\u0000\u0000\u020e\u0210\u00030\u0018\u0000\u020f\u020e\u0001\u0000"+
		"\u0000\u0000\u020f\u0210\u0001\u0000\u0000\u0000\u0210\u0212\u0001\u0000"+
		"\u0000\u0000\u0211\u0213\u00032\u0019\u0000\u0212\u0211\u0001\u0000\u0000"+
		"\u0000\u0212\u0213\u0001\u0000\u0000\u0000\u0213\u0215\u0001\u0000\u0000"+
		"\u0000\u0214\u0216\u0003P(\u0000\u0215\u0214\u0001\u0000\u0000\u0000\u0215"+
		"\u0216\u0001\u0000\u0000\u0000\u0216W\u0001\u0000\u0000\u0000\u0217\u0218"+
		"\u0005<\u0000\u0000\u0218Y\u0001\u0000\u0000\u0000\u0219\u021a\u0005\u0017"+
		"\u0000\u0000\u021a\u021b\u0003X,\u0000\u021b[\u0001\u0000\u0000\u0000"+
		"\u021c\u021d\u0005)\u0000\u0000\u021d\u021e\u0003&\u0013\u0000\u021e\u0225"+
		"\u0003Z-\u0000\u021f\u0221\u00051\u0000\u0000\u0220\u0222\u0003$\u0012"+
		"\u0000\u0221\u0220\u0001\u0000\u0000\u0000\u0222\u0223\u0001\u0000\u0000"+
		"\u0000\u0223\u0221\u0001\u0000\u0000\u0000\u0223\u0224\u0001\u0000\u0000"+
		"\u0000\u0224\u0226\u0001\u0000\u0000\u0000\u0225\u021f\u0001\u0000\u0000"+
		"\u0000\u0225\u0226\u0001\u0000\u0000\u0000\u0226\u0228\u0001\u0000\u0000"+
		"\u0000\u0227\u0229\u0003.\u0017\u0000\u0228\u0227\u0001\u0000\u0000\u0000"+
		"\u0228\u0229\u0001\u0000\u0000\u0000\u0229\u022b\u0001\u0000\u0000\u0000"+
		"\u022a\u022c\u00030\u0018\u0000\u022b\u022a\u0001\u0000\u0000\u0000\u022b"+
		"\u022c\u0001\u0000\u0000\u0000\u022c\u022e\u0001\u0000\u0000\u0000\u022d"+
		"\u022f\u00032\u0019\u0000\u022e\u022d\u0001\u0000\u0000\u0000\u022e\u022f"+
		"\u0001\u0000\u0000\u0000\u022f\u0231\u0001\u0000\u0000\u0000\u0230\u0232"+
		"\u0003P(\u0000\u0231\u0230\u0001\u0000\u0000\u0000\u0231\u0232\u0001\u0000"+
		"\u0000\u0000\u0232]\u0001\u0000\u0000\u0000\u0233\u0234\u0005*\u0000\u0000"+
		"\u0234\u0237\u0003`0\u0000\u0235\u0236\u00052\u0000\u0000\u0236\u0238"+
		"\u0003b1\u0000\u0237\u0235\u0001\u0000\u0000\u0000\u0237\u0238\u0001\u0000"+
		"\u0000\u0000\u0238\u023a\u0001\u0000\u0000\u0000\u0239\u023b\u00036\u001b"+
		"\u0000\u023a\u0239\u0001\u0000\u0000\u0000\u023a\u023b\u0001\u0000\u0000"+
		"\u0000\u023b\u023d\u0001\u0000\u0000\u0000\u023c\u023e\u00030\u0018\u0000"+
		"\u023d\u023c\u0001\u0000\u0000\u0000\u023d\u023e\u0001\u0000\u0000\u0000"+
		"\u023e\u0240\u0001\u0000\u0000\u0000\u023f\u0241\u00032\u0019\u0000\u0240"+
		"\u023f\u0001\u0000\u0000\u0000\u0240\u0241\u0001\u0000\u0000\u0000\u0241"+
		"_\u0001\u0000\u0000\u0000\u0242\u0243\u0005>\u0000\u0000\u0243a\u0001"+
		"\u0000\u0000\u0000\u0244\u024d\u0005<\u0000\u0000\u0245\u0248\u0005<\u0000"+
		"\u0000\u0246\u0247\u0005\u0003\u0000\u0000\u0247\u0249\u0005<\u0000\u0000"+
		"\u0248\u0246\u0001\u0000\u0000\u0000\u0249\u024a\u0001\u0000\u0000\u0000"+
		"\u024a\u0248\u0001\u0000\u0000\u0000\u024a\u024b\u0001\u0000\u0000\u0000"+
		"\u024b\u024d\u0001\u0000\u0000\u0000\u024c\u0244\u0001\u0000\u0000\u0000"+
		"\u024c\u0245\u0001\u0000\u0000\u0000\u024dc\u0001\u0000\u0000\u0000\u024e"+
		"\u024f\u0007\u0004\u0000\u0000\u024fe\u0001\u0000\u0000\u0000Kkt~\u0080"+
		"\u0087\u0095\u009a\u00a2\u00af\u00ba\u00bf\u00c2\u00cd\u00d2\u00d4\u00e0"+
		"\u00ed\u0101\u0106\u010e\u011a\u0122\u0126\u0132\u013a\u0144\u0147\u0152"+
		"\u0160\u0165\u0167\u0170\u0178\u017a\u0184\u0189\u0192\u019c\u01a7\u01af"+
		"\u01b1\u01b6\u01b8\u01c6\u01d7\u01df\u01e1\u01e4\u01e7\u01ea\u01ed\u01f0"+
		"\u01f3\u01f6\u01f9\u0201\u0203\u0206\u0209\u020c\u020f\u0212\u0215\u0223"+
		"\u0225\u0228\u022b\u022e\u0231\u0237\u023a\u023d\u0240\u024a\u024c";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}