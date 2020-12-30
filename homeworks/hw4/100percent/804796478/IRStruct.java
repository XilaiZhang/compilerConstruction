import java.util.*;

public class IRStruct{
    public ArrayList<String> first;
    public String second;
    public ArrayList<ArrayList<String>> optionalList;
    public ArrayList<String> optionalResult;

    public IRStruct (){
        first=new ArrayList<String>();
        second="";
        optionalList = new ArrayList<ArrayList<String>>();
        optionalResult = new ArrayList<String>();
    }

}