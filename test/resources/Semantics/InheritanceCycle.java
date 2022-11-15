class SomeClass {
    public static void main(String[] args) {
        System.out.println(new A());
    }
}

class A extends B { }

class B extends C { }

class C extends A { }