grammar ZQL;

@header {
package org.zstack.zql.antlr4;
}

zql
    : query
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
    ;

logicalOperator
    : 'and'
    | 'or'
    ;

condition
    : field operator value
    ;

conditions
    : condition (logicalOperator condition)*
    | '('+ condition (logicalOperator condition)* ')'+
    | conditions ('(' logicalOperator conditions ')')+
    ;

queryTarget
    : entity ('.' field)?
    ;

query
    : 'query' queryTarget ('where' conditions)+
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
    : '"' (~'"')+ '"' | '\'' + (~'\'') + '\''
    ;


fragment CHAR
    : 'a'..'z'+
    | 'A'..'Z'+
    ;

fragment NUMBER
    : '0'..'9'+
    ;