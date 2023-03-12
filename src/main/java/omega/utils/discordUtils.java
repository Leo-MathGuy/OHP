package omega.utils;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class discordUtils {

    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<discord>discord\\.gg/\\w+)|<@(?:(?<user>!?)|(?<role>&))(?<id>\\d{15,})>|@(?<everyone>everyone|here)", Pattern.CASE_INSENSITIVE);
    private static final char ZERO_WIDTH_SPACE = '\u200B';
    public static String commandDisabled = "This command is disabled.";
    public static String noPermission = "You don't have permissions to use this command!";

    public static TextChannel getTextChannel(DiscordApi api, String id){
        var optional = api.getChannelById(id);
        if(optional.isEmpty()) {

            return null;
        }

        Optional<Channel> dc = api.getChannelById(id);
        if (dc.isEmpty()) {
            System.out.println("[ERR!] discordplugin: channel not found!");
            return null;
        }
        Optional<TextChannel> dtc = dc.get().asTextChannel();
        if (dtc.isEmpty()){
            System.out.println("[ERR!] discordplugin: textchannel not found!");
            return null;
        }
        return dtc.get();
    }

    public static Role getRole(DiscordApi api, String id){
        Optional<Role> r1 = api.getRoleById(id);
        if (r1.isEmpty()) {
            System.out.println("[ERR!] discordplugin: role not found!");
            return null;
        }
        return r1.get();
    }

    public static Boolean hasPermission(Role r, MessageCreateEvent event){
        try {
            if (r == null) {
                if (event.isPrivateMessage()) return false;
                event.getChannel().sendMessage(commandDisabled);
                return false;
            } else if (!event.getMessageAuthor().asUser().get().getRoles(event.getServer().get()).contains(r)) {
                if (event.isPrivateMessage()) return false;
                event.getChannel().sendMessage(noPermission);
                return false;
            } else {
                return true;
            }
        } catch (Exception ignore){
            return false;
        }
    }
    public static String filterMentions(Server server, String message) {
        Matcher matcher = MENTION_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();

        String captured;
        while(matcher.find()) {
            if(matcher.group("discord") != null) {
                matcher.appendReplacement(builder, "discord.gg/dYRhUUMXFY "); // OmegaHub invite link, should probably be in a variable
            } else if((captured = matcher.group("user")) != null) {
                var name = server.getMemberById(captured)
                        .map(user -> user.getDisplayName(server))
                        .orElse("Unknown user");
                matcher.appendReplacement(builder, name);
            } else if((captured = matcher.group("role")) != null) {
                var role = server.getRoleById(captured).map(Role::getName)
                        .orElse("Unknown role");
                matcher.appendReplacement(builder, role);
            } else if((captured = matcher.group("everyone")) != null) {
                matcher.appendReplacement(builder, "@" + ZERO_WIDTH_SPACE + captured);
            }
        }

        matcher.appendTail(builder);
        return builder.toString();
    }

}

