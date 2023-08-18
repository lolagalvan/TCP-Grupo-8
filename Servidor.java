import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Servidor {

// creamos un ArrayList que almacenará las conexiones de los clientes que se conectan al servidor.

    private List<Socket> clientes;

    public Servidor() {
        clientes = new ArrayList<>();
    }

    public void start(int puerto) {
        try {
// se crea un ServerSocket que escucha en un puerto específico
            ServerSocket servidorSocket = new ServerSocket(puerto);
            System.out.println("Servidor iniciado en el puerto " + puerto);


//entra en un bucle infinito donde acepta conexiones de clientes entrantes.
            while (true) {

//Cuando un cliente se conecta, se crea una instancia de Socket y se agrega a la lista de clientes

                Socket clienteSocket = servidorSocket.accept();
                clientes.add(clienteSocket);
                System.out.println("Nuevo cliente conectado: " + clienteSocket.getInetAddress().getHostAddress());

/* Luego se inicia un hilo (Thread) llamado ClientHandler para manejar la comunicación
con ese cliente en particular. Esto permite que varios clientes se conecten
y comuniquen simultáneamente */

                ClientHandler clientHandler = new ClientHandler(clienteSocket);
                Thread hiloCliente = new Thread(clientHandler);
                hiloCliente.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void broadcastMensaje(byte[] mensaje, InetAddress ipEmisor) {
        for (Socket cliente : clientes) { // recorre la lista de clientes y envía el mensaje a todos ellos
            try {
                if (cliente.getInetAddress() != ipEmisor) {
                    OutputStream outputStream = cliente.getOutputStream();
                    outputStream.write(mensaje);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clienteSocket;// representa la conexion de un cliente

        public ClientHandler(Socket clienteSocket) {
            this.clienteSocket = clienteSocket;
        }


        @Override
        public void run() {
            try {

// se obtiene el flujo de entrada (InputStream) del cliente conectado a través del socket clienteSocket.
// Esto permitirá al servidor leer los datos enviados por el cliente.

                InputStream inputStream = clienteSocket.getInputStream();

                while (true) {
                    byte[] buffer = new byte[1024]; // creamos un buffer
                    int bytesRead = inputStream.read(buffer);//leer datos del flujo de entrada del cliente y almacenarlos en el búfer
                    if (bytesRead == -1) { //si lee -1 bytes el cliente cerro la conexion
                        break;
                    }

                    byte[] mensaje = new byte[bytesRead]; // se crea un buffer(mensaje) con el tamaño del msj
                    System.arraycopy(buffer, 0, mensaje, 0, bytesRead); //Se copian los bytes del buffer al buffer mensaje
                    System.out.println("Mensaje: " + new String(mensaje)); // convertimos msj en string y se imprime

                    broadcastMensaje(mensaje, clienteSocket.getInetAddress()); // se llama al metodo que envia el msj a tds los demás clientes
                }

                clientes.remove(clienteSocket);//se elimina el socket del cliente de la lista de clientes
                System.out.println("Cliente se desconecto: " + clienteSocket.getInetAddress().getHostAddress());
// imprime direccion IP del cliente
                clienteSocket.close();//se cierra el socket del cliente para liberar recursos
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Servidor s = new Servidor();
        s.start(2006);
    }

}