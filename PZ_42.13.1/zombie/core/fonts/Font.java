// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.fonts;

import zombie.core.Color;

public interface Font {
    /**
     * Draw a string to the screen
     * 
     * @param ERROR The x location at which to draw the string
     * @param ERROR The y location at which to draw the string
     * @param ERROR The text to be displayed
     */
    void drawString(float x, float y, String text);

    /**
     * Draw a string to the screen
     * 
     * @param ERROR The x location at which to draw the string
     * @param ERROR The y location at which to draw the string
     * @param ERROR The text to be displayed
     * @param ERROR The colour to draw with
     */
    void drawString(float x, float y, String text, Color col);

    /**
     * Draw part of a string to the screen. Note that this will
     *  still position the text as though it's part of the bigger string.
     * 
     * @param ERROR The x location at which to draw the string
     * @param ERROR The y location at which to draw the string
     * @param ERROR The text to be displayed
     * @param ERROR The colour to draw with
     * @param ERROR The index of the first character to draw
     * @param ERROR The index of the last character from the string to draw
     */
    void drawString(float x, float y, String text, Color col, int startIndex, int endIndex);

    /**
     * get the height of the given string
     * 
     * @param ERROR The string to obtain the rendered with of
     * @return The width of the given string
     */
    int getHeight(String str);

    /**
     * get the width of the given string
     * 
     * @param ERROR The string to obtain the rendered with of
     * @return The width of the given string
     */
    int getWidth(String str);

    int getWidth(String str, boolean xAdvance);

    int getWidth(String str, int startIndex, int endIndex);

    int getWidth(String str, int startIndex, int endIndex, boolean xAdvance);

    /**
     * get the maximum height of any line drawn by this font
     * @return The maxium height of any line drawn by this font
     */
    int getLineHeight();
}
