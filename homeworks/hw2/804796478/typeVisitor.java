import java.util.*;
import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.GJDepthFirst;
//TODO: having a class name as a java type

public class typeVisitor extends GJDepthFirst<typeStruct, SymbolTable>

{
        private classStruct currentClass = null;
        private methodStruct currentMethod = null;
        private Stack<methodStruct> methodStack = new Stack<methodStruct>();
        
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
        public typeStruct visit(MainClass n, SymbolTable table) {
            String className = n.f1.f0.toString();
            classStruct mainClass = table.getClass(className);
            String methodName = "main";
            methodStruct mainMethod = mainClass.getMethods().get(methodName);
            this.currentClass = mainClass; 
            this.currentMethod = mainMethod;
            for (Map.Entry<String, typeStruct> entry: mainMethod.getLocalVariables().entrySet())
            {
                typeStruct varType = entry.getValue();
                if (varType.classType && !table.hasClass(varType.type))
                    printError("invalid local var" + varType.type + " in main method " + methodName);
                // check all local var is valid
            }
            
            n.f15.accept(this, table);
            this.currentClass = null;
            this.currentMethod = null;
            return new typeStruct("Statement");
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
        public typeStruct visit(ClassDeclaration n, SymbolTable table) {
            
            String className = n.f1.f0.toString();
            //System.err.println("entering class "+className);
            assert(table.hasClass(className));
            classStruct myClass = table.getClass(className);
            if(myClass==null){
                System.err.println("retrieved class is empty");
            }
            for (Map.Entry<String, typeStruct> entry: myClass.getFields().entrySet())
            {
                typeStruct varType = entry.getValue();
                if (varType.classType && !table.hasClass(varType.type))
                    printError("invalid type " + varType.type + " in class " + className);
                // check all field type is valid
            }
            //System.err.println("finish checking fields of class");
            this.currentClass = myClass; // we are now entering the scope of this class
            n.f4.accept(this, table); // check all methods of this class
            this.currentClass = null;
            //System.err.println("leaving class "+className);
            return new typeStruct("Statement");
        }
        
        @Override
        public typeStruct visit(NodeList n, SymbolTable table) {
            Enumeration <Node> nodeListEnum=n.elements();
            while(nodeListEnum.hasMoreElements() ){
                nodeListEnum.nextElement().accept(this, table);
            }
                    
            return new typeStruct("Statement");
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
        public typeStruct visit(ClassExtendsDeclaration n, SymbolTable table) {
            
            String className = n.f1.f0.toString();
            classStruct myClass = table.getClass(className);
            String parent = myClass.getParent();
            if (! table.hasClass(parent))
                printError("no parent class " + parent);
            
            
            for (Map.Entry<String, typeStruct> entry: myClass.getFields().entrySet())
            {
                typeStruct varType = entry.getValue();
                if (varType.classType && !table.hasClass(varType.type)) //TODO: check if class name in hierachy
                    printError("invalid type " + varType.type + " in class " + className);
                // check all field type is valid
            }
            
            this.currentClass = myClass; // we are now entering the scope of this class
            n.f6.accept(this, table); // check all methods of this class
            this.currentClass = null;
            return new typeStruct("Statement");
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
        public typeStruct visit(MethodDeclaration n, SymbolTable table) {
            
            String methodName = n.f2.f0.toString();
            //System.err.println("entering method: "+methodName);
            assert(currentClass.getMethods().containsKey(methodName));
            methodStruct myMethod = currentClass.getMethods().get(methodName);
            //System.err.println(currentClass.getClassName() + " method " + methodName);
            typeStruct returnType = myMethod.getReturnType();
            if (returnType.classType && !table.hasClass(returnType.type))
                printError("invalid return type for method " + methodName);
            // check return type
            for (Map.Entry<String, typeStruct> entry: myMethod.getLocalVariables().entrySet())
            {
                typeStruct varType = entry.getValue();
                if (varType.classType && !table.hasClass(varType.type))
                    printError("invalid local var" + varType.type + " in method " + methodName);
                // check all local var is valid
            }
            //System.err.println("about to check statements in methods: "+methodName);
            this.currentMethod = myMethod; // we are entering the scope of current method
            n.f8.accept(this, table);
            typeStruct realReturnType = n.f10.accept(this, table);
            
            //System.err.println("return type from method is "+realReturnType.type);
            //System.err.println("expected return type is "+returnType.type);
            if ((! returnType.type.equals(realReturnType.type)) && (! table.checkSubType(returnType, realReturnType)))
                printError("unmatched return type " + returnType.type + " " + realReturnType.type);
            this.currentMethod = null;
            //System.err.println("leaving method: "+methodName);
            return new typeStruct("Statement");
        }

        /**
            * f0 -> Identifier()
            * f1 -> "="
            * f2 -> Expression()
            * f3 -> ";"
            */
        
        @Override
        public typeStruct visit(AssignmentStatement n, SymbolTable table) {
            //System.err.println("entering typecheck of assignment statement");
            typeStruct left = n.f0.accept(this, table);
            typeStruct right = n.f2.accept(this, table);
            //System.err.println("type on left is: "+left.type);
            //System.err.println("type on right is: "+right.type);

            if (left==null || right==null || !(left.type.equals(right.type) || table.checkSubType(left, right))){
                printError("unmatched type in assginmentStatement");
            }

            return new typeStruct("Statement");
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
        public typeStruct visit(ArrayAssignmentStatement n, SymbolTable table) {
            typeStruct result = null;
            typeStruct arrayA = n.f0.accept(this, table);
            typeStruct index = n.f2.accept(this, table);
            typeStruct right = n.f5.accept(this, table);
            if (arrayA!=null && arrayA.arrayType() && index.integerType() && right.integerType()){
                result = new typeStruct("Statement");
            }
            else {
                printError("Error int array assignment");
            }
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
        public typeStruct visit(IfStatement n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct condition = n.f2.accept(this, table);
            n.f4.accept(this, table);
            n.f6.accept(this, table);
            
            if (condition.booleanType())
                result = new typeStruct("Statement");
            else {
                printError("Error in if statement");
            }
            
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
        public typeStruct visit(WhileStatement n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct condition = n.f2.accept(this, table);
            n.f4.accept(this, table);
            if (condition.booleanType())
                result = new typeStruct("Statement");
            else {
                printError("Error in while statement");
            }
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
        public typeStruct visit(PrintStatement n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct content = n.f2.accept(this, table);
            if (content.integerType())
                result = new typeStruct("Statement");
            else {
                printError("Error in print statement");
            }
            return result;
        }

        @Override
        public typeStruct visit(AndExpression n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct first = n.f0.accept(this, table);
            typeStruct second = n.f2.accept(this, table);
            if (first.booleanType() && second.booleanType())
                result = new typeStruct("boolean");
            else {
                printError("non boolean type for andExpression");
            }
            
            return result;
        }

        @Override
        public typeStruct visit(CompareExpression n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct first = n.f0.accept(this, table);
            typeStruct second = n.f2.accept(this, table);
            if (first.integerType() && second.integerType())
                result = new typeStruct("boolean");
            else {
                printError("non boolean type for compareExpression");
            }
            
            return result;
        }

        /**
        * f0 -> PrimaryExpression()
        * f1 -> "+"
        * f2 -> PrimaryExpression()
        */
        @Override
        public typeStruct visit(PlusExpression n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct first = n.f0.accept(this, table);
            typeStruct second = n.f2.accept(this, table);
            if (first.integerType() && second.integerType())
                result = new typeStruct("int");
            else {
                printError("non int type for plusExpression");
            }
            
            return result;
        }
    
        @Override
        public typeStruct visit(MinusExpression n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct first = n.f0.accept(this, table);
            typeStruct second = n.f2.accept(this, table);
            if (first.integerType() && second.integerType())
                result = new typeStruct("int");
            else {
                printError("non int type for minusExpression");
            }
            
            return result;
        }

        @Override
        public typeStruct visit(TimesExpression n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct first = n.f0.accept(this, table);
            typeStruct second = n.f2.accept(this, table);
            if (first.integerType() && second.integerType())
                result = new typeStruct("int");
            else {
                printError("non int type for timesExpression");
            }
            
            return result;
        }

        /**
        * f0 -> PrimaryExpression()
        * f1 -> "["
        * f2 -> PrimaryExpression()
        * f3 -> "]"
        */
        public typeStruct visit(ArrayLookup n, SymbolTable table) {
            
            typeStruct result=null;
            typeStruct op = n.f0.accept(this, table);
            typeStruct index = n.f2.accept(this, table);
            if (op.arrayType() && index.integerType())
                result = new typeStruct("int");
            else {
                printError("non int type for arry Lookup");
            }
            
            return result;
       }

       /**
            * f0 -> PrimaryExpression()
            * f1 -> "."
            * f2 -> "length"
            */
        @Override
        public typeStruct visit(ArrayLength n, SymbolTable table) {
            
            typeStruct result=null;
            typeStruct op = n.f0.accept(this, table);
            if (op.arrayType())
                result = new typeStruct("int");
            else {
                printError("non int type for array Length");
            }
            
            return result;
        }

        @Override
        public typeStruct visit(PrimaryExpression n, SymbolTable table) {
            
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
        public typeStruct visit(MessageSend n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct invokingObject = n.f0.accept(this, table);
            if (!invokingObject.classType || !table.hasClass(invokingObject.type)){
                printError("need an object to invoke method " + invokingObject.type);
            }
            // check method belongs to invoking object
            classStruct callerClass = table.getClass(invokingObject.type);
            String methodName = n.f2.f0.toString();
            if (! callerClass.getMethods().containsKey(methodName)){
                printError("invoking object do not have method " + methodName);
            }

            
            methodStruct myMethod = callerClass.getMethods().get(methodName);// keep track of stack of methods
            
            methodStack.push(myMethod);
            n.f4.accept(this, table);
            methodStack.pop();
            
            result = myMethod.getReturnType();
            return result;
        }
        
        @Override
        public typeStruct visit(NodeListOptional n, SymbolTable table) {
            if ( n.present() ) {
                Enumeration <Node> nodeListEnum=n.elements();
                while(nodeListEnum.hasMoreElements() ){
                    nodeListEnum.nextElement().accept(this, table);
                }
            }
            return new typeStruct("Statement");
        }
        
        @Override
        public typeStruct visit(NodeOptional n, SymbolTable table) {
          if ( n.present() ) {
             n.node.accept(this, table);
        
          }
          return new typeStruct("Statement");
        }
        
        @Override
        public typeStruct visit(NodeSequence n, SymbolTable table) {
            Enumeration <Node> nodeSequenceEnum=n.elements();
            while(nodeSequenceEnum.hasMoreElements() ){
                nodeSequenceEnum.nextElement().accept(this, table);
            }
    
            return new typeStruct("Statement");
        }
        
        /**
         * f0 -> Expression()
         * f1 -> ( ExpressionRest() )*
         */
        @Override
        public typeStruct visit(ExpressionList n, SymbolTable table) {
            typeStruct result = null;
            methodStruct stackMethod = methodStack.peek();
            List<Pair> methodParameters = stackMethod.getParameters();
            if (n == null)
            {
                if (methodParameters.size() != 0){
                    printError("extra parameters for empty function");
                }
            }
            else {
                
                if (methodParameters.size() != 1 + n.f1.size())
                    printError("number of parameters are not same");
                
                typeStruct firstParameter = methodParameters.get(0).second();
                typeStruct firstInput = n.f0.accept(this, table);
                if ((! firstParameter.type.equals(firstInput.type)) && (! table.checkSubType(firstParameter, firstInput)))
                    printError("first parameter type doesn't match");
                
                for (int i = 1; i < methodParameters.size(); i++)
                {
                    typeStruct parameterI = methodParameters.get(i).second();
                    typeStruct inputI = n.f1.elementAt(i - 1).accept(this, table);
                    if ((! parameterI.type.equals(inputI.type)) && (! table.checkSubType(parameterI, inputI)))
                        printError("parameter type doesn't match at " + i);
                }
                
            }
            
            return result;
        }
        
        /**
            * f0 -> <IDENTIFIER>
            */
        @Override
        public typeStruct visit(Identifier n, SymbolTable table) {
            
            typeStruct result = null;
            String id = n.f0.toString();
            //System.err.println("resolving identifier "+id);
            //id is a class name
            if(table.hasClass(id)){
                result= new typeStruct(id,1);//return a type with the class name
                return result;
            }
            // id is a field variable of current class
            if (currentClass != null){
                result = currentClass.getFields().get(id);
                if(result!=null){return result;}
            }
            // id is a local variable of the current method
            if (currentMethod != null)
            {
                /*
                System.err.println("local variables in "+currentMethod.getMethodName()+ " are : ");
                for (String variableName: currentMethod.getLocalVariables().keySet()){
                    System.err.println(variableName + " " + currentMethod.getLocalVariables().get(variableName).type);
                }*/
                if (currentMethod.getLocalVariables().containsKey(id)){
                    result = currentMethod.getLocalVariables().get(id);
                }
            }
            
            return result;
        }
        
        
        
        @Override
        public typeStruct visit(IntegerLiteral n, SymbolTable table) {

            return new typeStruct("int");
        }

        @Override
        public typeStruct visit(TrueLiteral n, SymbolTable table) {
            
            return new typeStruct("boolean");
        }
        
        @Override
        public typeStruct visit(FalseLiteral n, SymbolTable table) {
            
            return new typeStruct("boolean");
        }
        
        
        
         
        
        @Override
        public typeStruct visit(ExpressionRest n, SymbolTable table) {
            
            return n.f1.accept(this, table);
        }
        
        @Override
        public typeStruct visit(ThisExpression n, SymbolTable table) {
            // TODO Auto-generated method stub
            return new typeStruct(currentClass.getClassName(), 1);
        }
        
        /**
            * f0 -> "new"
            * f1 -> "int"
            * f2 -> "["
            * f3 -> Expression()
            * f4 -> "]"
            */
        @Override
        public typeStruct visit(ArrayAllocationExpression n, SymbolTable table) {
            
            typeStruct result = null;
            typeStruct index = n.f3.accept(this, table);
            if (index.integerType())
                result = new typeStruct("array");
            else {
                printError("need integer length when allocating an array");
            }
            return result;
        }
        
        /**
            * f0 -> "!"
            * f1 -> Expression()
            */
        @Override
        public typeStruct visit(NotExpression n, SymbolTable table) {
            typeStruct result = null;
            typeStruct operand = n.f1.accept(this, table);
            if (operand.booleanType())
                result = new typeStruct("boolean");
            else {
                printError("error in not expression " + operand.type);
            }
            return result;
        }

        /**
            * f0 -> "new"
            * f1 -> Identifier()
            * f2 -> "("
            * f3 -> ")"
            */
        @Override
        public typeStruct visit(AllocationExpression n, SymbolTable table) {
            
            typeStruct result = null;
            String className = n.f1.f0.toString();
            if (table.hasClass(className))
            {
                result = new typeStruct(className, 1);
            }
            else {
                printError("cannot create instance of non class type");
            }
            
            return result;
        }
       

        @Override
        public typeStruct visit(BracketExpression n, SymbolTable table) {
            
            return n.f1.accept(this, table);
        }
        
        


        @Override
        public typeStruct visit(Expression n, SymbolTable table) {
            
            return n.f0.accept(this, table);
        }
        
        private void printError(String s)
        {
            System.out.println("Type error");
            System.err.println(s);
            System.exit(1);
        }
  
    
}
