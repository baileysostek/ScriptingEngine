package Scripting;

import engine.Engine;
import entity.EntityManager;
import org.lwjgl.glfw.GLFW;
import org.omg.CORBA.UNKNOWN;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;

import org.joml.Vector3f;

public class ScriptingEngine extends Engine {

    private static ScriptingEngine scriptingEngine;

    public static LanguageDescription ld = new LanguageDescription();

    HashMap<String, Script> loadedScripts = new HashMap<>();
    HashMap<String, Object> staticClassInstances = new HashMap<>();
    HashMap<String, ClassStruct> classStructs = new HashMap<>();
    //Use reflection to access these
    static HashMap<String, ExpandedClass> registeredClasses = new HashMap<>();

    private ScriptingEngine(){

    }

    public void put(String name, Class classFile){
//        System.out.println((Object)refrenceInstance);
        ExpandedClass expandedClass = new ExpandedClass(classFile);
        registeredClasses.put(name, expandedClass);
        registeredClasses.put(classFile.getName(), expandedClass);
    }

    public void put(String name, Object object){
//        System.out.println((Object)refrenceInstance);
        ExpandedClass expandedClass = new ExpandedClass(object.getClass());
        registeredClasses.put(name, expandedClass);
        registeredClasses.put(object.getClass().getName(), expandedClass);
    }

    public void put(String name, Class classFile, Object instance){
//        System.out.println((Object)refrenceInstance);
        ExpandedClass expandedClass = new ExpandedClass(classFile);
        registeredClasses.put(name, expandedClass);
        registeredClasses.put(classFile.getName(), expandedClass);
        staticClassInstances.put(name, instance);
    }

    public void run(Script script, String functionName, Object ... callingParams){
        if(script.functions.containsKey(functionName)){
            ScriptFunction function = script.functions.get(functionName);
            //Add all param variables into the scope of this function
            Scope scope = new Scope(script.scope);
            int paramIndex = 0;
            for (Object param : callingParams) {
                if(paramIndex > function.params.size()-1){
                    System.out.println("oh no");
                }
                scope.addVar(function.params.get(paramIndex).code, param);
                paramIndex++;
            }

            evalTokens(script, scope, function.body_segments);

        }else{
            System.out.println("Function does not exist:"+functionName);
        }
    }

    public void evalTokens(Script script, Scope scope, LinkedList<TokenCodePair>[] bodySegments ){
        for(int l = 0; l < bodySegments.length; l++) {
            LinkedList<TokenCodePair> bodySegment  = bodySegments[l];
            TokenCodePair action = bodySegment.get(0);
            switch (action.getToken()) {
                case CLASS_REFRENCE: {
                    resolveMethodFromClass(script, scope, bodySegment);
                    break;
                }
                case CALL_TO_FUNCTION: {
                    //Get Params out of this call
                    int tokenPointer = 1; // Start at position 2 of this function
                    //Check to see that
                    if (!bodySegment.get(tokenPointer).getToken().equals(EnumTokenType.PAREN_OPEN)) {
                        System.err.println("Error: problem with function definition, is this an arrow function? or a lambda:"+bodySegment.get(0).getCode()+"|"+bodySegment.get(0).getToken());
                        break;
                    }
                    //Resolve all methods this should be an ld function
                    Object[] resolvedParams =  evaluateParens(script, scope, bodySegment, tokenPointer);
                    //Parse to correct class type as well
                    run(script, action.code, resolvedParams);
                    break;
                }
                case VARIABLE_DECLARATION:{
                    TokenCodePair varName = bodySegment.get(1);
                    if(bodySegment.get(2).getToken().equals(EnumTokenType.SET_EQUAL)){
                        //TODO learn why 3 is unkown
                        if(bodySegment.size() >= 5 && bodySegment.get(4).getToken().equals(EnumTokenType.NEW)){
                            if(bodySegment.get(5).getToken().equals(EnumTokenType.CLASS_REFRENCE)){
//                                    System.out.println("Creating new instance of a:"+bodySegment.get(5).getCode());
                                try {
                                    //Get params
                                    LinkedList<TokenCodePair> variableEvaluation = new LinkedList<>();
                                    for (int j = 6; j < bodySegment.size(); j++) {
                                        variableEvaluation.addLast(bodySegment.get(j));
                                    }

                                    Object[] object1 = evaluateParens(script, scope, variableEvaluation, 0);

                                    Class[] classes = new Class[object1.length];
                                    int index = 0;
                                    for(Object functionParam : object1){
                                        if(functionParam.getClass().equals(Integer.class)){
                                            classes[index] = Float.TYPE;
                                        }else if(functionParam.getClass().equals(Float.class)){
                                            classes[index] = Float.TYPE;
                                        }else {
                                            classes[index] = functionParam.getClass();
                                        }
                                        index++;
                                    }

                                    Object classValue = registeredClasses.get(bodySegment.get(5).getCode()).instance.getDeclaredConstructor(classes).newInstance(object1);
                                    scope.addVar(varName.code, classValue);
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (NoSuchMethodException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        }else if(bodySegment.size() >= 3 && bodySegment.get(3).getToken().equals(EnumTokenType.CLASS_REFRENCE)){
                            LinkedList<TokenCodePair> offsetLine = new LinkedList<>();
                            for(int j = 3; j < bodySegment.size() ; j++){
                                offsetLine.addLast(bodySegment.get(j));
                            }
                            Object result = resolveMethodFromClass(script, scope, offsetLine);
                            scope.addVar(varName.code, result);
                        }else{
                            scope.addVar(varName.code, resolveToken(script, scope, (LinkedList<TokenCodePair>) bodySegment.subList(3, bodySegment.size())));
                        }
                    }else if(bodySegment.get(2).getToken().equals(EnumTokenType.LINE_CLOSE)){
                        scope.addVar(varName.code, null);
                    }else{
                        System.out.println("Unknown:");
                    }
                    break;
                }
                case VARIABLE_REFERENCE:{
                    Object value = scope.getVar(action.getCode());

                    TokenCodePair nextAction = bodySegment.get(1);
//                        System.out.println("nextAction.getToken():"+nextAction.getToken()+"|on|"+action.getCode()+"|val|"+value);
                    switch (nextAction.getToken()){
                        case PROP_OF:{
                            ExpandedClass clazz = registeredClasses.get(value.getClass().getName());
//                                System.out.println("Value:"+value.getClass()+" clazz:"+clazz);

                            if(clazz == null){
                                //Import indirect?
                                put(value.getClass().getName(), value.getClass());
                                clazz = registeredClasses.get(value.getClass().getName());
//                                    System.out.println("Value:"+value.getClass()+" clazz:"+clazz);
                            }

                            int index = 1;
                            TokenCodePair token = bodySegment.get(index);

                            while (!token.getToken().equals(EnumTokenType.PAREN_OPEN)){
//                            System.out.println(token.getCode());
                                if(!token.getToken().equals(EnumTokenType.PROP_OF)){
                                    if(clazz.fields.containsKey(token.code)) {
                                        Field field = clazz.fields.get(token.code);
//                                    System.out.println(field);
                                    }else{
                                        //Get params
                                        LinkedList<TokenCodePair> variableEvaluation = new LinkedList<>();
                                        for (int j = index+1; j < bodySegment.size(); j++) {
                                            variableEvaluation.addLast(bodySegment.get(j));
                                        }

                                        Object[] object1 = evaluateParens(script, scope, variableEvaluation, 0);

                                        Class[] classes = new Class[object1.length];
                                        int classIndex = 0;
                                        for(Object functionParam : object1){
                                            if(functionParam.getClass().equals(Integer.class)){
                                                classes[classIndex] = Float.TYPE;
//                                            object1[classIndex] = (Float)object1[classIndex];
                                            }else if(functionParam.getClass().equals(Float.class)){
                                                classes[classIndex] = Float.TYPE;
//                                            object1[classIndex] = (Float)object1[classIndex];
                                            }else if(functionParam.getClass().equals(Double.class)){
                                                classes[classIndex] = Double.TYPE;
//                                            object1[classIndex] = (Float)object1[classIndex];
                                            }else {
                                                classes[classIndex] = functionParam.getClass();
                                            }
                                            classIndex++;
                                        }

                                        Method method = clazz.getMethod(token.getCode(), classes);
                                        //Invoke the method
//                                    System.out.println("Method:"+method);
//                                    System.out.println("Class:"+clazz.instance);
                                        try {
                                            method.invoke(value, object1);
                                        } catch (IllegalAccessException e) {
                                            e.printStackTrace();
                                        } catch (InvocationTargetException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                index++;
                                token = bodySegment.get(index);
                            }
                            break;
                        }
                        case SET_EQUAL:{
                            TokenCodePair nextToken = bodySegment.get(2);
                            Object newValue = value;
                            switch (nextToken.getToken()){
                                case CLASS_REFRENCE:{
                                    LinkedList<TokenCodePair> offsetLine = new LinkedList<>();
                                    for(int j = 2; j < bodySegment.size() ; j++){
                                        offsetLine.addLast(bodySegment.get(j));
                                    }
                                    newValue = resolveMethodFromClass(script, scope, offsetLine);
                                    break;
                                }
                                case VARIABLE_REFERENCE:{
                                    if(scope.containedInScope(nextToken.getCode())){
                                        newValue = scope.getVar(nextToken.getCode());
                                    }
                                }
                            }
                            scope.setVar(action.getCode(), newValue);
                            System.out.println(ConsoleColors.PURPLE+action.getCode()+":"+value+"->"+scope.getVar(action.getCode())+ConsoleColors.RESET);
                            break;
                        }
                    }

                    break;
                }
                case UNKNOWN:{
                    //TODO cache this resolution
                    TokenCodePair varName = bodySegment.get(0);
                    if(bodySegment.get(1).getToken().equals(EnumTokenType.SET_EQUAL)){
                        if (scope.containedInScope(varName.code)) {
                            TokenCodePair nextToken = bodySegment.get(2);
                            Object value    = scope.getVar(varName.code);
                            Object newValue = value;
                            switch (nextToken.getToken()){
                                case CLASS_REFRENCE:{
                                    LinkedList<TokenCodePair> offsetLine = new LinkedList<>();
                                    for(int j = 2; j < bodySegment.size() ; j++){
                                        offsetLine.addLast(bodySegment.get(j));
                                    }
                                    newValue = resolveMethodFromClass(script, scope, offsetLine);
                                    break;
                                }
                                case VARIABLE_REFERENCE:{
                                    if(scope.containedInScope(nextToken.getCode())){
                                        newValue = scope.getVar(nextToken.getCode());
                                    }
                                    break;
                                }
                                default:{
                                    LinkedList<TokenCodePair> variableEvaluation = new LinkedList<>();
                                    for (int j = 2; j < bodySegment.size(); j++) {
                                        variableEvaluation.addLast(bodySegment.get(j));
                                    }
                                    variableEvaluation.addFirst(new TokenCodePair(EnumTokenType.PAREN_OPEN, ""));
                                    //Wrap postop token..?
                                    if (variableEvaluation.getLast().getToken().equals(EnumTokenType.LINE_CLOSE)) {
                                        variableEvaluation.getLast().setType(EnumTokenType.PAREN_CLOSE);
                                        variableEvaluation.getLast().code = "";
                                    } else {
                                        variableEvaluation.addLast(new TokenCodePair(EnumTokenType.PAREN_CLOSE, ""));
                                    }

                                    newValue = evaluateParens(script, scope, variableEvaluation, 0)[0];
                                    break;
                                }
                            }
                            scope.setVar(action.getCode(), newValue);
                            System.out.println(ConsoleColors.PURPLE+action.getCode()+":"+value+"->"+scope.getVar(action.getCode())+ConsoleColors.RESET);
                        }
                    }else{
                        System.out.println(ConsoleColors.RED +"Unknown:"+bodySegment.get(1).getToken() + ConsoleColors.RESET);
                    }
                    break;
                }
                case IF:{
                    Object resultCheck = evaluateParens(script, scope, script.scope.getStatementSize(Integer.parseInt(action.code)), 0)[0];
                    boolean result = false;
                    if(resultCheck instanceof String){
                        result = Boolean.parseBoolean((String) resultCheck);
                    }else if(resultCheck instanceof Field){
                        Object cache = getPrimitiveField(((Field) resultCheck));
                        System.out.println("CacheCheck:"+cache);
                    }else{
                        result = (boolean) resultCheck;
                    }
                    if(result){
                        // @WHEN "If passed"
                        System.out.println("If passed");
                        evalTokens(script, scope, script.nonlinearBlocks.get(bodySegment.get(1).code));
                    }else{
                        // @WHEN "If Failed"
                        System.out.println("If failed");
                    }
                    break;
                }
            }
        }
    }

    public Script loadScript(String fileName){
        Script script = this.parse(StringUtils.load("\\scripts\\" + fileName));
        script.setName(fileName);
        loadedScripts.put(fileName, script);
        return script;
    }

    public boolean hasClassLoaded(String className){
        return registeredClasses.containsKey(className);
    }

    private Script parse(String file){
        String[] fileLines = file.split("\n");
        return parse(fileLines);
    }

    private Script parse(String[] file){
        Script definition = new Script();
        //File is one file, all variables in this scope are in relative to a single file.
        int index = 0;
        for(String line : file){
            file[index] = steriliseLine(definition, line);
//            System.out.println(file[index]);
            index++;
        }
        if(definition.getInComment()){
            System.out.println("Ended inside of comment, no closing comment character.");
        }

        for(String line : file){
            LinkedList<TokenCodePair> tokensInLine = getTokensFromLine(line);
            for(TokenCodePair tcp : tokensInLine){
                definition.getTokensInFile().addLast(tcp);
            }
        }
        //Look through file and encapsualte scopes

        //TODO at this point we need to scopify our file, this means changing our file from a linear array of all tokens, to a file contining lookups to other scopes.das

        //Parse those tokens looking for patterns.
        boolean rerun = true;
        loop : while(rerun) {
            rerun = false;
            int offsetTotal = 0;
            for (Pattern pattern : ld.getPatterns()) {
                int SHUTTLE_WIDTH = pattern.getLength();
                //Loop and find pattern
                for (int i = offsetTotal; i < definition.getTokensInFile().size() - (SHUTTLE_WIDTH - 1); i++) {
                    TokenCodePair[] shuttle = new TokenCodePair[SHUTTLE_WIDTH];
                    for (int j = 0; j < SHUTTLE_WIDTH; j++) {
                        int shuttleIndex = j;
                        shuttle[shuttleIndex] = definition.getTokensInFile().get(i + j);
                    }
                    int offset = pattern.tryMatch(definition, shuttle, i);
                    if (offset >= 0) {
                        i += offset;
                        offsetTotal += offset;
                        if(offsetTotal >= definition.getTokensInFile().size() - (SHUTTLE_WIDTH - 1)){
                            break loop;
                        }
                    } else {
                        rerun = true;
                    }
                }
            }
        }

//        for(TokenCodePair tokenCodePair : definition.getTokensInFile()){
//            System.out.print("["+tokenCodePair.token+"]"+tokenCodePair.code+"|");
//        }
//        System.out.println();

        return definition;
    }

    // Utility functions for parsing
    /*
    This function takes in a Language description and filters out all spaces that are not in comments
     */
    private String steriliseLine(Script definition, String line){
        int SHUTTLE_WIDTH = ld.SHORTEST_STRING_DECLARATION;

        //If we are in a string or not.
        for(int i = 0; i < line.length() - (SHUTTLE_WIDTH - 1); i++){
            String shuttle = line.substring(i, i+SHUTTLE_WIDTH);
            //Check if shuttle is == to a string character.
            if(StringUtils.isComment(shuttle, ld.STRING_START_CHARACTER)){
                definition.setInComment(!definition.getInComment());
                continue;
            }
            if(!definition.getInComment()) {
                //If we are not in a comment but hit a space character, remove it.
                if(shuttle.equals(ld.SPACE_CHARACTER)){
                    line = line.substring(0, i)+line.substring(i+SHUTTLE_WIDTH, line.length());
                    i -= SHUTTLE_WIDTH;
                }
            }
        }

        return line;
    }

    private LinkedList<TokenCodePair> getTokensFromLine(String line) {
        LinkedList<TokenCodePair> lineTokens = new LinkedList<>();

        if(line.startsWith(ld.COMMENT_SEQUENCE)){
            return lineTokens;
        }

        for(int i = 0; i < ld.tokens.values().size(); i++){
            EnumTokenType token = (EnumTokenType) ld.tokens.values().toArray()[i];
            String search = ld.tokens_prime.get(token);
            if(line.startsWith(search) || line.equals(search)){
                line = StringUtils.replaceFirst(line, search);
                lineTokens.addLast(new TokenCodePair(token, search));
                if(line.isEmpty()){
                    return lineTokens;
                }
                i = 0;
            }
        }

        if(!line.isEmpty()){
            String inLine = line;
            int nextTokenIndex = hasTokensLeft(line);
            if(nextTokenIndex >= 0){
                String undefined = line.substring(0, nextTokenIndex);
                line = StringUtils.replaceFirst(line, undefined);
                //Check for 4 things
                if(StringUtils.isSurroundedByANY(undefined, ld.STRING_START_CHARACTER)){
                    if(StringUtils.replaceAll(undefined, ld.STRING_START_CHARACTER, "").startsWith("#")){ //TODO move to ld?
                        lineTokens.addLast((new TokenCodePair(EnumTokenType.HEX_DATA, undefined)));
                    }else{
                        lineTokens.addLast((new TokenCodePair(EnumTokenType.STRING_DATA, undefined)));
                    }
                }else if(StringUtils.isNumber(undefined)){
                    if(undefined.contains(".")){ //TODO move to ld?
                        lineTokens.addLast((new TokenCodePair(EnumTokenType.FLOAT_DATA, undefined)));
                    }else{
                        lineTokens.addLast((new TokenCodePair(EnumTokenType.INT_DATA, undefined)));
                    }
                }else{
                    lineTokens.addLast((new TokenCodePair(EnumTokenType.UNKNOWN, undefined)));
                }

                //Recursive call
                LinkedList<TokenCodePair> moreTokens = getTokensFromLine(line);
                for(TokenCodePair tcp : moreTokens){
                    lineTokens.addLast(tcp);
                }

            }else {
                lineTokens.addLast((new TokenCodePair(EnumTokenType.UNKNOWN, line)));
            }
        }

        return lineTokens;
    }

    private int hasTokensLeft(String line){
        int out = Integer.MAX_VALUE;

        String sterileLine = StringUtils.removeStringsFromLine(line, "\"");

        for(int i = 0; i < ld.tokens.values().size(); i++){
            EnumTokenType token = (EnumTokenType) ld.tokens.values().toArray()[i];
            String search = ld.tokens_prime.get(token);
            if(sterileLine.contains(search)){
                int index = line.indexOf(search);
                if(index < out){
                    out = index;
                }
            }
        }

        if(out == Integer.MAX_VALUE){
            out = -1;
        }
        return out;
    }

    //This function takes in a token code pair, and resolves it
    //TODO scope resolution
    public static Object resolveToken(Script callingScript, Scope scope, LinkedList<TokenCodePair> line){
        TokenCodePair data = line.getFirst();
        switch (data.getToken()){
            case STRING_DATA:{
                if(data.code.length() <= 2){
                    System.out.println(ConsoleColors.BLUE + data.code + ConsoleColors.RESET);
                    return "";
                }
                return data.code.substring(1, data.code.length()-1);
            }
            case FLOAT_DATA:{
                return Float.parseFloat(data.code);
            }
            case INT_DATA:{
                return Integer.parseInt(data.code);
            }
            case BOOLEAN_DATA:{
                return Boolean.parseBoolean(data.code);
            }
            case VARIABLE_REFERENCE:{
                return callingScript.getVar(data.code);
            }
            case UNKNOWN:{
                if(scope.containedInScope(data.code)){
                    return scope.getVar(data.code);
                }
            }
            case CLASS_REFRENCE:{
                if(scope.containedInScope(data.code)){
                    return scope.getVar(data.code);
                }
                if(registeredClasses.containsKey(data.code)){
                    return resolveMethodFromClass(callingScript, scope, line);
                }
            }
            default:{
                System.out.println(ConsoleColors.RED + "Unhandled token type, so we are just returning the code of this token as its value:" + ConsoleColors.RESET + data.token + "|"+ data.getCode());
                return  data.code;
            }
        }
    }

    public static ScriptingEngine getInstance(){
        if(scriptingEngine == null){
            scriptingEngine = new ScriptingEngine();
        }
        return scriptingEngine;
    }

    private Object invokeMethod(ExpandedClass clazz, String methodName, Object invoker, Object[] params){

        try {
            String append = "";
            for(Object param : params){
                //Add null check
                if(param != null){
                    if(append.isEmpty()){
                        append += "(" + param.getClass().getName();
                    }else {
                        append += "," + param.getClass().getName();
                    }
                }
            }
            Class<?>[] classes = clazz.methods.get(methodName+append+")").getParameterTypes();
            int index = 0;
            for(Object param : params){
                if(!classes[index].equals(param.getClass())){
                    Class expected = classes[index];
                    Class actual   = param.getClass();
                    if(expected.equals(Integer.TYPE)){
                        if(actual.equals(Integer.class)){
                            params[index] = ((Integer)param).intValue();
                        }
                    }
//                    System.out.println(methodName+":"+expected+"=>"+actual+"=cast=>"+params[index].getClass());
                }
                index++;
            }
            Method method = clazz.methods.get(methodName+append+")");
            if(method == null){
                System.out.println(ConsoleColors.RED + "Method does not exist:" + methodName+append+")" + ConsoleColors.RESET);
            }
            return method.invoke(invoker, params);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object resolveMethodFromClass(Script script, Scope scope, LinkedList<TokenCodePair> bodySegment){
        TokenCodePair action = bodySegment.getFirst();

        ExpandedClass clazz = registeredClasses.get(action.code);
        boolean foundFunction = false;

        int index = 1;
        while(!foundFunction){

            if(bodySegment.size() == 1){
                Field field = clazz.fields.get(bodySegment.get(0));
                return field;
            }

            TokenCodePair thisToken = bodySegment.get(index);
            TokenCodePair nextToken = bodySegment.get(index+1);
            if(thisToken.getToken().equals(EnumTokenType.PROP_OF)){
                System.out.println(nextToken.getCode());
                if(clazz.fields.containsKey(nextToken.getCode())){
                    //Continue with clazz = typeof field
                    Field field = clazz.fields.get(nextToken.getCode());
                    if(!registeredClasses.containsKey(field.getName())) {
                        ScriptingEngine.getInstance().put(field.getName(), field.getType());
                    }
                    //If less than end of file, there is a possibility this could be a class chain, else do not change class because we want to return a field off this obj
                    if(index < bodySegment.size()-2) {
                        clazz = registeredClasses.get(field.getName());
                    }

                }else if(clazz.methodNames.containsKey(nextToken.getCode())){
//                    System.out.println("Method:"+nextToken.getCode());
                    Object[] params = ScriptingEngine.getInstance().evaluateParens(script, scope, ScriptingEngine.getInstance().getNextParens(bodySegment, index+2), 0);
                    if(ScriptingEngine.getInstance().staticClassInstances.containsKey(action.getCode())){
//                        System.out.println("Instance:"+staticClassInstances.get(action.getCode()));
                        return ScriptingEngine.getInstance().invokeMethod(clazz,nextToken.getCode(),ScriptingEngine.getInstance().staticClassInstances.get(action.getCode()),params);
                    }
                }
            }
            if(index == bodySegment.size()-2){
                Field field = clazz.fields.get(nextToken.getCode());
                return field;
            }
            index++;
        }
        return null;
    }

    public static LinkedList<TokenCodePair>[] bodyToActions(LinkedList<TokenCodePair> body){
        LinkedList<LinkedList<TokenCodePair>> outList = new LinkedList<LinkedList<TokenCodePair>>();
        LinkedList<TokenCodePair> runningTokens = new LinkedList<TokenCodePair>();
        int index = 0;
        for(TokenCodePair token : body){
            if(index >= 1) {
                if (ld.tokenIsa(token, ld.bodyDelimiters)) {
                    if (!runningTokens.isEmpty()) {
                        outList.addLast(runningTokens);
                        runningTokens = new LinkedList<>();
                    }
                } else {
                    runningTokens.addLast(token);
                }
            }
            index++;
        }

        LinkedList<TokenCodePair>[] out = new LinkedList[outList.size()];
        index = 0;
        for(LinkedList<TokenCodePair> block : outList){
            out[index] = block;
            index++;
        }

        return out;
    }

    public LinkedList<TokenCodePair> getNextParens(LinkedList<TokenCodePair> bodySegment, int start){
        int parenIndex = 0;

        LinkedList<TokenCodePair> outList = new LinkedList<>();

        for(int i = start; i < bodySegment.size(); i++){
            TokenCodePair indexToken = bodySegment.get(i);
            outList.addLast(indexToken);
            switch (indexToken.getToken()){
                case PAREN_OPEN:{
                    parenIndex++;
                    break;
                }
                case PAREN_CLOSE:{
                    parenIndex--;
                    break;
                }
            }
            if(parenIndex == 0){
                return outList;
            }
        }

        return outList;
    }

    public LinkedList<TokenCodePair> getScope(LinkedList<TokenCodePair> bodySegment, int start){
        int scopeIndex = 0;

        LinkedList<TokenCodePair> outList = new LinkedList<>();

        for(int i = start; i < bodySegment.size(); i++){
            TokenCodePair indexToken = bodySegment.get(i);
            outList.addLast(indexToken);
            switch (indexToken.getToken()){
                case SCOPE_OPEN:{
                    scopeIndex++;
                    break;
                }
                case SCOPE_CLOSE:{
                    scopeIndex--;
                    break;
                }
            }
            if(scopeIndex == 0){
                return outList;
            }
        }

        return outList;
    }

    public Object[] evaluateParens(Script script, Scope scope, LinkedList<TokenCodePair> bodySegment, int start){

        //Base case
        if((bodySegment.size() - start) == 1){
//            LinkedList<TokenCodePair> futureTokens = new LinkedList<>();
//            for(int j = start+1; j < bodySegment.size(); j++){
//                futureTokens.addLast(bodySegment.get(j));
//            }
            return new Object[]{resolveToken(script, scope,new LinkedList<TokenCodePair>(bodySegment.subList(start, bodySegment.size())))};
        }

        Object[] out;

        int totalParams = 0;
        int parenScopes = 0;

        //Build out the body.
        LinkedList<TokenCodePair> param = new LinkedList<>();
        LinkedList<LinkedList<TokenCodePair>> params = new LinkedList<>();

        //TODO cache this somehow
        for(int i = start; i < bodySegment.size(); i++){
            TokenCodePair token = bodySegment.get(i);
            //Check first token is an open
            if(i == start){
                //Offset start by 1;
                int opIndex = ld.tokenIsInOuterParens(bodySegment, ld.evaluationCharacters, start + 1);
                if (opIndex >= 0) {
                    EnumTokenType opToken = bodySegment.get(opIndex).getToken();
                    LinkedList<TokenCodePair> preOppTokens = new LinkedList<>();
                    if(start + 1 < opIndex) {
                        for (int j = start + 1; j < opIndex; j++) {
                            preOppTokens.addLast(bodySegment.get(j));
                        }
                    }else{
                        preOppTokens.push(bodySegment.get(start));
                    }
                    LinkedList<TokenCodePair> postOppTokens = new LinkedList<>();

                    //NOTE may break
                    if (opIndex+1 < bodySegment.size()){
                        for (int j = opIndex + 1; j < bodySegment.size(); j++) {
                            postOppTokens.addLast(bodySegment.get(j));
                        }
                    }else {
                        System.out.println("This is a one token case");
                        postOppTokens.push(bodySegment.get(opIndex+1));
                    }

                    //Wrap postop token..?
                    if(postOppTokens.get(0).getToken().equals(EnumTokenType.PAREN_OPEN)){
                        postOppTokens.addFirst(new TokenCodePair(EnumTokenType.PAREN_OPEN, ""));
                        postOppTokens.addLast(new TokenCodePair(EnumTokenType.PAREN_CLOSE, ""));
                    }

                    Object object1 = evaluateParens(script, scope, preOppTokens , 0)[0];
                    Object object2 = evaluateParens(script, scope, postOppTokens, 0)[0];

                    return new Object[]{ld.performOpp(opToken, object1, object2)};

                }else{
//                    System.out.println("No OP");
                }
            }
            switch (token.getToken()){
                case PAREN_CLOSE:{
                    parenScopes--;
                    break;
                }
                case PAREN_OPEN:{
                    parenScopes++;
                    break;
                }
                case PARAM_SEPARATION:{
                    params.addLast(param);
                    param = new LinkedList<>();
                    break;
                }
                default:{
                    param.addLast(token);
                }
            }
            if(parenScopes == 0){
                if(param.size() > 0){
                    //We have 1 param
                    if(param.size() == 1 && params.size() == 0){
                        return new Object[]{resolveToken(script, scope, new LinkedList<TokenCodePair>(bodySegment.subList(0, bodySegment.size())))};
                    }else{
                        params.addLast(param);
                    }
                }
                break;
            }
        }

        LinkedList<Object> allObjects = new LinkedList<>();

        for(LinkedList<TokenCodePair> paramIndex : params){
            for(Object object: evaluateParens(script, scope, paramIndex, 0)){
                allObjects.addLast(object);
            }
        }
        //        System.out.println(allObjects);

        //Init out array
        out = new Object[allObjects.size()];
        int index = 0;
            for(Object object: allObjects){
            out[index] = object;
            index++;
        }

    //Return out
        return out;
    }

    public static Object getPrimitiveField(Field field){
        Class<?> t = field.getType();
        try {
            if (t == int.class) {
                return field.getInt(null);
            } else if (t == double.class) {
                return field.getDouble(null);
            } else if (t == float.class) {
                return field.getFloat(null);
            } else if (t == Boolean.class || t == Boolean.TYPE) {
                return field.getBoolean(Boolean.TRUE);
            } else if (t == byte.class) {
                return field.getByte(null);
            } else if (t == char.class) {
                return field.getChar(null);
            } else if (t == long.class) {
                return field.getLong(null);
            } else if (t == short.class) {
                return field.getShort(null);
            }

            //If we get to this point we did not anticipate this class type
            System.out.println("No else case for this class: " + t);
            return null;

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onShutdown() {
        //GC all files, and call shutdown methods of scripts.
    }
}
