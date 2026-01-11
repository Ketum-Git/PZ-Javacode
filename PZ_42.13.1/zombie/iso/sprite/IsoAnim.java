// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.core.textures.Texture;
import zombie.network.GameServer;
import zombie.network.ServerGUI;

public final class IsoAnim {
    public static final HashMap<String, IsoAnim> GlobalAnimMap = new HashMap<>();
    public short finishUnloopedOnFrame;
    public short frameDelay;
    public short lastFrame;
    public final ArrayList<IsoDirectionFrame> frames = new ArrayList<>(8);
    public String name;
    boolean looped = true;
    public int id;
    private static final ThreadLocal<StringBuilder> tlsStrBuf = new ThreadLocal<StringBuilder>() {
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };
    public IsoDirectionFrame[] framesArray = new IsoDirectionFrame[0];

    public static void DisposeAll() {
        GlobalAnimMap.clear();
    }

    public IsoAnim() {
        int dbg = 1;
    }

    void LoadExtraFrame(String ObjectName, String AnimName, int i) {
        this.name = AnimName;
        String pre = ObjectName + "_";
        String post = "_" + AnimName + "_";
        Integer a = i;
        IsoDirectionFrame frame = new IsoDirectionFrame(
            Texture.getSharedTexture(pre + "8" + post + a.toString() + ".png"),
            Texture.getSharedTexture(pre + "9" + post + a.toString() + ".png"),
            Texture.getSharedTexture(pre + "6" + post + a.toString() + ".png"),
            Texture.getSharedTexture(pre + "3" + post + a.toString() + ".png"),
            Texture.getSharedTexture(pre + "2" + post + a.toString() + ".png")
        );
        this.frames.add(frame);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesReverseAltName(String ObjectName, String AnimName, String AltName, int nFrames) {
        this.name = AltName;
        StringBuilder strBuf = tlsStrBuf.get();
        strBuf.setLength(0);
        strBuf.append(ObjectName);
        strBuf.append("_%_");
        strBuf.append(AnimName);
        strBuf.append("_^");
        int framei = strBuf.lastIndexOf("^");
        int diri = strBuf.indexOf("_%_") + 1;
        strBuf.setCharAt(diri, '9');
        strBuf.setCharAt(framei, '0');
        if (GameServer.server && !ServerGUI.isCreated()) {
            for (int n = 0; n < nFrames; n++) {
                this.frames.add(new IsoDirectionFrame(null));
            }

            this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
            this.framesArray = this.frames.toArray(this.framesArray);
        }

        Texture tex = Texture.getSharedTexture(strBuf.toString());
        if (tex != null) {
            for (int n = 0; n < nFrames; n++) {
                if (n == 10) {
                    strBuf.setLength(0);
                    strBuf.append(ObjectName);
                    strBuf.append("_1_");
                    strBuf.append(AnimName);
                    strBuf.append("_10");
                }

                Integer a = n;
                IsoDirectionFrame frame = null;
                String str = a.toString();
                if (tex == null) {
                    strBuf.setCharAt(diri, '8');

                    try {
                        strBuf.setCharAt(framei, a.toString().charAt(0));
                    } catch (Exception var21) {
                        this.LoadFramesReverseAltName(ObjectName, AnimName, AltName, nFrames);
                    }

                    String stra = strBuf.toString();
                    strBuf.setCharAt(diri, '9');
                    String strb = strBuf.toString();
                    strBuf.setCharAt(diri, '6');
                    String strc = strBuf.toString();
                    strBuf.setCharAt(diri, '3');
                    String strd = strBuf.toString();
                    strBuf.setCharAt(diri, '2');
                    String stre = strBuf.toString();
                    frame = new IsoDirectionFrame(
                        Texture.getSharedTexture(stra),
                        Texture.getSharedTexture(strb),
                        Texture.getSharedTexture(strc),
                        Texture.getSharedTexture(strd),
                        Texture.getSharedTexture(stre)
                    );
                } else {
                    strBuf.setCharAt(diri, '9');

                    for (int l = 0; l < str.length(); l++) {
                        strBuf.setCharAt(framei + l, str.charAt(l));
                    }

                    String stra = strBuf.toString();
                    strBuf.setCharAt(diri, '6');
                    String strb = strBuf.toString();
                    strBuf.setCharAt(diri, '3');
                    String strc = strBuf.toString();
                    strBuf.setCharAt(diri, '2');
                    String strd = strBuf.toString();
                    strBuf.setCharAt(diri, '1');
                    String stre = strBuf.toString();
                    strBuf.setCharAt(diri, '4');
                    String strf = strBuf.toString();
                    strBuf.setCharAt(diri, '7');
                    String strg = strBuf.toString();
                    strBuf.setCharAt(diri, '8');
                    String strh = strBuf.toString();
                    frame = new IsoDirectionFrame(
                        Texture.getSharedTexture(stra),
                        Texture.getSharedTexture(strb),
                        Texture.getSharedTexture(strc),
                        Texture.getSharedTexture(strd),
                        Texture.getSharedTexture(stre),
                        Texture.getSharedTexture(strf),
                        Texture.getSharedTexture(strg),
                        Texture.getSharedTexture(strh)
                    );
                }

                this.frames.add(0, frame);
            }

            this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
            this.framesArray = this.frames.toArray(this.framesArray);
        }
    }

    public void LoadFrames(String ObjectName, String AnimName, int nFrames) {
        this.name = AnimName;
        StringBuilder strBuf = tlsStrBuf.get();
        strBuf.setLength(0);
        strBuf.append(ObjectName);
        strBuf.append("_%_");
        strBuf.append(AnimName);
        strBuf.append("_^");
        int diri = strBuf.indexOf("_%_") + 1;
        int framei = strBuf.lastIndexOf("^");
        strBuf.setCharAt(diri, '9');
        strBuf.setCharAt(framei, '0');
        if (GameServer.server && !ServerGUI.isCreated()) {
            for (int n = 0; n < nFrames; n++) {
                this.frames.add(new IsoDirectionFrame(null));
            }

            this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        }

        Texture tex = Texture.getSharedTexture(strBuf.toString());
        if (tex != null) {
            for (int n = 0; n < nFrames; n++) {
                if (n % 10 == 0 && n > 0) {
                    strBuf.setLength(0);
                    strBuf.append(ObjectName);
                    strBuf.append("_%_");
                    strBuf.append(AnimName);
                    strBuf.append("_^_");
                    diri = strBuf.indexOf("_%_") + 1;
                    framei = strBuf.lastIndexOf("^");
                }

                Integer a = n;
                IsoDirectionFrame frame = null;
                String str = a.toString();
                if (tex != null) {
                    strBuf.setCharAt(diri, '9');

                    for (int l = 0; l < str.length(); l++) {
                        strBuf.setCharAt(framei + l, str.charAt(l));
                    }

                    String stra = strBuf.toString();
                    strBuf.setCharAt(diri, '6');
                    String strb = strBuf.toString();
                    strBuf.setCharAt(diri, '3');
                    String strc = strBuf.toString();
                    strBuf.setCharAt(diri, '2');
                    String strd = strBuf.toString();
                    strBuf.setCharAt(diri, '1');
                    String stre = strBuf.toString();
                    strBuf.setCharAt(diri, '4');
                    String strf = strBuf.toString();
                    strBuf.setCharAt(diri, '7');
                    String strg = strBuf.toString();
                    strBuf.setCharAt(diri, '8');
                    String strh = strBuf.toString();
                    frame = new IsoDirectionFrame(
                        Texture.getSharedTexture(stra),
                        Texture.getSharedTexture(strb),
                        Texture.getSharedTexture(strc),
                        Texture.getSharedTexture(strd),
                        Texture.getSharedTexture(stre),
                        Texture.getSharedTexture(strf),
                        Texture.getSharedTexture(strg),
                        Texture.getSharedTexture(strh)
                    );
                } else {
                    try {
                        strBuf.setCharAt(diri, '8');
                    } catch (Exception var21) {
                        this.LoadFrames(ObjectName, AnimName, nFrames);
                    }

                    for (int l = 0; l < str.length(); l++) {
                        try {
                            strBuf.setCharAt(framei + l, a.toString().charAt(l));
                        } catch (Exception var20) {
                            this.LoadFrames(ObjectName, AnimName, nFrames);
                        }
                    }

                    String stra = strBuf.toString();
                    strBuf.setCharAt(diri, '9');
                    String strb = strBuf.toString();
                    strBuf.setCharAt(diri, '6');
                    String strc = strBuf.toString();
                    strBuf.setCharAt(diri, '3');
                    String strd = strBuf.toString();
                    strBuf.setCharAt(diri, '2');
                    String stre = strBuf.toString();
                    frame = new IsoDirectionFrame(
                        Texture.getSharedTexture(stra),
                        Texture.getSharedTexture(strb),
                        Texture.getSharedTexture(strc),
                        Texture.getSharedTexture(strd),
                        Texture.getSharedTexture(stre)
                    );
                }

                this.frames.add(frame);
            }

            this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
            this.framesArray = this.frames.toArray(this.framesArray);
        }
    }

    public void LoadFramesUseOtherFrame(String ObjectName, String Variant, String AnimName, String OtherAnimName, int nOtherFrameFrame, String pal) {
        this.name = AnimName;
        String pre = OtherAnimName + "_" + Variant + "_";
        String post = "_";
        String palstr = "";
        if (pal != null) {
            palstr = "_" + pal;
        }

        for (int n = 0; n < 1; n++) {
            Integer a = nOtherFrameFrame;
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "9_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "6_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "3_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "2_" + a.toString() + palstr + ".png")
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBits(String ObjectName, String Variant, String AnimName, int nFrames) {
        this.name = AnimName;
        String pre = AnimName + "_" + Variant + "_";
        String post = "_";

        for (int n = 0; n < nFrames; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "9_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "6_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "3_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "2_" + a.toString() + ".png")
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBits(String ObjectName, String AnimName, int nFrames) {
        this.name = AnimName;
        String pre = ObjectName + "_" + AnimName + "_";
        String post = "_";

        for (int n = 0; n < nFrames; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "9_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "6_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "3_" + a.toString() + ".png"),
                Texture.getSharedTexture(pre + "2_" + a.toString() + ".png")
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBitRepeatFrame(String ObjectName, String AnimName, int RepeatFrame) {
        this.name = AnimName;
        String post = "_";
        String palstr = "";
        Integer a = RepeatFrame;
        IsoDirectionFrame frame = new IsoDirectionFrame(
            Texture.getSharedTexture(AnimName + "8_" + a.toString() + ".png"),
            Texture.getSharedTexture(AnimName + "9_" + a.toString() + ".png"),
            Texture.getSharedTexture(AnimName + "6_" + a.toString() + ".png"),
            Texture.getSharedTexture(AnimName + "3_" + a.toString() + ".png"),
            Texture.getSharedTexture(AnimName + "2_" + a.toString() + ".png")
        );
        this.frames.add(frame);
        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBitRepeatFrame(String ObjectName, String Variant, String AnimName, int RepeatFrame, String pal) {
        this.name = AnimName;
        String pre = AnimName + "_" + Variant + "_";
        String post = "_";
        String palstr = "";
        if (pal != null) {
            palstr = "_" + pal;
        }

        Integer a = RepeatFrame;
        IsoDirectionFrame frame = new IsoDirectionFrame(
            Texture.getSharedTexture(pre + "8_" + a.toString() + palstr + ".png"),
            Texture.getSharedTexture(pre + "9_" + a.toString() + palstr + ".png"),
            Texture.getSharedTexture(pre + "6_" + a.toString() + palstr + ".png"),
            Texture.getSharedTexture(pre + "3_" + a.toString() + palstr + ".png"),
            Texture.getSharedTexture(pre + "2_" + a.toString() + palstr + ".png")
        );
        this.frames.add(frame);
        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBits(String ObjectName, String Variant, String AnimName, int nFrames, String pal) {
        this.name = AnimName;
        String pre = AnimName + "_" + Variant + "_";
        String post = "_";
        String palstr = "";
        if (pal != null) {
            palstr = "_" + pal;
        }

        for (int n = 0; n < nFrames; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "9_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "6_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "3_" + a.toString() + palstr + ".png"),
                Texture.getSharedTexture(pre + "2_" + a.toString() + palstr + ".png")
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void Dispose() {
        for (int n = 0; n < this.frames.size(); n++) {
            IsoDirectionFrame dir = this.frames.get(n);
            dir.SetAllDirections(null);
        }
    }

    Texture LoadFrameExplicit(String ObjectName) {
        Texture result = Texture.getSharedTexture(ObjectName);
        IsoDirectionFrame frame = new IsoDirectionFrame(result);
        this.frames.add(frame);
        this.framesArray = this.frames.toArray(this.framesArray);
        return result;
    }

    void LoadFramesNoDir(String ObjectName, String AnimName, int nFrames) {
        this.name = AnimName;
        String pre = "media/" + ObjectName;
        String post = "_" + AnimName + "_";

        for (int n = 0; n < nFrames; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(pre + post + a.toString() + ".png"));
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void LoadFramesNoDirPage(String ObjectName, String AnimName, int nFrames) {
        this.name = AnimName;
        String pre = ObjectName;
        String post = "_" + AnimName + "_";

        for (int n = 0; n < nFrames; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(pre + post + a.toString()));
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void LoadFramesNoDirPageDirect(String ObjectName, String AnimName, int nFrames) {
        this.name = AnimName;
        String pre = ObjectName;
        String post = "_" + AnimName + "_";

        for (int n = 0; n < nFrames; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(pre + post + a.toString() + ".png"));
            this.frames.add(frame);
        }

        this.framesArray = this.frames.toArray(this.framesArray);
        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
    }

    void LoadFramesNoDirPage(String ObjectName) {
        this.name = "default";
        String pre = ObjectName;

        for (int n = 0; n < 1; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(pre));
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesPageSimple(String NObjectName, String SObjectName, String EObjectName, String WObjectName) {
        this.name = "default";

        for (int n = 0; n < 1; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(NObjectName),
                Texture.getSharedTexture(SObjectName),
                Texture.getSharedTexture(EObjectName),
                Texture.getSharedTexture(WObjectName)
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void LoadFramesPalette(String ObjectName, String AnimName, int nFrames, String Palette) {
        this.name = AnimName;
        String pre = ObjectName + "_";
        String post = "_" + AnimName + "_";

        for (int n = 0; n < nFrames; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8" + post + a.toString() + "_" + Palette),
                Texture.getSharedTexture(pre + "9" + post + a.toString() + "_" + Palette),
                Texture.getSharedTexture(pre + "6" + post + a.toString() + "_" + Palette),
                Texture.getSharedTexture(pre + "3" + post + a.toString() + "_" + Palette),
                Texture.getSharedTexture(pre + "2" + post + a.toString() + "_" + Palette)
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void DupeFrame() {
        for (int n = 0; n < 8; n++) {
            IsoDirectionFrame fr = new IsoDirectionFrame();
            fr.directions[n] = this.frames.get(0).directions[n];
            fr.doFlip = this.frames.get(0).doFlip;
            this.frames.add(fr);
        }

        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public boolean hasNoTextures() {
        for (int i = 0; i < this.frames.size(); i++) {
            IsoDirectionFrame frame = this.frames.get(i);
            if (!frame.hasNoTextures()) {
                return false;
            }
        }

        return true;
    }
}
