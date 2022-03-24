package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Types;

import java.util.HashMap;

public class SymbolTable {

//TODO:  Implement a symbol table class that is appropriate for this language.
    public record symbol(String name, Types.Type type, boolean init){}

    HashMap<String, Declaration> entries;

    public SymbolTable(){
        entries = new HashMap<>();
    }

    //Returns true if name inserted.
    public boolean insert(String name, Declaration dec){
        return (entries.putIfAbsent(name, dec) == null);
    }

    //Returns null if not declared.
    public Declaration lookup(String name){
        return entries.get(name);
    }
    public boolean remove(String name){
        if(entries.remove(name) != null){
            return true;
        }
        return false;
    }

}
