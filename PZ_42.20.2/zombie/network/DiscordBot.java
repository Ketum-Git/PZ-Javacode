// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.server.IEventController;
import zombie.util.StringUtils;

public class DiscordBot implements IEventController {
    private final DiscordSender sender;
    private final String serverName;
    private TextChannel chatChannel;
    private TextChannel logChannel;
    private TextChannel commandChannel;

    @Override
    public void process(String event) {
        this.logEvent(event);
    }

    public void logEvent(String event) {
        if (this.logChannel != null) {
            this.logChannel.sendMessage(event);
        }
    }

    public void sendMessage(String user, String text) {
        if (this.chatChannel != null) {
            this.chatChannel.sendMessage(user + ": " + text);
        }
    }

    public DiscordBot(String serverName, DiscordSender sender) {
        this.serverName = serverName;
        this.sender = sender;
    }

    public void connect(boolean enabled, String token, String chatChannelName, String logChannelName, String commandChannelName) {
        if (enabled && !StringUtils.isNullOrEmpty(token)) {
            try {
                DiscordApi api = new DiscordApiBuilder().setToken(token).setIntents(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT).login().join();
                if (api != null) {
                    if (!StringUtils.isNullOrEmpty(logChannelName)) {
                        this.logChannel = api.getTextChannelsByName(logChannelName).stream().findFirst().orElse(null);
                    } else {
                        DebugType.Discord.debugln("log channel name not configured");
                    }

                    if (!StringUtils.isNullOrEmpty(chatChannelName)) {
                        this.chatChannel = api.getTextChannelsByName(chatChannelName).stream().findFirst().orElse(null);
                    } else {
                        DebugType.Discord.debugln("chat channel name not configured");
                    }

                    if (!StringUtils.isNullOrEmpty(commandChannelName)) {
                        this.commandChannel = api.getTextChannelsByName(commandChannelName).stream().findFirst().orElse(null);
                    } else {
                        DebugType.Discord.debugln("command channel name not configured");
                    }

                    if (this.chatChannel != null || this.commandChannel != null) {
                        api.updateUsername(this.serverName);
                        api.addMessageCreateListener(this::receiveMessage);
                        DebugType.Discord.println("invite-url %s", api.createBotInvite());
                    }
                }
            } catch (Exception var8) {
                DebugType.General.printException(var8, "Can't connect to discord", LogSeverity.Warning);
            }
        }
    }

    private void receiveMessage(MessageCreateEvent messageCreateEvent) {
        TextChannel messageChannel = messageCreateEvent.getChannel();
        if (messageChannel != null) {
            Message message = messageCreateEvent.getMessage();
            if (message != null) {
                if (this.chatChannel != null && this.chatChannel.getId() == messageChannel.getId() && !message.getAuthor().isYourself()) {
                    String content = removeSmilesAndImages(message.getReadableContent());
                    if (!content.isEmpty()) {
                        this.sender.sendMessageFromDiscord(message.getAuthor().getDisplayName(), content);
                    }
                }

                if (this.commandChannel != null && this.commandChannel.getId() == messageChannel.getId() && !message.getAuthor().isYourself()) {
                    String response = GameServer.rcon(message.getReadableContent());
                    if (response != null) {
                        this.commandChannel.sendMessage(response);
                    }
                }
            }
        }
    }

    private static String removeSmilesAndImages(String content) {
        StringBuilder sb = new StringBuilder();
        char[] var2 = content.toCharArray();
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; var4++) {
            Character cur = var2[var4];
            if (!Character.isLowSurrogate(cur) && !Character.isHighSurrogate(cur)) {
                sb.append(cur);
            }
        }

        return sb.toString();
    }
}
