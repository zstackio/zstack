// Generated from ZQL.g4 by ANTLR 4.7

package org.zstack.zql.antlr4;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ZQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, FILTER_BY=24, 
		OFFSET=25, LIMIT=26, QUERY=27, GET=28, COUNT=29, SUM=30, DISTINCT=31, 
		ORDER_BY=32, GROUP_BY=33, NAMED_AS=34, ORDER_BY_VALUE=35, RESTRICT_BY=36, 
		RETURN_WITH=37, WHERE=38, AND=39, OR=40, ASC=41, DESC=42, INPUT=43, OUTPUT=44, 
		BOOLEAN=45, INT=46, FLOAT=47, ID=48, WS=49, STRING=50;
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
		RULE_sumBy = 42, RULE_sum = 43;
	public static final String[] ruleNames = {
		"zqls", "zql", "entity", "field", "multiFields", "operator", "value", 
		"listValue", "logicalOperator", "complexValue", "getQuery", "apiparams", 
		"input", "output", "expr", "equal", "condition", "queryTarget", "function", 
		"queryTargetWithFunction", "orderByExpr", "orderBy", "limit", "offset", 
		"restrictByExpr", "restrictBy", "returnWithExprBlock", "returnWithExpr", 
		"returnWith", "groupByExpr", "groupBy", "subQueryTarget", "subQuery", 
		"filterByExprBlock", "filterByExpr", "filterBy", "namedAsKey", "namedAsValue", 
		"namedAs", "query", "count", "sumByValue", "sumBy", "sum"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", 
		"'is null'", "'is not null'", "'in'", "'not in'", "'like'", "'not like'", 
		"'has'", "'not has'", "'('", "')'", "'list('", "'{'", "'}'", "'by'", "'filter by'", 
		"'offset'", "'limit'", "'query'", "'getapi'", "'count'", "'sum'", "'distinct'", 
		"'order by'", "'group by'", "'named as'", null, "'restrict by'", "'return with'", 
		"'where'", "'and'", "'or'", "'asc'", "'desc'", "'api'", "'output'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		"FILTER_BY", "OFFSET", "LIMIT", "QUERY", "GET", "COUNT", "SUM", "DISTINCT", 
		"ORDER_BY", "GROUP_BY", "NAMED_AS", "ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", 
		"WHERE", "AND", "OR", "ASC", "DESC", "INPUT", "OUTPUT", "BOOLEAN", "INT", 
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
			setState(88);
			zql();
			setState(93);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(89);
				match(T__0);
				setState(90);
				zql();
				}
				}
				setState(95);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(96);
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
			setState(101);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QUERY:
				_localctx = new QueryGrammarContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(98);
				query();
				}
				break;
			case COUNT:
				_localctx = new CountGrammarContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(99);
				count();
				}
				break;
			case SUM:
				_localctx = new SumGrammarContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(100);
				sum();
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
			setState(103);
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
			setState(113);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(105);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(106);
				match(ID);
				setState(109); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(107);
					match(T__1);
					setState(108);
					match(ID);
					}
					}
					setState(111); 
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
			setState(115);
			match(ID);
			setState(118); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(116);
				match(T__2);
				setState(117);
				match(ID);
				}
				}
				setState(120); 
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
			setState(122);
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
			setState(139);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(124);
				match(STRING);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 2);
				{
				setState(125);
				match(INT);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(126);
				match(FLOAT);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(127);
				match(BOOLEAN);
				}
				break;
			case T__17:
				enterOuterAlt(_localctx, 5);
				{
				setState(128);
				match(T__17);
				setState(129);
				value();
				setState(134);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(130);
					match(T__2);
					setState(131);
					value();
					}
					}
					setState(136);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(137);
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
			setState(141);
			match(T__19);
			setState(142);
			value();
			setState(147);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(143);
				match(T__2);
				setState(144);
				value();
				}
				}
				setState(149);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(150);
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
			setState(152);
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
			setState(197);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				_localctx = new SimpleValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(154);
				value();
				}
				break;
			case 2:
				_localctx = new SubQueryValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(155);
				match(T__17);
				setState(156);
				subQuery();
				setState(157);
				match(T__18);
				}
				break;
			case 3:
				_localctx = new ApiGetValueContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(160);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(159);
					match(T__17);
					}
				}

				setState(162);
				getQuery();
				setState(163);
				match(T__17);
				setState(164);
				input();
				setState(165);
				match(T__2);
				setState(166);
				output();
				setState(171);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(167);
					match(T__2);
					setState(168);
					apiparams();
					}
					}
					setState(173);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(174);
				match(T__18);
				setState(176);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(175);
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
				setState(179);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(178);
					match(T__17);
					}
				}

				setState(181);
				getQuery();
				setState(182);
				match(T__17);
				setState(183);
				output();
				setState(184);
				match(T__2);
				setState(185);
				input();
				setState(190);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(186);
					match(T__2);
					setState(187);
					apiparams();
					}
					}
					setState(192);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(193);
				match(T__18);
				setState(195);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(194);
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
			setState(199);
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
			setState(209);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(201);
				namedAsKey();
				setState(202);
				equal();
				setState(203);
				value();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(205);
				namedAsKey();
				setState(206);
				equal();
				setState(207);
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
			setState(211);
			match(INPUT);
			setState(212);
			equal();
			setState(213);
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
			setState(215);
			match(OUTPUT);
			setState(216);
			equal();
			setState(217);
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
			setState(219);
			field();
			setState(220);
			operator();
			setState(222);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(221);
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
			setState(224);
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
			setState(232);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__17:
				{
				_localctx = new ParenthesisConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(227);
				match(T__17);
				setState(228);
				condition(0);
				setState(229);
				match(T__18);
				}
				break;
			case ID:
				{
				_localctx = new SimpleConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(231);
				expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(244);
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
					setState(234);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(238); 
					_errHandler.sync(this);
					_alt = 1;
					do {
						switch (_alt) {
						case 1:
							{
							{
							setState(235);
							((NestConditionContext)_localctx).op = logicalOperator();
							setState(236);
							((NestConditionContext)_localctx).right = condition(0);
							}
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						setState(240); 
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
					} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
					}
					} 
				}
				setState(246);
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
			setState(256);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				_localctx = new OnlyEntityContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(247);
				entity();
				}
				break;
			case 2:
				_localctx = new WithSingleFieldContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(248);
				entity();
				setState(249);
				match(T__1);
				setState(250);
				field();
				}
				break;
			case 3:
				_localctx = new WithMultiFieldsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(252);
				entity();
				setState(253);
				match(T__1);
				setState(254);
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
			setState(258);
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
			setState(266);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				_localctx = new WithoutFunctionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(260);
				queryTarget();
				}
				break;
			case DISTINCT:
				_localctx = new WithFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(261);
				function();
				setState(262);
				match(T__17);
				setState(263);
				queryTargetWithFunction();
				setState(264);
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
			setState(268);
			match(ID);
			setState(269);
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
			setState(271);
			match(ORDER_BY);
			setState(272);
			orderByExpr();
			setState(277);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(273);
				match(T__2);
				setState(274);
				orderByExpr();
				}
				}
				setState(279);
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
			setState(280);
			match(LIMIT);
			setState(281);
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
			setState(283);
			match(OFFSET);
			setState(284);
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
			setState(298);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(286);
				entity();
				setState(287);
				match(T__1);
				setState(288);
				match(ID);
				setState(289);
				operator();
				setState(291);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__17) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(290);
					value();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(293);
				match(ID);
				setState(294);
				operator();
				setState(296);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__17) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(295);
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
			setState(300);
			match(RESTRICT_BY);
			setState(301);
			match(T__17);
			setState(302);
			restrictByExpr();
			setState(307);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(303);
				match(T__2);
				setState(304);
				restrictByExpr();
				}
				}
				setState(309);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(310);
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
			setState(312);
			match(T__20);
			setState(317);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__22) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << GET) | (1L << COUNT) | (1L << SUM) | (1L << DISTINCT) | (1L << ORDER_BY) | (1L << GROUP_BY) | (1L << NAMED_AS) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << INPUT) | (1L << OUTPUT) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(315);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
				case 1:
					{
					setState(313);
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
					setState(314);
					returnWithExprBlock();
					}
					break;
				}
				}
				setState(319);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(320);
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
			setState(332);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				_localctx = new ReturnWithExprIdContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(322);
				match(ID);
				setState(327);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(323);
					match(T__1);
					setState(324);
					match(ID);
					}
					}
					setState(329);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				_localctx = new ReturnWithExprFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(330);
				match(ID);
				setState(331);
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
			setState(334);
			match(RETURN_WITH);
			setState(335);
			match(T__17);
			setState(336);
			returnWithExpr();
			setState(341);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(337);
				match(T__2);
				setState(338);
				returnWithExpr();
				}
				}
				setState(343);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(344);
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
			setState(346);
			match(ID);
			setState(351);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(347);
				match(T__2);
				setState(348);
				match(ID);
				}
				}
				setState(353);
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
			setState(354);
			match(GROUP_BY);
			setState(355);
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
			setState(357);
			entity();
			setState(360); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(358);
				match(T__1);
				setState(359);
				match(ID);
				}
				}
				setState(362); 
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
			setState(364);
			match(QUERY);
			setState(365);
			subQueryTarget();
			setState(372);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(366);
				match(WHERE);
				setState(368); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(367);
					condition(0);
					}
					}
					setState(370); 
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
			setState(374);
			match(T__20);
			setState(379);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__22) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << GET) | (1L << COUNT) | (1L << SUM) | (1L << DISTINCT) | (1L << ORDER_BY) | (1L << GROUP_BY) | (1L << NAMED_AS) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << INPUT) | (1L << OUTPUT) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(377);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
				case 1:
					{
					setState(375);
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
					setState(376);
					filterByExprBlock();
					}
					break;
				}
				}
				setState(381);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(382);
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
			setState(384);
			match(ID);
			setState(385);
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
			setState(387);
			match(FILTER_BY);
			setState(388);
			filterByExpr();
			setState(393);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(389);
				match(T__2);
				setState(390);
				filterByExpr();
				}
				}
				setState(395);
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
			setState(396);
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
			setState(398);
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
			setState(400);
			match(NAMED_AS);
			setState(401);
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
			setState(403);
			match(QUERY);
			setState(404);
			queryTargetWithFunction();
			setState(411);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(405);
				match(WHERE);
				setState(407); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(406);
					condition(0);
					}
					}
					setState(409); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(414);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(413);
				restrictBy();
				}
			}

			setState(417);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RETURN_WITH) {
				{
				setState(416);
				returnWith();
				}
			}

			setState(420);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP_BY) {
				{
				setState(419);
				groupBy();
				}
			}

			setState(423);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(422);
				orderBy();
				}
			}

			setState(426);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(425);
				limit();
				}
			}

			setState(429);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(428);
				offset();
				}
			}

			setState(432);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FILTER_BY) {
				{
				setState(431);
				filterBy();
				}
			}

			setState(435);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(434);
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
			setState(437);
			match(COUNT);
			setState(438);
			queryTargetWithFunction();
			setState(445);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(439);
				match(WHERE);
				setState(441); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(440);
					condition(0);
					}
					}
					setState(443); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(448);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(447);
				restrictBy();
				}
			}

			setState(451);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP_BY) {
				{
				setState(450);
				groupBy();
				}
			}

			setState(454);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(453);
				orderBy();
				}
			}

			setState(457);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(456);
				limit();
				}
			}

			setState(460);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(459);
				offset();
				}
			}

			setState(463);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(462);
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
			setState(465);
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
			setState(467);
			match(T__22);
			setState(468);
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
			setState(470);
			match(SUM);
			setState(471);
			queryTarget();
			setState(472);
			sumBy();
			setState(479);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(473);
				match(WHERE);
				setState(475); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(474);
					condition(0);
					}
					}
					setState(477); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__17 || _la==ID );
				}
			}

			setState(482);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(481);
				orderBy();
				}
			}

			setState(485);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(484);
				limit();
				}
			}

			setState(488);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(487);
				offset();
				}
			}

			setState(491);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(490);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\64\u01f0\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\3\2\3\2\3\2\7\2^\n\2\f\2\16\2a\13\2\3\2\3\2\3\3\3\3\3\3\5\3"+
		"h\n\3\3\4\3\4\3\5\3\5\3\5\3\5\6\5p\n\5\r\5\16\5q\5\5t\n\5\3\6\3\6\3\6"+
		"\6\6y\n\6\r\6\16\6z\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\7\b\u0087"+
		"\n\b\f\b\16\b\u008a\13\b\3\b\3\b\5\b\u008e\n\b\3\t\3\t\3\t\3\t\7\t\u0094"+
		"\n\t\f\t\16\t\u0097\13\t\3\t\3\t\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13"+
		"\5\13\u00a3\n\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13\u00ac\n\13\f"+
		"\13\16\13\u00af\13\13\3\13\3\13\5\13\u00b3\n\13\3\13\5\13\u00b6\n\13\3"+
		"\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13\u00bf\n\13\f\13\16\13\u00c2\13"+
		"\13\3\13\3\13\5\13\u00c6\n\13\5\13\u00c8\n\13\3\f\3\f\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\5\r\u00d4\n\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17"+
		"\3\20\3\20\3\20\5\20\u00e1\n\20\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22"+
		"\5\22\u00eb\n\22\3\22\3\22\3\22\3\22\6\22\u00f1\n\22\r\22\16\22\u00f2"+
		"\7\22\u00f5\n\22\f\22\16\22\u00f8\13\22\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\5\23\u0103\n\23\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25"+
		"\5\25\u010d\n\25\3\26\3\26\3\26\3\27\3\27\3\27\3\27\7\27\u0116\n\27\f"+
		"\27\16\27\u0119\13\27\3\30\3\30\3\30\3\31\3\31\3\31\3\32\3\32\3\32\3\32"+
		"\3\32\5\32\u0126\n\32\3\32\3\32\3\32\5\32\u012b\n\32\5\32\u012d\n\32\3"+
		"\33\3\33\3\33\3\33\3\33\7\33\u0134\n\33\f\33\16\33\u0137\13\33\3\33\3"+
		"\33\3\34\3\34\3\34\7\34\u013e\n\34\f\34\16\34\u0141\13\34\3\34\3\34\3"+
		"\35\3\35\3\35\7\35\u0148\n\35\f\35\16\35\u014b\13\35\3\35\3\35\5\35\u014f"+
		"\n\35\3\36\3\36\3\36\3\36\3\36\7\36\u0156\n\36\f\36\16\36\u0159\13\36"+
		"\3\36\3\36\3\37\3\37\3\37\7\37\u0160\n\37\f\37\16\37\u0163\13\37\3 \3"+
		" \3 \3!\3!\3!\6!\u016b\n!\r!\16!\u016c\3\"\3\"\3\"\3\"\6\"\u0173\n\"\r"+
		"\"\16\"\u0174\5\"\u0177\n\"\3#\3#\3#\7#\u017c\n#\f#\16#\u017f\13#\3#\3"+
		"#\3$\3$\3$\3%\3%\3%\3%\7%\u018a\n%\f%\16%\u018d\13%\3&\3&\3\'\3\'\3(\3"+
		"(\3(\3)\3)\3)\3)\6)\u019a\n)\r)\16)\u019b\5)\u019e\n)\3)\5)\u01a1\n)\3"+
		")\5)\u01a4\n)\3)\5)\u01a7\n)\3)\5)\u01aa\n)\3)\5)\u01ad\n)\3)\5)\u01b0"+
		"\n)\3)\5)\u01b3\n)\3)\5)\u01b6\n)\3*\3*\3*\3*\6*\u01bc\n*\r*\16*\u01bd"+
		"\5*\u01c0\n*\3*\5*\u01c3\n*\3*\5*\u01c6\n*\3*\5*\u01c9\n*\3*\5*\u01cc"+
		"\n*\3*\5*\u01cf\n*\3*\5*\u01d2\n*\3+\3+\3,\3,\3,\3-\3-\3-\3-\3-\6-\u01de"+
		"\n-\r-\16-\u01df\5-\u01e2\n-\3-\5-\u01e5\n-\3-\5-\u01e8\n-\3-\5-\u01eb"+
		"\n-\3-\5-\u01ee\n-\3-\2\3\".\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \""+
		"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVX\2\5\3\2\6\23\3\2)*\3\2\30\30\2\u0209"+
		"\2Z\3\2\2\2\4g\3\2\2\2\6i\3\2\2\2\bs\3\2\2\2\nu\3\2\2\2\f|\3\2\2\2\16"+
		"\u008d\3\2\2\2\20\u008f\3\2\2\2\22\u009a\3\2\2\2\24\u00c7\3\2\2\2\26\u00c9"+
		"\3\2\2\2\30\u00d3\3\2\2\2\32\u00d5\3\2\2\2\34\u00d9\3\2\2\2\36\u00dd\3"+
		"\2\2\2 \u00e2\3\2\2\2\"\u00ea\3\2\2\2$\u0102\3\2\2\2&\u0104\3\2\2\2(\u010c"+
		"\3\2\2\2*\u010e\3\2\2\2,\u0111\3\2\2\2.\u011a\3\2\2\2\60\u011d\3\2\2\2"+
		"\62\u012c\3\2\2\2\64\u012e\3\2\2\2\66\u013a\3\2\2\28\u014e\3\2\2\2:\u0150"+
		"\3\2\2\2<\u015c\3\2\2\2>\u0164\3\2\2\2@\u0167\3\2\2\2B\u016e\3\2\2\2D"+
		"\u0178\3\2\2\2F\u0182\3\2\2\2H\u0185\3\2\2\2J\u018e\3\2\2\2L\u0190\3\2"+
		"\2\2N\u0192\3\2\2\2P\u0195\3\2\2\2R\u01b7\3\2\2\2T\u01d3\3\2\2\2V\u01d5"+
		"\3\2\2\2X\u01d8\3\2\2\2Z_\5\4\3\2[\\\7\3\2\2\\^\5\4\3\2][\3\2\2\2^a\3"+
		"\2\2\2_]\3\2\2\2_`\3\2\2\2`b\3\2\2\2a_\3\2\2\2bc\7\2\2\3c\3\3\2\2\2dh"+
		"\5P)\2eh\5R*\2fh\5X-\2gd\3\2\2\2ge\3\2\2\2gf\3\2\2\2h\5\3\2\2\2ij\7\62"+
		"\2\2j\7\3\2\2\2kt\7\62\2\2lo\7\62\2\2mn\7\4\2\2np\7\62\2\2om\3\2\2\2p"+
		"q\3\2\2\2qo\3\2\2\2qr\3\2\2\2rt\3\2\2\2sk\3\2\2\2sl\3\2\2\2t\t\3\2\2\2"+
		"ux\7\62\2\2vw\7\5\2\2wy\7\62\2\2xv\3\2\2\2yz\3\2\2\2zx\3\2\2\2z{\3\2\2"+
		"\2{\13\3\2\2\2|}\t\2\2\2}\r\3\2\2\2~\u008e\7\64\2\2\177\u008e\7\60\2\2"+
		"\u0080\u008e\7\61\2\2\u0081\u008e\7/\2\2\u0082\u0083\7\24\2\2\u0083\u0088"+
		"\5\16\b\2\u0084\u0085\7\5\2\2\u0085\u0087\5\16\b\2\u0086\u0084\3\2\2\2"+
		"\u0087\u008a\3\2\2\2\u0088\u0086\3\2\2\2\u0088\u0089\3\2\2\2\u0089\u008b"+
		"\3\2\2\2\u008a\u0088\3\2\2\2\u008b\u008c\7\25\2\2\u008c\u008e\3\2\2\2"+
		"\u008d~\3\2\2\2\u008d\177\3\2\2\2\u008d\u0080\3\2\2\2\u008d\u0081\3\2"+
		"\2\2\u008d\u0082\3\2\2\2\u008e\17\3\2\2\2\u008f\u0090\7\26\2\2\u0090\u0095"+
		"\5\16\b\2\u0091\u0092\7\5\2\2\u0092\u0094\5\16\b\2\u0093\u0091\3\2\2\2"+
		"\u0094\u0097\3\2\2\2\u0095\u0093\3\2\2\2\u0095\u0096\3\2\2\2\u0096\u0098"+
		"\3\2\2\2\u0097\u0095\3\2\2\2\u0098\u0099\7\25\2\2\u0099\21\3\2\2\2\u009a"+
		"\u009b\t\3\2\2\u009b\23\3\2\2\2\u009c\u00c8\5\16\b\2\u009d\u009e\7\24"+
		"\2\2\u009e\u009f\5B\"\2\u009f\u00a0\7\25\2\2\u00a0\u00c8\3\2\2\2\u00a1"+
		"\u00a3\7\24\2\2\u00a2\u00a1\3\2\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00a4\3"+
		"\2\2\2\u00a4\u00a5\5\26\f\2\u00a5\u00a6\7\24\2\2\u00a6\u00a7\5\32\16\2"+
		"\u00a7\u00a8\7\5\2\2\u00a8\u00ad\5\34\17\2\u00a9\u00aa\7\5\2\2\u00aa\u00ac"+
		"\5\30\r\2\u00ab\u00a9\3\2\2\2\u00ac\u00af\3\2\2\2\u00ad\u00ab\3\2\2\2"+
		"\u00ad\u00ae\3\2\2\2\u00ae\u00b0\3\2\2\2\u00af\u00ad\3\2\2\2\u00b0\u00b2"+
		"\7\25\2\2\u00b1\u00b3\7\25\2\2\u00b2\u00b1\3\2\2\2\u00b2\u00b3\3\2\2\2"+
		"\u00b3\u00c8\3\2\2\2\u00b4\u00b6\7\24\2\2\u00b5\u00b4\3\2\2\2\u00b5\u00b6"+
		"\3\2\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00b8\5\26\f\2\u00b8\u00b9\7\24\2\2"+
		"\u00b9\u00ba\5\34\17\2\u00ba\u00bb\7\5\2\2\u00bb\u00c0\5\32\16\2\u00bc"+
		"\u00bd\7\5\2\2\u00bd\u00bf\5\30\r\2\u00be\u00bc\3\2\2\2\u00bf\u00c2\3"+
		"\2\2\2\u00c0\u00be\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1\u00c3\3\2\2\2\u00c2"+
		"\u00c0\3\2\2\2\u00c3\u00c5\7\25\2\2\u00c4\u00c6\7\25\2\2\u00c5\u00c4\3"+
		"\2\2\2\u00c5\u00c6\3\2\2\2\u00c6\u00c8\3\2\2\2\u00c7\u009c\3\2\2\2\u00c7"+
		"\u009d\3\2\2\2\u00c7\u00a2\3\2\2\2\u00c7\u00b5\3\2\2\2\u00c8\25\3\2\2"+
		"\2\u00c9\u00ca\7\36\2\2\u00ca\27\3\2\2\2\u00cb\u00cc\5J&\2\u00cc\u00cd"+
		"\5 \21\2\u00cd\u00ce\5\16\b\2\u00ce\u00d4\3\2\2\2\u00cf\u00d0\5J&\2\u00d0"+
		"\u00d1\5 \21\2\u00d1\u00d2\5\20\t\2\u00d2\u00d4\3\2\2\2\u00d3\u00cb\3"+
		"\2\2\2\u00d3\u00cf\3\2\2\2\u00d4\31\3\2\2\2\u00d5\u00d6\7-\2\2\u00d6\u00d7"+
		"\5 \21\2\u00d7\u00d8\5L\'\2\u00d8\33\3\2\2\2\u00d9\u00da\7.\2\2\u00da"+
		"\u00db\5 \21\2\u00db\u00dc\5L\'\2\u00dc\35\3\2\2\2\u00dd\u00de\5\b\5\2"+
		"\u00de\u00e0\5\f\7\2\u00df\u00e1\5\24\13\2\u00e0\u00df\3\2\2\2\u00e0\u00e1"+
		"\3\2\2\2\u00e1\37\3\2\2\2\u00e2\u00e3\7\6\2\2\u00e3!\3\2\2\2\u00e4\u00e5"+
		"\b\22\1\2\u00e5\u00e6\7\24\2\2\u00e6\u00e7\5\"\22\2\u00e7\u00e8\7\25\2"+
		"\2\u00e8\u00eb\3\2\2\2\u00e9\u00eb\5\36\20\2\u00ea\u00e4\3\2\2\2\u00ea"+
		"\u00e9\3\2\2\2\u00eb\u00f6\3\2\2\2\u00ec\u00f0\f\4\2\2\u00ed\u00ee\5\22"+
		"\n\2\u00ee\u00ef\5\"\22\2\u00ef\u00f1\3\2\2\2\u00f0\u00ed\3\2\2\2\u00f1"+
		"\u00f2\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f2\u00f3\3\2\2\2\u00f3\u00f5\3\2"+
		"\2\2\u00f4\u00ec\3\2\2\2\u00f5\u00f8\3\2\2\2\u00f6\u00f4\3\2\2\2\u00f6"+
		"\u00f7\3\2\2\2\u00f7#\3\2\2\2\u00f8\u00f6\3\2\2\2\u00f9\u0103\5\6\4\2"+
		"\u00fa\u00fb\5\6\4\2\u00fb\u00fc\7\4\2\2\u00fc\u00fd\5\b\5\2\u00fd\u0103"+
		"\3\2\2\2\u00fe\u00ff\5\6\4\2\u00ff\u0100\7\4\2\2\u0100\u0101\5\n\6\2\u0101"+
		"\u0103\3\2\2\2\u0102\u00f9\3\2\2\2\u0102\u00fa\3\2\2\2\u0102\u00fe\3\2"+
		"\2\2\u0103%\3\2\2\2\u0104\u0105\7!\2\2\u0105\'\3\2\2\2\u0106\u010d\5$"+
		"\23\2\u0107\u0108\5&\24\2\u0108\u0109\7\24\2\2\u0109\u010a\5(\25\2\u010a"+
		"\u010b\7\25\2\2\u010b\u010d\3\2\2\2\u010c\u0106\3\2\2\2\u010c\u0107\3"+
		"\2\2\2\u010d)\3\2\2\2\u010e\u010f\7\62\2\2\u010f\u0110\7%\2\2\u0110+\3"+
		"\2\2\2\u0111\u0112\7\"\2\2\u0112\u0117\5*\26\2\u0113\u0114\7\5\2\2\u0114"+
		"\u0116\5*\26\2\u0115\u0113\3\2\2\2\u0116\u0119\3\2\2\2\u0117\u0115\3\2"+
		"\2\2\u0117\u0118\3\2\2\2\u0118-\3\2\2\2\u0119\u0117\3\2\2\2\u011a\u011b"+
		"\7\34\2\2\u011b\u011c\7\60\2\2\u011c/\3\2\2\2\u011d\u011e\7\33\2\2\u011e"+
		"\u011f\7\60\2\2\u011f\61\3\2\2\2\u0120\u0121\5\6\4\2\u0121\u0122\7\4\2"+
		"\2\u0122\u0123\7\62\2\2\u0123\u0125\5\f\7\2\u0124\u0126\5\16\b\2\u0125"+
		"\u0124\3\2\2\2\u0125\u0126\3\2\2\2\u0126\u012d\3\2\2\2\u0127\u0128\7\62"+
		"\2\2\u0128\u012a\5\f\7\2\u0129\u012b\5\16\b\2\u012a\u0129\3\2\2\2\u012a"+
		"\u012b\3\2\2\2\u012b\u012d\3\2\2\2\u012c\u0120\3\2\2\2\u012c\u0127\3\2"+
		"\2\2\u012d\63\3\2\2\2\u012e\u012f\7&\2\2\u012f\u0130\7\24\2\2\u0130\u0135"+
		"\5\62\32\2\u0131\u0132\7\5\2\2\u0132\u0134\5\62\32\2\u0133\u0131\3\2\2"+
		"\2\u0134\u0137\3\2\2\2\u0135\u0133\3\2\2\2\u0135\u0136\3\2\2\2\u0136\u0138"+
		"\3\2\2\2\u0137\u0135\3\2\2\2\u0138\u0139\7\25\2\2\u0139\65\3\2\2\2\u013a"+
		"\u013f\7\27\2\2\u013b\u013e\n\4\2\2\u013c\u013e\5\66\34\2\u013d\u013b"+
		"\3\2\2\2\u013d\u013c\3\2\2\2\u013e\u0141\3\2\2\2\u013f\u013d\3\2\2\2\u013f"+
		"\u0140\3\2\2\2\u0140\u0142\3\2\2\2\u0141\u013f\3\2\2\2\u0142\u0143\7\30"+
		"\2\2\u0143\67\3\2\2\2\u0144\u0149\7\62\2\2\u0145\u0146\7\4\2\2\u0146\u0148"+
		"\7\62\2\2\u0147\u0145\3\2\2\2\u0148\u014b\3\2\2\2\u0149\u0147\3\2\2\2"+
		"\u0149\u014a\3\2\2\2\u014a\u014f\3\2\2\2\u014b\u0149\3\2\2\2\u014c\u014d"+
		"\7\62\2\2\u014d\u014f\5\66\34\2\u014e\u0144\3\2\2\2\u014e\u014c\3\2\2"+
		"\2\u014f9\3\2\2\2\u0150\u0151\7\'\2\2\u0151\u0152\7\24\2\2\u0152\u0157"+
		"\58\35\2\u0153\u0154\7\5\2\2\u0154\u0156\58\35\2\u0155\u0153\3\2\2\2\u0156"+
		"\u0159\3\2\2\2\u0157\u0155\3\2\2\2\u0157\u0158\3\2\2\2\u0158\u015a\3\2"+
		"\2\2\u0159\u0157\3\2\2\2\u015a\u015b\7\25\2\2\u015b;\3\2\2\2\u015c\u0161"+
		"\7\62\2\2\u015d\u015e\7\5\2\2\u015e\u0160\7\62\2\2\u015f\u015d\3\2\2\2"+
		"\u0160\u0163\3\2\2\2\u0161\u015f\3\2\2\2\u0161\u0162\3\2\2\2\u0162=\3"+
		"\2\2\2\u0163\u0161\3\2\2\2\u0164\u0165\7#\2\2\u0165\u0166\5<\37\2\u0166"+
		"?\3\2\2\2\u0167\u016a\5\6\4\2\u0168\u0169\7\4\2\2\u0169\u016b\7\62\2\2"+
		"\u016a\u0168\3\2\2\2\u016b\u016c\3\2\2\2\u016c\u016a\3\2\2\2\u016c\u016d"+
		"\3\2\2\2\u016dA\3\2\2\2\u016e\u016f\7\35\2\2\u016f\u0176\5@!\2\u0170\u0172"+
		"\7(\2\2\u0171\u0173\5\"\22\2\u0172\u0171\3\2\2\2\u0173\u0174\3\2\2\2\u0174"+
		"\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175\u0177\3\2\2\2\u0176\u0170\3\2"+
		"\2\2\u0176\u0177\3\2\2\2\u0177C\3\2\2\2\u0178\u017d\7\27\2\2\u0179\u017c"+
		"\n\4\2\2\u017a\u017c\5D#\2\u017b\u0179\3\2\2\2\u017b\u017a\3\2\2\2\u017c"+
		"\u017f\3\2\2\2\u017d\u017b\3\2\2\2\u017d\u017e\3\2\2\2\u017e\u0180\3\2"+
		"\2\2\u017f\u017d\3\2\2\2\u0180\u0181\7\30\2\2\u0181E\3\2\2\2\u0182\u0183"+
		"\7\62\2\2\u0183\u0184\5D#\2\u0184G\3\2\2\2\u0185\u0186\7\32\2\2\u0186"+
		"\u018b\5F$\2\u0187\u0188\7\5\2\2\u0188\u018a\5F$\2\u0189\u0187\3\2\2\2"+
		"\u018a\u018d\3\2\2\2\u018b\u0189\3\2\2\2\u018b\u018c\3\2\2\2\u018cI\3"+
		"\2\2\2\u018d\u018b\3\2\2\2\u018e\u018f\7\62\2\2\u018fK\3\2\2\2\u0190\u0191"+
		"\7\64\2\2\u0191M\3\2\2\2\u0192\u0193\7$\2\2\u0193\u0194\5L\'\2\u0194O"+
		"\3\2\2\2\u0195\u0196\7\35\2\2\u0196\u019d\5(\25\2\u0197\u0199\7(\2\2\u0198"+
		"\u019a\5\"\22\2\u0199\u0198\3\2\2\2\u019a\u019b\3\2\2\2\u019b\u0199\3"+
		"\2\2\2\u019b\u019c\3\2\2\2\u019c\u019e\3\2\2\2\u019d\u0197\3\2\2\2\u019d"+
		"\u019e\3\2\2\2\u019e\u01a0\3\2\2\2\u019f\u01a1\5\64\33\2\u01a0\u019f\3"+
		"\2\2\2\u01a0\u01a1\3\2\2\2\u01a1\u01a3\3\2\2\2\u01a2\u01a4\5:\36\2\u01a3"+
		"\u01a2\3\2\2\2\u01a3\u01a4\3\2\2\2\u01a4\u01a6\3\2\2\2\u01a5\u01a7\5>"+
		" \2\u01a6\u01a5\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7\u01a9\3\2\2\2\u01a8"+
		"\u01aa\5,\27\2\u01a9\u01a8\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa\u01ac\3\2"+
		"\2\2\u01ab\u01ad\5.\30\2\u01ac\u01ab\3\2\2\2\u01ac\u01ad\3\2\2\2\u01ad"+
		"\u01af\3\2\2\2\u01ae\u01b0\5\60\31\2\u01af\u01ae\3\2\2\2\u01af\u01b0\3"+
		"\2\2\2\u01b0\u01b2\3\2\2\2\u01b1\u01b3\5H%\2\u01b2\u01b1\3\2\2\2\u01b2"+
		"\u01b3\3\2\2\2\u01b3\u01b5\3\2\2\2\u01b4\u01b6\5N(\2\u01b5\u01b4\3\2\2"+
		"\2\u01b5\u01b6\3\2\2\2\u01b6Q\3\2\2\2\u01b7\u01b8\7\37\2\2\u01b8\u01bf"+
		"\5(\25\2\u01b9\u01bb\7(\2\2\u01ba\u01bc\5\"\22\2\u01bb\u01ba\3\2\2\2\u01bc"+
		"\u01bd\3\2\2\2\u01bd\u01bb\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\u01c0\3\2"+
		"\2\2\u01bf\u01b9\3\2\2\2\u01bf\u01c0\3\2\2\2\u01c0\u01c2\3\2\2\2\u01c1"+
		"\u01c3\5\64\33\2\u01c2\u01c1\3\2\2\2\u01c2\u01c3\3\2\2\2\u01c3\u01c5\3"+
		"\2\2\2\u01c4\u01c6\5> \2\u01c5\u01c4\3\2\2\2\u01c5\u01c6\3\2\2\2\u01c6"+
		"\u01c8\3\2\2\2\u01c7\u01c9\5,\27\2\u01c8\u01c7\3\2\2\2\u01c8\u01c9\3\2"+
		"\2\2\u01c9\u01cb\3\2\2\2\u01ca\u01cc\5.\30\2\u01cb\u01ca\3\2\2\2\u01cb"+
		"\u01cc\3\2\2\2\u01cc\u01ce\3\2\2\2\u01cd\u01cf\5\60\31\2\u01ce\u01cd\3"+
		"\2\2\2\u01ce\u01cf\3\2\2\2\u01cf\u01d1\3\2\2\2\u01d0\u01d2\5N(\2\u01d1"+
		"\u01d0\3\2\2\2\u01d1\u01d2\3\2\2\2\u01d2S\3\2\2\2\u01d3\u01d4\7\62\2\2"+
		"\u01d4U\3\2\2\2\u01d5\u01d6\7\31\2\2\u01d6\u01d7\5T+\2\u01d7W\3\2\2\2"+
		"\u01d8\u01d9\7 \2\2\u01d9\u01da\5$\23\2\u01da\u01e1\5V,\2\u01db\u01dd"+
		"\7(\2\2\u01dc\u01de\5\"\22\2\u01dd\u01dc\3\2\2\2\u01de\u01df\3\2\2\2\u01df"+
		"\u01dd\3\2\2\2\u01df\u01e0\3\2\2\2\u01e0\u01e2\3\2\2\2\u01e1\u01db\3\2"+
		"\2\2\u01e1\u01e2\3\2\2\2\u01e2\u01e4\3\2\2\2\u01e3\u01e5\5,\27\2\u01e4"+
		"\u01e3\3\2\2\2\u01e4\u01e5\3\2\2\2\u01e5\u01e7\3\2\2\2\u01e6\u01e8\5."+
		"\30\2\u01e7\u01e6\3\2\2\2\u01e7\u01e8\3\2\2\2\u01e8\u01ea\3\2\2\2\u01e9"+
		"\u01eb\5\60\31\2\u01ea\u01e9\3\2\2\2\u01ea\u01eb\3\2\2\2\u01eb\u01ed\3"+
		"\2\2\2\u01ec\u01ee\5N(\2\u01ed\u01ec\3\2\2\2\u01ed\u01ee\3\2\2\2\u01ee"+
		"Y\3\2\2\2A_gqsz\u0088\u008d\u0095\u00a2\u00ad\u00b2\u00b5\u00c0\u00c5"+
		"\u00c7\u00d3\u00e0\u00ea\u00f2\u00f6\u0102\u010c\u0117\u0125\u012a\u012c"+
		"\u0135\u013d\u013f\u0149\u014e\u0157\u0161\u016c\u0174\u0176\u017b\u017d"+
		"\u018b\u019b\u019d\u01a0\u01a3\u01a6\u01a9\u01ac\u01af\u01b2\u01b5\u01bd"+
		"\u01bf\u01c2\u01c5\u01c8\u01cb\u01ce\u01d1\u01df\u01e1\u01e4\u01e7\u01ea"+
		"\u01ed";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}