import java.io.IOException;

public class Status extends Command {
    public Status(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            fs.status();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
