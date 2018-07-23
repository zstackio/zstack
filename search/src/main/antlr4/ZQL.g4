grammar ZQL;

@header {
package org.zstack.zql.antlr4;
}

zqls
    : zql (';' zql)* EOF
    ;

zql
    : query #queryGrammar
    | count #countGrammar
    | sum #sumGrammar
    ;

entity
    : ID
    ;

field
    : ID
    | ID ('.' ID)+
    ;

multiFields
    : ID (',' ID)+
    ;

operator
    : '='
    | '!='
    | '>'
    | '>='
    | '<'
    | '<='
    | 'is null'
    | 'is not null'
    | 'in'
    | 'not in'
    | 'like'
    | 'not like'
    ;

value
    : STRING
    | INT
    | FLOAT
    | BOOLEAN
    | '(' value (',' value)* ')'
    ;

logicalOperator
    : AND
    | OR
    ;

complexValue
    : value #simpleValue
    | '(' subQuery ')' #subQueryValue
    ;

expr
    : field operator complexValue?
    ;

condition
    : '(' condition ')' #parenthesisCondition
    | left=condition (op=logicalOperator right=condition)+ #nestCondition
    | expr #simpleCondition
    ;

queryTarget
    : entity #onlyEntity
    | entity '.' field #withSingleField
    | entity '.' multiFields #withMultiFields
    ;

orderBy
    : ORDER_BY ID ORDER_BY_VALUE
    ;

limit
    : LIMIT INT
    ;

offset
    : OFFSET INT
    ;


restrictByExpr
    : entity '.' ID operator value?
    | ID operator value?
    ;

restrictBy
    : RESTRICT_BY '(' restrictByExpr (',' restrictByExpr)* ')'
    ;

returnWithExprBlock
    : '{' (~'}' | returnWithExprBlock)* '}'
    ;

returnWithExpr
    : ID ('.' ID)* #returnWithExprId
    | ID returnWithExprBlock #returnWithExprFunction
    ;

returnWith
    : RETURN_WITH '(' returnWithExpr (',' returnWithExpr)* ')'
    ;

groupByExpr
    : ID (',' ID)*
    ;

groupBy
    : GROUP_BY groupByExpr
    ;

subQueryTarget
    : entity ('.' ID)+
    ;

subQuery
    : QUERY subQueryTarget (WHERE condition+)?
    ;


filterByExprBlock
    : '{' (~'}' | filterByExprBlock)* '}'
    ;

filterByExpr
    : ID filterByExprBlock
    ;

filterBy
    : FILTER_BY filterByExpr (',' filterByExpr)*
    ;

namedAsValue
    : STRING
    ;

namedAs
    : NAMED_AS namedAsValue
    ;

query
    : QUERY queryTarget (WHERE condition+)? restrictBy? returnWith? groupBy? orderBy? limit? offset? filterBy? namedAs?
    ;

count
    : COUNT queryTarget (WHERE condition+)? restrictBy? groupBy? orderBy? limit? offset? namedAs?
    ;

sumByValue
    : ID
    ;

sumBy
    : 'by' sumByValue
    ;

sum
    : SUM queryTarget sumBy (WHERE condition+)? namedAs?
    ;


FILTER_BY: 'filter by';

OFFSET: 'offset';

LIMIT: 'limit';

QUERY: 'query';

COUNT: 'count';

SUM: 'sum';

ORDER_BY: 'order by';

GROUP_BY: 'group by';

NAMED_AS: 'named as';

ORDER_BY_VALUE: ASC | DESC;

RESTRICT_BY: 'restrict by';

RETURN_WITH: 'return with';

WHERE: 'where';

AND: 'and';

OR: 'or';

ASC: 'asc';

DESC: 'desc';

BOOLEAN
    : 'true'
    | 'false'
    ;

INT : '-'? NUMBER;

FLOAT
    : INT '.' NUMBER+
    ;

ID
    : [a-zA-Z0-9_]+
    ;

WS
    : [ \t\r\n]+ -> skip ;

STRING
    : '"' (~'"')* '"'
    | '\'' (~'\'')* '\''
    ;


fragment CHAR
    : 'a'..'z'+
    | 'A'..'Z'+
    ;

fragment NUMBER
    : '0'..'9'+
    ;