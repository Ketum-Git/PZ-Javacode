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

    void LoadExtraFrame(String objectName, String animName, int i) {
        this.name = animName;
        String pre = objectName + "_";
        String post = "_" + animName + "_";
        IsoDirectionFrame frame = new IsoDirectionFrame(
            Texture.getSharedTexture(pre + "8" + post + i + ".png"),
            Texture.getSharedTexture(pre + "9" + post + i + ".png"),
            Texture.getSharedTexture(pre + "6" + post + i + ".png"),
            Texture.getSharedTexture(pre + "3" + post + i + ".png"),
            Texture.getSharedTexture(pre + "2" + post + i + ".png")
        );
        this.frames.add(frame);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesReverseAltName(String objectName, String animName, String altName, int nFrames) {
        this.name = altName;
        StringBuilder strBuf = tlsStrBuf.get();
        strBuf.setLength(0);
        strBuf.append(objectName);
        strBuf.append("_%_");
        strBuf.append(animName);
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
                    strBuf.append(objectName);
                    strBuf.append("_1_");
                    strBuf.append(animName);
                    strBuf.append("_10");
                }

                Integer a = n;
                String str = a.toString();
                IsoDirectionFrame frame;
                if (tex == null) {
                    strBuf.setCharAt(diri, '8');

                    try {
                        strBuf.setCharAt(framei, a.toString().charAt(0));
                    } catch (Exception var21) {
                        this.LoadFramesReverseAltName(objectName, animName, altName, nFrames);
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

    public void LoadFrames(String objectName, String animName, int nFrames) {
        this.name = animName;
        StringBuilder strBuf = tlsStrBuf.get();
        strBuf.setLength(0);
        strBuf.append(objectName);
        strBuf.append("_%_");
        strBuf.append(animName);
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
                    strBuf.append(objectName);
                    strBuf.append("_%_");
                    strBuf.append(animName);
                    strBuf.append("_^_");
                    diri = strBuf.indexOf("_%_") + 1;
                    framei = strBuf.lastIndexOf("^");
                }

                Integer a = n;
                String str = a.toString();
                IsoDirectionFrame frame;
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
                        this.LoadFrames(objectName, animName, nFrames);
                    }

                    for (int l = 0; l < str.length(); l++) {
                        try {
                            strBuf.setCharAt(framei + l, a.toString().charAt(l));
                        } catch (Exception var20) {
                            this.LoadFrames(objectName, animName, nFrames);
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

    public void LoadFramesUseOtherFrame(String objectName, String variant, String animName, String otherAnimName, int nOtherFrameFrame, String pal) {
        this.name = animName;
        String pre = otherAnimName + "_" + variant + "_";
        String post = "_";
        String palstr = "";
        if (pal != null) {
            palstr = "_" + pal;
        }

        for (int n = 0; n < 1; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + nOtherFrameFrame + palstr + ".png"),
                Texture.getSharedTexture(pre + "9_" + nOtherFrameFrame + palstr + ".png"),
                Texture.getSharedTexture(pre + "6_" + nOtherFrameFrame + palstr + ".png"),
                Texture.getSharedTexture(pre + "3_" + nOtherFrameFrame + palstr + ".png"),
                Texture.getSharedTexture(pre + "2_" + nOtherFrameFrame + palstr + ".png")
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBits(String objectName, String variant, String animName, int nFrames) {
        this.name = animName;
        String pre = animName + "_" + variant + "_";
        String post = "_";

        for (int n = 0; n < nFrames; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + n + ".png"),
                Texture.getSharedTexture(pre + "9_" + n + ".png"),
                Texture.getSharedTexture(pre + "6_" + n + ".png"),
                Texture.getSharedTexture(pre + "3_" + n + ".png"),
                Texture.getSharedTexture(pre + "2_" + n + ".png")
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBits(String objectName, String animName, int nFrames) {
        this.name = animName;
        String pre = objectName + "_" + animName + "_";
        String post = "_";

        for (int n = 0; n < nFrames; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + n + ".png"),
                Texture.getSharedTexture(pre + "9_" + n + ".png"),
                Texture.getSharedTexture(pre + "6_" + n + ".png"),
                Texture.getSharedTexture(pre + "3_" + n + ".png"),
                Texture.getSharedTexture(pre + "2_" + n + ".png")
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBitRepeatFrame(String objectName, String animName, int repeatFrame) {
        this.name = animName;
        String post = "_";
        String palstr = "";
        IsoDirectionFrame frame = new IsoDirectionFrame(
            Texture.getSharedTexture(animName + "8_" + repeatFrame + ".png"),
            Texture.getSharedTexture(animName + "9_" + repeatFrame + ".png"),
            Texture.getSharedTexture(animName + "6_" + repeatFrame + ".png"),
            Texture.getSharedTexture(animName + "3_" + repeatFrame + ".png"),
            Texture.getSharedTexture(animName + "2_" + repeatFrame + ".png")
        );
        this.frames.add(frame);
        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBitRepeatFrame(String objectName, String variant, String animName, int repeatFrame, String pal) {
        this.name = animName;
        String pre = animName + "_" + variant + "_";
        String post = "_";
        String palstr = "";
        if (pal != null) {
            palstr = "_" + pal;
        }

        IsoDirectionFrame frame = new IsoDirectionFrame(
            Texture.getSharedTexture(pre + "8_" + repeatFrame + palstr + ".png"),
            Texture.getSharedTexture(pre + "9_" + repeatFrame + palstr + ".png"),
            Texture.getSharedTexture(pre + "6_" + repeatFrame + palstr + ".png"),
            Texture.getSharedTexture(pre + "3_" + repeatFrame + palstr + ".png"),
            Texture.getSharedTexture(pre + "2_" + repeatFrame + palstr + ".png")
        );
        this.frames.add(frame);
        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesBits(String objectName, String variant, String animName, int nFrames, String pal) {
        this.name = animName;
        String pre = animName + "_" + variant + "_";
        String post = "_";
        String palstr = "";
        if (pal != null) {
            palstr = "_" + pal;
        }

        for (int n = 0; n < nFrames; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8_" + n + palstr + ".png"),
                Texture.getSharedTexture(pre + "9_" + n + palstr + ".png"),
                Texture.getSharedTexture(pre + "6_" + n + palstr + ".png"),
                Texture.getSharedTexture(pre + "3_" + n + palstr + ".png"),
                Texture.getSharedTexture(pre + "2_" + n + palstr + ".png")
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

    Texture LoadFrameExplicit(String objectName) {
        Texture result = Texture.getSharedTexture(objectName);
        IsoDirectionFrame frame = new IsoDirectionFrame(result);
        this.frames.add(frame);
        this.framesArray = this.frames.toArray(this.framesArray);
        return result;
    }

    void LoadFramesNoDir(String objectName, String animName, int nFrames) {
        this.name = animName;
        String pre = "media/" + objectName;
        String post = "_" + animName + "_";

        for (int n = 0; n < nFrames; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(pre + post + n + ".png"));
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void LoadFramesNoDirPage(String objectName, String animName, int nFrames) {
        this.name = animName;
        String post = "_" + animName + "_";

        for (int n = 0; n < nFrames; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(objectName + post + n));
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void LoadFramesNoDirPageDirect(String objectName, String animName, int nFrames) {
        this.name = animName;
        String post = "_" + animName + "_";

        for (int n = 0; n < nFrames; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(objectName + post + n + ".png"));
            this.frames.add(frame);
        }

        this.framesArray = this.frames.toArray(this.framesArray);
        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
    }

    void LoadFramesNoDirPage(String objectName) {
        this.name = "default";

        for (int n = 0; n < 1; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(Texture.getSharedTexture(objectName));
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    public void LoadFramesPageSimple(String nObjectName, String sObjectName, String eObjectName, String wObjectName) {
        this.name = "default";

        for (int n = 0; n < 1; n++) {
            Integer a = n;
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(nObjectName),
                Texture.getSharedTexture(sObjectName),
                Texture.getSharedTexture(eObjectName),
                Texture.getSharedTexture(wObjectName)
            );
            this.frames.add(frame);
        }

        this.finishUnloopedOnFrame = (short)(this.frames.size() - 1);
        this.framesArray = this.frames.toArray(this.framesArray);
    }

    void LoadFramesPalette(String objectName, String animName, int nFrames, String palette) {
        this.name = animName;
        String pre = objectName + "_";
        String post = "_" + animName + "_";

        for (int n = 0; n < nFrames; n++) {
            IsoDirectionFrame frame = new IsoDirectionFrame(
                Texture.getSharedTexture(pre + "8" + post + n + "_" + palette),
                Texture.getSharedTexture(pre + "9" + post + n + "_" + palette),
                Texture.getSharedTexture(pre + "6" + post + n + "_" + palette),
                Texture.getSharedTexture(pre + "3" + post + n + "_" + palette),
                Texture.getSharedTexture(pre + "2" + post + n + "_" + palette)
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
