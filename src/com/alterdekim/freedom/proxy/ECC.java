package com.alterdekim.freedom.proxy;

import org.bouncycastle.jcajce.provider.asymmetric.RSA;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.util.UUID;

public class ECC {
    public static ECCKeyPair generateECC() {
        try {
            String name = "secp256r1";

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            kpg.initialize(new ECGenParameterSpec(name));

            KeyPair keyPair = kpg.generateKeyPair();
            return new ECCKeyPair(Base58.encode(keyPair.getPrivate().getEncoded()),
                    Base58.encode(keyPair.getPublic().getEncoded()));
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encrypt(String data, String publicKey) throws Exception {
        Cipher iesCipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
        iesCipher.init(Cipher.ENCRYPT_MODE, ECC.getPublicKey(publicKey));
        return iesCipher.doFinal(data.getBytes());
    }

    public static String decrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher iesCipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
        iesCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(iesCipher.doFinal(data));
    }

    private static PublicKey getPublicKey(String base64PublicKey) {
        try {
            X509EncodedKeySpec specPublic = new X509EncodedKeySpec(Base58.decode(base64PublicKey));
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PublicKey public_key12 = keyFactory.generatePublic(specPublic);
            return public_key12;
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    private static PrivateKey getPrivateKey(String base64PrivateKey) {
        try {
            PKCS8EncodedKeySpec specPublic = new PKCS8EncodedKeySpec(Base58.decode(base64PrivateKey));
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey public_key12 = keyFactory.generatePrivate(specPublic);
            return public_key12;
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    public static String SHA256( String input ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(encodedhash);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return "";
    }

    public static String SHA512( String input ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] encodedhash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(encodedhash);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return "";
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String ECCDecode(String data, String base64PrivateKey) throws Exception {
        return ECC.decrypt(Base58.decode(data), ECC.getPrivateKey(base64PrivateKey));
    }

    public static String ECCEncode(String str, String key) throws Exception {
        return Base58.encode(ECC.encrypt(str, key));
    }

    public static String generateAES() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey key = keyGenerator.generateKey();
            return Base58.encode(key.getEncoded());
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return "";
    }

    public static String AESEncode( String str, String key ) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKey secret = new SecretKeySpec(Base58.decode(key), 0, Base58.decode(key).length, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            return Base64.getMimeEncoder().withoutPadding().encodeToString(cipher.doFinal(str.getBytes()));
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return "";
    }

    public static String AESDecode( String str, String key ) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKey secret = new SecretKeySpec(Base58.decode(key), 0, Base58.decode(key).length, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);
            byte[] plainText = cipher.doFinal(Base64.getMimeDecoder().decode(str));
            return new String(plainText);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return "";
    }

    public static String generateGUID() {
        return UUID.randomUUID().toString();
    }
}