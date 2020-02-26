package com.mparticle.kits.mocks;

import android.content.Context;

import com.mparticle.internal.ReportingManager;
import com.mparticle.kits.AppboyKit;
import com.mparticle.kits.AppboyKitTests;

import org.mockito.Mockito;

public class MockAppboyKit extends AppboyKit {
    final public String[] calledAuthority = new String[1];

    public MockAppboyKit() {
        setKitManager(new MockKitManagerImpl(Mockito.mock(Context.class), Mockito.mock(ReportingManager.class), new MockCoreCallbacks()));
    }

    @Override
    protected void setAuthority(String authority) {
        calledAuthority[0] = authority;
    }

    @Override
    protected void queueDataFlush() {
        //do nothing
    }
}
