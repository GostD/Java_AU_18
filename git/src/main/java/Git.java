import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class Git {
    private static List<String> availableCommands = Arrays.asList("init", "commit", "reset", "log", "checkout", "add", "rm", "status");

    public static void main(String[] args) {

        if (!availableCommands.contains(args[0])) {
            throw new IllegalArgumentException("No such command");
        }
        String clsName = Character.toUpperCase(args[0].charAt(0)) + args[0].substring(1);
        try {
            Command cmd = (Command) Class.forName(clsName).getConstructor(args.getClass()).newInstance(new Object[] {args});
            cmd.execute();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
}
