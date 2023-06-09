package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Network extends OptionGroup {
   public final Client Client;
   public final Server Server;
   public final PublicServerUtil PublicServerUtil;

   public Network() {
      super("Network");
      this.Client = new Client(this.Group);
      this.Server = new Server(this.Group);
      this.PublicServerUtil = new PublicServerUtil(this.Group);
   }

   public final class Client extends OptionGroup {
      public final BooleanDebugOption MainLoop;
      public final BooleanDebugOption UpdateZombiesFromPacket;
      public final BooleanDebugOption SyncIsoObject;

      public Client(IDebugOptionGroup var2) {
         super(var2, "Client");
         this.MainLoop = newDebugOnlyOption(this.Group, "MainLoop", true);
         this.UpdateZombiesFromPacket = newDebugOnlyOption(this.Group, "UpdateZombiesFromPacket", true);
         this.SyncIsoObject = newDebugOnlyOption(this.Group, "SyncIsoObject", true);
      }
   }

   public final class Server extends OptionGroup {
      public final BooleanDebugOption SyncIsoObject;

      public Server(IDebugOptionGroup var2) {
         super(var2, "Server");
         this.SyncIsoObject = newDebugOnlyOption(this.Group, "SyncIsoObject", true);
      }
   }

   public final class PublicServerUtil extends OptionGroup {
      public final BooleanDebugOption Enabled;

      public PublicServerUtil(IDebugOptionGroup var2) {
         super(var2, "PublicServerUtil");
         this.Enabled = newDebugOnlyOption(this.Group, "Enabled", true);
      }
   }
}
