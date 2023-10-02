import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class ClienteHiloR implements Runnable {

    private Socket clienteSocket;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private SecretKeySpec claveSimetrica;

    public ClienteHiloR(Socket clienteSocket, PublicKey publicKey, PrivateKey privateKey, SecretKeySpec claveSimetrica) {
        this.clienteSocket = clienteSocket;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.claveSimetrica = claveSimetrica;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(clienteSocket.getInputStream());

            while (true) {

                Object object = inputStream.readObject();
                Mensaje mensajeRecibido = (Mensaje) object;
                verificarMensaje(mensajeRecibido, clienteSocket);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void verificarMensaje(Mensaje mensaje, Socket cliente) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {

        SecretKeySpec secretKey = this.claveSimetrica;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] bytesEncriptados = Base64.getDecoder().decode(mensaje.getEncriptado());
        byte[] datosDesencriptados = cipher.doFinal(bytesEncriptados);
        String datos = new String(datosDesencriptados);

        Cipher desencriptar2 = Cipher.getInstance("RSA");
        desencriptar2.init(Cipher.DECRYPT_MODE, this.publicKey);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] desencriptadoByte = cipher.doFinal(mensaje.getEncriptado());
        String desencriptado = new String(desencriptadoByte, StandardCharsets.UTF_8);

        byte[] hasheadoByte = desencriptar2.doFinal(mensaje.getHasheado());
        String hasheado = new String(hasheadoByte, StandardCharsets.UTF_8);

        byte[] desencriptadoHasheadoByte = digest.digest(desencriptado.getBytes(StandardCharsets.UTF_8));
        String desencriptadoHasheado = new String(desencriptadoHasheadoByte, StandardCharsets.UTF_8);

        if (hasheado.equals(desencriptadoHasheado)){
            System.out.println(new String(desencriptado));
        }
    }
}
