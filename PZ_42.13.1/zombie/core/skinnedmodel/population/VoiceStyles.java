// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;

@XmlRootElement(name = "voiceStyles")
@UsedFromLua
public class VoiceStyles {
    @XmlElement(name = "style")
    public final ArrayList<VoiceStyle> styles = new ArrayList<>();
    @XmlTransient
    public static VoiceStyles instance;

    public static void init() {
        instance = Parse(
            ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath()
                + File.separator
                + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar)
        );
        if (instance != null) {
            instance.styles.add(0, new VoiceStyle());

            for (String modID : ZomboidFileSystem.instance.getModIDs()) {
                ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
                if (mod != null) {
                    String modDir = mod.getVersionDir();
                    VoiceStyles VoiceStyles = Parse(
                        modDir + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar)
                    );
                    if (VoiceStyles != null) {
                        for (VoiceStyle styleMod : VoiceStyles.styles) {
                            VoiceStyle style = instance.FindStyle(styleMod.prefix);
                            if (style == null) {
                                instance.styles.add(styleMod);
                            } else {
                                if (DebugLog.isEnabled(DebugType.Sound)) {
                                    DebugLog.Sound.println("mod \"%s\" overrides voice \"%s\"", modID, styleMod.prefix);
                                }

                                int index = instance.styles.indexOf(style);
                                instance.styles.set(index, styleMod);
                            }
                        }
                    } else {
                        modDir = mod.getCommonDir();
                        VoiceStyles = Parse(
                            modDir + File.separator + ZomboidFileSystem.processFilePath("media/voiceStyles/voiceStyles.xml", File.separatorChar)
                        );
                        if (VoiceStyles != null) {
                            for (VoiceStyle styleModx : VoiceStyles.styles) {
                                VoiceStyle style = instance.FindStyle(styleModx.prefix);
                                if (style == null) {
                                    instance.styles.add(styleModx);
                                } else {
                                    if (DebugLog.isEnabled(DebugType.Sound)) {
                                        DebugLog.Sound.println("mod \"%s\" overrides voice \"%s\"", modID, styleModx.prefix);
                                    }

                                    int index = instance.styles.indexOf(style);
                                    instance.styles.set(index, styleModx);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void Reset() {
        if (instance != null) {
            instance.styles.clear();
            instance = null;
        }
    }

    public static VoiceStyles Parse(String filename) {
        try {
            return parse(filename);
        } catch (FileNotFoundException var2) {
        } catch (IOException | JAXBException var3) {
            ExceptionLogger.logException(var3);
        }

        return null;
    }

    public static VoiceStyles parse(String filename) throws JAXBException, IOException {
        VoiceStyles var4;
        try (FileInputStream adrFile = new FileInputStream(filename)) {
            JAXBContext ctx = JAXBContext.newInstance(VoiceStyles.class);
            Unmarshaller um = ctx.createUnmarshaller();
            var4 = (VoiceStyles)um.unmarshal(adrFile);
        }

        return var4;
    }

    public VoiceStyle FindStyle(String name) {
        for (int i = 0; i < this.styles.size(); i++) {
            VoiceStyle style = this.styles.get(i);
            if (style.prefix.equalsIgnoreCase(name)) {
                return style;
            }
        }

        return null;
    }

    public VoiceStyles getInstance() {
        return instance;
    }

    public ArrayList<VoiceStyle> getAllStyles() {
        return this.styles;
    }
}
