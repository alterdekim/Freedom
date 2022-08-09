package com.alterdekim.freedom.proxy;

public class ECCKeyPair {
    private String privateKey;
    private String publicKey;

    public ECCKeyPair( String privateKey, String publicKey ) {
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
