import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.net.*;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Cliente {


    private static PublicKey servidorPublicKey;

    private static Llaves llaves;

    static {
        try {
            llaves = generarLlave(2048);
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

    public static Llaves generarLlave(int size) throws NoSuchAlgorithmException,NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException  {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(size);
        KeyPair kp = kpg.genKeyPair();

        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        Llaves llaves = new Llaves(publicKey,privateKey);
        return llaves;
    }



    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Socket clienteSocket = null;

        try {
            clienteSocket = new Socket("172.16.255.170", 2106);

        } catch (IOException e) {
            System.err.println("No pudo establer la conexion");
            System.exit(-1);
        }

        servidorPublicKey = recibirKey(clienteSocket);
        enviarKey(clienteSocket, llaves.getPublicKey());

        Thread hiloEscucha = new Thread(new ClienteHiloR(clienteSocket, servidorPublicKey, llaves.getPrivateKey()));

        Thread hiloEnvio = new Thread(new ClienteHiloE(clienteSocket, servidorPublicKey, llaves.getPrivateKey()));

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

    private static PublicKey recibirKey(Socket cliente) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream inputStream = cliente.getInputStream();
        byte[] publicKeyBytes = new byte[2048];
        inputStream.read(publicKeyBytes);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static void enviarKey(Socket socket, PublicKey publicKey) throws IOException {
        byte[] publicKeyBytes = llaves.getPublicKey().getEncoded();
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(publicKeyBytes);
    }

}
