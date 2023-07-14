import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Servidor {

    private List<Socket> clientes;

    public Servidor() {
        clientes = new ArrayList<>();
    }

    public void start(int puerto) {
        try {
            ServerSocket servidorSocket = new ServerSocket(puerto);
            System.out.println("Servidor iniciado en el puerto " + puerto);

            while (true) {
                Socket clienteSocket = servidorSocket.accept();
                clientes.add(clienteSocket);
                System.out.println("Nuevo cliente conectado: " + clienteSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clienteSocket);
                Thread hiloCliente = new Thread(clientHandler);
                hiloCliente.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMensaje(byte[] mensaje, InetAddress ipEmisor) {
        for (Socket cliente : clientes) {
            try {
                if (cliente.getInetAddress() != ipEmisor){
                    OutputStream outputStream = cliente.getOutputStream();
                    outputStream.write(mensaje);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clienteSocket;

        public ClientHandler(Socket clienteSocket) {
            this.clienteSocket = clienteSocket;
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = clienteSocket.getInputStream();

                while (true) {
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }

                    byte[] mensaje = new byte[bytesRead];
                    System.arraycopy(buffer, 0, mensaje, 0, bytesRead);
                    System.out.println("Mensaje: " + new String(mensaje));

                    broadcastMensaje(mensaje, clienteSocket.getInetAddress());
                }

                clientes.remove(clienteSocket);
                System.out.println("Cliente se desconecto: " + clienteSocket.getInetAddress().getHostAddress());
                clienteSocket.close();
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