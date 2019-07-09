package com.appboy;

import com.appboy.enums.Month;

import bo.app.bs;
import bo.app.bv;
import bo.app.dz;
import bo.app.ec;

public class MockAppboyUser extends AppboyUser {
    public int dobYear = -1;
    public Month dobMonth = null;
    public int dobDay = -1;

    MockAppboyUser() {
        super(null, null, null, null, null);
    }

    public boolean setDateOfBirth(int year, Month month, int day) {
        dobYear = year;
        dobMonth = month;
        dobDay = day;
        return true;
    }
}
