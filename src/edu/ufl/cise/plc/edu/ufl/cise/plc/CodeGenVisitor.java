package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import java.awt.Color;

import edu.ufl.cise.plc.runtime.ColorTuple;
import edu.ufl.cise.plc.runtime.ConsoleIO;
import edu.ufl.cise.plc.runtime.FileURLIO;
import edu.ufl.cise.plc.runtime.ImageOps;

import java.awt.image.BufferedImage;
import java.util.List;

public class CodeGenVisitor implements ASTVisitor {

    final String PackageName;
    String program;
    boolean run;
    boolean image;
    boolean color;

    public CodeGenVisitor(String PackageName){
        this.PackageName = PackageName;
        program = "";
        run = true;
        image = true;
        color = true;
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
        String str = "\"\"\"\n"+ stringLitExpr.getValue() +"\"\"\"";
        //"""
        //STRINGLIT
        //"""
        //"\"" + stringLitExpr.getValue() + "\""
        return str;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        String cast = "";
        String snippet = "";
        //Check for cast
        if(intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT){
            cast = "(" + intLitExpr.getCoerceTo().toString().toLowerCase() + ") ";
            if(cast.equals("(color) ")){
                cast = "";
            }
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
            cast = "(" + floatLitExpr.getCoerceTo().toString().toLowerCase() + ") ";
            if(cast.equals("(color) ")){
                cast = "";
            }
        }
        snippet = cast + floatLitExpr.getValue() + "f";
        //program += snippet;
        return snippet;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        //'BLACK','BLUE','CYAN','DARK_GRAY','GRAY','GREEN','LIGHT_GRAY','MAGENTA','ORANGE','PINK',
        //'RED','WHITE','YELLOW'
        color = true;
        String snippet = "";
        //This will get the name of a valid color const.
        String color = colorConstExpr.getText();
        //Looking at the documentation, the below line returns a ColorTuple itself.
        snippet += "(ColorTuple.unpack(Color."+ color+".getRGB()))";
        //ColorTuple.unpack(Color.BLACK.getRGB());
        //ImageOps.setColor();
        // throw new UnsupportedOperationException("Not yet Implemented");
        return snippet;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        // (Integer) ConsoleIO.readValueFromConsole(“INT”, “Enter integer:”);
        run = true;
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
        String snippet = "";
        color = true;
        String red = (String) colorExpr.getRed().visit(this,null);
        String green = (String) colorExpr.getGreen().visit(this,null);
        String blue = (String) colorExpr.getBlue().visit(this,null);
        snippet += "new ColorTuple("+red+", "+green+", "+blue+")";
        //throw new UnsupportedOperationException("Not yet Implemented");
        return snippet;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        String op = unaryExpression.getOp().getText();
        String snippet = (String) unaryExpression.getExpr().visit(this, null);
        snippet = "(" + op + snippet + ")";
        return snippet;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        String op = binaryExpr.getOp().getText();
        IToken.Kind oper = binaryExpr.getOp().getKind();

        /*
        * PLUS, MINUS, TIMES, DIV, MOD
        *
        * */
        String IOPS = null;
        switch(oper){
            case PLUS ->{
                IOPS = "ImageOps.OP.PLUS";
            }
            case MINUS -> {
                IOPS = "ImageOps.OP.MINUS";
            }
            case DIV -> {
                IOPS = "ImageOps.OP.DIV";
            }
            case TIMES -> {
                IOPS = "ImageOps.OP.TIMES";
            }
            case MOD -> {
                IOPS = "ImageOps.OP.MOD";
            }
        }
        String left = (String) binaryExpr.getLeft().visit(this, null);
        String right = (String) binaryExpr.getRight().visit(this, null);
        String snippet = "";
        Types.Type tLeft = binaryExpr.getLeft().getType();
        Types.Type tRight = binaryExpr.getRight().getType();

        if(binaryExpr.getLeft() instanceof UnaryExprPostfix
                && binaryExpr.getRight() instanceof  UnaryExprPostfix){

        }

        if(tLeft == Types.Type.STRING && tRight == Types.Type.STRING){
            if(op.equals("==")){
                op = ".equals(";
            }
            if(op.equals("!=")){
                op = ".equals(";
                snippet ="(!" + left + op + right +"))";
                return snippet;
            }
        }
        if(tLeft == Types.Type.IMAGE && tRight == Types.Type.IMAGE){
            snippet += "(ImageOps.binaryImageImageOp("+ IOPS+","+left+","+right+"))";
            //ImageOps.binaryImageScalarOp(op, left, right);
            return snippet;
        }
        if(tLeft == Types.Type.IMAGE && tRight == Types.Type.INT){
            snippet += "(ImageOps.binaryImageScalarOp("+ IOPS+","+left+","+right+"))";
            //ImageOps.binaryImageScalarOp(op, left, right);
            return snippet;
        }
        if((binaryExpr.getLeft().getCoerceTo() == Types.Type.COLOR &&
                binaryExpr.getRight().getCoerceTo() == Types.Type.COLOR) ||
                (binaryExpr.getLeft().getCoerceTo() == Types.Type.COLORFLOAT &&
                binaryExpr.getRight().getCoerceTo() == Types.Type.COLORFLOAT) ||
                (tLeft == Types.Type.COLOR && tRight == Types.Type.COLOR) ||
                (tLeft == Types.Type.COLORFLOAT && tRight == Types.Type.COLORFLOAT)){
            snippet += "(ImageOps.binaryTupleOp("+ IOPS+","+left+","+right+"))";
            //ImageOps.binaryImageScalarOp(op, left, right);
            return snippet;
        }
        if((tLeft == Types.Type.COLOR && tRight == Types.Type.INT)||
                tLeft == Types.Type.COLOR && tRight == Types.Type.FLOAT){
            snippet += "(ImageOps.binaryTupleOp("+ IOPS+","+left+", new ColorTuple("+right+")))";
            //ImageOps.binaryImageScalarOp(op, left, right);
            return snippet;
        }
        snippet ="(" + left + op + right +")";
        if(op.equals(".equals(")){
            snippet += ")";
        }
        return snippet;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        String cast = "";
        String snippet = "";
        //Check for cast
        if(identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()){
            cast = "(" + identExpr.getCoerceTo().toString().toLowerCase() + ") ";
            if(cast.equals("(color) ") || cast.equals("(image) ")){
                cast = "";
            }
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
        snippet = "((" + condition + ") ? (" + trueCase + ") :" + " (" + falseCase + "))";

        return snippet;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        String snippet = "";
        //Literally only returns "WIDTH, HEIGHT"
        String width = (String) dimension.getWidth().visit(this, null);
        String height = (String) dimension.getHeight().visit(this, null);
        snippet += width+", "+height;
        //throw new UnsupportedOperationException("Not yet Implemented");
        return snippet;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        String snippet = "";
        String x = (String) pixelSelector.getX().visit(this, null);
        String y = (String) pixelSelector.getY().visit(this, null);
        snippet += x+", "+y;

        //throw new UnsupportedOperationException("Not yet Implemented");
        return snippet;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        //TODO:
        //Because of type checking I assume there should be no way to have an incompatible type
        //I will chance this if necessary based on testing.

        String snippet = "";

        String resultType = assignmentStatement.getTargetDec().getType().toString().toLowerCase();
        if(resultType.equals("string")){
            resultType = "String";
        }
        if(resultType.equals("image") && assignmentStatement.getExpr().getType() == Types.Type.IMAGE){
            run = true;
            image = true;
            if(assignmentStatement.getTargetDec().getDim() != null){
                //We have a dimension.
                Dimension sel = assignmentStatement.getTargetDec().getDim();
                String x = (String) sel.getWidth().visit(this, null);
                String y = (String) sel.getHeight().visit(this, null);
                snippet = assignmentStatement.getName()+
                        " = ImageOps.resize("+assignmentStatement.getExpr().visit(this, null)+","
                        + x + "," + y + ");";
                return snippet;
                //ImageOps.resize(assignmentStatement.getExpr().visit(this, null),x,y);
            }
            else{ //We don't have a dimension.
                snippet = assignmentStatement.getName() + " = ImageOps.clone(" +
                        assignmentStatement.getExpr().visit(this, null) +
                        ");";
                //ImageOps.clone( assignmentStatement.getExpr().visit(this, null));

                return snippet;
            }
        }
        if(resultType.equals("image") && (assignmentStatement.getExpr().getType() == Types.Type.INT)){
            run = true;
            String literal = (String) assignmentStatement.getExpr().visit(this, null);
            String name = assignmentStatement.getName();
            snippet += "for(int i = 0; i < " + name+".getWidth(); i++){\n\t\t\t";
            snippet += "for(int j = 0; j < " + name+".getHeight(); j++){\n\t\t\t\t";
            snippet += "ImageOps.setColor("+ name+",i,j,"+ "new ColorTuple("+assignmentStatement.getExpr().visit(this,null)+"));\n";
            snippet += "\t\t\t}\n\t\t}";
            return snippet;
        }
        if(resultType.equals("image") && assignmentStatement.getExpr().getType() == Types.Type.COLOR){
            run = true;
            String name = assignmentStatement.getName();
            if(assignmentStatement.getSelector() == null){
                snippet += "for(int x = 0; x < " + name+".getWidth(); x++){\n\t\t\t";
                snippet += "for(int y = 0; y < " + name+".getHeight(); y++){\n\t\t\t\t";
                snippet += "ImageOps.setColor("+ name+" , x, y, "+ assignmentStatement.getExpr().visit(this,null)+");\n";
                snippet += "\t\t\t}\n\t\t}";
                //ImageOps.setColor();
                return snippet;
            }

            PixelSelector sel = assignmentStatement.getSelector();
            String x = sel.getX().getText();
            //String xVal = (String)sel.getX().visit(this, null);
            String y = sel.getY().getText();

            snippet += "for(int " + x +" = 0; "+ x + " < " + name+".getWidth(); "+ x +"++){\n\t\t\t";
            snippet += "for(int " + y +" = 0; "+ y + " < " + name+".getHeight(); "+ y +"++){\n\t\t\t\t";
            snippet += "ImageOps.setColor("+ name+","+x+","+ y +","+ assignmentStatement.getExpr().visit(this,null)+");\n";
            snippet += "\t\t\t}\n\t\t}";
            return snippet;

        }
        if(resultType.equals("image")){
            run = true;
            resultType = "BufferedImage";
        }
        snippet += assignmentStatement.getName() + "= ";
        String expr = (String) assignmentStatement.getExpr().visit(this, null);
        snippet += "(" + resultType +") " + expr + ";\n";

        return snippet;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        //ConsoleIO.console.println((String) writeStatement.getSource().visit(this, null));
        run = true;
        String snippet = "";
        Types.Type src = writeStatement.getSource().getType();
        Types.Type dest = writeStatement.getDest().getType();

        if(src == Types.Type.IMAGE && dest == Types.Type.CONSOLE){
            snippet = "ConsoleIO.displayImageOnScreen("+ writeStatement.getSource().visit(this, null) +");";
        }
        //Check for a string target.
        else if(src == Types.Type.IMAGE && dest == Types.Type.STRING){
            snippet = "FileURLIO.writeImage("+ writeStatement.getSource().visit(this, null)
                    +","+ writeStatement.getSource().visit(this, null) + ");";
        }
        //Didn't find a string target - must use writeValue.
        else if(src == Types.Type.IMAGE){
            snippet = "FileURLIO.writeValue("+ writeStatement.getSource().visit(this, null)
                    +","+ writeStatement.getSource().visit(this, null) + ");";
        }
        else { //Print to the console normally.
            String source = (String) writeStatement.getSource().visit(this, null);
            snippet = "ConsoleIO.console.println(" + source + ");";

        }
        return snippet;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        run = true;
        if(readStatement.getTargetDec().getType() == Types.Type.IMAGE){
            String snippet = "";
            Dimension dim = readStatement.getTargetDec().getDim();
            String URL = (String) readStatement.getSource().visit(this, null);
            if(dim != null) {
                snippet += "(FileURLIO.readImage(" + URL + ", " + dim.visit(this, null) + "))";
            }
            else{
                snippet +="(FileURLIO.readImage("+URL+"))";
            }
            return snippet;
        }
        String snippet = readStatement.getName() + " = ";
        String expr = (String) readStatement.getSource().visit(this, null);
        snippet += expr + ";\n";
        return snippet;
    }

    private String processImports(){
        String snippet = "";
        if(color){
            snippet += "import java.awt.Color;\n";
        }
        if(run){
            snippet += "import edu.ufl.cise.plc.runtime.*;\n";
        }
        if(image){
            snippet += "import java.awt.image.BufferedImage;\n\n";
        }
        snippet+= "\n\n";
        return snippet;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        String pack = "package " + PackageName + ";\n\n";
        //String imports = "import edu.ufl.cise.plc.runtime.*;\n";
        //imports += "import edu.ufl.cise.plc.ast.*;\n";
        //imports += "import java.awt.Color;\n";
        //imports += "import java.awt.image.BufferedImage;\n\n";
        //String snippet = pack + imports;
        String snippet = "public class " + program.getName() + "{\n";
        String returnType = program.getReturnType().toString().toLowerCase();
        if(returnType.equals("string")){
            returnType = "String";
        }
        if(returnType.equals("image")){
            returnType = "BufferedImage";
        }
        if(returnType.equals("color")){
            returnType = "ColorTuple";
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
        //Process imports dynamically.
        snippet = pack + processImports() + snippet;
        return snippet;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        String type = nameDef.getType().toString().toLowerCase();
        if(type.equals("string")){
            type = "String";
        }
        if(type.equals("image")){
            type = "BufferedImage";
        }
        if(type.equals("color")){
            type = "ColorTuple";
        }
        String name = nameDef.getName();
        String snippet = type + " " + name;
        return snippet;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        String snippet = "";
        image = true;
        String dim = (String) nameDefWithDim.getDim().visit(this, null);
        snippet += "BufferedImage "+ nameDefWithDim.getName();
        //throw new UnsupportedOperationException("Not yet Implemented");
        return snippet;
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
        Dimension dim = declaration.getNameDef().getDim();
        //IToken.Kind op = declaration.getOp().getKind();
        String resultType = declaration.getType().toString().toLowerCase();

        if(resultType.equals("color")){
            snippet += "= "+declaration.getExpr().visit(this, null)+";";
            return snippet;
        }

        if(resultType.equals("image") && declaration.getOp() == null){
            image = true;
            snippet+= " = ";
            //This image must have a dimension by default.
            snippet += "new BufferedImage("+ dim.visit(this, null) + ", BufferedImage.TYPE_INT_RGB)";
        }
        if(declaration.getOp() != null){
            snippet += " = ";
            if(resultType.equals("string")){
                resultType = "String";
            }
            if(resultType.equals("image")){
                //image = true;
                resultType = "BufferedImage";
            }
            if(declaration.getType() != declaration.getExpr().getType()){
                snippet += "(" + resultType+")";
            }

            if(declaration.getOp().getText().equals("<-")){
                run = true;
                String URL = (String) declaration.getExpr().visit(this, null);
                if(dim != null) {
                    snippet += "(FileURLIO.readImage(" + URL + ", " + dim.visit(this, null) + "))";
                }
                else{
                    snippet +="(FileURLIO.readImage("+URL+"))";
                }
            }
            else if(declaration.getOp().getText().equals("=") && resultType.equals("BufferedImage")){
                image = true;
                if(dim == null){
                    snippet += "ImageOps.clone(" +
                            declaration.getExpr().visit(this, null)
                            + ");";
                }
                else{
                    //snippet = "";
                    Dimension sel = declaration.getDim();
                    String x = (String) sel.getWidth().visit(this, null);
                    String y = (String) sel.getHeight().visit(this, null);
                    snippet += declaration.getName()+
                            " = ImageOps.resize("+declaration.getExpr().visit(this, null)+","
                            + x + "," + y + ");";
                }
                return snippet;
            }
            else{
                snippet += "(" + (String) declaration.getExpr().visit(this, null) + ")";
            }

        }
        snippet+= ";";
        return snippet;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        String snippet = "";

        //Ident
        String ident = (String) unaryExprPostfix.getExpr().visit(this, null);
        //Visit Selector to get param string.
        String sel = (String) unaryExprPostfix.getSelector().visit(this, null);
        //Create a ColorTuple with the Pixel Selector as params.
        snippet += "(ColorTuple.unpack("+ ident+".getRGB("+ sel + ")))";

        //throw new UnsupportedOperationException("Not yet Implemented (Unary)");
        return snippet;
    }
}
