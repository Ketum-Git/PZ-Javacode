// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.SDFShader;
import zombie.network.GameServer;
import zombie.network.ServerGUI;

@UsedFromLua
public final class TextManager {
    public AngelCodeFont font;
    public AngelCodeFont font2;
    public AngelCodeFont font3;
    public AngelCodeFont font4;
    public AngelCodeFont main1;
    public AngelCodeFont main2;
    public AngelCodeFont zombiefontcredits1;
    public AngelCodeFont zombiefontcredits2;
    public AngelCodeFont zombienew1;
    public AngelCodeFont zombienew2;
    public AngelCodeFont zomboidDialogue;
    public AngelCodeFont codetext;
    public AngelCodeFont codeSmall;
    public AngelCodeFont codeMedium;
    public AngelCodeFont codeLarge;
    public AngelCodeFont debugConsole;
    public AngelCodeFont intro;
    public AngelCodeFont handwritten;
    public final AngelCodeFont[] normal = new AngelCodeFont[14];
    public AngelCodeFont zombienew3;
    public final AngelCodeFont[] enumToFont = new AngelCodeFont[UIFont.values().length];
    public static SDFShader sdfShader;
    public UIFont currentCodeFont = UIFont.CodeSmall;
    public static final TextManager instance = new TextManager();
    public ArrayList<TextManager.DeferedTextDraw> todoTextList = new ArrayList<>();

    public void DrawString(double x, double y, String str) {
        this.font.drawString((float)x, (float)y, str, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void DrawString(double x, double y, String str, double r, double g, double b, double a) {
        this.font.drawString((float)x, (float)y, str, (float)r, (float)g, (float)b, (float)a);
    }

    public void DrawString(UIFont font, double x, double y, double zoom, String str, double r, double g, double b, double a) {
        AngelCodeFont toUse = this.getFontFromEnum(font);
        toUse.drawString((float)x, (float)y, (float)zoom, str, (float)r, (float)g, (float)b, (float)a);
    }

    public void DrawString(UIFont font, double x, double y, String str, double r, double g, double b, double a) {
        AngelCodeFont toUse = this.getFontFromEnum(font);
        toUse.drawString((float)x, (float)y, str, (float)r, (float)g, (float)b, (float)a);
    }

    public void DrawStringUntrimmed(UIFont font, double x, double y, String str, double r, double g, double b, double a) {
        AngelCodeFont toUse = this.getFontFromEnum(font);
        toUse.drawString((float)x, (float)y, str, (float)r, (float)g, (float)b, (float)a);
    }

    public void DrawStringCentre(double x, double y, String str, double r, double g, double b, double a) {
        x -= this.font.getWidth(str) / 2;
        this.font.drawString((float)x, (float)y, str, (float)r, (float)g, (float)b, (float)a);
    }

    public void DrawStringCentre(UIFont font, double x, double y, String str, double r, double g, double b, double a) {
        AngelCodeFont toUse = this.getFontFromEnum(font);
        x -= toUse.getWidth(str) / 2;
        toUse.drawString((float)x, (float)y, str, (float)r, (float)g, (float)b, (float)a);
    }

    public void DrawStringCentreDefered(UIFont font, double x, double y, String str, double r, double g, double b, double a) {
        this.todoTextList.add(new TextManager.DeferedTextDraw(font, x, y, str, r, g, b, a));
    }

    public void DrawTextFromGameWorld() {
        for (int n = 0; n < this.todoTextList.size(); n++) {
            TextManager.DeferedTextDraw deferedTextDraw = this.todoTextList.get(n);
            this.DrawStringCentre(
                deferedTextDraw.font,
                deferedTextDraw.x,
                deferedTextDraw.y,
                deferedTextDraw.str,
                deferedTextDraw.r,
                deferedTextDraw.g,
                deferedTextDraw.b,
                deferedTextDraw.a
            );
        }

        this.todoTextList.clear();
    }

    public void DrawStringRight(double x, double y, String str, double r, double g, double b, double a) {
        x -= this.font.getWidth(str);
        this.font.drawString((float)x, (float)y, str, (float)r, (float)g, (float)b, (float)a);
    }

    public TextDrawObject GetDrawTextObject(String str, int maxLineWidth, boolean restrictImages) {
        return new TextDrawObject();
    }

    public void DrawTextObject(double x, double y, TextDrawObject td) {
    }

    public void DrawStringBBcode(UIFont font, double x, double y, String str, double r, double g, double b, double a) {
    }

    public AngelCodeFont getNormalFromFontSize(int points) {
        return this.normal[points - 11];
    }

    public AngelCodeFont getFontFromEnum(UIFont font) {
        if (font == null) {
            return this.font;
        } else {
            AngelCodeFont toUse = this.enumToFont[font.ordinal()];
            return toUse == null ? this.font : toUse;
        }
    }

    public int getFontHeight(UIFont fontID) {
        AngelCodeFont font = this.getFontFromEnum(fontID);
        return font.getLineHeight();
    }

    public boolean isSdf(UIFont font) {
        return this.getFontFromEnum(font).isSdf();
    }

    public ArrayList<UIFont> getAllFonts(ArrayList<UIFont> result) {
        result.clear();
        result.addAll(Arrays.asList(UIFont.values()));
        result.sort((o1, o2) -> o1.name().compareToIgnoreCase(o2.name()));
        return result;
    }

    public void DrawStringRight(UIFont font, double x, double y, String str, double r, double g, double b, double a) {
        AngelCodeFont toUse = this.getFontFromEnum(font);
        x -= toUse.getWidth(str);
        toUse.drawString((float)x, (float)y, str, (float)r, (float)g, (float)b, (float)a);
    }

    private String getFontFilePath(String lang, String sizeDir, String fileName) {
        if (sizeDir != null) {
            String path = "media/fonts/" + lang + "/" + sizeDir + "/" + fileName;
            if (ZomboidFileSystem.instance.getString(path) != path) {
                return path;
            }
        }

        String path = "media/fonts/" + lang + "/" + fileName;
        if (ZomboidFileSystem.instance.getString(path) != path) {
            return path;
        } else {
            if (!"EN".equals(lang)) {
                if (sizeDir != null) {
                    path = "media/fonts/EN/" + sizeDir + "/" + fileName;
                    if (ZomboidFileSystem.instance.getString(path) != path) {
                        return path;
                    }
                }

                path = "media/fonts/EN/" + fileName;
                if (ZomboidFileSystem.instance.getString(path) != path) {
                    return path;
                }
            }

            path = "media/fonts/" + fileName;
            return ZomboidFileSystem.instance.getString(path) != path ? path : "media/" + fileName;
        }
    }

    public void Init() throws FileNotFoundException {
        FontsFile fontsFile = new FontsFile();
        HashMap<String, FontsFileFont> fontsFileFontMap = new HashMap<>();
        String lang = Translator.getLanguage().name();
        if (Core.getInstance().getOptionEnableDyslexicFont()) {
            String fontsFilePath = ZomboidFileSystem.instance.getString("media/fonts/" + lang + "/fontsDyslexic.txt");
            fontsFile.read(fontsFilePath, fontsFileFontMap);
        }

        if (fontsFileFontMap.isEmpty()) {
            String fontsFilePath = ZomboidFileSystem.instance.getString("media/fonts/EN/fonts.txt");
            fontsFile.read(fontsFilePath, fontsFileFontMap);
            if (!"EN".equals(lang)) {
                fontsFilePath = ZomboidFileSystem.instance.getString("media/fonts/" + lang + "/fonts.txt");
                fontsFile.read(fontsFilePath, fontsFileFontMap);
            }
        }

        HashMap<String, AngelCodeFont> loaded = new HashMap<>();
        int fontSize = Core.getInstance().getOptionFontSizeReal();
        String sizeDir = null;
        if (fontSize == 2) {
            sizeDir = "1x";
        } else if (fontSize == 3) {
            sizeDir = "2x";
        } else if (fontSize == 4) {
            sizeDir = "3x";
        } else if (fontSize == 5) {
            sizeDir = "4x";
        }

        for (AngelCodeFont acFont : this.enumToFont) {
            if (acFont != null) {
                acFont.destroy();
            }
        }

        Arrays.fill(this.enumToFont, null);

        for (AngelCodeFont acFontx : this.normal) {
            if (acFontx != null) {
                acFontx.destroy();
            }
        }

        Arrays.fill(this.normal, null);

        for (UIFont uiFont : UIFont.values()) {
            FontsFileFont fontsFileFont = fontsFileFontMap.get(uiFont.name());
            if (fontsFileFont != null) {
                String fnt = this.getFontFilePath(lang, sizeDir, fontsFileFont.fnt);
                String img = null;
                if (fontsFileFont.img != null) {
                    img = this.getFontFilePath(lang, sizeDir, fontsFileFont.img);
                }

                String key = fnt + "|" + img;
                if (loaded.get(key) != null) {
                    this.enumToFont[uiFont.ordinal()] = loaded.get(key);
                } else {
                    AngelCodeFont angelCodeFont = new AngelCodeFont(fnt, img);
                    angelCodeFont.setSdf("sdf".equalsIgnoreCase(fontsFileFont.type));
                    this.enumToFont[uiFont.ordinal()] = angelCodeFont;
                    loaded.put(key, angelCodeFont);
                }
            }
        }

        if (this.enumToFont[UIFont.DebugConsole.ordinal()] == null) {
            this.enumToFont[UIFont.DebugConsole.ordinal()] = this.enumToFont[UIFont.Small.ordinal()];
        }

        for (int n = 0; n < this.normal.length; n++) {
            this.normal[n] = new AngelCodeFont("media/fonts/zomboidNormal" + (n + 11) + ".fnt", "media/fonts/zomboidNormal" + (n + 11) + "_0");
        }

        this.font = this.enumToFont[UIFont.Small.ordinal()];
        this.font2 = this.enumToFont[UIFont.Medium.ordinal()];
        this.font3 = this.enumToFont[UIFont.Large.ordinal()];
        this.font4 = this.enumToFont[UIFont.Massive.ordinal()];
        this.main1 = this.enumToFont[UIFont.MainMenu1.ordinal()];
        this.main2 = this.enumToFont[UIFont.MainMenu2.ordinal()];
        this.zombiefontcredits1 = this.enumToFont[UIFont.Cred1.ordinal()];
        this.zombiefontcredits2 = this.enumToFont[UIFont.Cred2.ordinal()];
        this.zombienew1 = this.enumToFont[UIFont.NewSmall.ordinal()];
        this.zombienew2 = this.enumToFont[UIFont.NewMedium.ordinal()];
        this.zombienew3 = this.enumToFont[UIFont.NewLarge.ordinal()];
        this.codetext = this.enumToFont[UIFont.Code.ordinal()];
        this.codeSmall = this.enumToFont[UIFont.CodeSmall.ordinal()];
        this.codeMedium = this.enumToFont[UIFont.CodeMedium.ordinal()];
        this.codeLarge = this.enumToFont[UIFont.CodeLarge.ordinal()];
        this.enumToFont[UIFont.MediumNew.ordinal()] = null;
        this.enumToFont[UIFont.AutoNormSmall.ordinal()] = null;
        this.enumToFont[UIFont.AutoNormMedium.ordinal()] = null;
        this.enumToFont[UIFont.AutoNormLarge.ordinal()] = null;
        this.zomboidDialogue = this.enumToFont[UIFont.Dialogue.ordinal()];
        this.intro = this.enumToFont[UIFont.Intro.ordinal()];
        this.handwritten = this.enumToFont[UIFont.Handwritten.ordinal()];
        this.debugConsole = this.enumToFont[UIFont.DebugConsole.ordinal()];
        RenderThread.invokeOnRenderContext(() -> sdfShader = new SDFShader("sdf"));
    }

    public boolean isUsingNonEnglishFonts() {
        String lang = Translator.getLanguage().name();
        if ("EN".equalsIgnoreCase(lang)) {
            return false;
        } else {
            String path = "media/fonts/" + lang + "/fonts.txt";
            return ZomboidFileSystem.instance.getString(path) != path;
        }
    }

    public int MeasureStringX(UIFont font, String str) {
        if (GameServer.server && !ServerGUI.isCreated()) {
            return 0;
        } else if (str == null) {
            return 0;
        } else {
            AngelCodeFont toUse = this.getFontFromEnum(font);
            return toUse.getWidth(str);
        }
    }

    public int CentreStringYOffset(UIFont font, String str) {
        return this.MeasureStringYOffset(font, str) - (this.MeasureStringY(font, str) - this.MeasureStringYReal(font, str)) / 2;
    }

    public int MeasureStringY(UIFont font, String str) {
        return this.MeasureStringY(font, str, false, false);
    }

    public int MeasureStringYReal(UIFont font, String str) {
        return this.MeasureStringY(font, str, true, false);
    }

    public int MeasureStringYOffset(UIFont font, String str) {
        return this.MeasureStringY(font, str, false, true);
    }

    public int MeasureStringY(UIFont font, String str, boolean returnActualHeight, boolean returnOffset) {
        if (font == null || str == null) {
            return 0;
        } else if (GameServer.server && !ServerGUI.isCreated()) {
            return 0;
        } else {
            AngelCodeFont toUse = this.getFontFromEnum(font);
            return toUse.getHeight(str, returnActualHeight, returnOffset);
        }
    }

    public int MeasureFont(UIFont font) {
        if (font == UIFont.Small) {
            return 10;
        } else if (font == UIFont.Dialogue) {
            return 20;
        } else if (font == UIFont.Medium) {
            return 20;
        } else if (font == UIFont.Large) {
            return 24;
        } else if (font == UIFont.Massive) {
            return 30;
        } else if (font == UIFont.MainMenu1) {
            return 30;
        } else {
            return font == UIFont.MainMenu2 ? 30 : this.getFontFromEnum(font).getLineHeight();
        }
    }

    public String WrapText(UIFont font, String str, int maxWidth) {
        return this.WrapText(font, str, maxWidth, -1, "");
    }

    public String WrapText(UIFont font, String str, int maxWidth, int maxLines, String maxLinesSuffix) {
        ArrayList<String> outLines = new ArrayList<>();
        String[] inLines = str.split("\\r?\\n");
        int spaceWidth = this.MeasureStringX(font, " ");
        int minCharsPerLine = 5;

        for (int lineIdx = 0; lineIdx < inLines.length; lineIdx++) {
            int lineWidth = this.MeasureStringX(font, inLines[lineIdx]);
            if (lineWidth <= maxWidth) {
                outLines.add(inLines[lineIdx]);
            } else {
                String[] words = inLines[lineIdx].split(" ");
                ArrayList<String> newLine = new ArrayList<>();
                int currentLineLength = 0;

                for (int wordIdx = 0; wordIdx < words.length; wordIdx++) {
                    int newLineLength = currentLineLength + this.MeasureStringX(font, words[wordIdx]);
                    if (newLineLength <= maxWidth) {
                        newLine.add(words[wordIdx]);
                        currentLineLength += this.MeasureStringX(font, words[wordIdx]) + spaceWidth;
                    } else if (!newLine.isEmpty()) {
                        String newLineString = String.join(" ", newLine);
                        outLines.add(newLineString);
                        newLine.clear();
                        newLine.add(words[wordIdx]);
                        currentLineLength = this.MeasureStringX(font, words[wordIdx]) + spaceWidth;
                    } else {
                        int charsPerLineApprox = (int)Math.floor((double)maxWidth / newLineLength * words[wordIdx].length()) - 1;
                        charsPerLineApprox = Math.max(charsPerLineApprox, 5);
                        int lines = (int)Math.ceil((double)words[wordIdx].length() / charsPerLineApprox);

                        for (int line = 0; line < lines - 1; line++) {
                            outLines.add(words[wordIdx].substring(line * charsPerLineApprox, (line + 1) * charsPerLineApprox) + "-");
                        }

                        String finalPart = words[wordIdx].substring((lines - 1) * charsPerLineApprox);
                        newLine.add(finalPart);
                        currentLineLength = this.MeasureStringX(font, finalPart) + spaceWidth;
                    }

                    if (wordIdx == words.length - 1) {
                        String newLineString = String.join(" ", newLine);
                        outLines.add(newLineString);
                        currentLineLength = 0;
                        newLine.clear();
                    }
                }
            }
        }

        if (maxLines > 0 && outLines.size() > maxLines) {
            int suffixLength = this.MeasureStringX(font, maxLinesSuffix);
            if (maxWidth - this.MeasureStringX(font, outLines.get(maxLines - 1)) < suffixLength) {
                String finalLine = outLines.get(maxLines - 1);
                int finalIndex = Math.max(finalLine.length() - maxLinesSuffix.length(), 0);
                String newLine = finalLine.substring(0, finalIndex) + maxLinesSuffix;
                outLines.set(maxLines - 1, newLine);
            } else {
                String newLine = outLines.get(maxLines - 1) + maxLinesSuffix;
                outLines.set(maxLines - 1, newLine);
            }

            return String.join("\n", outLines.subList(0, maxLines));
        } else {
            return String.join("\n", outLines);
        }
    }

    public UIFont getCurrentCodeFont() {
        return this.currentCodeFont;
    }

    public static class DeferedTextDraw {
        public double x;
        public double y;
        public UIFont font;
        public String str;
        public double r;
        public double g;
        public double b;
        public double a;

        public DeferedTextDraw(UIFont font, double x, double y, String str, double r, double g, double b, double a) {
            this.font = font;
            this.x = x;
            this.y = y;
            this.str = str;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    public interface StringDrawer {
        void draw(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13);
    }
}
