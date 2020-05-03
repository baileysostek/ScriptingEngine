package Scripting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ExpandedClass {

    String name;
    HashMap<String, Method>  methods     = new HashMap<>();
    HashMap<String, Integer> methodNames = new HashMap<>();
    HashMap<String, Field>   fields      = new HashMap<>();
    public Class instance;

    public ExpandedClass(Class className){
        Class cls = className;
        this.name = cls.getName();
        instance = cls;
//        System.out.println(reference);

        Method[] methods = cls.getMethods();

        // Printing method names
        for (Method method:methods){
            String append = "";
            for(Class clazz : method.getParameterTypes()){
                if (clazz.isPrimitive()){
                    if(clazz.equals(int.class)){
                        clazz = Integer.class;
                    }
                }
                if(append.isEmpty()){
                    append += "(" + clazz.getName();
                }else {
                    append += "," + clazz.getName();
                }
            }
            System.out.println(ConsoleColors.BLUE+method.getName()+append+ConsoleColors.RESET);
            this.methods.put(method.getName()+append+")", method);
            if(methodNames.containsKey(method.getName())){
                methodNames.put(method.getName(), methodNames.get(method.getName())+1);
            }else{
                methodNames.put(method.getName(), 1);
            }
        }

        Field[] fields = cls.getFields();
        for (Field field : fields) {
            System.out.println(ConsoleColors.BLUE+field.getName()+ConsoleColors.RESET);
            this.fields.put(field.getName(), field);
        }
    }

    public Method getMethod(String name, Class<?> ... params){
        try {
            String append = "";
            for(Class clazz : params){
                if(append.isEmpty()){
                    append += "(" + clazz.getName();
                }else {
                    append += "," + clazz.getName();
                }
            }
            if(methods.containsKey(name+append+")")){
                //TODO
                return instance.getDeclaredMethod(name+append+")", params);
            }
        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
            for(int i = 0; i < params.length; i++){
                params[i] = Object.class;
            }
            String append = "";
            for(Class clazz : params){
                if(append.isEmpty()){
                    append += "(" + clazz.getName();
                }else {
                    append += "," + clazz.getName();
                }
            }
            try {
                //Try as objects
                return instance.getDeclaredMethod(name+append+")", params);
            } catch (NoSuchMethodException ex) {
//                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public Object getInstance(){
        return System.out;
    }
}
