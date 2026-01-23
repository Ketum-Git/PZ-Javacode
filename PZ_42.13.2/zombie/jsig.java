// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

class jsig {
    public static native String findJSIG();

    public static void main(String[] var0) {
        System.out.println(findJSIG());
    }

    static {
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            System.loadLibrary("pzexe_jni64");
        } else {
            System.loadLibrary("pzexe_jni32");
        }
    }
}
