import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class Servidor {

    private HashMap<Socket, PublicKey> clientes;
    private static Llaves llaves;
    private SecretKeySpec claveSimetrica;

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

    public String encriptar(String datos) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKey = this.claveSimetrica;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] datosEncriptar = datos.getBytes("UTF-8");
        byte[] bytesEncriptados = cipher.doFinal(datosEncriptar);
        String encriptado = Base64.getEncoder().encodeToString(bytesEncriptados);
        return encriptado;
    }
    public String desencriptar(String datosEncriptados) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKey = this.claveSimetrica;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] bytesEncriptados = Base64.getDecoder().decode(datosEncriptados);
        byte[] datosDesencriptados = cipher.doFinal(bytesEncriptados);
        String datos = new String(datosDesencriptados);

        return datos;
    }
    private static SecretKeySpec crearClave(String clave) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        byte[] claveEncriptacion = clave.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        claveEncriptacion = sha.digest(claveEncriptacion);
        claveEncriptacion = Arrays.copyOf(claveEncriptacion, 16);
        SecretKeySpec secretKey = new SecretKeySpec(claveEncriptacion, "AES");
        return secretKey;
    }
    public static String claveAString(SecretKeySpec claveSimetrica) throws NoSuchAlgorithmException {
        byte[] rawData = claveSimetrica.getEncoded();
        String encodedKey = Base64.getEncoder().encodeToString(rawData);
        return encodedKey;
    }


    public void mandarClaveSimetrica(Socket socket, PublicKey claveCliente) throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, InvalidKeyException, NoSuchProviderException {
        byte[] buffer;
        String claveString= claveAString(this.claveSimetrica);
        buffer=encriptarMensaje(claveString, claveCliente);
        //OutputStream outputStream = socket.getOutputStream();
        byte[] encriptadoPublicaServer;
        byte[] hasheado;
        encriptadoPublicaServer = encriptarMensaje(claveString, claveCliente);
        hasheado = hashearMensaje(claveString, llaves.getPrivateKey());
        Mensaje mensajeCompleto = new Mensaje(encriptadoPublicaServer, hasheado);


        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(mensajeCompleto);

    }


    public Servidor() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        clientes = new HashMap<>();
        this.claveSimetrica= crearClave("clave secreta");
    }

    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Servidor iniciado en el puerto " + port);



            while (true) {
                Socket Csocket = serverSocket.accept();
                PublicKey publicaCliente = null;

                enviarKey(Csocket, llaves.getPublicKey());
                publicaCliente = recibirKey(Csocket);
                mandarClaveSimetrica(Csocket, publicaCliente);



                clientes.put(Csocket, publicaCliente);
                System.out.println("Nuevo cliente conectado: " + Csocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(Csocket);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();


            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }



    public void enviarKey(Socket socket, PublicKey publicaServer) throws IOException {
        byte[] publicKeyBytes = llaves.getPublicKey().getEncoded();
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(publicKeyBytes);
    }

    public PublicKey recibirKey(Socket cliente) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream inputStream = cliente.getInputStream();
        byte[] publicKeyBytes = new byte[2048];
        inputStream.read(publicKeyBytes);


        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public String verificarMensaje(Mensaje mensaje, Socket cliente) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        Cipher desencriptar = Cipher.getInstance("RSA");
        desencriptar.init(Cipher.DECRYPT_MODE, llaves.getPrivateKey());

        Cipher desencriptar2 = Cipher.getInstance("RSA");
        desencriptar2.init(Cipher.DECRYPT_MODE, clientes.get(cliente));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] desencriptadoByte = desencriptar.doFinal(mensaje.getEncriptado());
        String desencriptado = new String(desencriptadoByte, StandardCharsets.UTF_8);

        byte[] hasheadoByte = desencriptar2.doFinal(mensaje.getHasheado());
        String hasheado = new String(hasheadoByte, StandardCharsets.UTF_8);

        byte[] desencriptadoHasheadoByte = digest.digest(desencriptado.getBytes(StandardCharsets.UTF_8));
        String desencriptadoHasheado = new String(desencriptadoHasheadoByte, StandardCharsets.UTF_8);

        if (hasheado.equals(desencriptadoHasheado)){
            System.out.println("Mensaje recibido: " + new String(desencriptado));
            return desencriptado;
        }
        return null;
    }

    public void broadcastMessage(String mensaje, InetAddress ipEnvio) {
        byte[] encriptadoPublicaCliente;
        byte[] hasheado;
        for (Map.Entry<Socket, PublicKey> cliente: clientes.entrySet()) {
            try {
                if (cliente.getKey().getInetAddress() != ipEnvio){
                    encriptadoPublicaCliente = encriptarMensaje(mensaje, cliente.getValue());
                    hasheado = hashearMensaje(mensaje, llaves.getPrivateKey());
                    Mensaje entero = new Mensaje(encriptadoPublicaCliente, hasheado);

                    ObjectOutputStream outputStream = new ObjectOutputStream(cliente.getKey().getOutputStream());
                    outputStream.writeObject(entero);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket Csocket;

        public ClientHandler(Socket Csocket) {
            this.Csocket = Csocket;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(Csocket.getInputStream());

                while (true) {

                    Object object = inputStream.readObject();
                    Mensaje recibido = (Mensaje) object;
                    String autenticacion;
                    autenticacion = verificarMensajeEncriptado(recibido, Csocket);

                    if (autenticacion != null){
                        broadcastMessage(autenticacion, Csocket.getInetAddress());
                    } else{
                        break;
                    }

                }

                clientes.remove(Csocket);
                System.out.println("Cliente desconectado: " + Csocket.getInetAddress().getHostAddress());
                Csocket.close();


            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] encriptarMensaje(String mensaje, PublicKey publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher encriptar = Cipher.getInstance("RSA");
        encriptar.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] mensajeBytes = mensaje.getBytes(StandardCharsets.UTF_8);
        byte[] encriptado = encriptar.doFinal(mensajeBytes);

        return encriptado;
    }

    public byte[] hashearMensaje(String mensaje, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher encriptar = Cipher.getInstance("RSA");
        encriptar.init(Cipher.ENCRYPT_MODE, privateKey);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hasheado = digest.digest(mensaje.getBytes(StandardCharsets.UTF_8));

        byte[] encriptado = encriptar.doFinal(hasheado);

        return encriptado;
    }

    public String verificarMensajeEncriptado(Mensaje mensaje, Socket cliente) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {

        SecretKeySpec secretKey = this.claveSimetrica;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        //byte[] bytesEncriptados = Base64.getDecoder().decode(mensaje.getEncriptado());
        //byte[] datosDesencriptados = cipher.doFinal(mensaje.getEncriptado());
        //String datos = new String(datosDesencriptados);

        Cipher desencriptar2 = Cipher.getInstance("RSA");
        desencriptar2.init(Cipher.DECRYPT_MODE, clientes.get(cliente));

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
        return desencriptado;
    }

    public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Servidor server = new Servidor();
        server.start(2106);
    }
}
