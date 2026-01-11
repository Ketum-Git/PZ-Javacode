// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.Set;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.server.IEventController;
import zombie.util.StringUtils;

public class DiscordBot implements IEventController {
    private static final String EVENT_CHANNEL_NAME = "event";
    private final DiscordSender sender;
    private final String serverName;
    private TextChannel chatChannel;
    private TextChannel eventChannel;
    private DiscordApi api;

    @Override
    public void process(String event) {
        this.sendEvent(event);
    }

    public void sendEvent(String event) {
        if (this.eventChannel != null) {
            this.eventChannel.sendMessage(event);
            DebugLog.Discord.trace("send event: \"%s\"", event);
        }
    }

    public void sendMessage(String user, String text) {
        if (this.chatChannel != null) {
            String message = user + ": " + text;
            this.chatChannel.sendMessage(message);
            DebugLog.Discord.trace("send message: \"%s\"", message);
        }
    }

    public DiscordBot(String serverName, DiscordSender sender) {
        this.serverName = serverName;
        this.sender = sender;
    }

    public void connect(boolean enabled, String token, String channelName, String channelID) {
        if (enabled) {
            DebugLog.Discord.debugln("enabled");
            if (!StringUtils.isNullOrEmpty(token)) {
                try {
                    this.api = new DiscordApiBuilder().setToken(token).setAllIntents().login().join();
                } catch (Exception var6) {
                    DebugLog.General.printException(var6, "Can't connect to discord", LogSeverity.Warning);
                    this.api = null;
                }

                if (this.api != null) {
                    this.chatChannel = this.initChannel(channelID, channelName);
                    if (this.chatChannel != null) {
                        DebugLog.Discord.println("invite-url %s", this.api.createBotInvite());
                    }

                    this.eventChannel = this.initChannel(null, "event");
                }
            } else {
                DebugLog.Discord.warn("token not configured");
            }
        } else {
            DebugLog.Discord.debugln("disabled");
        }
    }

    private TextChannel initChannel(String channelID, String channelName) {
        TextChannel channel = this.getChannel(channelID, channelName);
        if (channel != null) {
            DebugLog.Discord.noise("Channel \"%s\" creation succeed", channelName);
            this.api.updateUsername(this.serverName);
            this.api.addMessageCreateListener(this::receiveMessage);
        } else {
            DebugLog.Discord.warn("Channel \"%s\" creation failed");
        }

        return channel;
    }

    private TextChannel getChannel(String channelID, String channelName) {
        TextChannel channel = null;
        if (!StringUtils.isNullOrEmpty(channelID)) {
            channel = this.api.getTextChannelById(channelID).orElse(null);
            if (channel == null) {
                DebugLog.Discord.warn("channel with ID \"%s\" not found. Try to use channel name instead", channelID);
            } else {
                DebugLog.Discord.noise("enabled on channel with ID \"%s\"", channelID);
            }
        } else if (!StringUtils.isNullOrEmpty(channelName)) {
            Set<TextChannel> channels = this.api.getTextChannelsByName(channelName);
            if (channels.isEmpty()) {
                DebugLog.Discord.warn("channel with name \"%s\" not found. Try to use channel ID instead", channelName);
            } else if (channels.size() == 1) {
                DebugLog.Discord.noise("enabled on channel with name \"%s\"", channelName);
                channel = channels.stream().findFirst().orElse(null);
            } else {
                DebugLog.Discord.warn("server has few channels with name \"%s\". Please, use channel ID instead", channelName);
            }
        } else {
            channel = this.api.getTextChannels().stream().findFirst().orElse(null);
            if (channel == null) {
                DebugLog.Discord.warn("channels not found");
            }
        }

        return channel;
    }

    private void receiveMessage(MessageCreateEvent messageCreateEvent) {
        TextChannel messageChannel = messageCreateEvent.getChannel();
        if (this.chatChannel != null && messageChannel != null && this.chatChannel.getId() == messageChannel.getId()) {
            Message message = messageCreateEvent.getMessage();
            if (!message.getAuthor().isYourself()) {
                String content = this.removeSmilesAndImages(message.getReadableContent());
                if (!content.isEmpty()) {
                    this.sender.sendMessageFromDiscord(message.getAuthor().getDisplayName(), content);
                }

                DebugLog.Discord.trace("get message \"%s\" from user \"%s\"", content, message.getAuthor().getDisplayName());
            }
        } else if (this.eventChannel != null && messageChannel != null && this.eventChannel.getId() == messageChannel.getId()) {
            Message message = messageCreateEvent.getMessage();
            if (message != null) {
                String response = GameServer.rcon(message.getReadableContent());
                if (response != null) {
                    this.sendMessage(message.getAuthor().getDisplayName(), response);
                }
            }
        }
    }

    private String removeSmilesAndImages(String content) {
        StringBuilder sb = new StringBuilder();
        char[] var3 = content.toCharArray();
        int var4 = var3.length;

        for (int var5 = 0; var5 < var4; var5++) {
            Character cur = var3[var5];
            if (!Character.isLowSurrogate(cur) && !Character.isHighSurrogate(cur)) {
                sb.append(cur);
            }
        }

        return sb.toString();
    }
}
