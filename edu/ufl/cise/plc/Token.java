package edu.ufl.cise.plc;

public class Token implements IToken{
	
	Kind kind; //Store the Enum type.
	String rawText; //Store the Text.
	int line;
	int col;
	int intVal;
	float floatVal;
	boolean boolVal;
	String cleanText;
	
	public Token(Kind kind) {
		this.kind = kind;
	}
	public Token(Kind kind, String rawText, String cleanText, int line, int col){
		this.kind = kind;
		this.rawText = rawText;
		this.cleanText = cleanText;
		this.cleanText = cleanText.translateEscapes();
		this.line = line;
		this.col = col;
	}
	
	public Token(Kind kind, String rawText, int line, int col) {
		this.kind = kind;
		this.rawText = rawText;
		this.line = line;
		this.col = col;
		if(kind == Kind.INT_LIT){
			intVal = Integer.parseInt(rawText);
		}
		else if(kind == Kind.FLOAT_LIT){
			floatVal = Float.parseFloat(rawText);
		}
		else if(kind == Kind.BOOLEAN_LIT){
			boolVal = rawText.equals("true");
		}
		this.cleanText = this.rawText;
		if(kind == Kind.STRING_LIT) {
			cleanText = cleanText.translateEscapes();
			cleanText = cleanText.substring(1, cleanText.length()-1);
		}
	}
	
	public Token getToken() { //Return this token.
		return this;
	}

	@Override
	public Kind getKind() {
		// TODO Auto-generated method stub
		return kind;
	}
	
	public void setKind(Kind kind) {
		this.kind = kind;
	}

	@Override
	public String getText() { //RAW TEXT INPUT
		// TODO Auto-generated method stub
		return rawText;
	}

	@Override
	public SourceLocation getSourceLocation() {
		// TODO Auto-generated method stub
		return new SourceLocation(line, col);
	}

	@Override
	public int getIntValue() {
		// TODO Auto-generated method stub
		return intVal;
	}

	@Override
	public float getFloatValue() {
		// TODO Auto-generated method stub
		return floatVal;
	}

	@Override
	public boolean getBooleanValue() {
		// TODO Auto-generated method stub
		return boolVal;
	}

	@Override
	public String getStringValue() { //CLEAN TEXT INPUT
		// TODO Auto-generated method stub
		return cleanText;
	}

}
