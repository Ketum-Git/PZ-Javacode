// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.characters.Talker;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoTelevision;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.scripting.objects.CharacterTrait;
import zombie.ui.TextDrawObject;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclePart;

/**
 * Turbo
 *  shared display of chat lines functionallity for iso objects & players (characters)
 */
public class ChatElement implements Talker {
    protected ChatElement.PlayerLines[] playerLines = new ChatElement.PlayerLines[4];
    protected ChatElementOwner owner;
    protected float historyVal = 1.0F;
    protected boolean historyInRange;
    protected float historyRange = 15.0F;
    protected boolean useEuclidean = true;
    protected boolean hasChatToDisplay;
    protected int maxChatLines = -1;
    protected int maxCharsPerLine = -1;
    protected String sayLine;
    protected String sayLineTag;
    protected TextDrawObject sayLineObject;
    protected boolean speaking;
    protected boolean speakingNpc;
    protected String talkerType = "unknown";
    public static boolean doBackDrop = true;
    public static NineGridTexture backdropTexture;
    private final int bufferX = 0;
    private final int bufferY = 0;
    private static final ChatElement.PlayerLinesList[] renderBatch = new ChatElement.PlayerLinesList[4];
    private static final HashSet<String> noLogText = new HashSet<>();

    public ChatElement(ChatElementOwner chatowner, int numberoflines, String talkertype) {
        this.owner = chatowner;
        this.setMaxChatLines(numberoflines);
        this.setMaxCharsPerLine(75);
        this.talkerType = talkertype != null ? talkertype : this.talkerType;
        if (backdropTexture == null) {
            backdropTexture = new NineGridTexture("NineGridBlack", 5);
        }
    }

    public void setMaxChatLines(int num) {
        num = num < 1 ? 1 : (num > 10 ? 10 : num);
        if (num != this.maxChatLines) {
            this.maxChatLines = num;

            for (int i = 0; i < this.playerLines.length; i++) {
                this.playerLines[i] = new ChatElement.PlayerLines(this.maxChatLines);
            }
        }
    }

    public int getMaxChatLines() {
        return this.maxChatLines;
    }

    public void setMaxCharsPerLine(int maxChars) {
        for (int i = 0; i < this.playerLines.length; i++) {
            this.playerLines[i].setMaxCharsPerLine(maxChars);
        }

        this.maxCharsPerLine = maxChars;
    }

    @Override
    public boolean IsSpeaking() {
        return this.speaking;
    }

    public boolean IsSpeakingNPC() {
        return this.speakingNpc;
    }

    @Override
    public String getTalkerType() {
        return this.talkerType;
    }

    public void setTalkerType(String type) {
        this.talkerType = type == null ? "" : type;
    }

    @Override
    public String getSayLine() {
        return this.sayLine;
    }

    public String getSayLineTag() {
        return this.speaking && this.sayLineTag != null ? this.sayLineTag : "";
    }

    public void setHistoryRange(float range) {
        this.historyRange = range;
    }

    public void setUseEuclidean(boolean b) {
        this.useEuclidean = b;
    }

    public boolean getHasChatToDisplay() {
        return this.hasChatToDisplay;
    }

    protected float getDistance(IsoPlayer player) {
        if (player == null) {
            return -1.0F;
        } else {
            return this.useEuclidean
                ? (float)Math.sqrt(Math.pow(this.owner.getX() - player.getX(), 2.0) + Math.pow(this.owner.getY() - player.getY(), 2.0))
                : Math.abs(this.owner.getX() - player.getX()) + Math.abs(this.owner.getY() - player.getY());
        }
    }

    protected boolean playerWithinBounds(IsoPlayer player, float dist) {
        return player == null
            ? false
            : player.getX() > this.owner.getX() - dist
                && player.getX() < this.owner.getX() + dist
                && player.getY() > this.owner.getY() - dist
                && player.getY() < this.owner.getY() + dist;
    }

    public void SayDebug(int n, String text) {
        if (!GameServer.server && n >= 0 && n < this.maxChatLines) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null) {
                    ChatElement.PlayerLines pLines = this.playerLines[i];
                    if (n < pLines.chatLines.length) {
                        if (pLines.chatLines[n].getOriginal() != null && pLines.chatLines[n].getOriginal().equals(text)) {
                            pLines.chatLines[n].setInternalTickClock(pLines.lineDisplayTime);
                        } else {
                            pLines.chatLines[n].setSettings(true, true, true, true, true, true);
                            pLines.chatLines[n].setInternalTickClock(pLines.lineDisplayTime);
                            pLines.chatLines[n].setCustomTag("default");
                            pLines.chatLines[n].setDefaultColors(1.0F, 1.0F, 1.0F, 1.0F);
                            pLines.chatLines[n].ReadString(UIFont.Medium, text, this.maxCharsPerLine);
                        }
                    }
                }
            }

            this.sayLine = text;
            this.sayLineTag = "default";
            this.hasChatToDisplay = true;
        }
    }

    @Override
    public void Say(String line) {
        this.addChatLine(line, 1.0F, 1.0F, 1.0F, UIFont.Dialogue, 25.0F, "default", false, false, false, false, false, true);
    }

    public void addChatLine(String msg, float r, float g, float b, float baseRange) {
        this.addChatLine(msg, r, g, b, UIFont.Dialogue, baseRange, "default", false, false, false, false, false, true);
    }

    public void addChatLine(String msg, float r, float g, float b) {
        this.addChatLine(msg, r, g, b, UIFont.Dialogue, 25.0F, "default", false, false, false, false, false, true);
    }

    public void addChatLine(
        String msg,
        float r,
        float g,
        float b,
        UIFont font,
        float baseRange,
        String customTag,
        boolean bbcode,
        boolean img,
        boolean icons,
        boolean colors,
        boolean fonts,
        boolean equalizeHeights
    ) {
        if (!GameServer.server) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null
                    && (
                        !player.hasTrait(CharacterTrait.DEAF)
                            || !(
                                this.owner instanceof IsoTelevision isoTelevision
                                    ? !isoTelevision.isFacing(player)
                                    : this.owner instanceof IsoRadio || this.owner instanceof VehiclePart
                            )
                    )) {
                    float scramble = this.getScrambleValue(player, baseRange);
                    if (scramble < 1.0F) {
                        ChatElement.PlayerLines pLines = this.playerLines[i];
                        TextDrawObject lineObj = pLines.getNewLineObject();
                        if (lineObj != null) {
                            lineObj.setSettings(bbcode, img, icons, colors, fonts, equalizeHeights);
                            lineObj.setInternalTickClock(pLines.lineDisplayTime);
                            lineObj.setCustomTag(customTag);
                            String msgadd;
                            if (scramble > 0.0F) {
                                msgadd = ZomboidRadio.getInstance().scrambleString(msg, (int)(100.0F * scramble), true, "...");
                                lineObj.setDefaultColors(0.5F, 0.5F, 0.5F, 1.0F);
                            } else {
                                msgadd = msg;
                                lineObj.setDefaultColors(r, g, b, 1.0F);
                            }

                            lineObj.ReadString(font, msgadd, this.maxCharsPerLine);
                            this.sayLine = msg;
                            this.sayLineTag = customTag;
                            this.hasChatToDisplay = true;
                        }
                    }
                }
            }
        }
    }

    protected float getScrambleValue(IsoPlayer player, float baseRange) {
        if (this.owner == player) {
            return 0.0F;
        } else {
            float mod = 1.0F;
            boolean forceScramble = false;
            boolean forceDisplay = false;
            if (this.owner.getSquare() != null && player.getSquare() != null) {
                if (player.getBuilding() != null
                    && this.owner.getSquare().getBuilding() != null
                    && player.getBuilding() == this.owner.getSquare().getBuilding()) {
                    if (player.getSquare().getRoom() == this.owner.getSquare().getRoom()) {
                        mod = (float)(mod * 2.0);
                        forceDisplay = true;
                    } else if (Math.abs(player.getZ() - this.owner.getZ()) < 1.0F) {
                        mod = (float)(mod * 2.0);
                    }
                } else if (player.getBuilding() != null || this.owner.getSquare().getBuilding() != null) {
                    mod = (float)(mod * 0.5);
                    forceScramble = true;
                }

                if (Math.abs(player.getZ() - this.owner.getZ()) >= 1.0F) {
                    mod = (float)(mod - mod * (Math.abs(player.getZ() - this.owner.getZ()) * 0.25));
                    forceScramble = true;
                }
            }

            float range = baseRange * mod;
            float scramble = 1.0F;
            if (mod > 0.0F && this.playerWithinBounds(player, range)) {
                float dist = this.getDistance(player);
                if (dist >= 0.0F && dist < range) {
                    float fullRange = range * 0.6F;
                    if (!forceDisplay && (forceScramble || !(dist < fullRange))) {
                        if (range - fullRange != 0.0F) {
                            scramble = (dist - fullRange) / (range - fullRange);
                            if (scramble < 0.2F) {
                                scramble = 0.2F;
                            }
                        }
                    } else {
                        scramble = 0.0F;
                    }
                }
            }

            return scramble;
        }
    }

    protected void updateChatLines() {
        this.speaking = false;
        this.speakingNpc = false;
        boolean foundChat = false;
        if (this.hasChatToDisplay) {
            this.hasChatToDisplay = false;

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                float delta = 1.25F * GameTime.getInstance().getMultiplier();
                int lineDisplayTime = this.playerLines[i].lineDisplayTime;

                for (TextDrawObject line : this.playerLines[i].chatLines) {
                    float disptime = line.updateInternalTickClock(delta);
                    if (!(disptime <= 0.0F)) {
                        this.hasChatToDisplay = true;
                        if (!foundChat && !line.getCustomTag().equals("radio")) {
                            float alpha = disptime / (lineDisplayTime / 2.0F);
                            if (alpha >= 1.0F) {
                                this.speaking = true;
                            }

                            if (alpha >= 0.0F) {
                                this.speakingNpc = true;
                            }

                            foundChat = true;
                        }

                        delta *= 1.2F;
                    }
                }
            }
        }

        if (!this.speaking) {
            this.sayLine = null;
            this.sayLineTag = null;
        }
    }

    protected void updateHistory() {
        if (this.hasChatToDisplay) {
            this.historyInRange = false;
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null) {
                if (player == this.owner) {
                    this.historyVal = 1.0F;
                } else {
                    if (this.playerWithinBounds(player, this.historyRange)) {
                        this.historyInRange = true;
                    } else {
                        this.historyInRange = false;
                    }

                    if (this.historyInRange && this.historyVal != 1.0F) {
                        this.historyVal += 0.04F;
                        if (this.historyVal > 1.0F) {
                            this.historyVal = 1.0F;
                        }
                    }

                    if (!this.historyInRange && this.historyVal != 0.0F) {
                        this.historyVal -= 0.04F;
                        if (this.historyVal < 0.0F) {
                            this.historyVal = 0.0F;
                        }
                    }
                }
            }
        } else if (this.historyVal != 0.0F) {
            this.historyVal = 0.0F;
        }
    }

    public void update() {
        if (!GameServer.server) {
            this.updateChatLines();
            this.updateHistory();
        }
    }

    public void renderBatched(int playerNum, int x, int y) {
        this.renderBatched(playerNum, x, y, false);
    }

    public void renderBatched(int playerNum, int x, int y, boolean ignoreRadioLines) {
        if (playerNum < this.playerLines.length && this.hasChatToDisplay && !GameServer.server) {
            this.playerLines[playerNum].renderX = x;
            this.playerLines[playerNum].renderY = y;
            this.playerLines[playerNum].ignoreRadioLines = ignoreRadioLines;
            if (renderBatch[playerNum] == null) {
                renderBatch[playerNum] = new ChatElement.PlayerLinesList();
            }

            renderBatch[playerNum].add(this.playerLines[playerNum]);
        }
    }

    public void clear(int playerIndex) {
        this.playerLines[playerIndex].clear();
    }

    public static void RenderBatch(int playerNum) {
        if (renderBatch[playerNum] != null && !renderBatch[playerNum].isEmpty()) {
            for (int i = 0; i < renderBatch[playerNum].size(); i++) {
                ChatElement.PlayerLines element = renderBatch[playerNum].get(i);
                element.render();
            }

            renderBatch[playerNum].clear();
        }
    }

    public static void NoRender(int playerNum) {
        if (renderBatch[playerNum] != null) {
            renderBatch[playerNum].clear();
        }
    }

    public static void addNoLogText(String text) {
        if (text != null && !text.isEmpty()) {
            noLogText.add(text);
        }
    }

    class PlayerLines {
        protected int lineDisplayTime;
        protected int renderX;
        protected int renderY;
        protected boolean ignoreRadioLines;
        protected TextDrawObject[] chatLines;

        public PlayerLines(final int numLines) {
            Objects.requireNonNull(ChatElement.this);
            super();
            this.lineDisplayTime = 314;
            this.chatLines = new TextDrawObject[numLines];

            for (int i = 0; i < this.chatLines.length; i++) {
                this.chatLines[i] = new TextDrawObject(0, 0, 0, true, true, true, true, true, true);
                this.chatLines[i].setDefaultFont(UIFont.Medium);
            }
        }

        public void setMaxCharsPerLine(int maxChars) {
            for (int i = 0; i < this.chatLines.length; i++) {
                this.chatLines[i].setMaxCharsPerLine(maxChars);
            }
        }

        public TextDrawObject getNewLineObject() {
            if (this.chatLines != null && this.chatLines.length > 0) {
                TextDrawObject last = this.chatLines[this.chatLines.length - 1];
                last.Clear();

                for (int i = this.chatLines.length - 1; i > 0; i--) {
                    this.chatLines[i] = this.chatLines[i - 1];
                }

                this.chatLines[0] = last;
                return this.chatLines[0];
            } else {
                return null;
            }
        }

        public void render() {
            if (!GameServer.server) {
                if (ChatElement.this.hasChatToDisplay) {
                    int index = 0;

                    for (TextDrawObject line : this.chatLines) {
                        if (line.getEnabled()) {
                            if (line.getWidth() > 0 && line.getHeight() > 0) {
                                float disptime = line.getInternalClock();
                                if (!(disptime <= 0.0F) && (!line.getCustomTag().equals("radio") || !this.ignoreRadioLines)) {
                                    float alpha = disptime / (this.lineDisplayTime / 4.0F);
                                    if (alpha > 1.0F) {
                                        alpha = 1.0F;
                                    }

                                    this.renderY = this.renderY - (line.getHeight() + 1);
                                    boolean outlines = line.getDefaultFontEnum() != UIFont.Dialogue;
                                    if (ChatElement.doBackDrop && ChatElement.backdropTexture != null) {
                                        IndieGL.glBlendFunc(770, 771);
                                        ChatElement.backdropTexture
                                            .renderInnerBased(
                                                this.renderX - line.getWidth() / 2, this.renderY, line.getWidth(), line.getHeight(), 0.0F, 0.0F, 0.0F, 0.4F
                                            );
                                    }

                                    if (index == 0) {
                                        line.Draw(this.renderX, this.renderY, outlines, alpha);
                                    } else if (ChatElement.this.historyVal > 0.0F) {
                                        line.Draw(this.renderX, this.renderY, outlines, alpha * ChatElement.this.historyVal);
                                    }

                                    index++;
                                }
                            } else {
                                index++;
                            }
                        }
                    }
                }
            }
        }

        void clear() {
            if (ChatElement.this.hasChatToDisplay) {
                ChatElement.this.hasChatToDisplay = false;

                for (int i = 0; i < this.chatLines.length; i++) {
                    if (!(this.chatLines[i].getInternalClock() <= 0.0F)) {
                        this.chatLines[i].Clear();
                        this.chatLines[i].updateInternalTickClock(this.chatLines[i].getInternalClock());
                    }
                }

                ChatElement.this.historyInRange = false;
                ChatElement.this.historyVal = 0.0F;
            }
        }
    }

    class PlayerLinesList extends ArrayList<ChatElement.PlayerLines> {
        PlayerLinesList() {
            Objects.requireNonNull(ChatElement.this);
            super();
        }
    }
}
