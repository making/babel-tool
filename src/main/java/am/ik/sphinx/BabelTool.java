package am.ik.sphinx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Pattern;

public class BabelTool {

    private static final String LC_MESSAGES = "LC_MESSAGES";
    private static final String DOT_MO = ".mo";
    private static final String DOT_PO = ".po";
    private static final String DOT_POT = ".pot";
    private static final String MO = "mo";
    private static final String PO = "po";
    private static final String DEFAULT_OUT_DIR = "locale";
    private static final String DEFAULT_IN_DIR = "_build/locale";
    private static final Pattern POT_PATTERN = Pattern.compile(Pattern
            .quote(".") + "pot$");
    private static final Pattern PO_PATTERN = Pattern.compile(Pattern
            .quote(".") + "po$");

    public static void po(String[] args) {
        String in = args.length > 0 ? args[0] : DEFAULT_IN_DIR;
        String out = args.length > 1 ? args[1] : DEFAULT_OUT_DIR;
        File inDir = new File(in);
        File outDir = new File(out);

        if (!inDir.exists()) {
            System.err.println(in + " is not exits");
            System.exit(-1);
        }

        mkDir(outDir);

        File[] locales = getLocales(outDir);

        for (File locale : locales) {
            System.out.println(PO + " " + locale);
            File[] pots = inDir.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(DOT_POT);
                }
            });
            for (File pot : pots) {
                File inFile = pot;
                File msgDir = new File(locale, LC_MESSAGES);
                File outFile = new File(msgDir, POT_PATTERN.matcher(
                        pot.getName()).replaceFirst(DOT_PO));

                String ags = "-l " + locale.getName() + " -i " + inFile
                        + " -o " + outFile;
                if (!outFile.exists()) {
                    exec("pybabel init " + ags);
                } else {
                    exec("pybabel update -N " + ags);
                }
            }
        }
    }

    public static void mo(String[] args) {
        String dirName = args.length > 0 ? args[0] : DEFAULT_OUT_DIR;
        File dir = new File(dirName);
        if (!dir.exists()) {
            System.err.println(dir + " is not exits");
            System.exit(-1);
        }

        File[] locales = getLocales(dir);

        for (File locale : locales) {
            System.out.println(MO + " " + locale);
            File msgDir = new File(locale, LC_MESSAGES);
            File[] pos = msgDir.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(DOT_PO);
                }
            });
            for (File po : pos) {
                System.out.println(po);
                File inFile = po;
                File outFile = new File(msgDir, PO_PATTERN
                        .matcher(po.getName()).replaceFirst(DOT_MO));

                String ags = "-f -l " + locale.getName() + " -i " + inFile
                        + " -o " + outFile;
                exec("pybabel compile " + ags);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err
                    .printf("to create .po file : java -jar babel-tool.jar %s [inputDir(default:%s)] [output(default:%s)]%n",
                            PO, DEFAULT_IN_DIR, DEFAULT_OUT_DIR);
            System.err
                    .printf("to create .mo file : java -jar babel-tool.jar %s [dir(default:%s)]%n",
                            MO, DEFAULT_OUT_DIR);
            System.exit(-1);
        }

        String cmd = args[0];

        String[] cmdArgs = Arrays.<String> copyOfRange(args, 1, args.length);
        if (PO.equals(cmd)) {
            po(cmdArgs);
        } else if (MO.equals(cmd)) {
            mo(cmdArgs);
        }
    }

    public static void exec(String cmd) {
        System.out.println("$ " + cmd);
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            printInputStream(p.getInputStream());
            printInputStream(p.getErrorStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mkDir(File dir) {
        if (!dir.exists()) {
            System.out.println("mkdir " + dir);
            if (!dir.mkdirs()) {
                System.err.println("could not create " + dir);
                System.exit(-1);
            }
        }
    }

    public static File[] getLocales(File dir) {
        File[] locales = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        if (locales.length == 0) {
            System.err.println("No locale is found in " + dir);
            System.exit(-1);
        }

        return locales;
    }

    public static void printInputStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            for (;;) {
                String line = br.readLine();
                if (line == null)
                    break;
                System.out.println(line);
            }
        } finally {
            br.close();
        }
    }
}
