import java.io.IOException;

public class Rm extends Command {
    public Rm(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            fs.remove(arguments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
