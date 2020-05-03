package Scripting;

public enum EnumTokenType {
    //Class/structures
    NEW(),
    FREE(),
    CLASS_FIELD(),
    CLASS_METHOD(),

    //Logic
    IF(),
    FOR(),
    ADD(),
    SUB(),
    MUL(),
    DIV(),
    GREATER(),
    LESS(),
    GREATER_EQUAL(),
    LESS_EQUAL(),
    COMPARE_EQUAL(),
    NOT(),

    //Basic functions
    IDENTITY(),
    SCOPE_OPEN(),
    SCOPE_CLOSE(),
    PAREN_OPEN(),
    PAREN_CLOSE(),

    //Arrays
    EMPTY_ARRAY(),
    ARRAY_START(),
    ARRAY_END(),

    //Syntax stuff
    SET_EQUAL(),
    LINE_CLOSE(),

    //Link Script to Object
    VARIABLE_DECLARATION(),
    CLASS_DECLARATION(),
    CLASS_REFRENCE(),

    //Variabels
    VARIABLE_NAME(),
    PROP_OF(),
    PARAM_SEPARATION(),
    VARIABLE_REFERENCE(),

    //Structure data
    STRUCT_NAME(),

    //Data
    STRING_DATA(),
    INT_DATA(),
    FLOAT_DATA(),
    HEX_DATA(),
    BOOLEAN_DATA(),
    OBJECT_DATA(),

    //Function
    FUNCTION(),         //Instance of function keyword
    CALL_TO_FUNCTION(), //Call to function name
    FUNCTION_NAME(),    //Name of a function

    //IMPORTS
    IMPORT(),
    IMPORT_LOOKUP(),

    //WILDCARD
    WILDCARD(),

    //PlaceHolder
    UNKNOWN();
}
