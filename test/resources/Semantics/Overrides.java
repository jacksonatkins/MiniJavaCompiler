class SomeClass {
    public static void main(String[] args) {
        System.out.println(0);
    }
}

class A {
    public int foo() {
        return 0;
    }
}

class B extends A {
    public int foo() {
        return 1;
    }
}

class C extends A {
    public boolean foo() {
        return true;
    }
}

class D extends A {
    public int foo(int x) {
        return 2;
    }
}

class E {
    public int foo(int x) {
        return 3;
    }
}

class F extends E {
    public int foo(boolean x) {
        return 0; // needs to be integer
    }
}