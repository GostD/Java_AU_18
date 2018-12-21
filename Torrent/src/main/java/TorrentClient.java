import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TorrentClient {
    private ServerSocket servSc;
    private short port;
    private File seedFiles;
    private static List<String> avaliableRequests = Arrays.asList("stat", "get");
    private Map<Integer, FileInfo> idToFileInfo;
    private String serverAddress;
    private ExecutorService threadPool;
    private FileSystem fileSystem;

    public TorrentClient(String serverAddress, short port) {
        this.port = port;
        try {
            servSc = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Could not create socket");
            e.printStackTrace();
        }
        idToFileInfo = new HashMap<>();
        seedFiles = new File(".seeds");
        fileSystem = new FileSystem();
        if (!seedFiles.exists()) {
            try {
                boolean created = seedFiles.createNewFile();
                if (!created) throw new IOException();
            } catch (IOException e) {
                System.out.println("Could not create .seeds file");
                e.printStackTrace();
            }

        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(seedFiles))) {
                String line;
                while (br.ready()) {
                    line = br.readLine();
                    String[] str = line.split(" ");
                    if (str.length < 4) continue;
                    idToFileInfo.put(Integer.parseInt(str[0]), new FileInfo(str[1], Long.parseLong(str[2]), str[3]));
                    fileSystem.add(Integer.parseInt(str[0]), str[3], Long.parseLong(str[2]));
                }
            } catch (FileNotFoundException e) {
                System.out.println("Could not find seeds meta file");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.serverAddress = serverAddress;
        threadPool = Executors.newSingleThreadExecutor();
        try {
            update();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        threadPool.shutdown();
        try {
            fileSystem.close();
            servSc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listener() {
        while (true) {
            try {
                final Socket sc;
                try {
                    sc = servSc.accept();
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
                        String strReq = avaliableRequests.get(metByte - 1) + "Req";
                        Method met = TorrentClient.class.getDeclaredMethod(strReq, SocketInfo.class);
                        met.invoke(this, sInf);
                    } catch (IllegalAccessException | InvocationTargetException | IOException | NoSuchMethodException e) {
                        System.out.println("could not call request method");
                        e.printStackTrace();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                close();
                break;
            }
        }
    }

    public void list() throws IOException {
        try (SocketInfo sc = new SocketInfo(new Socket(serverAddress, 8081))) {
            sc.os.writeByte(1);
            sc.os.flush();
            int count = sc.is.readInt();
            System.out.println("Total count: " + count + " files");
            System.out.println("Id      Name        Size");
            for (int i = 0; i < count; i++) {
                int id = sc.is.readInt();
                String name = sc.is.readUTF();
                long size = sc.is.readLong();
                if (!fileSystem.contains(id))
                    fileSystem.add(id, name, size);
                System.out.println(id + " " + name + " " + size);
            }
        }
    }

    public void upload(String path) throws IOException {
        File nwFile = new File(path);
        if (!nwFile.exists()) throw new IOException("File does not exists");
        String name = nwFile.getName();
        long size = nwFile.length();
        try (SocketInfo sc = new SocketInfo(new Socket(serverAddress, 8081))) {
            sc.os.writeByte(2);
            sc.os.writeUTF(name);
            sc.os.writeLong(size);
            int id = sc.is.readInt();
            fileSystem.add(id, path, size);
            System.out.println("id=" + id);
            idToFileInfo.put(id, new FileInfo(name, size, path));
            try (BufferedWriter nw = new BufferedWriter(new FileWriter(seedFiles, true))) {
                nw.write(id + " " + name + " " +  size + " " + path + "\n");
                nw.flush();
            }
        }
    }

    public void sources(String id) throws IOException {
        try (SocketInfo sc = new SocketInfo(new Socket(serverAddress, 8081))) {
            sc.os.writeByte(3);
            sc.os.flush();
            sc.os.writeInt(Integer.parseInt(id));
            sc.os.flush();
            int count = sc.is.readInt();
            System.out.println("Total count: " + count + " sources");
            System.out.println("Ip          Port");
            for (int i = 0; i < count; i++) {
                byte[] ip = new byte[4];
                sc.is.readFully(ip);
                String ipStr = "" + (ip[0] < 0 ? ip[0] + 256 : ip[0]);
                for (int j = 0; j < 3; j++) ipStr += "." + (ip[j+1] < 0 ? ip[j+1] + 256 : ip[j+1]);
                System.out.println(ipStr + "   " + sc.is.readShort());
            }


        }
    }

    public void update() throws IOException {
        try (SocketInfo sc = new SocketInfo(new Socket(serverAddress, 8081))) {
            sc.os.writeByte(4);
            sc.os.writeShort(port);
            sc.os.writeInt(idToFileInfo.size());
            for (Integer id : idToFileInfo.keySet()) {
                sc.os.writeInt(id);
            }
            sc.os.flush();
            boolean updated = sc.is.readBoolean();
            System.out.println(updated ? "updated" : "could not update");
        }
    }



    public void statReq(SocketInfo sc) throws IOException {
        int id = sc.is.readInt();
        if (idToFileInfo.containsKey(id)) {
            if (!fileSystem.isFull(id)) {
                Set<Integer> parts = fileSystem.getAllParts(id);
                sc.os.writeInt(parts.size());
                for (Integer partNum : parts) {
                    sc.os.writeInt(partNum);
                }
                sc.os.flush();
            } else {
                int count = fileSystem.getPartsCount(id);
                sc.os.writeInt(count);
                for (int i = 0; i < count; i++) {
                    sc.os.writeInt(i);
                }
                sc.os.flush();
            }
        } else {
            sc.os.writeInt(0);
            sc.os.flush();
        }

    }
    public void stat(String ip, String port, String id) throws IOException {
        try (SocketInfo sc = new SocketInfo(new Socket(ip, Short.parseShort(port)))) {
            sc.os.writeByte(1);
            sc.os.flush();
            sc.os.writeInt(Integer.parseInt(id));
            sc.os.flush();
            int count = sc.is.readInt();
            System.out.println("Total " + count + " parts for id=" + id + ":");
            for (int i = 0; i < count; i++) {
                int partNum = sc.is.readInt();
                System.out.print(partNum + " ");
            }
            System.out.println();
        }
    }

    public void getReq(SocketInfo sc) throws IOException {
        int id = sc.is.readInt();
        int partNum = sc.is.readInt();
        if (fileSystem.isFull(id) || fileSystem.getAllParts(id).contains(partNum)) {
            byte[] bytesToWrite = fileSystem.getBytes(id, partNum);
            if (bytesToWrite == null) throw new IOException("Could not read bytes sequence");
            sc.os.write(bytesToWrite);
            sc.os.flush();
        } else {
            throw new IOException("No such part for this file");
        }
    }

    public void get(String ip, String port, String id, String partFrom, String partTo) throws IOException {
        for (int i = Integer.parseInt(partFrom); i <= Integer.parseInt(partTo); i++) {
            get(ip, port, id, "" + i);
        }
    }

    public void get(String ip, String port, String id, String partNum) throws IOException {
        try (SocketInfo sc = new SocketInfo(new Socket(ip, Short.parseShort(port)))) {
            sc.os.writeByte(2);
            sc.os.flush();
            sc.os.writeInt(Integer.parseInt(id));
            sc.os.writeInt(Integer.parseInt(partNum));
            sc.os.flush();
            long fileSize = fileSystem.getSize(Integer.parseInt(id));
            int numParts = (int)(Math.ceil(1.0*fileSize/fileSystem.getPartSize()));
            byte[] buf = (Integer.parseInt(partNum) + 1 < numParts) ? new byte[fileSystem.getPartSize()] : new byte[(int)(fileSize % fileSystem.getPartSize())];
            sc.is.readFully(buf);
            fileSystem.writeBytes(Integer.parseInt(id), Integer.parseInt(partNum), buf);
            if (!idToFileInfo.containsKey(Integer.parseInt(id))) {
                String path = fileSystem.getPath(Integer.parseInt(id));
                idToFileInfo.put(Integer.parseInt(id), new FileInfo(path, fileSize, path));
                try (FileWriter fw = new FileWriter(seedFiles, true)) {
                    fw.write(id + " " + path + " " + fileSize + " " + path + "\n");
                    fw.flush();
                }
            }

        }
    }

    private static class FileInfo {
        private FileInfo(String name, long size, String path) {
            this.name = name;
            this.size = size;
            this.path = path;
        }
        private String name;
        private Long size;
        private String path;

    }

}
