public class Dict implements Dictionary {
    private int size;
    private int mod;
    private final float loadFactor;
    private ListNode[] dictionary;

    public Dict() {
        size = 0;
        mod = 16;
        loadFactor = 0.75f;
        dictionary = new ListNode[mod];
    }

    public int size() {
        return size;
    }

    public boolean contains(String key) {
        return getCurNode(key) != null;
    }

    public String get(String key) {
        ListNode temp = getCurNode(key);
        return temp != null ? temp.value : null;
    }

    public String put(String key, String value) {
        ListNode temp = getCurNode(key);
        if (temp != null) {
            String prev = temp.value;
            temp.value = value;
            return prev;
        } else {
            int num = index(key);
            dictionary[num] = new ListNode(key, value, dictionary[num]);
            size++;
            if (size >= mod * loadFactor) {
                changeDict(false);
            }
            return null;
        }
    }

    public String remove(String key) {
        ListNode temp = getPrevNode(key);
        if (temp == null) {
            int num = index(key);
            if (dictionary[num] == null) {
                return null;
            } else if (dictionary[num].key.equals(key)) {
                String str = dictionary[num].value;
                dictionary[num] = dictionary[num].next;
                size--;
                if (size <= (mod / 2) * loadFactor * loadFactor && mod > 16) {
                    changeDict(true);
                }
                return str;
            } else {
                return null;
            }

        } else {
            String str = temp.next.value;
            temp.moveNext();
            size--;
            if (size <= (mod / 2) * loadFactor * loadFactor && mod > 16) {
                changeDict(true);
            }
            return str;
        }
    }

    private ListNode getPrevNode(String key) {
        int num = index(key);
        ListNode temp = dictionary[num];
        if (temp == null) {
            return null;
        }
        while (temp.next != null) {
            if (temp.next.key.equals(key)) {
                return temp;
            } else {
                temp = temp.next;
            }
        }
        return null;
    }

    private ListNode getCurNode(String key) {
        int num = index(key);
        ListNode temp = dictionary[num];
        if (temp == null) {
            return null;
        } else if (temp.key.equals(key)) {
            return temp;
        }
        temp = temp.next;
        while (temp != null) {
            if (temp.key.equals(key)) {
                return temp;
            } else {
                temp = temp.next;
            }
        }
        return null;
    }

    public void clear() {
        mod = 16;
        size = 0;
        dictionary = new ListNode[mod];
    }

    public int capacity() {
        return mod;
    }

    private void changeDict(boolean decrease) {
        int newSize = decrease ? mod / 2 : mod * 2;
        ListNode temp = null;
        for (int i = 0; i < mod; i++) {
            ListNode tmp = dictionary[i];
            while (tmp != null) {
                temp = new ListNode(tmp, temp);
                tmp = tmp.next;
            }
        }
        dictionary = new ListNode[newSize];
        size = 0;
        mod = newSize;
        while (temp != null) {
            put(temp.key, temp.value);
            temp = temp.next;
        }
    }

    private int index(String key) {
        int num = key.hashCode() % mod;
        return num < 0 ? mod + num : num;
    }


    private class ListNode {
        private String key;
        private String value;
        private ListNode next;
        ListNode(ListNode src, ListNode next) {
            key = src.key;
            value = src.value;
            this.next = next;
        }
        ListNode(String key, String value, ListNode next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
        private void moveNext() {
            next = next.next;
        }
    }

}
