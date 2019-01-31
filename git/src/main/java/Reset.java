import java.io.IOException;

public class Reset extends Command {
    public Reset(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            fs.reset(arguments[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
