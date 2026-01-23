// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

class pzexe {
    public static native String findJNI();

    public static void main(String[] var0) {
        System.out.println(findJNI());
    }

    static {
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            System.out.println("pzexe.java: loading shared library \"pzexe_jni64\"");
            System.loadLibrary("pzexe_jni64");
        } else {
            System.out.println("pzexe.java: loading shared library \"pzexe_jni32\"");
            System.loadLibrary("pzexe_jni32");
        }
    }
}
