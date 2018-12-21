import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ClientCli {
    private static List<String> avaliableRequests = Arrays.asList("list", "upload", "sources", "update", "stat", "get");
    public void worker(String ip, short port) throws IOException {
        TorrentClient torrentClient = new TorrentClient(ip, port);
        BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
        String cmd = "";
        Thread th = new Thread(torrentClient::listener);
        th.start();
        while (!cmd.equals("exit")) {
            cmd = is.readLine();
            if (cmd.equals(" ") || cmd.equals("/n") || cmd.equals("") || cmd.equals("exit")) continue;
            String[] cmdParts = cmd.split(" ");
            if (!avaliableRequests.contains(cmdParts[0])) {
                System.out.println("Unknown command");
            } else {
                avaliableRequests.indexOf(cmdParts[0]);
                try {
                    Object[] args = new String[cmdParts.length - 1];
                    Class[] argCls = new Class[args.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = cmdParts[i + 1];
                        argCls[i] = String.class;
                    }
                    Method met = TorrentClient.class.getMethod(cmdParts[0], argCls);
                    met.invoke(torrentClient, args);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    System.out.println("Unknown command");
                    continue;
                }
            }
        }

        torrentClient.close();
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2)
                new ClientCli().worker("localhost", (short)8082);//address of server, port of client
            else
                new ClientCli().worker(args[0], Short.parseShort(args[1]));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
