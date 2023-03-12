package omega;

// Arc
import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.*;

// Argon
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

// Modlib
import fr.redstonneur1256.modlib.net.NetworkDebuggable;

// Mindustry
import mindustry.game.EventType;
import mindustry.game.EventType.PlayerJoin;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Sounds;
import mindustry.mod.Plugin;
import mindustry.net.Packets;

// Omega
import omega.command.comCommands;
import omega.command.mapCommands;
import omega.command.serverCommands;
import omega.database.dataHandler;
import omega.utils.API;

// Javacord
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;

// Json
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

// Java
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

// Statics
import static arc.util.Log.err;
import static mindustry.Vars.netServer;
import static mindustry.Vars.player;
import static omega.utils.Constants.*;
import static omega.utils.Logger.discLog;
import static omega.utils.Logger.discLogErr;
import static omega.utils.discordUtils.*;

public class OmegaPlugin extends Plugin {
    private final long CDT = 300L;

    private static DiscordApi api = null;
    private final ObjectMap<Long, String> cooldowns = new ObjectMap<>(); //uuid
    private boolean invalidConfig = false;
    public static ObjectMap<String, Role> discRoles = new ObjectMap<>();
    public static ObjectMap<String, TextChannel> discChannels = new ObjectMap<>();
    public static final dataHandler mCH = new dataHandler();
    private String data = null;
    private static String format = "[coral]] [accent]>[white] ";
    private int i = 0;
    Argon2 argon2 = Argon2Factory.create();

    //private JSONObject config;
    private final String totalPath;
    public static String servername;

    //register event handlers and create variables in the constructor
    public OmegaPlugin() {
        //getting the config file:
        Fi path = Core.settings.getDataDirectory().child(diPath);
        totalPath = path.child(fileName).absolutePath();


        JSONObject config = null;

        if(path.exists()) {
            discLog("PATH EXISTS");
            String pureJSON = Fi.get(totalPath).readString();

            config = new JSONObject(new JSONTokener(pureJSON));
            if(!config.has("version")) {
                makeSettingsFile();
            } else if(config.getInt("version") < VERSION) {
                discLog("configfile: VERSION");
                makeSettingsFile();
            }
        } else {
            makeSettingsFile();
        }

        if(config == null || invalidConfig) return;

        readSettingsFile(config);

        //communication commands
        api.addMessageCreateListener(new comCommands(this));
        //server management commands
        api.addMessageCreateListener(new serverCommands(this));
        api.addMessageCreateListener(new mapCommands(this));
        try{
            API.main();
            discLog("API up and running!");
        }
        catch(Exception e){
            discLogErr("There was an error while starting the HTTP Server! ", e);
        }

        try {
            dataHandler.Init();
        } catch (Exception e) {
            err("There was an error while connecting to the database! ", e);
        }

        Thread mainThread = Thread.currentThread();
        mainThread.setUncaughtExceptionHandler((thread, throwable) -> sendCrashReport("crashed"));
        Core.app.addListener(new ApplicationListener() {
            @Override
            public void exit() {
                sendCrashReport("stopped");
            }
        });
        //live chat
        //TextChannel tc = discChannels.get("live_chat_channel_id");
        TextChannel tc2 = discChannels.get("ban_chat_channel_id");

        /*if(tc != null) {
            Events.on(PlayerChatEvent.class, event -> tc.sendMessage(filterMentions(api.getServerById(serverID).get(), "<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + Strings.stripColors(event.player.name).replace('*', '+') + "**: " + Strings.stripColors(event.message))));
            Events.on(PlayerLeave.class, event -> tc.sendMessage(filterMentions(api.getServerById(serverID).get(), "<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + Strings.stripColors(event.player.name).replace('*', '+') + "** " + "***has Left***")));
            Events.on(PlayerJoin.class, event -> tc.sendMessage(filterMentions(api.getServerById(serverID).get(), "<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + Strings.stripColors(event.player.name).replace('*', '+') + "** " + "***has Joined***")));
            Events.on(PlayerConnect.class, event -> tc.sendMessage(filterMentions(api.getServerById(serverID).get(), "<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + Strings.stripColors(event.player.name).replace('*', '+') + "** " + "*is Connecting*")));
        }*/
        if(tc2 != null) {
            Events.on(EventType.PlayerBanEvent.class, event -> {
                try {
                    tc2.sendMessage(filterMentions(api.getServerById(serverID).get(), "<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + Strings.stripColors(event.player.name).replace('*', '+') + "** " + "*has been* **Banned**"));
                }
                catch (Exception Ignored){
                }
            });
            Events.on(EventType.PlayerUnbanEvent.class, event -> {
                try {
                    tc2.sendMessage(filterMentions(api.getServerById(serverID).get(), "<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + Strings.stripColors(event.player.name).replace('*', '+') + "** " + "*has been* **Unbanned**"));
                }
                catch (Exception Ignored){
                }
            });
            Events.on(EventType.PlayerIpBanEvent.class, event -> {
                try {
                    tc2.sendMessage("<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + event.ip.replace('*', '+') + "** " + "*has been* **Banned**");
                }
                catch (Exception Ignored){
                }
            });
            Events.on(EventType.PlayerIpUnbanEvent.class, event -> {
                try {
                    tc2.sendMessage("<t:" + Instant.now().getEpochSecond() + ":R>" + " " + "**" + event.ip.replace('*', '+') + "** " + "*has been* **Unbanned**");
                }
                catch (Exception Ignored){
                }
            });
        }
    }

    private static void get(EventType.PlayerChatEvent event) {
        Player p = event.player;
        String messag = event.message;
        var asList = Arrays.asList("/t", "/a", "/");
        for (String s : asList) {
            if (messag.startsWith(s)) {
                return;
            }
        }
        var regList = new AtomicReference<java.util.List<String>>(Arrays.asList("register", "login", "logein", "reg", "regis", "regi", "logoin", "logi", "loginn"));
        for (String s : regList.get()) {
            if (messag.startsWith(s)) {
                player.sendMessage("We have detected that you are trying to register or login, you almost leaked your password, be more careful next time!.");
            }
        }

        final BiFunction<String, String, String> parse_formatter = (String hexcolor, String unicode) -> {
            return "[#" + hexcolor + "]<[accent]" + unicode + "[white]/" + mCH.check_xp(p) + "[#" + hexcolor + "]> [coral][[" + p.coloredName() + format + messag;
        };

        switch (mCH.check_rank(p)) {
            case "Omega":
                netServer.chatFormatter = (player, message) -> parse_formatter("FFFF00","\uF308");
            case "Volas":
                netServer.chatFormatter = (player, message) -> parse_formatter("17e2e2","\uE80E");
            case "Gamma+":
                netServer.chatFormatter = (player, message) -> parse_formatter("24abc6","\uE88E");
            case "ae": 
                netServer.chatFormatter = (player, message) -> parse_formatter("FEF508","\uF281");
            case "Gamma": 
                netServer.chatFormatter = (player, message) -> parse_formatter("D2272e","\uE80F");
            case "Beta": 
                netServer.chatFormatter = (player, message) -> parse_formatter("153f9a","\uE84D");
            case "Epsilon": 
                netServer.chatFormatter = (player, message) -> parse_formatter("0be86f","\uE86E");
            case "Verified": 
                netServer.chatFormatter = (player, message) -> parse_formatter("229a76","\uE800");
            case "Donator": 
                netServer.chatFormatter = (player, message) -> parse_formatter("E6dd44","\uE810");
            case "Player": 
                netServer.chatFormatter = (player, message) -> parse_formatter("82ad3a","\uE871");
            default: 
                netServer.chatFormatter = (player, message) -> "[#A6a7a8]<[accent]\uE872[white][#A6a7a8]> [coral][[" + p.coloredName() + format + messag;
        }
    }

    public void init(){
        omega.utils.mapParse.load();
        Events.on(PlayerJoin.class,event-> {
            Player p = event.player;
            String rank = mCH.check_rank(p);
            if (mCH.isBanned(p)){
                p.con.kick("[scarlet]You are banned from this server. If you think this is a mistake, contact us on dsc.gg/omegahub.");
            }
            if (Objects.equals(rank, "Admin") || Objects.equals(rank,"Moderator") || Objects.equals(rank,"Epsilon") || Objects.equals(rank, "Volas") || Objects.equals(rank, "Omega") || Objects.equals(rank, "lilgamma") || Objects.equals(rank, "ae")) {
                p.sendMessage("[red]Welcome Staff Member, go hunt some shitasses!");
                p.admin(true);
            }
            Call.announce(p.con, "[royal]Welcome to a Omega Hub Quality Server! Our Discord is: com.mindustry.me, if you like our server, donations are appreciated!");
            p.sendMessage("[green]To get a complete list of commands, type /help!");
            if (!mCH.check_regist(p)) {
                p.sendMessage("[green]If you see this, we recommend you to /register or /login if you already have an account to get the full OmegaHub Experience!");
            }
        });
        Runnable updateSec = new Runnable() {
            public void run() {
                Timekeeper min = new Timekeeper(1);
                Timekeeper keeper = new Timekeeper(3);
                if (keeper.get()) {
                    if (Groups.player.size() > 0) api.updateActivity("with " + Groups.player.size() + " players.");
                    else api.updateActivity("with no one :(");
                }
                if (min.get()) {
                    for (Player p : Groups.player) {
                        int playtim = mCH.check_playtime(p);
                        playtim++;
                        try {
                            mCH.update_playtime(p, playtim);
                            return;
                        } catch (SQLException e) {
                            discLogErr(e);
                            return;
                        }
                    }
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(updateSec, 0, 1, TimeUnit.SECONDS);
        Events.on(EventType.PlayerBanEvent.class, event -> {
            try {
                Player p = event.player;
                Call.infoMessage(p.con, "[scarlet]You have been banned from this server. If you believe this is a mistake, please appeal at https://discord.gg/heKa8Kdw8g.");
            }
            catch (Exception e) {
            }
        });
        Events.on(EventType.BlockBuildBeginEvent.class, (EventType.BlockBuildBeginEvent event) -> {
            i++;
            if(i == 250){
                i = 0;
                Player p = event.unit.getPlayer();
                try {
                    mCH.update_xp(Objects.requireNonNull(p), Integer.parseInt(mCH.check_xp(p))+1);
                } catch (NullPointerException e) {
                    discLogErr("XPUPDATE "+PlayerIsNullErrorMessage);
                }
            }
        });
        Events.on(EventType.DepositEvent.class, (EventType.DepositEvent event) -> {
            i++;
            if(i == 250){
                i = 0;
                Player p = event.player;
                try {
                    mCH.update_xp(Objects.requireNonNull(p), Integer.parseInt(mCH.check_xp(p))+1);
                } catch (NullPointerException e) {
                    discLogErr("XPUPDATE "+PlayerIsNullErrorMessage);
                }
            }
        });
        Events.on(EventType.UnitSpawnEvent.class, (EventType.UnitSpawnEvent unitSpawnEvent) -> {
            i++;
            if(i == 250){
                i = 0;
                Player p = unitSpawnEvent.unit.getPlayer();
                try {
                    mCH.update_xp(Objects.requireNonNull(p), Integer.parseInt(mCH.check_xp(p))+1);
                } catch (NullPointerException e) {
                    discLogErr("XPUPDATE "+PlayerIsNullErrorMessage);
                }
            }
        });
        Events.on(EventType.PlayerChatEvent.class, OmegaPlugin::get);
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("ping", "Display the ping of everyone on the server", args -> {
            Log.info("Ping of players:");
            for(Player player : Groups.player) {
                long ping = ((NetworkDebuggable) player).getPing();
                Log.info("- @: @ ms", player.name, ping);
            }
        });
        handler.register("exitquickly", "Kills the server (safely) as fast as possible.", (args)->{
            try{
                api.disconnect();
            }catch(Exception ignored){}
            Timer.schedule(()-> {
                System.exit(0);
            }, 1f);
        });
        handler.removeCommand("exit");
        handler.register("exit", "Shut the server down.", OmegaPlugin::kill);
        handler.register("changerank", "<username/ID> <rank...>", "Change a rank of someone based off database.", arg -> {
            if (arg[0].equals("id")) {
                String rank = arg[2];
                if (rank.equals("Gamma") || rank.equals("Beta") || rank.equals("Epsilon") || rank.equals("Verified") || rank.equals("Player")) {
                    try {
                        mCH.update_rank(arg[1], rank);
                        Log.info("Rank changed.");
                    } catch (Exception e) {
                        Log.info("Error Updating Rank : " + e);
                    }
                } else {
                    Log.info("Invalid rank.");
                }

            } else if (arg[0].equals("name")) {
                Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[1]));
                if (target != null) {
                    String rank = arg[2];
                    if (rank.equals("Gamma") || rank.equals("Beta") || rank.equals("Epsilon") || rank.equals("Verified") || rank.equals("Player")) {
                        try {
                            mCH.update_rank(target.uuid(), rank);
                            Log.info("Rank changed.");
                        } catch (Exception e) {
                            Log.info("Error Updating Rank : " + e);
                        }
                    } else {
                        Log.info("Invalid rank.");
                    }
                } else {
                    err("No matches found.");
                }
            } else {
                err("Invalid type.");
            }
        });
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler) {
        discLog("- Command '/ping' enabled");
        handler.register("ping", "Display the ping of everyone on the server", args -> {
            StringBuilder builder = new StringBuilder();
            builder.append("[orange]Ping of players: \n");
            for(Player player : Groups.player) {
                long ping = ((NetworkDebuggable) player).getPing();
                builder.append("[accent]- []").append(player.name).append("[accent] : ").append(ping).append(" ms").append("\n");
            }
            Call.infoMessage(builder.toString());
        });
        discLog("- Command '/link' enabled");
        handler.<Player>register("link", "", "links your account", (args, player) -> {
            if (comCommands.linkaccount == true && comCommands.found2 == player){
                comCommands.found2.sendMessage("[green]Your account has been linked.");
                dataHandler.set_dc(String.valueOf(comCommands.linkevent.getMessageAuthor().getId()), player.uuid());
                if (dataHandler.check_rank(player) == "Player"){
                    dataHandler.set_rank(player, "Verified");
                }
                comCommands.linkevent.getChannel().sendMessage("Linked :white_check_mark:");
                comCommands.linkaccount = false;
                return;
            }
            else if (comCommands.found2 != player && comCommands.linkaccount == true){
                player.sendMessage("[red]Another person is trying to verify at the moment, try again in a few minutes.");
                return;
            }
            else {
                player.sendMessage("[green]Either a very rare error happened or you haven't done linkdiscord in the discord yet.");
                return;
            }
        });
        discLog("- Command '/discord' enabled");
        handler.<Player>register("discord", "", "Sends you the discord link", (args, player) -> {
            Call.openURI(player.con, "https://discord.gg/heKa8Kdw8g");
        });
        discLog("- Command '/ranks' enabled");
        handler.<Player>register("ranks", "", "Sends you the ranks list", (args, player) -> {
            player.sendMessage("[green]Ranks List:");
            player.sendMessage("[#A6a7a8]<[accent]\uE872[#A6a7a8]>: You have not registered, you can do so by using the /register command.");
            player.sendMessage("[#82ad3a]<[accent]\uE871[#82ad3a]>: You have verified your account, you can now use the /link command.");
            player.sendMessage("[#229a76]<[accent]\uE800[#229a76]>: You have linked your account!");
            player.sendMessage("[#E6dd44]<[accent]\uE810[#E6dd44]>: You have donated to the server!");
            player.sendMessage("[#0be86f]<[accent]\uE86E[#0be86f]>: Trial Moderator, basically Helper.");
            player.sendMessage("[#153f9a]<[accent]\uE84D[#153f9a]>: Moderator.");
            player.sendMessage("[#D2272e]<[accent]\uE80F[#D2272e]>: Administrator.");
            player.sendMessage("[#24abc6]<[accent]\uE88E[#24abc6]>: Head Administrator.");
            player.sendMessage("[#17e2e2]<[accent]\uE80E[#17e2e2]>: CO-Owner.");
            player.sendMessage("[#FFFF00]<[accent]\uF308[#FFFF00]>: Owner.");
        });

        discLog("- Command '/register' enabled");
        handler.<Player>register("register", "<username> <password...>", "Registers you into our database", (args, player) -> {
            try {
                if (!mCH.check_regist(player)) {
                    if(!player.admin) {
                        mCH.send_registry(player, "Player", args[0], argon2.hash(2, 65536, 1, args[1]));
                        player.sendMessage("[green]You have successfully registered your account!");
                    }
                    else if (player.admin){
                        mCH.send_registry(player, "Gamma", args[0], argon2.hash(2, 65536, 1, args[1]));
                        player.sendMessage("[green]You have successfully registered your account!");
                    }
                }else {
                    player.sendMessage("[scarlet]This username is already taken or you already have an account!");
                }
            }
            catch (Exception e) {
                discLog(e);
                player.sendMessage("[red]Error, Please Contact an Admin and send them this error code: [accent]47");
            }
        });
        discLog("- Command '/login' enabled");
        handler.<Player>register("login", "<username> <password...>", "Logs you into our database to get your stats and update your info", (args, player) -> {
            data = mCH.check_registry(player, args[0], args[1]);
            String username = null;
            String pass = null;
            String rank;
            String joindate;
            String playtime;
            String xp;
            try {
                org.json.simple.JSONObject obj = (org.json.simple.JSONObject) new JSONParser().parse(data);
                username = (String) obj.get("username");
                pass = (String) obj.get("password");
                rank = (String) obj.get("rank");
                xp = (String) obj.get("xp");
                playtime = (String) obj.get("playtime");
                joindate = (String) obj.get("joindate");
                int playtimeint = Integer.parseInt(playtime);
                int hours = playtimeint / 3600;
                int minutes = (playtimeint % 3600) / 60;
                int seconds = playtimeint % 60;

                String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                Date date = new Date(Long.parseLong(joindate)*1000L);
                SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd");
                jdf.setTimeZone(TimeZone.getTimeZone("GMT"));

                String javadate = jdf.format(date);
                if (Objects.equals(username, args[0]) && argon2.verify(pass, args[1]) && rank == "Gamma" || rank == "Beta" || rank == "Epsilon" || rank == "Gamma+" || rank == "Volas") {
                    player.sendMessage("[green]Welcome Back Appreciated Administrator " + username + "!");
                    player.sendMessage("[accent]Your Rank is: " + rank);
                    player.sendMessage("[accent]Your XP is: " + xp);
                    player.sendMessage("[accent]You Registered At: " + javadate);
                    player.sendMessage("[accent]Your Playtime is: " + timeString + "(HH:MM:SS)");
                    mCH.update_registry(player, args[0], args[1]);
                    player.admin = true;
                }
                if (Objects.equals(username, args[0]) && argon2.verify(pass, args[1])) {
                    player.sendMessage("[green]You have been logged in successfully!");
                    player.sendMessage("[accent]Your Rank is: " + rank);
                    player.sendMessage("[accent]Your XP is: " + xp);
                    player.sendMessage("[accent]You Registered At: " + javadate);
                    player.sendMessage("[accent]Your Playtime is: " + timeString + "(HH:MM:SS)");
                    mCH.update_registry(player, args[0], args[1]);
                }

            } catch (ParseException e) {
                discLogErr(e);
                player.sendMessage("[red]Error, Please Contact an Admin and send them this error code: [accent]59");
            }

            if (data == null) {
                player.sendMessage("[red]You are not registered!");
            } else if (!argon2.verify(pass, args[1]) || !Objects.equals(username, args[0])) {
                player.sendMessage("[red]Incorrect Username or Password!");
            }
        });
        TextChannel tc_d = discChannels.get("dchannel_id");
        discLog("- Command '/d' enabled");
        handler.<Player>register("d", "<text...>", "Sends a message to discord.", (args, player) -> {
            tc_d.sendMessage(filterMentions(api.getServerById(serverID).get(), Strings.stripColors(player.name) + " *From Mindustry* : " + args[0]));
            Call.sendMessage(Strings.stripColors(player.name) + "[sky] to Discord[]: " + args[0]);
        });
        TextChannel tc_c = discChannels.get("channel_id");
        discLog("- JS fooler enabled");
        handler.<Player>register("js", "<code...>", "Execute JavaScript code.", (args, player) -> {
            Call.infoMessage(player.con, "[yellow] Theres 2R2T for /js, but no worries, just press ok, you have been redirected already :)");
            Call.connect(player.con, "n1.mindustry.me", 6568);
        });
        Role ro = discRoles.get("role_id", (Role) null);
        discLog("- Command '/ban' enabled");
        handler.<Player>register("ban", "<player> <reason...>", "Bans a player.", (args, player) -> {
            if(player.admin) {
                if(args.length == 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("[orange]List of bannable players: \n");
                    for(Player p : Groups.player) {
                        if(p.admin() || p.con == null) continue;

                        builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id).append(")\n");
                    }
                    player.sendMessage(builder.toString());
                } else {
                    Player found = null;
                    if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))) {
                        int id = Strings.parseInt(args[0].substring(1));
                        for(Player p : Groups.player) {
                            if(p.id == id) {
                                found = p;
                                break;
                            }
                        }
                    } else {
                        for(Player p : Groups.player) {
                            if(p.name.equalsIgnoreCase(args[0])) {
                                found = p;
                                break;
                            }
                        }
                    }
                    if(found != null) {
                        if(found.admin()) {
                            player.sendMessage("[red]Did you really expect to be able to ban an admin?");
                        } else {
                            //send message
                            if(args.length > 1) {
                                new MessageBuilder()
                                        .setEmbed(new EmbedBuilder()
                                                .setTitle("Ban")
                                                .setDescription(ro.getMentionTag())
                                                .addField("Name", found.name)
                                                .addField("Reason", args[1])
                                                .setColor(Color.ORANGE)
                                                .setFooter("Banned by " + player.name))
                                        .send(tc_c);
                            } else {
                                new MessageBuilder()
                                        .setEmbed(new EmbedBuilder()
                                                .setTitle("Ban")
                                                .setDescription(ro.getMentionTag())
                                                .addField("Name", found.name)
                                                .setColor(Color.ORANGE)
                                                .setFooter("Banned by " + player.name))
                                        .send(tc_c);
                            }
                            tc_c.sendMessage(ro.getMentionTag());
                            player.sendMessage("[red]Banned.");
                        }
                    } else {
                        player.sendMessage("[scarlet]No such player[orange] '" + args[0] + "'[scarlet] found.");
                    }
                }
            } else {
                player.sendMessage("[scarlet]lol no.");
            }
        });
        discLog("- Command '/gr' enabled");
        handler.<Player>register("gr", "[player] [reason...]", "Report a griefer by id (use '/gr' to get a list of ids)", (args, player) -> {
            //https://github.com/Anuken/Mindustry/blob/master/core/src/io/anuke/mindustry/core/NetServer.java#L300-L351

            for(Long key : cooldowns.keys()) {
                if(key + CDT < System.currentTimeMillis() / 1000L) {
                    cooldowns.remove(key);
                } else if(Objects.equals(player.uuid(), cooldowns.get(key))) {
                    player.sendMessage("[scarlet]This command is on a 5 minute cooldown!");
                    return;
                }
            }

            if(args.length == 0) {
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]List or reportable players: \n");
                for(Player p : Groups.player) {
                    if(p.admin() || p.con == null) continue;

                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id).append(")\n");
                }
                player.sendMessage(builder.toString());
            } else {
                Player found = null;
                if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))) {
                    int id = Strings.parseInt(args[0].substring(1));
                    for(Player p : Groups.player) {
                        if(p.id == id) {
                            found = p;
                            break;
                        }
                    }
                } else {
                    for(Player p : Groups.player) {
                        if(p.name.equalsIgnoreCase(args[0])) {
                            found = p;
                            break;
                        }
                    }
                }
                if(found != null) {
                    if(found.admin()) {
                        player.sendMessage("[red]Did you really expect to be able to report an admin?");
                    } else if(found.team() != player.team()) {
                        player.sendMessage("[scarlet]Only players on your team can be reported.");
                    } else {
                        //send message
                        if(args.length > 1) {
                            new MessageBuilder()
                                    .setEmbed(new EmbedBuilder()
                                            .setTitle("Potential griefer online")
                                            .setDescription(ro.getMentionTag())
                                            .addField("Name", found.name)
                                            .addField("Reason", args[1])
                                            .setColor(Color.ORANGE)
                                            .setFooter("Reported by " + player.name))
                                    .send(tc_c);
                        } else {
                            new MessageBuilder()
                                    .setEmbed(new EmbedBuilder()
                                            .setTitle("Potential griefer online")
                                            .setDescription(ro.getMentionTag())
                                            .addField("Name", found.name)
                                            .setColor(Color.ORANGE)
                                            .setFooter("Reported by " + player.name))
                                    .send(tc_c);
                        }
                        tc_c.sendMessage(ro.getMentionTag());
                        player.sendMessage("[green]Report sent.");
                        cooldowns.put(System.currentTimeMillis() / 1000L, player.uuid());
                    }
                } else {
                    player.sendMessage("[scarlet]No such player[orange] '" + args[0] + "'[scarlet] found.");
                }
            }
        });
    }

    //getters
    public DiscordApi getAPI() {
        return this.api;
    }


    private void readSettingsFile(JSONObject obj) {
        if(obj.has("token")) {
            try {
                api = new DiscordApiBuilder().setToken(obj.getString("token")).setAllIntents().login().join();
                discLog("Valid token");
            } catch(Exception e) {
                if(e.getMessage().contains("READY packet")) {
                    discLog("invalid token");
                } else {
                    e.printStackTrace();
                }
                invalidConfig = true;
                return;
            }
        } else {
            invalidConfig = true;
            discLog(FileNotFoundErrorMessage + "or invalid configuration");
            return;
        }

        if(obj.has("channel_ids")) {
            JSONObject temp = obj.getJSONObject("channel_ids");
            for(String field : temp.keySet()) {
                discChannels.put(field, getTextChannel(api, temp.getString(field)));
            }
        }

        if(obj.has("role_ids")) {
            JSONObject temp = obj.getJSONObject("role_ids");
            for(String field : temp.keySet()) {
                discRoles.put(field, getRole(api, temp.getString(field)));
            }
        }

        if(obj.has("servername")) {
            servername = obj.getString("servername");
        } else {
            servername = "";
        }

        discLog("config loaded");
    }


    private void makeSettingsFile() {
        discLog("CREATING JSON FILE");
        Fi directory = Core.settings.getDataDirectory().child(diPath);
        if(!directory.isDirectory()) {
            directory.mkdirs();
        }

        JSONObject config = new JSONObject();
        config.put("info", "more info available on: https://github.com/Omega-Network/DiscordPlugin");
        config.put("version", VERSION);

        config.put("servername", "name of your server - can be empty");

        config.put("token", "put your token here");

        JSONObject channels = new JSONObject();
        channels.put("dchannel_id", "messages using /d will be send to this channel - can be empty");
        channels.put("channel_id", "id of the channel where /gr reports will appear - can be empty");
        channels.put("live_chat_channel_id", "id of the channel where live chat will appear - can be empty");
        channels.put("ban_chat_channel_id", "id of the channel where all ban/kicks will appear - can be empty");
        channels.put("stats_channel_id", "id of the channel where all stats will appear - can be empty");

        config.put("channel_ids", channels);

        JSONObject roles = new JSONObject();
        String[] discordRoles = {
                "closeServer_role_id",
                "gameOver_role_id",
                "changeMap_role_id",
                "serverDown_role_id",
                "role_id"
        };
        for(String field : discordRoles) {
            roles.put(field, "");
        }

        config.put("role_ids", roles);

        discLog("Creating config.json");
        try {
            Files.write(Paths.get(totalPath), config.toString().getBytes());
        } catch(Exception e) {
            discLog("Failed to create config.json");
        }
    }
    public static void kill(String[] deth){
        int countDownMax = 20;
        discLog("Starting shut down sequence");

        AtomicInteger countdown = new AtomicInteger(countDownMax);
        Timer.schedule(()->{
            Time.runTask(20f,() -> {
                Call.setHudText("[#f]Server restarting in [accent]" + countdown.get() + "[] seconds!");
            });
            discLog("Server shutting down in " + countdown.get() + " seconds");
            Call.sound(Sounds.wind3, 1, 1, 0);
            countdown.getAndDecrement();
            Call.hideHudText();
        }, 0f, 1f);

        Time.runTask(1210f,() -> {
            Call.announce("[#f]Server restarting now");
            Groups.player.each((p) -> p.con.kick(Packets.KickReason.serverRestarting));
            discLog("Shutting down server");
            Core.app.exit();
            System.exit(0);
        });
    }
    private static void sendCrashReport(String reason) {
        Role downRole = discRoles.get("serverdown_role_id");
        TextChannel downChannel = discChannels.get("serverdown_channel_id");

        if(downRole != null && downChannel != null) {
            String message = servername.isEmpty() ?
                    "%s\nUnknown server %s".format(downRole.getMentionTag(), reason) :
                    "%s\nServer **%s** %s".format(downRole.getMentionTag(), servername, reason);

            try {
                new MessageBuilder()
                        .append(message)
                        .send(downChannel)
                        .join(); // wait for the message to be sent
            } catch(CompletionException ignored) {
            }
        }

        api.disconnect();
    }

}