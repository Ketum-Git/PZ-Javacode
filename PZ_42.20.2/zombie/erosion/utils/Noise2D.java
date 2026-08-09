// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.utils;

import java.util.ArrayList;
import java.util.Objects;

public class Noise2D {
    private final ArrayList<Noise2D.Layer> layers = new ArrayList<>(3);
    private static final int[] perm = new int[]{
        151,
        160,
        137,
        91,
        90,
        15,
        131,
        13,
        201,
        95,
        96,
        53,
        194,
        233,
        7,
        225,
        140,
        36,
        103,
        30,
        69,
        142,
        8,
        99,
        37,
        240,
        21,
        10,
        23,
        190,
        6,
        148,
        247,
        120,
        234,
        75,
        0,
        26,
        197,
        62,
        94,
        252,
        219,
        203,
        117,
        35,
        11,
        32,
        57,
        177,
        33,
        88,
        237,
        149,
        56,
        87,
        174,
        20,
        125,
        136,
        171,
        168,
        68,
        175,
        74,
        165,
        71,
        134,
        139,
        48,
        27,
        166,
        77,
        146,
        158,
        231,
        83,
        111,
        229,
        122,
        60,
        211,
        133,
        230,
        220,
        105,
        92,
        41,
        55,
        46,
        245,
        40,
        244,
        102,
        143,
        54,
        65,
        25,
        63,
        161,
        1,
        216,
        80,
        73,
        209,
        76,
        132,
        187,
        208,
        89,
        18,
        169,
        200,
        196,
        135,
        130,
        116,
        188,
        159,
        86,
        164,
        100,
        109,
        198,
        173,
        186,
        3,
        64,
        52,
        217,
        226,
        250,
        124,
        123,
        5,
        202,
        38,
        147,
        118,
        126,
        255,
        82,
        85,
        212,
        207,
        206,
        59,
        227,
        47,
        16,
        58,
        17,
        182,
        189,
        28,
        42,
        223,
        183,
        170,
        213,
        119,
        248,
        152,
        2,
        44,
        154,
        163,
        70,
        221,
        153,
        101,
        155,
        167,
        43,
        172,
        9,
        129,
        22,
        39,
        253,
        19,
        98,
        108,
        110,
        79,
        113,
        224,
        232,
        178,
        185,
        112,
        104,
        218,
        246,
        97,
        228,
        251,
        34,
        242,
        193,
        238,
        210,
        144,
        12,
        191,
        179,
        162,
        241,
        81,
        51,
        145,
        235,
        249,
        14,
        239,
        107,
        49,
        192,
        214,
        31,
        181,
        199,
        106,
        157,
        184,
        84,
        204,
        176,
        115,
        121,
        50,
        45,
        127,
        4,
        150,
        254,
        138,
        236,
        205,
        93,
        222,
        114,
        67,
        29,
        24,
        72,
        243,
        141,
        128,
        195,
        78,
        66,
        215,
        61,
        156,
        180
    };

    private float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private float fade(float t) {
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private float noise(float origX, float origY, int[] layerP) {
        int x = (int)Math.floor(origX - Math.floor(origX / 255.0F) * 255.0);
        int y = (int)Math.floor(origY - Math.floor(origY / 255.0F) * 255.0);
        float fx = this.fade(origX - (float)Math.floor(origX));
        float fy = this.fade(origY - (float)Math.floor(origY));
        int aa = layerP[x] + y;
        int ab = layerP[x] + y + 1;
        int ba = layerP[x + 1] + y;
        int bb = layerP[x + 1] + y + 1;
        return this.lerp(fy, this.lerp(fx, perm[layerP[aa]], perm[layerP[ba]]), this.lerp(fx, perm[layerP[ab]], perm[layerP[bb]]));
    }

    public float layeredNoise(float x, float y) {
        float fn = 0.0F;
        float maxSum = 0.0F;

        for (int i = 0; i < this.layers.size(); i++) {
            Noise2D.Layer layer = this.layers.get(i);
            maxSum += layer.amp;
            fn += this.noise(x * layer.freq, y * layer.freq, layer.p) * layer.amp;
        }

        return fn / maxSum / 255.0F;
    }

    public void addLayer(int seed, float freq, float amp) {
        seed = (int)Math.floor(seed - Math.floor(seed / 256.0F) * 256.0);
        Noise2D.Layer layer = new Noise2D.Layer();
        layer.freq = freq;
        layer.amp = amp;

        for (int i = 0; i < 256; i++) {
            int ii = (int)Math.floor(seed + i - Math.floor((seed + i) / 256.0F) * 256.0);
            layer.p[ii] = perm[i];
            layer.p[256 + ii] = layer.p[ii];
        }

        this.layers.add(layer);
    }

    public void reset() {
        if (!this.layers.isEmpty()) {
            this.layers.clear();
        }
    }

    private class Layer {
        public float freq;
        public float amp;
        public int[] p;

        private Layer() {
            Objects.requireNonNull(Noise2D.this);
            super();
            this.p = new int[512];
        }
    }
}
