import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerManager {
    public static void main(String[] args) throws IOException, NotImplementedException {
        try (ServerSocket servSc = new ServerSocket(8082)) {
            while (true) {
                int architectureType;
                int queryPerClient;
                int clientsNum;
                Server serv;
                try (Socket sc = servSc.accept();
                     InputStream is = sc.getInputStream(); OutputStream os = sc.getOutputStream()) {
                    architectureType = is.read();
                    queryPerClient = is.read();
                    clientsNum = is.read();
                    if (architectureType == 0) {
                        serv = new SingleThreadForEvery(queryPerClient, clientsNum);
                    } else if (architectureType == 1) {
                        serv = new OneReadThPoolSTPE(queryPerClient, clientsNum);
                    } else if (architectureType == 2) {
                        serv = new SelectorsThPool(queryPerClient, clientsNum);
                    } else {
                        throw new NotImplementedException();
                    }
                    os.write(new byte[]{1});
                    serv.worker();
                }
            }
        }
    }
}
