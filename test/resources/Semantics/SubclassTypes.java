class SomeClass {
    public static void main(String[] args) {
        System.out.println(0);
    }
}

class A { }

class B extends A { }

class C {
    public int foo(A x) {
        return 0;
    }

    public int bar(B x) {
        return 0;
    }

    public int baz() {
        A a1;
        A a2;
        B b1;
        B b2;
        int i;

        a1 = new A();
        b1 = new B();

        a2 = a1;
        a2 = b1;
        b2 = a1;
        b2 = b1;

        i = this.foo(a1);
        i = this.foo(b1);
        i = this.bar(a1);
        i = this.bar(b1);

        return 0;
    }
}