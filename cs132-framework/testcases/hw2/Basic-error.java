class Main {
	public static void main(String[] a){
		System.out.println(new A().run());
	}
}

class A {
	public int run() {
		int x;
		y = 1; // TE: Name error!  Variable 'y' doesn't exist.
		return x;
	}
}
