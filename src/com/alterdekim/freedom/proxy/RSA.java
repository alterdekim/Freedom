package com.alterdekim.freedom.proxy;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public class RSA {
    public static RSAKeyPair generateRSA() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(512);
            KeyPair pair = generator.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();
            return new RSAKeyPair(Base58.encode(privateKey.getEncoded()),
                    Base58.encode(publicKey.getEncoded()));
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encrypt(String data, String publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(1, RSA.getPublicKey(publicKey));
        return cipher.doFinal(data.getBytes());
    }

    public static String decrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(2, privateKey);
        return new String(cipher.doFinal(data));
    }

    private static PublicKey getPublicKey(String base64PublicKey) {
        PublicKey publicKey = null;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base58.decode(base64PublicKey));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        }
        catch (Exception e) {
            e.printStackTrace();
            return publicKey;
        }
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


    private static PrivateKey getPrivateKey(String base64PrivateKey) {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base58.decode(base64PrivateKey));
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    public static String RSADecode(String data, String base64PrivateKey) throws Exception {
        return RSA.decrypt(Base58.decode(data), RSA.getPrivateKey(base64PrivateKey));
    }

    public static String RSAEncode(String str, String key) throws Exception {
        return Base58.encode(RSA.encrypt(str, key));
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