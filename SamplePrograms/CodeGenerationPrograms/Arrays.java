class Main {
    public static void main(String[] args) {
        System.out.println(new Trolling().excellence(new Arrays().arraySum()));
    }
}

class Trolling {
    int superb;
    public int excellence(int erm) {
        superb = 17;
        return erm * superb;
    }
}

class Arrays {
    public int arraySum() {
        int[] arr1;
        int index;
        int sum;
        index = 0;
        sum = 0;
        arr1 = new int[5];
        arr1[0] = 0;
        arr1[1] = 1;
        arr1[2] = 2;
        arr1[3] = 3;
        arr1[4] = 4;
        while (index < arr1.length) {
            sum = sum + arr1[index];
            index = index + 1;
        }
        return sum;
    }
}