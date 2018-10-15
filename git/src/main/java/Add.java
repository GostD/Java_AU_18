import java.io.IOException;

public class Add extends Command {
    public Add(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            fs.add(arguments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
