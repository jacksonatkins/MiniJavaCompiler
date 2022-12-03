class Main {
    public static void main(String[] args) {
        System.out.println(new Complex().parameters(1, new Helper().two(), 3, 4, 5));
    }
}

class Complex {
    public int parameters(int one, int two, int three, int four, int five) {
        return one + two * three - four * five;
    }
}

class Helper {
    int one;
    public int two() {
        int three;
        one = 1;
        three = 3;
        return three - one;
    }
}