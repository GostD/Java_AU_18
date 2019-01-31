import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class FileSystem {

    private File file_;
    private File names_;
    private File commits_;
    private File repo_;
    private File index_;
    private File branches_;
    private File info_;

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
        index_ = new File(".simple-git/index");
        if (!index_.exists()) {
            index_.mkdir();
        }
        branches_ = new File(".simple-git/branches");
        if (!branches_.exists()) {
            branches_.mkdir();
        }
        info_ = new File(".simple-git/.info");
        if (!info_.exists()) {
            info_.createNewFile();
        }
    }

    public File getNames_() { return names_; }

    void addsToTree(UpdateTree tree, String[] files) {//TODO: not so easy, can be rm in childs of added files
        Path basePath = Paths.get("").toAbsolutePath();
//        Path curPath = Paths.get(args[i]).toAbsolutePath();
        for (String file : files) {
            String pathFromCurDir = basePath.relativize(Paths.get(file).toAbsolutePath()).toString();
            File indexFile = new File(".simple-git/index/" + pathFromCurDir);
            if (indexFile.exists()) {
                File repoFile = new File(".simple-git/repo/" + pathFromCurDir);
                if (repoFile.exists()) {
                    tree.addNode(pathFromCurDir, "update");
                } else {
                    tree.addNode(pathFromCurDir, "add");
                }
            }
        }
    }

    public String getHashFromStream(InputStream is, String fName) throws IOException, NoSuchAlgorithmException {
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

    public String getHash(File file) throws NoSuchAlgorithmException, IOException {
        String hash = "";
        if (file.isDirectory()) {
            hash = recursiveAddFiles(file);
        } else {
            try {
                hash = getHashFromStream(new FileInputStream(file), file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
            File nwFile = new File(file_.getCanonicalPath() + "/" + hash);
            if (!nwFile.exists()) {
                nwFile.createNewFile();
                try (FileWriter fw = new FileWriter(nwFile);
                BufferedReader fr = new BufferedReader(new FileReader(file))) {
                    while (true) {
                        String line = fr.readLine();
                        fw.write(line);
                        if (fr.ready()) {
                            fw.write('\n');
                        } else break;
                    }
                }
            }
            writeName(names_.getCanonicalPath() + "/" + hash, file.getName());
        }
        return hash;

    }

    private String recursiveAddFiles(File file) throws NoSuchAlgorithmException, IOException {
        File[] listFiles = file.listFiles();
        String[] hashes = new String[listFiles != null ? listFiles.length : 0];
        if (listFiles != null) {
            for (int i = 0; i < listFiles.length; i++) {
                File curFile = listFiles[i];
                if (curFile.isDirectory()) {
                    hashes[i] = recursiveAddFiles(curFile);
                } else {
                    hashes[i] = getHash(curFile);
                }
            }
        }
        String hash = getHashFromStream(new ByteArrayInputStream(
                Arrays.stream(hashes).sorted().reduce((a, b) -> a + b).orElse("").getBytes()), file.getName());
        File newFile = new File(file_.getCanonicalPath() + "/" + hash);
        if (!newFile.exists()) {
            try (FileWriter fw = new FileWriter(newFile)) {
                fw.write("//directory//");
                for (String hs : hashes) {
                    fw.write("\n" + hs);
                }
            }
        }
        writeName(names_.getCanonicalPath() + "/" + hash, file.getName());
        return hash;
    }

    public void add(String[] args) throws IOException {
        try (FileWriter fw = new FileWriter(info_, true)) {
            fw.write("add\n");
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                fw.write(arg);
                File indexCopy = new File(index_.getCanonicalPath() + "/" + arg);
                File curFile = new File(arg);
                if (!curFile.exists())
                    System.out.println("No such file in the dirrectory: " + arg);

                if (!indexCopy.exists()) {
                    if (!curFile.isDirectory()) {
                        indexCopy.getParentFile().mkdirs();
                        indexCopy.createNewFile();
                    } else {
                        indexCopy.mkdirs();
                    }

                }
                if (!curFile.isDirectory()) {
                    if (!curFile.exists()) curFile.getParentFile().mkdirs();
                    try (BufferedReader fr = new BufferedReader(new FileReader(curFile)); BufferedWriter fwr = new BufferedWriter(new FileWriter(indexCopy))) {
                        while (fr.ready()) {
                            fwr.write(fr.readLine());
                            if (fr.ready()) fwr.write("\n");
                        }
                    }
                } else {
                    if (!indexCopy.exists()) indexCopy.mkdirs();
                    copyAll(curFile, indexCopy);

                }

                if (i != args.length - 1) fw.write(" ");
            }
            fw.write("\n");
            fw.flush();
        }
    }

    private void copyAll(File from, File to) throws IOException {
        File[] listOfFiles = from.listFiles();
        for (File file : listOfFiles) {
            File toFile = new File(to.getCanonicalPath() + "/" + file.getName());
            if (file.isDirectory()) {
                toFile.mkdirs();
                copyAll(file, toFile);
            } else {
                toFile.createNewFile();
                try (BufferedReader fr = new BufferedReader(new FileReader(file)); BufferedWriter fw = new BufferedWriter(new FileWriter(toFile))) {
                    while (fr.ready()) {
                        fw.write(fr.readLine());
                        if (fr.ready()) fw.write("\n");
                    }
                }

            }
        }
    }

    private void recursiveRemove(File file) {
        if (file.isDirectory()) {
            File[] flList = file.listFiles();
            if (flList != null) {
                for (File fl : flList) {
                    recursiveRemove(fl);
                }
            }
        }
        file.delete();

    }

    public void remove(String[] args) throws IOException {
        try (FileWriter fw = new FileWriter(info_, true)) {
            fw.write("remove\n");
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                fw.write(arg);
                File indexCopy = new File(index_.getCanonicalPath() + "/" + arg);
                if (indexCopy.exists()) {
                    if (indexCopy.isDirectory())
                        recursiveRemove(indexCopy);
                    else
                        indexCopy.delete();
                }
                if (i != args.length - 1) fw.write(" ");
            }
            fw.write("\n");
            fw.flush();
        }
    }

    public void updateFilesFromPath(String root, List<String> hashes, String path) {
        if (path.length() > 0 && path.charAt(path.length() - 1) != '/') path += "/";
        for (String hash : hashes) {
            File hashFile = new File(".simple-git/files/" + hash);
            File nameHash = new File(".simple-git/names/" + hash);
            String name = "";
            try (BufferedReader fr = new BufferedReader(new FileReader(nameHash))) {
                name = fr.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<String> nextHashes = new ArrayList<>();
            try (BufferedReader fr = new BufferedReader(new FileReader(hashFile))) {
                String line = fr.readLine();
                if (line.equals("//directory//")) {
                    while (fr.ready()) nextHashes.add(fr.readLine());
                    if (nextHashes.isEmpty()) {
                        File fl = new File(root + path + name);
                        if (!fl.exists()) fl.mkdirs();
                    }
                } else {
                    File fileToWrite = new File(root + path + name);
                    if (!fileToWrite.exists()) {
                        fileToWrite.getParentFile().mkdirs();
                        fileToWrite.createNewFile();
                    }
                    try (FileWriter fw = new FileWriter(fileToWrite)) {
                        fw.write(line);
                        while (fr.ready()) {
                            fw.write("\n");
                            fw.write(fr.readLine());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!nextHashes.isEmpty()) {
                updateFilesFromPath(root, nextHashes,path + name);
            }
        }
    }

    private void recursiveRemoveFilesFromPath(String root, List<String> hashes, String path) {
        if (path.length() > 0 && path.charAt(path.length() - 1) != '/') path += "/";
        for (String hash : hashes) {
            File hashFile = new File(".simple-git/files/" + hash);
            List<String> nwHashes = new ArrayList<>();
            File nameFile = new File(".simple-git/names/" + hash);
            String name = "";
            try (BufferedReader br = new BufferedReader(new FileReader(nameFile))) {
                name = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (BufferedReader br = new BufferedReader(new FileReader(hashFile))) {
                if (br.readLine().equals("//directory//")) {
                    while (br.ready()) nwHashes.add(br.readLine());
                } else {
                    new File(root + path + name).delete();
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            recursiveRemoveFilesFromPath(root, nwHashes, path + name);
            File curFile = new File(root + path + name);
            if (curFile.isDirectory() && curFile.listFiles().length == 0) curFile.delete();
        }
    }

    public void diffUpdateFilesFromPath(String root, List<String> curHashes, List<String> nwHashes, String path) {
        if (path.length() > 0 && path.charAt(path.length() - 1) != '/') path += "/";
        if (curHashes.isEmpty()) {
            updateFilesFromPath(root, nwHashes, path);
        }
        List<String> curNames = new ArrayList<>();
        List<String> nwNames = new ArrayList<>();
        for (String hash : curHashes) {
            File nameHash = new File(".simple-git/names/" + hash);
            try (BufferedReader fr = new BufferedReader(new FileReader(nameHash))) {
                curNames.add(fr.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String hash : nwHashes) {
            File nameHash = new File(".simple-git/names/" + hash);
            try (BufferedReader fr = new BufferedReader(new FileReader(nameHash))) {
                nwNames.add(fr.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String name : curNames) {
            if (!nwNames.contains(name)) {
                recursiveRemoveFilesFromPath(root, Arrays.asList(curHashes.get(curNames.indexOf(name))), path);
            }
        }
        for (String name : nwNames) {
            List<String> updCurHashes = new ArrayList<>();
            List<String> updNwHashes = new ArrayList<>();
            if (curNames.contains(name)) {
                File curHashFile = new File(".simple-git/files/" + curHashes.get(curNames.indexOf(name)));
                try (BufferedReader br = new BufferedReader(new FileReader(curHashFile))) {
                    String line = br.readLine();
                    if (line.equals("//directory//")) {
                        while (br.ready()) updCurHashes.add(br.readLine());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (updCurHashes.isEmpty()) {
                    recursiveRemoveFilesFromPath(root, Arrays.asList(curHashes.get(curNames.indexOf(name))), path);
                    updateFilesFromPath(root, Arrays.asList(nwHashes.get(nwNames.indexOf(name))), path);
                }
            }
            if (!(curNames.contains(name) && updCurHashes.isEmpty())) {
                String nwHash = nwHashes.get(nwNames.indexOf(name));
                File nwHashFile = new File(".simple-git/files/" + nwHash);
                try (BufferedReader br = new BufferedReader(new FileReader(nwHashFile))) {
                    if (br.readLine().equals("//directory//")) {
                        while (br.ready()) updNwHashes.add(br.readLine());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (updNwHashes.isEmpty()) {
                    if (curNames.contains(name))
                        recursiveRemoveFilesFromPath(root, Arrays.asList(curHashes.get(curNames.indexOf(name))), path);
                    updateFilesFromPath(root, Arrays.asList(nwHash), path);
                } else {
                    diffUpdateFilesFromPath(root, updCurHashes, updNwHashes, path + name);
                }
            }

        }

    }


    private void removeFromRepo(String[] args, boolean flag, UpdateTree tree) {
        Path basePath = Paths.get("").toAbsolutePath();
        for (String arg : args) {
            Path curPath = Paths.get(arg).toAbsolutePath();
            String pathFromCurDir = basePath.relativize(curPath).toString();
            File indexFile = new File(".simple-git/index/" + pathFromCurDir);
            File repoFile = new File(".simple-git/repo/" + pathFromCurDir);
            if (repoFile.isDirectory()) {
                File[] fls = repoFile.listFiles();
                String[] names = new String[fls.length];
                for (int i = 0; i < fls.length; i++) {
                    names[i] = fls[i].getPath().substring(".simple-git/repo/".length());
                }
                if (flag && !indexFile.exists()) {
                    removeFromRepo(names, false, tree);
                    tree.addNode(pathFromCurDir, "rm");
                }
                else
                    removeFromRepo(names, flag, tree);
                if (!indexFile.exists()) repoFile.delete();
            } else {
                if (!indexFile.exists()) {
                    repoFile.delete();
                    if (flag) {
                        tree.addNode(pathFromCurDir, "rm");
                    }
                }
            }
        }
    }

    public void flush(String[] args) throws IOException {
        BufferedReader fr = new BufferedReader(new FileReader(info_));
        StringBuilder adds = new StringBuilder();
        StringBuilder rms = new StringBuilder();
        while (fr.ready()) {
            String line = fr.readLine();
            if (line.equals("add")) {
                String add = fr.readLine();
                if (adds.length() != 0) adds.append(" ");
                adds.append(add);
            } else if (line.equals("remove")) {
                String rm = fr.readLine();
                if (rms.length() != 0) rms.append(" ");
                rms.append(rm);

            }
        }

        UpdateTree tree = new UpdateTree();
        if (!adds.toString().equals("")) {
            String[] fls = adds.toString().split(" ");
            addsToTree(tree, fls);
        }

        if (!rms.toString().equals("")) removeFromRepo(rms.toString().split(" "), true, tree);

        if (!tree.isEmpty()) {
            File head = new File(".simple-git/HEAD");
            try {
                boolean changed = false;
                List<String> nwRootHashes;
                String curCommitHash = "";
                String branchName = "master";
                if (!head.exists()) {
                    nwRootHashes = tree.updateRootHashes(new ArrayList<>(), this);
                    if (!nwRootHashes.isEmpty()) changed = true;
                } else {
                    try (BufferedReader br = new BufferedReader(new FileReader(head))) {
                        branchName = br.readLine();
                    }
                    try (BufferedReader br = new BufferedReader(new FileReader(new File(".simple-git/branches/" + branchName)))) {
                        curCommitHash = br.readLine();
                        File commFile = new File(".simple-git/commits/" + curCommitHash);
                        List<String> curCommHashes = new ArrayList<>();
                        try (BufferedReader bufr = new BufferedReader(new FileReader(commFile))) {
                            bufr.readLine();
                            bufr.readLine();
                            bufr.readLine();
                            bufr.readLine();
                            String line;
                            while ((line = bufr.readLine()) != null) {
                                curCommHashes.add(line);
                            }
                        }
                        nwRootHashes = tree.updateRootHashes(new ArrayList<>(curCommHashes), this);
                        if (curCommHashes.size() != nwRootHashes.size()) {
                            changed = true;
                        }

                        else {
                            for (int i = 0; i < curCommHashes.size(); i++) {
                                if (!curCommHashes.get(i).equals(nwRootHashes.get(i))) {
                                    changed = true;
                                    break;
                                }
                            }
                        }

                    }
                }

                if (changed) {
                    String nwCommName = getHashFromStream(new ByteArrayInputStream(
                            nwRootHashes.stream().reduce((a, b) -> a + b).orElse("").getBytes()), curCommitHash);
                    File nwCommFile = new File(".simple-git/commits/" + nwCommName);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy\nHH:mm:ss\n");
                    try (FileWriter fw = new FileWriter(nwCommFile)) {
                        fw.write(dateFormat.format(new Date()));
                        fw.write(args[1]);
                        fw.write("\n");
                        fw.write(curCommitHash);
                        fw.flush();
                        for (String rootHash : nwRootHashes) {
                            fw.write("\n");
                            fw.write(rootHash);
                        }
                        fw.flush();
                    }

                    if (!head.exists()) {
                        try (FileWriter fw = new FileWriter(head)) {
                            fw.write(branchName);
                            fw.flush();
                        }
                    }
                    try (FileWriter fw = new FileWriter(".simple-git/branches/" + branchName)) {
                        fw.write(nwCommName);
                    }
                }

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        fr.close();
        new FileWriter(info_).close();
    }

    private void softUpdateFromIndex(String commitNum) {
        File commFile = new File(".simple-git/commits/" + commitNum);
        List<String> commitHashes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(commFile))) {
            br.readLine();
            br.readLine();
            br.readLine();
            br.readLine();

            while (br.ready()) commitHashes.add(br.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> commNames = new ArrayList<>();
        for (String hash : commitHashes) {
            File nameFile = new File(".simple-git/names/" + hash);
            try (BufferedReader br = new BufferedReader(new FileReader(nameFile))) {
                commNames.add(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File[] indexDirFiles = index_.listFiles();
        if (indexDirFiles != null) {
            for (File fl : indexDirFiles) {
                if (commNames.contains(fl.getName())) {
                    softUpdateDir(fl, "", commitHashes.get(commNames.indexOf(fl.getName())));
                    commitHashes.remove(commNames.indexOf(fl.getName()));
                    commNames.remove(fl.getName());
                } else {
                    try {
                        recursiveAddFromIndex("", fl);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        for (String name : commNames) {
            softRecursiveRemove(new File(name), commitHashes.get(commNames.indexOf(name)));
        }
    }

    private void softRecursiveRemove(File curFile, String curHash) {
        if (!curFile.exists()) return;

        if (!curFile.isDirectory()) {
            try {
                String hash = getHashFromStream(new FileInputStream(curFile), curFile.getName());
                if (hash.equals(curHash)) curFile.delete();
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } else {
            File hashFile = new File(".simple-git/files/" + curHash);
            List<String> dirHashes = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(hashFile))) {
                String line = br.readLine();
                if (!line.equals("//directory//")) return;
                while (br.ready()) dirHashes.add(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<String> dirNames = new ArrayList<>();
            for (String hash : dirHashes) {
                File nameFile = new File(".simple-git/files/" + hash);
                try (BufferedReader br = new BufferedReader(new FileReader(nameFile))) {
                    dirNames.add(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File[] listFiles = curFile.listFiles();
            List<String> listNames = new ArrayList<>();
            if (listFiles != null)
                listNames = Arrays.stream(listFiles).map(File::getName).collect(Collectors.toList());
            for (String name : dirNames) {
                if (listNames.contains(name)) {
                    softRecursiveRemove(listFiles[listNames.indexOf(name)], dirHashes.get(dirNames.indexOf(name)));
                }
            }
            if (curFile.listFiles() == null)
                curFile.delete();

        }
    }

    private void recursiveAddFromIndex(String path, File indexFile) throws IOException {
        if (path.length() > 0 && path.charAt(path.length() - 1) != '/') path += "/";
        File nwFile = new File(path + indexFile.getName());
        if (nwFile.exists()) recursiveRemove(nwFile);
        if (indexFile.isDirectory()) {
            nwFile.mkdirs();
            File[] listFiles = indexFile.listFiles();
            if (listFiles != null) {
                for (File fl : listFiles) {
                    recursiveAddFromIndex(path + indexFile.getName() + "/", fl);
                }
            }

        } else {
            nwFile.createNewFile();
            try (FileWriter fw = new FileWriter(nwFile); BufferedReader br = new BufferedReader(new FileReader(indexFile))) {
                if (br.ready()) fw.write(br.readLine());
                while (br.ready()) {
                    fw.write("\n");
                    fw.write(br.readLine());
                }
            }
        }
    }

    private void softUpdateDir(File curIndexFile, String path, String curHash) {
        if (path.length() > 0 && path.charAt(path.length() - 1) != '/') path += "/";
        if (curIndexFile.isDirectory()) {
            File[] files = curIndexFile.listFiles();
            File hashFile = new File(".simple-git/files/" + curHash);
            List<String> dirHashes = new ArrayList<>();
            List<String> dirNames = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(hashFile))) {
                String line = br.readLine();
                if (!line.equals("//directory//")) return;
                while (br.ready()) dirHashes.add(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (String hash : dirHashes) {
                File nameHash = new File(".simple-git/names/" + hash);
                try (BufferedReader br = new BufferedReader(new FileReader(nameHash))) {
                    dirNames.add(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (files != null) {
                for (File fl : files) {
                    if (dirNames.contains(fl.getName())) {
                        softUpdateDir(fl, path + curIndexFile.getName() + "/", dirHashes.get(dirNames.indexOf(fl.getName())));
                        dirHashes.remove(dirNames.indexOf(fl.getName()));
                        dirNames.remove(fl.getName());
                    } else {
                        try {
                            recursiveAddFromIndex(path + curIndexFile.getName() + "/", fl);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            for (String name : dirNames) {
                softRecursiveRemove(new File(path + curIndexFile.getName() + "/" + name), dirHashes.get(dirNames.indexOf(name)));
            }

        } else {
            File hashFile = new File(".simple-git/files/" + curHash);
            try (BufferedReader br = new BufferedReader(new FileReader(hashFile))) {
                if (br.ready() && br.readLine().equals("//directory//")) return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                File fl = new File(path + curIndexFile.getName());
                if (!fl.exists()) {
                    recursiveAddFromIndex(path, curIndexFile);
                }
                String curDirHash = getHashFromStream(new FileInputStream(fl), fl.getName());
                if (curHash.equals(curDirHash)) {
                    recursiveAddFromIndex(path, curIndexFile);
                } else {
                    return;
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

    }

    public void checkout(String[] args) {
        if (args.length == 1) {
            File checkBranch = new File(".simple-git/branches/" + args[0]);
            if (checkBranch.exists()) {
                File head = new File(".simple-git/HEAD");
                String curBranch = "";
                try (BufferedReader br = new BufferedReader(new FileReader(head))) {
                    curBranch = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File branchFile = new File(".simple-git/branches/" + curBranch);
                String curCommit = "";
                try (BufferedReader br = new BufferedReader(new FileReader(branchFile))) {
                    curCommit = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String nwCommit = "";
                try (BufferedReader br = new BufferedReader(new FileReader(checkBranch))) {
                    nwCommit = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                reset(nwCommit);
                try (FileWriter fwH = new FileWriter(head); FileWriter fwB = new FileWriter(branchFile)) {
                    fwH.write(args[0]);
                    fwB.write(curCommit);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                softUpdateFromIndex(curCommit);
                return;
            } else {
                File checkCommit = new File(".simple-git/commits/" + args[0]);
                if (checkCommit.exists()) {
                    File detachedBranch = new File(".simple-git/branches/detached");
                    try (FileWriter fw = new FileWriter(detachedBranch)) {
                        fw.write(args[0]);
                    } catch (IOException e) {e.printStackTrace();
                        e.printStackTrace();
                    }
                    checkout(new String[]{"detached"});
                    return;
                }
            }
        }

        Path base = Paths.get("").toAbsolutePath();

        try {
            for (String filePath : args) {
                String pathFromCurDir = base.relativize(Paths.get(filePath).toAbsolutePath()).toString();
                File indexFile = new File(".simple-git/index/" + pathFromCurDir);
                recursiveAddFromIndex(pathFromCurDir.substring(0, pathFromCurDir.length() - indexFile.getName().length()), indexFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reset(String version) {
        File head = new File(".simple-git/HEAD");
        String curBranch = "master";
        try (BufferedReader br = new BufferedReader(new FileReader(head))) {
            curBranch = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File branchFile = new File(".simple-git/branches/" + curBranch);
        String curCommit = "";
        try (BufferedReader br = new BufferedReader(new FileReader(branchFile))) {
            curCommit = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Path basePath = Paths.get("").toAbsolutePath();
        try (BufferedReader br = new BufferedReader(new FileReader(info_))) {
            while (br.ready()) {
                if (br.readLine().equals("add")) {
                    String[] addedFiles = br.readLine().split(" ");
                    for (String fName : addedFiles) {
                            Path curPath = Paths.get(fName).toAbsolutePath();
                            String pathFromCurDir = basePath.relativize(curPath).toString();
                            recursiveRemove(new File(".simple-git/index/" +  pathFromCurDir));
                    }
                } else {
                    br.readLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            new FileWriter(info_).close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File curCommFile = new File(".simple-git/commits/" + curCommit);
        List<String> curCommHashes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(curCommFile))) {
            br.readLine();
            br.readLine();
            br.readLine();
            br.readLine();
            while (br.ready()) curCommHashes.add(br.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (curCommit.equals(version)) {
            updateFilesFromPath(".simple-git/index/", curCommHashes, "");
        } else {
            File nwCommFile = new File(".simple-git/commits/" + version);
            List<String> nwCommHashes = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(nwCommFile))) {
                br.readLine();
                br.readLine();
                br.readLine();
                br.readLine();
                while (br.ready()) nwCommHashes.add(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
            diffUpdateFilesFromPath(".simple-git/index/", curCommHashes, nwCommHashes, "");
            diffUpdateFilesFromPath(".simple-git/repo/", curCommHashes, nwCommHashes, "");
            try (FileWriter fw = new FileWriter(branchFile)) {
                fw.write(version);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void statusTree(List<String> adds, List<String> removes, UpdateTree tree) throws IOException, NoSuchAlgorithmException {
        Path basePath = Paths.get("").toAbsolutePath();
        for (String rm : removes) {
            String pathFromCurDir = basePath.relativize(Paths.get(rm).toAbsolutePath()).toString();
            File indexFile = new File(".simple-git/index/" + pathFromCurDir);
            File repoFile = new File(".simple-git/repo/" + pathFromCurDir);
            if (repoFile.exists() && !indexFile.exists()) {
                tree.addNode(rm, "rm");
            } else if (indexFile.exists() && indexFile.isDirectory()) {
                File[] listFiles = indexFile.listFiles();
                if (listFiles != null) {
                    statusTree(new ArrayList<>(), Arrays.stream(listFiles).map(File::getPath).collect(Collectors.toList()), tree);
                }
            }
        }
        for (String add : adds) {
            String pathFromCurDir = basePath.relativize(Paths.get(add).toAbsolutePath()).toString();
            File indexFile = new File(".simple-git/index/" + pathFromCurDir);
            File repoFile = new File(".simple-git/repo/" + pathFromCurDir);
            if (indexFile.exists() && !repoFile.exists()) {
                tree.addNode(pathFromCurDir, "add");
            } else if (indexFile.exists() && repoFile.exists()) {
                if (!indexFile.isDirectory()) {
                    if (repoFile.isDirectory()) {
                        tree.addNode(pathFromCurDir, "update");
                    } else {
                        String indexFileHash = getHashFromStream(new FileInputStream(indexFile), indexFile.getName());
                        String repoFileHash = getHashFromStream(new FileInputStream(repoFile), repoFile.getName());
                        if (!indexFileHash.equals(repoFileHash))
                            tree.addNode(pathFromCurDir, "update");
                    }
                } else {
                    if (!repoFile.isDirectory()) {
                        tree.addNode(pathFromCurDir, "update");
                    } else {
                        File[] listFiles = indexFile.listFiles();
                        if (listFiles != null)
                            statusTree(Arrays.stream(listFiles).map(File::getPath)
                                    .map(p -> p.substring(".simple-git/index/".length()))
                                    .collect(Collectors.toList()), new ArrayList<>(), tree);
                    }
                }
            }

        }
    }

    public void status() throws IOException {
        List<String> adds = new ArrayList<>();
        List<String> rms = new ArrayList<>();
        try (BufferedReader fr = new BufferedReader(new FileReader(info_))) {
            while (fr.ready()) {
                String line = fr.readLine();
                if (line.equals("add")) {
                    line = fr.readLine();
                    String[] ad = line.split(" ");
                    adds.addAll(Arrays.asList(ad));
                } else if (line.equals("remove")) {
                    line = fr.readLine();
                    String[] rm = line.split(" ");
                    rms.addAll(Arrays.asList(rm));
                }
            }
        }

        UpdateTree tree = new UpdateTree();
        try {
            statusTree(adds, rms, tree);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        File head = new File(".simple-git/HEAD");
        String currBranch = "";
        try (BufferedReader br = new BufferedReader(new FileReader(head))) {
            currBranch = br.readLine();
        }
        if (currBranch.equals("detached")) {
            File detachedBr = new File(".simple-git/branches/detached");
            try (BufferedReader br = new BufferedReader(new FileReader(detachedBr))) {
                System.out.println("branch detached, currently on commit " + br.readLine());
            }
        } else {
            System.out.println("on branch " + currBranch);
        }

        tree.printStatus();
    }

    public void deleteBranch(String name) {
        File branch = new File(".simple-git/branches/" + name);
        if (name.equals("master")) {
            System.out.println("could not delete master branch");
            return;
        }
        if (!branch.exists())
            System.out.println("Branch " + name + "does not exists");
        else {
            branch.delete();
            File head = new File(".simple-git/HEAD");
            assert(head.exists());
            String curBranch = "master";
            try (BufferedReader br = new BufferedReader(new FileReader(head))) {
                curBranch = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (curBranch.equals(name)) {
                try (FileWriter fw = new FileWriter(head)) {
                    fw.write("master");
                    fw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void createBranch(String name, String from) {
        File curBranch = new File(".simple-git/branches/" + from);
        File nwBranch = new File(".simple-git/branches/" + name);
        if (nwBranch.exists()) {
            System.out.println("branch " + name + " already exists");
            return;
        }
        try {
            nwBranch.createNewFile();
            try (BufferedReader br = new BufferedReader(new FileReader(curBranch)); FileWriter fw = new FileWriter(nwBranch)) {
                fw.write(br.readLine());
                fw.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createBranch(String name) {
        createBranch(name, "master");
    }

    public void createBranchAndSwitch(String name) {
        createBranch(name);
        checkout(new String[]{name});
    }
    public void createBranchAndSwitch(String name, String from) {
        createBranch(name, from);
        checkout(new String[]{name});
    }

    public void merge(String branch) {
        File head = new File(".simple-git/HEAD");
        String curBranch = "";
        try (BufferedReader br = new BufferedReader(new FileReader(head))) {
            curBranch = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (branch.equals(curBranch)) return;
        File curBranchFile = new File(".simple-git/branches/" + curBranch);
        File mergeBranchFile = new File(".simple-git/branches/" + branch);
        if (!mergeBranchFile.exists()) {
            System.out.println("branch " + branch + " does not exists");
            return;
        }
        String curCommitHash = "";
        String mergeCommitHash = "";
        try (BufferedReader brC = new BufferedReader(new FileReader(curBranchFile)); BufferedReader brM = new BufferedReader(new FileReader(mergeBranchFile))) {
            curCommitHash = brC.readLine();
            mergeCommitHash = brM.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (curCommitHash.equals(mergeCommitHash))
            return;
        if (checkLinear(curCommitHash, mergeCommitHash)) {
            checkout(new String[]{branch});
            try (FileWriter fwH = new FileWriter(head); FileWriter fwC = new FileWriter(curBranchFile)) {
                fwH.write(curBranch);
                fwC.write(mergeCommitHash);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File fstCommitFile = new File(".simple-git/commits/" + curCommitHash);
            File sndCommitFile = new File(".simple-git/commits/" + mergeCommitHash);
            List<String> fstHashes = new ArrayList<>();
            List<String> sndHashes = new ArrayList<>();

            try (BufferedReader brF = new BufferedReader(new FileReader(fstCommitFile));
                BufferedReader brS = new BufferedReader(new FileReader(sndCommitFile))) {
                for (int i = 0; i < 4; i++) {
                    brF.readLine();
                    brS.readLine();
                }
                while (brF.ready()) fstHashes.add(brF.readLine());
                while (brS.ready()) sndHashes.add(brS.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<String> mergedHashes = recursiveMergeHashesAndAddToFiles(fstHashes, sndHashes, curBranch, branch, "");

            try {
                String nwCommName = getHashFromStream(new ByteArrayInputStream(
                        mergedHashes.stream().reduce((a, b) -> a + b).orElse("").getBytes()), curCommitHash);
                File nwCommFile = new File(".simple-git/commits/" + nwCommName);

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy\nHH:mm:ss\n");
                try (FileWriter fw = new FileWriter(nwCommFile)) {
                    fw.write(dateFormat.format(new Date()));
                    fw.write("merge " + curBranch + " " + branch);
                    fw.write("\n");
                    fw.write(curCommitHash);
                    fw.flush();
                    for (String hash : mergedHashes) {
                        fw.write("\n");
                        fw.write(hash);
                    }
                    fw.flush();
                }
                File tmpBranch = new File(".simple-git/branches/tmpBranch");
                tmpBranch.createNewFile();

                try (FileWriter fwB = new FileWriter(tmpBranch)) {
                    fwB.write(nwCommName);
                }

                checkout(new String[]{"tmpBranch"});

                try (FileWriter fwB = new FileWriter(curBranchFile); FileWriter fwH = new FileWriter(head)) {
                    fwB.write(nwCommName);
                    fwH.write(curBranch);
                }
                tmpBranch.delete();

            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }


        }


    }

    private List<String> recursiveMergeHashesAndAddToFiles(List<String> fstHashes, List<String> sndHashes,
                                                           String fstBranchName, String sndBranchName,
                                                           String path) {
        if (path.length() > 0 && path.charAt(path.length() - 1) != '/') path += "/";
        List<String> resultHashes = new ArrayList<>();

        List<String> fstNames = new ArrayList<>();
        List<String> sndNames = new ArrayList<>();

        for (String hash : fstHashes) {
            File nameFile = new File(".simple-git/names/" + hash);
            try (BufferedReader br = new BufferedReader(new FileReader(nameFile))) {
                fstNames.add(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (String hash : sndHashes) {
            File nameFile = new File(".simple-git/names/" + hash);
            try (BufferedReader br = new BufferedReader(new FileReader(nameFile))) {
                sndNames.add(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < fstNames.size(); i++) {
            if (!sndNames.contains(fstNames.get(i))) {
                resultHashes.add(fstHashes.get(i));
            } else {
                String fstHashToMerge = fstHashes.get(i);
                int sndId = sndNames.indexOf(fstNames.get(i));
                String sndHashToMerge = sndHashes.get(sndId);
                sndNames.remove(sndId);
                sndHashes.remove(sndId);

                if (fstHashToMerge.equals(sndHashToMerge)) {
                    resultHashes.add(fstHashToMerge);
                    continue;
                }

                List<String> fstHashesPack = new ArrayList<>();
                List<String> sndHashesPack = new ArrayList<>();

                File fstHashFile = new File(".simple-git/files/" + fstHashToMerge);
                File sndHashFile = new File(".simple-git/files/" + sndHashToMerge);

                try (BufferedReader brF = new BufferedReader(new FileReader(fstHashFile));
                    BufferedReader brS = new BufferedReader(new FileReader(sndHashFile))) {
                    String lineF = brF.readLine();
                    String lineS = brS.readLine();
                    if (!lineF.equals("//directory//") || !lineS.equals("//directory//")) {
                        System.out.println("Conflict in files " + path + fstNames.get(i));
                        System.out.print("Write name of branch to take file from [" + fstBranchName + ", " + sndBranchName + "]: ");
                        String branchName = fstBranchName;
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                            branchName = br.readLine();
                            while (!branchName.equals(fstBranchName) && !branchName.equals(sndBranchName)) {
                                System.out.println("Wrong branch name!");
                                System.out.println();
                                System.out.print("Write name of branch to take file from [" + fstBranchName + ", " + sndBranchName + "]: ");
                                branchName = br.readLine();
                            }
                        }
                        if (branchName.equals(fstBranchName))
                            resultHashes.add(fstHashToMerge);
                        else
                            resultHashes.add(sndHashToMerge);
                    } else {
                        while (brF.ready()) fstHashesPack.add(brF.readLine());
                        while (brS.ready()) sndHashesPack.add(brS.readLine());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                List<String> currentResHashes = recursiveMergeHashesAndAddToFiles(fstHashesPack, sndHashesPack, fstBranchName, sndBranchName, path + fstNames.get(i));
                try {
                    String dirHash = getHashFromStream(new ByteArrayInputStream(
                            currentResHashes.stream().sorted().reduce((a, b) -> a + b).orElse("").getBytes()), fstNames.get(i));
                    File fl = new File(".simple-git/files/" + dirHash);
                    try (FileWriter fw = new FileWriter(fl)) {
                        fw.write("//directory//\n");
                        fw.write(currentResHashes.stream().reduce((a, b) -> a + "\n" + b).orElse(""));
                    }

                    File nameHash = new File(".simple-git/names/" + dirHash);
                    try (FileWriter fw = new FileWriter(nameHash)) {
                        fw.write(fstNames.get(i));
                    }
                    resultHashes.add(dirHash);
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }


            }
        }
        resultHashes.addAll(sndHashes);

        return resultHashes.stream().sorted().collect(Collectors.toList());
    }

    private boolean checkLinear(String fstCommit, String sndCommit) {
        if (sndCommit.equals(fstCommit)) return true;
        if (sndCommit.equals("")) return false;
        File fl = new File(".simple-git/commits/" + sndCommit);
        String prevSnd = "";
        try (BufferedReader br = new BufferedReader(new FileReader(fl))) {
            br.readLine();
            br.readLine();
            br.readLine();
            prevSnd = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return checkLinear(fstCommit, prevSnd);
    }

    public void log(String[] args) {
        if (args.length > 1) {
            logToCommit(args[1]);
        } else {
            File head = new File(".simple-git/HEAD");
            String branch = "";
            try (BufferedReader br = new BufferedReader(new FileReader(head))) {
                branch = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File branchFile = new File(".simple-git/branches/" + branch);
            String commNum = "";
            try (BufferedReader br = new BufferedReader(new FileReader(branchFile))) {
                commNum = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logToCommit(commNum);
        }
    }

    private void logToCommit(String commit) {
        while (!commit.equals("")) {
            File commFile = new File(".simple-git/commits/" + commit);
            System.out.println("commit " + commit);
            try (BufferedReader br = new BufferedReader(new FileReader(commFile))) {
                System.out.println("Date: " + br.readLine() + " " + br.readLine());
                System.out.println();
                System.out.println("        " + br.readLine());
                System.out.println();
                commit = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
