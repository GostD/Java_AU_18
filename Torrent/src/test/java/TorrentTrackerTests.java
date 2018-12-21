import org.junit.Test;

import java.io.IOException;
import java.net.Socket;

import static junit.framework.TestCase.*;

public class TorrentTrackerTests {


    @Test
    public void closeTest() {
        TorrentTracker tt = new TorrentTracker();
        Thread th = new Thread(tt::listener);
        th.start();
        tt.close();
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void uploadTest() {
        TorrentTracker tt = new TorrentTracker();
        Thread th = new Thread(tt::listener);
        th.start();
        int countBeforeUpload = 0;
        String[][] idNameSizeBeforeUpload = new String[0][];
        try (SocketInfo sc = new SocketInfo(new Socket("localhost", 8081))) {
            sc.os.writeByte(1);
            sc.os.flush();
            countBeforeUpload = sc.is.readInt();
            idNameSizeBeforeUpload = new String[countBeforeUpload][3];
            for (int i = 0; i < countBeforeUpload; i++) {
                idNameSizeBeforeUpload[i][0] = ((Integer)sc.is.readInt()).toString();
                idNameSizeBeforeUpload[i][1] = sc.is.readUTF();
                idNameSizeBeforeUpload[i][2] = ((Long)sc.is.readLong()).toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int lastUploadId = 0;
        try (SocketInfo sc = new SocketInfo(new Socket("localhost", 8081))) {
            assertTrue(sc.socket.isConnected());
            sc.os.writeByte(2);
            sc.os.flush();
            sc.os.writeUTF("name1");
            sc.os.writeLong(123L);
            sc.os.flush();
            lastUploadId = sc.is.readInt();

        } catch (IOException e) {
            e.printStackTrace();
        }

        int countAftereUpload = 0;
        String[][] idNameSizeAfterUpload = new String[0][];
        try (SocketInfo sc = new SocketInfo(new Socket("localhost", 8081))) {
            sc.os.writeByte(1);
            sc.os.flush();
            countAftereUpload = sc.is.readInt();
            assertEquals(countAftereUpload, countBeforeUpload + 1);
            idNameSizeAfterUpload = new String[countAftereUpload][3];
            for (int i = 0; i < countAftereUpload; i++) {
                idNameSizeAfterUpload[i][0] = ((Integer)sc.is.readInt()).toString();
                idNameSizeAfterUpload[i][1] = sc.is.readUTF();
                idNameSizeAfterUpload[i][2] = ((Long)sc.is.readLong()).toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < countBeforeUpload; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(idNameSizeAfterUpload[i][j], idNameSizeBeforeUpload[i][j]);
            }
        }

        assertEquals(idNameSizeAfterUpload[countAftereUpload - 1][0], ((Integer)lastUploadId).toString());
        assertEquals(idNameSizeAfterUpload[countAftereUpload - 1][1], "name1");
        assertEquals(idNameSizeAfterUpload[countAftereUpload - 1][2], "123");

        tt.close();
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sourcesUpdateTest() {

        TorrentTracker tt = new TorrentTracker();
        Thread th = new Thread(tt::listener);
        th.start();

        int lastUploadId = 0;
        String ipSc = "";
        try (SocketInfo sc = new SocketInfo(new Socket("localhost", 8081))) {
            assertTrue(sc.socket.isConnected());
            byte[] ipScBytes = sc.socket.getInetAddress().getAddress();
            assertEquals(ipScBytes.length, 4);
            ipSc = ipScBytes[0] + "." + ipScBytes[1] + "." + ipScBytes[2] + "." + ipScBytes[3];
            sc.os.writeByte(2);
            sc.os.flush();
            sc.os.writeUTF("name1");
            sc.os.writeLong(123L);
            sc.os.flush();
            lastUploadId = sc.is.readInt();

        } catch (IOException e) {
            e.printStackTrace();
        }

        int sourcesCount = 0;
        String[] sourcesIpPort = new String[2];
        try (SocketInfo sc = new SocketInfo(new Socket("localhost", 8081))) {
            assertTrue(sc.socket.isConnected());
            sc.os.writeByte(3);
            sc.os.flush();
            sc.os.writeInt(lastUploadId);
            sc.os.flush();
            sourcesCount = sc.is.readInt();
            assertEquals(sourcesCount, 1);
            byte[] ip = new byte[4];
            sc.is.readFully(ip);
            sourcesIpPort[0] = ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];

            sc.is.readShort();//port, not valid until update, it runs then TorrentClient starts
            assertEquals(sourcesIpPort[0], ipSc);

        } catch (IOException e) {
            e.printStackTrace();
        }

        short newPort = 8082;
        try (SocketInfo sc = new SocketInfo(new Socket("localhost", 8081))) {
            sc.os.writeByte(4);
            sc.os.flush();
            sc.os.writeShort(newPort);
            sc.os.writeInt(1);
            sc.os.writeInt(lastUploadId);
            sc.os.flush();
            assertTrue(sc.is.readBoolean());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (SocketInfo sc = new SocketInfo(new Socket("localhost", 8081))) {
            assertTrue(sc.socket.isConnected());
            sc.os.writeByte(3);
            sc.os.flush();
            sc.os.writeInt(lastUploadId);
            sc.os.flush();
            sourcesCount = sc.is.readInt();
            assertEquals(sourcesCount, 1);
            byte[] ip = new byte[4];
            sc.is.readFully(ip);
            short port = sc.is.readShort();
            assertEquals(ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3], ipSc);
            assertEquals(port, newPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
        tt.close();
        try {
            th.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

}
