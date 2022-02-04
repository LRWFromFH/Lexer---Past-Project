package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.List;
import edu.ufl.cise.plc.IToken.Kind;

public class Lexer implements ILexer {
	
	List<Token> tokenList;
	int index; //Keeps track of where lexer is in list.
	int line;
	int col;
	int i;
	String fullText;
	int fullLen;
	String raw;
	
	//This string is the raw input of the program.
	public Lexer(String input) {
		tokenList = new ArrayList<>();
		line = 0;
		col = 0;
		fullText = input;
		fullLen = input.length();
		//Run the length of the string.
		lex(fullText);
	}
	
	private void lex(String input) {
		if(!input.equals("")) {
			for(i = 0; i < fullLen; i++) {
				Tokenize(fullText.charAt(i));
				if(!tokenList.isEmpty()){
					if(getLastToken().getKind() == Kind.ERROR){
						break;
					}
				}
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
			//TODO: Adapt to createToken
			// (eg. createToken(Kind.TIMES, "String", int ColumnIncrement))
			//	None of these alone can create new lines.
			tokenList.add(new Token(Kind.TIMES, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '+'){
			tokenList.add(new Token(Kind.PLUS, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '/'){
			tokenList.add(new Token(Kind.DIV, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '%'){
			tokenList.add(new Token(Kind.MOD, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '('){
			tokenList.add(new Token(Kind.LPAREN, String.valueOf(input), line, col));
			col++;
		}
		else if(input == ')'){
			tokenList.add(new Token(Kind.RPAREN, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '['){
			tokenList.add(new Token(Kind.LSQUARE, String.valueOf(input), line, col));
			col++;
		}
		else if(input == ']'){
			tokenList.add(new Token(Kind.RSQUARE, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '&'){
			tokenList.add(new Token(Kind.AND, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '|'){
			tokenList.add(new Token(Kind.OR, String.valueOf(input), line, col));
			col++;
		}
		else if(input == ','){
			tokenList.add(new Token(Kind.COMMA, String.valueOf(input), line, col));
			col++;
		}
		else if(input == ';'){
			tokenList.add(new Token(Kind.SEMI, String.valueOf(input), line, col));
			col++;
		}
		else if(input == '^'){
			tokenList.add(new Token(Kind.RETURN, String.valueOf(input), line, col));
			col++;
		}
		//
		else{//This is an error
			//For now this will be unimplemented junk.
			if(!genericSymbol(input)){
				tokenList.add(new Token(Kind.ERROR, "Symbol Unrecognized", line, col));
			}

		}
		
	}

	public boolean genericSymbol(char input){ //This returns true if handled.
		if(i+1 < fullLen){
			char temp = fullText.charAt(i+1);
			if(input == '-'){
				if(temp == '>'){
					tokenList.add(new Token(Kind.RARROW, "->", line, col));
					i++;
					col++;
				}
				else{
					tokenList.add(new Token(Kind.MINUS, "-", line, col));
				}
				col++;
				return true;
			}
			else if(input == '<'){
				if(temp == '<'){
					tokenList.add(new Token(Kind.LANGLE, "<<", line, col));
					i++;
					col++;
				}
				else if(temp == '-'){
					tokenList.add(new Token(Kind.LARROW, "<-", line, col));
					i++;
					col++;
				}
				else if(temp == '='){
					tokenList.add(new Token(Kind.LE, "<=", line, col));
					i++;
					col++;
				}
				else{
					tokenList.add(new Token(Kind.LT, "<", line, col));
				}
				col++;
				return true;
			}
			else if(input == '>'){
				if(temp == '>'){
					tokenList.add(new Token(Kind.RANGLE, ">>", line, col));
					i++;
					col++;
				}
				else if(temp == '='){
					tokenList.add(new Token(Kind.GE, ">=", line, col));
					i++;
					col++;
				}
				else{
					tokenList.add(new Token(Kind.GT, ">", line, col));
				}
				col++;
				return true;
			}
			else if(input == '!'){
				if(temp == '='){
					tokenList.add(new Token(Kind.NOT_EQUALS, "!=", line, col));
					i++;
					col++;
				}
				else{
					tokenList.add(new Token(Kind.BANG, "!", line, col));
				}
				col++;
				return true;
			}
			else if(input == '='){
				if(temp == '='){
					tokenList.add(new Token(Kind.EQUALS, "==", line, col));
					i++;
					col++;
				}
				else{
					tokenList.add(new Token(Kind.ASSIGN, "=", line, col));
				}
				col++;
				return true;
			}
		}

		return false;
	}

	private void StringLit() {
		String clean = "";
		raw += "\"";
		int tokenCol = col;
		int tokenLine = line;
		boolean esc = false;
		i++; //Skip the first quotation mark.
		for(; i < fullLen; i++) {
			char temp = fullText.charAt(i);
			if(temp == '\\'){
				esc = true;
				col++;
				continue;
			}
			if(temp == '"' && esc == false) {
				raw += '"';
				col++;
				break;
			}

			if(esc){
				switch(temp) {
					// 'b'|'t'|'n'|'f'|'r'|'"'|' ' '|'\'
					case 'b' -> {clean += '\b';}
					case 't' -> {clean += '\t';}
					case 'n' -> {clean += '\n';}
					case 'f' -> {clean += '\f';}
					case 'r' -> {clean += '\r';}
					case '"' -> {clean += '\"';}
					case 39  -> {clean += '\'';} //ASCII VAL
					case '|' -> {clean += '\\';}
				}
				raw += fullText.charAt(i - 1);
				raw += fullText.charAt(i);
				esc = false;
				continue;
			}
			if(fullText.charAt(i) != '"'){
				clean += fullText.charAt(i);
			}
			raw += fullText.charAt(i);
		}

		//Properly set col
		col += raw.length() -1;
		//This is a special case Literal.
		tokenList.add(new Token(Kind.STRING_LIT, raw, clean, tokenLine, tokenCol));
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
		i--;
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
			col += 4; //The IDE references \t for cols as well.
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
				return;
			}
		}

		raw += fullText.charAt(i);
		col++;
		createToken(Kind.INT_LIT, tokenLine, tokenCol);

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

		Kind k = Kind.IDENT; //Hold type for generic function call.

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
			case "true", "false" -> k = Kind.BOOLEAN_LIT;
			//default -> System.out.println("Parameter is unknown");
		}
		createToken(k, tokenLine, tokenCol);
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
				raw += temp;
				col++; //numLit will never change the line number.
			}
			else if(temp == '.' && decimal) {
				tokKind = Kind.ERROR;
				raw = "Multiple decimal points in FLOAT_LIT.";
			}
			else if(temp == '.') {
				decimal = true;
				raw += temp;
				col++;
				tokKind = Kind.FLOAT_LIT;
			}
			else {
				i--;
				break;
			}
		}

		if(tokKind == Kind.INT_LIT){
			try{
				int test = Integer.parseInt(raw);
			}
			catch(NumberFormatException e){
				tokKind = Kind.ERROR;
				raw = "Number is too big!";
			}
		}
		else if(tokKind == Kind.FLOAT_LIT){
			try{
				float test = Float.parseFloat(raw);
			}
			catch (NumberFormatException e){
				tokKind = Kind.ERROR;
				raw = "Float invalid!";
			}
		}

		createToken(tokKind, tokenLine, tokenCol);
	}

	private void EOF() {
		tokenList.add(new Token(Kind.EOF));
	}

	private IToken getLastToken(){
		return tokenList.get(tokenList.size()-1);
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
