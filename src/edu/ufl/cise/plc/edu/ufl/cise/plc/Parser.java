package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types;
import edu.ufl.cise.plc.IToken.Kind;

import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser{

    IToken current; //Current token
    IToken lookahead; //One token ahead
    List<Token> tokenList;
    int index;
    boolean paren;

    public Parser(List<Token> tokenList){
        this.tokenList = tokenList;
        index = 0;
        current = null;
        lookahead = null;
        paren = false;
        consume(); //Loads current and lookahead with values.
    }

    private ASTNode Prog() throws PLCException{
        IToken first = current;
        ASTNode node = null;
        List<NameDef> params = new ArrayList<>();
        List<ASTNode> decs = new ArrayList<>();
        String type;
        Types.Type test;
        try{
            test = validateType();
        }
        catch (IllegalArgumentException e){
            throw new SyntaxException("Illegal return type", current.getSourceLocation());
        }
        if(validateType() == null){
            if(current.getKind() == Kind.KW_VOID){
                //type = "void";
                test = Types.Type.VOID;
            }
        }
        if(test != null){
            type = current.getText();
            consume(); //Eat Type
            if(current.getKind() != Kind.IDENT){
                throw new SyntaxException("Program name must be a valid identifier!", current.getSourceLocation());
            }
            String name = current.getText();
            consume(); // Eat Ident
            if(current.getKind() != Kind.LPAREN){
                throw new SyntaxException("Expected '('", current.getSourceLocation());
            }
            consume();//Consume LPAREN
            while(current.getKind()!= Kind.RPAREN){
                    params.add((NameDef) nameDef());
                    if(current.getKind() == Kind.COMMA && lookahead.getKind() == Kind.RPAREN){
                        throw new SyntaxException("Invalid params", current.getSourceLocation());
                    }
                    else if(current.getKind() != Kind.RPAREN){
                        consume();
                    }
            }
            consume();//Eat RPAREN

            //TODO: Statements and Declarations go here.
            while(current.getKind() != Kind.EOF){
                decs.add(Declaration());
            }

            Types.Type t = Types.Type.toType(type);
            node = new Program(first, t, name, params, decs);

        }
        else{
            throw new SyntaxException("Program must have return type.", current.getSourceLocation());
        }

        return node;
    }

    private ASTNode Statement() throws PLCException{
        IToken first = current;
        ASTNode node = null;
        IToken op;
        if(current.getKind() == Kind.RETURN){
            op = current;
            consume();
            node = expr();
            node = new ReturnStatement(first,(Expr) node);
        }
        else if(current.getKind() == Kind.KW_WRITE){
            op = current;
            consume();
            ASTNode source = expr();
            ASTNode dest;
            if(current.getKind() != Kind.RARROW){
                throw new SyntaxException("Expected RARROW", current.getSourceLocation());
            }
            consume(); //Eat RARROW.
            dest = expr();
            node = new WriteStatement(first,(Expr) source, (Expr) dest);

        }
        else if(current.getKind() == Kind.IDENT){
            ASTNode ident = unary();
            PixelSelector selector = null;
            if(ident instanceof UnaryExprPostfix){
                selector = ((UnaryExprPostfix) ident).getSelector();
            }
            ASTNode val;
            if(current.getKind() == Kind.ASSIGN || current.getKind() == Kind.LARROW){
                op = current;
                consume();
                val = expr();
            }
            else{
                throw new SyntaxException("Expected '=' or '<-'", current.getSourceLocation());
            }
            if(op.getKind() == Kind.ASSIGN){
                node = new AssignmentStatement(first, ident.getText(), selector,(Expr) val);
            }
            if(op.getKind() == Kind.LARROW){
                node = new ReadStatement(first, ident.getText(), selector,(Expr) val);
            }
        }
        if(current.getKind() == Kind.SEMI){
            consume();
        }
        else{
            throw new SyntaxException("Expected ';'",current.getSourceLocation());
        }

        return node;
    }

    private ASTNode Declaration() throws PLCException{
        ASTNode node = null;
        IToken first = current;
        if(current.getKind() == Kind.IDENT || current.getKind() == Kind.KW_WRITE
                || current.getKind() == Kind.RETURN){
            return Statement();
        }
        try{
            node = nameDef();
            if(current.getKind() == Kind.ASSIGN || current.getKind() == Kind.LARROW){//Assign value
                IToken op = current;
                consume(); //Eat assign.
                ASTNode e = expr();
                if(current.getKind() == Kind.SEMI){
                    node = new VarDeclaration(first, (NameDef) node, op,(Expr) e);
                }
                else{
                    throw new SyntaxException("Expected ';'",current.getSourceLocation());
                }
                consume();//Eat Semi
            }

            else if(current.getKind() == Kind.SEMI){ //End Declaration
                node = new VarDeclaration(first, (NameDef) node,null, null);
                consume();//Eat Semi
            }

        } catch (IllegalArgumentException e) {
            //This is either a statement or an error.
            throw new SyntaxException("Invalid identifier.", current.getSourceLocation());

        }

        return node;
    }

    private ASTNode nameDef() throws PLCException{
        NameDef node = null;

        IToken first = current;
        try{
            validateType();
        }
        catch (IllegalArgumentException e){
            throw new SyntaxException("Invalid Type.", current.getSourceLocation());
        }
        if(validateType() != null){
            String type = current.getText();
            consume(); //Eat the type
            if(current.getKind() == Kind.LSQUARE){ //Same logic as a pixel selector
                consume(); //Consume L Bracket
                ASTNode x = expr(); //Get the value of x - This consumes.
                if(current.getKind() != Kind.COMMA){
                    throw new PLCException("Invalid Pixel Selector");
                }
                consume(); //Consume the comma
                ASTNode y = expr(); //Get expression.
                if(current.getKind() != Kind.RSQUARE){
                    throw new PLCException("Invalid Pixel Selector");
                }
                consume(); //Eat the R Bracket.
                if(current.getKind() == Kind.IDENT){
                    Dimension dim = new Dimension(first,(Expr) x, (Expr) y);
                    node = new NameDefWithDim(first,type, current.getText(), dim);
                }
            }
            else if(current.getKind() == Kind.IDENT){
                node =  new NameDef(first,type, current.getText());
            }
            else{
                throw new SyntaxException("Valid Identifier required", current.getSourceLocation());
            }
        }
        else{
            throw new SyntaxException("Expected a type", current.getSourceLocation());
        }
        consume(); //Eat the current Ident.
        return node;
    }

    private Types.Type validateType(){
        if(current.getKind() == Kind.KW_VOID || current.getKind() == Kind.KW_CONSOLE){
            return null;
        }
        return Types.Type.toType(current.getText());
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

    private ASTNode BinaryCmp() throws PLCException {
        //ASTNode node = null;
        IToken first = current;
        IToken op = lookahead;
        ASTNode right = null;
        //lookahead = new Token(Kind.EOF); // Temporary change to call EXPR
        ASTNode left = BinaryExp();
        while(current.getKind() == Kind.EQUALS || current.getKind() == Kind.LT ||
                current.getKind() == Kind.LE || current.getKind() == Kind.GT ||
                current.getKind() == Kind.GE || current.getKind() == Kind.NOT_EQUALS){
            op = current;
            consume();
            right = BinaryExp();
            left = new BinaryExpr(first,(Expr) left, op,(Expr) right);
        }
        //consume();
        //ASTNode right = expr();

        //node = new BinaryExpr(first,(Expr) left,op,(Expr) right);
        return left;
    }

    private ASTNode Term() throws PLCException{
        //ASTNode node = null;
        IToken first = current;
        IToken op = lookahead;
        ASTNode right = null;
        ASTNode left = unary();
        while(current.getKind() == Kind.TIMES || current.getKind() == Kind.DIV
                || current.getKind() == Kind.MOD){
            op = current;
            consume();
            right = unary();
            left = new BinaryExpr(first, (Expr) left, op, (Expr) right);
        }

        return left;
    }

    private ASTNode BinaryExp() throws PLCException {
        //ASTNode node = null;
        IToken first = current;
        IToken op = lookahead;
        ASTNode right = null;
        //lookahead = new Token(Kind.EOF); // Temporary change to call EXPR
        ASTNode left = Term();
        while(current.getKind() == Kind.PLUS || current.getKind() == Kind.MINUS ||
                current.getKind() == Kind.AND || current.getKind() == Kind.OR){
                    op = current;
                    consume();
                    right = Term();
                    left = new BinaryExpr(first,(Expr) left, op,(Expr) right);
        }
        //consume();
        //ASTNode right = expr();

        //node = new BinaryExpr(first,(Expr) left,op,(Expr) right);
        return left;
    }

    private ASTNode BinaryCmpLeft(ASTNode left) throws  PLCException{
        ASTNode first = left;
        IToken op = current;
        ASTNode right = null;
        if(current.getKind() == Kind.EQUALS || current.getKind() == Kind.LT ||
                current.getKind() == Kind.LE || current.getKind() == Kind.GT ||
                current.getKind() == Kind.GE || current.getKind() == Kind.NOT_EQUALS) {
            while (current.getKind() == Kind.EQUALS || current.getKind() == Kind.LT ||
                    current.getKind() == Kind.LE || current.getKind() == Kind.GT ||
                    current.getKind() == Kind.GE || current.getKind() == Kind.NOT_EQUALS) {
                op = current;
                consume();
                right = BinaryCmp();
                left = new BinaryExpr(op, (Expr) left, op, (Expr) right);
            }
        }
        else{
            left = BinaryExpLeft(left);
        }

        return left;
    }

    private ASTNode BinaryExpLeft(ASTNode left) throws  PLCException{
        ASTNode first = left;
        IToken op = current;
        ASTNode right = null;
        while(current.getKind() == Kind.PLUS || current.getKind() == Kind.MINUS
                || current.getKind() == Kind.AND || current.getKind() == Kind.OR){
            op = current;
            consume();
            right = Term();
            left = new BinaryExpr(op, (Expr) left, op, (Expr) right);
        }
        return left;
    }

    private ASTNode logicalOr() throws PLCException {
        ASTNode node = null;
        if(lookahead.getKind() == Kind.OR){
            node = BinaryCmp();
        }
        else {
            node = logicalAnd();
        }

        return node;
    }

    private ASTNode logicalAnd() throws PLCException {
        ASTNode node = null;
        if(lookahead.getKind() == Kind.AND){
            node = BinaryCmp();
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
                node = BinaryCmp();
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
            node = BinaryCmp();
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
            node = BinaryCmp();
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
            ASTNode Exp = unary();
            node = new UnaryExpr(op,op,(Expr) Exp);

            if(current.getKind() == Kind.PLUS || current.getKind() == Kind.MINUS ||
                    current.getKind() == Kind.TIMES || current.getKind() == Kind.DIV ||
                    current.getKind() == Kind.MOD || current.getKind() == Kind.LT ||
                    current.getKind() == Kind.GT || current.getKind() == Kind.EQUALS ||
                    current.getKind() == Kind.NOT_EQUALS || current.getKind() == Kind.LE ||
                    current.getKind() == Kind.GE){
                node = BinaryCmpLeft(node);
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
            //lookahead = new Token(Kind.EOF); //Temporarily end of file.
            ASTNode ident = primaryExpr(); //Ident - This consumes.
            consume(); //Consume L Bracket
            ASTNode x = expr(); //Get the value of x - This consumes.
            if(current.getKind() != Kind.COMMA){
                throw new PLCException("Invalid Pixel Selector");
            }
            consume();
            ASTNode y = expr();
            if(current.getKind() != Kind.RSQUARE){
                throw new PLCException("Invalid Pixel Selector");
            }

            PixelSelector select = new PixelSelector(first, (Expr) x, (Expr) y);
            node = new UnaryExprPostfix(first, (Expr) ident, select);
            consume();

            if(current.getKind() == Kind.PLUS || current.getKind() == Kind.MINUS ||
                    current.getKind() == Kind.TIMES || current.getKind() == Kind.DIV ||
                    current.getKind() == Kind.MOD || current.getKind() == Kind.LT ||
                    current.getKind() == Kind.GT || current.getKind() == Kind.EQUALS ||
                    current.getKind() == Kind.NOT_EQUALS || current.getKind() == Kind.LE ||
                    current.getKind() == Kind.GE){
                node = BinaryCmpLeft(node);
            }
        }
        else{
            node = primaryExpr();
        }
        return node;
    }

    private ASTNode primaryExpr() throws PLCException{
        ASTNode node = null;
        IToken first = current;
        if(current.getKind() == Kind.LPAREN){
            //Expression
            consume(); //Get next token.
            node = expr();
            if(current.getKind() != Kind.RPAREN){
                throw new SyntaxException("Expected ')'", current.getSourceLocation());
            }
            consume();

            if(current.getKind() == Kind.PLUS || current.getKind() == Kind.MINUS ||
                    current.getKind() == Kind.TIMES || current.getKind() == Kind.DIV ||
                    current.getKind() == Kind.MOD || current.getKind() == Kind.LT ||
                    current.getKind() == Kind.GT || current.getKind() == Kind.EQUALS ||
                    current.getKind() == Kind.NOT_EQUALS || current.getKind() == Kind.LE ||
                    current.getKind() == Kind.GE){
                node = BinaryExpLeft(node);
            }

            return node;
        }
        else if(current.getKind() == Kind.LANGLE){
            consume(); //Consume the angle
            ASTNode red = expr();
            if(current.getKind() != Kind.COMMA){
                throw new SyntaxException("Expected a comma.");
            }
            consume();//Eat the comma.
            ASTNode green = expr();
            if(current.getKind() != Kind.COMMA){
                throw new SyntaxException("Expected a comma.");
            }
            consume();//Eat the next comma
            ASTNode blue = expr();
            if(current.getKind() != Kind.RANGLE){
                throw new SyntaxException("Expected an RANGLE");
            }
            node = new ColorExpr(first, (Expr) red, (Expr) green, (Expr) blue);

        }
        else{
            switch (current.getKind()){
                case INT_LIT -> {node = intLit();}
                case FLOAT_LIT -> {node = floatLit();}
                case BOOLEAN_LIT -> {node = booleanLit();}
                case IDENT -> {node = ident();}
                case STRING_LIT -> {node = stringLit();}
                case COLOR_CONST -> {node = colorConst();}
                case KW_CONSOLE -> {node = consoleExpr();}
                default -> throw new SyntaxException("Unknown Symbol", current.getSourceLocation());
            }
        }
        consume();

        return node;
    }

    private ASTNode consoleExpr() {
        ASTNode node = new ConsoleExpr(current);
        return node;
    }

    private ASTNode colorConst() {
        ASTNode color = new ColorConstExpr(current);
        return color;
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
        node = Prog();
        return node;
    }
}
