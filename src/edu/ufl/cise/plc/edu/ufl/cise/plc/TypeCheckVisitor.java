package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;


import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	String prog;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.
	
	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(Type.COLOR);
		return Type.COLOR;
		//throw new UnsupportedOperationException("Unimplemented visit method.");
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);
	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}


	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
		Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
		Type resultType = null;
		switch(op){
			//Thus begins the long and boring task of filling in a chart.
			case PLUS, MINUS ->{
				if(leftType == INT && rightType == INT) resultType = INT;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if(leftType == INT && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == FLOAT && rightType == INT){
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if(leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if(leftType == COLOR && rightType == COLORFLOAT){
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLORFLOAT && rightType == COLOR){
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
				else{
					check(false, binaryExpr, "Compiler Error! (PLUS/MINUS)");
				}
			}
			case TIMES, DIV, MOD ->{
				if(leftType == INT && rightType == INT) resultType = INT;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if(leftType == INT && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == FLOAT && rightType == INT){
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if(leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if(leftType == COLOR && rightType == COLORFLOAT){
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLORFLOAT && rightType == COLOR){
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
				else if(leftType == IMAGE && rightType == INT) resultType = IMAGE;
				else if(leftType == IMAGE && rightType == FLOAT) resultType = IMAGE;
				else if(leftType == INT && rightType == COLOR){
					binaryExpr.getLeft().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if(leftType == COLOR && rightType == INT){
					binaryExpr.getRight().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if(leftType == FLOAT && rightType == COLOR){
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLOR && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else{
					check(false, binaryExpr, "Compiler Error! (TIMES/DIV/MOD)");
				}
			}
			case LT, LE, GT, GE ->{
				if(leftType == INT && rightType == INT) resultType = BOOLEAN;
				if(leftType == FLOAT && rightType == FLOAT) resultType = BOOLEAN;
				if(leftType == INT && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = BOOLEAN;
				}
				if(leftType == FLOAT && rightType == INT){
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = BOOLEAN;
				}
			}
			case AND, OR -> {
				if(leftType == BOOLEAN && rightType == BOOLEAN) resultType = BOOLEAN;
				else{
					check(false, binaryExpr, "Incorrect Types for AND/OR statement!");
				}
			}
			case EQUALS, NOT_EQUALS -> {
				if(leftType == rightType){
					resultType = BOOLEAN;
				}
				else{
					check(false, binaryExpr, "Incompatible types for EQUALS/NOT_EQUALS");
				}
			}
			default -> {
				check(false, binaryExpr, "Compiler error (Default case)");
			}
		}

		binaryExpr.setType(resultType);
		return resultType;
		//throw new UnsupportedOperationException("Unimplemented visit method.");
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		//Look up the variable.
		String name = identExpr.getText();
		Declaration d = symbolTable.lookup(identExpr.getFirstToken().getText());
		//Set the declaration.
		identExpr.setDec(d);
		//Check that it exists
		check(d != null, identExpr, "Undefined Identifier: " + name);
		//Check for initialization
		check(d.isInitialized(), identExpr, "using uninitialized variable.");
		//Set type and coercion is necessary
		identExpr.setType(d.getType());
		if(arg != null){
			identExpr.setCoerceTo((Type) arg);
		}
		//Return type by convention
		return identExpr.getType();
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		//Get the type of the conditional statement.
		Type con = (Type) conditionalExpr.getCondition().visit(this, arg);
		//Check for boolean type.
		if(con == BOOLEAN){
			//Get the true and false cases.
			Type trueCase = (Type) conditionalExpr.getTrueCase().visit(this, arg);
			Type falseCase = (Type) conditionalExpr.getFalseCase().visit(this, arg);
			//If the two are not the same type, then throw.
			if(trueCase == falseCase){
				//Set the type of the expression
				conditionalExpr.setType(trueCase);
				return trueCase;
			}
			else{
				throw new TypeCheckException("True and False cases have different types!");
			}

		}
		else{ // Condition is not a boolean
			throw new TypeCheckException("Condition not valid!");
		}

		//throw new UnsupportedOperationException("Conditional");
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		//Visit width and height to get types.
		Type width = (Type) dimension.getWidth().visit(this, null);
		Type height = (Type) dimension.getHeight().visit(this, null);
		//Declaration width = symbolTable.lookup(d.getWidth().getText());
		//Declaration height = symbolTable.lookup(d.getHeight().getText());
		//Makes sure that both at ints.
		if(width != INT || height != INT){
			check(false, dimension, "Dimension not both ints!");
		}
		//throw new UnsupportedOperationException("Visit Dimension");
		return null;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		//I modified the assignment statement.
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		//Grab declaration
		Declaration dec = symbolTable.lookup(assignmentStatement.getName());
		//Get the expression
		Expr assign = assignmentStatement.getExpr();
		//Set the target declaration
		assignmentStatement.setTargetDec(dec);
		//Make sure that there is a Declaration - otherwise the variable is not defined.
		if(dec == null){
			throw new TypeCheckException("Undefined variable.");
		}
		//Set the return type.
		Type ret = dec.getType();
		//Set to declared.
		dec.setInitialized(true);
		//Make sure that only images have a pixel selector.
		if(assignmentStatement.getSelector() != null && ret != IMAGE){
			throw new TypeCheckException("PixelSelector must be used on images!");
		}
		switch(ret){
			case IMAGE -> {
				//Without pixel selector
				if(assignmentStatement.getSelector() == null){
					//Get type of expression
					Type rhs = (Type) assign.visit(this, null);

					if(rhs != ret){
						//Set coercion or throw
						if(rhs == INT){
							assign.setCoerceTo(COLOR);
						}
						else if(rhs == FLOAT || rhs == COLORFLOAT){
							assign.setCoerceTo(COLORFLOAT);
						}
						//This will trigger for anything that is not a COLOR,
						// but was not already filtered out.
						else if(rhs != COLOR){
							throw new TypeCheckException("Not compatible types. (No PixelSelector)");
						}

					}

				}
				//With pixel selector
				else{
					//Get selector
					PixelSelector pixelSelector = assignmentStatement.getSelector();

					//Check that locals are not already used.
					Declaration x = symbolTable.lookup(pixelSelector.getX().getText());
					Declaration y = symbolTable.lookup(pixelSelector.getY().getText());
					check(x == null && y == null, pixelSelector, "Cannot use global variables in pixel selector!");

					//Make sure that both of the locals are idents.
					boolean px = pixelSelector.getX().getClass() == IdentExpr.class;
					boolean py = pixelSelector.getY().getClass() == IdentExpr.class;

					if(!px || !py){
						throw new TypeCheckException("Pixel selector must use Idents!");
					}

					//Insert locals to table
					NameDef localX = new NameDef(pixelSelector.getX().getFirstToken(), "int", pixelSelector.getX().getText());
					NameDef localY = new NameDef(pixelSelector.getY().getFirstToken(), "int", pixelSelector.getY().getText());
					symbolTable.insert(pixelSelector.getX().getText(), new VarDeclaration(pixelSelector.getX().getFirstToken(),localX,null, null));
					symbolTable.insert(pixelSelector.getY().getText(), new VarDeclaration(pixelSelector.getY().getFirstToken(),localY,null, null));

					//And set the to be initialized or they will fail elsewhere.
					symbolTable.lookup(pixelSelector.getX().getText()).setInitialized(true);
					symbolTable.lookup(pixelSelector.getY().getText()).setInitialized(true);

					//Visit to check types
					pixelSelector.visit(this, null);

					//Check expression.
					Type rhs = (Type) assign.visit(this, null);

					//Set coercion or throw
					if(rhs == FLOAT || rhs == COLOR || rhs == COLORFLOAT || rhs == INT){
						assign.setCoerceTo(COLOR);
					}
					else{
						throw new TypeCheckException("Not type compatible (PixelSelector)");
					}

					//Remove locals
					symbolTable.remove(pixelSelector.getX().getText());
					symbolTable.remove(pixelSelector.getY().getText());
					//throw new TypeCheckException("Placeholder for PixelSelector!");
				}
			}
			default -> {//Not an Image
				//Get type
				Type expr = (Type) assign.visit(this, null);

				//Check for compatible types and coercion
				if(ret != expr){
					if(ret == INT && expr == FLOAT){
						assign.setCoerceTo(INT);
					}
					else if(ret == FLOAT && expr == INT) {
						assign.setCoerceTo(FLOAT);
					}
					else if(ret == INT && expr == COLOR){
						assign.setCoerceTo(INT);
					}
					else if(ret == COLOR && expr == INT){
						assign.setCoerceTo(COLOR);
					}
					else{
						check(false, assignmentStatement, "Not compatible types.");
					}
				}
			}

		}
		//Return type by convention.
		return ret;
		//throw new UnsupportedOperationException("Unimplemented visit method. (Assignment)");
	}


	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		//TODO:  implement this method
		Declaration dec = symbolTable.lookup(readStatement.getName());
		Type ret = dec.getType();
		readStatement.setTargetDec(dec);
		if(readStatement.getSelector() != null){
			throw new TypeCheckException("Invalid PixelSelector! (Read Statement)");
		}
		Type rhs = (Type) readStatement.getSource().visit(this, null);
		if(rhs != STRING && rhs != CONSOLE){
			throw new TypeCheckException("Input must be console or string!");
		}
		if(rhs == CONSOLE){
			readStatement.getSource().setCoerceTo(ret);
		}
		dec.setInitialized(true);

		return ret;
		//throw new UnsupportedOperationException("Unimplemented visit method. (READ)");
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		Types.Type def = null; ///Default return type.

		if(declaration.getOp() == null){
			declaration.setInitialized(false);
			def = (Type) declaration.getNameDef().visit(this, null);
		}
		else{ //Op is either assign or read.
			//declaration.setInitialized(true);
			switch (declaration.getOp().getKind()){
				case LARROW -> {
					Type expect = (Type) declaration.getNameDef().visit(this, true);
					Type rhs = (Type) declaration.getExpr().visit(this, null);
					if(rhs != CONSOLE && rhs != STRING){
						throw new TypeCheckException("Reads must be console or string. (Assignment Statement)");
					}
					if(rhs == CONSOLE){
						declaration.getExpr().setCoerceTo(expect);
					}

				}
				case ASSIGN -> {
					//Check that expr is the right type.
					//Add name to table/visit node
					Types.Type expect = (Type) declaration.getNameDef().visit(this, true);
					Types.Type actual = (Type) declaration.getExpr().visit(this, null);

					//Best case they are correct types.
					if(expect == actual){
						//declaration.setInitialized(true);
						def = expect;
					}
					else if(expect == INT){
						if(actual == FLOAT || actual == COLOR){
							//declaration.getExpr().visit(this, INT);
							declaration.getExpr().setCoerceTo(INT);
						}
					}
					else if(expect == FLOAT){
						if(actual == INT || actual == COLOR){
							//declaration.getExpr().visit(this, FLOAT);
							declaration.getExpr().setCoerceTo(FLOAT);
						}
					}
					else if(expect == COLOR){
						if(actual == INT || actual == FLOAT){
							//declaration.getExpr().visit(this, COLOR);
							declaration.getExpr().setCoerceTo(COLOR);
						}
					}
					else if(expect == STRING){
						//declaration.getExpr().visit(this, STRING);
						declaration.getExpr().setCoerceTo(STRING);
					}
					else{
						throw new TypeCheckException("Type mismatch");
					}
				}
			}
		}
		return def;
		//throw new UnsupportedOperationException("Unimplemented visit method.");
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {		
		//TODO:  this method is incomplete, finish it.

		//We want to insert the name of the program into the symbol table.
		prog = program.getName();

		List<NameDef> args = program.getParams();
		for (NameDef a : args) {
			//This should be visitNameDef
			a.visit(this, true);
			//symbolTable.add(a.getName(),a.getType(),a.isInitialized());
		}
		
		//Save root of AST so return type can be accessed in return statements
		root = program;
		
		//Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		//if(decsAndStatements.isEmpty()){
			//throw new TypeCheckException("Empty program.");
		//}
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		Type ret = null;
		if(arg != null){
			nameDef.setInitialized(true);
		}
		//Insert nameDef to table.
		if(nameDef.getName().equals(prog)){
			throw new TypeCheckException("Cannot reuse program name.");
		}
		if(symbolTable.insert(nameDef.getName(), (Declaration) nameDef)){
			ret = nameDef.getType();
		}
		else{
			throw new TypeCheckException("Variable name already in use!");
		}
		if(nameDef.getType() == IMAGE && !nameDef.isInitialized()){
			throw new TypeCheckException("Images should always have a dimension!");
		}
		return ret;
		//throw new TypeCheckException("Variable already declared.");
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		Dimension d = nameDefWithDim.getDim();
		d.visit(this, arg);
		Type ret = null;
		if(arg != null){
			nameDefWithDim.setInitialized(true);
		}
		//Insert nameDef to table.
		if(nameDefWithDim.getName().equals(prog)){
			throw new TypeCheckException("Cannot reuse program name.");
		}
		if(symbolTable.insert(nameDefWithDim.getName(), (Declaration) nameDefWithDim)){
			ret = nameDefWithDim.getType();
		}
		else{
			throw new TypeCheckException("Variable name already in use!");
		}
		return ret;
	}
 
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		Declaration d = symbolTable.lookup(returnStatement.getExpr().getText());
		if(d != null) {
			if(!d.isInitialized()){
				throw new TypeCheckException("Returning uninitialized variable!");
			}
		}
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
