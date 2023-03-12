package omega.command;


import arc.Core;
import arc.Events;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.net.Packets;
import omega.OmegaPlugin;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.concurrent.atomic.AtomicInteger;

import static omega.utils.Constants.prefix;
import static omega.utils.Logger.discLog;
import static omega.utils.discordUtils.hasPermission;


public class serverCommands implements MessageCreateListener {
    final String commandDisabled = "You tried, thats all that matters.";

    private final OmegaPlugin mainData;

    public serverCommands(OmegaPlugin _data){
        this.mainData = _data;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        String[] incoming_msg = event.getMessageContent().split("\\s+");

        switch (incoming_msg[0]){
            case prefix + "gameover":
                Role gameOverRole = mainData.discRoles.get("gameOver_role_id");
                if (gameOverRole == null){
                    if (event.isPrivateMessage()) return;
                    event.getChannel().sendMessage(commandDisabled);
                    return;
                }
                if (!hasPermission(gameOverRole, event)) return;
                // ------------ has permission --------------
                if (Vars.state.is(GameState.State.menu)) {
                    return;
                }
                event.getChannel().sendMessage("Game Over event fired at Wave "+Vars.state.wave+" \uD83D\uDD25");
                Events.fire(new GameOverEvent(Team.crux));
                break;
            case prefix + "restart":
                Role closeServerRole = mainData.discRoles.get("closeServer_role_id");
                if (closeServerRole == null) {
                    if (event.isPrivateMessage()) return;
                    event.getChannel().sendMessage(commandDisabled);
                    return;
                }
                if (!hasPermission(closeServerRole, event)) return;
                int countDownMax = 20;

                discLog("Starting restart sequence (actioned from discord)");

                AtomicInteger countdown = new AtomicInteger(countDownMax);
                event.getChannel().sendMessage("Server restart sequence initiated.").thenAcceptAsync((message) -> {
                    Timer.schedule(()->{
                    Time.runTask(20f,() -> {
                                message.edit("Server restarting in " + countdown.get() + " seconds!");
                                if (countdown.get() == 0) {
                                    message.edit("Server restarting now!");
                            }
                        Call.setHudText("[#f]Server restarting in [accent]" + countdown.get() + "[] seconds!");
                    });

                    discLog("Server Restart in " + countdown.get() + " seconds");
                    Call.sound(Sounds.wind3, 1, 1, 0);
                    countdown.getAndDecrement();
                    Call.hideHudText();
                }, 0f, 1f);
                   });
                Time.runTask(1210f,() -> {
                    Call.announce("[#f]Server restarting now");
                    Groups.player.each((p) -> p.con.kick(Packets.KickReason.serverRestarting));
                    discLog("Restarting server");
                    Core.app.exit();
                    System.exit(0);
                });
                break;
            default:
                break;
        }
    }
}