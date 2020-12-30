import java.util.*;


public class methodStruct {
	
	private String methodName;
	private typeStruct returnType;
	private List<Pair> parameters = new ArrayList<Pair>();
	private Map<String, typeStruct> localVariables = new HashMap<String, typeStruct>();

	
	public methodStruct(String name, typeStruct type)
	{
		methodName = name;
		returnType = type;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String name) {
		methodName = name;
	}

	public typeStruct getReturnType() {
		return returnType;
	}

	public void setReturnType(typeStruct type) {
		returnType = type;
	}

	public Map<String, typeStruct> getLocalVariables() {
		return localVariables;
	}

	public void setLocalVariables(Map<String, typeStruct> localVariablesMap) {
		localVariables = localVariablesMap;
	}

	public List<Pair> getParameters() {
		return parameters;
	}

	public void setParameters(List<Pair> parametersList) {
		parameters = parametersList;
	}
	
	public boolean hasParameter(String id)
	{
		for (Pair param: parameters){
			if (param.first().equals(id)){
				return true;
			}
		}	
		return false;
	}
	
	public void printMethod()
	{
		System.out.println("methodName: " + methodName);
		System.out.println("Return type: " + returnType.type);
		System.out.print("method parameters are : ");
		for (Pair param: parameters)
			System.out.print(param.second().type + " " + param.first() + " ");
		System.out.println();
		System.out.print("local variables are : ");
		for (String variableName: localVariables.keySet()){
			System.out.print(variableName + " " + localVariables.get(variableName).type);
		}
		System.out.println();
	}
}
