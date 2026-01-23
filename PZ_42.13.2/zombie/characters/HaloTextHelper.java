// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class HaloTextHelper {
    public static final HaloTextHelper.ColorRGB COLOR_WHITE = new HaloTextHelper.ColorRGB(255, 255, 255);
    public static final HaloTextHelper.ColorRGB COLOR_GREEN = new HaloTextHelper.ColorRGB(137, 232, 148);
    public static final HaloTextHelper.ColorRGB COLOR_RED = new HaloTextHelper.ColorRGB(255, 105, 97);
    private static final String[] queuedLines = new String[4];
    private static final String[] currentLines = new String[4];
    private static boolean ignoreOverheadCheckOnce;

    public static HaloTextHelper.ColorRGB getColorWhite() {
        return COLOR_WHITE;
    }

    public static HaloTextHelper.ColorRGB getColorGreen() {
        return COLOR_GREEN;
    }

    public static HaloTextHelper.ColorRGB getColorRed() {
        return COLOR_RED;
    }

    public static void forceNextAddText() {
        ignoreOverheadCheckOnce = true;
    }

    public static void addTextWithArrow(IsoPlayer player, String text, String separator, boolean arrowIsUp, HaloTextHelper.ColorRGB color) {
        addTextWithArrow(player, text, separator, arrowIsUp, color.r, color.g, color.b, color.r, color.g, color.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, String separator, boolean arrowIsUp, int r, int g, int b) {
        addTextWithArrow(player, text, separator, arrowIsUp, r, g, b, r, g, b);
    }

    public static void addTextWithArrow(
        IsoPlayer player, String text, String separator, boolean arrowIsUp, HaloTextHelper.ColorRGB color, HaloTextHelper.ColorRGB arrowColor
    ) {
        addTextWithArrow(player, text, separator, arrowIsUp, color.r, color.g, color.b, arrowColor.r, arrowColor.g, arrowColor.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, String separator, boolean arrowIsUp, int r, int g, int b, int aR, int aG, int aB) {
        addText(
            player,
            "[col="
                + r
                + ","
                + g
                + ","
                + b
                + "]"
                + text
                + "[/] [img=media/ui/"
                + (arrowIsUp ? "ArrowUp.png" : "ArrowDown.png")
                + ","
                + aR
                + ","
                + aG
                + ","
                + aB
                + "]",
            separator
        );
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, HaloTextHelper.ColorRGB color) {
        addTextWithArrow(player, text, "[col=175,175,175], [/]", arrowIsUp, color.r, color.g, color.b, color.r, color.g, color.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, int r, int g, int b) {
        addTextWithArrow(player, text, "[col=175,175,175], [/]", arrowIsUp, r, g, b, r, g, b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, HaloTextHelper.ColorRGB color, HaloTextHelper.ColorRGB arrowColor) {
        addTextWithArrow(player, text, "[col=175,175,175], [/]", arrowIsUp, color.r, color.g, color.b, arrowColor.r, arrowColor.g, arrowColor.b);
    }

    public static void addTextWithArrow(IsoPlayer player, String text, boolean arrowIsUp, int r, int g, int b, int aR, int aG, int aB) {
        addText(
            player,
            "[col="
                + r
                + ","
                + g
                + ","
                + b
                + "]"
                + text
                + "[/] [img=media/ui/"
                + (arrowIsUp ? "ArrowUp.png" : "ArrowDown.png")
                + ","
                + aR
                + ","
                + aG
                + ","
                + aB
                + "]",
            "[col=175,175,175], [/]"
        );
    }

    public static void addText(IsoPlayer player, String text, String seperator, HaloTextHelper.ColorRGB color) {
        addText(player, text, seperator, color.r, color.g, color.b);
    }

    public static void addText(IsoPlayer player, String text, String separator, int r, int g, int b) {
        addText(player, "[col=" + r + "," + g + "," + b + "]" + text + "[/]", separator);
    }

    public static HaloTextHelper.ColorRGB getGoodColor() {
        int r = (int)Core.getInstance().getGoodHighlitedColor().getR() * 255;
        int g = (int)Core.getInstance().getGoodHighlitedColor().getG() * 255;
        int b = (int)Core.getInstance().getGoodHighlitedColor().getB() * 255;
        return new HaloTextHelper.ColorRGB(r, g, b);
    }

    public static HaloTextHelper.ColorRGB getBadColor() {
        int r = (int)Core.getInstance().getBadHighlitedColor().getR() * 255;
        int g = (int)Core.getInstance().getBadHighlitedColor().getG() * 255;
        int b = (int)Core.getInstance().getBadHighlitedColor().getB() * 255;
        return new HaloTextHelper.ColorRGB(r, g, b);
    }

    public static void addGoodText(IsoPlayer player, String text) {
        addGoodText(player, text, "[col=175,175,175], [/]");
    }

    public static void addGoodText(IsoPlayer player, String text, String separator) {
        addText(player, text, separator, getGoodColor());
    }

    public static void addBadText(IsoPlayer player, String text) {
        addBadText(player, text, "[col=175,175,175], [/]");
    }

    public static void addBadText(IsoPlayer player, String text, String separator) {
        addText(player, text, separator, getBadColor());
    }

    public static void addText(IsoPlayer player, String text) {
        addText(player, text, "[col=175,175,175], [/]");
    }

    public static void addText(IsoPlayer player, String text, String separator) {
        int num = player.getPlayerNum();
        if (!overheadContains(num, text)) {
            String haloStr = queuedLines[num];
            if (haloStr == null) {
                haloStr = text;
            } else {
                if (haloStr.contains(text)) {
                    return;
                }

                haloStr = haloStr + separator + text;
            }

            queuedLines[num] = haloStr;
            if (GameServer.server) {
                INetworkPacket.send(player, PacketTypes.PacketType.HaloText, player, text, separator);
            }
        }
    }

    private static boolean overheadContains(int num, String text) {
        if (ignoreOverheadCheckOnce) {
            ignoreOverheadCheckOnce = false;
            return false;
        } else {
            return currentLines[num] != null && currentLines[num].contains(text);
        }
    }

    public static void update() {
        for (int i = 0; i < 4; i++) {
            IsoPlayer plr = IsoPlayer.players[i];
            if (plr != null) {
                if (currentLines[i] != null && plr.getHaloTimerCount() <= 0.2F * GameTime.getInstance().getMultiplier()) {
                    currentLines[i] = null;
                }

                if (queuedLines[i] != null && plr.getHaloTimerCount() <= 0.2F * GameTime.getInstance().getMultiplier()) {
                    plr.setHaloNote(queuedLines[i]);
                    currentLines[i] = queuedLines[i];
                    queuedLines[i] = null;
                }
            } else {
                if (queuedLines[i] != null) {
                    queuedLines[i] = null;
                }

                if (currentLines[i] != null) {
                    currentLines[i] = null;
                }
            }
        }
    }

    @UsedFromLua
    public static class ColorRGB {
        public int r;
        public int g;
        public int b;
        public int a = 255;

        public ColorRGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
