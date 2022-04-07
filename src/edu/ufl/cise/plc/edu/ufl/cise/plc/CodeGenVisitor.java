package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.ConsoleIO;

import java.util.List;

public class CodeGenVisitor implements ASTVisitor {

    final String PackageName;
    String program;

    public CodeGenVisitor(String PackageName){
        this.PackageName = PackageName;
        program = "";
    }

    //The caller is responsible for adding the ending semicolon.
    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        String snippet = "";
        if(booleanLitExpr.getValue()){
            snippet = "true";
            //program += "true";
        }
        else{
            //program += "false";
            snippet = "false";
        }
        //Always return the string that we add for debug purposes.
        return snippet;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        //program += stringLitExpr.getValue();
        return "\""+stringLitExpr.getValue()+"\"";
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        String cast = "";
        String snippet = "";
        //Check for cast
        if(intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT){
            cast = "(" + intLitExpr.getCoerceTo().toString() + ") ";
        }
        snippet = cast + intLitExpr.getValue();
        //program += snippet;
        return snippet;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        String cast = "";
        String snippet = "";
        //Check for cast
        if(floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT){
            cast = "(" + floatLitExpr.getCoerceTo().toString() + ") ";
        }
        snippet = cast + floatLitExpr.getValue() + "f";
        //program += snippet;
        return snippet;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not yet Implemented");
        //return null;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        // (Integer) ConsoleIO.readValueFromConsole(“INT”, “Enter integer:”);
        String cast = "";
        String snippet = "";
        String consoleTypeText = "";
        Types.Type coerce = consoleExpr.getCoerceTo();
        switch (coerce){
            case INT ->{
                cast = "(Integer) ";
                consoleTypeText = "integer";
            }
            case FLOAT -> {
                cast = "(Float) ";
                consoleTypeText = "float";
            }
            case STRING -> {
                cast = "(String) ";
                consoleTypeText = "string";
            }
            case BOOLEAN -> {
                cast = "(Boolean) ";
                consoleTypeText = "boolean";
            }
        }
        snippet = cast + "ConsoleIO.readValueFromConsole(" + "\""+ coerce.toString()+ "\"" + ", \"Enter " + consoleTypeText + ":\")";
        //program += snippet;

        return snippet;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not yet Implemented");
        //return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        String op = unaryExpression.getOp().getText();
        String snippet = (String) unaryExpression.getExpr().visit(this, null);
        snippet = "(" + op + " " + snippet + ")";
        return snippet;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        String op = binaryExpr.getOp().getText();
        String left = (String) binaryExpr.getLeft().visit(this, null);
        String right = (String) binaryExpr.getRight().visit(this, null);
        String snippet = "(" + left + op + right +")";
        return snippet;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        String cast = "";
        String snippet = "";
        //Check for cast
        if(identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()){
            cast = "(" + identExpr.getCoerceTo().toString() + ") ";
        }
        snippet = cast + identExpr.getText();
        //program += snippet;
        return snippet;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        String snippet = "";
        String condition = (String) conditionalExpr.getCondition().visit(this, null);
        String trueCase = (String) conditionalExpr.getTrueCase().visit(this, null);
        String falseCase = (String) conditionalExpr.getFalseCase().visit(this, null);
        snippet = "(" + condition + ") ? \n\t(" + trueCase + ") :\n\t" + "(" + falseCase + ")";

        return snippet;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not yet Implemented");
        //return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not yet Implemented");
        //return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        //TODO:
        //Because of type checking I assume there should be no way to have an incompatible type
        //I will chance this if necessary based on testing.
        String snippet = assignmentStatement.getName() + "= ";
        String expr = (String) assignmentStatement.getExpr().visit(this, null);
        snippet += expr + ";\n";

        return snippet;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        //ConsoleIO.console.println((String) writeStatement.getSource().visit(this, null));
        String source = (String) writeStatement.getSource().visit(this, null);
        String snippet = "ConsoleIO.console.println("+ source +");";
        return snippet;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        String snippet = readStatement.getName() + " = ";
        String expr = (String) readStatement.getSource().visit(this, null);
        snippet += expr + ";\n";
        return snippet;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        String pack = "package " + PackageName + ";\n\n";
        String imports = "import edu.ufl.cise.plc.runtime.*;\n";
        imports += "import edu.ufl.cise.plc.ast.*;\n\n";
        String snippet = pack + imports;
        snippet += "public class " + program.getName() + "{\n";
        String returnType = program.getReturnType().toString().toLowerCase();
        if(returnType.equals("string")){
            returnType = "String";
        }
        snippet += "\tpublic static " + returnType + " apply(";
        List<NameDef> params = program.getParams();
        String plist = "";
        for(int i = 0; i < params.size(); i++){
            plist += (String) params.get(i).visit(this, null);
            if(i != params.size() - 1){
                plist += ", ";
            }
        }
        snippet += plist + "){\n";
        //Note: This does not add a semicolon.
        for(ASTNode dec : program.getDecsAndStatements()){
            snippet += "\t\t"+(String) dec.visit(this, null) + "\n";
        }

        snippet += "\t}\n}\n";
        return snippet;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        String type = nameDef.getType().toString().toLowerCase();
        if(type.equals("string")){
            type = "String";
        }
        String name = nameDef.getName();
        String snippet = type + " " + name;
        return snippet;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not yet Implemented");
        //return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        String snippet = "return ";
        String expr = (String) returnStatement.getExpr().visit(this,null);
        snippet += expr+";";
        return snippet;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        String snippet = (String) declaration.getNameDef().visit(this, null);
        //IToken.Kind op = declaration.getOp().getKind();
        if(declaration.getOp() != null){
            snippet += " = " + (String) declaration.getExpr().visit(this, null);
        }
        snippet+= ";";
        return snippet;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        throw new UnsupportedOperationException("Not yet Implemented");
        //return null;
    }
}
