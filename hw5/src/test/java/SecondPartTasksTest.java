import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecondPartTasksTest {

    @Test
    public void testFindQuotes() {
        try {
            File tempFile1 = File.createTempFile("aaaa", "aaaa");
            File tempFile2 = File.createTempFile("bbbb", "bbbb");
            FileWriter fw = new FileWriter(tempFile1);
            fw.write("abcdabcd\neffe\nsssss");
            fw.close();
            fw = new FileWriter(tempFile2);
            fw.write("abcd");
            fw.close();
            tempFile1.deleteOnExit();
            tempFile2.deleteOnExit();
            Assert.assertEquals(SecondPartTasks.findQuotes(Arrays.asList(tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()), "abcd")
                    , Arrays.asList("abcdabcd", "abcd"));
            Assert.assertEquals(SecondPartTasks.findQuotes(Arrays.asList(tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()), "efff")
                    , Arrays.asList());
            Assert.assertEquals(SecondPartTasks.findQuotes(Arrays.asList(tempFile1.getAbsolutePath(), tempFile2.getAbsolutePath()), "")
                    , Arrays.asList("abcdabcd","effe","sssss","abcd"));
        } catch (IOException e) {
            throw new Error("couldn't create a file");
        }

    }

    @Test
    public void testPiDividedBy4() {
        Assert.assertEquals(Math.PI * 0.25, SecondPartTasks.piDividedBy4(), 0.05);
    }

    @Test
    public void testFindPrinter() {
        Map<String, List<String>> compst = new HashMap<>();
        Assert.assertEquals(SecondPartTasks.findPrinter(compst), "");
        compst.put("abc", Arrays.asList("sadas", "safgdsgsdg", "dsgsdgdsglkm"));
        compst.put("def", Arrays.asList("sadas", "safgddg", "dsgs"));
        Assert.assertEquals(SecondPartTasks.findPrinter(compst), "abc");
        compst.put("ghk", Arrays.asList("sadassdasfg", "safgdsewergsdg", "dsgsdgdsglkmdsgsdg"));
        Assert.assertEquals(SecondPartTasks.findPrinter(compst), "ghk");
    }

    @Test
    public void testCalculateGlobalOrder() {
        Map<String, Integer> order1 = new HashMap<>();
        Map<String, Integer> order2 = new HashMap<>();
        Map<String, Integer> order3 = new HashMap<>();
        order1.put("a", 3);
        order2.put("b", 3);
        order3.put("c", 3);
        Map<String, Integer> order_sum = new HashMap<>();
        Assert.assertEquals(SecondPartTasks.calculateGlobalOrder(Arrays.asList()), order_sum);
        order_sum.put("a", 3);
        order_sum.put("b", 3);
        Assert.assertEquals(SecondPartTasks.calculateGlobalOrder(Arrays.asList(order1, order2)), order_sum);
        order2.put("c", 6);
        order_sum.put("c", 9);
        Assert.assertEquals(SecondPartTasks.calculateGlobalOrder(Arrays.asList(order1, order2, order3)), order_sum);
    }
}