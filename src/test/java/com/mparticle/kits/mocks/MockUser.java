package com.mparticle.kits.mocks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;

import java.util.Map;

public class MockUser implements MParticleUser {
    Map<MParticle.IdentityType, String> identities;

    public MockUser(Map<MParticle.IdentityType, String> identities) {
        this.identities = identities;
    }

    @NonNull
    @Override
    public long getId() {
        return 0;
    }

    @NonNull
    @Override
    public Map<String, Object> getUserAttributes() {
        return null;
    }

    @Nullable
    @Override
    public Map<String, Object> getUserAttributes(@Nullable UserAttributeListener userAttributeListener) {
        return null;
    }

    @Override
    public boolean setUserAttributes(@NonNull Map<String, Object> map) {
        return false;
    }

    @Override
    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return identities;
    }

    @Override
    public boolean setUserAttribute(@NonNull String s, @NonNull Object o) {
        return false;
    }

    @Override
    public boolean setUserAttributeList(@NonNull String s, @NonNull Object o) {
        return false;
    }

    @Override
    public boolean incrementUserAttribute(@NonNull String s, int i) {
        return false;
    }

    @Override
    public boolean removeUserAttribute(@NonNull String s) {
        return false;
    }

    @Override
    public boolean setUserTag(@NonNull String s) {
        return false;
    }

    @NonNull
    @Override
    public ConsentState getConsentState() {
        return null;
    }

    @Override
    public void setConsentState(@Nullable ConsentState consentState) {

    }


    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public long getFirstSeenTime() {
        return 0;
    }

    @Override
    public long getLastSeenTime() {
        return 0;
    }
}
