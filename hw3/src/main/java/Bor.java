import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Bor implements Trie, StreamSerializable {
    final private Node root;
    public Bor() {
        root = new Node();
        root.count = 0;
    }
    public boolean add(String element) {
        if  (!contains(element)) {
            Node cur = root;
            root.count++;
            int i = 0;
            while (i < element.length()) {
                Node temp = cur.edges.get(element.charAt(i));
                if (temp == null) {
                    break;
                }
                cur = temp;
                cur.count++;
                i++;
            }
            while (i < element.length()) {
                cur.edges.put(element.charAt(i), new Node());
                cur = cur.edges.get(element.charAt(i));
                i++;
            }
            cur.isEnd = true;
            return true;
        }
        return false;
    }

    public boolean contains(String element) {
        Node cur = root;
        for (int i = 0; i < element.length(); i++) {
            cur = cur.edges.get(element.charAt(i));
            if (cur == null) {
                return false;
            }
        }
        return cur.isEnd;
    }

    public boolean remove(String element) {
        if (contains(element)) {
            Node cur = root;
            root.count--;
            for (int i = 0; i < element.length(); i++) {
                Node temp = cur.edges.get(element.charAt(i));
                if (temp.count > 1) temp.count--;
                else {
                    cur.edges.remove(element.charAt(i));
                    return true;
                }
                cur = temp;
            }
            cur.isEnd = false;
            return true;
        }
        return false;
    }

    public int size() {
        return root.count;
    }

    public int howManyStartsWithPrefix(String prefix) {
        Node cur = root;
        for (int i = 0; i < prefix.length(); i++) {
            cur = cur.edges.get(prefix.charAt(i));
            if (cur == null) return 0;
        }
        return cur.count;
    }

    public void serialize(OutputStream out) throws IOException {
        root.serialize(out);
    }

    public void deserialize(InputStream in) throws IOException {
        root.deserialize(in);
    }

    private class Node {
        private boolean isEnd;
        private int count;
        final private Map<Character, Node> edges;
        private Node() {
            isEnd = false;
            count = 1;
            edges = new HashMap<Character, Node>();
        }
        private void serialize(OutputStream out) throws IOException {
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeInt(count);
            dataOut.writeBoolean(isEnd);
            for (Map.Entry<Character, Node> edge : edges.entrySet()) {
                dataOut.writeChar(edge.getKey());
                edge.getValue().serialize(dataOut);
            }
        }

        private void deserialize(InputStream in) throws IOException {
            DataInputStream dataIn = new DataInputStream(in);
            count = dataIn.readInt();
            isEnd = dataIn.readBoolean();
            if (!isEnd || count > 1) {
                for (int i = isEnd ? 1 : 0; i < count;) {
                    char ch = dataIn.readChar();
                    Node nd = new Node();
                    nd.deserialize(dataIn);
                    i += nd.count;
                    edges.put(ch, nd);
                }
            }
        }
    }
}
