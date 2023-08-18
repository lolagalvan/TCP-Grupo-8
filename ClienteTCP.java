import java.net.*;

import java.io.*;



public class ClienteTCP {

    public static void main(String[] args) throws IOException {

        Socket socketCliente = null;


        try {
//crear una conexión con el servidor utilizando la dirección IP de la compu y el puerto

            socketCliente = new Socket("localhost", 2006);


        } catch (IOException e) {

            System.err.println("No puede establer canales para la conexión");

            System.exit(-1);

        }
        // Crear los hilos
        Thread hiloEscucha = new Thread(new ClienteEscuchaHilo(socketCliente));
        Thread hiloEnvio = new Thread(new ClienteEnvioHilo(socketCliente));

        // Iniciar los hilos
        hiloEscucha.start();
        hiloEnvio.start();

        try {
            // Esperar a que ambos hilos terminen
            hiloEscucha.join();
            hiloEnvio.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        socketCliente.close();

    }

}