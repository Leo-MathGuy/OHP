package omega.command;

// Arc
import arc.Core;
import arc.struct.ObjectMap;
import arc.util.Strings;

// Mindustry
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration;
import mindustry.world.modules.ItemModule;

// Omega
import omega.OmegaPlugin;
import omega.database.dataHandler;
import omega.utils.Find;

// Javacord
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

// Java
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

// Static
import static omega.utils.Constants.prefix;
import static omega.utils.Logger.discLogErr;
import static omega.utils.discordUtils.hasPermission;
import static omega.utils.mapParse.renderMap;
import static omega.utils.mapParse.renderMinimap;

public class comCommands implements MessageCreateListener {
    private final OmegaPlugin mainData;
    private final long CDT = 60L;
    private final ObjectMap<Long, String> cooldowns = new ObjectMap<>(); //uuid
    public static boolean linkaccount;
    public static MessageCreateEvent linkevent;
    public static Player found2;
    public comCommands(OmegaPlugin _data){
        this.mainData = _data;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event){
        String[] incoming_msg = event.getMessageContent().split("\\s+");
        Role moderator = mainData.discRoles.get("role_id");

        if (event.getMessageAuthor().isBotUser()) return;
        switch (incoming_msg[0]){
            case prefix + "chat":
                String[] msg2 = (event.getMessageContent().replace('\n', ' ')).split("\\s+", 2);
                Call.sendMessage("[sky]" +event.getMessageAuthor().getName()+ " from discord >[] " + msg2[1].trim());
                event.getChannel().sendMessage("Sent :white_check_mark:");
                break;
            case prefix + "peepee":
                // Ping command displaying ms
                long time = System.currentTimeMillis();
                event.getChannel().sendMessage("poopoo!").thenAcceptAsync(message -> {
                    long ping = System.currentTimeMillis() - time;
                    message.edit("poopoo: " + ping + "ms");
                });
                break;
            case prefix + "modchat":
                if (moderator == null) {
                    event.getChannel().sendMessage("You do not have permission to use this command");
                    return;
                }
                if (!hasPermission(moderator, event)) return;
                String[] msg3 = (event.getMessageContent().replace('\n', ' ')).split("\\s+", 2);
                Call.sendMessage("[scarlet]Administrator >[] " + msg3[1].trim());
                event.getChannel().sendMessage(":white_check_mark:");

                break;
            case prefix + "players":
                StringBuilder lijst = new StringBuilder();
                StringBuilder admins = new StringBuilder();
                lijst.append("Players: ").append(Groups.player.size()).append("\n");
                if(Groups.player.count(p->p.admin) > 0) {
                    admins.append("Online Admins: \n");// + Vars.playerGroup.all().count(p->p.isAdmin)+"\n");
                }
                for (Player p : Groups.player){
                    if (p.admin()){
                        admins.append("* ").append(Strings.stripColors(p.name).trim()).append("\n");
                    } else {
                        lijst.append("* ").append(Strings.stripColors(p.name).trim()).append("\n");
                    }
                }
                new MessageBuilder().appendCode("", lijst.toString() + admins).send(event.getChannel());
                break;
            case prefix + "linkdiscord":
                if (event.isPrivateMessage()) return;
                if ((incoming_msg.length > 1) && !linkaccount) {
                    try {
                        Player found = null;
                        int id = Strings.parseInt(incoming_msg[1]);
                        for (Player p : Groups.player) {
                            if (p.id == id) {
                                found = p;
                                break;
                            }
                        }
                        if (dataHandler.isLinked(found)) {
                            event.getChannel().sendMessage("Please check in-game to link your discord account to your in-game account.");
                            try {
                                Objects.requireNonNull(found).sendMessage("[accent]User " + "[royal]" + event.getMessageAuthor().getName() + "[accent]" + " wants to link to your mindustry account to discord, please return /link if thats the case, you have " + CDT + " seconds to do so.");
                            }
                            catch (NullPointerException ignored){
                                event.getChannel().sendMessage("Error, player has either left or is null, try again.");
                                linkaccount = false;
                                return;
                            }
                            linkaccount = true;
                            linkevent = event;
                            found2 = found;
                            for (Long key : cooldowns.keys()) {
                                if (key + CDT < System.currentTimeMillis() / 1000L) {
                                    cooldowns.remove(key);
                                    try {
                                        Objects.requireNonNull(found).sendMessage("[scarlet]Time has run out, please try again.");
                                    }
                                    catch (NullPointerException ignored){
                                        continue;
                                    }
                                    event.getChannel().sendMessage("Time has run out, please try again. " + "<@!" + event.getMessageAuthor().getId() + ">");
                                    linkaccount = false;
                                }
                            }
                        }
                        else if (dataHandler.isLinked(found)){
                            event.getChannel().sendMessage("User is already linked to discord, contact an administrator if this is incorrect.");
                        }
                        else {
                            event.getChannel().sendMessage("User is not registered, please register in-game first or log in to fix your information.");
                        }
                    }
                    catch (Exception e){
                        discLogErr("Error linking discord account to in-game account", e);
                    }
                }
                else if (linkaccount){
                    event.getChannel().sendMessage("Please wait for the previous request to finish in " + CDT + " seconds.");
                }

                else {
                    StringBuilder list3 = new StringBuilder();
                    list3.append("Players: ").append(Groups.player.size()).append("\n");
                    for (Player p : Groups.player){
                        if (p.admin()){
                            list3.append("* ").append(Strings.stripColors(p.name)+" ("+p.id+")").append("\n");
                        } else {
                            list3.append("* ").append(Strings.stripColors(p.name)+" ("+p.id+")").append("\n");
                        }
                    }
                    new MessageBuilder().appendCode("", list3.toString()).send(event.getChannel());

                    event.getChannel().sendMessage("Please send your player ID to link your discord account to your in-game account (e.g. `"+prefix+"linkdiscord 1234`)");
                }
                break;
            case prefix + "info":
                try {
                    new MessageBuilder()
                            .setEmbed(new EmbedBuilder()
                                    .setTitle("Info for " + Strings.stripColors(Administration.Config.serverName.get().toString()))
                                    .setDescription("Map: " + Vars.state.map.name() + " by: " + Vars.state.map.author())
                                    .addField("Players: ", String.valueOf(Groups.player.size()))
                                    .addField("TPS: ", String.valueOf(Core.graphics.getFramesPerSecond()))
                                    .addField("Wave", String.valueOf(Vars.state.wave))
                                    .addField("Enemies: ", String.valueOf(Vars.state.enemies))
                                    .setImage(renderMinimap())
                                    .setFooter("Hosted proudly by Yeet Hosting from \uD83C\uDDE9\uD83C\uDDEA !")
                                    .setColor(Color.ORANGE))
                            .send(event.getChannel());
                } catch (Exception e) {
                    discLogErr(e.getMessage(), e);
                }
                break;
            case prefix + "infores":
                //event.getChannel().sendMessage("not implemented yet...");
                if (!Vars.state.rules.waves){
                    event.getChannel().sendMessage("Only available when playing survivalmode!");
                    return;
                } else {
                    StringBuilder lijst3 = new StringBuilder();
                    lijst3.append("Amount of items in the core\n\n");
                    ItemModule core = Vars.state.rules.defaultTeam.core().items;
                    core.each((i, a) -> lijst3.append(i.name).append(" ").append(a).append("\n"));
                    new MessageBuilder().appendCode("", lijst3.toString()).send(event.getChannel());
                }
                break;
            //ban command
            case prefix + "tempban":
                if (moderator == null) {
                    if (event.isPrivateMessage()) return;
                    event.getChannel().sendMessage("You do not have permission to use this command");
                    return;
                }
                if (!hasPermission(moderator, event)) return;
                if (incoming_msg.length < 2) {
                    event.getChannel().sendMessage("Please specify a player to ban");
                    StringBuilder list2 = new StringBuilder();
                    list2.append("Players: ").append(Groups.player.size()).append("\n");
                    for (Player p : Groups.player){
                        if (p.admin()){
                            list2.append("* ").append(Strings.stripColors(p.name)+" ("+p.id+")").append("\n");
                        } else {
                            list2.append("* ").append(Strings.stripColors(p.name)+" ("+p.id+")").append("\n");
                        }
                    }
                    new MessageBuilder().appendCode("", list2.toString()).send(event.getChannel());

                    return;
                }
                Player found = null;
                int id = Strings.parseInt(incoming_msg[1]);
                for (Player p : Groups.player) {
                    if (p.id == id) {
                        found = p;
                        break;
                    }
                }
                if (found == null) {
                    event.getChannel().sendMessage("Player not found");
                    return;
                }
                LocalDateTime dateObject = LocalDateTime.now().plusMinutes(Long.parseLong(incoming_msg[3]));
                DateTimeFormatter formatTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                String formattedDate = dateObject.format(formatTime);

                found.con.kick("[accent]You have been banned from the server by "+"[royal]"+event.getMessageAuthor().getName() + "\n[accent]For: "+'"'+incoming_msg[2]+'"'+ "\nIf you think this is a mistake please contact us on dsc.gg/omegahub \n[red]You are banned until: \n" + formattedDate + " GMT +1", Long.parseLong(incoming_msg[3]));
                event.getChannel().sendMessage("Player banned");
                break;
            case prefix + "map":
                var map = Find.map(incoming_msg[1]);
                if (map == null) {
                    event.getChannel().sendMessage("Map not found");
                    return;
                }
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setColor(Color.cyan)
                                .setTitle(map.name())
                                .setDescription(map.tags.get("description", ""))
                                .setAuthor(map.tags.get("author", ""))
                                .setFooter(map.width + "x" + map.height)
                                .setImage(renderMap(map), "map.png"))
                        .send(event.getChannel());
                event.getChannel().sendMessage(map.file.readByteStream(), map.file.name());
                break;
            case prefix + "help":
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setTitle("Commands:")
                                .addField("chat <message>", "Send a message to the server chat")
                                .addField("players", "List all players online")
                                .addField("info", "Get info about the map")
                                .addField("maps", "List all maps")
                                .addField("map <map name>", "Get info about a map and its file")
                                .addField("infores", "Get info about the resources in the core")
                                .addField("linkdiscord", "Link your discord account to your in-game account")
                                .addField("help", "Show this message")
                                .setColor(Color.CYAN)
                                .setFooter("Time is " + dtf.format(now) + " on " + Strings.stripColors(Administration.Config.serverName.get().toString())))
                        .send(event.getChannel());
                break;
            default:
                throw new IllegalStateException("Nonexistent Command: " + incoming_msg[0]);
        }
    }
}