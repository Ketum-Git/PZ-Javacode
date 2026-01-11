// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

public class NetworkUsers {
    public static NetworkUsers instance = new NetworkUsers();
    public ArrayList<NetworkUser> users = new ArrayList<>();

    public ArrayList<NetworkUser> getUsers() {
        return this.users;
    }

    public NetworkUser getUser(String username) {
        for (NetworkUser user : this.users) {
            if (username.equals(user.username)) {
                return user;
            }
        }

        return null;
    }

    public static void send(ByteBuffer output, Collection<NetworkUser> usersInt) {
        output.putInt(usersInt.size());

        for (NetworkUser user : usersInt) {
            user.send(output);
        }
    }

    public void parse(ByteBuffer input) {
        this.users.clear();
        int count = input.getInt();

        for (int i = 0; i < count; i++) {
            NetworkUser user = new NetworkUser();
            user.parse(input);
            this.users.add(user);
        }
    }
}
