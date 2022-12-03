class Main {
    public static void main(String[] args) {
        System.out.println(new Helper().function());
    }
}

class Helper {
    public int function() {
        int a;
        int b;
        CoolCalculator c;
        a = 10;
        b = 5;
        c = new CoolCalculator();
        System.out.println(c.add(a, b));
        System.out.println(c.subtract(a, b));
        return c.multiply(a, b);
    }
}

class Calculator {
    int result;

    public int add(int a, int b) {
        result = a + b;
        return result;
    }

    public int subtract(int a, int b) {
        result = a - b;
        return result;
    }

}

class CoolCalculator extends Calculator {
    public int multiply(int a, int b) {
        result = a * b;
        return result;
    }
}