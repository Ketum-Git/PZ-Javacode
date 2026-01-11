// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.util.Arrays;
import java.util.HashMap;
import zombie.core.math.PZMath;
import zombie.ui.TextManager;
import zombie.util.StringUtils;
import zombie.worldMap.styles.WorldMapTextStyleLayer;

public final class TextLayout {
    private static final HashMap<String, String> brReplacements = new HashMap<>();
    public String text;
    public String textWithoutFormatting;
    public short numLines;
    public short[] lineStartEnd;
    public WorldMapTextStyleLayer textLayer;
    public short[] lineLength;
    public int maxLineLength;

    public TextLayout set(String text1, WorldMapTextStyleLayer textLayer1) {
        if (this.textLayer == textLayer1 && StringUtils.equals(this.text, text1)) {
            return this;
        } else {
            this.splitLines(text1);
            this.calculateLineLengths(textLayer1);
            return this;
        }
    }

    public TextLayout set(TextLayout other) {
        this.text = other.text;
        this.textWithoutFormatting = other.textWithoutFormatting;
        this.numLines = other.numLines;
        if (other.lineStartEnd != null) {
            if (this.lineStartEnd == null || this.lineStartEnd.length < other.lineStartEnd.length) {
                this.lineStartEnd = new short[other.lineStartEnd.length];
            }

            System.arraycopy(other.lineStartEnd, 0, this.lineStartEnd, 0, this.numLines * 2);
        }

        this.textLayer = other.textLayer;
        if (other.lineLength != null) {
            if (this.lineLength == null || this.lineLength.length < other.lineLength.length) {
                this.lineLength = new short[other.lineLength.length];
            }

            System.arraycopy(other.lineLength, 0, this.lineLength, 0, this.numLines);
        }

        this.maxLineLength = other.maxLineLength;
        return this;
    }

    public TextLayout splitLines(String text1) {
        this.text = text1;
        this.textWithoutFormatting = brReplacements.computeIfAbsent(text1, s -> s.replaceAll("<br>", "\n").replaceAll("<BR>", "\n"));
        this.numLines = 0;
        short lineStart = 0;
        short lineEnd = 0;

        for (int i = 0; i < this.textWithoutFormatting.length(); i++) {
            char ch = this.textWithoutFormatting.charAt(i);
            if (ch == '\n') {
                this.addLine(lineStart, (short)(i - 1));
                lineStart = (short)(i + 1);
                lineEnd = lineStart;
            } else {
                lineEnd = (short)i;
            }
        }

        if (lineStart < this.textWithoutFormatting.length()) {
            this.addLine(lineStart, lineEnd);
        }

        return this;
    }

    public void addLine(short start, short end) {
        this.numLines++;
        if (this.lineStartEnd == null) {
            this.lineStartEnd = new short[6];
        } else if (this.numLines > this.lineStartEnd.length / 2) {
            this.lineStartEnd = Arrays.copyOf(this.lineStartEnd, this.numLines * 2);
        }

        this.lineStartEnd[(this.numLines - 1) * 2] = start;
        this.lineStartEnd[(this.numLines - 1) * 2 + 1] = end;
    }

    public void calculateLineLengths(WorldMapTextStyleLayer textLayer1) {
        this.textLayer = textLayer1;
        this.maxLineLength = 0;
        if (this.lineLength == null) {
            this.lineLength = new short[this.numLines];
        } else if (this.lineLength.length < this.numLines) {
            this.lineLength = new short[this.numLines];
        }

        Arrays.fill(this.lineLength, (short)0);

        for (int i = 0; i < this.numLines; i++) {
            String text1 = this.textWithoutFormatting.substring(this.lineStartEnd[i * 2], this.lineStartEnd[i * 2 + 1] + 1);
            this.lineLength[i] = (short)TextManager.instance.MeasureStringX(this.textLayer.getFont(), text1);
            this.maxLineLength = PZMath.max(this.maxLineLength, this.lineLength[i]);
        }
    }

    public int getMaxLineLength() {
        return this.maxLineLength;
    }

    public int getLineLength(int lineIndex) {
        return this.lineLength[lineIndex];
    }

    public float getLineOffsetX(int lineIndex) {
        return (this.getMaxLineLength() - this.getLineLength(lineIndex)) / 2.0F;
    }

    public int getFirstChar(int lineIndex) {
        return this.lineStartEnd[lineIndex * 2];
    }

    public int getLastChar(int lineIndex) {
        return this.lineStartEnd[lineIndex * 2 + 1];
    }
}
