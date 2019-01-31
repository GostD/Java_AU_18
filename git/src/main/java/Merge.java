import java.io.IOException;

public class Merge extends Command {
    public Merge(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            fs.merge(arguments[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
