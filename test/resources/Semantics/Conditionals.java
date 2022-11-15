class SomeClass {
    public static void main(String[] args) {
        System.out.println(new A());
    }
}

class Object { }

class A {
    int[] arr;
    boolean bool1;
    boolean bool2;
    int num1;
    int num2;
    Object obj;

    public int foo() {
        if (true) { }
        else { }

        if (false) { }
        else { }

        if (bool1) { }
        else { }

        if (bool1 && bool2) { }
        else { }

        if (!bool1) { }
        else { }

        if (num1 < num2) { }
        else { }

        if (arr.length) { }
        else { }

        if (arr[0]) { }
        else { }

        if (arr) { }
        else { }

        if (num1) { }
        else { }

        if (num1 + num2) { }
        else { }

        if (num1 - num2) { }
        else { }

        if (num1 * num2) { }
        else { }

        if (1) { }
        else { }

        if (obj) { }
        else { }

        return 0;
    }
}