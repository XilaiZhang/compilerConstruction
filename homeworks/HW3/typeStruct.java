
public class typeStruct {
	
	public String type;
	public boolean classType=false;
	
	public typeStruct(String typeName) 
	{
		type = typeName;
	}
	
	public typeStruct(String typeName, int setClass)
	{
		type = typeName;
		classType = true;
	}
	
	public String getType()
	{
		return type;
	}
	
	public boolean integerType()
	{
		return (!classType) && type.equals("int");
	}
	
	public boolean booleanType()
	{
		return (!classType) && type.equals("boolean");
	}
	
	public boolean arrayType()
	{
		return (!classType) && type.equals("array");
	}

}
