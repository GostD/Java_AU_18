import org.junit.Test;

import java.util.Random;

import static junit.framework.TestCase.*;

public class FunctionTest {

    private class CheckBox {
        private boolean fst = false;
        private boolean snd = false;
        void reset() {
            fst = false;
            snd = false;
        }
    }

    @Test
    public void funOneApply() {
        Function1<Integer, Integer> plusOne = a -> a + 1;
        assertEquals(13, (int) plusOne.apply(12));
    }

    @Test
    public void composeTwoOneFunc() {
        Function1<Integer, Integer> plusOne = a -> a + 1;
        Function1<Integer, Integer> plusTwo = plusOne.compose(plusOne);
        assertEquals(13, (int) plusTwo.apply(11));
    }

    @Test
    public void funTwoApply() {
        Function2<Integer, Integer, Integer> sum = (a, b) -> a + b;
        assertEquals(13, (int) sum.apply(7, 6));
    }

    @Test
    public void funTwoCompose() {
        Function2<Integer, Integer, Integer> sum = (a, b) -> a + b;
        Function1<Integer, Integer> multTwo = a -> 2*a;
        Function2<Integer, Integer, Integer> multSumOnTwo = sum.compose(multTwo);
        assertEquals(20, (int) multSumOnTwo.apply(3, 7));
    }

    @Test
    public void bindTwoTest() {
        Function2<Integer, Integer, Integer> minus = (a, b) -> a - b;
        Function1<Integer, Integer> minusTwo = minus.bind2(2);
        assertEquals(13, (int) minusTwo.apply(15));
    }

    @Test
    public void bindOneTest() {
        Function2<Integer, Integer, Integer> minus = (a, b) -> a - b;
        Function1<Integer, Integer> fifteenMinus = minus.bind1(15);
        assertEquals(13, (int) fifteenMinus.apply(2));
    }

    @Test
    public void curry() {
        Function2<Integer, Integer, Integer> minus = (a, b) -> a - b;
        assertEquals(13, (int) (minus.curry().apply(15)).apply(2));
    }

    @Test
    public void checkCheckBox() {
        CheckBox cb = new CheckBox();
        assertFalse(cb.fst);
        assertFalse(cb.snd);
        cb.fst = true;
        cb.snd = true;
        cb.reset();
        assertFalse(cb.fst);
        assertFalse(cb.snd);
    }

    @Test
    public void predAndTest() {
        CheckBox cb = new CheckBox();

        Predicate<Integer> positive = a -> {cb.fst = true; return a > 0;};
        Predicate<Integer> greaterThanTwo = a -> {cb.snd = true; return a > 2;};
        assertTrue(positive.and(greaterThanTwo).apply(5));
        assertTrue(cb.fst);
        assertTrue(cb.snd);
        cb.reset();

        assertFalse(positive.and(greaterThanTwo).apply(1));
        assertTrue(cb.fst);
        assertTrue(cb.snd);
        cb.reset();

        assertFalse(positive.and(greaterThanTwo).apply(0));
        assertTrue(cb.fst);
        assertFalse(cb.snd);
        cb.reset();

        Predicate<Object> notNull = a -> {cb.snd = true; return a != null;};
        assertFalse(Predicate.ALWAYS_TRUE.and(notNull).apply(null));
        assertTrue(cb.snd);
        cb.reset();

        assertFalse(Predicate.ALWAYS_FALSE.and(notNull).apply(null));
        assertFalse(cb.snd);
    }

    @Test
    public void predOrTest() {
        CheckBox cb = new CheckBox();

        Predicate<Integer> positive = a -> {cb.fst = true; return a > 0;};
        Predicate<Integer> greaterThanTwo = a -> {cb.snd = true; return a > 2;};

        assertTrue(positive.or(greaterThanTwo).apply(5));
        assertTrue(cb.fst);
        assertFalse(cb.snd);
        cb.reset();

        assertTrue(positive.or(greaterThanTwo).apply(1));
        assertTrue(cb.fst);
        assertFalse(cb.snd);
        cb.reset();

        assertFalse(positive.or(greaterThanTwo).apply(0));
        assertTrue(cb.fst);
        assertTrue(cb.snd);
        cb.reset();

        Predicate<Object> notNull = a -> {cb.snd = true; return a != null;};
        assertTrue(Predicate.ALWAYS_TRUE.or(notNull).apply(null));
        assertFalse(cb.snd);
        cb.reset();

        assertFalse(Predicate.ALWAYS_FALSE.or(notNull).apply(null));
        assertTrue(cb.snd);
    }

    @Test
    public void predNotTest() {
        Predicate<Integer> positive = a -> a > 0;
        assertTrue(positive.apply(5));
        assertTrue(positive.not().apply(0));
    }

    @Test
    public void randPredTest() {
        Predicate<Double> greaterThanDotThree = a -> a > 0.3;
        Predicate<Double> greaterThanDotSeven = a -> a > 0.7;
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            Double temp = rnd.nextDouble();
            assertEquals((boolean) greaterThanDotThree.and(greaterThanDotSeven).apply(temp),
                    !(greaterThanDotThree.not().or(greaterThanDotSeven.not())).apply(temp));
        }
    }

    @Test
    public void alwaysTrueFalse() {
        Predicate<Integer> positive = a -> a > 0;
        assertTrue(positive.ALWAYS_TRUE.apply(-1));
        assertTrue(positive.ALWAYS_TRUE.apply("false"));
        assertFalse(positive.ALWAYS_FALSE.apply(13));
        assertFalse(positive.ALWAYS_FALSE.apply("true"));
    }
}
