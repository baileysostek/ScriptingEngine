package Scripting;

import java.util.HashMap;
import java.util.LinkedList;

public class Script {
    String name;
    //Convert this file to linear list of tokens.
    LinkedList<TokenCodePair> tokensInFile = new LinkedList<>();

    // Functions in this class
    HashMap<String, ScriptFunction> functions                    = new HashMap<>();
    HashMap<String, LinkedList<TokenCodePair>[]> nonlinearBlocks = new HashMap<>();
    // Variables in this class
    Scope scope;


    private boolean inComment = false;

    public Script(){
        scope = new Scope();
    }

    public Script(Scope scope){
        this.scope = scope;
    }

    public boolean hasFunction(String name){
        return functions.containsKey(name);
    }

    public LinkedList<TokenCodePair> getTokensInFile() {
        return tokensInFile;
    }

    public void setName(String fileName) {
        this.name = fileName;
    }

    public boolean getInComment(){
        return this.inComment;
    }

    public void setInComment(boolean inComment){
        this.inComment = inComment;
    }

    public Object getVar(String name){
        return scope.getVar(name);
    }

    public void addVar(String name, Object value){
        this.scope.addVar(name, value);
    }

    //Does not allow for recompile of nonlinearBlock
    public String addNonlearBlock(LinkedList<TokenCodePair> nonlinearBody){
        String id = nonlinearBlocks.size()+"";
        nonlinearBlocks.put(id, ScriptingEngine.getInstance().bodyToActions(nonlinearBody));
        return id;
    }
}