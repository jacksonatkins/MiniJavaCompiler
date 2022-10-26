class A {
    public static void main(String[] args) {
        System.out.println(new B().Func(5));
    }

}
class B {
    public Foo Func(int q) {
        Foo a;
        Foo b;
        Foo c;
        c = a+b.Bar();
        return c;
    }
}