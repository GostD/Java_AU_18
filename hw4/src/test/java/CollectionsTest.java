import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.*;

public class CollectionsTest {

    @Test
    public void mapTest() {
        Set<Integer> base = new HashSet<>();
        assertEquals(Collections.map(a -> a, base), java.util.Collections.emptyList());
        base.add(1);
        base.add(2);
        base.add(3);
        assertEquals(Collections.map(a -> a + 1, base), Arrays.asList(2, 3, 4));
        assertEquals(Collections.map(a -> Integer.toString(a), base), Arrays.asList("1", "2", "3"));
    }

    @Test
    public void testFilter() {
        assertEquals(Collections.filter(a -> a > 0, Arrays.asList(-1, 0, 2, 1)), Arrays.asList(2, 1));
        assertEquals(Collections.filter(a -> a % 2 == 0, Arrays.asList(-1, 0, 2, 1)), Arrays.asList(0, 2));
        assertEquals(Collections.filter(Objects::isNull, Arrays.asList(2, null, null)), Arrays.asList(null, null));
    }

    @Test
    public void testTakeWhileUnless() {
        assertEquals(Collections.takeWhile(a -> a > 0, Arrays.asList(1,2,3,0,-1,-3)), Arrays.asList(1,2,3));
        assertEquals(Collections.takeWhile(Objects::nonNull, Arrays.asList(1, 2, 3, null, 5, 6)), Arrays.asList(1, 2, 3));
        assertEquals(Collections.takeWhile(((Predicate<Integer>)(a -> a > 0)).and(a -> a < 5),
                Arrays.asList(1, 2, 3, 5, 7, 8)), Arrays.asList(1, 2, 3));
        assertEquals(Collections.takeUntil(a -> a > 0, Arrays.asList(-1,-2,-3,0,1,3)), Arrays.asList(-1, -2, -3, 0));
        assertEquals(Collections.takeUntil(String::isEmpty, Arrays.asList("A", "B", "")),
                Arrays.asList("A", "B"));
    }

    @Test
    public void foldrTest() {
        assertEquals(2, (int)Collections.foldr((a, b) -> a - b, Arrays.asList(5,3,1), 1));
        assertEquals(5, (int)Collections.foldr((a, b) -> a, Arrays.asList(5,3,1), 1));
        assertEquals(0, (int)Collections.foldr((a, b) -> b, Arrays.asList(5,3,1), 0));
    }

    @Test
    public void foldlTest() {
        assertEquals(-1, (int)Collections.foldl((a, b) -> a - b, Arrays.asList(3,2,1), 5));
        assertEquals(1, (int)Collections.foldl((a, b) -> a, Arrays.asList(5,3,7), 1));
        assertEquals(7, (int)Collections.foldl((a, b) -> b, Arrays.asList(5,3,7), 1));
        assertEquals((int)Collections.foldr((a, b) -> a + b, Arrays.asList(5,3,1), 7),
                (int)Collections.foldl((a, b) -> a + b, Arrays.asList(5,3,1), 7));
    }

}
