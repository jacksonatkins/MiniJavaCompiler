class SomeClass {
    public static void main(String[] args) {
        System.out.println(0);
    }
}

class A {
    public int foo() {
        return 0;
    }

    public int bar(int x) {
        return 0;
    }

    public int baz(int x, boolean y) {
        return 0;
    }

    public int bop() {
        int i;

        i = this.foo();
        i = this.bar(1);
        i = this.baz(1, true);

        i = this.foo(1);
        i = this.bar();
        i = this.bar(true);
        i = this.baz(1, 2);
        i = this.baz(1, true, 1);

        return 0;
    }
}