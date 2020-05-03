package Scripting;

import util.Callback;

import java.util.LinkedList;

public class Pattern {
    public EnumTokenType[] pattern;
    Callback onMatch;

    public Pattern(LinkedList<EnumTokenType> pattern, Callback onMatch){
        this.pattern = new EnumTokenType[pattern.size()];
        int index = 0;
        for(EnumTokenType token : pattern){
            this.pattern[index] = token;
            index++;
        }
        this.onMatch = onMatch;
    }

    public int tryMatch(Script definition, TokenCodePair[] tokens, int tokenIndex){
        if(tokens.length == getLength()){
            int index = 0;
            loop:{
                for (TokenCodePair token : tokens) {
                    if(!pattern[index].equals(EnumTokenType.WILDCARD)){
//                        System.out.println("Token:"+token.getToken());
//                        System.out.println("pattern:"+pattern[index]);
                        if (!(token.getToken().equals( pattern[index]))) {
                            break loop;
                        }
                    }
                    index++;
                }
                //If we get here we have a match
                System.out.println(onMatch+"|"+definition+"|"+tokens+"|"+tokenIndex);
                return (int) onMatch.callback(definition, tokens, tokenIndex);
            }
        }
        return 0;
    }

    public int getLength(){
        return this.pattern.length;
    }
}
