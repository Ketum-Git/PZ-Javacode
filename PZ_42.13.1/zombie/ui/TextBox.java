// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.Stack;

public final class TextBox extends UIElement {
    public boolean resizeParent;
    UIFont font;
    Stack<String> lines = new Stack<>();
    String text;
    public boolean centered;

    public TextBox(UIFont font, int x, int y, int width, String text) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.text = text;
        this.width = width;
        this.Paginate();
    }

    @Override
    public void onresize() {
        this.Paginate();
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            super.render();
            this.Paginate();
            int y = 0;

            for (String text : this.lines) {
                if (this.centered) {
                    TextManager.instance
                        .DrawStringCentre(
                            this.font, this.getAbsoluteX().intValue() + this.getWidth() / 2.0, this.getAbsoluteY().intValue() + y, text, 1.0, 1.0, 1.0, 1.0
                        );
                } else {
                    TextManager.instance.DrawString(this.font, this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue() + y, text, 1.0, 1.0, 1.0, 1.0);
                }

                y += TextManager.instance.MeasureStringY(this.font, this.lines.get(0));
            }

            this.setHeight(y);
        }
    }

    @Override
    public void update() {
        this.Paginate();
        int y = 0;

        for (String text : this.lines) {
            y += TextManager.instance.MeasureStringY(this.font, this.lines.get(0));
        }

        this.setHeight(y);
    }

    private void Paginate() {
        int n = 0;
        this.lines.clear();
        String[] textarr = this.text.split("<br>");

        for (String text : textarr) {
            if (text.isEmpty()) {
                this.lines.add(" ");
            } else {
                while (true) {
                    int m = text.indexOf(" ", n + 1);
                    int z = m;
                    if (m == -1) {
                        z = text.length();
                    }

                    int wid = TextManager.instance.MeasureStringX(this.font, text.substring(0, z));
                    if (wid >= this.getWidth()) {
                        String sub = text.substring(0, n);
                        text = text.substring(n + 1);
                        this.lines.add(sub);
                        m = 0;
                    } else if (m == -1) {
                        this.lines.add(text);
                        break;
                    }

                    n = m;
                    if (text.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    public void SetText(String text) {
        this.text = text;
        this.Paginate();
    }
}
