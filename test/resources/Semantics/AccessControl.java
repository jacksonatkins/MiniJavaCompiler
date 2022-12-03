// This file is semantically correct, and should not produce any errors.
// As far as I can tell, it utilizes all aspects of the MiniJava language specification.

class AccessControl {
    public static void main(String[] args) {
        System.out.println(new Scaffold().test());
    }
}

class Scaffold {
    public int test() {
        int b;

        Employee employee;
        Guest guest;
        Guest contractor;

        employee = new Employee();
        guest = new Guest();
        contractor = new Contractor();

        //b = employee.create(1);
        //b = employee.canOverride(30);

        b = employee.create(10);
        System.out.println(b); // 1
        b = guest.create(2, 2);
        System.out.println(b); // 1
        b = contractor.create(3, 4);
        System.out.println(b); // 1
        b = employee.canOpen(4);
        System.out.println(b); // 1
        b = guest.canOpen(4);
        System.out.println(b); // 0
        b = contractor.canOpen(3);
        System.out.println(b); // 1
        b = employee.canOverride(30);
        System.out.println(b); // 0
        b = employee.print(); // 10, 5
        System.out.println(b); // 1
        b = guest.print();
        System.out.println(b); // 0
        b = contractor.print();
        System.out.println(b); // 1
        b = guest.invalidate();
        System.out.println(b); // 1
        b = guest.canOpen(1);
        System.out.println(b); // 0
        /*


        b = employee.canOverride(30);


        b = guest.invalidate();
        b = guest.canOpen(1);*/

        return b; // 0
    }
}

class Person {
    boolean isValid;
    boolean temporary;
    int id;
    int accessLevel;
    int[] overrideCodes;

    public int canOpen(int requiredAccessLevel) {
        int shouldOpen;

        if (isValid && !(accessLevel < requiredAccessLevel)) {
            shouldOpen = 1;
        } else {
            shouldOpen = 0;
        }

        return shouldOpen;
    }

    public int canOverride(int presentedOverrideCode) {
        int shouldOverride;
        int i;

        shouldOverride = 0;
        i = 0;

        while (i < overrideCodes.length) {
            if (overrideCodes[i] < presentedOverrideCode) {
                shouldOverride = 1;
            } else { }

            i = i + 1;
        }

        return shouldOverride;
    }

    public int print() {
        System.out.println(id);
        System.out.println(accessLevel);

        return 1;
    }
}

class Employee extends Person {
    public int create(int userId) {
        isValid = true;
        temporary = false;
        id = userId;
        accessLevel = 5;
        overrideCodes = new int[3];

        overrideCodes[0] = 42;
        overrideCodes[1] = 29;
        overrideCodes[2] = 91;

        return 1;
    }
}

class Guest extends Person {
    public int create(int userId, int givenAccessLevel) {
        isValid = true;
        temporary = true;
        id = userId;
        accessLevel = givenAccessLevel;
        overrideCodes = new int[0];

        return 1;
    }

    public int invalidate() {
        if (isValid) {
            isValid = !isValid;
        } else { }

        return 1;
    }

    public int print() {
        return 0;
    }
}

class Contractor extends Guest {
    public int print() {
        return 1;
    }
}