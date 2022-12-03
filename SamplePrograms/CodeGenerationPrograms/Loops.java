class Main {
    public static void main(String[] args) {
        System.out.println(new Loops().loop1(0, 10));
    }
}

class Loops {
    int c;
    int d;
    public int loop1(int a, int b) {
        c = 0;
        d = 10;
        while (a < b && !(d < c)) {
            a = a + 1;
            c = c + 1;
            if (5 < a && 5 < c) {
                a = this.loop2(a);
            } else {
                c = this.loop2(c);
            }
        }
        return a + c - b;
    }

    public int loop2(int val) {
        while (val < 5) {
            val = val + 1;
        }
        return val;
    }
}