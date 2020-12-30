import cs132.IR.syntaxtree.*;
import cs132.IR.visitor.GJDepthFirst;
import java.util.*;

//TODO: why does sample code store and restore s registers?
//TODO: retrieve values stored in a registers from above fp(bigger than fp), and store them in variableTable
//TODO: FIND OUT how many id are needed for each func
//TODO: load address from parameters, when dealing with e.g. multiplication
//TODO: add error message to include array out of bound (currently only null pointer)

public class IRVisitor extends GJDepthFirst<IRStruct, functionStruct> {
    
    public HashMap<String, HashMap<String,Integer>> IRTable= new HashMap<String, HashMap<String,Integer>>();
    public HashMap<String, HashMap<String,String>> aTable= new HashMap<String, HashMap<String,String>>();
    public HashMap<String, HashMap<String, String>> stackTable= new HashMap<String, HashMap<String, String>>();
    public HashMap<String, HashMap<String, String>> variableTable= new HashMap<String, HashMap<String, String>>();
    public HashMap<String, String> aRegisterTable= new HashMap<String, String>();
    public HashMap<String, Integer> functionArgNum= new HashMap<String, Integer>();
    public HashMap<String, HashSet<String>> numIds= new HashMap<String, HashSet<String>>();
    public HashMap<String, HashMap<String, String>> labelTable= new HashMap<String, HashMap<String, String>>();

    public ArrayList <String> regList= new ArrayList<String>();
    public ArrayList <String> extraList= new ArrayList<String>();
    public HashMap<String, String> sRegOffset= new HashMap<String, String>();
    public int tCounter = 0;
    public int sCounter = 0;
    public boolean isMain = true;
    public int idIndex = -56;

    public int randInt= 1;

    public IRVisitor(){
            List<String> aList= Arrays.asList( "a2","a3","a4","a5","a6","a7", //these are function arguments
            "s1","s2","s3","s4","s5","s6","s7","s8","s9","s10","s11",
            "t0","t1","t2","t3","t4","t5"); //save 9,10,11
            for(String s:aList){regList.add(s);}


            //List<String> bList= Arrays.asList();
            //for(String s:bList){extraList.add(s);}
            
            for(int i=1; i<12; ++i){  //store all 11 s registers -12 to -52
                String sReg= "s"+Integer.toString(i);
                String offset= "-"+Integer.toString(4*(i+2))+"(fp)";
                sRegOffset.put(sReg, offset);
            }
        
    }

    public String getTemp(){
        String r="m"+Integer.toString(randInt);
        randInt+=1;
        return r;
    }
    /**
        f0 -> ( FunctionDeclaration() )* f1 ->
        Program(NodeListOptional n0, NodeToken n1)
        */

    @Override
    public IRStruct visit(Program n, functionStruct fStruct) {
        IRStruct result = new IRStruct();
        result.first.add("  .equiv @sbrk, 9");
        result.first.add("  .equiv @print_string, 4");
        result.first.add("  .equiv @print_char, 11");
        result.first.add("  .equiv @print_int, 1");
        result.first.add("  .equiv @exit 10");
        result.first.add("  .equiv @exit2, 17");

        IRStruct functionObject = n.f0.accept(this, fStruct);
        for(int i=0;i<functionObject.optionalList.size();i++){
            result.first.addAll(functionObject.optionalList.get(i));
        }
        String programName = n.f1.toString();
        System.err.println("program name is "+programName);

        result.first.add("# Print the error message at a0 and ends the program");
        result.first.add(".globl error");
        result.first.add("error:");
        result.first.add("  mv a1, a0                                # Move msg address to a1");
        result.first.add("  li a0, @print_string                     # Code for print_string ecall");
        result.first.add("  ecall                                    # Print error message in a1");
        result.first.add("  li a1, 10                                # Load newline character");
        result.first.add("  li a0, @print_char                       # Code for print_char ecall");
        result.first.add("  ecall                                    # Print newline");
        result.first.add("  li a0, @exit                             # Code for exit ecall");
        result.first.add("  ecall                                    # Exit with code");
        result.first.add("abort_17:                                  # Infinite loop");
        result.first.add("  j abort_17                               # Prevent fallthrough");
        result.first.add("");
        result.first.add("");
        result.first.add("# Allocate a0 bytes on the heap, returns pointer to start in a0");
        result.first.add(".globl alloc");
        result.first.add("alloc:");
        result.first.add("  mv a1, a0                                # Move requested size to a1");
        result.first.add("  li a0, @sbrk                             # Code for ecall: sbrk");
        result.first.add("  ecall                                    # Request a1 bytes");
        
        result.first.add("  li t6, 0");
        result.first.add("  jalr t6, ra, 0                                    # Return to caller");
        //result.first.add("  jr ra                                    # Return to caller");
        result.first.add("");
        result.first.add("");
        result.first.add(".data");
        result.first.add("");
        result.first.add(".globl msg_0");
        result.first.add("msg_0:");
        result.first.add("  .asciiz \"null pointer\"");
        result.first.add("  .align 2");
        result.first.add("");
        result.first.add("");
        result.first.add(".globl msg_1");
        result.first.add("msg_1:");
        result.first.add("  .asciiz \"array index out of bounds\"");
        result.first.add("  .align 2");
        result.first.add("");
        result.first.add("");
        result.first.add(".globl msg_extra");
        result.first.add("msg_extra:");
        result.first.add("  .asciiz \"--- Error (Explicit) ---\"");
        result.first.add("  .align 2");
        result.first.add("");
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
        if(isMain){
            result.first.add(".text");
            result.first.add("");
            result.first.add("  jal "+myName+"                                 # Jump to "+myName);
            result.first.add("  li a0, @exit                             # Code for ecall: exit");
            result.first.add("  ecall");
            result.first.add("");
            isMain = false;
        }
        //System.err.println("visiting function "+myName);
        result.first.add(".globl "+myName);
        result.first.add(myName+":");

        result.first.add("sw fp, -8(sp)");
        result.first.add("mv fp, sp");
        int stackSize =(numIds.get(myName).size()+10 ) * 4 + 52;
        String stackSizeString= Integer.toString(stackSize);
        result.first.add("li t6, "+stackSizeString);     //allocate s1-s11, ra and fp, 13 ids, could be problem
        result.first.add("sub sp, sp, t6");
        result.first.add("sw ra, -4(fp)");

        for(int i=1; i<12; ++i){  //store all 11 s registers -12 to -52
            result.first.add("sw s"+Integer.toString(i)+", -"+Integer.toString(4*(i+2))+"(fp)");
        }

        idIndex = -56;

        //process parameter amd map them to stack

        int aboveOffset=24;
        if ( n.f3.present() ) {
            //System.err.println("entering parameters");
            Enumeration <Node> nodeListEnum=n.f3.elements();
            while(nodeListEnum.hasMoreElements() ){
                String parameterId= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                String aboveAddress= Integer.toString(aboveOffset)+"(fp)";
                aboveOffset += 4;

                variableTable.get(myName).put(parameterId, aboveAddress);
                stackTable.get(myName).put(aboveAddress, parameterId);
            }
        }

        tCounter = 0;

        IRStruct blockObject = n.f5.accept(this, fStruct);
        result.first.addAll(blockObject.first);
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

        //restore all s registers
        for(int i=1; i<12; ++i){  //store all 11 s registers -12 to -52
            result.first.add("lw s"+Integer.toString(i)+", -"+Integer.toString(4*(i+2))+"(fp)");
        }

        String stackAddress = variableTable.get(myName).get(returnObject);
        result.first.add("lw a0, "+stackAddress);
        result.first.add("lw ra, -4(fp)");
        result.first.add("lw fp, -8(fp)");

        int stackSize =(numIds.get(myName).size()+10 ) * 4 + 52;
        String stackSizeString= Integer.toString(stackSize);
        result.first.add("addi sp, sp, "+stackSizeString);
        result.first.add("addi sp, sp, "+Integer.toString(functionArgNum.get(myName) * 4) );

        result.first.add("li t6, 0");
        result.first.add("jalr t6, ra, 0");
        //result.first.add("jr ra");

        

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
        String myName= fStruct.functionName;
        IRStruct result= new IRStruct();
        String labelName = n.f0.f0.toString();
        String mappedName = labelTable.get(myName).get(labelName);
        //System.err.println("labelwithColon : "+labelName);
        result.first.add(mappedName+":");
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
        if(regList.contains(id)){ //a T,S,A register
            result.first.add("li "+id+", "+intObject);
        }
        else{
            if( ! variableTable.get(myName).containsKey(id)){
                String sAddress= Integer.toString(idIndex)+"(fp)";
                idIndex -= 4;
                stackTable.get(myName).put(sAddress, id); 
                variableTable.get(myName).put(id, sAddress); 
            }
            
            String stackAddress = variableTable.get(myName).get(id);
            result.first.add("li 6, "+intObject);
            result.first.add("sw t6, "+stackAddress);
            
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

        if(regList.contains(id)){ //a T,S,A register
            result.first.add("la "+id+", "+funcName);
        }
        else{
            if( ! variableTable.get(myName).containsKey(id)){
                String sAddress= Integer.toString(idIndex)+"(fp)";
                idIndex -= 4;
                stackTable.get(myName).put(sAddress, id); 
                variableTable.get(myName).put(id, sAddress); 
            }
            
            String stackAddress = variableTable.get(myName).get(id);
            result.first.add("la 6, "+funcName);
            result.first.add("sw t6, "+stackAddress);
            
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

        result.first.add("add "+resultIntObject+", "+firstIntObject+", "+secondIntObject);

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

        result.first.add("sub "+resultIntObject+", "+firstIntObject+", "+secondIntObject);

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

        result.first.add("mul "+resultIntObject+", "+firstIntObject+", "+secondIntObject);

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

        result.first.add("slt "+resultIntObject+", "+firstIntObject+", "+secondIntObject);

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

        result.first.add("lw "+loadedObject+", "+offsetObject+"("+baseObject+")");

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

        result.first.add("sw "+storedObject+", "+offsetObject+"("+baseObject+")");
        
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

        if(regList.contains(leftObject) && regList.contains(rightObject)){ //a T,S,A register
            result.first.add("mv "+leftObject+", "+rightObject);
        }
        else if(regList.contains(leftObject)){ // if left side is register and right side is id
            if( ! variableTable.get(myName).containsKey(rightObject)){
                String sAddress= Integer.toString(idIndex)+"(fp)";
                idIndex -= 4;
                stackTable.get(myName).put(sAddress, rightObject); 
                variableTable.get(myName).put(rightObject, sAddress); 
            }
            
            String stackAddress = variableTable.get(myName).get(rightObject);
            result.first.add("lw "+leftObject+", "+stackAddress);
            
        }
        else if(regList.contains(rightObject)){
            if( ! variableTable.get(myName).containsKey(leftObject)){
                String sAddress= Integer.toString(idIndex)+"(fp)";
                idIndex -= 4;
                stackTable.get(myName).put(sAddress, leftObject); 
                variableTable.get(myName).put(leftObject, sAddress); 
            }
            
            String stackAddress = variableTable.get(myName).get(leftObject);
            result.first.add("sw "+rightObject+", "+stackAddress);
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

        result.first.add("mv a0, "+sizeObject);

        //result.first.add("li t6, 0");
        //result.first.add("jal t6, alloc");
        result.first.add("jal ra, alloc");
        result.first.add("mv "+allocatedObject+", a0");

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

        result.first.add("mv a1, "+printObject);
        result.first.add("li a0, @print_int");
        result.first.add("ecall");
        result.first.add("li a1, 10                                # Load newline character");
        result.first.add("li a0, @print_char                       # Code for print_char ecall");
        result.first.add("ecall                                    # Print newline");

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
        String errorObject = n.f2.f0.toString();/*
        result.first.add("la a0, msg_extra");
        result.first.add("mv a1, a0                                # Move msg address to a1");
        result.first.add("li a0, @print_string                     # Code for print_string ecall");
        result.first.add("ecall                                    # Print error message in a1");
        result.first.add("li a1, 10                                # Load newline character");
        result.first.add("li a0, @print_char                       # Code for print_char ecall");
        result.first.add("ecall                                    # Print newline");*/


        if(errorObject == "null pointer"){
            result.first.add("la a0, msg_0");

            result.first.add("  mv a1, a0                                # Move msg address to a1");
            result.first.add("  li a0, @print_string                     # Code for print_string ecall");
            result.first.add("  ecall                                    # Print error message in a1");
            result.first.add("  li a1, 10                                # Load newline character");
            result.first.add("  li a0, @print_char                       # Code for print_char ecall");
            result.first.add("  ecall                                    # Print newline");
            result.first.add("  li a0, @exit                             # Code for exit ecall");
            result.first.add("  ecall                                    # Exit with code");
        }
        else{
            result.first.add("la a0, msg_1");
            
            result.first.add("  mv a1, a0                                # Move msg address to a1");
            result.first.add("  li a0, @print_string                     # Code for print_string ecall");
            result.first.add("  ecall                                    # Print error message in a1");
            result.first.add("  li a1, 10                                # Load newline character");
            result.first.add("  li a0, @print_char                       # Code for print_char ecall");
            result.first.add("  ecall                                    # Print newline");
            result.first.add("  li a0, @exit                             # Code for exit ecall");
            result.first.add("  ecall                                    # Exit with code");
        }
        
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
        String myName = fStruct.functionName;
        IRStruct result= new IRStruct();
        String label = n.f1.f0.toString();
        String mappedName = labelTable.get(myName).get(label);

        result.first.add("li t6, 0");
        result.first.add("jal t6, "+mappedName);
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
        String myName= fStruct.functionName;
        IRStruct result= new IRStruct();
        String conditionObject = n.f1.f0.toString();
        String label = n.f3.f0.toString();
        String mappedName = labelTable.get(myName).get(label);

        String workaround = getTemp();
        result.first.add("li t6, 0");
        result.first.add("bne "+conditionObject+", t6, "+workaround);
        result.first.add("jal t6, "+mappedName);

        result.first.add(workaround+":");

        //result.first.add("beqz "+conditionObject+", "+label);

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
        IRStruct parameterObject = n.f5.accept(this, fStruct);
        String myName= fStruct.functionName;

        int numArg = 6;
        if ( n.f5.present() ) {
            Enumeration <Node> nodeListEnum=n.f5.elements();
            while(nodeListEnum.hasMoreElements() ){
                String parameterId= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                numArg += 1;
            }
        }
        
        String argumentSize= Integer.toString(numArg *4);
        result.first.add("li t6, "+argumentSize);
        result.first.add("sub sp, sp, t6"); //allocate size on stack to pass in parameters

        //load a regs into the stack
        for(int i=2;i<8;++i){
            String aReg= "a"+Integer.toString(i);
            String sAddress= Integer.toString(4*(i-2))+"(sp)";
            result.first.add("sw "+aReg+", "+sAddress);
        }
        //push extra params onto stack
        int extraOffset=24;
        if ( n.f5.present() ) {
            Enumeration <Node> nodeListEnum=n.f5.elements();
            while(nodeListEnum.hasMoreElements() ){
                String parameterId= ((Identifier)nodeListEnum.nextElement()).f0.toString();
                String sAddress= Integer.toString(extraOffset)+"(sp)";
                extraOffset += 4;
                String idStackAddress= variableTable.get(myName).get(parameterId);
                result.first.add("lw t6, "+idStackAddress);
                result.first.add("sw t6, "+sAddress);

            }
        }

        result.first.add("jalr ra, "+functionName+", 0");
        //result.first.add("jalr "+functionName);
        result.first.add("mv "+returnObject+", a0");


        return result;
    
    }

    

    private void printError(String s)
    {
        System.err.println(s);
    }
    

}
