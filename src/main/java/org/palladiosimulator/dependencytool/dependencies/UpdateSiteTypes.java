package org.palladiosimulator.dependencytool;

public enum UpdateSiteTypes {
    NIGHTLY, RELEASE;
    
    @Override
    public String toString() {
        switch(this) {
            case NIGHTLY: return "nightly";
            case RELEASE: return "release";
            default: throw new IllegalArgumentException();
        }
    }
}
