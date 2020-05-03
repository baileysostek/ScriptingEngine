package Scripting;

import java.util.HashMap;
import java.util.LinkedList;

public class Scope {

    private Scope parent = null;
    private HashMap<String, Object> scopeVariables = new HashMap<>();
    //Compile to array?
    private HashMap<Integer, LinkedList<TokenCodePair>> statements = new HashMap<>();

    public Scope(){

    }

    public Scope(Scope parent){
        this.parent = parent;
    }

    public Object getVar(String name){
        if(scopeVariables.containsKey(name)){
            return scopeVariables.get(name);
        }else{
            if(this.parent != null){
                return this.parent.getVar(name);
            }
        }
        return null;
    }

    public void setVar(String name, Object value){
        if(scopeVariables.containsKey(name)){
            scopeVariables.put(name, value);
        }else{
            if(this.parent != null){
                this.parent.setVar(name, value);
            }
        }
    }

    public boolean containedInScope(String name){
        if(scopeVariables.containsKey(name)){
           return true;
        }else{
            if(parent != null){
                return parent.containedInScope(name);
            }else{
                return false;
            }
        }
    }

    public void addVar(String id, Object value){
        scopeVariables.put(id, value);
    }

    //Statements are only accessable in THIS SCOPE
    public int addStatement(LinkedList<TokenCodePair> pairs){
        int size = statements.size();
        this.statements.put(size, pairs);
        return size;
    }

    //TODO refactor to hashArray?
    public boolean hasStatement(int index){
        return statements.containsKey(index);
    }

    public LinkedList<TokenCodePair> getStatementSize(int key){
        return this.statements.get(key);
    }

    private void setParent(Scope scope) {
        this.parent = scope;
    }

}
