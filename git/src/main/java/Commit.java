import java.io.IOException;

public class Commit extends Command {
    public Commit(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            if (arguments.length > 2) fs.addFiles(arguments);
            else fs.flush(arguments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
