package Scripting;

import util.Callback;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;

public class LanguageDescription {

    public String   COMMENT_SEQUENCE       = "//";
    public String   BLOCK_COMMENT_START    = "/*";
    public String   BLOCK_COMMENT_END      = "*/";

    public String[] STRING_START_CHARACTER = new String[]{"\"", "\'"};
    public String   SPACE_CHARACTER        = " ";

    //Lengths
    public int LONGEST_STRING_DECLARATION  = 0;
    public int SHORTEST_STRING_DECLARATION = 0;

    //Token Mapping
    public HashMap<String, EnumTokenType> tokens       = new HashMap<>();
    public HashMap<EnumTokenType, String> tokens_prime = new HashMap<>();

    //Patterns for pattern matching
    private LinkedList<Pattern> patterns = new LinkedList<>();

    //Data definitions
    public EnumTokenType[] dataTokens           = new EnumTokenType[]{EnumTokenType.STRING_DATA, EnumTokenType.FLOAT_DATA, EnumTokenType.INT_DATA, EnumTokenType.HEX_DATA};
    public EnumTokenType[] bodyDelimiters       = new EnumTokenType[]{EnumTokenType.LINE_CLOSE, EnumTokenType.SCOPE_OPEN, EnumTokenType.SCOPE_CLOSE};
    public EnumTokenType[] evaluationCharacters = new EnumTokenType[]{EnumTokenType.ADD, EnumTokenType.SUB, EnumTokenType.MUL, EnumTokenType.DIV, EnumTokenType.NOT, EnumTokenType.LESS, EnumTokenType.LESS_EQUAL, EnumTokenType.GREATER, EnumTokenType.GREATER_EQUAL, EnumTokenType.NOT, EnumTokenType.COMPARE_EQUAL};


    public HashMap<EnumTokenType, Integer> oppValues = new HashMap<EnumTokenType, Integer>();

    public LanguageDescription(){
        LONGEST_STRING_DECLARATION  = STRING_START_CHARACTER[StringUtils.maxStringLength(STRING_START_CHARACTER)].length();
        SHORTEST_STRING_DECLARATION = STRING_START_CHARACTER[StringUtils.minStringLength(STRING_START_CHARACTER)].length();

        //Define pemdas
        oppValues.put(EnumTokenType.NOT, 4);
        oppValues.put(EnumTokenType.COMPARE_EQUAL, 4);
        oppValues.put(EnumTokenType.LESS, 3);
        oppValues.put(EnumTokenType.LESS_EQUAL, 3);
        oppValues.put(EnumTokenType.GREATER, 3);
        oppValues.put(EnumTokenType.GREATER_EQUAL, 3);
        oppValues.put(EnumTokenType.ADD, 2);
        oppValues.put(EnumTokenType.SUB, 2);
        oppValues.put(EnumTokenType.MUL, 1);
        oppValues.put(EnumTokenType.DIV, 1);

        //Token Definition section
        registerTokenCodePair(EnumTokenType.FUNCTION             , "function");
        registerTokenCodePair(EnumTokenType.PROP_OF              , ".");
        registerTokenCodePair(EnumTokenType.PAREN_OPEN           , "(");
        registerTokenCodePair(EnumTokenType.PAREN_CLOSE          , ")");
        registerTokenCodePair(EnumTokenType.SCOPE_OPEN           , "{");
        registerTokenCodePair(EnumTokenType.SCOPE_CLOSE          , "}");
        registerTokenCodePair(EnumTokenType.SET_EQUAL            , "=");
        registerTokenCodePair(EnumTokenType.LINE_CLOSE           , ";");
        registerTokenCodePair(EnumTokenType.PARAM_SEPARATION     , ",");
        registerTokenCodePair(EnumTokenType.EMPTY_ARRAY          , "[]");
        registerTokenCodePair(EnumTokenType.ARRAY_START          , "[");
        registerTokenCodePair(EnumTokenType.ARRAY_END            , "]");
        registerTokenCodePair(EnumTokenType.IF                   , "if");
        registerTokenCodePair(EnumTokenType.NEW                  , "new");
        registerTokenCodePair(EnumTokenType.ADD                  , "+");
        registerTokenCodePair(EnumTokenType.SUB                  , "-");
        registerTokenCodePair(EnumTokenType.MUL                  , "*");
        registerTokenCodePair(EnumTokenType.DIV                  , "/");
        registerTokenCodePair(EnumTokenType.LESS                 , "<");
        registerTokenCodePair(EnumTokenType.GREATER              , ">");
        registerTokenCodePair(EnumTokenType.GREATER_EQUAL        , ">=");
        registerTokenCodePair(EnumTokenType.LESS_EQUAL           , "<=");
        registerTokenCodePair(EnumTokenType.COMPARE_EQUAL        , "==");
        registerTokenCodePair(EnumTokenType.NOT                  , "!=");
        registerTokenCodePair(EnumTokenType.CLASS_DECLARATION    , "class");
        registerTokenCodePair(EnumTokenType.IMPORT               , "import");
        registerTokenCodePair(EnumTokenType.IMPORT_LOOKUP        , "from");
        registerTokenCodePair(EnumTokenType.VARIABLE_DECLARATION , new String[]{"var", "let", "const"});

        //Look for class
        //Look for function directly
        addPattern(new EnumTokenType[]{EnumTokenType.CLASS_DECLARATION, EnumTokenType.UNKNOWN, EnumTokenType.SCOPE_OPEN}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script script = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];

                int offset = tokenIndex;

                TokenCodePair unknown = script.tokensInFile.get(offset);
                pattern[1].setType(EnumTokenType.STRUCT_NAME);

                System.out.println("Class Definition:"+pattern[1].getCode());
                LinkedList<TokenCodePair> classBody = ScriptingEngine.getInstance().getScope(script.getTokensInFile(), tokenIndex+2);

                return classBody.size();
            }
        });

        //Pattern creation
        addPattern(new EnumTokenType[]{EnumTokenType.FUNCTION, EnumTokenType.UNKNOWN, EnumTokenType.PAREN_OPEN}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                pattern[1].setType(EnumTokenType.FUNCTION_NAME);
//                System.out.println("Found Function:"+pattern[1].getCode());
                return 0;
            }
        });

        addPattern(new EnumTokenType[]{EnumTokenType.VARIABLE_DECLARATION, EnumTokenType.UNKNOWN, EnumTokenType.SET_EQUAL}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                pattern[1].setType(EnumTokenType.VARIABLE_NAME);
//                System.out.println("Found Variable:"+pattern[1].getCode());
                return 0;
            }
        });

        addPattern(new EnumTokenType[]{EnumTokenType.VARIABLE_DECLARATION, EnumTokenType.UNKNOWN, EnumTokenType.LINE_CLOSE}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                pattern[1].setType(EnumTokenType.VARIABLE_NAME);
//                System.out.println("Found Variable:"+pattern[1].getCode());
                return 0;
            }
        });

        addPattern(new EnumTokenType[]{EnumTokenType.ARRAY_START, EnumTokenType.ARRAY_END}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                //Register Function

                def.tokensInFile.set(tokenIndex, new TokenCodePair(EnumTokenType.EMPTY_ARRAY, pattern[0].code+pattern[1].code));
                def.tokensInFile.remove(tokenIndex+1);

                return 0;
            }
        });

        addPattern(new EnumTokenType[]{EnumTokenType.FUNCTION, EnumTokenType.FUNCTION_NAME, EnumTokenType.PAREN_OPEN}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                //Register Function

                int findClose = (tokenIndex + 3);
                int params = 0;
                int offset = 0;
                LinkedList<TokenCodePair> paramTokens = new LinkedList<>();
                while(!def.tokensInFile.get(findClose).getToken().equals(EnumTokenType.PAREN_CLOSE)){
                    if(def.tokensInFile.get(findClose).getToken().equals(EnumTokenType.PARAM_SEPARATION)){
                        if(params == 0){
                            params++;
                        }
                        params++;
                    }else{
                        paramTokens.addLast(def.tokensInFile.get(findClose));
                    }
                    findClose++;
                    offset++;
                }
                offset++;
                int scopeStart = offset + 3;
                offset++;
                int diff = 1;
                int scopeSize = 1;
                while(diff > 0){
                    if(def.tokensInFile.get((tokenIndex + 3) + offset).getToken().equals(EnumTokenType.SCOPE_CLOSE)){
                        diff--;
                    }
                    if(def.tokensInFile.get((tokenIndex + 3) + offset).getToken().equals(EnumTokenType.SCOPE_OPEN)){
                        diff++;
                    }
                    offset++;
                    scopeSize++;
                }

//                System.out.println("-------------------------"+scopeSize+"-------------------------");
                LinkedList<TokenCodePair> body = new LinkedList<>();
                for(int i = 0; i < scopeSize; i++){
                    body.addLast(def.tokensInFile.get(scopeStart+i+tokenIndex));
                }

                def.functions.put(pattern[1].code, new ScriptFunction(pattern[1].code, body, paramTokens));

                return 0;
            }
        });

        addPattern(new EnumTokenType[]{EnumTokenType.VARIABLE_DECLARATION, EnumTokenType.VARIABLE_NAME, EnumTokenType.SET_EQUAL, EnumTokenType.WILDCARD}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                LinkedList<TokenCodePair> line = new LinkedList<>();
                line.addFirst(pattern[3]);
                def.addVar(pattern[1].code, ScriptingEngine.resolveToken(def, def.scope, line));
                System.out.println("Defining:"+pattern[1].code+"="+pattern[3].code);
//                System.out.println(def.tokensInFile.get(tokenIndex));

                return 0;
            }
        });

        addPattern(new EnumTokenType[]{EnumTokenType.VARIABLE_DECLARATION, EnumTokenType.VARIABLE_NAME, EnumTokenType.LINE_CLOSE}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                //Register Function
                def.addVar(pattern[1].code, null);

//                System.out.println(def.tokensInFile.get(tokenIndex));

                return 0;
            }
        });

        //Referencing Class or Variable or Function
        addPattern(new EnumTokenType[]{EnumTokenType.UNKNOWN, EnumTokenType.PROP_OF}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script script = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];

                int offset = tokenIndex;
                boolean foundBase = false;
                ExpandedClass baseClass = null;
                TokenCodePair unknown = script.tokensInFile.get(offset);
                while(unknown.getToken().equals(EnumTokenType.UNKNOWN)) {
                    if(!foundBase){
                        if (ScriptingEngine.getInstance().hasClassLoaded(unknown.code)) {
                            foundBase = true;
                            baseClass = ScriptingEngine.getInstance().registeredClasses.get(unknown.code);
                            unknown.setType(EnumTokenType.CLASS_REFRENCE);
                            //Determine how far we have to resolve
                        }else{
                            //Reference is not a class
//                            System.out.println("Check for variable reference");
                            if(script.scope.containedInScope(unknown.code)){
                                unknown.setType(EnumTokenType.VARIABLE_REFERENCE);
                            }else{
                                System.out.println("This token is truly unknown: "+unknown.code);
                                return  0;
                            }
                            break;
                        }
                    }else{
                        if (baseClass.fields.containsKey(unknown.code)) {
                            Field field = baseClass.fields.get(unknown.code);
//                            System.out.println("Prop Class type"+field.getType().getName());
                            if(!ScriptingEngine.getInstance().hasClassLoaded(field.getType().getName())){
                                //TODO dont just forward instance, resolve newInstance
                                ScriptingEngine.getInstance().put(field.getType().getName(), field.getType());
                            }
                            unknown.setType(EnumTokenType.CLASS_FIELD);
                            baseClass = ScriptingEngine.getInstance().registeredClasses.get(field.getType().getName());
                        }else if(baseClass.methods.containsKey(unknown.code)){
                            unknown.setType(EnumTokenType.CLASS_METHOD);
                            //Advance until find close_paren
                        }else{
//                            System.out.println("Prop chain is over");
                            break;
                        }
                    }
                    offset++;
                    unknown = script.tokensInFile.get(offset);
                    if(!unknown.getToken().equals(EnumTokenType.PROP_OF)){
//                        System.out.println("Prop chain is over");
                        break;
                    }
                    offset++;
                    unknown = script.tokensInFile.get(offset);
                }


                return 0;
            }
        });

        //Look for function directly
        addPattern(new EnumTokenType[]{EnumTokenType.UNKNOWN, EnumTokenType.PAREN_OPEN}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script script = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];

                int offset = tokenIndex;

                TokenCodePair unknown = script.tokensInFile.get(offset);
                //Resolve the function we are calling
                if(script.hasFunction(unknown.code)){
                    unknown.setType(EnumTokenType.CALL_TO_FUNCTION);
                    ScriptFunction function = script.functions.get(unknown.code);
//                    System.out.println(function);
                    int paramCount = 0;
                    int expectParams = function.params.size();
//                    while(!unknown.getToken().equals(EnumTokenType.SCOPE_CLOSE)) {
//                        if(){
//
//                        }
//                        offset++;
//                        unknown = script.tokensInFile.get(offset);
//                    }
                }else{
                    if(script.tokensInFile.get(offset-1).getToken().equals(EnumTokenType.VARIABLE_DECLARATION)){
                        TokenCodePair lastToken = script.tokensInFile.get(offset-1);
                        lastToken.setType(EnumTokenType.UNKNOWN);
                        lastToken.code = lastToken.getCode() + unknown.getCode();
                        script.tokensInFile.remove(offset);
                        unknown = lastToken;
                        System.out.println("Redefinition of token, this function name may have started with the characters that declare a variable. "+unknown.getCode());
                        //Rematch on function
                        return  -2;
                    }else if(script.tokensInFile.get(offset-1).getToken().equals(EnumTokenType.NEW)){
                        if(ScriptingEngine.getInstance().registeredClasses.containsKey(unknown.getCode())){
                            System.out.println("Class?"+ScriptingEngine.getInstance().registeredClasses.get(unknown.getCode()).name);
                            unknown.setType(EnumTokenType.CLASS_REFRENCE);
                        }
                        return  1;
                    }else {
                        System.out.println("This token is truly unknown: " + unknown.code + "|" + script.tokensInFile.get(offset - 1).getToken() + "2");
                        return  0;
                    }
                }

                return 0;
            }
        });

        //Move to own kind of pattern, def from def to def callback.
        addPattern(new EnumTokenType[]{EnumTokenType.IF, EnumTokenType.PAREN_OPEN}, new Callback() {
            //OnMatch
            @Override
            public Object callback(Object... objects) {
                Script def = (Script) objects[0];
                TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                int tokenIndex = (int) objects[2];
                //Advance until we find a this pattern
                LinkedList<EnumTokenType> tokens = new LinkedList<>();
                tokens.addLast(EnumTokenType.PAREN_CLOSE);
                tokens.addLast(EnumTokenType.SCOPE_OPEN);

                LinkedList<TokenCodePair> ifBody = new LinkedList<>();

                Pattern ifClosePattern = new Pattern(tokens, new Callback() {
                    @Override
                    public Object callback(Object... objects) {
                        Script def = (Script) objects[0];
                        TokenCodePair[] pattern = (TokenCodePair[]) objects[1];
                        int tokenIndex = (int) objects[2];

                        for(TokenCodePair tcp : ifBody){
                            System.out.println("if:"+tcp.getToken());
                        }

                        return 1;
                    }
                });

                TokenCodePair[] check = new TokenCodePair[ifClosePattern.getLength()];

                for(int i = tokenIndex + 1; i < def.getTokensInFile().size()-check.length; i++){
                    for(int j = 0; j < check.length; j++){
                        check[j] = def.getTokensInFile().get(i + j);
                        System.out.println("check"+check[j].code);
                        if(j == 0){
                            ifBody.addLast(def.getTokensInFile().get(i));
                        }
                        if(ifClosePattern.tryMatch(def, check, i+j) > 0){
                            System.out.println("Found if starting at:"+tokenIndex+" ending at:"+(i+j));
                            System.out.println("Adding statement to scope");

                            int scopeIndex = 0;
                            int k = 0;
                            int ifEnds = -1;
                            loop:while((i+j+k) < def.getTokensInFile().size()){
                                TokenCodePair token = def.getTokensInFile().get((i+j+k));
                                if(token.getToken().equals(EnumTokenType.SCOPE_OPEN)){
                                    scopeIndex++;
                                }
                                if(token.getToken().equals(EnumTokenType.SCOPE_CLOSE)){
                                    scopeIndex--;
                                }
                                if(scopeIndex == 0){
                                    System.out.println("Scope ends:"+(i+j+k));
                                    ifEnds = (i+j+k);
                                    break loop;
                                }
                                k++;
                            }

                            //TODO refactor Maybe this isnt a bad idea
                            //All iffs are global scoped for now
                            pattern[0].code = def.scope.addStatement(ifBody)+""; //Lookup

                            LinkedList<TokenCodePair> nonlinearBody = new LinkedList<>();

                            //Resolve nonlinear block in if statement
                            int lookupOffset = 0;
                            for(int l = (i+j); l < ifEnds+1; l++){
                                nonlinearBody.addLast(def.tokensInFile.get(l - lookupOffset));
                                def.tokensInFile.remove(l - lookupOffset);
                                lookupOffset++;
                            }

                            pattern[1].code = def.addNonlearBlock(nonlinearBody);

                            return (i + j - (tokenIndex + 1));
                        }
                    }
                }

                return 0;
            }
        });


        //Check to see that all tokens are defined
        LinkedList<EnumTokenType> missingTokens = new LinkedList<>();
        for(EnumTokenType token : EnumTokenType.values()){
            missingTokens.push(token);
        }
        for(EnumTokenType token : tokens.values()){
            missingTokens.remove(token);
        }
        for(EnumTokenType token : missingTokens){
            System.err.println("There is no definition for token: " + token + " in this language.");
        }
    }

    public void registerTokenCodePair(EnumTokenType token, String code){
        this.tokens.put(code, token);
        this.tokens_prime.put(token, code);
    }

    public void registerTokenCodePair(EnumTokenType token, String[] code){
        for(String s : code){
            this.tokens.put(s, token);
        }
        this.tokens_prime.put(token, code[0]);
    }

    public void addPattern(EnumTokenType[] tokens, Callback callback){
        LinkedList<EnumTokenType> patternTokens = new LinkedList<>();
        for(EnumTokenType token : tokens){
            patternTokens.addLast(token);
        }
        patterns.addLast(new Pattern(patternTokens, callback));
    }

    public LinkedList<Pattern> getPatterns() {
        return this.patterns;
    }

    public boolean tokenIsa(TokenCodePair token, EnumTokenType[] checkTokens) {
        for(EnumTokenType type : checkTokens){
            if(token.getToken().equals(type)){
                return true;
            }
        }
        return false;
    }

    public int tokenIsIn(LinkedList<TokenCodePair> tokens, EnumTokenType[] checkTokens){
        int index = 0;

        for(TokenCodePair token : tokens){
            if(tokenIsa(token, checkTokens)){
                return index;
            }
            index++;
        }
        return -1;
    }

    public int tokenIsInOuterParens(LinkedList<TokenCodePair> tokens, EnumTokenType[] checkTokens, int startIndex){
        int index = startIndex;
        int oppValue = 0;
        int outIndex = -1;

        int parenIndex = 0;

        for(int i = startIndex; i < tokens.size(); i++){
            TokenCodePair token = tokens.get(i);
            if(token.getToken().equals(EnumTokenType.PAREN_OPEN)){
                parenIndex++;
            }
            if(token.getToken().equals(EnumTokenType.PAREN_CLOSE)){
                parenIndex--;
            }
            if(parenIndex == 0) {
                if (tokenIsa(token, checkTokens)) {
                    int thisOppValue = oppValues.get(token.getToken());
                    if(thisOppValue > oppValue) {
                        outIndex = index;
                        oppValue = thisOppValue;
                    }
                }
            }
            index++;
        }

        return outIndex;
    }


    public Object performOpp(EnumTokenType opp, Object obj1, Object obj2){
        switch (opp){
            case ADD:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N + obj2N;
                }catch(NumberFormatException e){
                    return obj1.toString() + obj2.toString();
                }
            }
            case SUB:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N - obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case MUL:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N * obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case DIV:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N / obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case LESS:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N < obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case GREATER:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N > obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case LESS_EQUAL:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N <= obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case GREATER_EQUAL:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N >= obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case COMPARE_EQUAL:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N == obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
            case NOT:{
                //Number check
                try{
                    float obj1N = Float.parseFloat(obj1.toString());
                    float obj2N = Float.parseFloat(obj2.toString());
                    return obj1N != obj2N;
                }catch(NumberFormatException e){
                    return "NaN";
                }
            }
        }
        return null;
    }
}
