package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

/**
 * RTMPE support added based on the public spec created by http://lkcl.net
 * available at http://lkcl.net/rtmp/RTMPE.txt
 * thanks also to the detailed breakdown of the handshake created by the
 * crtmpserver project http://www.rtmpd.com
 * available at http://www.rtmpd.com/export/425/trunk/docs/RTMPEHandshake.pdf
 *
 * @author Peter Thomas
 * @author ntoskrnl
 */
class Handshake {

    private static final Logger logger = Logger.getLogger(Handshake.class.getName());

    private static final int HANDSHAKE_SIZE = 1536;

    /**
     * SHA 256 digest length
     */
    private static final int SHA256_LEN = 32;

    private static final byte[] SERVER_CONST = "Genuine Adobe Flash Media Server 001".getBytes();

    public static final byte[] CLIENT_CONST = "Genuine Adobe Flash Player 001".getBytes();

    private static final byte[] RANDOM_CRUD = Utils.fromHex(
            "F0EEC24A8068BEE82E00D0D1029E7E576EEC5D2D29806FAB93B8E636CFEB31AE"
    );

    private static final byte[] SERVER_CONST_CRUD = concat(SERVER_CONST, RANDOM_CRUD);

    private static final byte[] CLIENT_CONST_CRUD = concat(CLIENT_CONST, RANDOM_CRUD);

    private static final byte[] DH_MODULUS_BYTES = Utils.fromHex(
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74"
                    + "020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F1437"
                    + "4FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
                    + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF"
    );

    private static final BigInteger DH_MODULUS = new BigInteger(1, DH_MODULUS_BYTES);

    private static final BigInteger DH_BASE = BigInteger.valueOf(2);

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private static int calculateOffset(byte[] pointer, int modulus, int increment) {
        if (pointer.length != 4) {
            throw new RuntimeException("bad pointer length, should be 4 but is: " + pointer.length);
        }
        int offset = 0;
        // sum the 4 bytes of the pointer
        for (byte b : pointer) {
            offset += b & 0xff;
        }
        offset %= modulus;
        offset += increment;
        return offset;
    }

    private static byte[] getFourBytesFrom(IoBuffer buf, int offset) {
        int initial = buf.position();
        buf.position(offset);
        byte[] bytes = new byte[4];
        buf.get(bytes);
        buf.position(initial);
        return bytes;
    }

    private static KeyPair generateKeyPair(RtmpSession session) {
        DHParameterSpec keySpec = new DHParameterSpec(DH_MODULUS, DH_BASE);
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            keyGen.initialize(keySpec);
            KeyPair keyPair = keyGen.generateKeyPair();
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate());
            session.setKeyAgreement(keyAgreement);
            return keyPair;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getPublicKey(KeyPair keyPair) {
        DHPublicKey publicKey = (DHPublicKey) keyPair.getPublic();
        BigInteger dh_Y = publicKey.getY();
        byte[] result = dh_Y.toByteArray();
        logger.finest("public key as bytes, len = [" + result.length + "]: " + Utils.toHex(result));
        byte[] temp = new byte[128];
        if (result.length < 128) {
            System.arraycopy(result, 0, temp, 128 - result.length, result.length);
            result = temp;
            logger.finest("padded public key length to 128");
        } else if (result.length > 128) {
            System.arraycopy(result, result.length - 128, temp, 0, 128);
            result = temp;
            logger.finest("truncated public key length to 128");
        }
        return result;
    }

    private static byte[] getSharedSecret(byte[] otherPublicKeyBytes, KeyAgreement keyAgreement) {
        BigInteger otherPublicKeyInt = new BigInteger(1, otherPublicKeyBytes);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            KeySpec otherPublicKeySpec = new DHPublicKeySpec(otherPublicKeyInt, DH_MODULUS, DH_BASE);
            PublicKey otherPublicKey = keyFactory.generatePublic(otherPublicKeySpec);
            keyAgreement.doPhase(otherPublicKey, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] sharedSecret = keyAgreement.generateSecret();
        logger.finest("shared secret (" + sharedSecret.length + " bytes): " + Utils.toHex(sharedSecret));
        return sharedSecret;
    }

    private IoBuffer data;

    public IoBuffer getData() {
        return data;
    }

    public static Handshake generateClientRequest1(RtmpSession session) {
        IoBuffer buf = IoBuffer.allocate(HANDSHAKE_SIZE);
        Utils.writeInt32Reverse(buf, (int) System.currentTimeMillis() & 0x7FFFFFFF);
        if (session.isEncrypted()) {
            buf.put(new byte[]{(byte) 128, (byte) 0, (byte) 3, (byte) 2});
        } else {
            buf.put(new byte[]{(byte) 10, (byte) 0, (byte) 45, (byte) 2});
        }
        byte[] randomBytes = new byte[HANDSHAKE_SIZE - 8]; // 4 + 4 bytes [time, version] done already
        Random random = new Random();
        random.nextBytes(randomBytes);
        buf.put(randomBytes);
        buf.flip();
        if (session.isEncrypted()) {
            logger.fine("creating client handshake part 1 for encryption");
            KeyPair keyPair = generateKeyPair(session);
            byte[] clientPublicKey = getPublicKey(keyPair);
            byte[] dhPointer = getFourBytesFrom(buf, HANDSHAKE_SIZE - 4);
            int dhOffset = calculateOffset(dhPointer, 632, 772);
            buf.position(dhOffset);
            buf.put(clientPublicKey);
            session.setClientPublicKey(clientPublicKey);
            logger.finest("client public key: " + Utils.toHex(clientPublicKey));

            byte[] digestPointer = getFourBytesFrom(buf, 8);
            int digestOffset = calculateOffset(digestPointer, 728, 12);
            buf.rewind();
            int messageLength = HANDSHAKE_SIZE - SHA256_LEN;
            byte[] message = new byte[messageLength];
            buf.get(message, 0, digestOffset);
            int afterDigestOffset = digestOffset + SHA256_LEN;
            buf.position(afterDigestOffset);
            buf.get(message, digestOffset, HANDSHAKE_SIZE - afterDigestOffset);
            byte[] digest = Utils.sha256(message, CLIENT_CONST);
            buf.position(digestOffset);
            buf.put(digest);
            buf.rewind();
            session.setClientDigest(digest);
        }

        Handshake hs = new Handshake();
        hs.data = IoBuffer.allocate(HANDSHAKE_SIZE + 1);
        if (session.isEncrypted()) {
            hs.data.put((byte) 0x06);
        } else {
            hs.data.put((byte) 0x03);
        }
        hs.data.put(buf);
        hs.data.flip();
        return hs;
    }

    public static boolean decodeServerResponse(IoBuffer in, RtmpSession session) {
        if (in.remaining() < 1 + HANDSHAKE_SIZE + HANDSHAKE_SIZE) {
            return false;
        }
        byte[] serverResponse = new byte[1 + HANDSHAKE_SIZE + HANDSHAKE_SIZE];
        in.get(serverResponse);
        session.setServerResponse(serverResponse);

        int type = serverResponse[0];
        logger.finest("Server sent RTMP type " + type);
        // 3 - RTMP
        // 6 - RTMPE
        // 8 - FPv10 RTMPE
        if (type != 3 && type != 6 && type != 8) {
            throw new RuntimeException("RTMP type " + type + " not supported");
        }
        if (!session.isEncrypted() && (type == 6 || type == 8)) {
            logger.warning("Server wants RTMPE");
            session.setEncrypted(true);
        }
        if (type == 8) {
            session.setType8(true);
        }
        if (session.isEncrypted() && type == 3) {
            logger.warning("Server refused encryption");
            session.setEncrypted(false);
        }

        IoBuffer partOne = IoBuffer.allocate(HANDSHAKE_SIZE);
        partOne.put(serverResponse, 1, HANDSHAKE_SIZE);
        partOne.flip();
        logger.finest("server response part 1: " + partOne);

        if (session.isEncrypted()) {
            logger.fine("processing server response for encryption");
            byte[] serverTime = new byte[4];
            partOne.get(serverTime);
            logger.finest("server time: " + Utils.toHex(serverTime));

            byte[] serverVersion = new byte[4];
            partOne.get(serverVersion);
            logger.finest("server version: " + Utils.toHex(serverVersion));

            byte[] digestPointer = new byte[4]; // position 8
            partOne.get(digestPointer);
            int digestOffset = calculateOffset(digestPointer, 728, 12);
            partOne.rewind();

            int messageLength = HANDSHAKE_SIZE - SHA256_LEN;
            byte[] message = new byte[messageLength];
            partOne.get(message, 0, digestOffset);
            int afterDigestOffset = digestOffset + SHA256_LEN;
            partOne.position(afterDigestOffset);
            partOne.get(message, digestOffset, HANDSHAKE_SIZE - afterDigestOffset);
            byte[] digest = Utils.sha256(message, SERVER_CONST);
            byte[] serverDigest = new byte[SHA256_LEN];
            partOne.position(digestOffset);
            partOne.get(serverDigest);

            byte[] serverPublicKey = new byte[128];
            if (Arrays.equals(digest, serverDigest)) {
                logger.fine("type 1 digest comparison success");
                byte[] dhPointer = getFourBytesFrom(partOne, HANDSHAKE_SIZE - 4);
                int dhOffset = calculateOffset(dhPointer, 632, 772);
                partOne.position(dhOffset);
                partOne.get(serverPublicKey);
                session.setServerDigest(serverDigest);
            } else {
                logger.warning("type 1 digest comparison failed, trying type 2 algorithm");
                digestPointer = getFourBytesFrom(partOne, 772);
                digestOffset = calculateOffset(digestPointer, 728, 776);
                message = new byte[messageLength];
                partOne.rewind();
                partOne.get(message, 0, digestOffset);
                afterDigestOffset = digestOffset + SHA256_LEN;
                partOne.position(afterDigestOffset);
                partOne.get(message, digestOffset, HANDSHAKE_SIZE - afterDigestOffset);
                digest = Utils.sha256(message, SERVER_CONST);
                serverDigest = new byte[SHA256_LEN];
                partOne.position(digestOffset);
                partOne.get(serverDigest);
                if (Arrays.equals(digest, serverDigest)) {
                    logger.fine("type 2 digest comparison success");
                    byte[] dhPointer = getFourBytesFrom(partOne, 768);
                    int dhOffset = calculateOffset(dhPointer, 632, 8);
                    partOne.position(dhOffset);
                    partOne.get(serverPublicKey);
                    session.setServerDigest(serverDigest);
                } else {
                    throw new RuntimeException("type 2 digest comparison also failed, aborting");
                }
            }
            logger.finest("server public key: " + Utils.toHex(serverPublicKey));
            byte[] sharedSecret = getSharedSecret(serverPublicKey, session.getKeyAgreement());

            byte[] digestOut = Utils.sha256(serverPublicKey, sharedSecret);
            try {
                Cipher cipherOut = Cipher.getInstance("RC4");
                cipherOut.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(digestOut, 0, 16, "RC4"));
                session.setCipherOut(cipherOut);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            byte[] digestIn = Utils.sha256(session.getClientPublicKey(), sharedSecret);
            try {
                Cipher cipherIn = Cipher.getInstance("RC4");
                cipherIn.init(Cipher.DECRYPT_MODE, new SecretKeySpec(digestIn, 0, 16, "RC4"));
                session.setCipherIn(cipherIn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        IoBuffer partTwo = IoBuffer.allocate(HANDSHAKE_SIZE);
        partTwo.put(serverResponse, 1 + HANDSHAKE_SIZE, HANDSHAKE_SIZE);
        partTwo.flip();

        logger.finest("server response part 2: " + partTwo);

        // validate server response part 2, not really required for client, but just to show off ;)
        if (session.isEncrypted()) {
            byte[] firstFourBytes = getFourBytesFrom(partTwo, 0);
            if (Arrays.equals(new byte[]{0, 0, 0, 0}, firstFourBytes)) {
                logger.warning("server response part 2 first four bytes are zero, did handshake fail?");
            }
            byte[] message = new byte[HANDSHAKE_SIZE - SHA256_LEN];
            partTwo.get(message);
            byte[] digest = Utils.sha256(session.getClientDigest(), SERVER_CONST_CRUD);
            byte[] signature = Utils.sha256(message, digest);

            if (session.isType8()) {
                rtmpe8_crypt(digest, signature);
            }

            byte[] serverSignature = new byte[SHA256_LEN];
            partTwo.get(serverSignature);
            if (Arrays.equals(signature, serverSignature)) {
                logger.fine("server response part 2 validation success");
            } else {
                logger.warning("server response part 2 validation failed, not Flash Player v9/v10 handshake?");
            }
        }

        // swf verification
        if (session.getSwfHash() != null) {
            byte[] bytesFromServer = new byte[SHA256_LEN];
            partOne.position(HANDSHAKE_SIZE - SHA256_LEN);
            partOne.get(bytesFromServer);
            byte[] bytesFromServerHash = Utils.sha256(session.getSwfHash(), bytesFromServer);
            // construct SWF verification pong payload
            IoBuffer swfv = IoBuffer.allocate(42);
            swfv.put((byte) 0x01);
            swfv.put((byte) 0x01);
            swfv.putInt(session.getSwfSize());
            swfv.putInt(session.getSwfSize());
            swfv.put(bytesFromServerHash);
            byte[] swfvBytes = new byte[42];
            swfv.flip();
            swfv.get(swfvBytes);
            session.setSwfVerification(swfvBytes);
            logger.fine("calculated swf verification response: " + Utils.toHex(swfvBytes));
        }

        return true;
    }

    public static Handshake generateClientRequest2(RtmpSession session) {
        if (session.isEncrypted()) {
            logger.fine("creating client handshake part 2 for encryption");
            byte[] randomBytes = new byte[HANDSHAKE_SIZE];
            Random random = new Random();
            random.nextBytes(randomBytes);
            IoBuffer buf = IoBuffer.wrap(randomBytes);
            byte[] digest = Utils.sha256(session.getServerDigest(), CLIENT_CONST_CRUD);
            byte[] message = new byte[HANDSHAKE_SIZE - SHA256_LEN];
            buf.rewind();
            buf.get(message);
            byte[] signature = Utils.sha256(message, digest);

            if (session.isType8()) {
                rtmpe8_crypt(digest, signature);
            }

            buf.put(signature);
            buf.rewind();

            // update 'encoder / decoder state' for the RC4 keys
            // both parties *pretend* as if handshake part 2 (1536 bytes) was encrypted
            // effectively this hides / discards the first few bytes of encrypted session
            // which is known to increase the secure-ness of RC4
            // RC4 state is just a function of number of bytes processed so far
            // that's why we just run 1536 arbitrary bytes through the keys below
            byte[] dummyBytes = new byte[HANDSHAKE_SIZE];
            session.getCipherIn().update(dummyBytes);
            session.getCipherOut().update(dummyBytes);

            Handshake hs = new Handshake();
            hs.data = buf;
            return hs;
        } else { // return server response part 1
            IoBuffer buf = IoBuffer.allocate(HANDSHAKE_SIZE);
            buf.put(session.getServerResponse(), 1, HANDSHAKE_SIZE);
            buf.flip();
            Handshake hs = new Handshake();
            hs.data = buf;
            return hs;
        }
    }

    private final static int[][] RTMPE8_KEYS = {
            {0xbff034b2, 0x11d9081f, 0xccdfb795, 0x748de732},
            {0x086a5eb6, 0x1743090e, 0x6ef05ab8, 0xfe5a39e2},
            {0x7b10956f, 0x76ce0521, 0x2388a73a, 0x440149a1},
            {0xa943f317, 0xebf11bb2, 0xa691a5ee, 0x17f36339},
            {0x7a30e00a, 0xb529e22c, 0xa087aea5, 0xc0cb79ac},
            {0xbdce0c23, 0x2febdeff, 0x1cfaae16, 0x1123239d},
            {0x55dd3f7b, 0x77e7e62e, 0x9bb8c499, 0xc9481ee4},
            {0x407bb6b4, 0x71e89136, 0xa7aebf55, 0xca33b839},
            {0xfcf6bdc3, 0xb63c3697, 0x7ce4f825, 0x04d959b2},
            {0x28e091fd, 0x41954c4c, 0x7fb7db00, 0xe3a066f8},
            {0x57845b76, 0x4f251b03, 0x46d45bcd, 0xa2c30d29},
            {0x0acceef8, 0xda55b546, 0x03473452, 0x5863713b},
            {0xb82075dc, 0xa75f1fee, 0xd84268e8, 0xa72a44cc},
            {0x07cf6e9e, 0xa16d7b25, 0x9fa7ae6c, 0xd92f5629},
            {0xfeb1eae4, 0x8c8c3ce1, 0x4e0064a7, 0x6a387c2a},
            {0x893a9427, 0xcc3013a2, 0xf106385b, 0xa829f927}
    };

    /*
     * RTMPE type 8 uses XTEA on the regular signature
     * http://en.wikipedia.org/wiki/XTEA
     */

    private static void rtmpe8_crypt(byte[] digest, byte[] signature0) {
        IoBuffer signature = IoBuffer.wrap(signature0);
        for (int i = 0; i < SHA256_LEN; i += 8) {
            int[] v = {
                    Utils.readInt32Reverse(signature),
                    Utils.readInt32Reverse(signature)
            };
            int[] k = RTMPE8_KEYS[(digest[i] & 0xff) % 15];

            XTEA_encrypt(v, k);

            signature.position(signature.position() - 8);
            Utils.writeInt32Reverse(signature, v[0]);
            Utils.writeInt32Reverse(signature, v[1]);
        }
    }

    private final static int DELTA = 0x9E3779B9;
    private final static int NUM_ROUNDS = 32;

    private static void XTEA_encrypt(int[] v, int[] k) {
        int v0 = v[0], v1 = v[1];
        int sum = 0;
        for (int i = 0; i < NUM_ROUNDS; i++) {
            v0 += (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + k[sum & 3]);
            sum += DELTA;
            v1 += (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + k[(sum >>> 11) & 3]);
        }
        v[0] = v0;
        v[1] = v1;
    }

}