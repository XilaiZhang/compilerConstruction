import cs132.IR.syntaxtree.*;
import cs132.IR.visitor.GJDepthFirst;
import java.util.*;
//TODO: reset t counter
//TODO: only need one register for move
//TODO: clear all t variables after exiting function calls

public class IRVisitor extends GJDepthFirst<IRStruct, functionStruct> {
    
    public HashMap<String, HashMap<String,Integer>> IRTable= new HashMap<String, HashMap<String,Integer>>();
    public HashMap<String, HashMap<String,String>> aTable= new HashMap<String, HashMap<String,String>>();
    public HashMap<String, HashMap<String, String>> registerTable= new HashMap<String, HashMap<String, String>>();
    public HashMap<String, HashMap<String, String>> variableTable= new HashMap<String, HashMap<String, String>>();

    public HashMap<String, HashMap<String, String>> linearTable = new HashMap<String, HashMap<String, String>>();
    public HashMap<String, HashMap<String, Pair>> rangeTable = new HashMap<String, HashMap<String, Pair>>();
    public HashMap<String, HashMap<String, Pair>> aRangeTable = new HashMap<String, HashMap<String, Pair>>();
    public HashSet<String> assigned= new HashSet<String>();
    public HashSet<String> available = new HashSet<String>();
    public ArrayList <String> regList= new ArrayList<String>();
    public ArrayList <String> extraList= new ArrayList<String>();
    public int tCounter = 0;
    public int sCounter = 0;
    public int linePos;

    public IRVisitor(){
            List<String> aList= Arrays.asList( //"a2","a3","a4","a5","a6","a7" these are function arguments
            //"s1","s2","s3","s4","s5","s6","s7","s8","s9","s10","s11",
            "t0","t1","t2","t3","t4","t5","s1","s2","s3","s4","s5","s6","s8"); //save 9,10,11
            for(String s:aList){regList.add(s);}
  
            available= new HashSet<> (aList);

            List<String> bList= Arrays.asList();
            for(String s:bList){extraList.add(s);}
            
            linePos = 1;
    }
    /**
        f0 -> ( FunctionDeclaration() )* f1 ->
        Program(NodeListOptional n0, NodeToken n1)
        */

    @Override
    public IRStruct visit(Program n, functionStruct fStruct) {
        IRStruct result = new IRStruct();
        IRStruct functionObject = n.f0.accept(this, fStruct);
        for(int i=0;i<functionObject.optionalList.size();i++){
            result.first.addAll(functionObject.optionalList.get(i));
        }
        String programName = n.f1.toString();
        //result.first.add("to debug");
        //System.err.println("program name is "+programName);
        return result;
    
    }

    @Override
    public IRStruct visit(NodeListOptional n, functionStruct fStruct) {
        IRStruct result=new IRStruct();
        //System.err.println("entering nodelistoptional");
        if ( n.present() ) {
            Enumeration <Node> nodeListEnum=n.elements();
            while(nodeListEnum.hasMoreElements() ){
                IRStruct small=nodeListEnum.nextElement().accept(this, fStruct);
                result.optionalList.add(small.first);
                result.optionalResult.add(small.second);
                //System.err.println("collected one node");
            }
            //System.err.println("leaving nodeListOptional");
            return result;
        }
        return result;
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
    public IRStruct visit(FunctionDeclaration n, functionStruct fStruct) {
        IRStruct result= new IRStruct();
        String myName = n.f1.f0.toString();
        fStruct.functionName= myName;
        //System.err.println("visiting function "+myName);
        String header="func "+myName+"(";

        HashMap <String, String> extraDict= new HashMap<String, String>();
        int seq=2;
        if ( n.f3.present() ) {
            //System.err.println("entering parameters");
            Enumeration <Node> nodeListEnum=n.f3.elements();
            while(nodeListEnum.hasMoreElements() ){
                String parameterId= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                if(seq > 7){
                    //String aReg= "a"+Integer.toString(seq);
                    header= header+" "+parameterId;
                    if(linearTable.get(myName).containsKey(parameterId)){
                        extraDict.put(linearTable.get(myName).get(parameterId), parameterId);
                    }
                }
                seq+=1;
            }
        }

        header=header + ")";

        result.first.add(header);
        for(String tReg: extraDict.keySet()){
            String extraP= extraDict.get(tReg);
            result.first.add(tReg+" = "+extraP);  //handle case when parameter greater than 6 but assigned reg
        }

        IRStruct blockObject = n.f5.accept(this, fStruct);
        result.first.addAll(blockObject.first);

        linePos=1;
        return result;
    
    }

    /**
    NodeToken   f0 
        */
    @Override
    public IRStruct visit(Identifier n, functionStruct fStruct) {
        IRStruct result = new IRStruct();
        String id = n.f0.toString();
        result.first.add(id);
        //System.err.println("visiting identifier "+id);
        return result;
    
    }

    /**
    Grammar production: f0 -> ( Instruction() )* f1 -> "return" f2 -> Identifier()
    NodeListOptional f0 
    NodeToken f1 
    Identifier f2 
        */
    @Override
    public IRStruct visit(Block n, functionStruct fStruct) {
        IRStruct result= new IRStruct();
        IRStruct instructionObject = n.f0.accept(this, fStruct);
        for(int i=0;i<instructionObject.optionalList.size();i++){
            result.first.addAll(instructionObject.optionalList.get(i));
        }
        String returnObject = n.f2.f0.toString();

        String myName= fStruct.functionName;
        if(linearTable.get(myName).containsKey(returnObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(returnObject);
            result.first.add(returnObject+" = "+myReg);
            result.first.add("return "+returnObject);
        }
        else if(aTable.get(myName).containsKey(returnObject)){
            String myReg= aTable.get(myName).get(returnObject);
            result.first.add(returnObject+" = "+myReg);
            result.first.add("return "+ returnObject);
        }
        else{
            result.first.add("return "+returnObject);
        }

        //reset all t registers

        return result; 
    
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
    public IRStruct visit(Instruction n, functionStruct fStruct) {
        fStruct.lineNumber= linePos;
        linePos += 1;
        IRStruct instructionObject = n.f0.choice.accept(this, fStruct); //nodechoice.f0 
        return instructionObject;
    
    }

    /**
    Grammar production: f0 -> Label() f1 -> ":"
    Label   f0 
    NodeToken   f1
        */
    @Override
    public IRStruct visit(LabelWithColon n, functionStruct fStruct) {
        
        IRStruct result= new IRStruct();
        String labelName = n.f0.f0.toString();
        //System.err.println("labelwithColon : "+labelName);
        result.first.add(labelName+":");
        return result;
    
    }

    /**
    Grammar production: f0 ->
    NodeToken   f0
        */
    @Override
    public IRStruct visit(Label n, functionStruct fStruct) {
        //System.err.println("label");
        IRStruct result = new IRStruct();
        String label = n.f0.toString();
        result.first.add(label);
        return result;
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> IntegerLiteral()
    Identifier  f0 
    NodeToken   f1 
    IntegerLiteral  f2
        */
    @Override
    public IRStruct visit(SetInteger n, functionStruct fStruct) {
        //System.err.println("setting int");
        IRStruct result = new IRStruct();
        String id = n.f0.f0.toString();
        String intObject = n.f2.f0.toString();

        String myName= fStruct.functionName;
        if(linearTable.get(myName).containsKey(id)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(id);
            result.first.add(myReg+" = "+intObject);
        }
        else if(aTable.get(myName).containsKey(id)){
            String myReg= aTable.get(myName).get(id);
            result.first.add(myReg+" = "+intObject);
        }
        else{
            result.first.add(" s9 = "+intObject);
            result.first.add(id+" = s9");       
        }
        return result;
    
    }

    /**
    Grammar production: f0 ->
    NodeToken   f0 
        */
    @Override
    public IRStruct visit(IntegerLiteral n, functionStruct fStruct) {
        IRStruct result= new IRStruct();
        String intObject = n.f0.toString();
        result.first.add(intObject);

        return result;
    
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
    public IRStruct visit(SetFuncName n, functionStruct fStruct) {
        //System.err.println("setFuncName");
        IRStruct result= new IRStruct();
        String id = n.f0.f0.toString();
        String funcName = n.f3.f0.toString();

        String myName= fStruct.functionName;
        if(linearTable.get(myName).containsKey(id)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(id);
            result.first.add(myReg+" = @"+funcName);
        }
        else if(aTable.get(myName).containsKey(id)){
            String myReg= aTable.get(myName).get(id);
            result.first.add(myReg+" = @"+funcName);
        }
        else{
            result.first.add(" s9 = @"+funcName);
            result.first.add(id+" = s9");
        }

        return result;
    
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
    public IRStruct visit(Add n, functionStruct fStruct) {
        //System.err.println("add operation");
        IRStruct result= new IRStruct();
        String resultIntObject = n.f0.f0.toString();
        String firstIntObject = n.f2.f0.toString();
        String secondIntObject = n.f4.f0.toString();
        String myName= fStruct.functionName;

        String a1= "s9";
        if(linearTable.get(myName).containsKey(firstIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(firstIntObject);
            a1= myReg;
        }
        else if(aTable.get(myName).containsKey(firstIntObject)){
            String myReg= aTable.get(myName).get(firstIntObject);
            a1=myReg;
        }
        else{
            result.first.add("s9 = "+firstIntObject);         
        }

        String a2="s10";
        if(linearTable.get(myName).containsKey(secondIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(secondIntObject);
            a2= myReg;
        }
        else if(aTable.get(myName).containsKey(secondIntObject)){
            String myReg= aTable.get(myName).get(secondIntObject);
            a2=myReg;
        }
        else{
            result.first.add("s10 = "+secondIntObject);
        }

        if(linearTable.get(myName).containsKey(resultIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" + "+a2);
        }
        else if(aTable.get(myName).containsKey(resultIntObject)){
            String myReg= aTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" + "+a2);
        }
        else{
            result.first.add(" s11 = "+a1+" + "+a2);
            result.first.add(resultIntObject+" = s11");          
        }

        return result;
    
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
    public IRStruct visit(Subtract n, functionStruct fStruct) {
        //System.err.println("subtract operation");
        IRStruct result= new IRStruct();
        String resultIntObject = n.f0.f0.toString();
        String firstIntObject = n.f2.f0.toString();
        String secondIntObject = n.f4.f0.toString();
        String myName= fStruct.functionName;

        String a1= "s9";
        if(linearTable.get(myName).containsKey(firstIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(firstIntObject);
            a1= myReg;
        }
        else if(aTable.get(myName).containsKey(firstIntObject)){
            String myReg= aTable.get(myName).get(firstIntObject);
            a1=myReg;
        }
        else{
            result.first.add("s9 = "+firstIntObject);         
        }

        String a2="s10";
        if(linearTable.get(myName).containsKey(secondIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(secondIntObject);
            a2= myReg;
        }
        else if(aTable.get(myName).containsKey(secondIntObject)){
            String myReg= aTable.get(myName).get(secondIntObject);
            a2=myReg;
        }
        else{
            result.first.add("s10 = "+secondIntObject);
        }

        if(linearTable.get(myName).containsKey(resultIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" - "+a2);
        }
        else if(aTable.get(myName).containsKey(resultIntObject)){
            String myReg= aTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" - "+a2);
        }
        else{
            result.first.add(" s11 = "+a1+" - "+a2);
            result.first.add(resultIntObject+" = s11");          
        }

        return result;
    
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
    public IRStruct visit(Multiply n, functionStruct fStruct) {
        //System.err.println("multiply operation");
        IRStruct result = new IRStruct();
        String resultIntObject = n.f0.f0.toString();
        String firstIntObject = n.f2.f0.toString();
        String secondIntObject = n.f4.f0.toString();
        String myName= fStruct.functionName;

        String a1= "s9";
        if(linearTable.get(myName).containsKey(firstIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(firstIntObject);
            a1= myReg;
        }
        else if(aTable.get(myName).containsKey(firstIntObject)){
            String myReg= aTable.get(myName).get(firstIntObject);
            a1=myReg;
        }
        else{
            result.first.add("s9 = "+firstIntObject);         
        }

        String a2="s10";
        if(linearTable.get(myName).containsKey(secondIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(secondIntObject);
            a2= myReg;
        }
        else if(aTable.get(myName).containsKey(secondIntObject)){
            String myReg= aTable.get(myName).get(secondIntObject);
            a2=myReg;
        }
        else{
            result.first.add("s10 = "+secondIntObject);
        }

        if(linearTable.get(myName).containsKey(resultIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" * "+a2);
        }
        else if(aTable.get(myName).containsKey(resultIntObject)){
            String myReg= aTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" * "+a2);
        }
        else{
            result.first.add(" s11 = "+a1+" * "+a2);
            result.first.add(resultIntObject+" = s11");          
        }

        return result;
    
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
    public IRStruct visit(LessThan n, functionStruct fStruct) {
        //System.err.println("computing less than ");
        IRStruct result= new IRStruct();
        String resultIntObject = n.f0.f0.toString();
        String firstIntObject = n.f2.f0.toString();
        String secondIntObject = n.f4.f0.toString();
        String myName= fStruct.functionName;

        String a1= "s9";
        if(linearTable.get(myName).containsKey(firstIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(firstIntObject);
            a1= myReg;
        }
        else if(aTable.get(myName).containsKey(firstIntObject)){
            String myReg= aTable.get(myName).get(firstIntObject);
            a1=myReg;
        }
        else{
            result.first.add("s9 = "+firstIntObject);         
        }

        String a2="s10";
        if(linearTable.get(myName).containsKey(secondIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(secondIntObject);
            a2= myReg;
        }
        else if(aTable.get(myName).containsKey(secondIntObject)){
            String myReg= aTable.get(myName).get(secondIntObject);
            a2=myReg;
        }
        else{
            result.first.add("s10 = "+secondIntObject);
        }

        if(linearTable.get(myName).containsKey(resultIntObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" < "+a2);
        }
        else if(aTable.get(myName).containsKey(resultIntObject)){
            String myReg= aTable.get(myName).get(resultIntObject);
            result.first.add(myReg+" = "+a1+" < "+a2);
        }
        else{
            result.first.add(" s11 = "+a1+" < "+a2);
            result.first.add(resultIntObject+" = s11");          
        }

        return result;
    
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
    public IRStruct visit(Load n, functionStruct fStruct) {
        //System.err.println("load operation");
        IRStruct result= new IRStruct();
        String loadedObject = n.f0.f0.toString();
        String baseObject = n.f3.f0.toString();
        String offsetObject = n.f5.f0.toString();
        String myName= fStruct.functionName;

        String a2= "s9";
        if(linearTable.get(myName).containsKey(baseObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(baseObject);
            a2=myReg;
        }
        else if(aTable.get(myName).containsKey(baseObject)){
            String myReg= aTable.get(myName).get(baseObject);
            a2=myReg;
        }
        else{
            result.first.add("s9 = "+baseObject);
        }

        if(linearTable.get(myName).containsKey(loadedObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(loadedObject);
            result.first.add(myReg+" = ["+a2+" + "+offsetObject+"]");
        }
        else if(aTable.get(myName).containsKey(loadedObject)){
            String myReg= aTable.get(myName).get(loadedObject);
            result.first.add(myReg+" = ["+a2+" + "+offsetObject+"]");
        }
        else{
            result.first.add(" s10 = ["+a2+" + "+offsetObject+"]");
            result.first.add(loadedObject+" = s10");
        }

        return result;
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
    public IRStruct visit(Store n, functionStruct fStruct) {
        //System.err.println("store operation");
        IRStruct result= new IRStruct();
        String baseObject = n.f1.f0.toString();
        String offsetObject = n.f3.f0.toString();
        String storedObject = n.f6.f0.toString();
        String myName= fStruct.functionName;

        String a2= "s9";
        if(linearTable.get(myName).containsKey(storedObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(storedObject);
            a2=myReg;
        }
        else if(aTable.get(myName).containsKey(storedObject)){
            String myReg= aTable.get(myName).get(storedObject);
            a2=myReg;
        }
        else{
            result.first.add("s9 = "+storedObject);
        }

        if(linearTable.get(myName).containsKey(baseObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(baseObject);
            result.first.add("["+myReg+" + "+offsetObject+"] = "+a2);
        }
        else if(aTable.get(myName).containsKey(baseObject)){
            String myReg= aTable.get(myName).get(baseObject);
            result.first.add("["+myReg+" + "+offsetObject+"] = "+a2);
        }
        else{
            result.first.add("s10 = "+baseObject);
            result.first.add("[ s10 + " + offsetObject+"] = " + a2);

        }
        
        return result;
    
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> Identifier()
         
    Identifier  f0 
    NodeToken   f1 
    Identifier  f2  
        */
    @Override
    public IRStruct visit(Move n, functionStruct fStruct) {
        //System.err.println("move operation");
        IRStruct result= new IRStruct();
        String leftObject = n.f0.f0.toString();
        String rightObject = n.f2.f0.toString();
        String myName= fStruct.functionName;

        String a2=rightObject;
        if(linearTable.get(myName).containsKey(rightObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(rightObject);
            a2 = myReg;
        }
        else if(aTable.get(myName).containsKey(rightObject)){
            String myReg= aTable.get(myName).get(rightObject);
            a2 = myReg;
        }

        if(linearTable.get(myName).containsKey(leftObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(leftObject);
            result.first.add( myReg+" = "+a2);
        }
        else if(aTable.get(myName).containsKey(leftObject)){
            String myReg= aTable.get(myName).get(leftObject);
            result.first.add( myReg+" = "+a2);
        }
        else{
            result.first.add("s9 = "+a2);
            result.first.add(leftObject+" = s9");

        }

        return result;
    
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
    public IRStruct visit(Alloc n, functionStruct fStruct) {
        //System.err.println("alloc");
        IRStruct result= new IRStruct();
        String allocatedObject = n.f0.f0.toString();
        String sizeObject = n.f4.f0.toString();
        String myName= fStruct.functionName;

        String a2= "s9";
        if(linearTable.get(myName).containsKey(sizeObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(sizeObject);
            a2= myReg;
        }
        else if(aTable.get(myName).containsKey(sizeObject)){
            String myReg= aTable.get(myName).get(sizeObject);
            a2=myReg;
        }
        else{
            result.first.add("s9 = "+sizeObject);
        }

        if(linearTable.get(myName).containsKey(allocatedObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(allocatedObject);
            result.first.add(myReg+" = alloc( "+a2+" )");
        }
        else if(aTable.get(myName).containsKey(allocatedObject)){
            String myReg= aTable.get(myName).get(allocatedObject);
            result.first.add(myReg+" = alloc( "+a2+" )");
        }
        else{
            result.first.add("s10 = alloc( "+a2+" )");
            result.first.add(allocatedObject+" = s10");

        }

        return result;
    
    }

    /**
    Grammar production: f0 -> "print" f1 -> "(" f2 -> Identifier() f3 -> ")"
         
    NodeToken   f0 
    NodeToken   f1 
    Identifier  f2 
    NodeToken   f3  
        */
    @Override
    public IRStruct visit(Print n, functionStruct fStruct) {
        //System.err.println("printint statement");
        IRStruct result = new IRStruct();
        String printObject = n.f2.f0.toString();
        String myName= fStruct.functionName;

        if(linearTable.get(myName).containsKey(printObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(printObject);
            result.first.add("print( "+myReg+")");
        }
        else if(aTable.get(myName).containsKey(printObject)){
            String myReg = aTable.get(myName).get(printObject);
            result.first.add("print( "+myReg+")");
        }
        else{
            result.first.add("s9 = "+printObject);
            result.first.add("print( s9 )");
        }


        return result;
    
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
    public IRStruct visit(ErrorMessage n, functionStruct fStruct) {
        IRStruct result = new IRStruct();
        String errorObject = n.f2.f0.toString();
        String line= "error("+errorObject+")";
        result.first.add(line);
        return result;
    
    }

    /**
    Grammar production: f0 -> "goto" f1 -> Label()
         
    NodeToken    f0 
    Label   f1 
        NodeToken   f0  
        */
    @Override
    public IRStruct visit(Goto n, functionStruct fStruct) {
        //System.err.println("goto");
        IRStruct result= new IRStruct();
        String label = n.f1.f0.toString();
        String line="goto "+label;
        result.first.add(line);
        return result;
    
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
    public IRStruct visit(IfGoto n, functionStruct fStruct) {
        //System.err.println("ifgoto");
        IRStruct result= new IRStruct();
        String conditionObject = n.f1.f0.toString();
        String label = n.f3.f0.toString();
        String myName= fStruct.functionName;

        if(linearTable.get(myName).containsKey(conditionObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(conditionObject);
            result.first.add("if0 "+myReg+" goto "+label);
        }
        else if(aTable.get(myName).containsKey(conditionObject)){
            String myReg = aTable.get(myName).get(conditionObject);
            result.first.add("if0 "+myReg+" goto "+label);
        }
        else{

            result.first.add("s9 = "+conditionObject);
            result.first.add("if0 s9 goto "+label);
        }

        return result;
    
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
    //TODO: clear temporaries
    @Override
    public IRStruct visit(Call n, functionStruct fStruct) {
        IRStruct result=new IRStruct();
        String returnObject = n.f0.f0.toString();
        String functionName = n.f3.f0.toString();
        String myName= fStruct.functionName;
        
        int currentLine = fStruct.lineNumber;
        HashMap<String, String> savedReg= new HashMap<String, String>();
        HashMap<String, String> oldA = aTable.get(myName);
        for(String variable: oldA.keySet()){ //if aReg was previously occupied by somebody
            int lastUse = aRangeTable.get(myName).get(variable).second();
            if(lastUse < currentLine){
                continue;                 //using strictly less than to prevent swapping problem, may be optimized later
            }
            String aReg= oldA.get(variable);
            String stackSaveReg = aReg+"_stacksave";
            result.first.add(stackSaveReg +" = "+ aReg);
            savedReg.put(variable, aReg);
        }
        
        HashMap< String, String> savedT = new HashMap<String, String> ();
        for(String variable: linearTable.get(myName).keySet()){  //clear all 
            int lastUse = rangeTable.get(myName).get(variable).second();
            int firstUse = rangeTable.get(myName).get(variable).first();
            if(lastUse <= currentLine || firstUse> currentLine){
                continue;    //skip tReg that won't be used after func call, or only happens after func call
            }

            String tReg= linearTable.get(myName).get(variable);
            result.first.add(variable+" = "+tReg);
            savedT.put(tReg, variable);
        }


        int numParameters= n.f5.nodes.size();
        int seq= 2; //starting from a2

        ArrayList<String> moreParameters= new ArrayList<String>();
        
        if ( n.f5.present() ) {
            //System.err.println("entering parameters");
            Enumeration <Node> nodeListEnum=n.f5.elements();
            while(nodeListEnum.hasMoreElements() ){
                String parameterId= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                String aReg= "a"+Integer.toString(seq);
                

                if( seq <= 7){
                    if(linearTable.get(myName).containsKey(parameterId)){
                        String myReg= linearTable.get(myName).get(parameterId);
                        result.first.add(aReg+" = "+myReg);
                    }
                    else if(aTable.get(myName).containsKey(parameterId)){
                        String myReg = aTable.get(myName).get(parameterId);

                        result.first.add(aReg+" = "+myReg+"_stacksave");
                    }
                    else{
                        result.first.add(aReg+" = "+parameterId);
                    }
                }
                else{
                    moreParameters.add(aReg);

                    if(linearTable.get(myName).containsKey(parameterId)){ // if variable is already assigned to register
                        String myReg= linearTable.get(myName).get(parameterId);
                        result.first.add(aReg+" = "+myReg);
                    }
                    else if(aTable.get(myName).containsKey(parameterId)){
                        String myReg= aTable.get(myName).get(parameterId);
                        result.first.add("s11 = "+myReg+"_stacksave");
                        result.first.add(aReg+" = s11");
                    }
                    else{
                        result.first.add("s11 = "+parameterId);
                        result.first.add(aReg+" = s11");
                    }
                }

                seq+=1;
                //associate a regs with params should be during function declaration
            }
            //System.err.println("leaving parameters");
        }

        String callerId = "s9";
        if(linearTable.get(myName).containsKey(functionName)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(functionName);
            callerId = myReg;
        }
        else if(aTable.get(myName).containsKey(functionName)){
            String myReg= aTable.get(myName).get(functionName)+"_stacksave";
            callerId = myReg;
        }
        else{
            callerId = "s9";
            result.first.add("s9 = "+functionName);
        }

        String header = " = call "+callerId+"(";
        for(String extraP: moreParameters){
            header = header + " " + extraP;
        }
        header+=")";
        
        boolean inReg = true;
        String exception = "bullshit";
        if(linearTable.get(myName).containsKey(returnObject)){ // if variable is already assigned to register
            String myReg= linearTable.get(myName).get(returnObject);
            exception=myReg;
        }
        else if(aTable.get(myName).containsKey(returnObject)){
            String myReg = aTable.get(myName).get(returnObject);
            exception=myReg;
        }
        else{
            inReg=false;
            exception = "s10";
        }
        header= exception + header;
        result.first.add(header);
        if(!inReg){
            result.first.add(returnObject+" = s10");
        }
        


        for(String variable: savedReg.keySet()){
            String aReg = savedReg.get(variable);
            if(aReg.equals(exception)){
                continue;
            }

            if(!aRangeTable.get(myName).containsKey(variable)){
                continue;   //the variable is in function parameter but never used
            }
            int lastUse = aRangeTable.get(myName).get(variable).second();
            int firstUse = aRangeTable.get(myName).get(variable).first();
            if(lastUse <= currentLine || firstUse> currentLine){
                continue;    //skip tReg that won't be used after func call, or only happens after func call
            }
            String stackSaveReg = aReg+"_stacksave";
            result.first.add(aReg+" = "+stackSaveReg);
        }

        //System.err.println("restoring potential Ts");
        for(String tReg: savedT.keySet()){
            if(tReg.equals(exception)){
                continue;
            }
            String variable = savedT.get(tReg);
            result.first.add(tReg+" = "+variable);
        }

        return result;
    
    }

    

    private void printError(String s)
    {
        System.err.println(s);
    }
    
}
