// This file is semantically correct, and should not produce any errors.
// As far as I can tell, it utilizes all aspects of the MiniJava language specification.

class AccessControl {
    public static void main(String[] args) {
        System.out.println(new Scaffold().test());
    }
}

class Scaffold {
    public int test() {
        boolean b;

        Employee employee;
        Guest guest;
        Guest contractor;

        employee = new Employee();
        guest = new Guest();
        contractor = new Contractor();

        b = employee.create(1);
        b = guest.create(2, 2);
        b = contractor.create(3, 4);

        b = employee.canOpen(4);
        b = guest.canOpen(4);
        b = contractor.canOpen(3);

        b = employee.canOverride(30);

        b = employee.print();
        b = guest.print();
        b = contractor.print();

        b = guest.invalidate();
        b = guest.canOpen(1);

        return 0;
    }
}

class Person {
    boolean isValid;
    boolean temporary;
    int id;
    int accessLevel;
    int[] overrideCodes;

    public boolean canOpen(int requiredAccessLevel) {
        boolean shouldOpen;

        if (isValid && !(accessLevel < requiredAccessLevel)) {
            shouldOpen = true;
        } else {
            shouldOpen = false;
        }

        return shouldOpen;
    }

    public boolean canOverride(int presentedOverrideCode) {
        boolean shouldOverride;
        int i;

        shouldOverride = false;
        i = 0;

        while (i < overrideCodes.length) {
            if (overrideCodes[i] < presentedOverrideCode) {
                shouldOverride = true;
            } else { }

            i = i + 1;
        }

        return shouldOverride;
    }

    public boolean print() {
        System.out.println(id);
        System.out.println(accessLevel);

        return true;
    }
}

class Employee extends Person {
    public boolean create(int userId) {
        isValid = true;
        temporary = false;
        id = userId;
        accessLevel = 5;
        overrideCodes = new int[3];

        overrideCodes[0] = 42;
        overrideCodes[1] = 29;
        overrideCodes[2] = 91;

        return true;
    }
}

class Guest extends Person {
    public boolean create(int userId, int givenAccessLevel) {
        isValid = true;
        temporary = true;
        id = userId;
        accessLevel = givenAccessLevel;
        overrideCodes = new int[0];

        return true;
    }

    public boolean invalidate() {
        if (isValid) {
            isValid = !isValid;
        } else { }

        return true;
    }

    public boolean print() {
        return false;
    }
}

class Contractor extends Guest {
    public boolean print() {
        return true;
    }
}