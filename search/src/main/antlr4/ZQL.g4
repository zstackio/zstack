grammar ZQL;

@header {
package org.zstack.zql.antlr4;
}

zql
    : query EOF
    ;

entity
    : ID
    ;

field
    : ID
    | ID ('.' ID)+
    ;

operator
    : '='
    | '!='
    | '>'
    | '>='
    | '<'
    | '<='
    | 'is null'
    | 'not null'
    | 'in'
    | 'not in'
    | 'like'
    | 'not like'
    ;

value
    : STRING
    | INT
    | FLOAT
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
    : entity ('.' field)?
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
    ;

restrictBy
    : RESTRICT_BY '(' restrictByExpr (',' restrictByExpr)* ')'
    ;

returnWithExpr
    : ID ('.' ID)*
    ;

returnWith
    : RETURN_WITH '(' returnWithExpr (',' returnWithExpr)* ')'
    ;

subQueryTarget
    : entity ('.' ID)+
    ;

subQuery
    : QUERY subQueryTarget (WHERE condition+)?
    ;

query
    : QUERY queryTarget (WHERE condition+)? restrictBy? returnWith? orderBy? limit? offset?
    ;

OFFSET: 'offset';

LIMIT: 'limit';

QUERY: 'query';

ORDER_BY: 'order by';

ORDER_BY_VALUE: ASC | DESC;

RESTRICT_BY: 'restrict by';

RETURN_WITH: 'return with';

WHERE: 'where';

AND: 'and';

OR: 'or';

ASC: 'asc';

DESC: 'desc';

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
    : '"' (~'"')+ '"' | '\'' + (~'\'') + '\''
    ;


fragment CHAR
    : 'a'..'z'+
    | 'A'..'Z'+
    ;

fragment NUMBER
    : '0'..'9'+
    ;