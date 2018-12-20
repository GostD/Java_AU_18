import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TorrentTracker {

    private ServerSocket serverSocket;
    private MetaInfo metaInfo;
    private File trackedFiles;
    private int lastId;
    private static List<String> avaliableRequests = Arrays.asList("list", "upload", "sources", "update");
    private ExecutorService threadPool;

    public TorrentTracker() {
        try {
            serverSocket = new ServerSocket(8081);
        } catch (IOException e) {
            System.out.println("Could not create ServerSocket");
            e.printStackTrace();
        }
        metaInfo = new MetaInfo();
        trackedFiles = new File(".torrents");
        boolean created;
        if (!trackedFiles.exists()) {
            try {
                created = trackedFiles.createNewFile();
                if (!created)
                    throw new IOException();
            } catch (IOException e) {
                System.out.println("could not create torrent meta file");
                e.printStackTrace();
            }
            lastId = 0;
        } else {
            try {
                BufferedReader fr = new BufferedReader(new FileReader(trackedFiles));
                String line;
                while (fr.ready()) {
                    line = fr.readLine();
                    if (line.equals("")) break;
                    String[] str = line.split(" ");
                    metaInfo.addFile(Integer.parseInt(str[0]), str[1], Long.parseLong(str[2]));
                }
                fr.close();
            } catch (FileNotFoundException e) {
                System.out.println("Could not found torrent meta file");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Could not read from file");
                e.printStackTrace();
            }
            lastId = metaInfo.files.size();
        }
        threadPool = Executors.newSingleThreadExecutor();
    }

    public int addFile(String name, long size) {
        metaInfo.addFile(lastId, name, size);
        try {
            FileWriter fw = new FileWriter(trackedFiles, true);
            fw.write(lastId + " " + name + " " + size + '\n');
            fw.flush();
            fw.close();
        } catch (IOException e) {
            System.out.println("Could not write into a file");
            e.printStackTrace();
        }
        return lastId++;
    }
    public void addClient(int id, byte[] ip, short port) {
        metaInfo.addClient(id, ip, port);
    }

    public void setTimeOut(int time) {
        try {
            serverSocket.setSoTimeout(time);
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    public void listener() {

        while (true) {
            try {
                final Socket sc;
                try {
                    sc = serverSocket.accept();
                } catch (SocketTimeoutException | SocketException e) {
                    close();
                    break;
                }
                    threadPool.execute(() -> {

                        try (final SocketInfo sInf = new SocketInfo(sc)) {
                                byte metByte = sInf.is.readByte();
                                if (metByte > 4 || metByte < 1) {
                                    throw new IOException("Invalid request");
                                }
                                String strReq = avaliableRequests.get(metByte - 1);
                                Method met = TorrentTracker.class.getDeclaredMethod(strReq, SocketInfo.class);
                                met.invoke(TorrentTracker.this, sInf);
                        } catch (IllegalAccessException | InvocationTargetException | IOException | NoSuchMethodException e) {
                            System.out.println("could not call request method");
                            e.printStackTrace();
                        }
                    });
            } catch (IOException e) {
                System.out.println("could not accept connection to server socket");
                e.printStackTrace();
                close();
                break;
            }
        }
    }

    public void close() {
        threadPool.shutdown();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void list(SocketInfo sc) throws IOException {
        sc.os.writeInt(lastId);
        for (int i = 0; i < lastId; i++) {
            FileInfo fi = metaInfo.files.get(i);
            sc.os.writeInt(i);
            sc.os.writeUTF(fi.name);
            sc.os.writeLong(fi.size);
        }
        sc.os.flush();
    }


    public void upload(SocketInfo sc) throws IOException {
        String fName = sc.is.readUTF();
        long fSize = sc.is.readLong();
        int fId = addFile(fName, fSize);
        byte[] ip = sc.socket.getInetAddress().getAddress();
        if (ip.length != 4) {
            throw new IOException("Wrong ip address of client");
        }
        short port = (short) sc.socket.getPort();
        addClient(fId, ip, port);
        sc.os.writeInt(fId);
        sc.os.flush();
    }

    public void sources(SocketInfo sc) throws IOException {
            int fId = sc.is.readInt();
            Date curDate = new Date();
            Deque<TimeStampedClient> clients = metaInfo.clients.get(fId);
            while (!clients.isEmpty() && (curDate.getTime() - clients.getFirst().timeStamp.getTime()) > 300000) clients.pollFirst();
            sc.os.writeInt(clients.size());
            for (TimeStampedClient client : clients) {
                sc.os.write(client.address.ip);
                sc.os.writeShort(client.address.port);
            }
            sc.os.flush();
    }

    public void update(SocketInfo sc) throws IOException {//change to private
        byte[] clIp = sc.socket.getInetAddress().getAddress();
        if (clIp.length != 4) {
            throw new IOException("Wrong ip address of client");
        }
        short clPort = sc.is.readShort();
        int clFiles = sc.is.readInt();
        boolean allFilesUpdated = true;
        for (int i = 0; i < clFiles; i++) {
            int fId = sc.is.readInt();
            allFilesUpdated = allFilesUpdated && metaInfo.update(fId, clIp, clPort);
        }
        sc.os.writeBoolean(allFilesUpdated);
        sc.os.flush();
    }

    private static class Address {
        public Address() {
            ip = new byte[4];
        }
        public Address(byte[] ip, short port) {
            new Address();
            this.ip = ip;
            this.port = port;
        }
        public byte[] ip;
        public short port;
    }

    private static class TimeStampedClient {
        public TimeStampedClient(Date timeStamp, Address address) {
            this.timeStamp = timeStamp;
            this.address = address;
        }
        public Date timeStamp;
        public Address address;
    }

    private static class FileInfo {
        public FileInfo(String name, long size) {
            this.name = name;
            this.size = size;
        }
        public String name;
        public Long size;

    }


    private static class MetaInfo {
        public MetaInfo() {
            clients = new HashMap<>();
            files = new HashMap<>();
        }
        public void addFile(int id, String name, long size) {
            files.put(id, new FileInfo(name, size));
            clients.put(id, new LinkedList<>());
        }
        public void addClient(int id, byte[] ip, short port) {
            clients.get(id).add(new TimeStampedClient(new Date(), new Address(ip, port)));
        }
        boolean update(int id, byte[] ip, short port) {
            if (!clients.containsKey(id)) return false;
            clients.get(id).removeIf(ts -> Arrays.equals(ts.address.ip, ip));
            addClient(id, ip, port);
            return true;
        }
        public Map<Integer, Deque<TimeStampedClient>> clients;
        public Map<Integer, FileInfo> files;
    }

//    public static void main(String[] args) throws IOException, InterruptedException {
//        TorrentTracker tt = new TorrentTracker();
//        Thread th = new Thread(tt::listener);
//        th.start();
////        tt.addFile("f1", 123L);
////        tt.addFile("f2", 126L);
//        Socket cli = new Socket("localhost", 8081);
//        if (cli.isConnected()) System.out.println("Alive");
//        DataOutputStream os = new DataOutputStream(cli.getOutputStream());
////        BufferedReader brSc = new BufferedReader(new InputStreamReader(cli.getInputStream()));
//        os.writeByte(4);
//        os.flush();
//        os.writeShort(8808);
//        os.writeInt(2);
//        os.writeInt(1);
//        os.writeInt(5);
//        os.flush();
////        os.writeByte(2);
////        os.flush();
////        os.writeUTF("name1");
////        os.writeLong(123L);
////        os.flush();
//
////        os.close();
////        cli.close();
//
////        Thread.sleep(20);
//        Socket cli1 = new Socket("localhost", 8081);
//        if (cli1.isConnected()) System.out.println("Alive");
//        DataOutputStream os1 = new DataOutputStream(cli1.getOutputStream());
//        DataInputStream is1 = new DataInputStream(cli1.getInputStream());
////        BufferedReader brSc1 = new BufferedReader(new InputStreamReader(cli1.getInputStream()));
//        os1.writeByte(3);
//        os1.writeInt(5);
//        os1.flush();
//
////        os.flush();
////        os.writeUTF("name1");
////        os.writeLong(123L);
////        os.flush();
//
////        os.writeByte(2);
////        os.flush();
////        os.writeUTF("name2");
////        os.writeLong(125L);
////        os.flush();
////        os.writeByte(1);
//
////        while (!request.equals("exit")) {
////            os.write((request).getBytes());
////            os.flush();
//////            os.close();
////
//
////        Thread.sleep(3);
//
//
//
//        int count = is1.readInt();
//        for (int i = 0; i < count; i++) {
//            System.out.println(is1.readByte() + "." + is1.readByte() + "." + is1.readByte() + "." + is1.readByte() + ":" + is1.readShort());
//        }
////        for (int i = 0; i < count; i++) {
////            System.out.println("id: " + is1.readInt() + " name: " + is1.readUTF() + " size: " + is1.readLong());
////        }
////            while (brSc1.ready()) {
////                System.out.println(brSc1.readLine());
////            }
////            request = br.readLine();
////        }
//        os.close();
//        cli.close();
//        os1.close();
////        brSc1.close();
//        cli1.close();
//        th.interrupt();
//        System.exit(0);
//
//    }
}
