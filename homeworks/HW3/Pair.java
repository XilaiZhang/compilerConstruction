public class Pair{
    private String a;
    private typeStruct t;

    public Pair(String x, typeStruct y){
        a=x;
        t=y;
    }

    public String first(){
        return a;
    }

    public typeStruct second(){
        return t;
    }
}