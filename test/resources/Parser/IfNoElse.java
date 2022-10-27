class M {
    public static void main(String[] args) {
        System.out.println(new B().Test());
    }
}

class B extends A {

    public int Test() {
        int x;
        int y;
        int out;
        x = 2;
        y = 3;
        out = 0;
        if (x < y) {
            out = 1;
        }
        return out;
    }
}