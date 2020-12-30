
import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.*; 
import cs132.minijava.ParseException;

public class Typecheck {
	
	public static void main(String[] args) {
		
		Goal root=null;
		try {
			root = new MiniJavaParser(System.in).Goal();
			
		} catch (ParseException pError) {
			System.err.println("miniJava throws parse exception " + pError.toString());
			System.exit(1);
		}
		
		try {
			classVisitor cv = new classVisitor();
			root.accept(cv, null);
			cv.processExtendedClass();
			
			SymbolTable symbolTable = new SymbolTable(cv.getClassTable());
			/*
			if(symbolTable.hasClass("Element")){
				methodStruct m=symbolTable.getClass("Element").getMethods().get("Init");
				System.err.print("In symbol Table, local variables are : ");
				for (String variableName: m.getLocalVariables().keySet()){
					System.err.print(variableName + " " + m.getLocalVariables().get(variableName).type);
				}
				System.err.print("finished printing symbolTable ");
			}*/
			typeVisitor varVisitor = new typeVisitor();
			root.accept(varVisitor, symbolTable);
		}
		catch (Exception e) {
			System.err.println("error message from e: "+e.toString());
			System.out.println("Type error");
			System.exit(1);
		}
		System.out.println("Program type checked successfully");
	}
}