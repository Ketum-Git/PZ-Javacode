// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;
import zombie.ZomboidFileSystem;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntries;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

/**
 * Used for recording the activity of an AnimationPlayer
 */
public final class AnimationPlayerRecorder {
    private static boolean isInitialized;
    private boolean isRecording;
    private boolean isLineActive;
    private final AnimationTrackRecordingFrame animationTrackFrame;
    private final AnimationNodeRecordingFrame animationNodeFrame;
    private final AnimationVariableRecordingFrame animationVariableFrame;
    private final AnimationEventRecordingFrame animationEventFrame;
    private final IsoGameCharacter character;

    public AnimationPlayerRecorder(IsoGameCharacter owner) {
        this.character = owner;
        String characterId = this.character.getUID();
        String fileKey = characterId + "_AnimRecorder";
        this.animationTrackFrame = new AnimationTrackRecordingFrame(fileKey + "_Track");
        this.animationNodeFrame = new AnimationNodeRecordingFrame(fileKey + "_Node");
        this.animationVariableFrame = new AnimationVariableRecordingFrame(fileKey + "_Vars");
        this.animationEventFrame = new AnimationEventRecordingFrame(fileKey + "_Events");
        init();
    }

    public static synchronized void init() {
        if (!isInitialized) {
            DebugLog.General.debugln("Initializing...");
            isInitialized = true;
            backupOldRecordings();
        }
    }

    public static void backupOldRecordings() {
        String recordingDir = getRecordingDir();

        try {
            File recordingsDir = new File(recordingDir);
            File[] recordingFileList = ZomboidFileSystem.listAllFiles(recordingsDir);
            if (recordingFileList.length == 0) {
                return;
            }

            String backupDirName = "backup_" + ZomboidFileSystem.getStartupTimeStamp();
            File backupDir = new File(recordingDir + File.separator + backupDirName);
            ZomboidFileSystem.ensureFolderExists(backupDir);

            for (int i = 0; i < recordingFileList.length; i++) {
                File fileToMove = recordingFileList[i];
                if (fileToMove.isFile()) {
                    fileToMove.renameTo(new File(backupDir.getAbsolutePath() + File.separator + fileToMove.getName()));
                    fileToMove.delete();
                }
            }
        } catch (Exception var6) {
            DebugLog.General.printException(var6, "Exception thrown trying to backup old recordings, Trying to copy old recording files.", LogSeverity.Error);
        }
    }

    public static void discardOldRecordings() {
        String recordingDir = getRecordingDir();

        try {
            File recordingsDir = new File(recordingDir);
            File[] recordingFileList = ZomboidFileSystem.listAllFiles(recordingsDir);
            if (recordingFileList.length == 0) {
                return;
            }

            for (int i = 0; i < recordingFileList.length; i++) {
                File fileToMove = recordingFileList[i];
                if (fileToMove.isFile()) {
                    fileToMove.delete();
                }
            }
        } catch (Exception var5) {
            DebugLog.General
                .printException(var5, "Exception thrown trying to discard old recordings, Trying to delete old recording files.", LogSeverity.Error);
        }
    }

    public void newFrame(int frameNo) {
        if (this.isLineActive) {
            this.writeFrame();
        }

        if (!this.isRecording()) {
            this.close();
        } else {
            this.isLineActive = true;
            this.animationTrackFrame.reset();
            this.animationTrackFrame.setFrameNumber(frameNo);
            this.animationNodeFrame.reset();
            this.animationNodeFrame.setFrameNumber(frameNo);
            this.animationVariableFrame.reset();
            this.animationVariableFrame.setFrameNumber(frameNo);
            this.animationEventFrame.reset();
            this.animationEventFrame.setFrameNumber(frameNo);
        }
    }

    public boolean hasActiveLine() {
        return this.isLineActive;
    }

    public void writeFrame() {
        this.animationTrackFrame.writeLine();
        this.animationNodeFrame.writeLine();
        this.animationVariableFrame.writeLine();
        this.animationEventFrame.writeLine();
        this.isLineActive = false;
    }

    public void discardRecording() {
        this.animationTrackFrame.closeAndDiscard();
        this.animationNodeFrame.closeAndDiscard();
        this.animationVariableFrame.closeAndDiscard();
        this.animationEventFrame.closeAndDiscard();
        this.isLineActive = false;
    }

    public void close() {
        this.animationTrackFrame.close();
        this.animationNodeFrame.close();
        this.animationVariableFrame.close();
        this.animationEventFrame.close();
        this.isLineActive = false;
    }

    public static PrintStream openFileStream(String key, boolean append, Consumer<String> fileNameConsumer) {
        String filePath = getTimeStampedFilePath(key);

        try {
            fileNameConsumer.accept(filePath);
            File file = new File(filePath);
            return new PrintStream(new FileOutputStream(file, append), true);
        } catch (FileNotFoundException var5) {
            DebugLog.General.error("Exception thrown trying to create animation player recording file.");
            DebugLog.General.error(var5);
            var5.printStackTrace();
            return null;
        }
    }

    public static String getRecordingDir() {
        String recordingDirPath = ZomboidFileSystem.instance.getCacheDirSub("Recording");
        ZomboidFileSystem.ensureFolderExists(recordingDirPath);
        File recordingDir = new File(recordingDirPath);
        return recordingDir.getAbsolutePath();
    }

    private static String getTimeStampedFilePath(String key) {
        return getRecordingDir() + File.separator + getTimeStampedFileName(key) + ".csv";
    }

    private static String getTimeStampedFileName(String name) {
        return ZomboidFileSystem.getStartupTimeStamp() + "_" + name;
    }

    public void logAnimWeights(LiveAnimationTrackEntries trackEntries, Vector2 deferredMovement, Vector2 deferredMovementFromRagdoll) {
        this.animationTrackFrame.logAnimWeights(trackEntries);
        this.animationVariableFrame.logDeferredMovement(deferredMovement, deferredMovementFromRagdoll);
    }

    public void logAnimNode(LiveAnimNode liveNode) {
        if (liveNode.isTransitioningIn()) {
            this.animationNodeFrame
                .logWeight(
                    "transition(" + liveNode.getTransitionFrom() + "->" + liveNode.getName() + ")",
                    liveNode.getTransitionLayerIdx(),
                    liveNode.getTransitionInWeight()
                );
        }

        if (liveNode.runningRagdollTrack != null) {
            this.animationNodeFrame
                .logWeight(
                    liveNode.getName() + "." + liveNode.runningRagdollTrack.getName(), liveNode.getLayerIdx(), liveNode.runningRagdollTrack.getBlendWeight()
                );
        }

        this.animationNodeFrame.logWeight(liveNode.getName(), liveNode.getLayerIdx(), liveNode.getWeight());
    }

    public void logActionState(ActionGroup in_actionGroup, ActionState in_actionState, List<ActionState> in_subStates) {
        this.animationNodeFrame.logActionState(in_actionGroup, in_actionState, in_subStates);
    }

    public void logAIState(State state, List<StateMachine.SubstateSlot> subStates) {
        this.animationNodeFrame.logAIState(state, subStates);
    }

    public void logAnimState(AnimState state) {
        this.animationNodeFrame.logAnimState(state);
    }

    public void logVariables(IAnimationVariableSource varSource) {
        this.animationVariableFrame.logVariables(varSource);
    }

    public void logAnimEvent(AnimationTrack track, AnimEvent evt) {
        this.animationEventFrame.logAnimEvent(track, evt);
    }

    public void logGlobalAnimEvent(AnimEvent evt) {
        this.animationEventFrame.logGlobalAnimEvent(evt);
    }

    public void logCharacterPos() {
        IsoPlayer player = IsoPlayer.getInstance();
        IsoGameCharacter chr = this.getOwner();
        Vector3 playerPos = player.getPosition(new Vector3());
        Vector3 charPos = chr.getPosition(new Vector3());
        Vector3 diff = playerPos.sub(charPos, new Vector3());
        this.animationNodeFrame.logCharacterToPlayerDiff(diff);
    }

    public IsoGameCharacter getOwner() {
        return this.character;
    }

    public boolean isRecording() {
        return this.isRecording;
    }

    public void setRecording(boolean value) {
        if (this.isRecording != value) {
            this.isRecording = value;
            if (!this.isRecording) {
                this.close();
            }

            DebugLog.General.println("AnimationPlayerRecorder %s.", this.isRecording ? "recording" : "stopped");
        }
    }
}
