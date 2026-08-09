// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import zombie.debug.DebugLog;
import zombie.debug.DebugType;

public class PortMapper {
    private static String externalAddress;

    public static void startup() {
    }

    public static void shutdown() {
        _cleanup();
    }

    public static boolean discover() {
        _discover();
        return _igd_found();
    }

    public static boolean igdFound() {
        return _igd_found();
    }

    public static boolean addMapping(int wanPort, int lanPort, String description, String proto, int leaseTime) {
        return addMapping(wanPort, lanPort, description, proto, leaseTime, false);
    }

    public static boolean addMapping(int wanPort, int lanPort, String description, String proto, int leaseTime, boolean force) {
        boolean result = _add_mapping(wanPort, lanPort, description, proto, leaseTime, force);
        if (!result && leaseTime != 0) {
            DebugLog.log(DebugType.Network, "Failed to add port mapping, retrying with zero lease time");
            result = _add_mapping(wanPort, lanPort, description, proto, 0, force);
        }

        return result;
    }

    public static boolean removeMapping(int wanPort, String proto) {
        return _remove_mapping(wanPort, proto);
    }

    public static void fetchMappings() {
        _fetch_mappings();
    }

    public static int numMappings() {
        return _num_mappings();
    }

    public static PortMappingEntry getMapping(int index) {
        return _get_mapping(index);
    }

    public static String getGatewayInfo() {
        return _get_gateway_info();
    }

    public static synchronized String getExternalAddress(boolean forceUpdate) {
        if (forceUpdate || externalAddress == null) {
            externalAddress = _get_external_address();
        }

        return externalAddress;
    }

    public static String getExternalAddress() {
        return getExternalAddress(false);
    }

    private static native void _discover();

    private static native void _cleanup();

    private static native boolean _igd_found();

    private static native boolean _add_mapping(int var0, int var1, String var2, String var3, int var4, boolean var5);

    private static native boolean _remove_mapping(int var0, String var1);

    private static native void _fetch_mappings();

    private static native int _num_mappings();

    private static native PortMappingEntry _get_mapping(int var0);

    private static native String _get_gateway_info();

    private static native String _get_external_address();
}
