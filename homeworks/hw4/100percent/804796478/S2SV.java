
import cs132.IR.SparrowParser;
import cs132.IR.syntaxtree.*; 
import cs132.IR.ParseException;

public class S2SV {
	
	public static void main(String[] args) {
		
		Program root=null;
		try {
			root = new SparrowParser(System.in).Program();
			
		} catch (ParseException pError) {
			System.err.println("sparrow parser throws parse exception " + pError.toString());
			System.exit(1);
		}
		
		try {
			aVisitor tableVisitor = new aVisitor();
			functionStruct what= new functionStruct();
			root.accept(tableVisitor, what);
			System.err.println("first pass was okay");
			System.err.println("");

			positionVisitor pv= new positionVisitor();
			pv.aTable = tableVisitor.aTable;
			functionStruct fucker= new functionStruct();
			root.accept(pv, fucker);
			System.err.println("second pass was okay");
			System.err.println("");

			IRVisitor cv = new IRVisitor();
			cv.IRTable = pv.pTable;
			cv.aTable = pv.aTable;
			cv.registerTable = pv.registerTable;
			cv.variableTable = pv.variableTable;
			cv.linearTable = pv.linearTable;
			cv.rangeTable = pv.rangeTable;
			cv.aRangeTable = pv.aRangeTable;
			fucker= new functionStruct();
			IRStruct IRFile = root.accept(cv, fucker); //null could be some argements

			System.err.println("");
			System.err.println("Spitting sparrowV file:");
			for(String x:IRFile.first){
				//System.err.println("what I have in my sparrow code:");
				System.err.println(x);
				System.out.println(x);
			}
		}
		catch (Exception e) {
			System.err.println("error message from e: "+e.toString());
			System.out.println("Type error");
			System.exit(1);
		}
		
	}
}