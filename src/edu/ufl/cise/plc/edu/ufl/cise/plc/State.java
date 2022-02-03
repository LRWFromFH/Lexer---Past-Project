package edu.ufl.cise.plc;

public enum State {
	START, STRING, DECIMAL, IDENT, NUMBER, COMMENT, ZERO, NEWLINE
}

/************************************
* START - Beginning or reset		*
* STRING - "[LETTER|NUMBER]*"					*
* DECIMAL - [0-9].					*
* LETTER - [A-Z] | [a-z]	   		*
* NUMBER - 0 | [1-9][0-9]*		    *
*    *** ---MORE TO COME --- ***    *
*									*
************************************/