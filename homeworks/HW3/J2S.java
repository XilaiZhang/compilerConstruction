
import cs132.minijava.MiniJavaParser;
import cs132.minijava.syntaxtree.*; 
import cs132.minijava.ParseException;

public class J2S {
	
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
			System.err.println("first pass was okay");
			typeVisitor varVisitor = new typeVisitor();
			SparrowStruct sparrowFile= root.accept(varVisitor, symbolTable);

			System.err.println("");
			System.err.println("Spitting sparrow file:");
			for(String x:sparrowFile.first){
				//System.err.println("what I have in my sparrow code:");
				System.err.println(x);
				System.out.println(x);
			}
			System.err.println("to debug");
			System.out.println("to debug");
		}
		catch (Exception e) {
			System.err.println("error message from e: "+e.toString());
			System.out.println("Type error");
			System.exit(1);
		}
		
	}
}