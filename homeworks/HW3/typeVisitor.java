import java.util.*;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.GJDepthFirst;
//TODO: visit identifier and deal with class type as field or variable
//TODO: set currentClass variable
//f4 in messageSend
//this does not work with main class
//TODO: currently not filling in 0 for alloc in array, if not work, change later
public class typeVisitor extends GJDepthFirst<SparrowStruct, SymbolTable>

{       
        int varCounter=0;
        private classStruct currentClass = null;
        private methodStruct currentMethod = null;
        private Stack<methodStruct> methodStack = new Stack<methodStruct>();
        public HashMap<String, String> idToClass = new HashMap<String, String>();
        
        public String getTemp(){
            String result= "v"+Integer.toString(varCounter);
            varCounter=varCounter+1;
            return result;
        }
         /**
        * f0 -> "class"
        * f1 -> Identifier()
        * f2 -> "{"
        * f3 -> "public"
        * f4 -> "static"
        * f5 -> "void"
        * f6 -> "main"
        * f7 -> "("
        * f8 -> "String"
        * f9 -> "["
        * f10 -> "]"
        * f11 -> Identifier()
        * f12 -> ")"
        * f13 -> "{"
        * f14 -> ( VarDeclaration() )*
        * f15 -> ( Statement() )*
        * f16 -> "}"
        * f17 -> "}"
        */
        @Override
        public SparrowStruct visit(MainClass n, SymbolTable table) {
            SparrowStruct result = new SparrowStruct();
            String className = n.f1.f0.toString();
            classStruct mainClass = table.getClass(className);
            String header="func "+className+"main()";
            result.first.add(header);

            String methodName = "main";
            methodStruct mainMethod = mainClass.getMethods().get(methodName);
            this.currentClass = mainClass; 
            this.currentMethod = mainMethod;
            
            SparrowStruct methodObject= n.f15.accept(this, table);
            
            for(ArrayList<String>x:methodObject.optionalList){
                for(String y:x){
                    
                    //System.out.println(y);
                    result.first.add(y);
                }
            }
            result.first.add("trivial = 0");
            result.first.add("return trivial");

            this.currentClass = null;
            this.currentMethod = null;
            return result;
        }

        @Override
        public SparrowStruct visit(Goal n, SymbolTable argu) {
            SparrowStruct result=new SparrowStruct();
            SparrowStruct mainObject=n.f0.accept(this, argu);
            result.first=mainObject.first;

            SparrowStruct classObject=n.f1.accept(this, argu);
            for(int i=0;i<classObject.optionalList.size();i++){
                result.first.addAll(classObject.optionalList.get(i));
            }
            n.f2.accept(this, argu); 
            return result;
        }

        @Override 
        public SparrowStruct visit(TypeDeclaration n, SymbolTable table){
            SparrowStruct result= n.f0.choice.accept(this, table);

            return result;
        }

        /**
            * f0 -> "class"
            * f1 -> Identifier()
            * f2 -> "{"
            * f3 -> ( VarDeclaration() )*
            * f4 -> ( MethodDeclaration() )*
            * f5 -> "}"
            */
        @Override
        public SparrowStruct visit(ClassDeclaration n, SymbolTable table) {
            this.idToClass.clear();
            SparrowStruct result = new SparrowStruct();
            String className = n.f1.f0.toString();

            assert(table.hasClass(className));
            classStruct myClass = table.getClass(className);
            
            //omitting fields

            this.currentClass = myClass; // we are now entering the scope of this class
            SparrowStruct methodObject=n.f4.accept(this, table); // check all methods of this class
            for(ArrayList<String>x:methodObject.optionalList){
                result.first.addAll(x);
            }
            this.currentClass = null;

            
            this.idToClass.clear();
            return result;
        }
        
        @Override
        public SparrowStruct visit(NodeList n, SymbolTable table) {
            Enumeration <Node> nodeListEnum=n.elements();
            while(nodeListEnum.hasMoreElements() ){
                nodeListEnum.nextElement().accept(this, table);
            }
                    
            return new SparrowStruct();
            // Statement is a primitive structure
        }

        /**
            * f0 -> "class"
            * f1 -> Identifier()
            * f2 -> "extends"
            * f3 -> Identifier()
            * f4 -> "{"
            * f5 -> ( VarDeclaration() )*
            * f6 -> ( MethodDeclaration() )*
            * f7 -> "}"
            */
        @Override
        public SparrowStruct visit(ClassExtendsDeclaration n, SymbolTable table) {
            this.idToClass.clear();
            SparrowStruct result = new SparrowStruct();
            String className = n.f1.f0.toString();

            classStruct myClass = table.getClass(className);

            this.currentClass = myClass; // we are now entering the scope of this class
            SparrowStruct methodObject=n.f6.accept(this, table); // check all methods of this class
            for(ArrayList<String>x:methodObject.optionalList){
                result.first.addAll(x);
            }
            this.currentClass = null;


            this.idToClass.clear();
            return result;
        }

        /**
            * f0 -> "public"
            * f1 -> Type()
            * f2 -> Identifier()
            * f3 -> "("
            * f4 -> ( FormalParameterList() )?
            * f5 -> ")"
            * f6 -> "{"
            * f7 -> ( VarDeclaration() )*
            * f8 -> ( Statement() )*
            * f9 -> "return"
            * f10 -> Expression()
            * f11 -> ";"
            * f12 -> "}"
            */
        @Override
        public SparrowStruct visit(MethodDeclaration n, SymbolTable table) {
            SparrowStruct result=new SparrowStruct();
            String methodName = n.f2.f0.toString();
            String header="func "+currentClass.getClassName()+methodName+"(this";
            

            assert(currentClass.getMethods().containsKey(methodName));
            methodStruct myMethod = currentClass.getMethods().get(methodName);

            typeStruct returnType = myMethod.getReturnType();
            if (returnType.classType && !table.hasClass(returnType.type))
                printError("invalid return type for method " + methodName);
            // check return type
            for (Pair p: myMethod.getParameters())
            {
                header=header+" "+p.first();//fill in func parameters
            }
            header=header+")";
            result.first.add(header);


            this.currentMethod = myMethod; // we are entering the scope of current method
            SparrowStruct statementObject = n.f8.accept(this, table);
            for(int i=0;i<statementObject.optionalList.size();i++){
                result.first.addAll(statementObject.optionalList.get(i));
            }
            SparrowStruct returnObject = n.f10.accept(this, table);
            result.first.addAll(returnObject.first);
            result.second=returnObject.second;
            result.first.add("return "+returnObject.second);
            
            result.rawClassName= returnObject.rawClassName;
            this.currentMethod = null;

            return result;
        }

        /**
            * f0 -> Identifier()
            * f1 -> "="
            * f2 -> Expression()
            * f3 -> ";"
            */
        
        @Override
        public SparrowStruct visit(AssignmentStatement n, SymbolTable table) {
            SparrowStruct result=new SparrowStruct();
            SparrowStruct left = n.f0.accept(this, table);

            SparrowStruct right = n.f2.accept(this, table);
            result.first=right.first;

            if(left.isField && currentMethod!= null){//statement is either in method or mainClass
                int position=currentClass.fieldPos.get(n.f0.f0.toString());
                
                result.first.add("[this + "+Integer.toString(4*position)+"] = "+right.second);
            }
            else{
                result.first.add(left.second+" = "+right.second);
            }

            if(right.rawClassName != ""){
                this.idToClass.put(n.f0.f0.toString() , right.rawClassName);
            }
            return result;

        }

        /**
         * Grammar production:
         * f0 -> "{"
         * f1 -> ( Statement() )*
         * f2 -> "}"
         */
        @Override
        public SparrowStruct visit(Block n, SymbolTable table){
            SparrowStruct result= new SparrowStruct();
            SparrowStruct optionalObject = n.f1.accept(this,table);
            for(ArrayList<String>x: optionalObject.optionalList){
                result.first.addAll(x);
            }
            return result;
        }

        /**
         * Grammar production:
         * f0 -> Block()
         *       | AssignmentStatement()
         *       | ArrayAssignmentStatement()
         *       | IfStatement()
         *       | WhileStatement()
         *       | PrintStatement()
         */
        @Override
        public SparrowStruct visit(Statement n, SymbolTable table){
            SparrowStruct result=n.f0.choice.accept(this,table);
            return result;
        }

        /**
            * f0 -> Identifier()
            * f1 -> "["
            * f2 -> Expression()
            * f3 -> "]"
            * f4 -> "="
            * f5 -> Expression()
            * f6 -> ";"
            */
        @Override
        public SparrowStruct visit(ArrayAssignmentStatement n, SymbolTable table) {
            SparrowStruct result= new SparrowStruct();

            SparrowStruct expObject= n.f5.accept(this, table);
            result.first=expObject.first;

            SparrowStruct indexObject= n.f2.accept(this, table);
            result.first.addAll(indexObject.first);

            SparrowStruct arrayObject= n.f0.accept(this, table);
            result.first.addAll(arrayObject.first);

            String fourVar=getTemp();
            result.first.add(fourVar+" = 4");
            String fiveVar=getTemp();
            result.first.add(fiveVar+" = 5");
            String negOneVar=getTemp();
            result.first.add(negOneVar+" = "+fourVar+" - "+fiveVar);


            String lowVar=getTemp();
            result.first.add(lowVar+" = "+ negOneVar+" < "+indexObject.second);

            String arrayLengthVar=getTemp();
            result.first.add(arrayLengthVar+" = ["+arrayObject.second+" + 0]");
            String highVar=getTemp();
            result.first.add(highVar+" = "+indexObject.second+" < "+arrayLengthVar);

            String evaluation= getTemp();
            result.first.add(evaluation+" = "+lowVar+" * "+highVar);
            result.first.add("if0 "+evaluation+" goto "+lowVar+"Error");
            result.first.add("goto "+lowVar+"End");

            result.first.add(lowVar+"Error:");
            //result.first.add("print(\"--- Error (Explicit) ---\")");
            result.first.add("error(\"array index out of bounds\")");
            //TODO: i hope sparrow exit exit prgram by calling error

            result.first.add(lowVar+"End:");
            String offset=getTemp();
            result.first.add(offset+" = "+fourVar+" * "+indexObject.second);
            result.first.add(offset+" = "+offset+" + "+fourVar);

            String elementVar=getTemp();
            result.first.add(elementVar+" = "+arrayObject.second+" + "+offset);
            result.first.add("["+elementVar+" + 0] = "+expObject.second);
            result.second = elementVar; //shouldn't matter, trash value

            return result;
            
            
        }

        /**
            * f0 -> "if"
            * f1 -> "("
            * f2 -> Expression()
            * f3 -> ")"
            * f4 -> Statement()
            * f5 -> "else"
            * f6 -> Statement()
            */
        @Override
        public SparrowStruct visit(IfStatement n, SymbolTable table) {
            
            SparrowStruct result=new SparrowStruct();
            SparrowStruct condition = n.f2.accept(this, table);
            result.first=condition.first;

            result.first.add("if0 "+condition.second+" goto "+condition.second+"_else");
            SparrowStruct ifResult=n.f4.accept(this, table);
            for(String x:ifResult.first){result.first.add(x);}

            result.first.add("goto "+condition.second+"_end");

            result.first.add(condition.second+"_else:");
            SparrowStruct elseResult=n.f6.accept(this, table);
            for(String x:elseResult.first){result.first.add(x);}

            result.first.add(condition.second+"_end:");

            return result;
            
        }

        /**
            * f0 -> "while"
            * f1 -> "("
            * f2 -> Expression()
            * f3 -> ")"
            * f4 -> Statement()
            */
        @Override
        public SparrowStruct visit(WhileStatement n, SymbolTable table) {
            SparrowStruct result= new SparrowStruct();
            String loopVariable=getTemp();
            result.first.add("loop"+loopVariable+":");

            SparrowStruct condition= n.f2.accept(this, table);
            result.first.addAll(condition.first);

            result.first.add("if0 "+condition.second+" goto "+loopVariable+"_end");
            SparrowStruct statementObject= n.f4.accept(this, table);
            result.first.addAll(statementObject.first);
            result.first.add("goto loop"+loopVariable);

            result.first.add(loopVariable+"_end:");
            return result;
            
        }

        /**
            * f0 -> "System.out.println"
            * f1 -> "("
            * f2 -> Expression()
            * f3 -> ")"
            * f4 -> ";"
            */
        @Override
        public SparrowStruct visit(PrintStatement n, SymbolTable table) {
            SparrowStruct result=new SparrowStruct();

            SparrowStruct content = n.f2.accept(this, table);

            result.first=content.first;
            result.first.add("print("+content.second+")");
            result.second="";
            System.err.println("result first has a size "+Integer.toString(result.first.size()));

            return result;
        }

        @Override
        public SparrowStruct visit(AndExpression n, SymbolTable table) {
            //two boolean
            SparrowStruct result=new SparrowStruct();
            SparrowStruct leftResult = n.f0.accept(this, table);
            SparrowStruct rightResult = n.f2.accept(this, table);
            result.first=leftResult.first;
            result.first.addAll(rightResult.first);
            result.second=getTemp();
            result.first.add(result.second+" = "+leftResult.second+" * "+rightResult.second);
            return result;
        }

        //TODO: COMPARE,plus, minus etc. to deal with fields
        @Override
        public SparrowStruct visit(CompareExpression n, SymbolTable table) {
            //must be two ints
            SparrowStruct result=new SparrowStruct();
            SparrowStruct leftResult = n.f0.accept(this, table);
            SparrowStruct rightResult = n.f2.accept(this, table);
            result.first=leftResult.first;
            result.first.addAll(rightResult.first);
            result.second=getTemp();
            result.first.add(result.second+" = "+leftResult.second+" < "+rightResult.second);
            return result;

        }

        /**
        * f0 -> PrimaryExpression()
        * f1 -> "+"
        * f2 -> PrimaryExpression()
        */
        @Override
        public SparrowStruct visit(PlusExpression n, SymbolTable table) {
            SparrowStruct result = new SparrowStruct();
            SparrowStruct left = n.f0.accept(this, table);
            SparrowStruct right = n.f2.accept(this, table);
            
            result.first=left.first;
            result.first.addAll(right.first);
            result.second=getTemp();
            result.first.add(result.second+" = "+left.second+" + "+right.second);
            return result;
        }
    
        @Override
        public SparrowStruct visit(MinusExpression n, SymbolTable table) {
            //two ints type
            SparrowStruct result = new SparrowStruct();
            SparrowStruct left = n.f0.accept(this, table);
            SparrowStruct right = n.f2.accept(this, table);
            
            result.first=left.first;
            result.first.addAll(right.first);
            result.second=getTemp();
            result.first.add(result.second+" = "+left.second+" - "+right.second);
            return result;

        }

        /**
         * Grammar production:
         * f0 -> PrimaryExpression()
         * f1 -> "*"
         * f2 -> PrimaryExpression()
         */
        @Override
        public SparrowStruct visit(TimesExpression n, SymbolTable table) {
            //two ints type
            SparrowStruct result = new SparrowStruct();
            SparrowStruct left = n.f0.accept(this, table);
            SparrowStruct right = n.f2.accept(this, table);
            
            result.first=left.first;
            result.first.addAll(right.first);

            result.second=getTemp();
            result.first.add(result.second+" = "+left.second+" * "+right.second);
            return result;
        }

        /**
        * f0 -> PrimaryExpression()
        * f1 -> "["
        * f2 -> PrimaryExpression()
        * f3 -> "]"
        */
        public SparrowStruct visit(ArrayLookup n, SymbolTable table) {
            SparrowStruct result= new SparrowStruct();
            SparrowStruct indexObject= n.f2.accept(this, table);
            result.first=indexObject.first;

            SparrowStruct arrayObject= n.f0.accept(this, table);
            result.first.addAll(arrayObject.first);

            String fourVar=getTemp();
            result.first.add(fourVar+" = 4");
            String fiveVar=getTemp();
            result.first.add(fiveVar+" = 5");
            String negOneVar=getTemp();
            result.first.add(negOneVar+" = "+fourVar+" - "+fiveVar);


            String lowVar=getTemp();
            result.first.add(lowVar+" = "+ negOneVar+" < "+indexObject.second);

            String arrayLengthVar=getTemp();
            result.first.add(arrayLengthVar+" = ["+arrayObject.second+" + 0]");
            String highVar=getTemp();
            result.first.add(highVar+" = "+indexObject.second+" < "+arrayLengthVar);

            String evaluation= getTemp();
            result.first.add(evaluation+" = "+lowVar+" * "+highVar);
            result.first.add("if0 "+evaluation+" goto "+lowVar+"Error");
            result.first.add("goto "+lowVar+"End");

            result.first.add(lowVar+"Error:");
            //result.first.add("print(\"--- Error (Explicit) ---\")");
            result.first.add("error(\"array index out of bounds\")");
            //TODO: i hope sparrow exit exit prgram by calling error

            result.first.add(lowVar+"End:");
            String offset=getTemp();
            result.first.add(offset+" = "+fourVar+" * "+indexObject.second);
            result.first.add(offset+" = "+offset+" + "+fourVar);

            String elementVar=getTemp();
            result.first.add(elementVar+" = "+arrayObject.second+" + "+offset);
            result.first.add(elementVar+" = ["+elementVar+" + 0]");
            result.second = elementVar;

            return result;
       }

       /**
            * f0 -> PrimaryExpression()
            * f1 -> "."
            * f2 -> "length"
            */
        @Override
        public SparrowStruct visit(ArrayLength n, SymbolTable table) {
            SparrowStruct result = new SparrowStruct();
            SparrowStruct arrayObject = n.f0.accept(this, table);

            String lengthVar=getTemp();
            result.first.add(lengthVar="["+arrayObject.second+" + 0]");
            result.second=lengthVar;
            return result;
        }

        @Override
        public SparrowStruct visit(PrimaryExpression n, SymbolTable table) {
            
            return n.f0.choice.accept(this, table);
        }

        /**
            * f0 -> PrimaryExpression()
            * f1 -> "."
            * f2 -> Identifier()
            * f3 -> "("
            * f4 -> ( ExpressionList() )?
            * f5 -> ")"
            */
        @Override
        public SparrowStruct visit(MessageSend n, SymbolTable table) {

            SparrowStruct result = new SparrowStruct();
            SparrowStruct invokingObject = n.f0.accept(this, table);
            result.first=invokingObject.first;
            String functionVar=getTemp();

            if(invokingObject.rawClassName == ""){
                if( ! this.idToClass.containsKey( ((Identifier)n.f0.f0.choice).f0.toString() )){
                    result.first.add("goto "+functionVar+"Error");
                    result.first.add(functionVar+"Error:");
                    result.first.add("error(\"null pointer\")");
                    invokingObject.rawClassName = "shit"; 
                }
                else{
                   invokingObject.rawClassName = this.idToClass.get( ((Identifier)n.f0.f0.choice).f0.toString() ); 
                }
                 //3 ways to have a class parameter: new Class(), predefined x= new Class() 
            }//dealing with call return class type at line 630

            
            result.first.add(functionVar+" = ["+invokingObject.second+" + 0]");

            String methodName = n.f2.f0.toString();
            classStruct myClass= table.getClass(invokingObject.rawClassName);
            int position=myClass.methodPos.get(methodName);
            result.first.add(functionVar+" = "+"["+functionVar+" + "+Integer.toString(4*position)+"]");

            
            methodStruct myMethod = myClass.getMethods().get(methodName);// keep track of stack of methods
            
            methodStack.push(myMethod);

            String callVar=getTemp();
            String partial=callVar+" = call "+functionVar+"("+invokingObject.second;
            SparrowStruct parameterObject = n.f4.accept(this, table);
            for(int i=0;i<parameterObject.optionalResult.size();i++){
                result.first.addAll(parameterObject.optionalList.get(i));
                partial=partial+" "+parameterObject.optionalResult.get(i);
            }
            methodStack.pop();

            partial=partial+")";
            result.first.add(partial);
            result.second=callVar;

            if(myClass.methods.get(methodName).returnType.classType){
                result.rawClassName= myClass.methods.get(methodName).returnType.type;
            }

            return result;
        }
        
        @Override
        public SparrowStruct visit(NodeListOptional n, SymbolTable table) {
            SparrowStruct result=new SparrowStruct();
            System.err.println("entering nodelistoptional");
            if ( n.present() ) {
                Enumeration <Node> nodeListEnum=n.elements();
                while(nodeListEnum.hasMoreElements() ){
                    SparrowStruct small=nodeListEnum.nextElement().accept(this, table);

                    result.optionalList.add(small.first);

                    result.optionalResult.add(small.second);

                }

                return result;
            }
            return result;
        }
        
        @Override
        public SparrowStruct visit(NodeOptional n, SymbolTable table) {
          SparrowStruct result=new SparrowStruct();
          if ( n.present() ) {
             return n.node.accept(this, table);
          }
          return result;
        }
        
        @Override
        public SparrowStruct visit(NodeSequence n, SymbolTable table) {
            Enumeration <Node> nodeSequenceEnum=n.elements();
            while(nodeSequenceEnum.hasMoreElements() ){
                nodeSequenceEnum.nextElement().accept(this, table);
            }
    
            return new SparrowStruct();
        }
        
        /**
         * f0 -> Expression()
         * f1 -> ( ExpressionRest() )* -->NodeListOptional
         */
        @Override
        public SparrowStruct visit(ExpressionList n, SymbolTable table) {
            SparrowStruct result = new SparrowStruct();
            methodStruct stackMethod = methodStack.peek();
            List<Pair> methodParameters = stackMethod.getParameters();
            if (n == null)
            {
                if (methodParameters.size() != 0){
                    printError("extra parameters for empty function");
                }
                return result; //nothing
            }
            else {
                
                if (methodParameters.size() != 1 + n.f1.size())
                    printError("number of parameters are not same");
                
                typeStruct firstParameter = methodParameters.get(0).second();
                SparrowStruct firstInput = n.f0.accept(this, table);
                result.optionalList.add(firstInput.first);
                result.optionalResult.add(firstInput.second);
                
                for (int i = 1; i < methodParameters.size(); i++)
                {
                    SparrowStruct inputI = n.f1.elementAt(i - 1).accept(this, table);
                    result.optionalList.add(inputI.first);
                    result.optionalResult.add(inputI.second);
                }
                return result;
            }

        }
        
        /**
            * f0 -> <IDENTIFIER>
            */
        @Override
        public SparrowStruct visit(Identifier n, SymbolTable table) {
            
            SparrowStruct result = new SparrowStruct();
            String id = n.f0.toString();


            // id is a field variable of current class
            
            // id is a local variable of the current method
            if (currentMethod != null)
            {

                if (currentMethod.getLocalVariables().containsKey(id)){
                    result.isField=false;
                    result.second=id;
                    if(currentMethod.localVariables.get(id).classType){
                        result.rawClassName = currentMethod.localVariables.get(id).type;
                    }
                }
                else{
                    if (currentClass != null){
                        if(currentClass.getFields().get(id)!=null){
                            result.isField=true;
                            int position= currentClass.fieldPos.get(id);
                            String idVar= getTemp();
                            result.first.add(idVar+" = "+"[this + "+Integer.toString(4*position)+"]");
                            result.second= idVar;
                        }
                    }
                }
            }
            if(result.second == ""){result.second= id;}
            return result;
        }
        
        
        
        @Override
        public SparrowStruct visit(IntegerLiteral n, SymbolTable table) {
            String intString=n.f0.toString();
            String nextVar=getTemp();
            SparrowStruct result=new SparrowStruct();
            result.first.add(nextVar+" = "+intString);
            result.second=nextVar;
            return result;
        }

        @Override
        public SparrowStruct visit(TrueLiteral n, SymbolTable table) {
            
            String intString="1";
            String nextVar=getTemp();
            SparrowStruct result=new SparrowStruct();
            result.first.add(nextVar+" = "+intString);
            result.second=nextVar;
            return result;
        }
        
        @Override
        public SparrowStruct visit(FalseLiteral n, SymbolTable table) {
            
            String intString="0";
            String nextVar=getTemp();
            SparrowStruct result=new SparrowStruct();
            result.first.add(nextVar+" = "+intString);
            result.second=nextVar;
            return result;
        }
        
        
        
         
        
        @Override
        public SparrowStruct visit(ExpressionRest n, SymbolTable table) {
            
            return n.f1.accept(this, table);
        }
        
        @Override
        public SparrowStruct visit(ThisExpression n, SymbolTable table) {
            // TODO Auto-generated method stub
            SparrowStruct result=new SparrowStruct();
            result.second="this";
            result.rawClassName=this.currentClass.getClassName();
            return result;
        }
        
        /**
            * f0 -> "new"
            * f1 -> "int"
            * f2 -> "["
            * f3 -> Expression()
            * f4 -> "]"
            */
        @Override
        public SparrowStruct visit(ArrayAllocationExpression n, SymbolTable table) {
            SparrowStruct result=new SparrowStruct();
            SparrowStruct expObject=n.f3.accept(this, table);
            result.first=expObject.first;

            String oneVar=getTemp();
            result.first.add(oneVar+" = 1");
            String fourVar=getTemp();
            result.first.add(fourVar+" = 4");
            String totalLengthVar=getTemp();
            result.first.add(totalLengthVar+" = "+expObject.second+" + "+oneVar);
            result.first.add(totalLengthVar+" = "+totalLengthVar+" * "+fourVar);

            String arrayVar=getTemp();
            result.first.add(arrayVar+" = alloc( "+totalLengthVar+")"); 
            result.second=arrayVar;

            result.first.add("if0 "+arrayVar+" goto "+arrayVar+"Error");
            result.first.add("goto "+arrayVar+"End");

            result.first.add(arrayVar+"Error:");
            result.first.add("error(\"null pointer\")");

            result.first.add(arrayVar+"End:");

            result.first.add("["+arrayVar+" + 0] = "+expObject.second); //expObject.second has the array length
            return result;
        }
        
        /**
            * f0 -> "!"
            * f1 -> Expression()
            */
        @Override
        public SparrowStruct visit(NotExpression n, SymbolTable table) {
            SparrowStruct result = new SparrowStruct();
            SparrowStruct boolObject = n.f1.accept(this, table);
            
            result.first=boolObject.first;
            String oneVar=getTemp();
            result.first.add(oneVar+" = 1");

            result.second=getTemp();
            result.first.add(result.second+" = "+oneVar+" - "+boolObject.second);
            return result;
        }

        /**
            * f0 -> "new"
            * f1 -> Identifier()
            * f2 -> "("
            * f3 -> ")"
            */
        @Override
        public SparrowStruct visit(AllocationExpression n, SymbolTable table) {

            SparrowStruct result = new SparrowStruct();
            String className = n.f1.f0.toString();

            String lengthVar=getTemp();
            classStruct myClass=table.getClass(className);
            int totalLength=1+myClass.getFields().size();
            result.first.add(lengthVar+" = "+Integer.toString(4*totalLength));

            String classVar=getTemp();
            result.first.add(classVar+" = alloc("+lengthVar+")");

            int methodLength=myClass.methodIndex;
            String methodLengthVar=getTemp();
            if(methodLength!=0){
                result.first.add(methodLengthVar+" = "+Integer.toString(4*methodLength));
                result.first.add("vmt_"+className+" = alloc("+methodLengthVar+")");
                for(String myMethod:myClass.methodPos.keySet()){
                    String methodVariable=getTemp();
                    if(!myClass.parentMethods.contains(myMethod)){
                        result.first.add(methodVariable+" = @"+className+myMethod);
                        result.first.add("[vmt_"+className+" + "+Integer.toString(4*(myClass.methodPos.get(myMethod)))+" ] = "+methodVariable);
                    }
                    else{ //inherited method from parent
                        result.first.add(methodVariable+" = @"+myClass.parent+myMethod);
                        result.first.add("[vmt_"+className+" + "+Integer.toString(4*(myClass.methodPos.get(myMethod)))+" ] = "+methodVariable);
                    }
                }
                result.first.add("["+classVar+" + 0] = vmt_"+className);
            }

            result.second=classVar;
            result.rawClassName=className;

            result.first.add("if0 "+classVar+" goto "+classVar+"Error");
            result.first.add("goto "+classVar+"End");

            result.first.add(classVar+"Error:");
            result.first.add("error(\"null pointer\")");

            result.first.add(classVar+"End:");

            return result;
            //stored class name as second, might be something else?
        }
       

        @Override
        public SparrowStruct visit(BracketExpression n, SymbolTable table) {
            
            return n.f1.accept(this, table);
        }
        
        


        @Override
        public SparrowStruct visit(Expression n, SymbolTable table) {
            
            return n.f0.choice.accept(this, table);
        }
        
        private void printError(String s)
        {
            System.out.println("Type error");
            System.err.println(s);
            System.exit(1);
        }
  
    
}
