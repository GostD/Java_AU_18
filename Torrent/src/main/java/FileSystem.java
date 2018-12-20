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
    }

    private Map<Integer, RandomAccessFile> workingFiles;
    private Map<Integer, FileInfo> filesInfo;
    private Map<Integer, File> partFiles;
    private static int partSize = 10*1024*1024;
    FileSystem() {
        workingFiles = new HashMap<>();
        filesInfo = new HashMap<>();
        partFiles = new HashMap<>();
    }

    public void close() throws IOException {
        for (RandomAccessFile fl : workingFiles.values()) {
            fl.close();
        }
    }

    public void addPart(int id, int partNum) {
        filesInfo.get(id).readyParts.add(partNum);
    }

    public long getSize(int id) {
        return filesInfo.get(id).size;
    }

    public int getPartSize() { return partSize; }

    public int getPartsCount(int id) { return  (int)(Math.ceil(1.0*filesInfo.get(id).size/partSize)); }

    void add(Integer id, String path, long size) {
        filesInfo.put(id, new FileInfo(path, size));
        partFiles.put(id, new File(path + ".part"));
        File parts = partFiles.get(id);
        boolean full = false;
        if (!parts.exists()) full = true;
//        int partsCount = (int)Math.ceil(1.0*size/partSize);
        try {
                filesInfo.get(id).isFull = full;
                if (!full) {
                    try (BufferedReader is = new BufferedReader(new FileReader(parts))) {
                        while (is.ready()) {
                            int cur = Integer.parseInt(is.readLine());
                            filesInfo.get(id).readyParts.add(cur);
                        }
                    }
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
                workingFiles.put(id,new RandomAccessFile(getFileName, "rw"));
            }
            RandomAccessFile raf = workingFiles.get(id);

            FileChannel fc = raf.getChannel();
            fc.write(ByteBuffer.wrap(buf), partNum * partSize);
            filesInfo.get(id).readyParts.add(partNum);
            File partGetFile = new File(getFileName + ".part");
            if (getAllParts(id).size() == getPartsCount(id) || isFull(id)) {
                if (partGetFile.exists()) {
                    partGetFile.delete();
                }
            } else {
                if (!partGetFile.exists() && isFull(id)) {
                    partGetFile.createNewFile();
                }
                try (BufferedWriter os = new BufferedWriter(new FileWriter(partGetFile, true))) {
                    os.write(partNum + "\n");
                }
            }



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    boolean isFull(int id) { return filesInfo.get(id).isFull; }
    Set<Integer> getAllParts(int id) {
        return filesInfo.get(id).readyParts;
    }
}
