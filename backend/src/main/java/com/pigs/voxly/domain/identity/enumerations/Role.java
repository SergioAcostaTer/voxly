package com.pigs.voxly.domain.identity.enumerations;

import com.pigs.voxly.sharedKernel.domain.types.Enumeration;

public final class Role extends Enumeration<Role> {

    public static final Role USER = new Role(1, "User");
    public static final Role ADMIN = new Role(2, "Admin");
    public static final Role MODERATOR = new Role(3, "Moderator");

    private Role(int id, String name) {
        super(id, name);
    }
}
