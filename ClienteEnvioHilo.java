import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClienteEnvioHilo implements Runnable {

    private Socket socketCliente;

    public ClienteEnvioHilo(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    @Override
    public void run() {
        try {
            PrintWriter salida = new PrintWriter(socketCliente.getOutputStream(), true);
            BufferedReader entradaUsuario = new BufferedReader(new InputStreamReader(System.in));
            String mensaje;
            while (true) {
                System.out.println("Ingrese un mensaje para enviar al servidor: ");
                mensaje = entradaUsuario.readLine();
                salida.println(mensaje);
                if (mensaje.equalsIgnoreCase("fin")) {
                    break;
                }
            }
            salida.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}