package Scripting;

import java.util.LinkedList;

public class ScriptFunction {
    String name;
    LinkedList<TokenCodePair> body   = new LinkedList<>();
    LinkedList<TokenCodePair> params = new LinkedList<>();

    LinkedList<TokenCodePair>[] body_segments;

    //TODO unique hash for this function name with these param types. in JS this is param number.

    public ScriptFunction(String name, LinkedList<TokenCodePair> body, LinkedList<TokenCodePair> params){
        this.name = name;
        this.body = body;
        this.params = params;

        body_segments = ScriptingEngine.getInstance().bodyToActions(body);
    }

}
