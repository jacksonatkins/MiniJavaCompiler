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

    public int and() {
        boolean x;

        x = true && false;
        x = bool1 && bool2;
        x = true && bool1;
        x = bool1 && true;

        x = true && arr;
        x = true && 1;
        x = true && num1;
        x = true && obj;

        return 0;
    }

    public int lessThan() {
        boolean x;

        x = 1 < 2;
        x = num1 < 2;
        x = 1 < num2;
        x = num1 < num2;
        x = num1 < arr[0];

        x = 1 < arr;
        x = 1 < bool1;
        x = 1 < obj;

        return 0;
    }

    public int plus() {
        int x;

        x = 1 + 2;
        x = num1 + 2;
        x = 1 + num2;
        x = num1 + num2;
        x = arr[0] + arr[1];

        x = 1 + arr;
        x = 1 + bool1;
        x = 1 + obj;

        return 0;
    }

    public int minus() {
        int x;

        x = 1 - 2;
        x = num1 - 2;
        x = 1 - num2;
        x = num1 - num2;
        x = arr[0] - arr[1];

        x = 1 - arr;
        x = 1 - bool1;
        x = 1 - obj;

        return 0;
    }

    public int times() {
        int x;

        x = 1 * 2;
        x = num1 * 2;
        x = 1 * num2;
        x = num1 * num2;
        x = arr[0] * arr[1];

        x = 1 * arr;
        x = 1 * bool1;
        x = 1 * obj;

        return 0;
    }
}