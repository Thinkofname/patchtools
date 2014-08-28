/*
 * Copyright 2014 Matthew Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
