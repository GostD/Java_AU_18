import java.io.IOException;

public class Commit extends Command {
    public Commit(String[] args) {
        arguments = args;
    }
    public void execute() {
        try {
            FileSystem fs = new FileSystem();
            String[] adds = new String[arguments.length - 1];
            adds[0] = "commit";
            for (int i = 1; i < adds.length; i++) adds[i] = arguments[i + 1];
            fs.add(adds);
//            int commNum = fs.addFiles(arguments);
//            if (arguments.length > 2) fs.addFiles(arguments);
            fs.flush(arguments);//else
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] arguments;
}
