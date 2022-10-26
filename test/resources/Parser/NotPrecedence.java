class A {
    public static void main(String[] args) {
        System.out.println(new B().Not());
    }

}
class B {
    public boolean Not() {
        boolean a;
        a = false;
        c = !a;
        return c;
    }
}