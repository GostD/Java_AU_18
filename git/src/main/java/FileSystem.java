import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;

public class FileSystem {
    public FileSystem() throws IOException {
        File tmp = new File(".simple-git");
        if (!tmp.exists()) {
            throw new ExceptionInInitializerError("git was not initialised");
        }
        file_ = new File(".simple-git/files");
        if (!file_.exists()) {
            file_.mkdir();
        }
        names_ = new File(".simple-git/names");
        if (!names_.exists()) {
            names_.mkdir();
        }
        commits_ = new File(".simple-git/commits");
        if (!commits_.exists()) {
            commits_.mkdir();
        }
        repo_ = new File(".simple-git/repo");
        if (!repo_.exists()) {
            repo_.mkdir();
        }
        info_ = new File(".simple-git/.info");
        if (!info_.exists()) {
            info_.createNewFile();
        }
    }

    public int addFiles(String[] args) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy\nHH:mm:ss\n");
        String[] comLst = commits_.list();
        int nwComNum = 1;
        if (comLst != null) nwComNum += comLst.length;
        File nwComFile = new File(commits_.getAbsolutePath() + "/" + nwComNum);
        FileWriter fw = new FileWriter(nwComFile);
        fw.write(dateFormat.format(new Date()));
        fw.write(args[1]);
        for (int i = 2; i < args.length; i++) {
            File curFile = new File(args[i]);
            if (!curFile.exists()) {
                throw new IllegalArgumentException("No such file in the path: " + args[i]);
            }
            String hashOfFile = "";
            try {
                hashOfFile = getHash(curFile);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("Could not get file hash");
            } catch (IOException e) {
                e.printStackTrace();
            }
            fw.write("\n" + curFile.getAbsolutePath());
            fw.write("\n" + hashOfFile);
        }
        fw.flush();
        fw.close();
        return -1;
    }
    private String getHashFromStream(InputStream is, String fName) throws IOException, NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        byte[] buffer = new byte[1024];
        for (int read; (read = is.read(buffer)) != -1; ) {
            messageDigest.update(buffer, 0, read);
        }
        messageDigest.update(fName.getBytes());
        try(Formatter formatter = new Formatter()) {
            for (final byte b : messageDigest.digest()) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    private void writeName(String path, String name) throws IOException {
        File nwName = new File(path);
        if (!nwName.exists()) {
            FileWriter fw = new FileWriter(nwName);
            fw.write(name);
            fw.flush();
            fw.close();
        }
    }

    private String getHash(File file) throws NoSuchAlgorithmException, IOException {
        String hash = "";
        if (file.isDirectory()) {
            hash = recursiveAddFiles(file);
        } else {
            try {
                hash = getHashFromStream(new FileInputStream(file), file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
            File nwFile = new File(file_.getAbsolutePath() + "/" + hash);
            if (!nwFile.exists()) {
                nwFile.createNewFile();
                FileWriter fw = new FileWriter(nwFile);
                BufferedReader fr = new BufferedReader(new FileReader(file));
                File repoFile = new File(".simple-git/repo/" + file.getPath());
                if (repoFile.exists()) {
                    new PrintWriter(repoFile).close();
                } else {
                    repoFile.getParentFile().mkdirs();
                    repoFile.createNewFile();
                }
                FileWriter fwr = new FileWriter(repoFile);
                while (true) {
                    String line = fr.readLine();
                    fw.write(line);
                    fwr.write(line);
                    if (fr.ready()) {
                        fw.write('\n');
                        fwr.write('\n');
                    }
                    else break;
                }
                fw.flush();
                fw.close();
                fwr.flush();
                fwr.close();
                fr.close();



            }
            writeName(names_.getAbsolutePath() + "/" + hash, file.getName());
        }
        return hash;

    }

    private String recursiveAddFiles(File file) throws NoSuchAlgorithmException, IOException {
        File[] listFiles = file.listFiles();
        if (listFiles == null) return "0";
        String[] hashes = new String[listFiles.length];
        for (int i = 0; i < listFiles.length; i++) {
            File curFile = listFiles[i];
            if (curFile.isDirectory()) {
                hashes[i] = recursiveAddFiles(curFile);
            } else {
                hashes[i] = getHash(curFile);
            }
        }
        String hash = getHashFromStream(new ByteArrayInputStream(
                Arrays.stream(hashes).sorted().reduce((a, b) -> a + b).orElse("").getBytes()), file.getName());
        File newFile = new File(file_.getAbsolutePath() + "/" + hash);
        if (!newFile.exists()) {
            FileWriter fw = new FileWriter(newFile);
            fw.write("//directory//");
            for (String hs : hashes) {
                fw.write("\n" + hs);
            }
            fw.flush();
            fw.close();
        }
        writeName(names_.getAbsolutePath() + "/" + hash, file.getName());
        return hash;
    }

    public void add(String[] args) throws IOException {
        FileWriter fw = new FileWriter(info_, true);
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            fw.write("\nadd\n");
            fw.write(arg);
        }
        fw.flush();
        fw.close();
    }

    public void remove(String[] args) throws IOException {
        FileWriter fw = new FileWriter(info_, true);
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            fw.write("\nremove\n");
            fw.write(arg);
        }
        fw.flush();
        fw.close();
    }

    private void removeFromRepo(String[] args) {
        for (String arg : args) {
            File repoFile = new File(".simple-git/repo/" + (new File(arg)).getPath());
            if (repoFile.isDirectory()) {
                File[] fls = repoFile.listFiles();
                String[] names = new String[fls.length];
                for (int i = 0; i < fls.length; i++) {
                    names[i] = fls[i].getPath().substring(".simple-git/repo/".length());
                }
                removeFromRepo(names);
            }
            repoFile.delete();
        }
    }

    public void flush(String[] args) throws IOException {
        BufferedReader fr = new BufferedReader(new FileReader(info_));
        StringBuilder adds = new StringBuilder();
        while (fr.ready()) {
            String line = fr.readLine();
            if (line.equals("add")) {
                String add = fr.readLine();
                adds.append(add + " ");
            } else if (line.equals("remove")) {
                String rm = fr.readLine();
                removeFromRepo(rm.split(" "));
            }
        }
        if (!adds.toString().equals("")) {
            String[] fls = adds.toString().split(" ");
            String[] ff = new String[fls.length + 2];
            ff[0] = args[0];
            ff[1] = args[1];
            for (int i = 0; i < fls.length; i++) {
                ff[i + 2] = fls[i];
            }
            addFiles(ff);
        }

        fr.close();
        new FileWriter(info_).close();
    }

    public void status() throws IOException {
        BufferedReader fr = new BufferedReader(new FileReader(info_));
        StringBuilder adds = new StringBuilder();
        StringBuilder rms = new StringBuilder();
        while (fr.ready()) {
            String line = fr.readLine();
            if (line.equals("add")) {
                line = fr.readLine();
                String[] ad = line.split(" ");
                for (String str : ad) {
                    adds.append(str + '\n');
                }
            } else if (line.equals("remove")) {
                line = fr.readLine();
                String[] rm = line.split(" ");
                for (String str : rm) {
                    rms.append(str);
                }
            }
        }
        fr.close();
        System.out.println("ADDED:");
        System.out.println(adds.toString());
        System.out.println("REMOVED:");
        System.out.println(rms.toString());
    }



    private File file_;
    private File names_;
    private File commits_;
    private File repo_;
    private File info_;

}
