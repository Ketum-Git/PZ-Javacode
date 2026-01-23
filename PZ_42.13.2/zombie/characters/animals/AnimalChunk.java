// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.popman.ObjectPool;

@UsedFromLua
public final class AnimalChunk {
    int x;
    int y;
    final ArrayList<VirtualAnimal> animals = new ArrayList<>();
    public AnimalCell cell;
    public final ArrayList<AnimalTracks> animalTracks = new ArrayList<>();
    float tracksUpdateTimer;
    static final ObjectPool<AnimalChunk> pool = new ObjectPool<>(AnimalChunk::new);

    AnimalChunk init(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    void save(ByteBuffer output) throws IOException {
        output.putShort((short)this.animals.size());

        for (int i = 0; i < this.animals.size(); i++) {
            VirtualAnimal animal = this.animals.get(i);
            animal.save(output);
        }

        output.putShort((short)this.animalTracks.size());

        for (int i = 0; i < this.animalTracks.size(); i++) {
            AnimalTracks track = this.animalTracks.get(i);
            track.save(output);
        }
    }

    public void updateTracks() {
        if (!this.animalTracks.isEmpty()) {
            if (this.tracksUpdateTimer <= 0.0F) {
                this.tracksUpdateTimer = 5000.0F;

                for (int i = 0; i < this.animalTracks.size(); i++) {
                    AnimalTracks track = this.animalTracks.get(i);
                    if (track.getTrackAgeDays() >= 4) {
                        this.animalTracks.remove(i);
                        i--;
                    }
                }
            }

            this.tracksUpdateTimer = this.tracksUpdateTimer - GameTime.getInstance().getMultiplier();
        }
    }

    void save(ByteBuffer output, ArrayList<VirtualAnimal> realAnimals) throws IOException {
        output.putShort((short)(this.animals.size() + realAnimals.size()));

        for (int i = 0; i < this.animals.size(); i++) {
            VirtualAnimal animal = this.animals.get(i);
            animal.save(output);
        }

        for (int i = 0; i < realAnimals.size(); i++) {
            VirtualAnimal animal = realAnimals.get(i);
            animal.save(output);
        }

        output.putShort((short)this.animalTracks.size());

        for (int i = 0; i < this.animalTracks.size(); i++) {
            AnimalTracks track = this.animalTracks.get(i);
            track.save(output);
        }
    }

    void load(ByteBuffer input, int WorldVersion) throws IOException {
        int count = input.getShort();

        for (int i = 0; i < count; i++) {
            VirtualAnimal animal = new VirtualAnimal();
            animal.load(input, WorldVersion);
            MigrationGroupDefinitions.initValueFromDef(animal);
            this.animals.add(animal);

            for (int j = 0; j < animal.animals.size(); j++) {
                IsoAnimal isoAnimal = animal.animals.get(j);
                if (isoAnimal.attachBackToMother > 0) {
                    if (this.cell.animalListToReattach == null) {
                        this.cell.animalListToReattach = new ArrayList<>();
                    }

                    this.cell.animalListToReattach.add(isoAnimal);
                }
            }
        }

        int var8 = input.getShort();

        for (int i = 0; i < var8; i++) {
            AnimalTracks track = new AnimalTracks();
            track.load(input, WorldVersion);
            this.animalTracks.add(track);
        }

        if (!this.animals.isEmpty() || !this.animalTracks.isEmpty()) {
            AnimalZones.addAnimalChunk(this);
        }
    }

    public ArrayList<VirtualAnimal> getVirtualAnimals() {
        return this.animals;
    }

    public ArrayList<AnimalTracks> getAnimalsTracks() {
        return this.animalTracks;
    }

    public void deleteTracks() {
        if (this.animals.isEmpty() && !this.animalTracks.isEmpty()) {
            AnimalZones.removeAnimalChunk(this);
        }

        this.animalTracks.clear();
    }

    public void addTracksStr(VirtualAnimal animal, String trackType) {
        AnimalTracksDefinitions.AnimalTracksType track = AnimalTracksDefinitions.tracksDefinitions.get(animal.migrationGroup).tracks.get(trackType);
        this.addTracks(animal, track);
    }

    public void addTracks(VirtualAnimal animal, AnimalTracksDefinitions.AnimalTracksType trackType) {
        float closestZoneDist = AnimalZones.getClosestZoneDist(animal.x, animal.y);
        if (closestZoneDist != -1.0F && !(closestZoneDist > 20.0F)) {
            this.animalTracks.add(AnimalTracks.addAnimalTrack(animal, trackType));
            if (this.animals.size() + this.animalTracks.size() == 1) {
                AnimalZones.addAnimalChunk(this);
            }
        }
    }

    public VirtualAnimal findAnimalByID(double id) {
        if (id == 0.0) {
            return null;
        } else {
            for (int i = 0; i < this.animals.size(); i++) {
                VirtualAnimal virtualAnimal = this.animals.get(i);
                if (virtualAnimal.id != 0.0 && virtualAnimal.id == id) {
                    return virtualAnimal;
                }
            }

            return null;
        }
    }

    static AnimalChunk alloc() {
        return pool.alloc();
    }

    void release() {
        if (this.animals.size() + this.animalTracks.size() > 0) {
            AnimalZones.removeAnimalChunk(this);
        }

        this.animals.clear();
        this.animalTracks.clear();
        pool.release(this);
    }
}
