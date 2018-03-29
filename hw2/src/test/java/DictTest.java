import org.junit.Test;
import static junit.framework.TestCase.*;

public class DictTest {

    @Test
    public void emptyDict() {
        Dict dict = new Dict();
        assertEquals(dict.size(), 0);
    }
    @Test
    public void addRemove() {
        Dict dict = new Dict();
        dict.put("abc", "abc");
        assertTrue(dict.contains("abc"));
        assertEquals(dict.size(), 1);
        dict.put("efg", "efg");
        assertTrue(dict.contains("efg"));
        assertEquals(dict.size(), 2);
        assertEquals(dict.remove("abc"), "abc");
        assertEquals(dict.size(), 1);
    }
    @Test
    public void simpleOverloadTest() {
        Dict dict = new Dict();
        for (int i = 0; i < 11; i++) {
            dict.put("abc" + i, "abc" + i);
        }
        assertEquals(dict.size(), 11);
        assertEquals(dict.capacity(), 16);
        dict.put("abc11", "abc11");
        assertEquals(dict.size(), 12);
        assertEquals(dict.capacity(), 32);
        for (int i = 0; i < 12; i++) {
            assertTrue(dict.contains("abc" + i));
        }
    }

    @Test
    public void emptyRemove() {
        Dict dict = new Dict();
        dict.remove("abc");
        assertEquals(dict.size(), 0);
    }

    @Test
    public void doubleRemove() {
        Dict dict = new Dict();
        dict.put("abc", "abc");
        dict.put("efg", "efg");
        assertEquals(dict.remove("efg"), "efg");
        assertEquals(dict.size(), 1);
        assertEquals(dict.remove("efg"), null);
        assertEquals(dict.size(), 1);
    }

    @Test
    public void clearTest() {
        Dict dict = new Dict();
        dict.put("abc", "abc");
        dict.put("efg", "efg");
        assertEquals(dict.size(), 2);
        dict.clear();
        assertEquals(dict.size(), 0);
        assertEquals(dict.capacity(), 16);
        assertFalse(dict.contains("abc"));
        assertFalse(dict.contains("efg"));
    }

    @Test
    public void clearRemovePut() {
        Dict dict = new Dict();
        dict.put("abc", "abc");
        dict.put("efg", "efg");
        dict.clear();
        assertEquals(dict.remove("abc"), null);
        dict.put("abc", "abc");
        assertEquals(dict.size(), 1);
        assertTrue(dict.contains("abc"));
    }

    @Test
    public void overloadTest() {
        Dict dict = new Dict();
        int cap = 16;
        int iMax = 12;
        int curNum = 0;
        for (int j = 1; j < 4; j++) {
            for (int i = 0; i < iMax; i++) {
                dict.put("abc" + j + i, "abc" + j + i);
                curNum++;
            }
            assertEquals(dict.size(), curNum);
            cap *= 2;
            iMax *= 2;
            assertEquals(dict.capacity(), cap);
        }
        iMax /= 2;
        cap /= 2;
        for (int j = 3; j > 1; j--) {
            for (int i = 0; i < iMax; i++) {
                dict.remove("abc" + j + i);
                curNum--;
            }
            assertEquals(dict.size(), curNum);
            assertEquals(dict.capacity(), cap);
            cap /= 2;
            iMax /= 2;
        }
    }
    @Test
    public void multiRemove() {
        Dict dict = new Dict();
        dict.put("abc1", "abc1");
        dict.remove("abc1");
        assertEquals(dict.size(), 0);
        for (int i = 0; i < 32; i++) {
            dict.remove("abc" + i);
            assertEquals(dict.size(), 0);
            assertEquals(dict.capacity(), 16);
        }
    }

    @Test
    public void reWriteValues() {
        Dict dict = new Dict();
        for (int i = 0; i < 35; i++) {
            dict.put("abc" + i, "abc" + i);
        }
        assertEquals(dict.size(), 35);
        assertEquals(dict.capacity(), 64);
        assertTrue(dict.contains("abc0"));
        assertEquals(dict.get("abc0"), "abc0");
        for (int i = 0; i < 35; i++) {
            dict.put("abc" + i, "abc" + 9 + i);
            assertEquals(dict.size(), 35);
        }
        assertEquals(dict.capacity(), 64);
        assertTrue(dict.contains("abc0"));
        assertEquals(dict.get("abc0"), "abc90");
    }

}
