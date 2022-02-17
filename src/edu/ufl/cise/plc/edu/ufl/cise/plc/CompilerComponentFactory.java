package edu.ufl.cise.plc;

//This class eliminates hard coded dependencies on the actual Lexer class.  You can call your lexer whatever you
//want as long as it implements the ILexer interface and you have provided an appropriate body for the getLexer method.

import edu.ufl.cise.plc.Lexer;
import edu.ufl.cise.plc.ILexer;

public class CompilerComponentFactory {
	
	//This method will be invoked to get an instance of your lexer.  
	public static ILexer getLexer(String input) {
		//TODO:  modify this method so it returns an instance of your Lexer instead of throwing the exception.
		//for example:  
			return new Lexer(input);
		
	}

	public static IParser getParser(String input) throws PLCException {
		//TODO:  modify this method so it returns an instance of your Lexer instead of throwing the exception.
		//for example:
		Lexer lex = new Lexer(input);
		return new Parser(lex.getTokenList());

	}
	
}
