import org.junit.Test;

import java.io.IOException;

public class TorrentClientTests {

    @Test
    public void closeTest() {
        TorrentTracker tt = new TorrentTracker();
        Thread th1 = new Thread(tt::listener);
        th1.start();
        TorrentClient tc = new TorrentClient((short)8082);
        Thread th2 = new Thread(tc::listener);
        th2.start();
        tt.close();
        tc.close();
        try {
            th1.join();
            th2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() {
        TorrentTracker tt = new TorrentTracker();
        Thread th1 = new Thread(tt::listener);
        th1.start();
        TorrentClient tc1 = new TorrentClient((short)8082);
        Thread th2 = new Thread(tc1::listener);
        th2.start();

        TorrentClient tc2 = new TorrentClient((short)8083);

        try {
            tc1.upload("src/main/java/Bethoven.flac");
            tc1.update();
            tc2.stat("localhost", "8082", "86");
            for (int i = 0; i < 35; i++) {
                tc2.get("localhost", "8082", "86", "" + i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        tt.close();
        tc1.close();
        tc2.close();
        try {
            th1.join();
            th2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
