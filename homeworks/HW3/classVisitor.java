import cs132.minijava.syntaxtree.*;
import cs132.minijava.visitor.GJVoidDepthFirst;
import java.util.*;

public class classVisitor extends GJVoidDepthFirst<classStruct> {
	int variableCounter=0;
	private Map<String, classStruct> classTable = new HashMap<String, classStruct>();
	public Map<String, classStruct> getClassTable() {
		return classTable;
	}

	public String getTemp(){
		String result= "v"+Integer.toString(variableCounter);
		variableCounter=variableCounter+1;
		return result;
	}

	public void setClassTable(Map<String, classStruct> classTable) {
		this.classTable = classTable;
	}

	/**
	Goal::=	MainClass ( TypeDeclaration )* <EOF>
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    use default visitor from GJVoidDepthFirst for visitr(Goal,type)
    */

    /**
	    MainClass	::=	"class" Identifier "{" "public" "static" "void" "main" "(" "String" "[" "]" Identifier ")" "{" ( VarDeclaration )* ( Statement )* "}" "}"
	    * f0 -> "class"
	    * f1 -> Identifier(), f1.f0 -> NodeToken
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
	public void visit(MainClass n, classStruct passedClass) {
		
		String mainClassName = n.f1.f0.toString();
		classStruct mainClassObject = new classStruct(mainClassName); //initialized classStruct to null, pass in initialized classStruct
		classTable.put(mainClassName, mainClassObject);
		methodStruct mainMethod = new methodStruct("main", new typeStruct("void"));
		mainClassObject.getMethods().put("main", mainMethod);
		
		String parameterName = n.f11.f0.toString();
		mainMethod.getParameters().add(new Pair(parameterName, new typeStruct("array")));
		mainMethod.getLocalVariables().put(parameterName, new typeStruct("array"));
		
		mainClassObject.setVisitingMethod(true);
		mainClassObject.setCurrentMethod("main");

		n.f14.accept(this, mainClassObject); 

		mainClassObject.setVisitingMethod(false);
		mainClassObject.setCurrentMethod(null);
	
	}

	/**
	    * f0 -> "class"
	    * f1 -> Identifier()
	    * f2 -> "{"
	    * f3 -> ( VarDeclaration() )* -> NodeListOptional
	    * f4 -> ( MethodDeclaration() )* ->NodeListOptional
	    * f5 -> "}"
	    */
	
	@Override
	public void visit(ClassDeclaration n, classStruct passedClass) {
		
		String currentClassName = n.f1.f0.toString();
		classStruct currentClass = new classStruct(currentClassName);
		if (! classTable.containsKey(currentClassName))
			classTable.put(currentClassName, currentClass);
		else {
			printError("Class " + currentClassName + " is defined twice");
		}
		n.f3.accept(this, currentClass);  // default visitor of NodeListOptional, each varDeclaration is visited	
		n.f4.accept(this, currentClass);  // each Method declaration is visited
		
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
	public void visit(ClassExtendsDeclaration n, classStruct passedClass) {
		
		String className = n.f1.f0.toString();
		classStruct currentClass = new classStruct(className);
		if (! classTable.containsKey(className))
			classTable.put(className, currentClass);
		else {
			printError("Class " + className + " is declared twice");
		}
		
		String parentName = n.f3.f0.toString(); // set parent class name
		if (!currentClass.setParent(parentName)){
			printError("Class " + className + " is extending two different classes");
		}
		
		n.f5.accept(this, currentClass);  
		n.f6.accept(this, currentClass);  
	}

	@Override
	/**
	    * f0 -> Type()->f0->NodeChoice
	    * f1 -> Identifier()->f0->NodeToken
	    * f2 -> ";"
	    */
	public void visit(VarDeclaration n, classStruct passedClass) {
		
		typeStruct type = getJavaTypeFromChoice(n.f0.f0);
		String id = n.f1.f0.toString();
		if (!passedClass.getVisitingMethod() ) //not inside a method
		{
			if (! passedClass.getFields().containsKey(id)){
				passedClass.getFields().put(id, type);
				passedClass.markField(id);
			}
			else {
				printError("class variable " + id + " is re-declared");		
			}
		}
		
		else {
			methodStruct currentMethod = passedClass.getMethods().get(passedClass.getCurrentMethod());
			if (! currentMethod.getLocalVariables().containsKey(id)){
				currentMethod.getLocalVariables().put(id, type);
			}
			else {
				printError("Local variable " + id + " is declared twice in method " + currentMethod.getMethodName());
			}
		}
		
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
	
	//TODO: 
	//fill in information of return type, return type not guaranteed to be correct
	@Override
	public void visit(MethodDeclaration n, classStruct passedClass) {
		
		typeStruct return_type = getJavaTypeFromChoice(n.f1.f0);
		String method_name = n.f2.f0.toString();
		methodStruct curr_method = new methodStruct(method_name, return_type);
		if (passedClass.getMethods().containsKey(method_name)){
			// check method re-declaration in the current class
			printError("method redeclaration in current class: " + method_name);
		}
		
		passedClass.getMethods().put(method_name, curr_method);
		passedClass.markMethod(method_name);
		passedClass.setVisitingMethod(true);
		passedClass.setCurrentMethod(method_name);
		
		n.f4.accept(this, passedClass);  
		n.f7.accept(this, passedClass);  
		
		passedClass.setVisitingMethod(false);
		passedClass.setCurrentMethod(null);
		
	}

	@Override
	public void visit(FormalParameterList n, classStruct passedClass) {
		n.f0.accept(this, passedClass);
		n.f1.accept(this, passedClass); // call default node list visitor
	}

	/**
	    * f0 -> ","
	    * f1 -> FormalParameter()
	    */
	@Override
	public void visit(FormalParameterRest n, classStruct passedClass) {
		n.f1.accept(this, passedClass);
	}

	/**
	    * f0 -> Type()
	    * f1 -> Identifier()
	    */
	@Override
	public void visit(FormalParameter n, classStruct passedClass) {
		
		typeStruct type = getJavaTypeFromChoice(n.f0.f0);
		String id = n.f1.f0.toString();
		methodStruct curr_method = passedClass.getMethods().get(passedClass.getCurrentMethod());
		if (! curr_method.hasParameter(id))
		{
			Pair param = new Pair(id, type);
			curr_method.getParameters().add(param);
			curr_method.getLocalVariables().put(id,type);
			/*
			if(passedClass.getCurrentMethod().equals("Init")){
				System.err.println("In method "+curr_method.getMethodName()+" variable with id "+id+" is added with type "+type.type);
			}*/
			
			// add method parameters to local vars so later local var declarations can be checked
		}
		else {
			printError("Parameter " + id + " is declared twice in method " + passedClass.getCurrentMethod());
		}
	}
		

	private typeStruct getJavaTypeFromChoice(NodeChoice n)
	{
		typeStruct result = null;
		int selection=n.which;
		if(selection==0) {result = new typeStruct("array");}
		else if(selection==1){result = new typeStruct("boolean");}
		else if(selection==2){result = new typeStruct("int");}
		else if (selection==3){
			String className = ((Identifier)n.choice).f0.toString();
			result = new typeStruct(className, 1);
		}
		
		return result;
	}
	
	//TODO: Check method parameter matching for each extending class
	public void processExtendedClass()
	{
		HashSet <String> seen = new HashSet<String>();
		ArrayDeque <String> candidates = new ArrayDeque <String> ();
		for (String cName: classTable.keySet()){
			candidates.add(cName);
		}

		while ( ! candidates.isEmpty() ){
			String className = candidates.poll();
			classStruct childClass= classTable.get(className);
			if(childClass.parent != null){
				if( !seen.contains(childClass.parent) ){ //if parent class order has not been set
					candidates.add(className);
					continue;
				}
				else{
					classStruct parentClass = classTable.get(childClass.parent);
					for(String methodName: parentClass.getMethods().keySet()){
						if( ! childClass.getMethods().containsKey(methodName)){ //inherit from parent
							methodStruct pMethod = parentClass.getMethods().get(methodName);
							childClass.getMethods().put(methodName, pMethod);
							childClass.parentMethods.add(methodName);
						}
						childClass.methodPos.put(methodName, parentClass.methodPos.get(methodName));
					}
					int startIndex= parentClass.methods.size();
					for (String methodName: childClass.methods.keySet()){
						if( ! parentClass.methods.containsKey(methodName)){
							childClass.methodPos.put(methodName, startIndex);
							startIndex= startIndex+1;
						}
					}
					classTable.put(className,childClass);
					seen.add(className);
				}
			}
			else{
				seen.add(className);
				continue; //if no parent, current method order is fine
			}
		}
		for (String child: classTable.keySet())
		{
			classStruct child_class = classTable.get(child);
			String parent = child_class.getParent();
			while (parent != null && classTable.containsKey(parent))
			{
				classStruct parent_class = classTable.get(parent);
				//check for extending cycle in topology
				if(parent.equals(child)){
					printError("class extending loop detected : "+child);
				}
				//field of child class take precedence, rule 6.6
				for(String variable: parent_class.getFields().keySet()){
					if(!child_class.getFields().containsKey(variable)){
						child_class.getFields().put(variable,parent_class.getFields().get(variable));
						child_class.markField(variable);
					}
				}
				//method overriding and type check , rule 16-19
				for(String methodName: parent_class.getMethods().keySet()){
					if(child_class.getMethods().containsKey(methodName)){

						methodStruct parentMethod=parent_class.getMethods().get(methodName);
						methodStruct kidMethod=child_class.getMethods().get(methodName);
						if(!checkOverrideMethod(parentMethod, kidMethod)){
							printError("fails to override method, becomes overload : "+child+" "+methodName);
						}

					}/*
					else{
						methodStruct pMethod = parent_class.getMethods().get(methodName);
						child_class.getMethods().put(methodName, pMethod);
						child_class.markMethod(methodName);
						child_class.parentMethods.add(methodName);
					}*/
				}
				
				parent = parent_class.getParent();
			}
		}
	}

	public boolean checkOverrideMethod(methodStruct parentMethod, methodStruct kidMethod){
		if(parentMethod.getReturnType().getType()!=kidMethod.getReturnType().getType()){
			return false;
		}
		List<Pair> parentParameters=parentMethod.getParameters();
		List<Pair> kidParameters=kidMethod.getParameters();
		if(parentParameters.size()!=kidParameters.size()){return false;}
		for(int i=0;i<parentParameters.size();++i){
			if(parentParameters.get(i).second().getType()!=kidParameters.get(i).second().getType()){
				return false;
			}
		}
		return true;
	}

	public void printAllClasses()
	{
		for (classStruct currentClass: classTable.values())
			currentClass.printClass();
	}
	
	// print error message and exit the program
	private void printError(String s)
	{
		System.out.println("Type error");
		System.err.println(s);
		System.exit(1);
	}
	

}
