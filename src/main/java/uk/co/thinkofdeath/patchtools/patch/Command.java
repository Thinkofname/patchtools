package uk.co.thinkofdeath.patchtools.patch;

import java.util.Arrays;

class Command {

    public static Command from(String line) {
        Command command = new Command();
        String args[] = line.split(" ");
        if (args.length < 1) throw new IllegalArgumentException();
        char mode = args[0].charAt(0);
        command.mode = Mode.values()[".-+".indexOf(mode)];
        command.name = args[0].substring(1);
        command.args = new String[args.length - 1];
        System.arraycopy(args, 1, command.args, 0, command.args.length);
        return command;
    }

    Mode mode;
    String name;
    String[] args;

    private Command() {

    }

    @Override
    public String toString() {
        return "Command{" +
                "mode=" + mode +
                ", name='" + name + '\'' +
                ", args=" + Arrays.toString(args) +
                '}';
    }
}
