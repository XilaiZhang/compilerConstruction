import java.util.*;

//remember to check emptyness of deque
class Logic{
	Deque<Integer> tokens=new ArrayDeque<Integer>();
	Boolean canEat=true;//can't use this one

	void eat(int target){
		if(tokens.isEmpty() || tokens.peek()!=target){
			canEat=false;
		}
		else{tokens.poll();}
	}

	Boolean logicL(){
		if(tokens.isEmpty()){return true;}
		//Deque<Integer> copy=tokens;
		//if(logicS() && logicL()){return true;}
		//tokens=copy;
		int op=tokens.peek();
		if(op==0 || op==2 || op==6 || op==8){
			return(logicS() && logicL());
		}
		else{return true;}
	}

	Boolean logicE(){
		if(tokens.isEmpty()){return false;}
		int op=tokens.peek();
		if(op==9){eat(9);}
		else if(op==10){eat(10);}
		else if(op==11){
			eat(11);
			return logicE();
		}
		else{return false;}
		return true;
	}

	Boolean logicS(){
		if(tokens.isEmpty()){return false;}
		int op=tokens.peek();
		if(op==0){ // { 
			eat(0);
			if(!logicL()){return false;}
			eat(1);
		}
		else if(op==2){ //Sytem.out....
			eat(2);
			eat(3);
			if(!logicE()){return false;}
			eat(4);
			eat(5);
		}
		else if(op==6){
			eat(6);
			eat(3);
			if(!logicE()){return false;}
			eat(4);
			if(!logicS()){return false;}
			eat(7);
			if(!logicS()){return false;}
		}
		else if(op==8){
			eat(8);
			eat(3);
			if(!logicE()){return false;}
			eat(4);
			if(!logicS()){return false;}
		}
		else{return false;}

		return true;
	}

	boolean work(){
		canEat=true;
		if(!logicS()){return false;}
		if(!tokens.isEmpty()){
			return false;
		}
		if(!canEat){return false;}
		return true;
	}

}

public class Parse{	

	public static void main(String [] args) {
		HashMap<String, Integer> tid = new HashMap<String,Integer>();
		//{, }, System.out.println, (, ), ;, if, else, while, true, false, !

		tid.put("{",0);
		tid.put("}",1);
		tid.put("System.out.println",2);
		tid.put("(",3);
		tid.put(")",4);
		tid.put(";",5);
		tid.put("if",6);
		tid.put("else",7);
		tid.put("while",8);
		tid.put("true",9);
		tid.put("false",10);
		tid.put("!",11);


		Scanner scanner=new Scanner(System.in);
		String word="";
		Deque<Integer> T=new ArrayDeque<Integer>(); // the mapped integers
		while(scanner.hasNext()){
			String myWord=scanner.next();
			for(int i=0;i<myWord.length();++i){
				word+=myWord.charAt(i);
				if(tid.containsKey(word)){
					T.add(tid.get(word));
					word="";
				}
			}//recheck the logic here
			if(word.length()!=0){ //bakward compatible
				System.out.println("Parse error");
				System.exit(1);
			}
		}
		Logic logic=new Logic();
		logic.tokens=T;
		if(!logic.work()){
			System.out.println("Parse error");
			System.exit(1);
		}

		System.out.println("Program parsed successfully");
		scanner.close();
	}
}