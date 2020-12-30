import java.util.*;

public class SparrowStruct{
    public ArrayList<String> first;
    public String second; // the variable name that stores the result
    public String rawClassName;
    public ArrayList<ArrayList<String>> optionalList;
    public ArrayList<String> optionalResult;
    public boolean isField=false;

    public SparrowStruct (){
        first=new ArrayList<String>();
        second="";
        rawClassName="";
        optionalList = new ArrayList<ArrayList<String>>();
        optionalResult = new ArrayList<String>();
        isField=false;
    }

    public SparrowStruct (ArrayList<String> x, String y){
        first=x;
        second=y;
    }

}