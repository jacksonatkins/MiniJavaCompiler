class A {
    public static void main(String[] args) {
        System.out.println(new B().index());
    }

}
class B {
    public int index() {
        int[] a;
        int b;
        a = new int[1];
        a[0] = 5;
        b = a[0];
        return b;
    }
}