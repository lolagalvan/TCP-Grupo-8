import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Cliente {


    private static PublicKey servidorPublicKey;
    private static SecretKeySpec claveSimetrica;

    private static Llaves llavesC;

    static {
        try {
            llavesC = generarLlave(2048);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public SecretKeySpec getClaveSimetrica() {
        return claveSimetrica;
    }

    public void setClaveSimetrica(SecretKeySpec claveSimetrica) {
        this.claveSimetrica = claveSimetrica;
    }

    public static Llaves generarLlave(int size) throws NoSuchAlgorithmException,NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException  {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(size);
        KeyPair kp = kpg.genKeyPair();

        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        Llaves llavesC = new Llaves(publicKey,privateKey);
        return llavesC;
    }



    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        Socket clienteSocket = null;

        try {
            clienteSocket = new Socket("127.0.0.1", 2106);

        } catch (IOException e) {
            System.err.println("No pudo establer la conexion");
            System.exit(-1);
        }

        servidorPublicKey = recibirKey(clienteSocket);
        enviarKey(clienteSocket, llavesC.getPublicKey());

        recibirClaveS(clienteSocket);
        Thread hiloEscucha = new Thread(new ClienteHiloR(clienteSocket, servidorPublicKey, llavesC.getPrivateKey(),claveSimetrica));

        Thread hiloEnvio = new Thread(new ClienteHiloE(clienteSocket, servidorPublicKey, llavesC.getPrivateKey(),claveSimetrica));

        hiloEscucha.start();
        hiloEnvio.start();

        try {
            hiloEscucha.join();
            hiloEnvio.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        clienteSocket.close();
    }


    public static String bytesToString(byte[] b) {
        byte[] b2 = new byte[b.length + 1];
        b2[0] = 1;
        System.arraycopy(b, 0, b2, 1, b.length);
        return new BigInteger(b2).toString(36);
    }


    private static PublicKey recibirKey(Socket cliente) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream inputStream = cliente.getInputStream();
        byte[] publicKeyBytes = new byte[2048];
        inputStream.read(publicKeyBytes);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static void enviarKey(Socket socket, PublicKey publicKey) throws IOException {
        byte[] publicKeyBytes = llavesC.getPublicKey().getEncoded();
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(publicKeyBytes);
    }
    private static void recibirClaveS(Socket cliente) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException {
        ObjectInputStream inputStream = new ObjectInputStream(cliente.getInputStream());
        Object object = inputStream.readObject();
        Mensaje mensajeRecibido = (Mensaje) object;

        Cipher desencriptar = Cipher.getInstance("RSA");
        desencriptar.init(Cipher.DECRYPT_MODE, llavesC.getPrivateKey());

        Cipher desencriptar2 = Cipher.getInstance("RSA");
        desencriptar2.init(Cipher.DECRYPT_MODE, servidorPublicKey);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] desencriptadoByte = desencriptar.doFinal(mensajeRecibido.getEncriptado());
        String desencriptado = new String(desencriptadoByte, StandardCharsets.UTF_8);

        byte[] hasheadoByte = desencriptar2.doFinal(mensajeRecibido.getHasheado());
        String hasheado = new String(hasheadoByte, StandardCharsets.UTF_8);

        byte[] desencriptadoHasheadoByte = digest.digest(desencriptado.getBytes(StandardCharsets.UTF_8));
        String desencriptadoHasheado = new String(desencriptadoHasheadoByte, StandardCharsets.UTF_8);
        if (hasheado.equals(desencriptadoHasheado)){
            System.out.println("llego bien");
            claveSimetrica = convertStringToSecretKey(desencriptado);

        }

    }
    public static SecretKeySpec convertStringToSecretKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        SecretKeySpec originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        return originalKey;
    }

}

