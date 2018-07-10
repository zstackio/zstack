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
		T__17=18, T__18=19, FILTER_BY=20, OFFSET=21, LIMIT=22, QUERY=23, COUNT=24, 
		ORDER_BY=25, NAMED_AS=26, ORDER_BY_VALUE=27, RESTRICT_BY=28, RETURN_WITH=29, 
		WHERE=30, AND=31, OR=32, ASC=33, DESC=34, BOOLEAN=35, INT=36, FLOAT=37, 
		ID=38, WS=39, STRING=40;
	public static final int
		RULE_zqls = 0, RULE_zql = 1, RULE_entity = 2, RULE_field = 3, RULE_multiFields = 4, 
		RULE_operator = 5, RULE_value = 6, RULE_logicalOperator = 7, RULE_complexValue = 8, 
		RULE_expr = 9, RULE_condition = 10, RULE_queryTarget = 11, RULE_orderBy = 12, 
		RULE_limit = 13, RULE_offset = 14, RULE_restrictByExpr = 15, RULE_restrictBy = 16, 
		RULE_returnWithExprBlock = 17, RULE_returnWithExpr = 18, RULE_returnWith = 19, 
		RULE_subQueryTarget = 20, RULE_subQuery = 21, RULE_filterByExprBlock = 22, 
		RULE_filterByExpr = 23, RULE_filterBy = 24, RULE_namedAsValue = 25, RULE_namedAs = 26, 
		RULE_query = 27, RULE_count = 28;
	public static final String[] ruleNames = {
		"zqls", "zql", "entity", "field", "multiFields", "operator", "value", 
		"logicalOperator", "complexValue", "expr", "condition", "queryTarget", 
		"orderBy", "limit", "offset", "restrictByExpr", "restrictBy", "returnWithExprBlock", 
		"returnWithExpr", "returnWith", "subQueryTarget", "subQuery", "filterByExprBlock", 
		"filterByExpr", "filterBy", "namedAsValue", "namedAs", "query", "count"
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
			setState(58);
			zql();
			setState(63);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(59);
				match(T__0);
				setState(60);
				zql();
				}
				}
				setState(65);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(66);
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

	public final ZqlContext zql() throws RecognitionException {
		ZqlContext _localctx = new ZqlContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_zql);
		try {
			setState(70);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QUERY:
				_localctx = new QueryGrammarContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(68);
				query();
				}
				break;
			case COUNT:
				_localctx = new CountGrammarContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(69);
				count();
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
			setState(72);
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
			setState(82);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(74);
				match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(75);
				match(ID);
				setState(78); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(76);
					match(T__1);
					setState(77);
					match(ID);
					}
					}
					setState(80); 
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
			setState(84);
			match(ID);
			setState(87); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(85);
				match(T__2);
				setState(86);
				match(ID);
				}
				}
				setState(89); 
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
			setState(91);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14))) != 0)) ) {
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
			setState(108);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(93);
				match(STRING);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 2);
				{
				setState(94);
				match(INT);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 3);
				{
				setState(95);
				match(FLOAT);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(96);
				match(BOOLEAN);
				}
				break;
			case T__15:
				enterOuterAlt(_localctx, 5);
				{
				setState(97);
				match(T__15);
				setState(98);
				value();
				setState(103);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__2) {
					{
					{
					setState(99);
					match(T__2);
					setState(100);
					value();
					}
					}
					setState(105);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(106);
				match(T__16);
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
		enterRule(_localctx, 14, RULE_logicalOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110);
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
		enterRule(_localctx, 16, RULE_complexValue);
		try {
			setState(117);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				_localctx = new SimpleValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(112);
				value();
				}
				break;
			case 2:
				_localctx = new SubQueryValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				match(T__15);
				setState(114);
				subQuery();
				setState(115);
				match(T__16);
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
		enterRule(_localctx, 18, RULE_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			field();
			setState(120);
			operator();
			setState(122);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(121);
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
		int _startState = 20;
		enterRecursionRule(_localctx, 20, RULE_condition, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(130);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__15:
				{
				_localctx = new ParenthesisConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(125);
				match(T__15);
				setState(126);
				condition(0);
				setState(127);
				match(T__16);
				}
				break;
			case ID:
				{
				_localctx = new SimpleConditionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(129);
				expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(142);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new NestConditionContext(new ConditionContext(_parentctx, _parentState));
					((NestConditionContext)_localctx).left = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_condition);
					setState(132);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(136); 
					_errHandler.sync(this);
					_alt = 1;
					do {
						switch (_alt) {
						case 1:
							{
							{
							setState(133);
							((NestConditionContext)_localctx).op = logicalOperator();
							setState(134);
							((NestConditionContext)_localctx).right = condition(0);
							}
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						setState(138); 
						_errHandler.sync(this);
						_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
					} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
					}
					} 
				}
				setState(144);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
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
		enterRule(_localctx, 22, RULE_queryTarget);
		try {
			setState(154);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				_localctx = new OnlyEntityContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(145);
				entity();
				}
				break;
			case 2:
				_localctx = new WithSingleFieldContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(146);
				entity();
				setState(147);
				match(T__1);
				setState(148);
				field();
				}
				break;
			case 3:
				_localctx = new WithMultiFieldsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(150);
				entity();
				setState(151);
				match(T__1);
				setState(152);
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
		enterRule(_localctx, 24, RULE_orderBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			match(ORDER_BY);
			setState(157);
			match(ID);
			setState(158);
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
		enterRule(_localctx, 26, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(160);
			match(LIMIT);
			setState(161);
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
		enterRule(_localctx, 28, RULE_offset);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(163);
			match(OFFSET);
			setState(164);
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
		enterRule(_localctx, 30, RULE_restrictByExpr);
		int _la;
		try {
			setState(178);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(166);
				entity();
				setState(167);
				match(T__1);
				setState(168);
				match(ID);
				setState(169);
				operator();
				setState(171);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__15) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(170);
					value();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(173);
				match(ID);
				setState(174);
				operator();
				setState(176);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__15) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << STRING))) != 0)) {
					{
					setState(175);
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
		enterRule(_localctx, 32, RULE_restrictBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			match(RESTRICT_BY);
			setState(181);
			match(T__15);
			setState(182);
			restrictByExpr();
			setState(187);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(183);
				match(T__2);
				setState(184);
				restrictByExpr();
				}
				}
				setState(189);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(190);
			match(T__16);
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
		enterRule(_localctx, 34, RULE_returnWithExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			match(T__17);
			setState(197);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << COUNT) | (1L << ORDER_BY) | (1L << NAMED_AS) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(195);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
				case 1:
					{
					setState(193);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==T__18) ) {
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
					setState(194);
					returnWithExprBlock();
					}
					break;
				}
				}
				setState(199);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(200);
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
		enterRule(_localctx, 36, RULE_returnWithExpr);
		int _la;
		try {
			setState(212);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				_localctx = new ReturnWithExprIdContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(202);
				match(ID);
				setState(207);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(203);
					match(T__1);
					setState(204);
					match(ID);
					}
					}
					setState(209);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				_localctx = new ReturnWithExprFunctionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(210);
				match(ID);
				setState(211);
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
		enterRule(_localctx, 38, RULE_returnWith);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(214);
			match(RETURN_WITH);
			setState(215);
			match(T__15);
			setState(216);
			returnWithExpr();
			setState(221);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(217);
				match(T__2);
				setState(218);
				returnWithExpr();
				}
				}
				setState(223);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(224);
			match(T__16);
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
		enterRule(_localctx, 40, RULE_subQueryTarget);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			entity();
			setState(229); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(227);
				match(T__1);
				setState(228);
				match(ID);
				}
				}
				setState(231); 
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
		enterRule(_localctx, 42, RULE_subQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(233);
			match(QUERY);
			setState(234);
			subQueryTarget();
			setState(241);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(235);
				match(WHERE);
				setState(237); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(236);
					condition(0);
					}
					}
					setState(239); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__15 || _la==ID );
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
		enterRule(_localctx, 44, RULE_filterByExprBlock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(243);
			match(T__17);
			setState(248);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << FILTER_BY) | (1L << OFFSET) | (1L << LIMIT) | (1L << QUERY) | (1L << COUNT) | (1L << ORDER_BY) | (1L << NAMED_AS) | (1L << ORDER_BY_VALUE) | (1L << RESTRICT_BY) | (1L << RETURN_WITH) | (1L << WHERE) | (1L << AND) | (1L << OR) | (1L << ASC) | (1L << DESC) | (1L << BOOLEAN) | (1L << INT) | (1L << FLOAT) | (1L << ID) | (1L << WS) | (1L << STRING))) != 0)) {
				{
				setState(246);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
				case 1:
					{
					setState(244);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==T__18) ) {
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
					setState(245);
					filterByExprBlock();
					}
					break;
				}
				}
				setState(250);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(251);
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
		enterRule(_localctx, 46, RULE_filterByExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(253);
			match(ID);
			setState(254);
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
		enterRule(_localctx, 48, RULE_filterBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(256);
			match(FILTER_BY);
			setState(257);
			filterByExpr();
			setState(262);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__2) {
				{
				{
				setState(258);
				match(T__2);
				setState(259);
				filterByExpr();
				}
				}
				setState(264);
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
		enterRule(_localctx, 50, RULE_namedAsValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
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
		enterRule(_localctx, 52, RULE_namedAs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			match(NAMED_AS);
			setState(268);
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
		enterRule(_localctx, 54, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(270);
			match(QUERY);
			setState(271);
			queryTarget();
			setState(278);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(272);
				match(WHERE);
				setState(274); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(273);
					condition(0);
					}
					}
					setState(276); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__15 || _la==ID );
				}
			}

			setState(281);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(280);
				restrictBy();
				}
			}

			setState(284);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RETURN_WITH) {
				{
				setState(283);
				returnWith();
				}
			}

			setState(287);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(286);
				orderBy();
				}
			}

			setState(290);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(289);
				limit();
				}
			}

			setState(293);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(292);
				offset();
				}
			}

			setState(296);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FILTER_BY) {
				{
				setState(295);
				filterBy();
				}
			}

			setState(299);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(298);
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
		enterRule(_localctx, 56, RULE_count);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			match(COUNT);
			setState(302);
			queryTarget();
			setState(309);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(303);
				match(WHERE);
				setState(305); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(304);
					condition(0);
					}
					}
					setState(307); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==T__15 || _la==ID );
				}
			}

			setState(312);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==RESTRICT_BY) {
				{
				setState(311);
				restrictBy();
				}
			}

			setState(315);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER_BY) {
				{
				setState(314);
				orderBy();
				}
			}

			setState(318);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(317);
				limit();
				}
			}

			setState(321);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(320);
				offset();
				}
			}

			setState(324);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAMED_AS) {
				{
				setState(323);
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
		case 10:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3*\u0149\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\3\2\3\2\3\2\7\2@\n"+
		"\2\f\2\16\2C\13\2\3\2\3\2\3\3\3\3\5\3I\n\3\3\4\3\4\3\5\3\5\3\5\3\5\6\5"+
		"Q\n\5\r\5\16\5R\5\5U\n\5\3\6\3\6\3\6\6\6Z\n\6\r\6\16\6[\3\7\3\7\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\7\bh\n\b\f\b\16\bk\13\b\3\b\3\b\5\bo\n\b\3"+
		"\t\3\t\3\n\3\n\3\n\3\n\3\n\5\nx\n\n\3\13\3\13\3\13\5\13}\n\13\3\f\3\f"+
		"\3\f\3\f\3\f\3\f\5\f\u0085\n\f\3\f\3\f\3\f\3\f\6\f\u008b\n\f\r\f\16\f"+
		"\u008c\7\f\u008f\n\f\f\f\16\f\u0092\13\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\5\r\u009d\n\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\20\3\20\3\20"+
		"\3\21\3\21\3\21\3\21\3\21\5\21\u00ae\n\21\3\21\3\21\3\21\5\21\u00b3\n"+
		"\21\5\21\u00b5\n\21\3\22\3\22\3\22\3\22\3\22\7\22\u00bc\n\22\f\22\16\22"+
		"\u00bf\13\22\3\22\3\22\3\23\3\23\3\23\7\23\u00c6\n\23\f\23\16\23\u00c9"+
		"\13\23\3\23\3\23\3\24\3\24\3\24\7\24\u00d0\n\24\f\24\16\24\u00d3\13\24"+
		"\3\24\3\24\5\24\u00d7\n\24\3\25\3\25\3\25\3\25\3\25\7\25\u00de\n\25\f"+
		"\25\16\25\u00e1\13\25\3\25\3\25\3\26\3\26\3\26\6\26\u00e8\n\26\r\26\16"+
		"\26\u00e9\3\27\3\27\3\27\3\27\6\27\u00f0\n\27\r\27\16\27\u00f1\5\27\u00f4"+
		"\n\27\3\30\3\30\3\30\7\30\u00f9\n\30\f\30\16\30\u00fc\13\30\3\30\3\30"+
		"\3\31\3\31\3\31\3\32\3\32\3\32\3\32\7\32\u0107\n\32\f\32\16\32\u010a\13"+
		"\32\3\33\3\33\3\34\3\34\3\34\3\35\3\35\3\35\3\35\6\35\u0115\n\35\r\35"+
		"\16\35\u0116\5\35\u0119\n\35\3\35\5\35\u011c\n\35\3\35\5\35\u011f\n\35"+
		"\3\35\5\35\u0122\n\35\3\35\5\35\u0125\n\35\3\35\5\35\u0128\n\35\3\35\5"+
		"\35\u012b\n\35\3\35\5\35\u012e\n\35\3\36\3\36\3\36\3\36\6\36\u0134\n\36"+
		"\r\36\16\36\u0135\5\36\u0138\n\36\3\36\5\36\u013b\n\36\3\36\5\36\u013e"+
		"\n\36\3\36\5\36\u0141\n\36\3\36\5\36\u0144\n\36\3\36\5\36\u0147\n\36\3"+
		"\36\2\3\26\37\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64"+
		"\668:\2\5\3\2\6\21\3\2!\"\3\2\25\25\2\u015b\2<\3\2\2\2\4H\3\2\2\2\6J\3"+
		"\2\2\2\bT\3\2\2\2\nV\3\2\2\2\f]\3\2\2\2\16n\3\2\2\2\20p\3\2\2\2\22w\3"+
		"\2\2\2\24y\3\2\2\2\26\u0084\3\2\2\2\30\u009c\3\2\2\2\32\u009e\3\2\2\2"+
		"\34\u00a2\3\2\2\2\36\u00a5\3\2\2\2 \u00b4\3\2\2\2\"\u00b6\3\2\2\2$\u00c2"+
		"\3\2\2\2&\u00d6\3\2\2\2(\u00d8\3\2\2\2*\u00e4\3\2\2\2,\u00eb\3\2\2\2."+
		"\u00f5\3\2\2\2\60\u00ff\3\2\2\2\62\u0102\3\2\2\2\64\u010b\3\2\2\2\66\u010d"+
		"\3\2\2\28\u0110\3\2\2\2:\u012f\3\2\2\2<A\5\4\3\2=>\7\3\2\2>@\5\4\3\2?"+
		"=\3\2\2\2@C\3\2\2\2A?\3\2\2\2AB\3\2\2\2BD\3\2\2\2CA\3\2\2\2DE\7\2\2\3"+
		"E\3\3\2\2\2FI\58\35\2GI\5:\36\2HF\3\2\2\2HG\3\2\2\2I\5\3\2\2\2JK\7(\2"+
		"\2K\7\3\2\2\2LU\7(\2\2MP\7(\2\2NO\7\4\2\2OQ\7(\2\2PN\3\2\2\2QR\3\2\2\2"+
		"RP\3\2\2\2RS\3\2\2\2SU\3\2\2\2TL\3\2\2\2TM\3\2\2\2U\t\3\2\2\2VY\7(\2\2"+
		"WX\7\5\2\2XZ\7(\2\2YW\3\2\2\2Z[\3\2\2\2[Y\3\2\2\2[\\\3\2\2\2\\\13\3\2"+
		"\2\2]^\t\2\2\2^\r\3\2\2\2_o\7*\2\2`o\7&\2\2ao\7\'\2\2bo\7%\2\2cd\7\22"+
		"\2\2di\5\16\b\2ef\7\5\2\2fh\5\16\b\2ge\3\2\2\2hk\3\2\2\2ig\3\2\2\2ij\3"+
		"\2\2\2jl\3\2\2\2ki\3\2\2\2lm\7\23\2\2mo\3\2\2\2n_\3\2\2\2n`\3\2\2\2na"+
		"\3\2\2\2nb\3\2\2\2nc\3\2\2\2o\17\3\2\2\2pq\t\3\2\2q\21\3\2\2\2rx\5\16"+
		"\b\2st\7\22\2\2tu\5,\27\2uv\7\23\2\2vx\3\2\2\2wr\3\2\2\2ws\3\2\2\2x\23"+
		"\3\2\2\2yz\5\b\5\2z|\5\f\7\2{}\5\22\n\2|{\3\2\2\2|}\3\2\2\2}\25\3\2\2"+
		"\2~\177\b\f\1\2\177\u0080\7\22\2\2\u0080\u0081\5\26\f\2\u0081\u0082\7"+
		"\23\2\2\u0082\u0085\3\2\2\2\u0083\u0085\5\24\13\2\u0084~\3\2\2\2\u0084"+
		"\u0083\3\2\2\2\u0085\u0090\3\2\2\2\u0086\u008a\f\4\2\2\u0087\u0088\5\20"+
		"\t\2\u0088\u0089\5\26\f\2\u0089\u008b\3\2\2\2\u008a\u0087\3\2\2\2\u008b"+
		"\u008c\3\2\2\2\u008c\u008a\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008f\3\2"+
		"\2\2\u008e\u0086\3\2\2\2\u008f\u0092\3\2\2\2\u0090\u008e\3\2\2\2\u0090"+
		"\u0091\3\2\2\2\u0091\27\3\2\2\2\u0092\u0090\3\2\2\2\u0093\u009d\5\6\4"+
		"\2\u0094\u0095\5\6\4\2\u0095\u0096\7\4\2\2\u0096\u0097\5\b\5\2\u0097\u009d"+
		"\3\2\2\2\u0098\u0099\5\6\4\2\u0099\u009a\7\4\2\2\u009a\u009b\5\n\6\2\u009b"+
		"\u009d\3\2\2\2\u009c\u0093\3\2\2\2\u009c\u0094\3\2\2\2\u009c\u0098\3\2"+
		"\2\2\u009d\31\3\2\2\2\u009e\u009f\7\33\2\2\u009f\u00a0\7(\2\2\u00a0\u00a1"+
		"\7\35\2\2\u00a1\33\3\2\2\2\u00a2\u00a3\7\30\2\2\u00a3\u00a4\7&\2\2\u00a4"+
		"\35\3\2\2\2\u00a5\u00a6\7\27\2\2\u00a6\u00a7\7&\2\2\u00a7\37\3\2\2\2\u00a8"+
		"\u00a9\5\6\4\2\u00a9\u00aa\7\4\2\2\u00aa\u00ab\7(\2\2\u00ab\u00ad\5\f"+
		"\7\2\u00ac\u00ae\5\16\b\2\u00ad\u00ac\3\2\2\2\u00ad\u00ae\3\2\2\2\u00ae"+
		"\u00b5\3\2\2\2\u00af\u00b0\7(\2\2\u00b0\u00b2\5\f\7\2\u00b1\u00b3\5\16"+
		"\b\2\u00b2\u00b1\3\2\2\2\u00b2\u00b3\3\2\2\2\u00b3\u00b5\3\2\2\2\u00b4"+
		"\u00a8\3\2\2\2\u00b4\u00af\3\2\2\2\u00b5!\3\2\2\2\u00b6\u00b7\7\36\2\2"+
		"\u00b7\u00b8\7\22\2\2\u00b8\u00bd\5 \21\2\u00b9\u00ba\7\5\2\2\u00ba\u00bc"+
		"\5 \21\2\u00bb\u00b9\3\2\2\2\u00bc\u00bf\3\2\2\2\u00bd\u00bb\3\2\2\2\u00bd"+
		"\u00be\3\2\2\2\u00be\u00c0\3\2\2\2\u00bf\u00bd\3\2\2\2\u00c0\u00c1\7\23"+
		"\2\2\u00c1#\3\2\2\2\u00c2\u00c7\7\24\2\2\u00c3\u00c6\n\4\2\2\u00c4\u00c6"+
		"\5$\23\2\u00c5\u00c3\3\2\2\2\u00c5\u00c4\3\2\2\2\u00c6\u00c9\3\2\2\2\u00c7"+
		"\u00c5\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8\u00ca\3\2\2\2\u00c9\u00c7\3\2"+
		"\2\2\u00ca\u00cb\7\25\2\2\u00cb%\3\2\2\2\u00cc\u00d1\7(\2\2\u00cd\u00ce"+
		"\7\4\2\2\u00ce\u00d0\7(\2\2\u00cf\u00cd\3\2\2\2\u00d0\u00d3\3\2\2\2\u00d1"+
		"\u00cf\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d2\u00d7\3\2\2\2\u00d3\u00d1\3\2"+
		"\2\2\u00d4\u00d5\7(\2\2\u00d5\u00d7\5$\23\2\u00d6\u00cc\3\2\2\2\u00d6"+
		"\u00d4\3\2\2\2\u00d7\'\3\2\2\2\u00d8\u00d9\7\37\2\2\u00d9\u00da\7\22\2"+
		"\2\u00da\u00df\5&\24\2\u00db\u00dc\7\5\2\2\u00dc\u00de\5&\24\2\u00dd\u00db"+
		"\3\2\2\2\u00de\u00e1\3\2\2\2\u00df\u00dd\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0"+
		"\u00e2\3\2\2\2\u00e1\u00df\3\2\2\2\u00e2\u00e3\7\23\2\2\u00e3)\3\2\2\2"+
		"\u00e4\u00e7\5\6\4\2\u00e5\u00e6\7\4\2\2\u00e6\u00e8\7(\2\2\u00e7\u00e5"+
		"\3\2\2\2\u00e8\u00e9\3\2\2\2\u00e9\u00e7\3\2\2\2\u00e9\u00ea\3\2\2\2\u00ea"+
		"+\3\2\2\2\u00eb\u00ec\7\31\2\2\u00ec\u00f3\5*\26\2\u00ed\u00ef\7 \2\2"+
		"\u00ee\u00f0\5\26\f\2\u00ef\u00ee\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\u00ef"+
		"\3\2\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f4\3\2\2\2\u00f3\u00ed\3\2\2\2\u00f3"+
		"\u00f4\3\2\2\2\u00f4-\3\2\2\2\u00f5\u00fa\7\24\2\2\u00f6\u00f9\n\4\2\2"+
		"\u00f7\u00f9\5.\30\2\u00f8\u00f6\3\2\2\2\u00f8\u00f7\3\2\2\2\u00f9\u00fc"+
		"\3\2\2\2\u00fa\u00f8\3\2\2\2\u00fa\u00fb\3\2\2\2\u00fb\u00fd\3\2\2\2\u00fc"+
		"\u00fa\3\2\2\2\u00fd\u00fe\7\25\2\2\u00fe/\3\2\2\2\u00ff\u0100\7(\2\2"+
		"\u0100\u0101\5.\30\2\u0101\61\3\2\2\2\u0102\u0103\7\26\2\2\u0103\u0108"+
		"\5\60\31\2\u0104\u0105\7\5\2\2\u0105\u0107\5\60\31\2\u0106\u0104\3\2\2"+
		"\2\u0107\u010a\3\2\2\2\u0108\u0106\3\2\2\2\u0108\u0109\3\2\2\2\u0109\63"+
		"\3\2\2\2\u010a\u0108\3\2\2\2\u010b\u010c\7*\2\2\u010c\65\3\2\2\2\u010d"+
		"\u010e\7\34\2\2\u010e\u010f\5\64\33\2\u010f\67\3\2\2\2\u0110\u0111\7\31"+
		"\2\2\u0111\u0118\5\30\r\2\u0112\u0114\7 \2\2\u0113\u0115\5\26\f\2\u0114"+
		"\u0113\3\2\2\2\u0115\u0116\3\2\2\2\u0116\u0114\3\2\2\2\u0116\u0117\3\2"+
		"\2\2\u0117\u0119\3\2\2\2\u0118\u0112\3\2\2\2\u0118\u0119\3\2\2\2\u0119"+
		"\u011b\3\2\2\2\u011a\u011c\5\"\22\2\u011b\u011a\3\2\2\2\u011b\u011c\3"+
		"\2\2\2\u011c\u011e\3\2\2\2\u011d\u011f\5(\25\2\u011e\u011d\3\2\2\2\u011e"+
		"\u011f\3\2\2\2\u011f\u0121\3\2\2\2\u0120\u0122\5\32\16\2\u0121\u0120\3"+
		"\2\2\2\u0121\u0122\3\2\2\2\u0122\u0124\3\2\2\2\u0123\u0125\5\34\17\2\u0124"+
		"\u0123\3\2\2\2\u0124\u0125\3\2\2\2\u0125\u0127\3\2\2\2\u0126\u0128\5\36"+
		"\20\2\u0127\u0126\3\2\2\2\u0127\u0128\3\2\2\2\u0128\u012a\3\2\2\2\u0129"+
		"\u012b\5\62\32\2\u012a\u0129\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u012d\3"+
		"\2\2\2\u012c\u012e\5\66\34\2\u012d\u012c\3\2\2\2\u012d\u012e\3\2\2\2\u012e"+
		"9\3\2\2\2\u012f\u0130\7\32\2\2\u0130\u0137\5\30\r\2\u0131\u0133\7 \2\2"+
		"\u0132\u0134\5\26\f\2\u0133\u0132\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0133"+
		"\3\2\2\2\u0135\u0136\3\2\2\2\u0136\u0138\3\2\2\2\u0137\u0131\3\2\2\2\u0137"+
		"\u0138\3\2\2\2\u0138\u013a\3\2\2\2\u0139\u013b\5\"\22\2\u013a\u0139\3"+
		"\2\2\2\u013a\u013b\3\2\2\2\u013b\u013d\3\2\2\2\u013c\u013e\5\32\16\2\u013d"+
		"\u013c\3\2\2\2\u013d\u013e\3\2\2\2\u013e\u0140\3\2\2\2\u013f\u0141\5\34"+
		"\17\2\u0140\u013f\3\2\2\2\u0140\u0141\3\2\2\2\u0141\u0143\3\2\2\2\u0142"+
		"\u0144\5\36\20\2\u0143\u0142\3\2\2\2\u0143\u0144\3\2\2\2\u0144\u0146\3"+
		"\2\2\2\u0145\u0147\5\66\34\2\u0146\u0145\3\2\2\2\u0146\u0147\3\2\2\2\u0147"+
		";\3\2\2\2.AHRT[inw|\u0084\u008c\u0090\u009c\u00ad\u00b2\u00b4\u00bd\u00c5"+
		"\u00c7\u00d1\u00d6\u00df\u00e9\u00f1\u00f3\u00f8\u00fa\u0108\u0116\u0118"+
		"\u011b\u011e\u0121\u0124\u0127\u012a\u012d\u0135\u0137\u013a\u013d\u0140"+
		"\u0143\u0146";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}