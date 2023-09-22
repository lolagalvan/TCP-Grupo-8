import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class ClienteHiloE implements Runnable {

    private Socket clienteSocket;

    private PublicKey serverPublicKey;
    private PrivateKey privateKey;

    public ClienteHiloE(Socket clienteSocket, PublicKey publicKey, PrivateKey privateKey) {
        this.clienteSocket = clienteSocket;
        this.serverPublicKey = publicKey;
        this.privateKey = privateKey;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(clienteSocket.getOutputStream());

            BufferedReader entradaUsuario = new BufferedReader(new InputStreamReader(System.in));

            String mensaje;

            byte[] encriptadoPublicaServer;
            byte[] hasheado;

            while (true) {
                mensaje = entradaUsuario.readLine();

                encriptadoPublicaServer = encriptarMensaje(mensaje, serverPublicKey);
                hasheado = hashearMensaje(mensaje, privateKey);
                Mensaje mensajeCompleto = new Mensaje(encriptadoPublicaServer, hasheado);

                outputStream.writeObject(mensajeCompleto);
                if (mensaje.equalsIgnoreCase("fin")) {
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public byte[] encriptarMensaje(String mensaje, PublicKey publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] mensajeBytes = mensaje.getBytes(StandardCharsets.UTF_8);
        byte[] encriptado = encryptCipher.doFinal(mensajeBytes);

        return encriptado;
    }

    public byte[] hashearMensaje(String mensaje, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hasheado = digest.digest(mensaje.getBytes(StandardCharsets.UTF_8));

        byte[] encriptado = encryptCipher.doFinal(hasheado);

        return encriptado;
    }
}
