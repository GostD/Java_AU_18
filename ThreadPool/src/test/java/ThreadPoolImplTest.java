import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.*;

public class ThreadPoolImplTest {
    @Test
    public void emptyPoolShutdown() throws InterruptedException {
        for (int i = 1; i < 100; ++i) {
            ThreadPoolImpl thP = new ThreadPoolImpl(i);
            thP.shutdown();
        }
    }
    @Test
    public void poolShutdown() throws InterruptedException {
        for (int i = 1; i < 100; ++i) {
            ThreadPoolImpl thP = new ThreadPoolImpl(i);
            for (int j = 0; j < i; j++) thP.submit(() -> 1);
            thP.shutdown();
        }
    }

    @Test
    public void numThreads() throws InterruptedException, LightExecutionException {
        for (int i = 1; i < 100; ++i) {
            ThreadPoolImpl thP = new ThreadPoolImpl(i);
            Set<String> threadNames = new HashSet<>();
            int threshold = 100*i*i;
            int count = 0;
            while (threadNames.size() < i) {
                if (count > threshold) fail();
                threadNames.add(thP.submit(() -> Thread.currentThread().getName()).get());
                count++;
            }

            thP.shutdown();
        }
    }

    @Test
    public void getCheck() throws LightExecutionException, InterruptedException {
        ThreadPoolImpl thP = new ThreadPoolImpl(12);
        List<LightFuture> futures = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            futures.add(thP.submit(() -> 1));
        }
        for (int i = 0; i < 500; i++) {
            assertEquals(futures.get(i).get(), 1);
        }
        thP.shutdown();
    }

    @Test
    public void incompleteFuturesAfterShutdown() throws InterruptedException, LightExecutionException {
        for (int j = 0; j < 200; ++j) {
            ThreadPoolImpl thP = new ThreadPoolImpl(10);
            List<LightFuture> futures = new ArrayList<>();
            for (int i = 0; i < 5000; i++) {
                futures.add(thP.submit(() -> {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {return 1;}
                    return 1;}));
            }
            thP.shutdown();
            int countedFutures = 0;
            for (LightFuture fut : futures) {
                if (fut.isReady()) {
                    assertEquals(fut.get(), 1);
                    countedFutures += 1;
                }
            }
            assertTrue(countedFutures < futures.size());
        }
    }

    @Test
    public void exceptionInTask() {
        ThreadPoolImpl thP = new ThreadPoolImpl(10);
        LightFuture lf = thP.submit(() -> 1/0);
        try {
            lf.get();
        } catch (Exception e) {
            assertTrue(e instanceof LightExecutionException);
        }
    }

}
