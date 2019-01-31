import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UpdateTree {
    private List<Node> rootNodes;
    UpdateTree() {
        rootNodes = new ArrayList<>();
    }

    public boolean isEmpty() {
        return rootNodes.isEmpty();
    }

    void addNode(String path, String type) {
        String[] pathNames = path.split("/");
        Node curNode = null;
        for (Node nod : rootNodes) {
            if (nod.name.equals(pathNames[0])) {
                curNode = nod;
                break;
            }
        }
        if (curNode == null) {
            if (pathNames.length > 1)
                curNode = new Node(pathNames[0], "other");
            else
                curNode = new Node(pathNames[0], type);
            rootNodes.add(curNode);
        }
        for (int i = 1; i < pathNames.length; i++) {
            List<Node> childs = curNode.getChildrens();
            boolean nodeFound = false;
            for (Node child : childs) {
                if (child.name.equals(pathNames[i])) {
                    nodeFound = true;
                    curNode = child;
                    if (i == pathNames.length - 1) curNode = new Node(pathNames[i], type);
                    break;
                }
            }
            if (!nodeFound) {
                if (i != pathNames.length - 1) {
                    Node tmp = new Node(pathNames[i], "other");
                    curNode.childrens.add(tmp);
                    curNode = tmp;
                } else {
                    curNode.childrens.add(new Node(pathNames[i], type));
                }
            }
        }
    }

    public List<String> updateRootHashes(List<String> curHashes, FileSystem fs) throws IOException, NoSuchAlgorithmException {
        List<String> hashes = new ArrayList<>();
        List<String> curNames = new ArrayList<>();
        for (String hash : curHashes) {
            File fl = new File(".simple-git/names/" + hash);
            try (BufferedReader br = new BufferedReader(new FileReader(fl))) {
                String name = br.readLine();
                curNames.add(name);
            } catch (IOException e) {
                System.out.println("copy of file is missing");
            }
        }
        for (Node nod : rootNodes) {
            if (curNames.contains(nod.name)) {
                String nwHash = recursiveRecountHash(curHashes.get(curNames.indexOf(nod.name)), nod, "", fs);
                if (!nwHash.equals(""))
                    hashes.add(nwHash);
                curHashes.remove(curNames.indexOf(nod.name));
                curNames.remove(nod.name);
            } else {
                String nwHash = recursiveRecountHash("", nod, "", fs);
                if (!nwHash.equals("")) hashes.add(nwHash);
            }
        }
        hashes.addAll(curHashes);
        return hashes.stream().sorted().collect(Collectors.toList());
    }

    public String recursiveRecountHash(String curHash, Node curNode, String curPath, FileSystem fs) throws IOException, NoSuchAlgorithmException {
        String resHash = "";
        if (curPath.length() > 0 && curPath.charAt(curPath.length() - 1) != '/') curPath += "/";
        if (curHash.equals("")) {
            String nwHash = fs.getHash(new File(".simple-git/index/" + curPath + curNode.name));
            fs.updateFilesFromPath(".simple-git/repo/", Arrays.asList(nwHash), curPath);
            return nwHash;
        }

        if (curNode.type == Node.NodeType.OTHER) {
            File curHashFile = new File(".simple-git/files/" + curHash);
            File names = fs.getNames_();
            List<String> updatedHashes = new ArrayList<>();
            boolean hashChanges = false;
            List<String> hashesFromFile = new ArrayList<>();
            List<String> correspondingNames = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(curHashFile))) {
                String hash = br.readLine();
                assert(hash.equals("//directory//"));
                while (br.ready()) {
                    hash = br.readLine();

                    File nameFile = new File(names.getCanonicalPath() + "/" + hash);

                    try (BufferedReader nr = new BufferedReader(new FileReader(nameFile))) {
                        String name = nr.readLine();
                        hashesFromFile.add(hash);
                        correspondingNames.add(name);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<String> curHashes = new ArrayList<>(hashesFromFile);

            for (Node child : curNode.getChildrens()) {
                if (child.type == Node.NodeType.RM) {
                    hashChanges = true;
                    hashesFromFile.remove(correspondingNames.indexOf(child.name));
                    correspondingNames.remove(child.name);
                } else if (child.type == Node.NodeType.ADD) {
                    String nwHash = fs.getHash(new File(".simple-git/index/" + curPath + curNode.name + "/" + child.name));
                    updatedHashes.add(nwHash);
                    hashChanges = true;
                } else {
                    for (int i = 0; i < correspondingNames.size(); i++) {
                        String name = correspondingNames.get(i);
                        if (child.name.equals(name)) {
                            String hash = hashesFromFile.get(i);
                            String nwHash = recursiveRecountHash(hash, child, curPath + curNode.name, fs);//+ "/" + child.name//TODO: check this
                            if (!hash.equals(nwHash)) {
                                hashChanges = true;
                            }
                            updatedHashes.add(nwHash);
                            correspondingNames.remove(i);
                            hashesFromFile.remove(i);
                            break;
                        }
                    }
                }

            }


            if (hashChanges) {
                hashesFromFile = hashesFromFile.stream().filter(Objects::nonNull).collect(Collectors.toList());

                updatedHashes.addAll(hashesFromFile);

                String nwHash = fs.getHashFromStream(new ByteArrayInputStream(
                        updatedHashes.stream().sorted().reduce((a, b) -> a + b).orElse("").getBytes()), curNode.name);
                File fl = new File(".simple-git/files/" + nwHash);
                try (FileWriter fw = new FileWriter(fl)) {
                    fw.write("//directory//\n");
                    fw.write(updatedHashes.stream().reduce((a, b) -> a + "\n" + b).orElse(""));
                }

                File nameHash = new File(".simple-git/names/" + nwHash);
                try (FileWriter fw = new FileWriter(nameHash)) {
                    fw.write(curNode.name);
                }

                fs.diffUpdateFilesFromPath(".simple-git/repo/", curHashes, updatedHashes, curPath +  curNode.name + "/");

                return nwHash;
            } else {
                return curHash;
            }

        } else if (curNode.type == Node.NodeType.UPDATE || curNode.type == Node.NodeType.ADD) {
            String nwHash = fs.getHash(new File(".simple-git/index/" + curPath + curNode.name));
            if (!nwHash.equals(curHash)) {
                fs.diffUpdateFilesFromPath(".simple-git/repo/", Arrays.asList(curHash), Arrays.asList(nwHash), curPath);
            }
            return nwHash;
        }

        return resHash;
    }

    public void printStatus() {
        for (Node nod : rootNodes) {
            nod.recursiveStatus("");
        }
    }

    private static class Node {
        Node(String name, String type) {
            this.name = name;
            if (type.equals("add"))
                this.type = NodeType.ADD;
            else if (type.equals("rm"))
                this.type = NodeType.RM;
            else if (type.equals("update"))
                this.type = NodeType.UPDATE;
            else
                this.type = NodeType.OTHER;
            childrens = new ArrayList<>();
        }
        private void recursiveStatus(String path) {
            if (this.type != NodeType.OTHER) {
                if (type == NodeType.ADD) {
                    System.out.print("added: ");
                } else if (type == NodeType.UPDATE) {
                    System.out.print("updated: ");
                } else {
                    System.out.print("removed: ");
                }
                System.out.println(path + name);
            } else {
                String nwPath = path + name + "/";
                for (Node nod : childrens) {
                    nod.recursiveStatus(nwPath);
                }
            }
        }
        private List<Node> getChildrens() { return childrens; }
        private enum NodeType { ADD, RM, UPDATE, OTHER };
        private NodeType type;
        private String name;
        private List<Node> childrens;
    }

}
