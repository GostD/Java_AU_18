import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class FileSystem {

    private static class FileInfo {
        FileInfo(String path, long size) {
            this.path = path;
            this.size = size;
            isFull = false;
            readyParts = new HashSet<>();
        }
        FileInfo(String path, long size, boolean isFull) {
            this.path = path;
            this.size = size;
            this.isFull = isFull;
            readyParts = new HashSet<>();
        }
        public String path;
        public boolean isFull;
        public Set<Integer> readyParts;
        public long size;
//        public boolean[] markedParts;
    }

    private Map<Integer, RandomAccessFile> workingFiles;
    private Map<Integer, FileInfo> filesInfo;
    private Map<Integer, boolean[]> markedParts;
    private Map<Integer, File> partFiles;
    private static int partSize = 10*1024*1024;
    FileSystem() {
        workingFiles = new HashMap<>();
        filesInfo = new HashMap<>();
        markedParts = new HashMap<>();
        partFiles = new HashMap<>();
    }

    public void close() throws IOException {
        for (RandomAccessFile fl : workingFiles.values()) {
            fl.close();
        }
    }

    public long getSize(int id) {
        return filesInfo.get(id).size;
    }

    public int getPartSize() { return partSize; }

    public int getPartsCount(int id) { return  (int)(Math.ceil(1.0*filesInfo.get(id).size/partSize)); }

    void addFull(Integer id, String path, long size) throws IOException {
        File fl = new File(path + ".part");
        boolean created = false;
        if (!fl.exists()) created = fl.createNewFile();
//        if (!created) throw new IOException("Could not create .part file");
        try (BufferedWriter fw = new BufferedWriter(new FileWriter(fl))) {
            fw.write("1\n");
            fw.flush();
        }
        add(id, path, size);

    }
    void add(Integer id, String path, long size) {
        filesInfo.put(id, new FileInfo(path, size));
        partFiles.put(id, new File(path + ".part"));
        File parts = partFiles.get(id);
        int partsCount = (int)Math.ceil(1.0*size/partSize);

        try {
            boolean created = false;
            if (!parts.exists()) {
                created = parts.createNewFile();
                if (!created) throw new IOException();
                try (BufferedWriter os = new BufferedWriter(new FileWriter(parts))) {
                    os.write("0\n");
                    os.flush();
                }
            }
            else {
                BufferedReader is = new BufferedReader(new FileReader(parts));
                boolean full = Integer.parseInt(is.readLine()) == 1;
                FileInfo fileInfForId = filesInfo.get(id);
                fileInfForId.isFull = full;
                if (!full) {
                    markedParts.put(id, new boolean[partsCount]);
                    boolean[] marks = markedParts.get(id);
                    while (is.ready()) {
                        int cur = Integer.parseInt(is.readLine());
                        marks[cur] = true;
                        fileInfForId.readyParts.add(cur);
                    }
                }
                is.close();
            }

        } catch (FileNotFoundException e) {
            System.out.println("Could not find part file");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public byte[] getBytes(int id, int partNum) throws IOException {
        try {
            if (!workingFiles.containsKey(id)) workingFiles.put(id,new RandomAccessFile(filesInfo.get(id).path, "rw"));//check valid id
            RandomAccessFile raf = workingFiles.get(id);
            int size = getPartsCount(id);
            int sizeToRead = partSize;
            if (partNum + 1 == size) {
                sizeToRead = (int) (filesInfo.get(id).size % partSize);
            }
            ByteBuffer buffer = ByteBuffer.allocate(sizeToRead);
            raf.getChannel().read(buffer, partNum * partSize);
            return buffer.array();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeBytes(int id, int partNum, byte[] buf) throws IOException {
        try {
            String getFileName = "";
            if (!workingFiles.containsKey(id)) {
                String[] fullName = filesInfo.get(id).path.split("/");
                getFileName = fullName[fullName.length - 1];
                workingFiles.put(id,new RandomAccessFile(getFileName, "rw"));//check valid id
            }
            RandomAccessFile raf = workingFiles.get(id);
            File partGetFile = new File(getFileName + ".part");
            if (!partGetFile.exists()) {
                partGetFile.createNewFile();
                FileWriter os = new FileWriter(partGetFile);
                os.write("0\n");
                os.close();
            }
            FileChannel fc = raf.getChannel();
            fc.write(ByteBuffer.wrap(buf), partNum * partSize);
            filesInfo.get(id).readyParts.add(partNum);
            if (getAllParts(id).size() == getPartsCount(id) || isFull(id)) {
                try (BufferedWriter os = new BufferedWriter(new FileWriter(partGetFile))) {
                    os.write("1\n");
                }
            } else {
                try (BufferedWriter os = new BufferedWriter(new FileWriter(partGetFile, true))) {
                    os.write(partNum + "\n");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

//    String getName(int id) {
//        return idToPath.get(id);
//    }
    boolean isFull(int id) { return filesInfo.get(id).isFull; }
    Set<Integer> getAllParts(int id) {
        return filesInfo.get(id).readyParts;
    }
}
