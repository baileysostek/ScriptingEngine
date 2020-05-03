package Scripting;

public class TokenCodePair {

    EnumTokenType token;
    String code;

    public TokenCodePair(EnumTokenType token, String code){
        this.token = token;
        this.code  = code;
    }

    public EnumTokenType getToken(){
        return this.token;
    }

    public String getCode(){
        return this.code;
    }

    public void setType(EnumTokenType type){
        this.token = type;
    }
}
