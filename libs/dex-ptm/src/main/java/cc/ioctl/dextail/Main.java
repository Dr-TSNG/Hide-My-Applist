package cc.ioctl.dextail;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class Main {

    public static void main(String[] args) {
        int exitCode = 0;
        if (args.length == 0) {
            System.out.println("E Incorrect arguments: Please specify input file");
            System.out.println("E Try '--help' for more information.");
            System.exit(1);
        }
        CliArgs arg = new CliArgs();
        try {
            arg.parse(args);
        } catch (IllegalArgumentException e) {
            System.out.println("E " + e.getMessage());
            System.exit(1);
            return;
        }
        if (arg.showHelp) {
            arg.printHelp(System.out);
            System.exit(0);
            return;
        }
        if (arg.unrecognized.size() > 0) {
            System.out.println("E Unrecognized option: " + arg.unrecognized);
            System.exit(1);
            return;
        }
        if (arg.useTimeAsPayload && arg.payloadFile != null) {
            System.out.println("E You can specify only ONE payload");
            System.exit(1);
            return;
        }
        int stuff = 0;
        if (arg.useTimeAsPayload || arg.payloadFile != null) {
            stuff++;
        }
        if (arg.checkDex) {
            stuff++;
        }
        if (arg.showInfo) {
            stuff++;
        }
        if (stuff > 1) {
            System.out.println("E You can do only ONE thing at a time");
            System.exit(1);
            return;
        }
        String dexPath = arg.dexFile;
        if (dexPath == null) {
            System.out.println("E argv: missing dex file");
            System.exit(1);
            return;
        }
        File dexFile = new File(dexPath);
        if (!dexFile.exists()) {
            System.out.println("E file not found: " + dexPath);
            System.exit(1);
            return;
        }
        byte[] dex;
        try {
            dex = HexUtils.readFileData(dexPath);
        } catch (Exception e) {
            System.out.println("E Failed to read file: " + dexPath);
            e.printStackTrace(System.out);
            System.exit(1);
            return;
        }
        if (arg.checkDex) {
            if (DexTail.checkDexSum(dex, System.out)) {
                System.out.println("I Checksum is OK");
                exitCode = 0;
            } else {
                System.out.println("W Checksum is INCORRECT");
                exitCode = 1;
            }
        } else {
            String output = arg.outputFile;
            if (arg.showInfo || arg.extractPayload) {
                if (arg.extractPayload && output == null) {
                    System.out.println("E Please specify extract output path");
                    exitCode = 1;
                } else {
                    try {
                        if (!DexTail.checkDexSum(dex, System.out)) {
                            System.out.println("E Dex is invalid, abort.");
                            exitCode = 1;
                        } else {
                            byte[] dat = DexTail.extractPayload(dex, System.out);
                            if (dat != null) {
                                System.out.printf("I Dex payload size 0x%x(%d)\n", dat.length,
                                    dat.length);
                                if (output != null) {
                                    HexUtils.writeFileData(output, dat);
                                }
                            } else {
                                System.out.println("E Dex payload does not exist.");
                            }
                        }
                    } catch (Throwable e) {
                        System.out.println("E Error when extracting payload : " + dexPath);
                        e.printStackTrace(System.out);
                        System.exit(1);
                    }
                }
            } else {
                if (output == null) {
                    System.out.println("E Please specify extract output path");
                    exitCode = 1;
                } else {
                    try {
                        byte[] payload;
                        if (arg.useTimeAsPayload) {
                            payload = HexUtils.getTimeAsByteArray();
                        } else {
                            payload = HexUtils.readFileData(arg.payloadFile);
                        }
                        if (!DexTail.checkDexSum(dex, System.out)) {
                            System.out.println("E Dex is invalid, abort.");
                            exitCode = 1;
                        } else {
                            byte[] buf = DexTail
                                .injectPayload(dex, payload, arg.disableXor, System.out);
                            HexUtils.writeFileData(output, buf);
                        }
                    } catch (Exception e) {
                        System.out.println("E Failed to inject payload: " + dexPath);
                        e.printStackTrace(System.out);
                        System.exit(1);
                        return;
                    }
                }
            }
        }
        System.exit(exitCode);
    }

    public static boolean checkAndUpdateTail(String dexPath, byte[] payload, boolean disableXor,
        PrintStream out) throws IOException {
        byte[] dex = HexUtils.readFileData(dexPath);
        if (!DexTail.checkDexSum(dex, System.out)) {
            System.out.println("E Dex is invalid, abort.");
            return false;
        } else {
            byte[] buf = DexTail.injectPayload(dex, payload, disableXor, System.out);
            HexUtils.writeFileData(dexPath, buf);
        }
        return true;
    }
}
