import java.awt.*;
import java.io.IOException;

public class Branch extends Command {
    public Branch(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            if (arguments[1].equals("-d")) {
                assert(arguments.length >= 3);
                fs.deleteBranch(arguments[2]);
            } else if (arguments[1].equals("-b")) {
                assert(arguments.length >= 3);
                if (arguments.length == 4)
                    fs.createBranchAndSwitch(arguments[2], arguments[3]);
                else
                    fs.createBranchAndSwitch(arguments[2]);
            } else {
                if (arguments.length == 2)
                    fs.createBranch(arguments[1]);
                else
                    fs.createBranch(arguments[1], arguments[2]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
