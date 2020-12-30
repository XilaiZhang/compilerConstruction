import java.util.HashMap;
import java.util.Map;


public class SymbolTable {
	
	private Map<String, classStruct> classTable;
	public SymbolTable(Map<String, classStruct> table)
	{
		classTable = table;
	}
	
	public classStruct getClass(String className)
	{
		return classTable.get(className);
	}
	
	public boolean hasClass(String className)
	{
		return classTable.containsKey(className);
	}
	
	public boolean checkSubType(typeStruct lhs, typeStruct rhs)
	{
		if (! (lhs.classType && rhs.classType ))
			return false;
		
		String left = lhs.type;
		String right = rhs.type;
		while (right != null)
		{
			if (left.equals(right)){
				return true;
			}
			right = this.getClass(right).getParent();
		}
		return false;
	}
}
