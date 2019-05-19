
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.json.*;

import java.io.*;
import java.util.List;
import java.util.Scanner;

public class OneLinkCompiler {
    CommonTokenStream tokens;
    ParseTree tree;
    ParseTreeWalker walker;
    boolean debug = false;

    static final String ARDUINO_COMPILOR = "/home/freg/share/arduino-1.8.8/arduino";    // Arduino compilor location
    //    static final String ARDUINO_COMPILOR = "/home/freg/arduino-1.6.9/arduino";
    static final String RPI_COMPILOR = "/home/freg/gcc-linaro-arm-linux-gnueabihf-raspbian/bin/arm-linux-gnueabihf-g++";  // Raspberry Pi compilor location
    static final String BBB_COMPILOR = "/home/freg/gcc-linaro-arm-linux-gnueabihf-4.8/bin/arm-linux-gnueabihf-g++";       // BeagleBone compilor location
    static final String HUMMING_COMPILOR = "/home/freg/gcc-linaro-arm-linux-gnueabihf-4.8/bin/arm-linux-gnueabihf-g++";   // HummingBoard compilor location
    static final String MBED_COMPILOR = "python /home/freg/.local/lib/python2.7/site-packages/mbed/mbed.py";
    static final String ALIOS_COMPILOR = "python /home/freg/.local/lib/python2.7/site-packages/aos/__main__.py";
    static final String CORRECTOR_API="/home/freg/share/API_list.txt";

    void writeToFile(String contents, String fileName) {
        File file = null;
        Writer writer = null;
        // Write to file
        if (!fileName.isEmpty()) {
            file = new File(fileName);
        } else {
            return;
        }

        try {
            writer = new FileWriter(file);
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }

    String readFromFile(String fileName) {
        String encoding = "UTF-8";
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println("The OS does not support " + encoding);
            e.printStackTrace();
            return null;
        }
    }

    public OneLinkCompiler(String filename) {
        try {
            CharStream charStream = CharStreams.fromFileName(filename);
            OneLinkLexer lexer = new OneLinkLexer(charStream);
            tokens = new CommonTokenStream(lexer);

            OneLinkParser parser = new OneLinkParser(tokens);
            tree = parser.compliationUnit();
            walker = new ParseTreeWalker();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
    public void correctCode(){
        OneLinkDebugger oneLinkDebugger = new OneLinkDebugger(CORRECTOR_API);
        walker.walk(oneLinkDebugger, tree);
    }

    public JSONObject extractHardwareDemand() {
        OneLinkHardwareExtractor extractor = new OneLinkHardwareExtractor(tokens);
        JSONObject hardware = new JSONObject();
        walker.walk(extractor, tree);

        // Generate filtered code
        if(extractor.filtered==null){
            System.out.println("Extractor failed.");
            return null;
        }
        writeToFile(extractor.filtered, "sketch.ino");

        JSONArray requirements = new JSONArray();

        for (OneLinkHardwareExtractor.MeasureRange m : extractor.measureRangeList) {
            extractor.lib.remove(m.dev);
            if (debug)
                System.out.println("required measureRange: [" + m.dev + "] from " + m.lowerBound + " " + m.unit + " to " +
                        m.upperBound + " " + m.unit);
            JSONObject item = new JSONObject();
            item.put("Function", m.dev);
            JSONObject feature = new JSONObject();
            JSONObject measureRange = new JSONObject();
            JSONArray range = new JSONArray();
            range.put(m.lowerBound);
            range.put(m.upperBound);
            measureRange.put("Numeric", range);
            measureRange.put("Unit", m.unit);
            feature.put("Measure_Range", measureRange);
            item.put("Feature", feature);
            requirements.put(item);
        }
        // Requirement
        for (String str : extractor.lib) {
            if (debug) System.out.println("required lib: " + str);
            JSONObject item = new JSONObject();
            item.put("Function", str);
            requirements.put(item);
        }
        // Mandatory requirement
        JSONArray mandatory = new JSONArray();
        for (OneLinkParser.UnqualifiedidContext ctx : extractor.req) {
            ParserRuleContext p = ctx.getParent().getParent().getParent().getParent();
            if (p instanceof OneLinkParser.PostfixexpressionContext) {
                OneLinkParser.InitializerlistContext ictx;
                ictx = ((OneLinkParser.PostfixexpressionContext) p).expressionlist().initializerlist();
                if (debug) System.out.print("required devices:(");
                List<String> devices = OneLinkHardwareExtractor.getTextFromInitializerlist(ictx);

                for (String str : devices) {
                    if (debug) System.out.print(" " + str);
                    mandatory.put(str.substring(1, str.length() - 1));
                }

                if (debug) System.out.println(" )");
            }
        }
        if (!requirements.isEmpty())
            hardware.put("Requirements", requirements);
        if (!mandatory.isEmpty())
            hardware.put("Mandatory", mandatory);
        if (debug) System.out.println(" ");
        if (debug) System.out.println(hardware);

        return hardware;
    }

    public JSONObject extractRecommandation(String boardName, String outfile) {
        JSONObject hardware = extractHardwareDemand();
        JSONObject recommand = new JSONObject();
        recommand.put("Requirements", hardware.getJSONArray("Requirements"));
        recommand.put("Mandatory", boardName);
        return recommand;
    }

    public static String exec(String[] cmd) {
        String output = "";
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();

            BufferedReader outreader = new BufferedReader(new InputStreamReader(stdout));
            BufferedReader errreader = new BufferedReader(new InputStreamReader(stderr));
            process.waitFor();
            String line;


            System.out.print("executing:");
            for (String i : cmd) {
                System.out.print(" " + i);
            }
            System.out.print("\n");
            while ((line = outreader.readLine()) != null) {
                System.out.println(line);
                output+=line+"\r\n";
            }
            while ((line = errreader.readLine()) != null) {
                System.out.println(line);
                output+=line+"\r\n";
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public static void exec_gui(String[] cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd, new String[]{"DISPLAY=:1"});
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();

            BufferedReader outreader = new BufferedReader(new InputStreamReader(stdout));
            BufferedReader errreader = new BufferedReader(new InputStreamReader(stderr));

            String line;


            System.out.print("executing:");
            for (String i : cmd) {
                System.out.print(" " + i);
            }
            System.out.print("\n");
            while ((line = outreader.readLine()) != null) {
                System.out.println(line);
            }
            while ((line = errreader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Excuting shell command
    public static String shell(String cmd) {
        String[] sh = new String[]{"sh", "-c", ""};
        sh[2] = cmd;
        return exec(sh);
    }

    public static void shell_gui(String cmd) {
        String[] sh = new String[]{"sh", "-c", ""};
        sh[2] = cmd;
        exec_gui(sh);
    }

    private char upper(char lower) {
        if (lower >= 'a' && lower <= 'z') {
            return (char) ('A' + lower - 'a');
        } else {
            return lower;
        }
    }

    public int readBoardID(String filename) {
        FileReader file;
        BufferedReader reader;
        try {
            file = new FileReader(filename);
            reader = new BufferedReader(file);

            reader.readLine();
            reader.readLine();
            reader.readLine();
            reader.readLine();
            String fourthline = reader.readLine();

            String ID = fourthline.strip().split("\t")[1].strip();

            return Integer.parseInt(ID);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    String getFileName(String filePath) {
        String[] strs = filePath.split("/");
        return strs[strs.length - 1];
    }

    void createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
    }

    public void genCode(int boardID, String hardwarePath, String inputSrc, String buildDir, String libDir) throws Exception {
        System.out.println("Start generate code...");
        // Read hardware.json
        JSONTokener tokener = new JSONTokener(new FileReader(new File(hardwarePath)));
        JSONObject hardware = new JSONObject(tokener);
        // Construct TL_Config.h
        createDirectory(buildDir);
        FileWriter pinConfigWriter = new FileWriter(buildDir + "/TL_UserPinConfig.h");
        FileWriter configWriter = new FileWriter(buildDir + "/TL_Config.h", false);
        configWriter.write("#ifndef TL_CONFIG_H\r\n");
        configWriter.write("#define TL_CONFIG_H\r\n");
        configWriter.write("#include \"TL_Device_ID.h\"\r\n");
        // Whether a linux board will use lib curl
        boolean LibCurl = false;
        // Whether a linux board will use lib sphinx
        boolean LibSphinx = false;
        // The platform
        String Platform = "";
        String CPPList = "";

        String outputSrc = buildDir + "/" + getFileName(inputSrc);
        FileWriter srcWriter = new FileWriter(outputSrc, false);
        switch (boardID) {
            case 1001: {
                configWriter.write("#define PLATFORM 2\r\n");
                configWriter.write("#define BOARD 1001\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "LinkIt";
                break;
            }
            case 1002: {
                configWriter.write("#define PLATFORM 1\r\n");
                configWriter.write("#define BOARD 1002\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Arduino";
                break;
            }
            case 1003: {
                configWriter.write("#define PLATFORM 4\r\n");
                configWriter.write("#define BOARD 1003\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"Arduino.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Raspberry_Pi";
                break;
            }
            case 1004: {
                pinConfigWriter.write("#ifndef TL_USERPINCONFIG_H\r\n");
                pinConfigWriter.write("#define TL_USERPINCONFIG_H\r\n");
                configWriter.write("#define PLATFORM 3\r\n");
                configWriter.write("#define BOARD 1004\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"Arduino.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Beagle_Bone";
                break;
            }
            case 1005: {
                pinConfigWriter.write("#ifndef TL_USERPINCONFIG_H\r\n");
                pinConfigWriter.write("#define TL_USERPINCONFIG_H\r\n");
                configWriter.write("#define PLATFORM 3\r\n");
                configWriter.write("#define BOARD 1005\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"Arduino.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Beagle_Bone";
                break;
            }
            case 1006: {
                pinConfigWriter.write("#ifndef TL_USERPINCONFIG_H\r\n");
                pinConfigWriter.write("#define TL_USERPINCONFIG_H\r\n");
                configWriter.write("#define PLATFORM 3\r\n");
                configWriter.write("#define BOARD 1006\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"Arduino.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Beagle_Bone";
                break;
            }
            case 1007: {
                configWriter.write("#define PLATFORM 1\r\n");
                configWriter.write("#define BOARD 1007\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Arduino";
                break;
            }
            case 1008: {
                configWriter.write("#define PLATFORM 1\r\n");
                configWriter.write("#define BOARD 1008\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Arduino";
                break;
            }
            case 1009: {
                configWriter.write("#define PLATFORM 1\r\n");
                configWriter.write("#define BOARD 1009\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Arduino";
                break;
            }
            case 1010: {
                configWriter.write("#define PLATFORM 1\r\n");
                configWriter.write("#define BOARD 1010\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Arduino";
                break;
            }
            case 1011: {
                configWriter.write("#define PLATFORM 5\r\n");
                configWriter.write("#define BOARD 1011\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Mbed";
                break;
            }
            case 1012: {
                configWriter.write("#define PLATFORM 6\r\n");
                configWriter.write("#define BOARD 1012\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"TL_Libraries.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "AliOS";
                break;
            }
            case 1013: {
                configWriter.write("#define PLATFORM 7\r\n");
                configWriter.write("#define BOARD 1013\r\n");

                String srcStr = readFromFile(inputSrc);
                srcWriter.write("#include \"Arduino.h\"\r\n");
                srcWriter.write(srcStr);
                srcWriter.close();

                Platform = "Humming";
                break;
            }
            default: {
                System.out.println("Unknown Platform.");
                return;
            }
        }
        // Platform
        switch (Platform) {
            case "Arduino": {
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Arduino/TinyLink/* " + buildDir});
                break;
            }
            case "LinkIt": {
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "LinkIt/TinyLink/* " + buildDir});
                break;
            }
            case "Beagle_Bone": {
                CPPList = "src = (sketch";
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Beagle_Bone/TinyLink/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Beagle_Bone/library/* " + buildDir});
                String[] coreCpps =
                        shell("ls " + libDir + Platform + "/library/*.cpp | xargs").split(" ");
                for (String coreCpp : coreCpps) {
                    String[] temp1 = coreCpp.split("/");
                    String temp2 = temp1[temp1.length - 1].split(".cpp")[0].strip();
                    if (!CPPList.contains(temp2)) {
                        CPPList += " " + temp2;
                    }
                }
                break;
            }
            case "Humming": {
                CPPList = "src = (sketch";
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Humming/TinyLink/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Humming/library/* " + buildDir});
                String[] coreCpps =
                        shell("ls " + libDir + Platform + "/library/*.cpp | xargs").split(" ");
                for (String coreCpp : coreCpps) {
                    String[] temp1 = coreCpp.split("/");
                    String temp2 = temp1[temp1.length - 1].split(".cpp")[0].strip();
                    if (!CPPList.contains(temp2)) {
                        CPPList += " " + temp2;
                    }
                }
                break;
            }
            case "Raspberry_Pi": {
                CPPList = "src = (sketch";
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Raspberry_Pi/TinyLink/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Raspberry_Pi/library/* " + buildDir});
                String[] coreCpps =
                        shell("ls " + libDir + Platform + "/library/*.cpp | xargs").split(" ");
                for (String coreCpp : coreCpps) {
                    String[] temp1 = coreCpp.split("/");
                    String temp2 = temp1[temp1.length - 1].split(".cpp")[0].strip();
                    if (!CPPList.contains(temp2)) {
                        CPPList += " " + temp2;
                    }
                }
                break;
            }
            case "Mbed": {
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Mbed/TinyLink/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Mbed/library/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "Mbed/.temp/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp " + libDir + "Mbed/.mbed " + buildDir});
                break;
            }
            case "AliOS": {
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "AliOS/TinyLink/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "AliOS/library/* " + buildDir});
                exec(new String[]{"sh", "-c", "cp -r " + libDir + "AliOS/sketch/* " + buildDir});
                break;
            }
        }
        // Hardware
        /// Function
        try {
            JSONObject board = (JSONObject) hardware.get("Board");
            JSONArray Functions = (JSONArray) board.get("Function");
            JSONArray Modules = (JSONArray) board.get("Module");
            for (int i = 0; i < Functions.length(); i++) {
                Object function = Functions.get(i);
                Object module = Modules.get(i);
                configWriter.write("#define TINYLINK_" + function.toString().toUpperCase() + " " +
                        module.toString().toUpperCase() + "\r\n");
                exec(new String[]{"sh", "-c",
                        "cp -r " + libDir + Platform + "/TL_" + function.toString().toUpperCase() + '/' +
                                module.toString().toUpperCase() + "/*" + " " + buildDir});
                if (Platform.equals("Beagle_Bone") || Platform.equals("Raspberry_Pi") || Platform.equals("Humming")) {
                    String[] externalCpps =
                            shell("ls " + libDir + Platform + "/TL_" + function.toString().toUpperCase()
                                    + "/" + module.toString().toUpperCase() + "/*.cpp | xargs").split(" ");
                    for (String externalCpp : externalCpps) {
                        String[] temp1 = externalCpp.split("/");
                        String temp2 = temp1[temp1.length - 1].split(".cpp")[0].strip();
                        if (!CPPList.contains(temp2)) {
                            CPPList += " " + temp2;
                        }
                    }
                    if (function.toString().equalsIgnoreCase("WIFI")) {
                        LibCurl = true;
                    }
                    if (function.toString().equalsIgnoreCase("VOICE")) {
                        LibSphinx = true;
                    }
                }
            }
        } catch (JSONException e) {
            System.out.println("No board information");
//            e.printStackTrace();
        }
        /// Shield connection
        if (hardware.has("ShieldConnection")) {
            JSONArray shieldConnection = (JSONArray) hardware.get("ShieldConnection");
            for (int i = 0; i < shieldConnection.length(); i++) {
                JSONObject shieldItem = (JSONObject) shieldConnection.get(i);
                if (shieldItem.has("Function") && shieldItem.has("Module")) {
                    JSONArray Functions = shieldItem.getJSONArray("Function");
                    JSONArray Modules = shieldItem.getJSONArray("Module");
                    for (int j = 0; j < Functions.length(); j++) {
                        Object function = Functions.get(j);
                        Object module = Modules.get(j);
                        configWriter.write("#define TINYLINK_" + function.toString().toUpperCase() + " " +
                                module.toString().toUpperCase() + "\r\n");
                        shell("cp -r " + libDir + Platform + "/TL_" + function.toString().toUpperCase() + "/" +
                                module.toString().toUpperCase() + "/* " + buildDir);
                        if (Platform.equals("Beagle_Bone") || Platform.equals("Raspberry_Pi") || Platform.equals("Humming")) {
                            String[] externalCpps =
                                    shell("ls " + libDir + Platform + "/TL_" + function.toString().toUpperCase()
                                            + "/" + module.toString().toUpperCase() + "/*.cpp | xargs").split(" ");
                            for (String externalCpp : externalCpps) {
                                String[] temp1 = externalCpp.split("/");
                                String temp2 = temp1[temp1.length - 1].split(".cpp")[0].strip();
                                if (!CPPList.contains(temp2)) {
                                    CPPList += " " + temp2;
                                }
                            }
                            if (function.toString().equalsIgnoreCase("WIFI")) {
                                LibCurl = true;
                            }
                            if (function.toString().equalsIgnoreCase("VOICE")) {
                                LibSphinx = true;
                            }
                        }
                        String[] Pins;
                        if (!shieldItem.get("Input").toString().equalsIgnoreCase("NULL")) {
                            Pins = shieldItem.get("Input").toString().split(",");
                            int k = 0;
                            for (String inputPin : Pins) {
                                String pin = "";
                                if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                    pin = inputPin.substring(0, 2).toUpperCase() + "_" + inputPin.substring(3).toUpperCase();
                                } else {
                                    if (inputPin.charAt(0) == 'D' || inputPin.charAt(0) == 'd' || inputPin.charAt(0) == 'A' || inputPin.charAt(0) == 'a') {
                                        pin = inputPin.substring(1).toUpperCase();
                                    } else {
                                        pin = inputPin.toUpperCase();
                                    }
                                }
                                if (inputPin.charAt(0) == 'A' || inputPin.charAt(0) == 'a') {
                                    pin = "A" + pin;
                                }
                                if (shieldItem.get("Type").toString().equalsIgnoreCase("DIGITAL")) {
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#define " + pin + "_MODE gpio\r\n");
                                        pinConfigWriter.write("#endif\r\n");
                                    }
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_DIGITAL_INPUT" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_DIGITAL_INPUT" + k + " " + pin + "\r\n");
                                    }
                                } else if (shieldItem.get("Type").toString().equalsIgnoreCase("I2C")) {
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_I2C_SDA" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_I2C_SDA" + k + " " + pin + "\r\n");
                                    }
                                    pin = "";
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pin = shieldItem.get("SCL").toString().substring(0, 2).toUpperCase() + "_" +
                                                shieldItem.get("SCL").toString().substring(3);
                                    } else {
                                        if (upper(shieldItem.get("SCL").toString().charAt(0)) == 'D' || upper(shieldItem.get("SCL").toString().charAt(0)) == 'A') {
                                            pin = shieldItem.get("SCL").toString().substring(1).toUpperCase();
                                        } else {
                                            pin = shieldItem.get("SCL").toString().toUpperCase();
                                        }
                                    }
                                    if (upper(shieldItem.get("SCL").toString().charAt(0)) == 'A') {
                                        pin = "A" + pin;
                                    }
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_I2C_SCL" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_I2C_SCL" + k + " " + pin + "\r\n");
                                    }
                                } else if (shieldItem.get("Type").toString().equalsIgnoreCase("UART")) {
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#define " + pin + "_MODE uart\r\n");
                                        pinConfigWriter.write("#endif\r\n");
                                    }
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_UART_RX" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_UART_RX" + k + " " + pin + "\r\n");
                                    }
                                } else if (shieldItem.get("Type").toString().equalsIgnoreCase("PWM")) {
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_PWM" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_PWM" + k + " " + pin + "\r\n");
                                    }
                                } else if (shieldItem.get("Type").toString().equalsIgnoreCase("SPI")) {
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_MISO" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_MISO" + k + " " + pin + "\r\n");
                                    }
                                    pin = "";
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pin = shieldItem.get("SCK").toString().substring(0, 2).toUpperCase() + "_" +
                                                shieldItem.get("SCK").toString().substring(3).toUpperCase();
                                    } else {
                                        if (upper(shieldItem.get("SCK").toString().charAt(0)) == 'D' || upper(shieldItem.get("SCK").toString().charAt(0)) == 'A') {
                                            pin = shieldItem.get("SCK").toString().substring(1).toUpperCase();
                                        } else {
                                            pin = shieldItem.get("SCK").toString().toUpperCase();
                                        }
                                    }
                                    if (upper(shieldItem.get("SCK").toString().charAt(0)) == 'A') {
                                        pin = "A" + pin;
                                    }
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_SCK" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_SCK" + k + " " + pin + "\r\n");
                                    }
                                    pin = "";
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pin = shieldItem.get("SS").toString().substring(0, 2).toUpperCase() + "_" +
                                                shieldItem.get("SS").toString().substring(3).toUpperCase();
                                    } else {
                                        if (upper(shieldItem.get("SS").toString().charAt(0)) == 'D' || upper(shieldItem.get("SS").toString().charAt(0)) == 'A') {
                                            pin = shieldItem.get("SS").toString().substring(1).toUpperCase();
                                        } else {
                                            pin = shieldItem.get("SS").toString().toUpperCase();
                                        }
                                    }
                                    if (upper(shieldItem.get("SS").toString().charAt(0)) == 'A') {
                                        pin = "A" + pin;
                                    }
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_SS" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_SS" + k + " " + pin + "\r\n");
                                    }
                                }
                                k++;


                            }
                        }
                        if (!shieldItem.get("Output").toString().equalsIgnoreCase("NULL")) {
                            Pins = shieldItem.get("Output").toString().split(",");
                            int k = 0;
                            for (String outputPin : Pins) {
                                String pin = "";
                                if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                    pin = outputPin.substring(0, 2).toUpperCase() + "_" +
                                            outputPin.substring(3).toUpperCase();
                                } else {
                                    if (upper(outputPin.charAt(0)) == 'D' || upper(outputPin.charAt(0)) == 'A') {

                                        pin = Integer.toString(Integer.parseInt(outputPin.substring(1))).toUpperCase();
                                    } else {
                                        pin = outputPin.toUpperCase();
                                    }
                                }
                                if (upper(outputPin.charAt(0)) == 'A') {
                                    pin = "A" + pin;
                                }

                                if (shieldItem.get("Type").toString().equalsIgnoreCase("DIGITAL")) {
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#define " + pin + "_MODE gpio\r\n");
                                        pinConfigWriter.write("#endif\r\n");
                                    }
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_DIGITAL_OUTPUT" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_DIGITAL_OUTPUT" + k + " " + pin + "\r\n");
                                    }
                                } else if (shieldItem.get("Type").toString().equalsIgnoreCase("ANALOG")) {
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_ANALOG" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_ANALOG" + k + " " + pin + "\r\n");
                                    }
                                } else if (shieldItem.get("Type").toString().equalsIgnoreCase("UART")) {
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                        pinConfigWriter.write("#define " + pin + "_MODE gpio\r\n");
                                        pinConfigWriter.write("#endif\r\n");
                                        int no = Integer.parseInt(pin.substring(3));
                                        String serial;
                                        if (no == 13 || no == 11) {
                                            serial = "TL_Serial2";
                                        } else if (no == 37 || no == 38) {
                                            serial = "TL_Serial3";
                                        } else if (no == 21 || no == 22) {
                                            serial = "TL_Serial1";
                                        } else {
                                            serial = "TL_Serial2";
                                        }
                                        for (Object temp : shieldItem.getJSONArray("Module")) {
                                            JSONObject shieldItemModule = (JSONObject) temp;
                                            pinConfigWriter.write("#ifdef " + shieldItem.toString().toUpperCase()
                                                    + "_" + function.toString().toUpperCase() + "_Serial \r\n");
                                            pinConfigWriter.write("#undef " + shieldItem.toString().toUpperCase()
                                                    + "_" + function.toString().toUpperCase() + "_Serial \r\n");
                                            pinConfigWriter.write("#define " + shieldItem.toString().toUpperCase()
                                                    + "_" + function.toString().toUpperCase() + "_Serial \r\n");
                                            pinConfigWriter.write("#endif\r\n");
                                        }
                                    }
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_UART_TX" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_UART_TX" + k + " " + pin + "\r\n");
                                    }
                                } else if (shieldItem.get("Type").toString().equalsIgnoreCase("SPI")) {
                                    if (k == 0) {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_MOSI" + " " + pin + "\r\n");
                                    } else {
                                        configWriter.write("#define " + function.toString().toUpperCase() +
                                                "_SPI_MOSI" + k + " " + pin + "\r\n");
                                    }
                                }
                                k++;
                            }

                        }

                    }

                }
            }
        } else {
            System.out.println("No ShieldConnection information");
        }
        /// Device connection
        if (hardware.has("DeviceConnection")) {
            JSONArray deviceConnection = (JSONArray) hardware.get("DeviceConnection");
            for (int i = 0; i < deviceConnection.length(); i++) {
                JSONObject item = (JSONObject) deviceConnection.get(i);
                if (item.has("Function") && item.has("Module")) {
                    JSONArray Functions = item.getJSONArray("Function");
                    JSONArray Modules = item.getJSONArray("Module");
                    for (int j = 0; j < Functions.length(); j++) {
                        Object function = Functions.get(j);
                        Object module = Modules.get(j);
                        configWriter.write("#define TINYLINK_" + function.toString().toUpperCase() + " " +
                                module.toString().toUpperCase() + "\r\n");
                        exec(new String[]{"sh",
                                "-c", "cp -r " + libDir + Platform + "/TL_" + function.toString().toUpperCase() + '/' +
                                module.toString().toUpperCase() + "/*" + " " + buildDir});
                        if (Platform.equals("Beagle_Bone") || Platform.equals("Raspberry_Pi") || Platform.equals("Humming")) {
                            String[] externalCpps =
                                    shell("ls " + libDir + Platform + "/TL_" + function.toString().toUpperCase()
                                            + "/" + module.toString().toUpperCase() + "/*.cpp | xargs").split(" ");
                            for (String externalCpp : externalCpps) {
                                String[] temp1 = externalCpp.split("/");
                                String temp2 = temp1[temp1.length - 1].split(".cpp")[0].strip();
                                if (!CPPList.contains(temp2)) {
                                    CPPList += " " + temp2;
                                }
                            }
                            if (function.toString().equalsIgnoreCase("WIFI")) {
                                LibCurl = true;
                            }
                            if (function.toString().equalsIgnoreCase("VOICE")) {
                                LibSphinx = true;
                            }
                        }

                        if (!item.get("Form").toString().equalsIgnoreCase("PORT") ||
                                !item.get("AddPin").toString().equalsIgnoreCase("EXTRA")) {
                            if (!item.get("Input").toString().equalsIgnoreCase("NULL")) {
                                String[] Pins = item.get("Input").toString().split(",");
                                int k = 0;
                                for (String inputPin : Pins) {
                                    String pin = "";

                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pin = inputPin.substring(0, 2).toUpperCase() + "_" + inputPin.substring(3);
                                    } else {
                                        if (upper(inputPin.charAt(0)) == 'D' || upper(inputPin.charAt(0)) == 'A') {
                                            pin = inputPin.substring(1);
                                        } else {
                                            pin = inputPin.toUpperCase();
                                        }

                                    }
                                    if (upper(inputPin.charAt(0)) == 'A') {
                                        pin = "A" + pin;
                                    }
                                    if (item.get("Type").toString().equalsIgnoreCase("DIGITAL")) {
                                        if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                            pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#define " + pin + "_MODE gpio\r\n");
                                            pinConfigWriter.write("#endif\r\n");
                                        }
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_DIGITAL_INPUT" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_DIGITAL_INPUT" + k + " " + pin + "\r\n");
                                        }
                                    } else if (item.get("Type").toString().equalsIgnoreCase("I2C")) {
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_I2C_SDA" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_I2C_SDA" + k + " " + pin + "\r\n");
                                        }
                                        pin = "";
                                        if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                            pin = item.get("SCL").toString().substring(0, 2).toUpperCase() + "_" +
                                                    item.get("SCL").toString().substring(3);
                                        } else {
                                            if (upper(item.get("SCL").toString().charAt(0)) == 'D' || upper(item.get("SCL").toString().charAt(0)) == 'A') {
                                                pin = item.get("SCL").toString().substring(1).toUpperCase();
                                            } else {
                                                pin = item.get("SCL").toString().toUpperCase();
                                            }
                                        }
                                        if (upper(item.get("SCL").toString().charAt(0)) == 'A') {
                                            pin = "A" + pin;
                                        }
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_I2C_SCL" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_I2C_SCL" + k + " " + pin + "\r\n");
                                        }
                                    } else if (item.get("Type").toString().equalsIgnoreCase("UART")) {
                                        if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                            pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#define " + pin + "_MODE uart\r\n");
                                            pinConfigWriter.write("#endif\r\n");
                                        }
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_UART_RX" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_UART_RX" + k + " " + pin + "\r\n");
                                        }
                                    } else if (item.get("Type").toString().equalsIgnoreCase("PWM")) {
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_PWM" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_PWM" + k + " " + pin + "\r\n");
                                        }
                                    } else if (item.get("Type").toString().equalsIgnoreCase("SPI")) {
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_MISO" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_MISO" + k + " " + pin + "\r\n");
                                        }
                                        pin = "";
                                        if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                            pin = item.get("SCK").toString().substring(0, 2).toUpperCase() + "_" +
                                                    item.get("SCK").toString().substring(3).toUpperCase();
                                        } else {
                                            if (upper(item.get("SCK").toString().charAt(0)) == 'D' || upper(item.get("SCK").toString().charAt(0)) == 'A') {
                                                pin = item.get("SCK").toString().substring(1).toUpperCase();
                                            } else {
                                                pin = item.get("SCK").toString().toUpperCase();
                                            }
                                        }
                                        if (upper(item.get("SCK").toString().charAt(0)) == 'A') {
                                            pin = "A" + pin;
                                        }
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_SCK" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_SCK" + k + " " + pin + "\r\n");
                                        }
                                        pin = "";
                                        if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                            pin = item.get("SS").toString().substring(0, 2).toUpperCase() + "_" +
                                                    item.get("SS").toString().substring(3).toUpperCase();
                                        } else {
                                            if (upper(item.get("SS").toString().charAt(0)) == 'D' || upper(item.get("SS").toString().charAt(0)) == 'A') {
                                                pin = item.get("SS").toString().substring(1).toUpperCase();
                                            } else {
                                                pin = item.get("SS").toString().toUpperCase();
                                            }
                                        }
                                        if (upper(item.get("SS").toString().charAt(0)) == 'A') {
                                            pin = "A" + pin;
                                        }
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_SS" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_SS" + k + " " + pin + "\r\n");
                                        }
                                    }
                                    k++;

                                }
                            }
                            if (!item.get("Output").toString().equalsIgnoreCase("NULL")) {
                                if (debug) System.out.println("Check device output...");
                                String[] Pins = item.get("Output").toString().split(",");
                                int k = 0;
                                for (String outputPin : Pins) {
                                    String pin = "";
                                    if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                        pin = outputPin.substring(0, 2).toUpperCase() + "_" +
                                                outputPin.substring(3).toUpperCase();
                                    } else {
                                        if (upper(outputPin.charAt(0)) == 'D' || upper(outputPin.charAt(0)) == 'A') {
                                            pin = Integer.toString(Integer.parseInt(outputPin.substring(1))).toUpperCase();
                                        } else {
                                            pin = outputPin.toUpperCase();
                                        }
                                    }
                                    if (upper(outputPin.charAt(0)) == 'A') {
                                        pin = "A" + pin;
                                    }

                                    if (item.get("Type").toString().equalsIgnoreCase("DIGITAL")) {
                                        if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                            pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#define " + pin + "_MODE gpio\r\n");
                                            pinConfigWriter.write("#endif\r\n");
                                        }
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_DIGITAL_OUTPUT" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_DIGITAL_OUTPUT" + k + " " + pin + "\r\n");
                                        }
                                    } else if (item.get("Type").toString().equalsIgnoreCase("ANALOG")) {
                                        if (debug) System.out.println("Output type: analog");
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_ANALOG" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_ANALOG" + k + " " + pin + "\r\n");
                                        }
                                    } else if (item.get("Type").toString().equalsIgnoreCase("UART")) {
                                        if (boardID == 1004 || boardID == 1005 || boardID == 1006) {
                                            pinConfigWriter.write("#ifdef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#undef " + pin + "_MODE\r\n");
                                            pinConfigWriter.write("#define " + pin + "_MODE gpio\r\n");
                                            pinConfigWriter.write("#endif\r\n");
                                            int no = Integer.parseInt(pin.substring(3));
                                            String serial;
                                            if (no == 13 || no == 11) {
                                                serial = "TL_Serial2";
                                            } else if (no == 37 || no == 38) {
                                                serial = "TL_Serial3";
                                            } else if (no == 21 || no == 22) {
                                                serial = "TL_Serial1";
                                            } else {
                                                serial = "TL_Serial2";
                                            }
                                            for (Object temp : item.getJSONArray("Module")) {
                                                JSONObject itemModule = (JSONObject) temp;
                                                pinConfigWriter.write("#ifdef " + item.toString().toUpperCase()
                                                        + "_" + function.toString().toUpperCase() + "_Serial \r\n");
                                                pinConfigWriter.write("#undef " + item.toString().toUpperCase()
                                                        + "_" + function.toString().toUpperCase() + "_Serial \r\n");
                                                pinConfigWriter.write("#define " + item.toString().toUpperCase()
                                                        + "_" + function.toString().toUpperCase() + "_Serial \r\n");
                                                pinConfigWriter.write("#endif\r\n");
                                            }
                                        }
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_UART_TX" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_UART_TX" + k + " " + pin + "\r\n");
                                        }
                                    } else if (item.get("Type").toString().equalsIgnoreCase("SPI")) {
                                        if (k == 0) {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_MOSI" + " " + pin + "\r\n");
                                        } else {
                                            configWriter.write("#define " + function.toString().toUpperCase() +
                                                    "_SPI_MOSI" + k + " " + pin + "\r\n");
                                        }
                                    }
                                    k++;
                                }

                            }
                        } else {
                            configWriter.write("#define " + function.toString().toUpperCase() + "_" +
                                    item.get("Type").toString().toUpperCase() + " " +
                                    item.get("Port").toString().toUpperCase() + "\r\n");
                        }

                    }
                }
            }
        } else {
            System.out.println("No DeviceConnection information");
        }

        // Config.h done
        configWriter.write("#endif\r\n");
        configWriter.close();

        // CMake
        FileWriter CMakeFile = new FileWriter(new File(buildDir + "/Compile.txt"));
        if(!CPPList.equals("src = (sketch")){
            CMakeFile.write(CPPList + ")\r\n");
        }


        if (Platform.equals("Beagle_Bone")) {
            pinConfigWriter.write("#endif\r\n");
            pinConfigWriter.close();
        }

        if (Platform.equals("Beagle_Bone") || Platform.equals("Raspberry_Pi") || Platform.equals("Humming")) {
            if (LibCurl) {
                if (LibSphinx) {
                    CMakeFile.write("lib = (-lm -lz -lrt -lssl -lcrypto "+libDir+Platform+"/lib/libcurl.a -lpthread " +
                            "-lpocketsphinx " +
                            "-lsphinxbase -lsphinxad)");
                } else {
                    CMakeFile.write("lib = (-lm -lz -lrt -lssl -lcrypto "+libDir+Platform+"/lib/libcurl.a -lpthread)");
                }
            } else {
                if (LibSphinx) {
                    CMakeFile.write("lib = (-lm -lz -lrt -lpthread -lpocketsphinx -lsphinxbase -lsphinxad)");
                } else {
                    CMakeFile.write("lib = (-lm -lz -lrt -lpthread)");
                }
            }
        }
        if (Platform.equals("AliOS")) {
            shell("chmod 777 " + buildDir + " -R");
            shell("mv " + buildDir + "/*.h " + buildDir + "/sketch");
            shell("mv " + buildDir + "/*.c " + buildDir + "/sketch");
        }
        CMakeFile.close();

        System.out.println("Code generated.");
    }

    void compile(int boardID, String LibDir,String targetDir, String inputSrc) {
        createDirectory(targetDir);


        switch (boardID) {
            case 1001: {
                shell(ARDUINO_COMPILOR + " --pref" + " build.path=" + targetDir + " --board" + " LinkItOneLinuxArduino:arm:linkit_one"
                        + " --verify " + inputSrc + " 2>&1");
                break;
            }
            case 1002: {
                shell(ARDUINO_COMPILOR + " --pref" + " build.path=" + targetDir + " --board" + " arduino:avr:uno"
                        + " --verify " + inputSrc + " 2>&1");
                break;
            }
            case 1003:
            case 1004:
            case 1005:
            case 1006:
            case 1013:
                {
                shell("chmod 755 "+targetDir);

                LibDir = new File(LibDir+"Raspberry_Pi/lib").getAbsolutePath();
                String inputfile = new File(inputSrc).getPath();
                String inputParentDir = new File(inputSrc).getParentFile().getAbsolutePath();
                targetDir = new File(targetDir).getAbsolutePath();

                new File(inputSrc).renameTo(new File(inputParentDir+"/sketch.cpp"));
                if(!inputParentDir.equalsIgnoreCase(targetDir)){
                    shell("cp -r "+inputParentDir+"/* "+targetDir);
                }

                String compilor;
                if(boardID==1003){
                    compilor = RPI_COMPILOR;
                }else {
                    compilor = HUMMING_COMPILOR;
                }

                String cxxFlags = "-std=c++11 -s";
                String include = "-I"+targetDir;
                String linkLib = "-L"+LibDir+" -Wl,-rpath-link "+LibDir;
                String libs = "";

                if(debug)System.out.println("compile file: "+inputParentDir+ "/Compile.txt");
                File compile = new File( inputParentDir+ "/Compile.txt");

                try {
                    Scanner scanner = new Scanner(compile);
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        String[] items;
                        if (line.contains("src")) {
                            if(debug)System.out.println("line: "+line);
                            items = line.split("= \\(")[1].split("\\)")[0].split(" ");
                            for (String item : items) {
                                if(debug)System.out.println("item: "+item);
                                item = new File(targetDir+"/"+item).getAbsolutePath();
                                shell( compilor + " " + cxxFlags + " -c " + item + ".cpp -o " + item + ".o " + include + " 2>&1");

                            }
                        } else if (line.contains("lib")) {
                            libs = line.split("= \\(")[1].split("\\)")[0];
                        }
                    }
                    scanner.close();
                } catch (Exception e) {
                    System.out.println(e.toString());
                }

                try {
                    String cmd=compilor + " " + cxxFlags +" -o "+targetDir+"/main ";
                    Scanner scanner = new Scanner(compile);
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        String[] items;
                        if (line.contains("src")) {
                            items = line.split("= \\(")[1].split("\\)")[0].split(" ");
                            for (String item : items) {
                                item = new File(targetDir+"/"+item).getAbsolutePath();
                                cmd+=item+".o ";
                            }
                        }
                    }
                    cmd+=" "+linkLib+" "+libs+" "+include;
                    shell(cmd);
                    scanner.close();
                } catch (Exception e) {
                    System.out.println(e.toString());
                }

                break;
            }
            case 1007:{
                shell(ARDUINO_COMPILOR + " --pref" + " build.path=" + targetDir + " --board" + " arduino:avr:micro"
                        + " --verify " + inputSrc + " 2>&1");
                break;
            }
            case 1008:{
                shell(ARDUINO_COMPILOR + " --pref" + " build.path=" + targetDir + " --board" + " arduino:avr:mega:cpu=atmega2560"
                        + " --verify " + inputSrc + " 2>&1");
                break;
            }
            case 1009:{
                shell(ARDUINO_COMPILOR + " --pref" + " build.path=" + targetDir + " --board" + " arduino:sam:due"
                        + " --verify " + inputSrc + " 2>&1");
                break;
            }
            case 1010:{
                shell(ARDUINO_COMPILOR + " --pref" + " build.path=" + targetDir + " --board" + " arduino:avr:nano:cpu=atmega328"
                        + " --verify " + inputSrc + " 2>&1");
                break;
            }
            case 1011:{
                String inputParentDir = new File(inputSrc).getParentFile().getAbsolutePath();
                targetDir = new File(targetDir).getAbsolutePath();
                new File(inputSrc).renameTo(new File(inputParentDir+"/sketch.cpp"));

                shell("cd "+inputParentDir+"&& "+MBED_COMPILOR+" compile -t GCC_ARM -m nRF51822 --build "+targetDir+" 2>&1");
                break;
            }
//            case 1012:{
//                inputSrc = new File(inputSrc).getPath();
//                shell("cd "+targetDir+" && "+ALIOS_COMPILOR+" make "+inputSrc+"@mk3060 2>&1");
//                shell("cd "+targetDir+" && mv out/sketch@mk3060/binary/sketch@mk3060.ota.bin sketch.ota.bin");
//                break;
//            }
            default:{
                System.out.println("Unknown Platform.");
            }
        }
    }

    static public void main(String args[]) throws Exception {
        if(args.length==0){
            System.out.println("Usage: java -jar OneLinkLang.jar command [srcfile]");
        }

        if(args[0].equalsIgnoreCase("clean")){
            OneLinkCompiler.shell("rm -r sketch ELF Hardware.json  HardwareList.txt connection.jpg connection.txt " +
                    "sketch.ino");
            return;
        }

        if (args.length < 2) {
            System.out.println("Usage: java -jar OneLinkLang.jar command srcfile");
            return;
        }
        OneLinkCompiler compiler = new OneLinkCompiler(args[1]);
        compiler.debug = true;

        if(args[0].equalsIgnoreCase("correct")){
            compiler.correctCode();
        }else if (args[0].equalsIgnoreCase("extract")) {
            compiler.writeToFile(compiler.extractHardwareDemand().toString(), "Function.json");
        } else if (args[0].equalsIgnoreCase("select")) {
            OneLinkCompiler.exec(new String[]{"./main", "Function.json",
                    "Hardware.json", "HardwareList.txt", "connection.jpg", "connection.txt"});
        } else if (args[0].equalsIgnoreCase("generate")) {
            int boardID = compiler.readBoardID("./HardwareList.txt");
            compiler.genCode(boardID, "Hardware.json", "sketch.ino", "sketch/", "../Lib/");
        } else if (args[0].equalsIgnoreCase("build")) {
            int boardID = compiler.readBoardID("./HardwareList.txt");
            compiler.compile(boardID, "../Lib/","ELF/", "sketch/sketch.ino");
        }
        if (args[0].equalsIgnoreCase("all")) {
            compiler.correctCode();
            compiler.writeToFile(compiler.extractHardwareDemand().toString(), "Function.json");
            OneLinkCompiler.exec(new String[]{"./main", "Function.json",
                    "Hardware.json", "HardwareList.txt", "connection.jpg", "connection.txt"});
            int boardID = compiler.readBoardID("./HardwareList.txt");
            compiler.genCode(boardID, "Hardware.json", "sketch.ino", "sketch/", "../Lib/");
            compiler.compile(boardID, "../Lib/","ELF/", "sketch/sketch.ino");
        }


//        System.out.println(compiler.extractHardwareDemand());

//        System.out.println(compiler.extractRecommandation("board", ""));


//        System.out.println(boardID);

    }
}
