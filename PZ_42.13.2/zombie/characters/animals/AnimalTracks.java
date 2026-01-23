// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@UsedFromLua
public class AnimalTracks {
    public String animalType;
    public long addedTime;
    public String trackType;
    public IsoDirections dir;
    public int x;
    public int y;
    public int minSkill;
    public boolean addedToWorld;
    private InventoryItem item;

    public static AnimalTracks addAnimalTrack(VirtualAnimal animal, AnimalTracksDefinitions.AnimalTracksType trackType) {
        AnimalTracks track = new AnimalTracks();
        track.animalType = animal.migrationGroup;
        track.trackType = trackType.type;
        if (trackType.needDir) {
            track.dir = IsoDirections.fromAngle(animal.forwardDirection.x, animal.forwardDirection.y);
        }

        track.x = Rand.Next((int)animal.x - 2, (int)animal.x + 2);
        track.y = Rand.Next((int)animal.y - 2, (int)animal.y + 2);
        track.addedTime = GameTime.getInstance().getCalender().getTimeInMillis();
        return track;
    }

    public static AnimalTracks addAnimalTrackAtPos(VirtualAnimal animal, int x, int y, AnimalTracksDefinitions.AnimalTracksType trackType, long timeMinus) {
        AnimalTracks track = new AnimalTracks();
        track.animalType = animal.migrationGroup;
        track.trackType = trackType.type;
        if (trackType.needDir) {
            track.dir = IsoDirections.fromAngle(animal.forwardDirection.x, animal.forwardDirection.y);
        }

        track.x = x;
        track.y = y;
        track.addedTime = GameTime.getInstance().getCalender().getTimeInMillis() - timeMinus;
        return track;
    }

    public boolean canFindTrack(IsoGameCharacter chr) {
        if (this.addedToWorld) {
            return true;
        } else if (chr != null && chr.getCurrentSquare() != null) {
            AnimalTracksDefinitions def = AnimalTracksDefinitions.tracksDefinitions.get(this.animalType);
            if (def == null) {
                return false;
            } else {
                AnimalTracksDefinitions.AnimalTracksType track = def.tracks.get(this.trackType);
                if (track == null) {
                    return false;
                } else if (chr.getPerkLevel(PerkFactory.Perks.Tracking) >= track.minSkill) {
                    float totalChance = def.chanceToFindTrack + track.chanceToFindTrack;
                    totalChance /= (chr.getPerkLevel(PerkFactory.Perks.Tracking) + 1) / 0.7F;
                    float dist = chr.getCurrentSquare().DistToProper(this.x, this.y);
                    if (dist < 20.0F) {
                        dist = (20.0F - dist) / 20.0F;
                        totalChance /= dist + 2.0F;
                    }

                    if (dist < 4.0F) {
                        totalChance /= 20.0F;
                    }

                    if (!Rand.NextBool((int)totalChance)) {
                        this.addTrackingExp(chr, false);
                        return false;
                    } else {
                        this.addTrackingExp(chr, true);
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public void addTrackingExp(IsoGameCharacter chr, boolean success) {
        if (success) {
            if (GameServer.server) {
                GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.Tracking, Rand.Next(7.0F, 15.0F));
            } else if (!GameClient.client) {
                chr.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(7.0F, 15.0F));
            }
        } else if (GameServer.server) {
            if (Rand.NextBool(10)) {
                GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.Tracking, Rand.Next(2.0F, 4.0F));
            }
        } else if (!GameClient.client && Rand.NextBool(10)) {
            chr.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(2.0F, 4.0F));
        }
    }

    public static String getTrackStr(String trackType) {
        return Translator.getText("IGUI_AnimalTracks_" + trackType);
    }

    public static ArrayList<AnimalTracks> getAndFindNearestTracks(IsoGameCharacter character) {
        ArrayList<AnimalTracks> result = getNearestTracks(
            (int)character.getX(), (int)character.getY(), 20 + character.getPerkLevel(PerkFactory.Perks.Tracking) * 2
        );
        if (result == null) {
            return null;
        } else {
            for (AnimalTracks track : result) {
                if (track.canFindTrack(character)) {
                    if (track.isItem()) {
                        track.addItemToWorld();
                    } else {
                        track.addToWorld();
                    }

                    if (GameServer.server) {
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalTracks, track.x, track.y, character, track, track.getItem());
                    }
                }
            }

            return result;
        }
    }

    public static ArrayList<AnimalTracks> getNearestTracks(int x, int y, int radius) {
        ArrayList<AnimalTracks> result = new ArrayList<>();
        AnimalCell cell = AnimalManagerWorker.getInstance().getCellFromSquarePos(PZMath.fastfloor((float)x), PZMath.fastfloor((float)y));
        if (cell == null) {
            return null;
        } else {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
            if (sq == null) {
                return null;
            } else {
                AnimalManagerWorker.getInstance().loadIfNeeded(cell);

                for (int i = 0; i < cell.chunks.length; i++) {
                    if (cell.chunks[i] != null && !cell.chunks[i].animalTracks.isEmpty()) {
                        for (int j = 0; j < cell.chunks[i].animalTracks.size(); j++) {
                            AnimalTracks trackTest = cell.chunks[i].animalTracks.get(j);
                            if (sq.DistToProper(trackTest.x, trackTest.y) <= radius) {
                                result.add(trackTest);
                            }
                        }
                    }
                }

                return result;
            }
        }
    }

    public void save(ByteBuffer output) throws IOException {
        GameWindow.WriteString(output, this.animalType);
        GameWindow.WriteString(output, this.trackType);
        output.putInt(this.x);
        output.putInt(this.y);
        if (this.dir != null) {
            output.put((byte)1);
            output.putInt(this.dir.index());
        } else {
            output.put((byte)0);
        }

        output.putLong(this.addedTime);
        output.put((byte)(this.addedToWorld ? 1 : 0));
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.animalType = GameWindow.ReadString(input);
        this.trackType = GameWindow.ReadString(input);
        this.x = input.getInt();
        this.y = input.getInt();
        if (input.get() == 1) {
            this.dir = IsoDirections.fromIndex(input.getInt());
        }

        this.addedTime = input.getLong();
        this.addedToWorld = input.get() == 1;
    }

    public String getTrackType() {
        return this.trackType;
    }

    public String getTrackAge(IsoGameCharacter chr) {
        return PZMath.fastfloor((float)((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L)) + " mins ago";
    }

    public IsoDirections getDir() {
        return this.dir;
    }

    public int getMinSkill() {
        return this.minSkill;
    }

    public String getTrackItem() {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
        return track == null ? null : track.item;
    }

    public String getTrackSprite() {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
        if (track == null) {
            return null;
        } else if (track.sprites != null && this.dir != null) {
            return track.sprites.get(this.dir);
        } else if (!StringUtils.isNullOrEmpty(track.sprite)) {
            return track.sprite;
        } else {
            DebugLog.Animal.debugln("Couldn't find sprite for track " + this.trackType + " for animal " + this.animalType);
            return null;
        }
    }

    public boolean isAddedToWorld() {
        return this.addedToWorld;
    }

    public void setAddedToWorld(boolean b) {
        this.addedToWorld = b;
    }

    public InventoryItem addItemToWorld() {
        if (this.addedToWorld) {
            return this.getItem();
        } else {
            this.addedToWorld = true;
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
            if (sq != null) {
                String item = this.getTrackItem();
                if (StringUtils.isNullOrEmpty(item)) {
                    return null;
                } else {
                    this.setItem(sq.AddWorldInventoryItem(InventoryItemFactory.CreateItem(item), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F));
                    this.getItem().setAnimalTracks(this);
                    return this.getItem();
                }
            } else {
                return null;
            }
        }
    }

    public ArrayList<IsoAnimalTrack> getAllIsoTracks() {
        if (this.getSquare() == null) {
            return null;
        } else {
            ArrayList<IsoAnimalTrack> result = new ArrayList<>();

            for (int x2 = this.x - 5; x2 < this.x + 6; x2++) {
                for (int y2 = this.y - 5; y2 < this.y + 6; y2++) {
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.getSquare().z);
                    if (sq != null) {
                        result.add(sq.getAnimalTrack());
                    }
                }
            }

            return result;
        }
    }

    public ArrayList<IsoAnimalTrack> addToWorld() {
        if (this.addedToWorld) {
            return this.getAllIsoTracks();
        } else {
            this.addedToWorld = true;
            ArrayList<IsoAnimalTrack> result = new ArrayList<>();
            if (this.dir != null) {
                int x1 = 0;
                int x2 = 0;
                int y1 = 0;
                int y2 = 0;
                if (this.dir == IsoDirections.N || this.dir == IsoDirections.S) {
                    x1 = this.x - 1;
                    x2 = this.x + 1;
                    y1 = this.y - 5;
                    y2 = this.y + 5;
                }

                if (this.dir == IsoDirections.W || this.dir == IsoDirections.E) {
                    x1 = this.x - 1;
                    x2 = this.x + 1;
                    y1 = this.y - 5;
                    y2 = this.y + 5;
                }

                if (this.dir == IsoDirections.NE || this.dir == IsoDirections.SE) {
                    y1 = this.y - 4;
                    y2 = this.y + 4;
                    x1 = this.x - 2;
                    x2 = this.x + 2;
                }

                if (this.dir == IsoDirections.NW || this.dir == IsoDirections.SW) {
                    x1 = this.x - 4;
                    x2 = this.x + 4;
                    y1 = this.y - 2;
                    y2 = this.y + 2;
                }

                int added = 0;

                for (int x3 = x1; x3 < x2 + 1; x3++) {
                    for (int y3 = y1; y3 < y2 + 1; y3++) {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x3, y3, 0);
                        if (sq != null) {
                            String sprite = this.getTrackSprite();
                            if (sprite != null && (added == 0 || Rand.NextBool(6))) {
                                added++;
                                result.add(new IsoAnimalTrack(sq, sprite, this));
                                if (GameServer.server) {
                                    INetworkPacket.sendToRelative(PacketTypes.PacketType.AddTrack, sq.x, sq.y, sq, this);
                                }
                            }
                        }
                    }
                }
            } else {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
                if (sq != null) {
                    String sprite = this.getTrackSprite();
                    if (sprite != null) {
                        result.add(new IsoAnimalTrack(sq, sprite, this));
                    }

                    if (GameServer.server) {
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.AddTrack, sq.x, sq.y, sq, this);
                    }
                }
            }

            return result;
        }
    }

    public IsoAnimalTrack getIsoAnimalTrack() {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
        if (sq == null) {
            return null;
        } else {
            for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
                if (sq.getSpecialObjects().get(i) instanceof IsoAnimalTrack) {
                    return (IsoAnimalTrack)sq.getSpecialObjects().get(i);
                }
            }

            return null;
        }
    }

    public String getFreshnessString(int trackingLevel) {
        int hour = this.getTrackHours();
        if (trackingLevel > 6) {
            if (hour < 12) {
                return Translator.getText("IGUI_AnimalTracks_Time_12Hours");
            } else if (hour < 24) {
                return Translator.getText("IGUI_AnimalTracks_Time_24Hours");
            } else if (hour < 48) {
                return Translator.getText("IGUI_AnimalTracks_Time_2Days");
            } else if (hour < 72) {
                return Translator.getText("IGUI_AnimalTracks_Time_3Days");
            } else {
                return hour < 96 ? Translator.getText("IGUI_AnimalTracks_Time_4Days") : Translator.getText("IGUI_AnimalTracks_Time_5Days");
            }
        } else if (trackingLevel > 3) {
            if (hour < 24) {
                return Translator.getText("IGUI_AnimalTracks_Time_VeryRecent");
            } else if (hour < 48) {
                return Translator.getText("IGUI_AnimalTracks_Time_Recent");
            } else {
                return hour < 72 ? Translator.getText("IGUI_AnimalTracks_Time_SomeDays") : Translator.getText("IGUI_AnimalTracks_Time_Old");
            }
        } else if (hour < 24) {
            return Translator.getText("IGUI_AnimalTracks_Time_Recent");
        } else {
            return hour < 72 ? Translator.getText("IGUI_AnimalTracks_Time_SomeDays") : Translator.getText("IGUI_AnimalTracks_Time_Old");
        }
    }

    public int getTrackAgeDays() {
        return PZMath.fastfloor((float)((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L)) / 1440;
    }

    public int getTrackHours() {
        return PZMath.fastfloor((float)((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L)) / 60;
    }

    public boolean isItem() {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
        return track == null ? false : !StringUtils.isNullOrEmpty(track.item);
    }

    public IsoGridSquare getSquare() {
        return IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
    }

    public String getTimestamp() {
        return "Fresh";
    }

    public String getAnimalType() {
        return this.animalType;
    }

    public InventoryItem getItem() {
        return this.item;
    }

    public void setItem(InventoryItem item) {
        this.item = item;
    }
}
