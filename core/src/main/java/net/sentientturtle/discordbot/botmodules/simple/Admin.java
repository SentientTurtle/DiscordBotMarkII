package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.sentientturtle.discordbot.components.core.Shutdown;
import net.sentientturtle.discordbot.components.healthcheck.HealthCheck;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.ModuleManager;
import net.sentientturtle.discordbot.components.module.command.Command;
import net.sentientturtle.discordbot.components.module.command.CommandCall;
import net.sentientturtle.discordbot.components.permission.BotPermission;
import net.sentientturtle.discordbot.helpers.MessageHelper;

import java.io.IOException;
import java.util.Optional;

/**
 * Module providing various administrative commands
 */
public class Admin extends BotModule {
    @Command(commandName = "admin", subcommandName = "shutdown", description = "Shuts down the bot")
    public void shutdown(CommandCall commandCall) {
        commandCall.reply("Shutting down...");
        Shutdown.shutdownAll(true, 0);
    }

    @Command(commandName = "admin", subcommandGroup = "module", subcommandName = "enable", description = "Enables a module")
    public void enable_module(CommandCall commandCall, @Command.Parameter(name = "module", description = "module to enable") String module) {
        if (!ModuleManager.moduleExists(module)) {
            commandCall.error("⚠ Module does not exist!");
        } else {
            boolean permissionsChanged = ModuleManager.enableModule(module);
            if (permissionsChanged) {
                commandCall.reply(new MessageBuilder("Module ").appendCodeLine(module).append(" now enabled. Please restart to apply changes.").build());
            } else {
                commandCall.reply(new MessageBuilder("Module ").appendCodeLine(module).append(" was already enabled!").build());
            }
        }
    }

    @Command(commandName = "admin", subcommandGroup = "module", subcommandName = "disable", description = "Disabled a module")
    public void disable_module(CommandCall commandCall, @Command.Parameter(name = "module", description = "module to disable") String module) {
        if (!ModuleManager.moduleExists(module)) {
            commandCall.error("⚠ Module does not exist!");
        } else {
            boolean permissionsChanged = ModuleManager.disableModule(module);
            if (permissionsChanged) {
                commandCall.reply(new MessageBuilder("Module ").appendCodeLine(module).append(" now disabled. Please restart to apply changes.").build());
            } else {
                commandCall.reply(new MessageBuilder("Module ").appendCodeLine(module).append(" was already disabled!").build());
            }
        }
    }

    @Command(commandName = "admin", subcommandName = "restart", description = "Restarts the bot")
    public void restart(CommandCall commandCall) {
        commandCall.reply("Restarting...", false, interactionHook -> {
            try {
                Shutdown.restart();
            } catch (IOException e) {
                interactionHook.editOriginal("Restart failed: " + e.getMessage()).queue();
            }
        });
    }

    @Command(commandName = "admin", subcommandName = "reregister", description = "Re-registers the commands")
    public void reregister(CommandCall commandCall) {
        ModuleManager.registerCommands();
        commandCall.reply("Re-registering commands...", false);
    }

    @Command(commandName = "admin", subcommandGroup = "list", subcommandName = "modules", description = "Lists available modules")
    public static void list_modules(CommandCall commandCall, @Command.Parameter(name = "page", description = "page", optional = true) Long page) {
        commandCall.reply(
                MessageHelper.paginate(
                        "Modules",
                        ModuleManager.modules.keySet().stream().sorted().map(module -> switch (ModuleManager.moduleStatus(module)) {
                            case ENABLED -> module;
                            case PENDING_ENABLED -> module + " (enabled after restart)";
                            case DISABLED -> module + " (disabled)";
                            case PENDING_DISABLED -> module + " (disabled after restart)";
                        }).toList(),
                        Math.max(1, page == null ? 1 : page)
                ),
                true
        );
    }

    @Command(commandName = "admin", subcommandGroup = "health", subcommandName = "status", description = "Displays health check status")
    public static void health_status(CommandCall commandCall, @Command.Parameter(name = "page", description = "page", optional = true) Long page) {
        commandCall.reply(
                MessageHelper.paginate(
                        "Health status",
                        HealthCheck.getMessages(),
                        Math.max(1, page == null ? 1 : page)
                ),
                true
        );
    }

    @Command(commandName = "admin", subcommandGroup = "grant", subcommandName = "command", description = "Grant command permission")
    public static void grant_cmd(
            CommandCall commandCall,
            @Command.Parameter(name = "command", description = "Command for which to give permission") String command,
            @Command.Parameter(name = "role", description = "Role to grant permission", optional = true) Role role,
            @Command.Parameter(name = "user", description = "User to grant permission", optional = true) User user
    ) {
        if (command.contains("/")) {
            commandCall.error("Invalid command!");
        } else if (role == null && user == null) {
            commandCall.error("You must specify a role or user to grant permission to");
        } else if (role != null && user != null) {
            commandCall.error("You may not specify both a role and user at the same time"); // Not a technical constraint, but the cases where this feature would be used intentionally is assumed to be less frequent than the cases where the user specifies both options by mistake
        } else {
            var unifiedCommand = ModuleManager.getCommandMap().get(command);
            if (unifiedCommand != null) {
                Optional<String> permission = unifiedCommand.commandUserPermission().getString();
                if (permission.isPresent()) {
                    if (role != null) {
                        if (BotPermission.grantPermission(role, permission.get())) {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] now granted to ").append(role).build(), true);
                        } else {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] was already granted to ").append(role).build(), true);
                        }
                    } else {
                        if (BotPermission.grantPermission(user, permission.get())) {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] now granted to ").append(user).build(), true);
                        } else {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] was already granted to ").append(user).build(), true);
                        }
                    }
                } else {
                    commandCall.error("Cannot set permission for command [" + command + "]");
                }
            } else {
                commandCall.error("Command [" + command + "] does not exist!");
            }
        }
    }

    @Command(commandName = "admin", subcommandGroup = "revoke", subcommandName = "command", description = "Revoke command permission")
    public static void revoke_cmd(
            CommandCall commandCall,
            @Command.Parameter(name = "command", description = "Command for which to revoke permission") String command,
            @Command.Parameter(name = "role", description = "Role to revoke permission from", optional = true) Role role,
            @Command.Parameter(name = "user", description = "User to revoke permission from", optional = true) User user
    ) {
        if (command.contains("/")) {
            commandCall.error("Invalid command!");
        } else if (role == null && user == null) {
            commandCall.error("You must specify a role or user to revoke permission from");
        } else if (role != null && user != null) {
            commandCall.error("You may not specify both a role and user at the same time"); // Not a technical constraint, but the cases where this feature would be used intentionally is assumed to be less frequent than the cases where the user specifies both options by mistake
        } else {
            var unifiedCommand = ModuleManager.getCommandMap().get(command);
            if (unifiedCommand != null) {
                Optional<String> permission = unifiedCommand.commandUserPermission().getString();
                if (permission.isPresent()) {
                    if (role != null) {
                        if (BotPermission.revokePermission(role, permission.get())) {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] now revoked from ").append(role).build(), true);
                        } else {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] was already revoked from ").append(role).build(), true);
                        }
                    } else {
                        if (BotPermission.revokePermission(user, permission.get())) {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] now revoked from ").append(user).build(), true);
                        } else {
                            commandCall.reply(new MessageBuilder("Permission for command [" + command + "] was already revoked from ").append(user).build(), true);
                        }
                    }
                } else {
                    commandCall.error("Cannot set permission for command [" + command + "]");
                }
            } else {
                commandCall.error("Command [" + command + "] does not exist!");
            }
        }
    }
}
