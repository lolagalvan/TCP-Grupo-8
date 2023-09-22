import java.io.Serializable;

public class Mensaje implements Serializable {
    private byte[] encriptado;
    private byte[] hasheado;

    public Mensaje(byte[] encriptado, byte[] hasheado) {
        this.encriptado = encriptado;
        this.hasheado = hasheado;
    }

    public byte[] getEncriptado() {
        return encriptado;
    }

    public void setEncriptado(byte[] encriptado) {
        this.encriptado = encriptado;
    }

    public byte[] getHasheado() {
        return hasheado;
    }

    public void setHasheado(byte[] hasheado) {
        this.hasheado = hasheado;
    }
}