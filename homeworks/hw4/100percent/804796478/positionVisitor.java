import cs132.IR.syntaxtree.*;
import cs132.IR.visitor.GJVoidDepthFirst;
import java.util.*;
import java.lang.*;

public class positionVisitor extends GJVoidDepthFirst<functionStruct> {
    public HashMap<String, HashMap<String,Integer>> fTable; //record the first lhs apprearance
    public HashMap<String, HashMap<String,Integer>> pTable; //record the last rhs apprearance
    public HashMap<String, HashMap<String, String>> aTable= new HashMap<String, HashMap<String, String>>();;
    public HashMap<String, HashMap<String, String>> registerTable= new HashMap<String, HashMap<String, String>>();
    public HashMap<String, HashMap<String, String>> variableTable= new HashMap<String, HashMap<String, String>>();
    public HashMap<String, HashMap<String, Integer>> labelDict = new HashMap<String, HashMap<String, Integer>>();
    public HashMap<String, HashMap<String, String>> linearTable = new HashMap<String, HashMap<String, String>>();
    public HashMap<String, HashMap<String, Pair>> rangeTable = new HashMap<String, HashMap<String, Pair>>();

    public HashMap<String, HashMap<String, Pair>> aBounds = new HashMap<String, HashMap<String, Pair>>();
    public HashMap<String, HashMap<String, Pair>> aRangeTable = new HashMap<String, HashMap<String, Pair>>();
    public ArrayList<Pair> loopArray= new ArrayList<Pair>();
    

    public int lineCount;
    public positionVisitor (){
        fTable= new HashMap<String, HashMap<String,Integer>>();
        pTable= new HashMap<String, HashMap<String,Integer>>();
        lineCount= 1; //TODO: reset lineCount after exiting function
    }

    public void addId(String id, String functionName, int lineNumber){
        if(aTable.get(functionName).containsKey(id)){

            if(!aBounds.get(functionName).containsKey(id)){ //first appearance of a reg var
                aBounds.get(functionName).put(id, new Pair(lineNumber, lineNumber));
            }
            else{
                int sx= Math.min( aBounds.get(functionName).get(id).first(), lineNumber);
                int sy= Math.max( aBounds.get(functionName).get(id).second(), lineNumber);
                aBounds.get(functionName).put(id, new Pair(sx, sy));
            }
            return;
        }
        addDef(id, functionName, lineNumber);
        addEntry(id, functionName, lineNumber);
    }

    public void addDef(String id, String functionName, int lineNumber){
        if(!fTable.containsKey(functionName)){
            HashMap <String, Integer> myMap= new HashMap<String, Integer>();
            fTable.put(functionName, myMap);
        }
        if(fTable.get(functionName).containsKey(id)){
            int better=Math.min(lineNumber, fTable.get(functionName).get(id));
            fTable.get(functionName).put(id, better);
        }
        else{
            fTable.get(functionName).put(id, lineNumber);
        }

    }

    public void addEntry(String id, String functionName, int lineNumber){
        if(!pTable.containsKey(functionName)){
            HashMap <String, Integer> myMap= new HashMap<String, Integer>();
            pTable.put(functionName, myMap);
        }
        if(pTable.get(functionName).containsKey(id)){
            int better=Math.max(lineNumber, pTable.get(functionName).get(id));
            pTable.get(functionName).put(id, better);
        }
        else{
            pTable.get(functionName).put(id, lineNumber);
        }
    }

    public void addLabel(String id, String functionName, int lineNumber){
        if(!labelDict.containsKey(functionName)){
            HashMap <String, Integer> myMap= new HashMap<String, Integer>();
            labelDict.put(functionName, myMap);
        }
        
        labelDict.get(functionName).put(id, lineNumber);
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
        loopArray.clear();
        System.err.println("entering function "+myName);
        
        HashMap<String, String> rt= new HashMap<String, String>();
        HashMap<String, String> vt= new HashMap<String, String>();
        variableTable.put(myName, vt);
        registerTable.put(myName, rt);
        HashMap <String, Pair> myMap= new HashMap<String, Pair>();
        aBounds.put(myName, myMap);

        //we want to add a2-a7, also the passed ids to have a use at line 0
        if ( n.f3.present() ) {
            Enumeration <Node> nodeListEnum=n.f3.elements();
            while(nodeListEnum.hasMoreElements() ){
                //System.err.println("ready to print singleParam");
                String singleParameter= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                addId(singleParameter, myName, 0); //give function parameters a line number of 0
            }
        }


        n.f5.accept(this, fStruct);
        lineCount=1;

        HashMap<String, Pair> range= new HashMap<String, Pair>();
        HashMap<String, Pair> aArgRange= new HashMap<String, Pair>();
        ArrayList<Pair> events= new ArrayList<Pair>();

        for(String id: fTable.get(myName).keySet()){
            int startPos= fTable.get(myName).get(id);
            int endPos= pTable.get(myName).get(id);
            for(Pair p: loopArray){
                if((p.first() <= startPos && p.second() >= startPos) || (p.first() <= endPos && p.second()>= endPos) ){
                    startPos = Math.min(p.first(), startPos);
                    endPos = Math.max(p.second(), endPos);
                }
            }
            events.add(new Pair(startPos, -1, id,0));
            events.add(new Pair(-1, endPos, id, 1));
            range.put(id, new Pair(startPos, endPos));
        }

        for(String id: aBounds.get(myName).keySet()){
            int startPos= aBounds.get(myName).get(id).first();
            int endPos= aBounds.get(myName).get(id).second();
            for(Pair p: loopArray){
                if((p.first() <= startPos && p.second() >= startPos) || (p.first() <= endPos && p.second()>= endPos) ){
                    startPos = Math.min(p.first(), startPos);
                    endPos = Math.max(p.second(), endPos);
                }
            }

            aArgRange.put(id, new Pair(startPos, endPos));
        }

        /*
        System.err.println("printing range");
        for(String name:range.keySet()){
            System.err.println(name+" has startPos "+range.get(name).first()+" and endPos "+range.get(name).second());
        }*/
        rangeTable.put(myName, range);
        aRangeTable.put(myName, aArgRange);

        Collections.sort(events, Pair.myComparator);

        ArrayDeque <String> available= new ArrayDeque<String>();
        List<String> cList = Arrays.asList("t0","t1","t2","t3","t4","t5","s1","s2","s3","s4","s5","s6","s7","s8");
        for(String ss :cList){available.add(ss);}
        HashMap <String, String> temp= new HashMap<String, String>();
        HashMap <String, String> assignment= new HashMap<String, String>();

        for(Pair p: events){
            String id = p.id;
            if(p.op == 1){
                if(temp.containsKey(id)){
                    assignment.put(id, temp.get(id));
                    available.add(temp.get(id));
                    temp.remove(id);
                }
            } 
            else{
                if(!available.isEmpty()){
                    String myReg= available.poll();
                    temp.put(id, myReg);
                }
                else{
                    int myEnd= range.get(id).second();
                    int otherEnd= myEnd;
                    String otherName= id;
                    for(String candidate: temp.keySet()){
                        int tEnd= range.get(candidate).second();
                        if(tEnd > otherEnd){
                            otherEnd = tEnd;
                            otherName = candidate;
                        }
                    }
                    if(!otherName.equals(id)){
                        String spitReg= temp.get(otherName);
                        temp.remove(otherName);
                        temp.put(id, spitReg);
                    }
                }
            }
        }
        for(String name: temp.keySet()){
            assignment.put(name, temp.get(name));
        }

        for(String name:assignment.keySet()){
            System.err.println(name+" is assigned to "+assignment.get(name));
        }
        linearTable.put(myName, assignment);
        System.err.println("leaving function "+myName);
    }

    /**
    NodeToken   f0 
        */
    @Override
    public void visit(Identifier n, functionStruct fStruct) {
        String id = n.f0.toString();
        addId(id, fStruct.functionName, fStruct.lineNumber);
    
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

        //String returnObject = n.f2.f0.toString(); //seems like we can return from stack
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
        n.f0.accept(this, fStruct);
    }

    /**
    Grammar production: f0 ->
    NodeToken   f0
        */
    @Override
    public void visit(Label n, functionStruct fStruct) {
        String labelName= n.f0.toString();
        addLabel(labelName, fStruct.functionName, fStruct.lineNumber);
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
        String id = n.f0.f0.toString();
        addId(id, fStruct.functionName, fStruct.lineNumber);
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
        //System.err.println("entering and set function");
        String id = n.f0.f0.toString();
        addId(id, fStruct.functionName, fStruct.lineNumber);
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
        //System.err.println("add operation");
        String resultIntObject = n.f0.f0.toString();
        addId(resultIntObject, fStruct.functionName, fStruct.lineNumber);

        String firstIntObject = n.f2.f0.toString();
        addId(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addId(secondIntObject, fStruct.functionName, fStruct.lineNumber);      
    
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
        //System.err.println("computing subtraction");
        String resultIntObject = n.f0.f0.toString();
        addId(resultIntObject, fStruct.functionName, fStruct.lineNumber);
        
        String firstIntObject = n.f2.f0.toString();
        addId(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addId(secondIntObject, fStruct.functionName, fStruct.lineNumber);
    
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
        //System.err.println("computing multiplication");
        String resultIntObject = n.f0.f0.toString();
        addId(resultIntObject, fStruct.functionName, fStruct.lineNumber);

        String firstIntObject = n.f2.f0.toString();
        addId(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addId(secondIntObject, fStruct.functionName, fStruct.lineNumber);
    
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
        //System.err.println("computing less than ");
        String resultIntObject = n.f0.f0.toString();
        addId(resultIntObject, fStruct.functionName, fStruct.lineNumber);
        
        String firstIntObject = n.f2.f0.toString();
        addId(firstIntObject, fStruct.functionName, fStruct.lineNumber);

        String secondIntObject = n.f4.f0.toString();
        addId(secondIntObject, fStruct.functionName, fStruct.lineNumber);
    
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
        //System.err.println("perform loading");
        String loadedObject = n.f0.f0.toString();
        addId(loadedObject, fStruct.functionName, fStruct.lineNumber);

        String baseObject = n.f3.f0.toString();
        addId(baseObject, fStruct.functionName, fStruct.lineNumber);
        
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
        String baseObject = n.f1.f0.toString();
        addId(baseObject, fStruct.functionName, fStruct.lineNumber);
        //String offsetObject = n.f3.f0.toString();
        String storedObject = n.f6.f0.toString();
        addId(storedObject, fStruct.functionName, fStruct.lineNumber);
    }

    /**
    Grammar production: f0 -> Identifier() f1 -> "=" f2 -> Identifier()
         
    Identifier  f0 
    NodeToken   f1 
    Identifier  f2  
        */
    @Override
    public void visit(Move n, functionStruct fStruct) {
        //System.err.println("a move operation");
        String leftObject = n.f0.f0.toString();
        addId(leftObject, fStruct.functionName, fStruct.lineNumber);

        String rightObject = n.f2.f0.toString();
        addId(rightObject, fStruct.functionName, fStruct.lineNumber);
    
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
        //System.err.println("allocating");
        String leftObject = n.f0.f0.toString();
        addId(leftObject, fStruct.functionName, fStruct.lineNumber);

        String sizeObject = n.f4.f0.toString();
        addId(sizeObject, fStruct.functionName, fStruct.lineNumber);
    
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
        //System.err.println("print operation");
        String printObject = n.f2.f0.toString();
        addId(printObject, fStruct.functionName, fStruct.lineNumber);
    
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
        //System.err.println("goto statement");
        String label= n.f1.f0.toString();
        String myName= fStruct.functionName;
        int myNumber= fStruct.lineNumber;
        if(!labelDict.containsKey(myName)|| !labelDict.get(myName).containsKey(label)){
            ;
        }
        else{
            int nextNumber= labelDict.get(myName).get(label);
            if(nextNumber < myNumber){
                loopArray.add(new Pair(nextNumber, myNumber));
            }
        }
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
        addId(conditionObject, fStruct.functionName, fStruct.lineNumber);

        String label= n.f3.f0.toString();
        String myName= fStruct.functionName;
        int myNumber= fStruct.lineNumber;
        if(!labelDict.containsKey(myName)|| !labelDict.get(myName).containsKey(label) ){
            ;
        }
        else{
            int nextNumber= labelDict.get(myName).get(label);
            if(nextNumber < myNumber){
                loopArray.add(new Pair(nextNumber, myNumber));
            }
        }
    
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
        //System.err.println("function call");
        String resultObject = n.f0.f0.toString();
        addId(resultObject, fStruct.functionName, fStruct.lineNumber);
        //System.err.println("entering func call");
        String functionObject = n.f3.f0.toString();
        addId(functionObject, fStruct.functionName, fStruct.lineNumber);
        n.f5.accept(this, fStruct); //process all parameters
    
    }

    

    private void printError(String s)
    {
        //System.err.println(s);
    }
    

}
