package net.sentientturtle.discordbot.botmodules.simple;

import net.dv8tion.jda.api.MessageBuilder;
import net.sentientturtle.discordbot.components.core.Core;
import net.sentientturtle.discordbot.components.core.Scheduling;
import net.sentientturtle.discordbot.components.module.BotModule;
import net.sentientturtle.discordbot.components.module.command.Command;
import net.sentientturtle.discordbot.components.module.command.CommandCall;
import net.sentientturtle.discordbot.components.persistence.Persistence;
import net.sentientturtle.discordbot.components.persistence.PersistentObject;
import net.sentientturtle.discordbot.loader.StaticLoaded;
import net.sentientturtle.util.TimeFormat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Module providing reminder commands
 */
public class Reminder extends BotModule implements StaticLoaded {
    private static final Reminders persistence = Persistence.loadObject(Reminders.class, Reminders::new);

    private static final Map<String, TimeUnit> unitNames = new HashMap<>();
    static {
        unitNames.put("seconds", TimeUnit.SECONDS);
        unitNames.put("second", TimeUnit.SECONDS);
        unitNames.put("s", TimeUnit.SECONDS);
        unitNames.put("minutes", TimeUnit.MINUTES);
        unitNames.put("minute", TimeUnit.MINUTES);
        unitNames.put("m", TimeUnit.MINUTES);
        unitNames.put("hours", TimeUnit.HOURS);
        unitNames.put("hour", TimeUnit.HOURS);
        unitNames.put("h", TimeUnit.HOURS);
        unitNames.put("days", TimeUnit.DAYS);
        unitNames.put("day", TimeUnit.DAYS);
        unitNames.put("d", TimeUnit.DAYS);

        Scheduling.schedule(() -> {
            while (Core.getJDA() == null) Thread.sleep(500);  // TODO: Replace busy-wait; Add a method to Core for explicit waiting-on-ready.

            synchronized (persistence) {
                persistence.reminders.forEach(Reminder::scheduleReminder);
            }
            return null;
        }, 0, TimeUnit.MICROSECONDS);
    }

    @Command(commandName = "remindme", description = "Set a reminder")
    public static void remindme(
            CommandCall commandCall,
            @Command.Parameter(name = "duration", description = "Time until the reminder is sent") String durationString,
            @Command.Parameter(name = "message", description = "Message to attach to the reminder", optional = true) String message
    ) {
        String[] split = durationString.split(" ");
        if (split.length > 1 && split.length % 2 == 0) {
            long durationTotal = 0;
            for (int i = 0; i < split.length; i += 2) {
                try {
                    long duration = Long.parseLong(split[i]);
                    TimeUnit unit = unitNames.get(split[i + 1].toLowerCase());
                    durationTotal += unit.toMillis(duration);
                } catch (NumberFormatException | NullPointerException ignored) {
                    commandCall.error("⚠ Invalid duration syntax!");
                    return;
                }
            }
            long userID = commandCall.getUser().getIdLong();
            long finalDurationTotal = durationTotal;
            long currentTime = System.currentTimeMillis();

            var reminder = new PersistentReminder(userID, message, currentTime + finalDurationTotal, currentTime);
            scheduleReminder(reminder);
            persistence.add(reminder);
            commandCall.reply("Reminder set!", true);
        } else {
            commandCall.error("⚠ Invalid duration syntax!");
        }
    }

    private static void scheduleReminder(PersistentReminder reminder) {
        Scheduling.schedule(
                () -> {
                    persistence.remove(reminder);
                    assert Core.getJDA() != null;
                    var user = Core.getJDA().getUserById(reminder.userID);
                    if (user != null) {  // If user no longer exists, do nothing and drop reminder
                        var channel = user.openPrivateChannel().complete();
                        channel.sendMessage(
                                new MessageBuilder()
                                        .append(user)
                                        .append(" reminder from ")
                                        .append(TimeFormat.formatWDHMS(System.currentTimeMillis() - reminder.creationTime, TimeUnit.MILLISECONDS))
                                        .append(" ago.")
                                        .append(reminder.message != null ? "\n" + reminder.message : "")
                                        .build()
                        ).queue();
                    }
                },
                reminder.scheduledTime - System.currentTimeMillis(),    // This may be negative; The reminder will simply be ran immediately.
                TimeUnit.MILLISECONDS
        );
    }

    private record PersistentReminder(long userID, String message, long scheduledTime, long creationTime) {}

    private static class Reminders implements PersistentObject {
        public HashSet<PersistentReminder> reminders = new HashSet<>();

        public synchronized void add(PersistentReminder reminder) {
            reminders.add(reminder);
        }

        public synchronized void remove(PersistentReminder reminder) {
            reminders.remove(reminder);
        }
    }
}
