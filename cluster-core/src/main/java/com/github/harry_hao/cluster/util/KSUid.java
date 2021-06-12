package com.github.harry_hao.cluster.util;

import io.seruco.encoding.base62.Base62;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;

public class KSUid {
    private static final Random random = new SecureRandom();
    private static final Base62 base62 = Base62.createInstance();
    private static final int EPOCH = 1522602300;
    private static final int TIMESTAMP_OFFSET = 0;
    private static final int TIMESTAMP_BYTES = 4;
    private static final int PAYLOAD_OFFSET = TIMESTAMP_BYTES;
    private static final int PAYLOAD_BYTES = 16;
    private static final int TOTAL_BYTES = TIMESTAMP_BYTES + PAYLOAD_BYTES;

    private Instant timestamp;
    private byte[] payload = new byte[PAYLOAD_BYTES];

    public KSUid() {
        this.timestamp = Instant.now();
        random.nextBytes(this.payload);
    }

    private KSUid(Instant timestamp, byte[] payload) {
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public byte[] encode() {
        final int seconds = (int) (timestamp.toEpochMilli() / 1000) - EPOCH;
        return ByteBuffer.allocate(TOTAL_BYTES)
                .putInt(seconds)
                .put(payload)
                .array();
    }

    public String toString() {
        return new String(base62.encode(encode()));
    }

    public static KSUid from(String str) {
        return from(Base64.getDecoder().decode(str));
    }

    public static KSUid from(byte[] bytes) {
        int seconds = ByteBuffer.wrap(bytes, TIMESTAMP_OFFSET, TIMESTAMP_BYTES).getInt();
        Instant timestamp = Instant.ofEpochSecond(EPOCH+seconds);

        byte[] payload = new byte[PAYLOAD_BYTES];
        System.arraycopy(bytes, PAYLOAD_OFFSET, payload, 0, PAYLOAD_BYTES);

        return new KSUid(timestamp, payload);
    }
}
