// Generated from ZQL.g4 by ANTLR 4.7

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
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, FILTER_BY=19, OFFSET=20, LIMIT=21, QUERY=22, COUNT=23, ORDER_BY=24, 
		ORDER_BY_VALUE=25, RESTRICT_BY=26, RETURN_WITH=27, WHERE=28, AND=29, OR=30, 
		ASC=31, DESC=32, BOOLEAN=33, INT=34, FLOAT=35, ID=36, WS=37, STRING=38;
	public static final int
		RULE_zql = 0, RULE_entity = 1, RULE_field = 2, RULE_multiFields = 3, RULE_operator = 4, 
		RULE_value = 5, RULE_logicalOperator = 6, RULE_complexValue = 7, RULE_expr = 8, 
		RULE_condition = 9, RULE_queryTarget = 10, RULE_orderBy = 11, RULE_limit = 12, 
		RULE_offset = 13, RULE_restrictByExpr = 14, RULE_restrictBy = 15, RULE_returnWithExprBlock = 16, 
		RULE_returnWithExpr = 17, RULE_returnWith = 18, RULE_subQueryTarget = 19, 
		RULE_subQuery = 20, RULE_filterByExprBlock = 21, RULE_filterByExpr = 22, 
		RULE_filterBy = 23, RULE_query = 24, RULE_count = 25;
	public static final String[] ruleNames = {
		"zql", "entity", "field", "multiFields", "operator", "value", "logicalOperator", 
		"complexValue", "expr", "condition", "queryTarget", "orderBy", "limit", 
		"offset", "restrictByExpr", "restrictBy", "returnWithExprBlock", "returnWithExpr", 
		"returnWith", "subQueryTarget", "subQuery", "filterByExprBlock", "filterByExpr", 
		"filterBy", "query", "count"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'.'", "','", "'='", "'!='", "'>'", "'>='", "'<'", "'<='", "'is null'", 
		"'not null'", "'in'", "'not in'", "'like'", "'not like'", "'('", "')'", 
		"'{'", "'}'", "'filter by'", "'offset'", "'limit'", "'query'", "'count'", 
		"'order by'", null, "'restrict by'", "'return with'", "'where'", "'and'", 
		"'or'", "'asc'", "'desc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, "FILTER_BY", "OFFSET", "LIMIT", 
		"QUERY", "COUNT", "ORDER_BY", "ORDER_BY_VALUE", "RESTRICT_BY", "RETURN_WITH", 
		"WHERE", "AND", "OR", "ASC", "DESC", "BOOLEAN", "INT", "FLOAT", "ID", 
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
		public TerminalNode EOF() { return getToken(ZQLParser.EOF, 0); }
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
		public TerminalNode EOF() { return getToken(ZQLParser.EOF, 0); }
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

	public final ZqlContext zql() throws RecognitionException {
		ZqlContext _localctx = new ZqlContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_zql);
		try {
			setState(58);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QUERY:
				_localctx = new QueryGrammarContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(52);
				query();
				setState(53);
				match(EOF);
				}
				break;
			case COUNT:
				_localctx = new CountGrammarContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(55);
				count();
				setState(56);
				match(EOF);
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
		enterRule(_localctx, 2, RULE_entity);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(60);
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
		enterRule(_localctx, 4, RULE_field);
		int _la;
		try {
			setState(70);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(62);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(63);
				match(ID);
				setState(66); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(64);
					match(T__0);
					setState(65);
					match(ID);
					}
					}
					setState(68); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__0 );
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
		enterRule(_localctx, 6, RULE_multiFields);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(72);
			match(ID);
			setState(75); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(73);
				match(T__1);
				setState(74);
				match(ID);
				}
				}
				setState(77); 
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
		enterRule(_localctx, 8, RULE_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(79);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13))) != 0)) ) {
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
		enterRule(_localctx, 10, RULE_value);
		int _la;
		try {
			setState(96);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(81);
				match(STRING);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 2);
				{
				setState(82);
				match(INT);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(83);
				match(FLOAT);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(84);
				match(BOOLEAN);
				}
				break;
			case T__14:
				enterOuterAlt(_localctx, 5);
				{
				setState(85);
				match(T__14);
				setState(86);
				value();
				setState(91);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(87);
					match(T__1);
					setState(88);
					value();
					}
					}
					setState(93);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(94);
				match(T__15);
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
		enterRule(_localctx, 12, RULE_logicalOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(98);
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

	public final ComplexValueContext complexValue() throws RecognitionException {
		ComplexValueContext _localctx = new ComplexValueContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_complexValue);
		try {
			setState(105);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				_localctx = new SimpleValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(100);
				value();
				}
				break;
			case 2:
				_localctx = new SubQueryValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(101);
				match(T__14);
				setState(102);
				subQuery();
				setState(103);
				match(T__15);
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
		enterRule(_localctx, 16, RULE_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(107);
			field();
			setState(108);
			operator();
			setState(110);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				{
				setState(109);
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
		int _startState = 18;
		enterRecursionRule(_localctx, 18, RULE_condition, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(118);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__14:
				{
				_localctx = new ParenthesisConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(113);
				match(T__14);
				setState(114);
				condition(0);
				setState(115);
				match(T__15);
				}
				break;
			case ID:
				{
				_localctx = new SimpleConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(117);
				expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(130);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new NestConditionContext(new ConditionContext(_parentctx, _parentState));
					((NestConditionContext)_localctx).left = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_condition);
					setState(120);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(124); 
					_errHandler.sync(this);
					_alt = 1;
					do {
						switch (_alt) {
						case 1:
							{
							{
							setState(121);
							((NestConditionContext)_localctx).op = logicalOperator();
							setState(122);
							((NestConditionContext)_localctx).right = condition(0);
							}
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						setState(126); 
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
					} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
					}
					} 
				}
				setState(132);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
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
		enterRule(_localctx, 20, RULE_queryTarget);
		try {
			setState(142);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				_localctx = new OnlyEntityContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(133);
				entity();
				}
				break;
			case 2:
				_localctx = new WithSingleFieldContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(134);
				entity();
				setState(135);
				match(T__0);
				setState(136);
				field();
				}
				break;
			case 3:
				_localctx = new WithMultiFieldsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(138);
				entity();
				setState(139);
				match(T__0);
				setState(140);
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

	public static class OrderByContext extends ParserRuleContext {
		public TerminalNode ORDER_BY() { return getToken(ZQLParser.ORDER_BY, 0); }
		public TerminalNode ID() { return getToken(ZQLParser.ID, 0); }
		public TerminalNode ORDER_BY_VALUE() { return getToken(ZQLParser.ORDER_BY_VALUE, 0); }
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
		enterRule(_localctx, 22, RULE_orderBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
			match(ORDER_BY);
			setState(145);
			match(ID);
			setState(146);
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
		enterRule(_localctx, 24, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			match(LIMIT);
			setState(149);
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
		enterRule(_localctx, 26, RULE_offset);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(151);
			match(OFFSET);
			setState(152);
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
		enterRule(_localctx, 28, RULE_restrictByExpr);
		int _la;
		try {
			setState(166);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(154);
				entity();
				setState(155);
				match(T__0);
				setState(156);
				match(ID);
				setState(157);
				operator();
				setState(159);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__14) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(158);
					value();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(161);
				match(ID);
				setState(162);
				operator();
				setState(164);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__14) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(163);
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
		enterRule(_localctx, 30, RULE_restrictBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(168);
			match(RESTRICT_BY);
			setState(169);
			match(T__14);
			setState(170);
			restrictByExpr();
			setState(175);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(171);
				match(T__1);
				setState(172);
				restrictByExpr();
				}
				}
				setState(177);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(178);
			match(T__15);
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
		enterRule(_localctx, 32, RULE_returnWithExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			match(T__16);
			setState(185);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << COUNT) | (1L << ORDER_BY) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(183);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(181);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==T__17) ) {
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
					setState(182);
					returnWithExprBlock();
					}
					break;
				}
				}
				setState(187);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(188);
			match(T__17);
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
		enterRule(_localctx, 34, RULE_returnWithExpr);
		int _la;
		try {
			setState(200);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				_localctx = new ReturnWithExprIdContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(190);
				match(ID);
				setState(195);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__0) {
					{
					{
					setState(191);
					match(T__0);
					setState(192);
					match(ID);
					}
					}
					setState(197);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				_localctx = new ReturnWithExprFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(198);
				match(ID);
				setState(199);
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
		enterRule(_localctx, 36, RULE_returnWith);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(202);
			match(RETURN_WITH);
			setState(203);
			match(T__14);
			setState(204);
			returnWithExpr();
			setState(209);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(205);
				match(T__1);
				setState(206);
				returnWithExpr();
				}
				}
				setState(211);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(212);
			match(T__15);
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
		enterRule(_localctx, 38, RULE_subQueryTarget);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(214);
			entity();
			setState(217); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(215);
				match(T__0);
				setState(216);
				match(ID);
				}
				}
				setState(219); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__0 );
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
		enterRule(_localctx, 40, RULE_subQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			match(QUERY);
			setState(222);
			subQueryTarget();
			setState(229);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(223);
				match(WHERE);
				setState(225); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(224);
					condition(0);
					}
					}
					setState(227); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__14 || _la==ID );
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
		enterRule(_localctx, 42, RULE_filterByExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(231);
			match(T__16);
			setState(236);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << COUNT) | (1L << ORDER_BY) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(234);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
				case 1:
					{
					setState(232);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==T__17) ) {
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
					setState(233);
					filterByExprBlock();
					}
					break;
				}
				}
				setState(238);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(239);
			match(T__17);
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
		enterRule(_localctx, 44, RULE_filterByExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(241);
			match(ID);
			setState(242);
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
		enterRule(_localctx, 46, RULE_filterBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			match(FILTER_BY);
			setState(245);
			filterByExpr();
			setState(250);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(246);
				match(T__1);
				setState(247);
				filterByExpr();
				}
				}
				setState(252);
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

	public static class QueryContext extends ParserRuleContext {
		public TerminalNode QUERY() { return getToken(ZQLParser.QUERY, 0); }
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(ZQLParser.WHERE, 0); }
		public RestrictByContext restrictBy() {
			return getRuleContext(RestrictByContext.class,0);
		}
		public ReturnWithContext returnWith() {
			return getRuleContext(ReturnWithContext.class,0);
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
		enterRule(_localctx, 48, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(253);
			match(QUERY);
			setState(254);
			queryTarget();
			setState(261);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(255);
				match(WHERE);
				setState(257); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(256);
					condition(0);
					}
					}
					setState(259); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__14 || _la==ID );
				}
			}

			setState(264);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(263);
				restrictBy();
				}
			}

			setState(267);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RETURN_WITH) {
				{
				setState(266);
				returnWith();
				}
			}

			setState(270);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(269);
				orderBy();
				}
			}

			setState(273);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(272);
				limit();
				}
			}

			setState(276);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(275);
				offset();
				}
			}

			setState(279);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FILTER_BY) {
				{
				setState(278);
				filterBy();
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
		public QueryTargetContext queryTarget() {
			return getRuleContext(QueryTargetContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(ZQLParser.WHERE, 0); }
		public RestrictByContext restrictBy() {
			return getRuleContext(RestrictByContext.class,0);
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
		enterRule(_localctx, 50, RULE_count);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
			match(COUNT);
			setState(282);
			queryTarget();
			setState(289);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(283);
				match(WHERE);
				setState(285); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(284);
					condition(0);
					}
					}
					setState(287); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__14 || _la==ID );
				}
			}

			setState(292);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(291);
				restrictBy();
				}
			}

			setState(295);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(294);
				orderBy();
				}
			}

			setState(298);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(297);
				limit();
				}
			}

			setState(301);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(300);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 9:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3(\u0132\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\3\2\3\2\3\2\3\2\3\2\3\2\5\2=\n\2\3\3\3\3\3\4\3\4"+
		"\3\4\3\4\6\4E\n\4\r\4\16\4F\5\4I\n\4\3\5\3\5\3\5\6\5N\n\5\r\5\16\5O\3"+
		"\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\7\7\\\n\7\f\7\16\7_\13\7\3\7\3"+
		"\7\5\7c\n\7\3\b\3\b\3\t\3\t\3\t\3\t\3\t\5\tl\n\t\3\n\3\n\3\n\5\nq\n\n"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\5\13y\n\13\3\13\3\13\3\13\3\13\6\13\177"+
		"\n\13\r\13\16\13\u0080\7\13\u0083\n\13\f\13\16\13\u0086\13\13\3\f\3\f"+
		"\3\f\3\f\3\f\3\f\3\f\3\f\3\f\5\f\u0091\n\f\3\r\3\r\3\r\3\r\3\16\3\16\3"+
		"\16\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\5\20\u00a2\n\20\3\20\3\20"+
		"\3\20\5\20\u00a7\n\20\5\20\u00a9\n\20\3\21\3\21\3\21\3\21\3\21\7\21\u00b0"+
		"\n\21\f\21\16\21\u00b3\13\21\3\21\3\21\3\22\3\22\3\22\7\22\u00ba\n\22"+
		"\f\22\16\22\u00bd\13\22\3\22\3\22\3\23\3\23\3\23\7\23\u00c4\n\23\f\23"+
		"\16\23\u00c7\13\23\3\23\3\23\5\23\u00cb\n\23\3\24\3\24\3\24\3\24\3\24"+
		"\7\24\u00d2\n\24\f\24\16\24\u00d5\13\24\3\24\3\24\3\25\3\25\3\25\6\25"+
		"\u00dc\n\25\r\25\16\25\u00dd\3\26\3\26\3\26\3\26\6\26\u00e4\n\26\r\26"+
		"\16\26\u00e5\5\26\u00e8\n\26\3\27\3\27\3\27\7\27\u00ed\n\27\f\27\16\27"+
		"\u00f0\13\27\3\27\3\27\3\30\3\30\3\30\3\31\3\31\3\31\3\31\7\31\u00fb\n"+
		"\31\f\31\16\31\u00fe\13\31\3\32\3\32\3\32\3\32\6\32\u0104\n\32\r\32\16"+
		"\32\u0105\5\32\u0108\n\32\3\32\5\32\u010b\n\32\3\32\5\32\u010e\n\32\3"+
		"\32\5\32\u0111\n\32\3\32\5\32\u0114\n\32\3\32\5\32\u0117\n\32\3\32\5\32"+
		"\u011a\n\32\3\33\3\33\3\33\3\33\6\33\u0120\n\33\r\33\16\33\u0121\5\33"+
		"\u0124\n\33\3\33\5\33\u0127\n\33\3\33\5\33\u012a\n\33\3\33\5\33\u012d"+
		"\n\33\3\33\5\33\u0130\n\33\3\33\2\3\24\34\2\4\6\b\n\f\16\20\22\24\26\30"+
		"\32\34\36 \"$&(*,.\60\62\64\2\5\3\2\5\20\3\2\37 \3\2\24\24\2\u0144\2<"+
		"\3\2\2\2\4>\3\2\2\2\6H\3\2\2\2\bJ\3\2\2\2\nQ\3\2\2\2\fb\3\2\2\2\16d\3"+
		"\2\2\2\20k\3\2\2\2\22m\3\2\2\2\24x\3\2\2\2\26\u0090\3\2\2\2\30\u0092\3"+
		"\2\2\2\32\u0096\3\2\2\2\34\u0099\3\2\2\2\36\u00a8\3\2\2\2 \u00aa\3\2\2"+
		"\2\"\u00b6\3\2\2\2$\u00ca\3\2\2\2&\u00cc\3\2\2\2(\u00d8\3\2\2\2*\u00df"+
		"\3\2\2\2,\u00e9\3\2\2\2.\u00f3\3\2\2\2\60\u00f6\3\2\2\2\62\u00ff\3\2\2"+
		"\2\64\u011b\3\2\2\2\66\67\5\62\32\2\678\7\2\2\38=\3\2\2\29:\5\64\33\2"+
		":;\7\2\2\3;=\3\2\2\2<\66\3\2\2\2<9\3\2\2\2=\3\3\2\2\2>?\7&\2\2?\5\3\2"+
		"\2\2@I\7&\2\2AD\7&\2\2BC\7\3\2\2CE\7&\2\2DB\3\2\2\2EF\3\2\2\2FD\3\2\2"+
		"\2FG\3\2\2\2GI\3\2\2\2H@\3\2\2\2HA\3\2\2\2I\7\3\2\2\2JM\7&\2\2KL\7\4\2"+
		"\2LN\7&\2\2MK\3\2\2\2NO\3\2\2\2OM\3\2\2\2OP\3\2\2\2P\t\3\2\2\2QR\t\2\2"+
		"\2R\13\3\2\2\2Sc\7(\2\2Tc\7$\2\2Uc\7%\2\2Vc\7#\2\2WX\7\21\2\2X]\5\f\7"+
		"\2YZ\7\4\2\2Z\\\5\f\7\2[Y\3\2\2\2\\_\3\2\2\2][\3\2\2\2]^\3\2\2\2^`\3\2"+
		"\2\2_]\3\2\2\2`a\7\22\2\2ac\3\2\2\2bS\3\2\2\2bT\3\2\2\2bU\3\2\2\2bV\3"+
		"\2\2\2bW\3\2\2\2c\r\3\2\2\2de\t\3\2\2e\17\3\2\2\2fl\5\f\7\2gh\7\21\2\2"+
		"hi\5*\26\2ij\7\22\2\2jl\3\2\2\2kf\3\2\2\2kg\3\2\2\2l\21\3\2\2\2mn\5\6"+
		"\4\2np\5\n\6\2oq\5\20\t\2po\3\2\2\2pq\3\2\2\2q\23\3\2\2\2rs\b\13\1\2s"+
		"t\7\21\2\2tu\5\24\13\2uv\7\22\2\2vy\3\2\2\2wy\5\22\n\2xr\3\2\2\2xw\3\2"+
		"\2\2y\u0084\3\2\2\2z~\f\4\2\2{|\5\16\b\2|}\5\24\13\2}\177\3\2\2\2~{\3"+
		"\2\2\2\177\u0080\3\2\2\2\u0080~\3\2\2\2\u0080\u0081\3\2\2\2\u0081\u0083"+
		"\3\2\2\2\u0082z\3\2\2\2\u0083\u0086\3\2\2\2\u0084\u0082\3\2\2\2\u0084"+
		"\u0085\3\2\2\2\u0085\25\3\2\2\2\u0086\u0084\3\2\2\2\u0087\u0091\5\4\3"+
		"\2\u0088\u0089\5\4\3\2\u0089\u008a\7\3\2\2\u008a\u008b\5\6\4\2\u008b\u0091"+
		"\3\2\2\2\u008c\u008d\5\4\3\2\u008d\u008e\7\3\2\2\u008e\u008f\5\b\5\2\u008f"+
		"\u0091\3\2\2\2\u0090\u0087\3\2\2\2\u0090\u0088\3\2\2\2\u0090\u008c\3\2"+
		"\2\2\u0091\27\3\2\2\2\u0092\u0093\7\32\2\2\u0093\u0094\7&\2\2\u0094\u0095"+
		"\7\33\2\2\u0095\31\3\2\2\2\u0096\u0097\7\27\2\2\u0097\u0098\7$\2\2\u0098"+
		"\33\3\2\2\2\u0099\u009a\7\26\2\2\u009a\u009b\7$\2\2\u009b\35\3\2\2\2\u009c"+
		"\u009d\5\4\3\2\u009d\u009e\7\3\2\2\u009e\u009f\7&\2\2\u009f\u00a1\5\n"+
		"\6\2\u00a0\u00a2\5\f\7\2\u00a1\u00a0\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2"+
		"\u00a9\3\2\2\2\u00a3\u00a4\7&\2\2\u00a4\u00a6\5\n\6\2\u00a5\u00a7\5\f"+
		"\7\2\u00a6\u00a5\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7\u00a9\3\2\2\2\u00a8"+
		"\u009c\3\2\2\2\u00a8\u00a3\3\2\2\2\u00a9\37\3\2\2\2\u00aa\u00ab\7\34\2"+
		"\2\u00ab\u00ac\7\21\2\2\u00ac\u00b1\5\36\20\2\u00ad\u00ae\7\4\2\2\u00ae"+
		"\u00b0\5\36\20\2\u00af\u00ad\3\2\2\2\u00b0\u00b3\3\2\2\2\u00b1\u00af\3"+
		"\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b4\3\2\2\2\u00b3\u00b1\3\2\2\2\u00b4"+
		"\u00b5\7\22\2\2\u00b5!\3\2\2\2\u00b6\u00bb\7\23\2\2\u00b7\u00ba\n\4\2"+
		"\2\u00b8\u00ba\5\"\22\2\u00b9\u00b7\3\2\2\2\u00b9\u00b8\3\2\2\2\u00ba"+
		"\u00bd\3\2\2\2\u00bb\u00b9\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc\u00be\3\2"+
		"\2\2\u00bd\u00bb\3\2\2\2\u00be\u00bf\7\24\2\2\u00bf#\3\2\2\2\u00c0\u00c5"+
		"\7&\2\2\u00c1\u00c2\7\3\2\2\u00c2\u00c4\7&\2\2\u00c3\u00c1\3\2\2\2\u00c4"+
		"\u00c7\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c5\u00c6\3\2\2\2\u00c6\u00cb\3\2"+
		"\2\2\u00c7\u00c5\3\2\2\2\u00c8\u00c9\7&\2\2\u00c9\u00cb\5\"\22\2\u00ca"+
		"\u00c0\3\2\2\2\u00ca\u00c8\3\2\2\2\u00cb%\3\2\2\2\u00cc\u00cd\7\35\2\2"+
		"\u00cd\u00ce\7\21\2\2\u00ce\u00d3\5$\23\2\u00cf\u00d0\7\4\2\2\u00d0\u00d2"+
		"\5$\23\2\u00d1\u00cf\3\2\2\2\u00d2\u00d5\3\2\2\2\u00d3\u00d1\3\2\2\2\u00d3"+
		"\u00d4\3\2\2\2\u00d4\u00d6\3\2\2\2\u00d5\u00d3\3\2\2\2\u00d6\u00d7\7\22"+
		"\2\2\u00d7\'\3\2\2\2\u00d8\u00db\5\4\3\2\u00d9\u00da\7\3\2\2\u00da\u00dc"+
		"\7&\2\2\u00db\u00d9\3\2\2\2\u00dc\u00dd\3\2\2\2\u00dd\u00db\3\2\2\2\u00dd"+
		"\u00de\3\2\2\2\u00de)\3\2\2\2\u00df\u00e0\7\30\2\2\u00e0\u00e7\5(\25\2"+
		"\u00e1\u00e3\7\36\2\2\u00e2\u00e4\5\24\13\2\u00e3\u00e2\3\2\2\2\u00e4"+
		"\u00e5\3\2\2\2\u00e5\u00e3\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e8\3\2"+
		"\2\2\u00e7\u00e1\3\2\2\2\u00e7\u00e8\3\2\2\2\u00e8+\3\2\2\2\u00e9\u00ee"+
		"\7\23\2\2\u00ea\u00ed\n\4\2\2\u00eb\u00ed\5,\27\2\u00ec\u00ea\3\2\2\2"+
		"\u00ec\u00eb\3\2\2\2\u00ed\u00f0\3\2\2\2\u00ee\u00ec\3\2\2\2\u00ee\u00ef"+
		"\3\2\2\2\u00ef\u00f1\3\2\2\2\u00f0\u00ee\3\2\2\2\u00f1\u00f2\7\24\2\2"+
		"\u00f2-\3\2\2\2\u00f3\u00f4\7&\2\2\u00f4\u00f5\5,\27\2\u00f5/\3\2\2\2"+
		"\u00f6\u00f7\7\25\2\2\u00f7\u00fc\5.\30\2\u00f8\u00f9\7\4\2\2\u00f9\u00fb"+
		"\5.\30\2\u00fa\u00f8\3\2\2\2\u00fb\u00fe\3\2\2\2\u00fc\u00fa\3\2\2\2\u00fc"+
		"\u00fd\3\2\2\2\u00fd\61\3\2\2\2\u00fe\u00fc\3\2\2\2\u00ff\u0100\7\30\2"+
		"\2\u0100\u0107\5\26\f\2\u0101\u0103\7\36\2\2\u0102\u0104\5\24\13\2\u0103"+
		"\u0102\3\2\2\2\u0104\u0105\3\2\2\2\u0105\u0103\3\2\2\2\u0105\u0106\3\2"+
		"\2\2\u0106\u0108\3\2\2\2\u0107\u0101\3\2\2\2\u0107\u0108\3\2\2\2\u0108"+
		"\u010a\3\2\2\2\u0109\u010b\5 \21\2\u010a\u0109\3\2\2\2\u010a\u010b\3\2"+
		"\2\2\u010b\u010d\3\2\2\2\u010c\u010e\5&\24\2\u010d\u010c\3\2\2\2\u010d"+
		"\u010e\3\2\2\2\u010e\u0110\3\2\2\2\u010f\u0111\5\30\r\2\u0110\u010f\3"+
		"\2\2\2\u0110\u0111\3\2\2\2\u0111\u0113\3\2\2\2\u0112\u0114\5\32\16\2\u0113"+
		"\u0112\3\2\2\2\u0113\u0114\3\2\2\2\u0114\u0116\3\2\2\2\u0115\u0117\5\34"+
		"\17\2\u0116\u0115\3\2\2\2\u0116\u0117\3\2\2\2\u0117\u0119\3\2\2\2\u0118"+
		"\u011a\5\60\31\2\u0119\u0118\3\2\2\2\u0119\u011a\3\2\2\2\u011a\63\3\2"+
		"\2\2\u011b\u011c\7\31\2\2\u011c\u0123\5\26\f\2\u011d\u011f\7\36\2\2\u011e"+
		"\u0120\5\24\13\2\u011f\u011e\3\2\2\2\u0120\u0121\3\2\2\2\u0121\u011f\3"+
		"\2\2\2\u0121\u0122\3\2\2\2\u0122\u0124\3\2\2\2\u0123\u011d\3\2\2\2\u0123"+
		"\u0124\3\2\2\2\u0124\u0126\3\2\2\2\u0125\u0127\5 \21\2\u0126\u0125\3\2"+
		"\2\2\u0126\u0127\3\2\2\2\u0127\u0129\3\2\2\2\u0128\u012a\5\30\r\2\u0129"+
		"\u0128\3\2\2\2\u0129\u012a\3\2\2\2\u012a\u012c\3\2\2\2\u012b\u012d\5\32"+
		"\16\2\u012c\u012b\3\2\2\2\u012c\u012d\3\2\2\2\u012d\u012f\3\2\2\2\u012e"+
		"\u0130\5\34\17\2\u012f\u012e\3\2\2\2\u012f\u0130\3\2\2\2\u0130\65\3\2"+
		"\2\2+<FHO]bkpx\u0080\u0084\u0090\u00a1\u00a6\u00a8\u00b1\u00b9\u00bb\u00c5"+
		"\u00ca\u00d3\u00dd\u00e5\u00e7\u00ec\u00ee\u00fc\u0105\u0107\u010a\u010d"+
		"\u0110\u0113\u0116\u0119\u0121\u0123\u0126\u0129\u012c\u012f";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}