import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static junit.framework.TestCase.*;

public class BorTests {

    @Test
    public void simpleAddRemove() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertTrue(bor.contains("asd"));
        assertEquals(1, bor.size());
        assertTrue(bor.remove("asd"));
        assertEquals(0, bor.size());
        assertFalse(bor.contains("asd"));
    }
    @Test
    public void prefixTest() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertTrue(bor.contains("asd"));
        assertEquals(1, bor.howManyStartsWithPrefix("a"));
        assertTrue(bor.remove("asd"));
        assertEquals(0, bor.size());
        assertEquals(0, bor.howManyStartsWithPrefix("a"));
    }
    @Test
    public void noExistsRemove() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertFalse(bor.remove("as"));
        assertTrue(bor.contains("asd"));
        assertTrue(bor.remove("asd"));
        assertEquals(0, bor.howManyStartsWithPrefix("as"));
    }
    @Test
    public void doubleAdd() {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertFalse(bor.add("asd"));
        assertEquals(1, bor.size());
        assertEquals(1, bor.howManyStartsWithPrefix("as"));
    }
    @Test
    public void prefixRemoveTry() {
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
    public void prefixRemove() {
        Bor bor = new Bor();
        assertTrue(bor.add("asf"));
        assertTrue(bor.add("asfght"));
        assertTrue(bor.remove("asf"));
        assertFalse(bor.contains("asf"));
        assertTrue(bor.contains("asfght"));
    }

    @Test
    public void emptySerializeTest() throws IOException {
        Bor bor = new Bor();
        assertEquals(0, bor.size());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bor.serialize(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Bor other = new Bor();
        other.deserialize(in);
        assertEquals(0, other.size());

    }

    @Test
    public void simpleSerializeTest() throws IOException {
        Bor bor = new Bor();
        assertTrue(bor.add("asd"));
        assertTrue(bor.add("asf"));
        assertEquals(2, bor.size());
        assertEquals(2, bor.howManyStartsWithPrefix("as"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bor.serialize(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Bor other = new Bor();
        other.deserialize(in);
        assertEquals(2, other.size());
        assertEquals(2, other.howManyStartsWithPrefix("as"));
        assertTrue(other.contains("asd"));
        assertTrue(other.contains("asf"));
        assertFalse(other.contains("as"));
    }

    @Test
    public void depthSerializeTest() throws IOException {
        Bor bor = new Bor();
        assertTrue(bor.add("a"));
        assertTrue(bor.add("as"));
        assertTrue(bor.add("asd"));
        assertTrue(bor.add("asdf"));
        assertEquals(4, bor.size());
        assertEquals(3, bor.howManyStartsWithPrefix("as"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bor.serialize(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Bor other = new Bor();
        other.deserialize(in);
        assertEquals(4, bor.size());
        assertEquals(3, bor.howManyStartsWithPrefix("as"));
        assertTrue(other.contains("a"));
        assertTrue(other.contains("as"));
        assertTrue(other.contains("asd"));
        assertTrue(other.contains("asdf"));
    }

    @Test
    public void breadthSerializeTest() throws IOException {
        Bor bor = new Bor();
        assertTrue(bor.add("af"));
        assertTrue(bor.add("as"));
        assertTrue(bor.add("asd"));
        assertTrue(bor.add("asf"));
        assertTrue(bor.add("asg"));
        assertTrue(bor.add("ask"));
        assertTrue(bor.add("asfdfg"));
        assertTrue(bor.add("at"));
        assertTrue(bor.add("ar"));
        assertEquals(9, bor.size());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bor.serialize(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Bor other = new Bor();
        other.deserialize(in);
        assertEquals(9, bor.size());
        assertTrue(other.contains("af"));
        assertTrue(other.contains("as"));
        assertTrue(other.contains("asd"));
        assertTrue(other.contains("asf"));
        assertTrue(other.contains("asg"));
        assertTrue(other.contains("ask"));
        assertTrue(other.contains("asfdfg"));
        assertTrue(other.contains("at"));
        assertTrue(other.contains("ar"));
    }

}
