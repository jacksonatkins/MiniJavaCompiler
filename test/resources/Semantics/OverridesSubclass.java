class SomeClass {
    public static void main(String[] args) {
        System.out.println(0);
    }
}

class First {
    public int first() {
        return 0;
    }
}

class Second extends First{
    public int second() {
        return 0;
    }
}

class A {
    public First foo() {
        return new First();
    }
}

class B extends A {
    public First foo() {
        return new Second();
    }
}

class C extends A {
    public Second foo() {
        return new Second();
    }
}

class D extends A {
    public First foo(int x) {
        return new First();
    }
}

class E {
    public First foo(First x) {
        return new First();
    }
}

class F extends E {
    public First foo(Second x) {
        return new Second();
    }
}