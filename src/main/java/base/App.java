package base;

import picocli.CommandLine;

public class App {

    public static void main(String[] args) {

        CommandLine.run(new Args(), args);
    }
}
