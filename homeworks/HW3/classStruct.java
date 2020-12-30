import java.util.*;

public class classStruct {
	
	public String className;
	public String parent=null; 
	public Map<String, typeStruct> fields = new HashMap<String, typeStruct>();
	public Map<String, methodStruct> methods = new HashMap<String, methodStruct>();
	private boolean visitingMethod=false;
	private String currentMethod;

	public HashSet<String> parentMethods = new HashSet<String> ();
	public int methodIndex=0;
	public Map<String, Integer> methodPos = new HashMap<String, Integer> ();
	public int fieldIndex=1;
	public Map<String, Integer> fieldPos = new HashMap<String, Integer> ();
	public String classVariable="";

	public Map<String, String> fieldsSparrow =  new HashMap<String, String>();
	
	public classStruct(String name)
	{
		className = name;
	}

	public void markMethod(String methodName){
		methodPos.put(methodName,methodIndex);
		methodIndex=methodIndex+1;
	}

	public void markField(String fieldName){
		fieldPos.put(fieldName,fieldIndex);
		fieldIndex=fieldIndex+1;
	}
	
	public String getClassName()
	{
		return className;
	}
	
	public void setFields(Map<String, typeStruct> fieldsMap)
	{
		fields = fieldsMap;
	}

	public Map<String, typeStruct> getFields()
	{
		return fields;
	}
	
	public void setMethods(Map<String, methodStruct> methodsMap) {
		methods = methodsMap;
	}

	public Map<String, methodStruct> getMethods() {
		return methods;
	}
	
	public String getParent() {
		return parent;
	}

	public boolean setParent(String father) {
		if(parent==null){
			parent = father;
			return true;
		}
		else{ return false;}
		
	}
	
	public boolean getVisitingMethod()
	{
		return visitingMethod;
	}
	
	public void setVisitingMethod(boolean b)
	{
		visitingMethod = b;
	}
	
	public String getCurrentMethod()
	{
		return currentMethod;
	}
	
	public void setCurrentMethod(String methodName)
	{
		currentMethod = methodName;
	}
	
	public void printClass()
	{
		System.out.println("in class " + className+" : ");
		System.out.print("fields entries are : ");
		for (String fieldName: fields.keySet())
			System.out.print(fieldName + " " + fields.get(fieldName) );
		System.out.println("parent of class is: " + parent);
		for (methodStruct method: methods.values()){
			method.printMethod();
		}
	}

}
