import java.security.PrivateKey;
import java.security.PublicKey;

public class Llaves {
    private static PublicKey publicKey ;
    private static PrivateKey privateKey;

    public Llaves(PublicKey publicKey,PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public static PublicKey getPublicKey() {
        return publicKey;
    }

    public static void setPublicKey(PublicKey publicKey) {
        Llaves.publicKey = publicKey;
    }

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static void setPrivateKey(PrivateKey privateKey) {
        Llaves.privateKey = privateKey;
    }
}