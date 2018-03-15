import org.junit.Test;
import static junit.framework.TestCase.*;

public class TrieImplTest {

    @Test
    public void test1() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertTrue(bor.contains("asd"));
        assertEquals(1, bor.size());
        assertTrue(bor.remove("asd"));
        assertEquals(0, bor.size());
        assertFalse(bor.contains("asd"));
    }
    @Test
    public void test2() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertTrue(bor.contains("asd"));
        assertEquals(1, bor.howManyStartsWithPrefix("a"));
        assertTrue(bor.remove("asd"));
        assertEquals(0, bor.size());
        assertEquals(0, bor.howManyStartsWithPrefix("a"));
    }
    @Test
    public void test3() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertFalse(bor.remove("as"));
        assertTrue(bor.contains("asd"));
        assertTrue(bor.remove("asd"));
        assertEquals(0, bor.howManyStartsWithPrefix("as"));
    }
    @Test
    public void test4() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertFalse(bor.add("asd"));
        assertEquals(1, bor.size());
        assertEquals(1, bor.howManyStartsWithPrefix("as"));
    }
    @Test
    public void test5() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertTrue(bor.add("asf"));
        assertEquals(2, bor.howManyStartsWithPrefix("as"));
        assertFalse(bor.remove("as"));
        assertEquals(2, bor.howManyStartsWithPrefix("as"));
        assertTrue(bor.remove("asd"));
        assertTrue(bor.remove("asf"));
        assertEquals(0, bor.size());
        assertEquals(0, bor.howManyStartsWithPrefix("as"));
    }
    @Test
    public void test6() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertTrue(bor.add("asfght"));
        assertEquals(2, bor.howManyStartsWithPrefix("as"));
        assertTrue(bor.remove("asd"));
        assertEquals(1, bor.howManyStartsWithPrefix("as"));
        assertTrue(bor.add("ask"));
        assertTrue(bor.remove("asfght"));
        assertEquals(1, bor.size());
        assertEquals(1, bor.howManyStartsWithPrefix("as"));
        assertEquals(0, bor.howManyStartsWithPrefix("asf"));
    }
}

