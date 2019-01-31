import java.io.IOException;

public class Checkout extends Command {
    public Checkout(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            String[] args = new String[arguments.length - 1];
            for (int i = 0; i < args.length; i++) {
                args[i] = arguments[i + 1];
            }
            fs.checkout(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;

}
