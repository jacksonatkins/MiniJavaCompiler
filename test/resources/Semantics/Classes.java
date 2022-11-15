class SomeClass {
    public static void main(String[] args) {
        System.out.println(new A());
    }
}

class A {
    public int foo() {
        return 0;
    }
}

class B extends A { }

class C extends A {
    public int foo() {
        return 1;
    }
}

class D {
    public int foo() {
        A a;
        B b;
        C c;
        int i;

        i = a.foo();
        i = b.foo();
        i = c.foo();

        return 0;
    }
}