/* SPDX-License-Identifier: GPL-2.0-or-later */
package org.telegram.teleflow.folders;

public final class ManagedFolderResolution {
    public enum Status { ACTIVE, UNREGISTERED, MISSING, IDENTITY_MISMATCH }

    private final Status status;
    private final ManagedFolderRegistration registration;
    private final String liveName;

    public ManagedFolderResolution(Status status, ManagedFolderRegistration registration, String liveName) {
        if (status == null) throw new IllegalArgumentException("status is required");
        this.status = status;
        this.registration = registration;
        this.liveName = liveName;
    }

    public Status getStatus() { return status; }
    public ManagedFolderRegistration getRegistration() { return registration; }
    public String getLiveName() { return liveName; }
    public boolean isActive() { return status == Status.ACTIVE; }
}
