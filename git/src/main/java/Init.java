import java.io.File;

public class Init extends Command {// Command {
    public Init(String[] args) {}
    public void execute() {
        File fl = new File(".simple-git");
        if (!fl.exists()) {
            fl.mkdir();
        }
    }
}
