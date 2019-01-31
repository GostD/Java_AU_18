import java.io.IOException;

public class Log extends Command {
    public Log(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            fs.log(arguments);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
