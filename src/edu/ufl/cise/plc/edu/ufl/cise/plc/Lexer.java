package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.List;
import edu.ufl.cise.plc.IToken.Kind;

public class Lexer implements ILexer {
	
	List<Token> tokenList;
	State state; //State machine for the lexer.
	int index; //Keeps track of where lexer is in list.
	int line;
	int col;
	int curLine;
	int curCol;
	int i;
	String fullText;
	int fullLen;
	String raw;
	boolean escape;
	//boolean ident;
	
	//This string is the raw input of the program.
	public Lexer(String input) {
		tokenList = new ArrayList<Token>();
		state = State.START;
		escape = false;
		line = 0;
		col = 0;
		curLine = 0;
		curCol = 0;
		//boolean ident = false;
		fullText = input;
		fullLen = input.length();
		//Run the length of the string.
		lex(fullText);
	}
	
	private void lex(String input) {
		if(input != "") {
			for(i = 0; i < fullLen; i++) {
				Tokenize(fullText.charAt(i));
			}
			//Generate Last Token
		}
		EOF();
	}
	
	private void createToken(Kind kind, int line, int col) {
		tokenList.add(new Token(kind, raw, line, col));
	}
	
	
	private void Tokenize(char input) { //Automatically at start state if here
		raw = "";
		if(input == '"') {
			StringLit();
		}
		else if(input == '#') {
			Comment();
		}
		else if(input == ' ' || input == '\n' || input == '\t' || input == '\r') {
			Whitespace(input);
		}
		//A-Z & a-z & $ & _
		else if((input >= 65 && input <= 90) || (input >= 97 && input <= 122) ||
				input == '_' || input == '$') {
			Ident();
		}
		//1-9
		else if(input >=49 && input <= 57) {
			NumLit();
		}
		//Zero
		else if(input == 48) {
			Zero();
		}
		else if(input == '*'){
			tokenList.add(new Token(Kind.TIMES));
		}
		else if(input == '+'){
			tokenList.add(new Token(Kind.PLUS));
		}
		else if(input == '/'){
			tokenList.add(new Token(Kind.DIV));
		}
		else if(input == '%'){
			tokenList.add(new Token(Kind.MOD));
		}
		else if(input == '('){
			tokenList.add(new Token(Kind.LPAREN));
		}
		else if(input == ')'){
			tokenList.add(new Token(Kind.RPAREN));
		}
		else if(input == '['){
			tokenList.add(new Token(Kind.LSQUARE));
		}
		else if(input == ']'){
			tokenList.add(new Token(Kind.RSQUARE));
		}
		else if(input == '&'){
			tokenList.add(new Token(Kind.AND));
		}
		else if(input == '|'){
			tokenList.add(new Token(Kind.OR));
		}
		else if(input == ','){
			tokenList.add(new Token(Kind.COMMA));
		}
		else if(input == ';'){
			tokenList.add(new Token(Kind.SEMI));
		}
		else if(input == '^'){
			tokenList.add(new Token(Kind.RETURN));
		}
		//
		else{//This is an error
			//For now this will be unimplemented junk.

		}
		
	}
	
	private void StringLit() {
		raw += "\"";
		int tokenCol = col;
		int tokenLine = line;
		boolean esc = false;
		i++; //Skip the first quotation mark.
		for(; i < fullLen; i++) {
			char temp = fullText.charAt(i);
			if(temp == '\\') { //Regular Backslash.
				esc = true;
			}
			//'b'|'t'|'n'|'f'|'r'|'"'|' ' '|'\'
			else if(!(temp == 'b' || temp == 't' || temp == 'n' || temp == 'f'
					|| temp == 'r' || temp == '"' || temp == '\'' || temp == '\\') 
					&& esc) {
				esc = false;
			}
			else if(temp == '"') {
				break;
			}
			raw += fullText.charAt(i);
		}
		
		createToken(Kind.STRING_LIT, tokenLine, tokenLine);

	}
	
	private void Comment() {
		//Consume "#" character at start of comment.
		for(;i < fullLen; i++) {
			if(fullText.charAt(i) == '\n') {
				i++; //Move past newline char.
				col = 0;
				line++;
				break;
			}
		}
	}
	
	private void Whitespace(char input) {
		if(input == ' ') {
			col++;
		}
		else if(input == '\n') {
			col = 0;
			line++;
		}
		else if(input == '\t') {
			col += 4;
		}
		else if(input == '\r') {
			col = 0;
			line++;
		}
	}
	
	private void Zero() {
		int tokenCol = col;
		int tokenLine = line;
		if(i+1 < fullLen) { //Check if not at end of file.
			if(fullText.charAt(i+1) == '.'){ //If decimal.
				NumLit();
			}
		}
		else{	
			raw += fullText.charAt(i);
			col++;
			createToken(Kind.INT_LIT, tokenLine, tokenCol);
		}		
	}

	private void Ident() {
		int tokenCol = col;
		int tokenLine = line;
		for(;i<fullLen;i++) { //Check for other valid ident chars
			//i will remain consistent.
			char temp = fullText.charAt(i);
			//A-Z a-z $ _ 0-9
			if((temp >= 65 && temp <= 90) || (temp >= 97 && temp <= 122) ||
					temp == '_' || temp == '$' || (temp >=48 && temp <= 57)){
				raw+= fullText.charAt(i);
				col++; //Ident will never change the line number.
			}
			else { //If not valid Ident break;
				i--;
				break;
			}
		}

		Kind k; //Hold type for generic function call.

		//HERE WOULD BE A GOOD TIME TO CHECK FOR KEYWORDS.
		switch (raw) {
			case "int", "float", "string", "boolean", "color", "image" -> k = Kind.TYPE;
			case "getRed", "getGreen", "getBlue" -> k = Kind.COLOR_OP;
			case "getWidth", "getHeight" -> k = Kind.IMAGE_OP;
			case "void" -> k = Kind.KW_VOID;
			case "BLACK" ,"BLUE", "CYAN","DARK_GRAY", "GRAY", "GREEN","LIGHT_GRAY","MAGENTA","ORANGE","PINK",
					"RED","WHITE","YELLOW" -> k = Kind.COLOR_CONST;
			case "console" -> k = Kind.KW_CONSOLE;
			case "if" -> k = Kind.KW_IF;
			case "fi" -> k = Kind.KW_FI;
			case "else" -> k = Kind.KW_ELSE;
			case "write" -> k = Kind.KW_WRITE;
			default -> System.out.println("Parameter is unknown");
		};
		createToken(Kind.IDENT, tokenLine, tokenCol);
	}
	
	private void NumLit() {
		int tokenCol = col;
		int tokenLine = line;
		Kind tokKind = Kind.INT_LIT;
		boolean decimal = false;
		boolean wasZero = false; //fullText.charAt(i) == 48 ? true : false;
		for(; i < fullLen;i++) {
			char temp = fullText.charAt(i);
			if(temp >= 48 && temp <= 57) {
				raw += fullText.charAt(i);
				col++; //numLit will never change the line number.
			}
			else if(temp == '.' && decimal) {
				tokKind = Kind.ERROR;
				raw = "Multiple decimal points in FLOAT_LIT.";
			}
			else if(temp == '.') {
				decimal = true;
				tokKind = Kind.FLOAT_LIT;
			}
			else {
				i--;
				break;
			}
		}
		createToken(tokKind, tokenLine, tokenCol);
	}
	
	/*
	private void Start(char input) {
		/* --- BEGIN START --- */
		/*raw = "";
		ident = false;
		if(input == '"') { //String Start
			state = State.STRING;
			//Do not add input to string.
			return;
		}
		//A-Z && a-z && $ && _
		else if((input >= 65 && input <= 90) || (input >= 97 && input <= 122) ||
				input == '_' || input == '$') {
			state = State.IDENT; //Can be treated as IDENT beginning.
			ident = true;
		}
		//Numbers 1-9
		else if(input >= 49 && input <= 57) {
			state = State.NUMBER; //Always a NUM lit.
		}
		else if(input == 48){
			state = State.ZERO;
		}
		else if(input == '#') {
			state = State.COMMENT;
		}
		
		col = curCol; //Set the start position.
		curCol++; //Increment current column.
	}*/
	
	private void EOF() {
		tokenList.add(new Token(Kind.EOF));
	}

	
	private void checkVal(char input) {	
		//ANY RESET SHOULD GENERATE A TOKEN.
		
		//If we are not in a comment or string and find a white space, then reset.
		if((state != State.STRING) || (state != State.COMMENT) && (input == ' ' ||
				input == '\t' || input == '\r' || input == '\n')) {
			if(input == '\r' || input == '\n') { //New Line chars.
				curLine++;
				curCol = 0; //Reset column.
			}
			else {
				curCol++;
			}
			state = State.START;
			return;
		}
		//Check current state and determine what to do.
		switch(state) {
			case START: //White space should reset to here.
				/* --- BEGIN START --- */
				raw = "";
				//ident = false;
				if(input == '"') { //String Start
					state = State.STRING;
					//Do not add input to string.
					return;
				}
				//A-Z && a-z && $ && _
				else if((input >= 65 && input <= 90) || (input >= 97 && input <= 122) ||
						input == '_' || input == '$') {
					state = State.IDENT; //Can be treated as IDENT beginning.
					//ident = true;
				}
				//Numbers 1-9
				else if(input >= 49 && input <= 57) {
					state = State.NUMBER; //Always a NUM lit.
				}
				else if(input == 48){
					state = State.ZERO;
				}
				else if(input == '#') {
					state = State.COMMENT;
				}
				else if(input == '.') { //This is not valid input
					state = State.DECIMAL;
				}
				
				col = curCol; //Set the start position.
				curCol++; //Increment current column.
				
				break; 
				/* --- END OF START --- */
			case STRING:
				/* --- BEGIN STRING --- */
				if(input == '"' && escape != true) {
					state = State.START;
					//GENERATE TOKEN
					//Do not add input to string.
					return;
				}
				else if(input == '"' && escape == true) {
					escape = false;
				}
				
				break;
				/* --- END OF STRING ---*/
			case COMMENT:
				if(input == '\r' || input == '\n') {
					state = State.NEWLINE;
					//GENERATE TOKEN
					checkVal(input);
					return;
				}
			case DECIMAL:
				/* If we find anything other than a number:
				 * 1: Reset
				 * 2: Call yourself again
				 * 3: return back to loop. */
				
				if(!(input >= 48 && input <= 57)) {
					state = State.START;
					checkVal(input);
					return;
				}
				break;
			case IDENT:
				//Anything other than A-Z & a-z & 0-9
				if(!((input >= 65 && input <= 90) || (input >= 97 && input <= 122) || 
						(input >= 48 && input <= 57) || input == '_' || input == '$')) {
					state = State.START;
					//GENERATE TOKEN.
					//ident = false;
				}
				curCol++;
				break;
			case NUMBER:
				//Check for Decimal
				if(input == '.' ) {//&& ident != true) {
					state = State.DECIMAL;
				}
				//Check for 0-9
				else if(!(input >= 48 && input <= 57)) {
					state = State.START;
					//GENERATE TOKEN.
				}
				break;
			case ZERO:
				//Not Legal to have 00
				if(input == '0') {
					state = State.START;
					//GENERATE TOKEN
				}
				if(input == '.') {
					state = State.DECIMAL;
				}
				break;
			case NEWLINE:
				if(input == '\n' || input == '\r') {
					curCol = 0;
					col = 0;
					line++;
					curLine++;
					return;
				}
			default:
				break;
			}
		
		//Add input to raw string.
		raw += input;
	}

	@Override
	public IToken next() throws LexicalException {
		// TODO Auto-generated method stub
		IToken token = tokenList.get(index);
		if(token.getKind() == Kind.ERROR) {
			throw new LexicalException(token.getText(), token.getSourceLocation());
		}
		index++;
		return token;
	}

	@Override
	public IToken peek() throws LexicalException { //I assume this will be used later...
		// TODO Auto-generated method stub
		if(tokenList.get(index).getKind() != Kind.EOF) {
			return tokenList.get((index+1));
		}
		return null;
	}

}
