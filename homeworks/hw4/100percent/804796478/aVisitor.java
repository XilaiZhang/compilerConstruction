import cs132.IR.syntaxtree.*;
import cs132.IR.visitor.GJVoidDepthFirst;
import java.util.*;
import java.lang.*;

public class aVisitor extends GJVoidDepthFirst<functionStruct> {

    public HashMap<String, HashMap<String, String>> aTable = new HashMap<String, HashMap<String, String>>();
    /**
    f0 -> "func" 
    f1 -> FunctionName() 
    f2 -> "(" 
    f3 -> ( Identifier() )* 
    f4 -> ")" 
    f5 -> Block()
        NodeToken f0 
        FunctionName f1 :
                NodeToken   f0 
        NodeToken f2 
        NodeListOptional f3 
        NodeToken f4 
        Block f5
        */
    @Override
    public void visit(FunctionDeclaration n, functionStruct fStruct) {
        String myName = n.f1.f0.toString();

        HashMap<String, String> aPos= new HashMap<String, String>();
        aTable.put(myName, aPos);

        int seq=2;
        if ( n.f3.present() ) {
            Enumeration <Node> nodeListEnum=n.f3.elements();
            while(nodeListEnum.hasMoreElements() ){
                //System.err.println("ready to print singleParam");
                String singleParameter= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                //System.err.println("printed single Param");
                String aReg= "a"+Integer.toString(seq);
                if(seq <= 7){
                   aTable.get(myName).put(singleParameter, aReg); 
                }
                seq=seq + 1;
            
            }
            //System.err.println("set up a Pos");
        }
    }
}