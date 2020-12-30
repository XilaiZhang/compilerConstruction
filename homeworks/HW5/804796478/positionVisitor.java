import cs132.IR.syntaxtree.*;
import cs132.IR.visitor.GJVoidDepthFirst;
import java.util.*;

//NOTE: do need argument size of function itself for storing purposes

public class positionVisitor extends GJVoidDepthFirst<functionStruct> {
    
    public HashMap<String, HashMap<String,Integer>> pTable; //record the last rhs apprearance
    public HashMap<String, HashMap<String, String>> aTable;
    public HashMap<String, HashMap<String, String>> stackTable= new HashMap<String, HashMap<String, String>>();
    public HashMap<String, HashMap<String, String>> variableTable= new HashMap<String, HashMap<String, String>>();
    public HashMap<String, Integer> numArgument= new HashMap<String, Integer>();
    public HashMap<String, HashSet<String>> numIds= new HashMap<String, HashSet<String>>();
    public HashMap<String, HashMap<String, String>> labelTable= new HashMap<String, HashMap<String, String>>();

    public HashSet<String> regList= new HashSet<String>();
    public int lineCount;
    public int labelIndex = 1;
    public positionVisitor (){
        aTable= new HashMap<String, HashMap<String, String>>();
        pTable= new HashMap<String, HashMap<String,Integer>>();
        lineCount= 1; //TODO: reset lineCount after exiting function
        List<String> aList= Arrays.asList( "a2","a3","a4","a5","a6","a7", //these are function arguments
            "s1","s2","s3","s4","s5","s6","s7","s8","s9","s10","s11",
            "t0","t1","t2","t3","t4","t5"); //save 9,10,11
        for(String s:aList){regList.add(s);}
    }

    public String getTemp(){
        String r= "v"+Integer.toString(labelIndex);
        labelIndex += 1;
        return r;
    }

    public void addEntry(String id, String functionName, int lineNumber){
        if(!pTable.containsKey(functionName)){
            HashMap myMap= new HashMap<String, Integer>();
            pTable.put(functionName, myMap);
        }
        pTable.get(functionName).put(id, lineNumber);
    }

    public void addId(String id, String functionName){
        if(!numIds.containsKey(functionName)){
            HashSet myMap= new HashSet<String>();
            numIds.put(functionName, myMap);
        }
        numIds.get(functionName).add(id);
    }

    /**
        f0 -> ( FunctionDeclaration() )* f1 ->
        Program(NodeListOptional n0, NodeToken n1)
        */

    @Override
    public void visit(Program n, functionStruct fStruct) {
        n.f0.accept(this, fStruct);
    
    }

    @Override
    public void visit(NodeListOptional n, functionStruct fStruct) {

        //System.err.println("entering nodelistoptional");
        if ( n.present() ) {
            Enumeration <Node> nodeListEnum=n.elements();
            while(nodeListEnum.hasMoreElements() ){
                nodeListEnum.nextElement().accept(this, fStruct);
                
            }
            
        }
        //System.err.println("leaving nodeListOptional");
    }

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
        fStruct.functionName = myName;
        //System.err.println("entering function "+myName);
        HashMap<String, String> aPos= new HashMap<String, String>();
        //System.err.println("trying to add entry into aTable ");
        aTable.put(myName, aPos);
        HashMap<String, String> rt= new HashMap<String, String>();
        HashMap<String, String> vt= new HashMap<String, String>();
        variableTable.put(myName, vt);
        stackTable.put(myName, rt);
        HashMap<String, String> labelPos= new HashMap<String, String>();
        labelTable.put(myName, labelPos);
        //System.err.println("putted entry into aTable ");

        int numArg = 6;
        int posOffset = 28;
        if ( n.f3.present() ) {
            Enumeration <Node> nodeListEnum=n.f3.elements();
            while(nodeListEnum.hasMoreElements() ){
                //System.err.println("ready to print singleParam");
                String singleParameter= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                //System.err.println("printed single Param");
                numArg = numArg + 1;
            
            }
            //System.err.println("set up a Pos");
        }
        numArgument.put(myName, numArg);
        //n.f3.accept(this, fStruct); //we don't care about function parameters
        n.f5.accept(this, fStruct);
        lineCount=1;
    
    }

    /**
    NodeToken   f0 
        */
    @Override
    public void visit(Identifier n, functionStruct fStruct) {
        String id = n.f0.toString();
        addEntry(id, fStruct.functionName, fStruct.lineNumber);
        if(!regList.contains(id)){
            addId(id, fStruct.functionName);
        }
    }

    /**
    Grammar production: f0 -> ( Instruction() )* f1 -> "return" f2 -> Identifier()
    NodeListOptional f0 
    NodeToken f1 
    Identifier f2 
        */
    @Override
    public void visit(Block n, functionStruct fStruct) {
        n.f0.accept(this, fStruct);

        String returnObject = n.f2.f0.toString(); 
        if(!regList.contains(returnObject)){
            addId(returnObject, fStruct.functionName);
        }
    }

    /**
    Grammar production: f0 -> LabelWithColon() | SetInteger() | SetFuncName() | 
    Add() | Subtract() | Multiply() | LessThan() | Load() | Store() | Move() | 
    Alloc() | Print() | ErrorMessage() | Goto() | IfGoto() | Call()

    NodeChoice  f0
        node choide
        int which 
        */
    @Override
    public void visit(Instruction n, functionStruct fStruct) {
        
        fStruct.lineNumber= lineCount;
        lineCount+=1;
        n.f0.choice.accept(this, fStruct); //nodechoice.f0 
    
    }

    /**
    Grammar production: f0 -> Label() f1 -> ":"
    Label   f0 
    NodeToken   f1
        */
    @Override
    public void visit(LabelWithColon n, functionStruct fStruct) {
        String myName = fStruct.functionName;
        String labelName = n.f0.f0.toString();
        if(!labelTable.get(myName).containsKey(labelName)){
            String mappedLabel= getTemp();
            labelTable.get(myName).put(labelName, mappedLabel);
        }
    }

    /**
    Grammar production: f0 ->
    NodeToken   f0
        */
    @Override
    public void visit(Label n, functionStruct fStruct) {
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> IntegerLiteral()
    Identifier  f0 
    NodeToken   f1 
    IntegerLiteral  f2
        */
    @Override
    public void visit(SetInteger n, functionStruct fStruct) {
        //System.err.println("entering and set integer");
        //String id = n.f0.f0.toString();
        //addEntry(id, fStruct.functionName, fStruct.lineNumber);
        //only takes care of rhs
    }

    /**
    Grammar production: f0 ->
    NodeToken   f0 
        */
    @Override
    public void visit(IntegerLiteral n, functionStruct fStruct) {
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> "@" f3 -> FunctionName() 
    Identifier  f0 
        NodeToken   f0
    NodeToken   f1 
    NodeToken   f2 
    FunctionName    f3
        NodeToken   f0 
        */
    @Override
    public void visit(SetFuncName n, functionStruct fStruct) {
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> Identifier() f3 -> "+" f4 -> Identifier()
    Identifier  f0
        NodeToken   f0  
    NodeToken   f1 
    Identifier  f2 
    NodeToken   f3 
    Identifier  f4 
        */
    @Override
    public void visit(Add n, functionStruct fStruct) {
        //String resultIntObject = n.f0.f0.toString();
        String firstIntObject = n.f2.f0.toString();
        addEntry(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addEntry(secondIntObject, fStruct.functionName, fStruct.lineNumber);      
    
    }

    /**
    f0 -> Identifier() f1 -> "=" f2 -> Identifier() f3 -> "-" f4 -> Identifier()
    Identifier  f0
        NodeToken   f0  
    NodeToken   f1 
    Identifier  f2 
    NodeToken   f3 
    Identifier  f4 
        */
    @Override
    public void visit(Subtract n, functionStruct fStruct) {
        //String resultIntObject = n.f0.f0.toString();
        //System.err.println("computing subtraction");
        String firstIntObject = n.f2.f0.toString();
        addEntry(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addEntry(secondIntObject, fStruct.functionName, fStruct.lineNumber);
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> Identifier() f3 -> "*" f4 -> Identifier()
    Identifier  f0
        NodeToken   f0  
    NodeToken   f1 
    Identifier  f2 
    NodeToken   f3 
    Identifier  f4 
        */
    @Override
    public void visit(Multiply n, functionStruct fStruct) {
        //String resultIntObject = n.f0.f0.toString();
        String firstIntObject = n.f2.f0.toString();
        addEntry(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addEntry(secondIntObject, fStruct.functionName, fStruct.lineNumber);
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> Identifier() f3 -> "<" f4 -> Identifier()
    Identifier  f0
        NodeToken   f0  
    NodeToken   f1 
    Identifier  f2 
    NodeToken   f3 
    Identifier  f4 
        */
    @Override
    public void visit(LessThan n, functionStruct fStruct) {
        //String resultIntObject = n.f0.f0.toString();
        //System.err.println("computing less than ");
        String firstIntObject = n.f2.f0.toString();
        addEntry(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addEntry(secondIntObject, fStruct.functionName, fStruct.lineNumber);
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> "[" 
    f3 -> Identifier() f4 -> "+" f5 -> IntegerLiteral() f6 -> "]"
         
    Identifier  f0 
        NodeToken   f0 
    NodeToken   f1 
    NodeToken   f2 
    Identifier  f3 
    NodeToken   f4 
    IntegerLiteral  f5 
        NodeToken   f0 
    NodeToken   f6 
        */
    @Override
    public void visit(Load n, functionStruct fStruct) {
        //System.err.println("loading");
        //String loadedObject = n.f0.f0.toString();
        String baseObject = n.f3.f0.toString();
        String offsetObject = n.f5.f0.toString();
        String concatObject = baseObject+"+"+offsetObject;

        addEntry(concatObject, fStruct.functionName, fStruct.lineNumber);
        
    }

    /**
    Grammar production: f0 -> "[" f1 -> Identifier() f2 -> "+" f3 -> IntegerLiteral() 
    f4 -> "]" f5 -> "=" f6 -> Identifier()
         
    NodeToken   f0 
    Identifier  f1 
    NodeToken   f2 
    IntegerLiteral  f3 
    NodeToken   f4 
    NodeToken   f5 
    Identifier  f6  
        */
    @Override
    public void visit(Store n, functionStruct fStruct) {
        //System.err.println("storing");
        //String baseObject = n.f1.f0.toString();
        //String offsetObject = n.f3.f0.toString();
        String storedObject = n.f6.f0.toString();
        addEntry(storedObject, fStruct.functionName, fStruct.lineNumber);
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> Identifier()
         
    Identifier  f0 
    NodeToken   f1 
    Identifier  f2  
        */
    @Override
    public void visit(Move n, functionStruct fStruct) {
        
        String rightObject = n.f2.f0.toString();
        addEntry(rightObject, fStruct.functionName, fStruct.lineNumber);

        String leftObject = n.f0.f0.toString();
        if(!regList.contains(rightObject)){
            addId(rightObject, fStruct.functionName);
        }
        if(!regList.contains(leftObject)){
            addId(leftObject, fStruct.functionName);
        }
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> "alloc" f3 -> "(" f4 -> Identifier() f5 -> ")"
         
    Identifier  f0 
    NodeToken   f1 
    NodeToken   f2 
    NodeToken   f3 
    Identifier  f4 
    NodeToken   f5  
        */
    @Override
    public void visit(Alloc n, functionStruct fStruct) {
        
        String sizeObject = n.f4.f0.toString();
        addEntry(sizeObject, fStruct.functionName, fStruct.lineNumber);
    
    }

    /**
    Grammar production: f0 -> "print" f1 -> "(" f2 -> Identifier() f3 -> ")"
         
    NodeToken   f0 
    NodeToken   f1 
    Identifier  f2 
    NodeToken   f3  
        */
    @Override
    public void visit(Print n, functionStruct fStruct) {

        String printObject = n.f2.f0.toString();
        addEntry(printObject, fStruct.functionName, fStruct.lineNumber);
    
    }

    /**
    Grammar production: f0 -> "error" f1 -> "(" f2 -> StringLiteral() f3 -> ")"
         
    NodeToken   f0 
    NodeToken   f1 
    StringLiteral   f2 
        NodeToken   f0 
    NodeToken   f3   
        */
    @Override
    public void visit(ErrorMessage n, functionStruct fStruct) {
    
    }

    /**
    Grammar production: f0 -> "goto" f1 -> Label()
         
    NodeToken    f0 
    Label   f1 
        NodeToken   f0  
        */
    @Override
    public void visit(Goto n, functionStruct fStruct) {
        String label= n.f1.f0.toString();
        //System.err.println("entering goto "+label);
    }

    /**
    Grammar production: f0 -> "if0" f1 -> Identifier() f2 -> "goto" f3 -> Label()
         
    NodeToken   f0 
    Identifier  f1 
    NodeToken   f2 
    Label   f3 
        NodeToken   f0 
        */
    @Override
    public void visit(IfGoto n, functionStruct fStruct) {
        //System.err.println("entering ifGoto");
        String conditionObject = n.f1.f0.toString();
        addEntry(conditionObject, fStruct.functionName, fStruct.lineNumber);
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> "call" 
    f3 -> Identifier() f4 -> "(" f5 -> ( Identifier() )* f6 -> ")"
         
    Identifier  f0 
    NodeToken   f1 
    NodeToken   f2 
    Identifier  f3 
    NodeToken   f4 
    NodeListOptional    f5 
    NodeToken   f6 
        */
    @Override
    public void visit(Call n, functionStruct fStruct) {

        //System.err.println("entering func call");
        String functionObject = n.f3.f0.toString();
        addEntry(functionObject, fStruct.functionName, fStruct.lineNumber);
        n.f5.accept(this, fStruct); //process all parameters
    
    }

    

    private void printError(String s)
    {
        //System.err.println(s);
    }
    

}
