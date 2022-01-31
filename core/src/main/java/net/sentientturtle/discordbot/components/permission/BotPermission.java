package net.sentientturtle.discordbot.components.permission;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Permissions for bot-use
 */
public class BotPermission {
    private static final Permissions permissions = Persistence.loadObject(Permissions.class, Permissions::new);

    private final Type type;
    private final Object value;

    private BotPermission(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    /**
     * @return Permission representing everyone
     */
    public static BotPermission EVERYONE() {
        return new BotPermission(Type.EVERYONE, null);
    }

    /**
     * @return Permission representing everyone with the specified discord role
     */
    public static BotPermission ROLE(Role permissionRole) {
        if (permissionRole.isPublicRole()) {
            return EVERYONE();
        } else {
            return new BotPermission(Type.ROLE, permissionRole.getIdLong());
        }
    }

    /**
     * @return Permission with the given String name
     */
    public static BotPermission STRING(@NotNull String permission) {
        return new BotPermission(Type.STRING, permission);
    }

    public boolean isEveryone() {
        return this.type == Type.EVERYONE;
    }

    /**
     * @return Stream of discord User IDs, only applies to STRING permissions. EVERYONE or ROLE permissions will return an empty stream.
     */
    public Stream<Long> getTargetedUsers() {
        return switch (this.type) {
            case EVERYONE, ROLE -> Stream.empty();
            case STRING -> Stream.concat(
                    permissions.usersWithAllPermissions.stream(),
                    permissions.userStringPermissions.computeIfAbsent((String) this.value, s -> new HashSet<>()).stream()
            ).distinct();
        };
    }

    /**
     * @return Stream of discord role IDs, only applies to ROLE and STRING permissions. EVERYONE permissions will return an empty stream.
     */
    public Stream<Long> getTargetedRoles() {
        return switch (this.type) {
            case EVERYONE -> Stream.empty();
            case ROLE -> Stream.of((Long) this.value);
            case STRING -> permissions.roleStringPermissions.computeIfAbsent((String) this.value, s -> new HashSet<>()).stream();
        };
    }


    /**
     * @return The permission-string for this permission, or Empty if this is not a STRING permission
     */
    public Optional<String> getString() {
        if (this.type == Type.STRING) {
            return Optional.of((String) this.value);
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return True if the specific user (Guild member) has this permission
     */
    public boolean memberHasPermission(Member member) {
        return switch (this.type) {
            case EVERYONE -> true;
            case ROLE -> permissions.usersWithAllPermissions.contains(member.getIdLong()) || member.getRoles().stream().anyMatch(role -> role.getId() == value);
            case STRING -> {
                var roles = permissions.roleStringPermissions.computeIfAbsent((String) this.value, s -> new HashSet<>());
                yield permissions.usersWithAllPermissions.contains(member.getIdLong())
                      || permissions.userStringPermissions.computeIfAbsent((String) this.value, s -> new HashSet<>()).contains(member.getIdLong())
                      || member.getRoles().stream().anyMatch(role -> roles.contains(role.getIdLong()));
            }
        };
    }

    // Most of the API is used through instance methods, making separation of the below concerns into a new "PermissionManager" class clumsy

    /**
     * @param user User to grant permission to
     * @param permission Permission to grant user
     * @return True if the user did not already have this permission
     */
    public static boolean grantPermission(User user, String permission) {
        return permissions.userStringPermissions.computeIfAbsent(permission, s -> new HashSet<>()).add(user.getIdLong());
    }

    /**
     * @param role Role to grant permission to
     * @param permission Permission to grant role
     * @return True if the role did not already have this permission
     */
    public static boolean grantPermission(Role role, String permission) {
        return permissions.roleStringPermissions.computeIfAbsent(permission, s -> new HashSet<>()).add(role.getIdLong());
    }

    /**
     * @param user User to revoke permission from
     * @param permission Permission to revoke from user
     * @return True if the user had this permission
     */
    public static boolean revokePermission(User user, String permission) {
        return permissions.userStringPermissions.computeIfAbsent(permission, s -> new HashSet<>()).remove(user.getIdLong());
    }

    /**
     * @param role Role to revoke permission from
     * @param permission Permission to revoke from role
     * @return True if the role had this permission
     */
    public static boolean revokePermission(Role role, String permission) {
        return permissions.roleStringPermissions.computeIfAbsent(permission, s -> new HashSet<>()).remove(role.getIdLong());
    }

    private enum Type {
        EVERYONE,
        ROLE,
        STRING,
    }

    private static class Permissions implements PersistentObject {
        public HashSet<Long> usersWithAllPermissions = new HashSet<>();
        public HashMap<String, HashSet<Long>> userStringPermissions = new HashMap<>();
        public HashMap<String, HashSet<Long>> roleStringPermissions = new HashMap<>();
    }
}
