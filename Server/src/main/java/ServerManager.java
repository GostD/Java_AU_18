import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerManager {
    public static void main(String[] args) throws IOException, NotImplementedException {
        try (ServerSocket servSc = new ServerSocket(8082); ServerSocket servS = new ServerSocket(8081)) {//ServerSocketChannel.open().bind(new InetSocketAddress(8081)).socket()) {
            while (true) {
                int architectureType;
                int queryPerClient;
                int clientsNum;
                try (Socket sc = servSc.accept(); InputStream is = sc.getInputStream()) {
                    architectureType = is.read();
                    queryPerClient = is.read();
                    clientsNum = is.read();
                }
                Server serv;
//                System.out.println(architectureType + " " + queryPerClient + " " + clientsNum);
                if (architectureType == 0) {
                    serv = new SingleThreadForEvery(queryPerClient, clientsNum);
                } else if (architectureType == 1) {
                    serv = new OneReadThPoolSTPE(queryPerClient, clientsNum);
                } else if (architectureType == 2) {
                    serv = new SelectorsThPool(queryPerClient, clientsNum);
                } else {
                    throw new NotImplementedException();
                }
                serv.worker(servS);

            }
        }
    }
}
