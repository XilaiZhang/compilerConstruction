import java.util.*;

public class classStruct {
	
	private String className;
	private String parent=null; 
	private Map<String, typeStruct> fields = new HashMap<String, typeStruct>();
	private Map<String, methodStruct> methods = new HashMap<String, methodStruct>();
	private boolean visitingMethod=false;
	private String currentMethod;
	
	public classStruct(String name)
	{
		className = name;
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
