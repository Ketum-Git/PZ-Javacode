// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

/**
 * The default sprite renderer has various different styles of rendering, which
 *  affect what data it uses from the buffer and what GL state that it sets and
 *  resets before and after rendering.
 */
public interface Style {
    /**
     * Called to set up GL rendering state before actual drawing is done.
     */
    void setupState();

    /**
     * Called to reset GL rendering state after actual drawing is done.
     */
    void resetState();

    /**
     * @return the style's ID, which affects its rendering order
     */
    int getStyleID();

    /**
     * @return the style's alpha operation
     */
    AlphaOp getAlphaOp();

    /**
     * Whether to actually render a sprite when using this Style.
     * @return boolean
     */
    boolean getRenderSprite();

    /**
     * If not rendering a sprite, then we perform a build() to create GeometryData
     *  Later, render() is called, with a vertex offset position and index offset position. Return null if you are going
     *  to handle your own VBOs in setupState().
     * @return the vertex data, or null
     */
    GeometryData build();

    /**
     * If not rendering a sprite, then render stuff. Our geometry was written to a pre-prepared buffer which is pointed to
     *  already.
     */
    void render(int vertexOffset, int indexOffset);
}
