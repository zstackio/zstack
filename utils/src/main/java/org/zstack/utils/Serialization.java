package org.zstack.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.util.Pool;

import java.util.ArrayList;
import java.util.List;

public class Serialization {
    private static int SIZE = 30;
    private static Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, SIZE) {
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setDefaultSerializer(FieldSerializer.class);
            return kryo;
        }
    };

    private static void fulfillPool() {
        List<Kryo> kryos = new ArrayList<>();
        for (int i=0; i<SIZE; i++) {
            Kryo kryo = kryoPool.obtain();
            kryos.add(kryo);
        }

        kryos.forEach(k -> kryoPool.free(k));
    }

    static {
        fulfillPool();
    }

    public static byte[] serialize(Object obj) {
        Kryo kryo = kryoPool.obtain();
        try {
            Output output = new Output(1024, -1);
            kryo.writeClassAndObject(output, obj);
            return output.getBuffer();
        } finally {
            kryoPool.free(kryo);
        }
    }

    public static <T> T deserialize(byte[] bytes) {
        Kryo kryo = kryoPool.obtain();
        try {
            Input input = new Input(bytes);
            return (T) kryo.readClassAndObject(input);
        } finally {
            kryoPool.free(kryo);
        }
    }
}
