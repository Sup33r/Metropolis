package live.supeer.metropolis.city;

import lombok.Getter;

@Getter
public class Role {
    public static final Role MAYOR = new Role("mayor", 5);
    public static final Role VICE_MAYOR = new Role("vicemayor", 4);
    public static final Role ASSISTANT = new Role("assistant", 3);
    public static final Role INVITER = new Role("inviter", 2);
    public static final Role MEMBER = new Role("member", 1);

    private final String roleName;
    private final int permissionLevel;

    public Role(String roleName, int permissionLevel) {
        this.roleName = roleName;
        this.permissionLevel = permissionLevel;
    }

    public boolean hasPermission(Role role) {
        return this.permissionLevel >= role.permissionLevel;
    }

    public static Role fromString(String roleName) {
        return switch (roleName.toLowerCase()) {
            case "mayor" -> MAYOR;
            case "vicemayor" -> VICE_MAYOR;
            case "assistant" -> ASSISTANT;
            case "inviter" -> INVITER;
            default -> MEMBER;
        };
    }
}