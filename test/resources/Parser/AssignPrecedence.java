class A {
    public static void main(String[] args) {
        System.out.println(new B().Assign());
    }

}
class B {
    public int Assign() {
        int a;
        int b;
        int c;
        a = 4;
        b = 5;
        c = a + b;
        return c;
    }
}