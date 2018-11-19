import org.junit.Test;

import java.util.function.Supplier;
import static junit.framework.TestCase.*;

public class LightFutureImplTest {
    @Test
    public void runGetTest() throws LightExecutionException, InterruptedException {
        Supplier<Integer> supl = () -> 1;
        LightFutureImpl<Integer> lf1 = new LightFutureImpl<>(supl);
        assertFalse(lf1.isReady());
        lf1.run();
        assertEquals(lf1.get(), (Integer)1);
        assertTrue(lf1.isReady());
    }
    @Test
    public void exceptionTest() {
        Supplier<Integer> sup = () -> 1/0;
        LightFutureImpl<Integer> lf= new LightFutureImpl<>(sup);
        lf.run();
        try {
            lf.get();
        } catch (Exception e) {
            assertTrue(e instanceof LightExecutionException);
        }
        assertFalse(lf.isReady());
    }
}
