parser grammar OneLinkParser;
options{ tokenVocab=OneLinkLexer; }
// OneLink Grammar
compliationUnit : unitseq;
unitseq :   importStmt  unitseq
        |   tinyApp     unitseq
        |   policy      unitseq
        |
        ;
importStmt  :   IMPORT  Stringliteral Semi;
tinyApp :   TINYAPP   Identifier    MOBILE  LeftBrace  appBlocks    RightBrace idseq    Semi    #mobileAPP
        |   TINYAPP   Identifier    CLOUD   LeftBrace  appBlocks    RightBrace idseq    Semi    #deviceAPP
        |   TINYAPP   Identifier    LeftBrace  appBlocks   RightBrace   idseq  Semi             #deviceAPP
        ;
policy  :   POLICY    Identifier    LeftBrace policyBlocks RightBrace   idseq  Semi;
appBlocks   :   interfaceBlock  appBlocks
            |   programBlock    appBlocks
            |
            ;
policyBlocks:   interfaceBlock  policyBlocks
            |   ruleBlock    policyBlocks
            |
            ;
idseq   :   Identifier  Comma   idseq
        |   Identifier
        ;
interfaceBlock   :  INTERFACE Colon declarationseq;
programBlock :  PROGRAMSTART translationunit;
ruleBlock    :  RULE    Colon   stmtseq;    //TODO
paralist:   parameter paralist
        |
        ;
parameter   :   expr;
// Interface
data    :   DATA_T  idseq   Semi;
event   :   EVENT_T Identifier  LeftParen list RightParen Semi;
service :   simpletypespecifier Identifier  LeftParen paralist RightParen Semi;
// Rule
list    :   LeftBrace exprseq  RightBrace
        |   LeftBrace RightBrace; // Empty list
exprseq :   expr Comma exprseq
        |   expr
        ;
stmtseq :   stmt stmtseq
        |
        ;
stmt    :   exprStmt    Semi
        |   ifStmt
        |   forStmt
        |   compStmt
        ;
exprStmt:   connectExpr
        |   allStmt
        |   anyStmt
        ;
ifStmt  :   IF LeftParen expr RightParen stmt ELSE stmt WITHIN LeftParen deadline Comma missRatio RightParen Semi
        |   IF LeftParen expr RightParen stmt ELSE stmt
        |   IF LeftParen expr RightParen stmt WITHIN LeftParen deadline Comma missRatio RightParen Semi
        |   IF LeftParen expr RightParen stmt
        ;
forStmt :   FOR LeftParen Identifier IN list RightParen stmt;
allStmt :   ALL LeftParen expr RightParen;//TODO
anyStmt :   ANY LeftParen expr RightParen;//TODO
compStmt:   LeftBrace stmtseq RightBrace
        ;
idExpr  :   Identifier;
callExpr:   Identifier LeftParen paralist RightParen;

numberLiteral:  Integerliteral|Floatingliteral;
deadline:   numberLiteral;
missRatio   :   numberLiteral;
connectExpr :   connectExpr Doublecolon idExpr
            |   connectExpr Doublecolon callExpr
            |   connectExpr Dot idExpr
            |   connectExpr Dot callExpr
            |   idExpr
            |   list
            ;
expr    :   list                            #listexpr
        |   exprStmt                        #stmtexpr
        |   Integerliteral                  #intexpr
        |   Floatingliteral                 #floatexpr
        |   Stringliteral                   #stringexpr
        |   booleanliteral                  #boolexpr
        |   LeftParen expr RightParen       #parenexpr
        |   Not     expr                    #notexpr
        |   Minus   expr                    #minusexpr
        |   expr    Star    expr            #mulexpr
        |   expr    Div     expr            #divexpr
        |   expr    Mod     expr            #modexpr
        |   expr    Plus    expr            #addexpr
        |   expr    Minus   expr            #subexpr
        |   expr    Greater expr            #gtexpr
        |   expr    GreaterEqual    expr    #geexpr
        |   expr    Equal   expr            #eqexpr
        |   expr    NotEqual    expr        #neqexpr
        |   expr    LessEqual   expr        #leexpr
        |   expr    Less    expr            #ltexpr
        |   expr    AndAnd  expr            #andexpr
        |   expr    OrOr    expr            #orexpr
        ;



// Mobile program
type    :   BUTTON_T
        |   TEXT_T
        ;

// C++14 Grammar

/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Camilo Sanchez (Camiloasc1)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

/*******************************************************************************
 * C++14 Grammar for ANTLR v4
 *
 * Based on n4140 draft paper
 * https://github.com/cplusplus/draft/blob/master/papers/n4140.pdf
 * and
 * http://www.nongnu.org/hcb/
 *
 * Possible Issues:
 *
 * Input must avoid conditional compilation blocks (this grammar ignores any preprocessor directive)
 * GCC extensions not yet supported (do not try to parse the preprocessor output)
 * Right angle bracket (C++11) - Solution '>>' and '>>=' are not tokens, only '>'
 * Lexer issue with pure-specifier rule ('0' token) - Solution in embedded code
 *   Change it to match the target language you want in line 1097 or verify inside your listeners/visitors
 *   Java:
if($val.text.compareTo("0")!=0) throw new InputMismatchException(this);
 *   JavaScript:

 *   Python2:

 *   Python3:

 *   C#:

 ******************************************************************************/

/*Basic concepts*/
translationunit
:
	declarationseq?
;

/*Expressions*/
primaryexpression
:
	literal
	| This
	| LeftParen expression RightParen
	| idexpression
	| lambdaexpression
;

idexpression
:
	unqualifiedid
	| qualifiedid
;

unqualifiedid
:
	Identifier
	| operatorfunctionid
	| conversionfunctionid
	| literaloperatorid
	| '~' classname
	| '~' decltypespecifier
	| templateid
;

qualifiedid
:
	nestednamespecifier Template? unqualifiedid
;

nestednamespecifier
:
	Doublecolon
	| thetypename Doublecolon
	| namespacename Doublecolon
	| decltypespecifier Doublecolon
	| nestednamespecifier Identifier Doublecolon
	| nestednamespecifier Template? simpletemplateid Doublecolon
;

lambdaexpression
:
	lambdaintroducer lambdadeclarator? compoundstatement
;

lambdaintroducer
:
	'[' lambdacapture? ']'
;

lambdacapture
:
	capturedefault
	| capturelist
	| capturedefault Comma capturelist
;

capturedefault
:
	'&'
	| Assign
;

capturelist
:
	capture '...'?
	| capturelist Comma capture '...'?
;

capture
:
	simplecapture
	| initcapture
;

simplecapture
:
	Identifier
	| '&' Identifier
	| This
;

initcapture
:
	Identifier initializer
	| '&' Identifier initializer
;

lambdadeclarator
:
	LeftParen parameterdeclarationclause RightParen Mutable? exceptionspecification?
	attributespecifierseq? trailingreturntype?
;

postfixexpression
:
	primaryexpression
	| postfixexpression '[' expression ']'
	| postfixexpression '[' bracedinitlist ']'
	| postfixexpression LeftParen expressionlist? RightParen
	| simpletypespecifier LeftParen expressionlist? RightParen
	| typenamespecifier LeftParen expressionlist? RightParen
	| simpletypespecifier bracedinitlist
	| typenamespecifier bracedinitlist
	| postfixexpression Dot Template? idexpression
	| postfixexpression '->' Template? idexpression
	| postfixexpression Dot pseudodestructorname
	| postfixexpression '->' pseudodestructorname
	| postfixexpression '++'
	| postfixexpression '--'
	| Dynamic_cast Less thetypeid Greater LeftParen expression RightParen
	| Static_cast Less thetypeid Greater LeftParen expression RightParen
	| Reinterpret_cast Less thetypeid Greater LeftParen expression RightParen
	| Const_cast Less thetypeid Greater LeftParen expression RightParen
	| typeidofthetypeid LeftParen expression RightParen
	| typeidofthetypeid LeftParen thetypeid RightParen
;

/*
add a middle layer to eliminate duplicated function declarations
*/
typeidofexpr
:
	Typeid
;
typeidofthetypeid
:
	Typeid
;

expressionlist
:
	initializerlist
;

pseudodestructorname
:
	nestednamespecifier? thetypename Doublecolon '~' thetypename
	| nestednamespecifier Template simpletemplateid Doublecolon '~' thetypename
	| nestednamespecifier? '~' thetypename
	| '~' decltypespecifier
;

unaryexpression
:
	postfixexpression
	| '++' castexpression
	| '--' castexpression
	| unaryoperator castexpression
	| Sizeof unaryexpression
	| Sizeof LeftParen thetypeid RightParen
	| Sizeof '...' LeftParen Identifier RightParen
	| Alignof LeftParen thetypeid RightParen
	| noexceptexpression
	| newexpression
	| deleteexpression
;

unaryoperator
:
	'|'
	| Star
	| '&'
	| Plus
	| Not
	| '~'
	| Minus
;

newexpression
:
	Doublecolon? New newplacement? newtypeid newinitializer?
	| Doublecolon? New newplacement? LeftParen thetypeid RightParen newinitializer?
;

newplacement
:
	LeftParen expressionlist RightParen
;

newtypeid
:
	typespecifierseq newdeclarator?
;

newdeclarator
:
	ptroperator newdeclarator?
	| noptrnewdeclarator
;

noptrnewdeclarator
:
	'[' expression ']' attributespecifierseq?
	| noptrnewdeclarator '[' constantexpression ']' attributespecifierseq?
;

newinitializer
:
	LeftParen expressionlist? RightParen
	| bracedinitlist
;

deleteexpression
:
	Doublecolon? Delete castexpression
	| Doublecolon? Delete '[' ']' castexpression
;

noexceptexpression
:
	Noexcept LeftParen expression RightParen
;

castexpression
:
	unaryexpression
	| LeftParen thetypeid RightParen castexpression
;

pmexpression
:
	castexpression
	| pmexpression '.*' castexpression
	| pmexpression '->*' castexpression
;

multiplicativeexpression
:
	pmexpression
	| multiplicativeexpression Star pmexpression
	| multiplicativeexpression Div pmexpression
	| multiplicativeexpression Mod pmexpression
;

additiveexpression
:
	multiplicativeexpression
	| additiveexpression Plus multiplicativeexpression
	| additiveexpression Minus multiplicativeexpression
;

shiftexpression
:
	additiveexpression
	| shiftexpression '<<' additiveexpression
	| shiftexpression rightShift additiveexpression
;

relationalexpression
:
	shiftexpression
	| relationalexpression Less shiftexpression
	| relationalexpression Greater shiftexpression
	| relationalexpression LessEqual shiftexpression
	| relationalexpression GreaterEqual shiftexpression
;

equalityexpression
:
	relationalexpression
	| equalityexpression Equal relationalexpression
	| equalityexpression NotEqual relationalexpression
;

andexpression
:
	equalityexpression
	| andexpression '&' equalityexpression
;

exclusiveorexpression
:
	andexpression
	| exclusiveorexpression '^' andexpression
;

inclusiveorexpression
:
	exclusiveorexpression
	| inclusiveorexpression '|' exclusiveorexpression
;

logicalandexpression
:
	inclusiveorexpression
	| logicalandexpression AndAnd inclusiveorexpression
;

logicalorexpression
:
	logicalandexpression
	| logicalorexpression OrOr logicalandexpression
;

conditionalexpression
:
	logicalorexpression
	| logicalorexpression '?' expression Colon assignmentexpression
;

assignmentexpression
:
	conditionalexpression
	| logicalorexpression assignmentoperator initializerclause
	| throwexpression
;

assignmentoperator
:
	Assign
	| '*='
	| '/='
	| '%='
	| PlusAssign
	| '-='
	| rightShiftAssign
	| '<<='
	| '&='
	| '^='
	| '|='
;

expression
:
	assignmentexpression
	| expression Comma assignmentexpression
;

constantexpression
:
	conditionalexpression
;
/*Statements*/
statement
:
	labeledstatement
	| attributespecifierseq? expressionstatement
	| attributespecifierseq? compoundstatement
	| attributespecifierseq? selectionstatement
	| attributespecifierseq? iterationstatement
	| attributespecifierseq? jumpstatement
	| declarationstatement
	| attributespecifierseq? tryblock
;

labeledstatement
:
	attributespecifierseq? Identifier Colon statement
	| attributespecifierseq? Case constantexpression Colon statement
	| attributespecifierseq? Default Colon statement
;

expressionstatement
:
	expression? Semi
;

compoundstatement
:
	LeftBrace statementseq? RightBrace
;

statementseq
:
	statement
	| statementseq statement
;

selectionstatement
:
	If LeftParen condition RightParen statement
	| If LeftParen condition RightParen statement Else statement
	| Switch LeftParen condition RightParen statement
;

condition
:
	expression
	| attributespecifierseq? declspecifierseq declarator Assign initializerclause
	| attributespecifierseq? declspecifierseq declarator bracedinitlist
;

iterationstatement
:
	While LeftParen condition RightParen statement
	| Do statement While LeftParen expression RightParen Semi
	| For LeftParen forinitstatement condition? Semi expression? RightParen statement
	| For LeftParen forrangedeclaration Colon forrangeinitializer RightParen statement
;

forinitstatement
:
	expressionstatement
	| simpledeclaration
;

forrangedeclaration
:
	attributespecifierseq? declspecifierseq declarator
;

forrangeinitializer
:
	expression
	| bracedinitlist
;

jumpstatement
:
	Break Semi
	| Continue Semi
	| Return expression? Semi
	| Return bracedinitlist Semi
	| Goto Identifier Semi
;

declarationstatement
:
	blockdeclaration
;

/*Declarations*/
declarationseq
:
	declaration
	| declarationseq declaration
;

declaration
:
	blockdeclaration
	| functiondefinition
	| templatedeclaration
	| explicitinstantiation
	| explicitspecialization
	| linkagespecification
	| namespacedefinition
	| emptydeclaration
	| attributedeclaration
;

blockdeclaration
:
	simpledeclaration
	| asmdefinition
	| namespacealiasdefinition
	| usingdeclaration
	| usingdirective
	| static_assertdeclaration
	| aliasdeclaration
	| opaqueenumdeclaration
;

aliasdeclaration
:
	Using Identifier attributespecifierseq? Assign thetypeid Semi
;

simpledeclaration
:
	declspecifierseq? initdeclaratorlist? Semi
	| attributespecifierseq declspecifierseq? initdeclaratorlist Semi
;

static_assertdeclaration
:
	Static_assert LeftParen constantexpression Comma Stringliteral RightParen Semi
;

emptydeclaration
:
	Semi
;

attributedeclaration
:
	attributespecifierseq Semi
;

declspecifier
:
	storageclassspecifier
	| typespecifier
	| functionspecifier
	| Friend
	| Typedef
	| Constexpr
;

declspecifierseq
:
	declspecifier attributespecifierseq?
	| declspecifier declspecifierseq
;

storageclassspecifier
:
	Register
	| Static
	| Thread_local
	| Extern
	| Mutable
;

functionspecifier
:
	Inline
	| Virtual
	| Explicit
;

typedefname
:
	Identifier
;

typespecifier
:
	trailingtypespecifier
	| classspecifier
	| enumspecifier
;

trailingtypespecifier
:
	simpletypespecifier
	| elaboratedtypespecifier
	| typenamespecifier
	| cvqualifier
;

typespecifierseq
:
	typespecifier attributespecifierseq?
	| typespecifier typespecifierseq
;

trailingtypespecifierseq
:
	trailingtypespecifier attributespecifierseq?
	| trailingtypespecifier trailingtypespecifierseq
;

simpletypespecifier
:
	nestednamespecifier? thetypename
	| nestednamespecifier Template simpletemplateid
	| Char
	| Char16
	| Char32
	| Wchar
	| Bool
	| Short
	| Int
	| Long
	| Signed
	| Unsigned
	| Float
	| Double
	| Void
	| Auto
	| DATA_T
	| EVENT_T
	| BUTTON_T
	| TEXT_T
	| decltypespecifier
;

thetypename
:
	classname
	| enumname
	| typedefname
	| simpletemplateid
;

decltypespecifier
:
	Decltype LeftParen expression RightParen
	| Decltype LeftParen Auto RightParen
;

elaboratedtypespecifier
:
	classkey attributespecifierseq? nestednamespecifier? Identifier
	| classkey simpletemplateid
	| classkey nestednamespecifier Template? simpletemplateid
	| Enum nestednamespecifier? Identifier
;

enumname
:
	Identifier
;

enumspecifier
:
	enumhead LeftBrace enumeratorlist? RightBrace
	| enumhead LeftBrace enumeratorlist Comma RightBrace
;

enumhead
:
	enumkey attributespecifierseq? Identifier? enumbase?
	| enumkey attributespecifierseq? nestednamespecifier Identifier enumbase?
;

opaqueenumdeclaration
:
	enumkey attributespecifierseq? Identifier enumbase? Semi
;

enumkey
:
	Enum
	| Enum Class
	| Enum Struct
;

enumbase
:
	Colon typespecifierseq
;

enumeratorlist
:
	enumeratordefinition
	| enumeratorlist Comma enumeratordefinition
;

enumeratordefinition
:
	enumerator
	| enumerator Assign constantexpression
;

enumerator
:
	Identifier
;

namespacename
:
	originalnamespacename
	| namespacealias
;

originalnamespacename
:
	Identifier
;

namespacedefinition
:
	namednamespacedefinition
	| unnamednamespacedefinition
;

namednamespacedefinition
:
	originalnamespacedefinition
	| extensionnamespacedefinition
;

originalnamespacedefinition
:
	Inline? Namespace Identifier LeftBrace namespacebody RightBrace
;

extensionnamespacedefinition
:
	Inline? Namespace originalnamespacename LeftBrace namespacebody RightBrace
;

unnamednamespacedefinition
:
	Inline? Namespace LeftBrace namespacebody RightBrace
;

namespacebody
:
	declarationseq?
;

namespacealias
:
	Identifier
;

namespacealiasdefinition
:
	Namespace Identifier Assign qualifiednamespacespecifier Semi
;

qualifiednamespacespecifier
:
	nestednamespecifier? namespacename
;

usingdeclaration
:
	Using Typename? nestednamespecifier unqualifiedid Semi
	| Using Doublecolon unqualifiedid Semi
;

usingdirective
:
	attributespecifierseq? Using Namespace nestednamespecifier? namespacename Semi
;

asmdefinition
:
	Asm LeftParen Stringliteral RightParen Semi
;

linkagespecification
:
	Extern Stringliteral LeftBrace declarationseq? RightBrace
	| Extern Stringliteral declaration
;

attributespecifierseq
:
	attributespecifier
	| attributespecifierseq attributespecifier
;

attributespecifier
:
	'[' '[' attributelist ']' ']'
	| alignmentspecifier
;

alignmentspecifier
:
	Alignas LeftParen thetypeid '...'? RightParen
	| Alignas LeftParen constantexpression '...'? RightParen
;

attributelist
:
	attribute?
	| attributelist Comma attribute?
	| attribute '...'
	| attributelist Comma attribute '...'
;

attribute
:
	attributetoken attributeargumentclause?
;

attributetoken
:
	Identifier
	| attributescopedtoken
;

attributescopedtoken
:
	attributenamespace Doublecolon Identifier
;

attributenamespace
:
	Identifier
;

attributeargumentclause
:
	LeftParen balancedtokenseq RightParen
;

balancedtokenseq
:
	balancedtoken?
	| balancedtokenseq balancedtoken
;

balancedtoken
:
	LeftParen balancedtokenseq RightParen
	| '[' balancedtokenseq ']'
	| LeftBrace balancedtokenseq RightBrace
	/*any token other than a parenthesis , a bracket , or a brace*/
;

/*Declarators*/
initdeclaratorlist
:
	initdeclarator
	| initdeclaratorlist Comma initdeclarator
;

initdeclarator
:
	declarator initializer?
;

declarator
:
	ptrdeclarator
	| noptrdeclarator parametersandqualifiers trailingreturntype
;

ptrdeclarator
:
	noptrdeclarator
	| ptroperator ptrdeclarator
;

noptrdeclarator
:
	declaratorid attributespecifierseq?
	| noptrdeclarator parametersandqualifiers
	| noptrdeclarator '[' constantexpression? ']' attributespecifierseq?
	| LeftParen ptrdeclarator RightParen
;

parametersandqualifiers
:
	LeftParen parameterdeclarationclause RightParen cvqualifierseq? refqualifier?
	exceptionspecification? attributespecifierseq?
;

trailingreturntype
:
	'->' trailingtypespecifierseq abstractdeclarator?
;

ptroperator
:
	Star attributespecifierseq? cvqualifierseq?
	| '&' attributespecifierseq?
	| AndAnd attributespecifierseq?
	| nestednamespecifier Star attributespecifierseq? cvqualifierseq?
;

cvqualifierseq
:
	cvqualifier cvqualifierseq?
;

cvqualifier
:
	Const
	| Volatile
;

refqualifier
:
	'&'
	| AndAnd
;

declaratorid
:
	'...'? idexpression
;

thetypeid
:
	typespecifierseq abstractdeclarator?
;

abstractdeclarator
:
	ptrabstractdeclarator
	| noptrabstractdeclarator? parametersandqualifiers trailingreturntype
	| abstractpackdeclarator
;

ptrabstractdeclarator
:
	noptrabstractdeclarator
	| ptroperator ptrabstractdeclarator?
;

noptrabstractdeclarator
:
	noptrabstractdeclarator parametersandqualifiers
	| parametersandqualifiers
	| noptrabstractdeclarator '[' constantexpression? ']' attributespecifierseq?
	| '[' constantexpression? ']' attributespecifierseq?
	| LeftParen ptrabstractdeclarator RightParen
;

abstractpackdeclarator
:
	noptrabstractpackdeclarator
	| ptroperator abstractpackdeclarator
;

noptrabstractpackdeclarator
:
	noptrabstractpackdeclarator parametersandqualifiers
	| noptrabstractpackdeclarator '[' constantexpression? ']'
	attributespecifierseq?
	| '...'
;

parameterdeclarationclause
:
	parameterdeclarationlist? '...'?
	| parameterdeclarationlist Comma '...'
;

parameterdeclarationlist
:
	parameterdeclaration
	| parameterdeclarationlist Comma parameterdeclaration
;

parameterdeclaration
:
	attributespecifierseq? declspecifierseq declarator
	| attributespecifierseq? declspecifierseq declarator Assign initializerclause
	| attributespecifierseq? declspecifierseq abstractdeclarator?
	| attributespecifierseq? declspecifierseq abstractdeclarator? Assign
	initializerclause
;

functiondefinition
:
	attributespecifierseq? declspecifierseq? declarator virtspecifierseq?
	functionbody
;

functionbody
:
	ctorinitializer? compoundstatement
	| functiontryblock
	| Assign Default Semi
	| Assign Delete Semi
;

initializer
:
	braceorequalinitializer
	| LeftParen expressionlist RightParen
;

braceorequalinitializer
:
	Assign initializerclause
	| bracedinitlist
;

initializerclause
:
	assignmentexpression
	| bracedinitlist
;

initializerlist
:
	initializerclause '...'?
	| initializerlist Comma initializerclause '...'?
;

bracedinitlist
:
	LeftBrace initializerlist Comma? RightBrace
	| LeftBrace RightBrace
;

/*Classes*/
classname
:
	Identifier
	| simpletemplateid
;

classspecifier
:
	classhead LeftBrace memberspecification? RightBrace
;

classhead
:
	classkey attributespecifierseq? classheadname classvirtspecifier? baseclause?
	| classkey attributespecifierseq? baseclause?
;

classheadname
:
	nestednamespecifier? classname
;

classvirtspecifier
:
	Final
;

classkey
:
	Class
	| Struct
	| Union
;

memberspecification
:
	memberdeclaration memberspecification?
	| accessspecifier Colon memberspecification?
;

memberdeclaration
:
	attributespecifierseq? declspecifierseq? memberdeclaratorlist? Semi
	| functiondefinition
	| usingdeclaration
	| static_assertdeclaration
	| templatedeclaration
	| aliasdeclaration
	| emptydeclaration
;

memberdeclaratorlist
:
	memberdeclarator
	| memberdeclaratorlist Comma memberdeclarator
;

memberdeclarator
:
	declarator virtspecifierseq? purespecifier?
	| declarator braceorequalinitializer?
	| Identifier? attributespecifierseq? Colon constantexpression
;

virtspecifierseq
:
	virtspecifier
	| virtspecifierseq virtspecifier
;

virtspecifier
:
	Override
	| Final
;

/*
purespecifier:
	Assign '0'//Conflicts with the lexer
 ;
 */
purespecifier
:
	Assign val = Octalliteral
	{if($val.text.compareTo("0")!=0) throw new InputMismatchException(this);}

;

/*Derived classes*/
baseclause
:
	Colon basespecifierlist
;

basespecifierlist
:
	basespecifier '...'?
	| basespecifierlist Comma basespecifier '...'?
;

basespecifier
:
	attributespecifierseq? basetypespecifier
	| attributespecifierseq? Virtual accessspecifier? basetypespecifier
	| attributespecifierseq? accessspecifier Virtual? basetypespecifier
;

classordecltype
:
	nestednamespecifier? classname
	| decltypespecifier
;

basetypespecifier
:
	classordecltype
;

accessspecifier
:
	Private
	| Protected
	| Public
;

/*Special member functions*/
conversionfunctionid
:
	Operator conversiontypeid
;

conversiontypeid
:
	typespecifierseq conversiondeclarator?
;

conversiondeclarator
:
	ptroperator conversiondeclarator?
;

ctorinitializer
:
	Colon meminitializerlist
;

meminitializerlist
:
	meminitializer '...'?
	| meminitializer '...'? Comma meminitializerlist
;

meminitializer
:
	meminitializerid LeftParen expressionlist? RightParen
	| meminitializerid bracedinitlist
;

meminitializerid
:
	classordecltype
	| Identifier
;

/*Overloading*/
operatorfunctionid
:
	Operator theoperator
;

literaloperatorid
:
	Operator Stringliteral Identifier
	| Operator Userdefinedstringliteral
;

/*Templates*/
templatedeclaration
:
	Template Less templateparameterlist Greater declaration
;

templateparameterlist
:
	templateparameter
	| templateparameterlist Comma templateparameter
;

templateparameter
:
	typeparameter
	| parameterdeclaration
;

typeparameter
:
	Class '...'? Identifier?
	| Class Identifier? Assign thetypeid
	| Typename '...'? Identifier?
	| Typename Identifier? Assign thetypeid
	| Template Less templateparameterlist Greater Class '...'? Identifier?
	| Template Less templateparameterlist Greater Class Identifier? Assign idexpression
;

simpletemplateid
:
	templatename Less templateargumentlist? Greater
;

templateid
:
	simpletemplateid
	| operatorfunctionid Less templateargumentlist? Greater
	| literaloperatorid Less templateargumentlist? Greater
;

templatename
:
	Identifier
;

templateargumentlist
:
	templateargument '...'?
	| templateargumentlist Comma templateargument '...'?
;

templateargument
:
	thetypeid
	| constantexpression
	| idexpression
;

typenamespecifier
:
	Typename nestednamespecifier Identifier
	| Typename nestednamespecifier Template? simpletemplateid
;

explicitinstantiation
:
	Extern? Template declaration
;

explicitspecialization
:
	Template Less Greater declaration
;

/*Exception handling*/
tryblock
:
	Try compoundstatement handlerseq
;

functiontryblock
:
	Try ctorinitializer? compoundstatement handlerseq
;

handlerseq
:
	handler handlerseq?
;

handler
:
	Catch LeftParen exceptiondeclaration RightParen compoundstatement
;

exceptiondeclaration
:
	attributespecifierseq? typespecifierseq declarator
	| attributespecifierseq? typespecifierseq abstractdeclarator?
	| '...'
;

throwexpression
:
	Throw assignmentexpression?
;

exceptionspecification
:
	dynamicexceptionspecification
	| noexceptspecification
;

dynamicexceptionspecification
:
	Throw LeftParen typeidlist? RightParen
;

typeidlist
:
	thetypeid '...'?
	| typeidlist Comma thetypeid '...'?
;

noexceptspecification
:
	Noexcept LeftParen constantexpression RightParen
	| Noexcept
;

rightShift
:
//'>>'
	Greater Greater
;

rightShiftAssign
:
//'>>='
	Greater Greater Assign
;

theoperator
:
	New
	| Delete
	| New '[' ']'
	| Delete '[' ']'
	| Plus
	| Minus
	| Star
	| Div
	| Mod
	| '^'
	| '&'
	| '|'
	| '~'
	| Not
	| Assign
	| Less
	| Greater
	| PlusAssign
	| '-='
	| '*='
	| '/='
	| '%='
	| '^='
	| '&='
	| '|='
	| '<<'
	| rightShift
	| rightShiftAssign
	| '<<='
	| Equal
	| NotEqual
	| LessEqual
	| GreaterEqual
	| AndAnd
	| OrOr
	| '++'
	| '--'
	| Comma
	| '->*'
	| '->'
	| LeftParen RightParen
	| '[' ']'
;

literal
:
	Integerliteral
	| Characterliteral
	| Floatingliteral
	| Stringliteral
	| booleanliteral
	| pointerliteral
	| userdefinedliteral
;

booleanliteral
:
	False
	| True
;

pointerliteral
:
	Nullptr
;

userdefinedliteral
:
	Userdefinedintegerliteral
	| Userdefinedfloatingliteral
	| Userdefinedstringliteral
	| Userdefinedcharacterliteral
;

// C-- Grammar

//ext_def_list : ext_def ext_def_list
//                |
//                ;
//ext_def : specifier ext_dec_list Semi
//            | specifier Semi
//            | specifier func_dec comp_st
//            ;
//ext_dec_list : var_dec
//                | var_dec Comma ext_dec_list
//                ;
//
//specifier : type
//            | struct_specifier
//            ;
//type : INT
//        | FLOAT
//        | CHAR
//        ;
//struct_specifier : STRUCT opt_tag LC def_list RC
//                    | STRUCT tag
//                    ;
//opt_tag : ID
//            |
//            ;
//tag : ID;
//
//var_dec : ID
//            | var_dec LB INT_LITERAL RB
//            ;
//func_dec : ID LP var_list RP
//            | ID LP RP
//            ;
//var_list : param_dec
//            | param_dec Comma var_list
//            ;
//param_dec :specifier var_dec;
//
//comp_st : LC def_list stmt_list RC ;
//stmt_list : stmt stmt_list
//            |
//            ;
//stmt : exp Semi
//        | comp_st
//        | RETURN exp Semi
////        | IF LP exp RP stmt
//        | IF LP exp RP stmt ELSE stmt
//        | WHILE LP exp RP stmt
//        | FOR LP exp Semi exp Semi exp Semi RP stmt
//        ;
//
//def_list : def def_list
//            |
//            ;
//def : specifier dec_list Semi;
//dec_list : dec
//            | dec Comma dec_list
//            ;
//dec : var_dec
//        | var_dec ASSIGN exp
//        ;
//
//exp :   exp ASSIGN  exp
//        | exp AND   exp
//        | exp OR    exp
//        | exp GT    exp
//        | exp GE    exp
//        | exp LT    exp
//        | exp LE    exp
//        | exp EQ    exp
//        | exp NEQ   exp
//        | exp PLUS  exp
//        | exp MINUS exp
//        | exp MUL   exp
//        | exp DIV   exp
//        | exp MOD   exp
//        | LeftParen   exp RightParen
//        | MINUS exp
//        | NOT   exp
//        | exp   LP  args    RP
//        | exp   LP  RP
//        | exp   LB  exp RB
//        | exp   DOT exp
//        | ID
//        | INT_LITERAL
//        | FLOAT_LITERAL
//        | STRING_LITERAL
//        | CHAR_LITERAL
//        ;
//
//args    :   exp
//        |   exp args
//        ;
//

