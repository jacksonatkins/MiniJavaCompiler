class A {
    public static void main(String[] args) {
        System.out.println(new B().LT());
    }

}
class B {
    public int LT() {
        int a;
        int b;
        int c;
        a = 4;
        b = 5;

        if (a < b) {
            c = a;
        } else {
            c = b;
        }
        return c;
    }
}