package com.alterdekim.freedom.proxy;

public class RSAKeyPair {
    private String privateKey;
    private String publicKey;

    public RSAKeyPair( String privateKey, String publicKey ) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }
}
