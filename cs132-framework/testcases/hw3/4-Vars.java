class Main {
	public static void main(String[] a){
		System.out.println(new A().run());
	}
}

class A {
	public int run() {
		int a;
		int b;
		a = this.helper(12);
		b = this.helper(15);
		return a + b;
	}

	public int helper(int param) {
		int x;
		x = param;
		param = param + 1;
		System.out.println(x);
		return x;
	}
}
