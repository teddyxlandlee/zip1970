package xland.ioutils.zip1970;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Main {
    public static void main(String[] rawArgs) {
        if (rawArgs.length == 0) printAndExit(help());
        final List<Arg> args = Arg.parse(rawArgs, UNIX_MAPPER);
        LocalDateTime mTime, cTime, aTime;
        mTime = cTime = aTime = LocalDateTime.MIN;
        Pattern inclusion = null, exclusion = null;
        Supplier<? extends Path> output = null;
        Supplier<? extends String> input = null;
        for (Iterator<Arg> itr = args.iterator(); itr.hasNext();) {
            Arg arg = itr.next();
            final String context = arg.getContext();
            if (arg.isGnu()) {
                switch (context) {
                    case "help":
                        printAndExit(help());
                    case "version":
                        printAndExit(version());

                    case "access-time":
                        checkItr(itr);
                        aTime = LocalDateTime.parse(itr.next().toString());
                        break;
                    case "create-time":
                        checkItr(itr);
                        cTime = LocalDateTime.parse(itr.next().toString());
                        break;
                    case "time":
                        checkItr(itr);
                        mTime = LocalDateTime.parse(itr.next().toString());
                        break;
                    case "include":
                        checkItr(itr);
                        inclusion = Pattern.compile(itr.next().toString());
                        break;
                    case "exclude":
                        checkItr(itr);
                        exclusion = Pattern.compile(itr.next().toString());
                        break;
                    case "output":
                        checkItr(itr);
                        final Arg n = itr.next();
                        output = () -> (Paths.get(n.toString()));
                        break;
                }
            } else/* arg.isRaw() == true*/ {
                if (input != null) printAndExit("ERROR: Too many inputs");
                input = arg::getContext;
            }
        } // END for loop
        if (input == null) {
            printAndExit("ERROR: An input required"); return;
        }
        try (ZipFile zipFile = new ZipFile(input.get());
             OutputStream os = output == null ? System.out : Files.newOutputStream(output.get());
             Zip1970 zip1970 = new Zip1970(zipFile, inclusion, exclusion, cTime, mTime, aTime, new ZipOutputStream(os))) {
            zip1970.process();
        } catch (IOException e) {
            System.err.println("An error occurred when transforming");
            e.printStackTrace();
        }
    }

    static final Function<Character, String> UNIX_MAPPER;
    static {
        Map<Character, String> m = new HashMap<>();
        m.put('a', "access-time");
        m.put('c', "create-time");
        m.put('h', "help");
        m.put('i', "include");
        m.put('m', "time");
        m.put('o', "output");
        m.put('v', "version");
        m.put('x', "exclude");
        UNIX_MAPPER = m::get;
    }

    public static String help() {
        return  "Usage: java -jar zip1970.jar [options] <input_zipfile>\n" +
                "or:    java -jar zip1970.jar [-h|--help|-v|--version]\n\n" +
                "Options:\n" +
                "\t-a, --access-time [timestamp]: set access time matching ISO Local DateTime, defaulted to the epoch.\n" +
                "\t-c, --create-time [timestamp]: set creation time matching ISO Local DateTime, defaulted to the epoch.\n" +
                "\t-h, --help: print this help message\n" +
                "\t-i, --include: set entry inclusion pattern\n" +
                "\t-m, --time [timestamp]: set modified time matching ISO Local DateTime, defaulted to the epoch.\n" +
                "\t-o, --output [file]: set the output path, defaulted to stdout\n" +
                "\t-v, --version: print software version and copyright information\n" +
                "\t-x, --exclude: set entry exclusion pattern\n" +
                "\tinput_zipfile: the input zipfile path, defaulted to stdin";
    }

    private static String version() {
        return  "zip1970 " + VER + "\n" +
                "Copyright (C) 2022 teddyxlandlee\n" +
                "This program comes with ABSOLUTELY NO WARRANTY.\n" +
                "This is free software licensed under GNU Affero General Public License, Version 3, or " +
                "(at your option) later, and you are welcome to redistribute it " +
                "under certain conditions";
    }

    private static final String VER = "1.0";

    private static class Arg {
        //UNIX = 1, RAW = 0, GNU = -1;
        private final int type;
        private final String ctx;

        private Arg(int type, String ctx) {
            this.type = type;
            this.ctx = ctx;
        }

        static Arg unix(char c) { return new Arg(1, String.valueOf(c)); }
        static Arg gnu(String s) { return new Arg(-1, s); }
        static Arg raw(String s) { return new Arg(0, s); }

        String getContext() { return ctx; }
        int getType() { return Integer.signum(type); }
        boolean isGnu() { return type < 0; }
        boolean isUnix() { return type > 0; }
        boolean isRaw() { return type == 0; }

        public String toString() {
            if (isGnu()) return "--" + ctx;
            else if (isRaw()) return ctx;
            return '-' + ctx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Arg)) return false;
            final Arg other = (Arg) o;
            return Objects.equals(this.ctx, other.ctx) &&
                    this.getType() == other.getType();
        }

        @Override
        public int hashCode() {
            return (Objects.hashCode(this.ctx) << 2) | (getType() & 3);
        }

        static List<Arg> parse(String... args) {
            List<Arg> list = new ArrayList<>();
            for (String s : args) {
                if ("--".equals(s) || s.length() < 2 || s.charAt(0) != '-')
                    list.add(raw(s));
                else if (s.charAt(1) == '-') {
                    int idx;
                    if ((idx = s.indexOf('=')) < 0)
                        list.add(gnu(s.substring(2)));
                    else {
                        list.add(gnu(s.substring(2, idx)));
                        list.add(raw(s.substring(idx + 1)));
                    }
                } else {
                    final char[] chars = s.toCharArray();
                    for (int i = 1; i < chars.length; i++)
                        list.add(unix(chars[i]));
                }
            }
            return list;
        }

        @SuppressWarnings("all")
        static List<Arg> parse(String[] args, Function<Character, String> unixMapper) {
            return parse(args).stream().flatMap(arg -> {
                if (arg.isUnix()) {
                    final String s = unixMapper.apply(arg.getContext().charAt(0));
                    if (s == null) return Stream.empty();
                    return Stream.of(gnu(s));
                }
                return Stream.of(arg);
            }).collect(Collectors.toList());
        }
    }

    private static void printAndExit(String s) {
        System.err.println(s);
        System.exit(0);
    }

    private static void checkItr(Iterator<?> itr) {
        if (!itr.hasNext()) {
            printAndExit("ERROR: Invalid arguments, required parameter does not exist");
        }
    }
}
