// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.character.PlayerID;

@UsedFromLua
public class IsoButcherHook extends IsoObject {
    private IsoAnimal animal;
    private boolean removingBlood;
    private float removingBloodProgress;
    private float removingBloodTick;
    private float bloodAtStart;
    private float currentBlood;
    private KahluaTableImpl luaHook;
    private boolean playRemovingBloodSound;
    private BaseSoundEmitter emitter;
    private final PlayerID usingPlayerId = new PlayerID();

    public IsoButcherHook(IsoGridSquare sq) {
        this.sprite = IsoSpriteManager.instance.getSprite("crafted_04_120");
        this.square = sq;
        sq.getCell().addToProcessIsoObject(this);
    }

    public IsoButcherHook(IsoCell cell) {
        super(cell);
        if (cell != null) {
            cell.addToProcessIsoObject(this);
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        if (GameClient.client) {
            this.getCell().addToProcessIsoObject(this);
        }
    }

    public void stopRemovingBlood() {
        this.removingBlood = false;
        this.playRemovingBloodSound = false;
        this.removingBloodTick = 0.0F;
        this.removingBloodProgress = 0.0F;
        Object functionObj = LuaManager.getFunctionObject("ISButcherHookUI.onStopBleedingAnimal");
        if (functionObj != null && this.luaHook != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, this.luaHook);
        }

        if (GameServer.server) {
            this.animal.transmitModData();
        }
    }

    public void startRemovingBlood(KahluaTableImpl luaHook) {
        this.removingBlood = true;
        this.bloodAtStart = ((KahluaTableImpl)this.getAnimal().getModData()).rawgetFloat("BloodQty");
        this.luaHook = luaHook;
        if (GameServer.server) {
            this.sync();
        }
    }

    @Override
    public void update() {
        if (this.removingBlood) {
            this.updateRemovingBlood();
        }

        this.updateRemovingBloodSound();
        this.updateDeathAge();
    }

    private void updateRemovingBlood() {
        if (this.getAnimal() == null) {
            this.removingBlood = false;
            this.removingBloodTick = 0.0F;
            this.removingBloodProgress = 0.0F;
        } else {
            this.removingBloodTick = this.removingBloodTick + GameTime.getInstance().getMultiplier();
            this.currentBlood = ((KahluaTableImpl)this.getAnimal().getModData()).rawgetFloat("BloodQty");
            float currentProgress = 1.0F - this.currentBlood / this.bloodAtStart;
            float nextProgress = 1.0F - (this.currentBlood - 0.5F) / this.bloodAtStart;
            float diff = nextProgress - currentProgress;
            float tickDiff = this.removingBloodTick / 30.0F;
            this.removingBloodProgress = 1.0F - this.currentBlood / this.bloodAtStart + diff * tickDiff;
            if (this.removingBloodTick >= 30.0F) {
                this.removingBloodTick = 0.0F;
                this.currentBlood -= 0.5F;
                if (this.currentBlood <= 0.0F) {
                    this.currentBlood = 0.0F;
                }

                this.getAnimal().getModData().rawset("BloodQty", (double)this.currentBlood);
                int randBlood = Rand.Next(5, 10);

                for (int i = 0; i < randBlood; i++) {
                    this.getSquare()
                        .getChunk()
                        .addBloodSplat(
                            this.getAnimal().getX() + Rand.Next(-0.2F, 0.2F), this.getAnimal().getY() + Rand.Next(-0.2F, 0.2F), this.getZ(), Rand.Next(20)
                        );
                }

                if (this.currentBlood <= 0.0F) {
                    this.stopRemovingBlood();
                }
            }
        }
    }

    public void setPlayRemovingBloodSound(boolean b) {
        this.playRemovingBloodSound = b;
    }

    private void updateRemovingBloodSound() {
        if ((this.isRemovingBlood() || this.playRemovingBloodSound) && this.getObjectIndex() != -1 && this.animal != null) {
            if (this.emitter == null) {
                this.emitter = IsoWorld.instance.getFreeEmitter(this.animal.getX(), this.animal.getY(), this.square.z);
                IsoWorld.instance.setEmitterOwner(this.emitter, this);
            }

            if (!this.emitter.isPlaying("ButcheringBleedCorpse")) {
                this.emitter.playSoundImpl("ButcheringBleedCorpse", (IsoObject)null);
            }
        } else if (this.emitter != null) {
            this.emitter.stopOrTriggerSoundByName("ButcheringBleedCorpse");
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
            this.emitter = null;
        }
    }

    public boolean isRemovingBlood() {
        return this.removingBlood;
    }

    public float getRemovingBloodProgress() {
        return this.removingBloodProgress;
    }

    private void updateDeathAge() {
        IsoAnimal animal = this.getAnimal();
        if (animal != null) {
            if (animal.getModData().rawget("deathTime") == null) {
                animal.getModData().rawset("deathTime", GameTime.getInstance().getWorldAgeHours());
            }

            if (animal.getModData().rawget("deathTime") instanceof Double deathTime) {
                double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
                if (deathTime < 0.0) {
                    deathTime = worldAgeHours;
                } else if (deathTime > worldAgeHours) {
                    deathTime = worldAgeHours;
                }

                animal.getModData().rawset("deathAge", worldAgeHours - deathTime);
            }
        }
    }

    @Override
    public String getObjectName() {
        return "ButcherHook";
    }

    public void setAnimal(IsoAnimal animal) {
        this.animal = animal;
    }

    public IsoAnimal getAnimal() {
        return this.animal;
    }

    public void removeHook() {
        if (this.getAnimal() != null) {
            IsoDeadBody body = new IsoDeadBody(this.getAnimal(), false);
            body.setZ(this.getZ());
            body.setModData(this.getAnimal().getModData());
            body.invalidateCorpse();
            this.getAnimal().remove();
            if (GameServer.server) {
                GameServer.sendCorpse(body);
            }
        }

        this.removeFromWorld();
        this.getSquare().transmitRemoveItemFromSquare(this);
    }

    public void playPutDownCorpseSound(IsoAnimal animal) {
        if (animal != null) {
            BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(animal.getX(), animal.getY(), this.getZ());
            AnimalDefinitions def = AnimalDefinitions.getDef(animal.getAnimalType());
            if (def == null) {
                emitter.playSound("BodyHitGround");
            } else {
                AnimalBreed breed = animal.getBreed();
                if (breed == null) {
                    emitter.playSound("BodyHitGround");
                } else {
                    AnimalBreed.Sound sound = breed.getSound("put_down_corpse");
                    if (sound == null) {
                        emitter.playSound("BodyHitGround");
                    } else {
                        emitter.playSound(sound.soundName);
                    }
                }
            }
        }
    }

    @Override
    public void removeFromWorld() {
        if (this.getAnimal() != null) {
            this.getAnimal().attachBackToHookX = this.getXi();
            this.getAnimal().attachBackToHookY = this.getYi();
            this.getAnimal().attachBackToHookZ = this.getZi();
        }

        if (this.emitter != null) {
            this.emitter.stopAll();
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
            this.emitter = null;
        }

        super.removeFromWorld();
    }

    public void reattachAnimal(IsoAnimal animal) {
        this.setAnimal(animal);
        animal.setHook(this);
        animal.setOnHook(true);
        animal.setVariable("onhook", true);
        animal.getAdvancedAnimator().setState("onhook");
        Object functionObj = LuaManager.getFunctionObject("ISButcherHookUI.onReattachAnimal");
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, this, animal);
        }
    }

    public void setLuaHook(KahluaTableImpl luaHook) {
        this.luaHook = luaHook;
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        super.syncIsoObjectSend(b);
        if (this.animal != null) {
            b.putByte((byte)1);
            b.putFloat(this.currentBlood);
        } else {
            b.putByte((byte)0);
        }

        if (this.removingBlood) {
            b.putByte((byte)1);
            b.putFloat(this.bloodAtStart);
        } else {
            b.putByte((byte)0);
        }

        if (this.getUsingPlayer() != null) {
            b.putByte((byte)1);
            this.usingPlayerId.set(this.getUsingPlayer());
            this.usingPlayerId.write(b.bb);
        } else {
            b.putByte((byte)0);
        }
    }

    @Override
    public void syncIsoObjectReceive(ByteBuffer bb) {
        super.syncIsoObjectReceive(bb);
        boolean hasAnimal = bb.get() != 0;
        if (hasAnimal) {
            this.currentBlood = bb.getFloat();
        } else {
            this.setAnimal(null);
        }

        this.removingBlood = bb.get() != 0;
        if (this.removingBlood) {
            this.bloodAtStart = bb.getFloat();
        }

        boolean isUsingByPlayer = bb.get() != 0;
        if (isUsingByPlayer) {
            this.usingPlayerId.parse(bb, null);
            this.setUsingPlayer(this.usingPlayerId.getPlayer());
        } else {
            this.setUsingPlayer(null);
        }

        this.onReceivedNetUpdate();
    }

    public void onReceivedNetUpdate() {
        Object functionObj = LuaManager.getFunctionObject("ISButcherHookUI.onHookReceivedNetUpdate");
        if (functionObj != null && this.luaHook != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, this.luaHook);
        } else if (this.getAnimal() != null) {
            this.updateAnimalModel();
        }
    }

    public void updateAnimalModel() {
        this.animal.resetModel();
        this.animal.resetModelNextFrame();
        this.reattachAnimal(this.animal);
    }
}
