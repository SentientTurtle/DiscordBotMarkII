package net.sentientturtle.discordbot.components.core;

import net.dv8tion.jda.api.entities.Role;
import net.sentientturtle.discordbot.components.StaticInitException;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistenceException;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Handles permission validation<br>
 * Currently only supports 1 server/guild at a time
 */
public class PermissionManager implements StaticLoaded {
    private static final Logger logger = LoggerFactory.getLogger(PermissionManager.class);
    private static final UserPermissions settings;
    private static final HashSet<String> emptySet = new HashSet<>();

    static {
        try {
            settings = Persistence.loadObject(UserPermissions.class, UserPermissions::new);
        } catch (PersistenceException.ObjectLoadFailed objectLoadFailed) {
            throw new StaticInitException(objectLoadFailed);
        }
        logger.info("Module initialised!");
    }

    public static boolean checkUserPermission(String permission, long guildID, long guildOwnerID, long channelID, long userID, List<Role> roles) {
        return (settings.targetGuildOwnerHasAllPermissions && guildOwnerID == userID) |
                settings.guildPermissions.getOrDefault(guildID, emptySet).contains(permission) |
                settings.channelPermissions.getOrDefault(channelID, emptySet).contains(permission) |
                settings.userPermissions.getOrDefault(userID, emptySet).contains(permission) |
                roles.stream().mapToLong(Role::getIdLong).anyMatch(roleID -> settings.rolePermissions.getOrDefault(roleID, emptySet).contains(permission));
    }


    private static class UserPermissions implements PersistentObject {
        public boolean targetGuildOwnerHasAllPermissions = true;
        public HashMap<Long, HashSet<String>> guildPermissions = new HashMap<>();
        public HashMap<Long, HashSet<String>> channelPermissions = new HashMap<>();
        public HashMap<Long, HashSet<String>> userPermissions = new HashMap<>();
        public HashMap<Long, HashSet<String>> rolePermissions = new HashMap<>();
    }
}
