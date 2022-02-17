package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.IToken.Kind;

import java.util.List;

public class Parser implements IParser{

    IToken current; //Current token
    IToken lookahead; //One token ahead
    ASTNode tree;
    List<Token> tokenList;
    int index;

    public Parser(List<Token> tokenList){
        this.tokenList = tokenList;
        index = 0;
        current = null;
        lookahead = null;
        consume(); //Loads current and lookahead with values.
    }

    private ASTNode conditionalExpr() throws PLCException {
        ASTNode node = null;
        IToken first = current;

        consume(); //Eat if
        consume(); //Eat (

        ASTNode condition =  expr();

        if(current.getKind() != Kind.RPAREN){
            throw new SyntaxException("Expected )", current.getSourceLocation());
        }
        consume();
        ASTNode trueCase = expr();
        if(current.getKind() != Kind.KW_ELSE){
            throw new SyntaxException("Expected Else", current.getSourceLocation());
        }
        consume();
        ASTNode falseCase = expr();
        if(current.getKind() != Kind.KW_FI){
            throw new SyntaxException("Expected 'fi'", current.getSourceLocation());
        }
        consume();

        node = new ConditionalExpr(first, (Expr) condition, (Expr) trueCase, (Expr) falseCase);

        return node;
    }

    private ASTNode expr() throws PLCException {
        if(current.getKind() == Kind.ERROR){
            throw new LexicalException("Unknow Symbol", current.getSourceLocation());
        }
        ASTNode node = null;
        if(current.getKind() == Kind.KW_IF){
            //CONDITIONAL CALL
            if(lookahead.getKind() != Kind.LPAREN){
                throw new SyntaxException("Missing Left Paren", lookahead.getSourceLocation());
            }
            node = conditionalExpr();
        }
        else{
            node = logicalOr();
        }
        return node;
        /*
        switch (lookahead.getKind()){
            case EOF -> {
                switch (current.getKind()){
                    case BOOLEAN_LIT -> {tree = booleanLit();}
                    case INT_LIT -> {tree = intLit();}
                    case FLOAT_LIT -> {tree = floatLit();}
                    case STRING_LIT -> {tree = stringLit();}
                    case IDENT -> {tree = ident();}
                    default -> {} //SYNTAX ERROR
                }
            }
        }*/
    }

    private ASTNode BinaryExp() throws PLCException {
        ASTNode node = null;
        IToken op = lookahead;
        lookahead = new Token(Kind.EOF); // Temporary change to call EXPR
        ASTNode left = expr();
        consume();
        ASTNode right = expr();
        node = new BinaryExpr(op,(Expr) left,op,(Expr) right);
        return node;
    }

    private ASTNode BinaryExp(ASTNode left) throws  PLCException{
        ASTNode node = null;
        IToken op = lookahead;
        consume();
        consume();
        ASTNode right = expr();
        node = new BinaryExpr(op, (Expr) left, op, (Expr) right);
        return node;
    }

    private ASTNode logicalOr() throws PLCException {
        ASTNode node = null;
        if(lookahead.getKind() == Kind.OR){
            node = BinaryExp();
        }
        else {
            node = logicalAnd();
        }

        return node;
    }

    private ASTNode logicalAnd() throws PLCException {
        ASTNode node = null;
        if(lookahead.getKind() == Kind.AND){
            node = BinaryExp();
        }
        else{
            node = comparison();
        }
        return node;
    }

    private ASTNode comparison() throws PLCException {
        ASTNode node = null;
        switch(lookahead.getKind()){
            case LT,GT,EQUALS, NOT_EQUALS, LE, GE -> {
                /* BINARY EXPRESSION */
                node = BinaryExp();
            }
            default -> {
                node = additive();
            } //ADDITIVE EXPRESSION
        }
        return node;
    }

    private ASTNode additive() throws PLCException {
        ASTNode node = null;
        if(lookahead.getKind() == Kind.PLUS || lookahead.getKind() == Kind.MINUS){
            node = BinaryExp();
        }
        else{
            node = multiplicative();
        }
        return node;
    }

    private ASTNode multiplicative() throws PLCException {
        ASTNode node = null;
        if(lookahead.getKind() == Kind.TIMES || lookahead.getKind() == Kind.DIV ||
                lookahead.getKind() == Kind.MOD){
            BinaryExp();
        }
        else{
            node = unary();
        }

        return node;
    }

    private ASTNode unary() throws PLCException {
        ASTNode node = null;
        IToken op;
        if(current.getKind() == Kind.BANG || current.getKind() == Kind.MINUS
                || current.getKind() == Kind.COLOR_OP || current.getKind() == Kind.IMAGE_OP){
            //We need to call consume and then check for unary again. Or we fall to unarypostfix
            op = current;
            consume();
            IToken binaryOp = lookahead;
            lookahead = new Token(Kind.EOF);
            ASTNode Exp = expr();
            node = new UnaryExpr(op,op,(Expr) Exp);

            if(lookahead.getKind() == Kind.PLUS || lookahead.getKind() == Kind.MINUS ||
                    lookahead.getKind() == Kind.TIMES || lookahead.getKind() == Kind.DIV ||
                    lookahead.getKind() == Kind.MOD){
                node = BinaryExp(node);
            }

        }
        else{
            node = unarypostfix();
        }
        return node;
    }

    private ASTNode unarypostfix() throws PLCException {
        //ident[x,y]
        ASTNode node = null;
        IToken first = current;
        if(lookahead.getKind() == Kind.LSQUARE){
            //Pixel Selector
            lookahead = new Token(Kind.EOF); //Temporarily end of file.
            ASTNode ident = expr(); //Ident - This consumes.
            consume(); //Lookahead is now a comma.
            if(lookahead.getKind() != Kind.COMMA){
                throw new PLCException("Invalid Pixel Selector");
            }
            ASTNode x = expr(); //Get the value of x - This consumes.
            consume();
            ASTNode y = expr();
            if(current.getKind() != Kind.RSQUARE){
                throw new PLCException("Invalid Pixel Selector");
            }

            PixelSelector select = new PixelSelector(first, (Expr) x, (Expr) y);
            node = new UnaryExprPostfix(first, (Expr) ident, select);

            if(lookahead.getKind() == Kind.PLUS || lookahead.getKind() == Kind.MINUS ||
                    lookahead.getKind() == Kind.TIMES || lookahead.getKind() == Kind.DIV ||
                    lookahead.getKind() == Kind.MOD){
                node = BinaryExp(node);
            }
        }
        else{
            node = primaryExpr();
        }
        return node;
    }

    private ASTNode primaryExpr() throws PLCException{
        ASTNode node = null;
        if(current.getKind() == Kind.LPAREN){
            //Expression
            consume(); //Get next token.
            expr();
            if(current.getKind() != Kind.RPAREN){
                throw new PLCException("");
            }
        }

        switch (current.getKind()){
            case INT_LIT -> {node = intLit();}
            case FLOAT_LIT -> {node = floatLit();}
            case BOOLEAN_LIT -> {node = booleanLit();}
            case IDENT -> {node = ident();}
            case STRING_LIT -> {node = stringLit();}
        }
        consume();

        return node;
    }

    private ASTNode booleanLit(){
        ASTNode bool = new BooleanLitExpr(current);
        return bool;
    }

    private ASTNode floatLit(){
        ASTNode fLit = new FloatLitExpr(current);
        return fLit;
    }

    private ASTNode intLit(){
        ASTNode iLit = new IntLitExpr(current);
        return iLit;
    }

    private ASTNode stringLit(){
        ASTNode sLit = new StringLitExpr(current);
        return sLit;
    }
    private ASTNode ident(){
        ASTNode identExpr = new IdentExpr(current);
        return identExpr;
    }
    private void consume(){
        //current = lookahead;
        if(tokenList.get(0).getKind() == Kind.EOF){
            current = tokenList.get(0);
            lookahead = current;
            return;
        }
        if(current == null){ //This is the start of the file.
            current = tokenList.get(index);
        }
        else{
            current = lookahead;
        }
        setLookahead();
    }

    private void setLookahead(){
        index++;
        if(index != tokenList.size()){ //Not At end.
            lookahead = tokenList.get(index);
        }
    }

    @Override
    public ASTNode parse() throws PLCException {
        ASTNode node = null;
        if(current.getKind() == Kind.EOF){
            throw new PLCException("NOTHING TO PARSE");
        }
        node = expr();
        return node;
    }
}
