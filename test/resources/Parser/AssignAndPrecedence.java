class A {
    public static void main(String[] args) {
        System.out.println(new B().And());
    }

}
class B {
    public boolean And() {
        boolean a;
        boolean b;
        boolean c;
        a = false;
        b = true;
        c = a && b;
        return c;
    }
}