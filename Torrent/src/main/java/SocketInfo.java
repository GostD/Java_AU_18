import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketInfo implements AutoCloseable {
    public SocketInfo(Socket socket) {
        this.socket = socket;
        try {
            this.is = new DataInputStream(socket.getInputStream());
            this.os = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Failed get streams for socket");
            e.printStackTrace();
        }
    }
    public SocketInfo(Socket socket, DataInputStream is, DataOutputStream os) {
        this.socket = socket;
        this.is = is;
        this.os = os;
    }
    public void close() throws IOException {
        this.os.close();
        this.is.close();
        this.socket.close();
    }
    public Socket socket;
    public DataInputStream is;
    public DataOutputStream os;
}