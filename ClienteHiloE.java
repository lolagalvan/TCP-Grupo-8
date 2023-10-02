import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class ClienteHiloE implements Runnable {

    private Socket clienteSocket;

    private PublicKey serverPublicKey;
    private PrivateKey privateKey;
    private SecretKeySpec claveSimetrica;

    public ClienteHiloE(Socket clienteSocket, PublicKey publicKey, PrivateKey privateKey, SecretKeySpec claveS) {
        this.clienteSocket = clienteSocket;
        this.serverPublicKey = publicKey;
        this.privateKey = privateKey;
        this.claveSimetrica = claveS;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(clienteSocket.getOutputStream());

            BufferedReader entradaUsuario = new BufferedReader(new InputStreamReader(System.in));

            String mensaje;

            byte[] encriptadoClaveS;
            byte[] hasheado;

            while (true) {
                mensaje = entradaUsuario.readLine();

                encriptadoClaveS = encriptarClaveS(mensaje);
                hasheado = hashearMensaje(mensaje, privateKey);
                Mensaje mensajeCompleto = new Mensaje(encriptadoClaveS, hasheado);

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
    public byte[] encriptarClaveS(String datos) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKey = this.claveSimetrica;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] datosEncriptar = datos.getBytes("UTF-8");
        byte[] bytesEncriptados = cipher.doFinal(datosEncriptar);
        return bytesEncriptados;
    }

}
