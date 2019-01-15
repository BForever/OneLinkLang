
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.json.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

public class OneLinkCompiler {
    CommonTokenStream tokens;
    ParseTree tree;
    ParseTreeWalker walker;
    boolean debug = false;

    static final String ARDUINO_COMPILOR = "/home/freg/arduino-1.6.9/arduino";    // Arduino compilor location
    static final String RPI_COMPILOR = "/home/freg/gcc-linaro-arm-linux-gnueabihf-raspbian/bin/arm-linux-gnueabihf-g++";  // Raspberry Pi compilor location
    static final String BBB_COMPILOR = "/home/freg/gcc-linaro-arm-linux-gnueabihf-4.8/bin/arm-linux-gnueabihf-g++";       // BeagleBone compilor location
    static final String HUMMING_COMPILOR = "/home/freg/gcc-linaro-arm-linux-gnueabihf-4.8/bin/arm-linux-gnueabihf-g++";   // HummingBoard compilor location

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
            FileInputStream file = new FileInputStream(filename);
            ANTLRInputStream input = new ANTLRInputStream(file);
            OneLinkLexer lexer = new OneLinkLexer(input);
            tokens = new CommonTokenStream(lexer);
            OneLinkParser parser = new OneLinkParser(tokens);
            tree = parser.compliationUnit();
            walker = new ParseTreeWalker();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public JSONObject extractHardwareDemand() {
        OneLinkHardwareExtractor extractor = new OneLinkHardwareExtractor(tokens);
        JSONObject hardware = new JSONObject();
        walker.walk(extractor, tree);

        // Generate filtered code
        writeToFile(extractor.filtered, "filtered.cpp");

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

    public void exec(String[] cmd) {
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
            }
            while ((line = errreader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
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
        }
        // Platform
        switch (Platform) {
            case "Arduino": {
                exec(new String[]{"sh", "-c", "cp " + libDir + "/Arduino/TinyLink/* " + buildDir});
            }
        }
        // Hardware TODO
        /// Function TODO Beagle_Bone...
        try {
            JSONObject board = (JSONObject) hardware.get("Board");
            JSONArray Functions = (JSONArray) board.get("Function");
            JSONArray Modules = (JSONArray) board.get("Module");
            for (int i = 0; i < Functions.length(); i++) {
                Object function = Functions.get(i);
                Object module = Modules.get(i);
                configWriter.write("#define TINYLINK_" + function.toString().toUpperCase() + " " +
                        module.toString().toUpperCase() + "\r\n");
                exec(new String[]{"sh", "-c", "cp " + libDir + "/" + Platform + "/TL_" + function.toString().toUpperCase() + '/' +
                        module.toString().toUpperCase() + "/*" + " " + buildDir + " -r"});
            }
        } catch (JSONException e) {
            System.out.println("No board information");
//            e.printStackTrace();
        }
        /// Shield connection TODO
        try {
            JSONObject shieldConnection = (JSONObject) hardware.get("ShieldConnection");
        } catch (JSONException e) {
            System.out.println("No ShieldConnection information");
//            e.printStackTrace();
        }
        /// Device connection
        try {
            JSONArray deviceConnection = (JSONArray) hardware.get("DeviceConnection");
            for (int i = 0; i < deviceConnection.length(); i++) {
                JSONObject item = (JSONObject) deviceConnection.get(i);
                JSONArray Functions = (JSONArray) item.get("Function");
                JSONArray Modules = (JSONArray) item.get("Module");
                for (int j = 0; j < Functions.length(); j++) {
                    Object function = Functions.get(j);
                    Object module = Modules.get(j);
                    configWriter.write("#define TINYLINK_" + function.toString().toUpperCase() + " " +
                            module.toString().toUpperCase() + "\r\n");
                    exec(new String[]{"sh",
                            "-c", "cp " + libDir + "/" + Platform + "/TL_" + function.toString().toUpperCase() + '/' +
                            module.toString().toUpperCase() + "/*" + " " + buildDir + " -r"});


                    if (item.get("Form").toString().toUpperCase().equals("PORT") &&
                            item.get("AddPin").toString().toUpperCase().equals("EXTRA")) {
                        configWriter.write("#define " + function.toString().toUpperCase() +
                                "_" + item.get("Type").toString().toUpperCase() + " " +
                                item.get("Port").toString().toUpperCase() + "\r\n");
                    }
                    // TODO else

                }

            }


        } catch (JSONException e) {
            System.out.println("No DeviceConnection information");
            System.out.println(e.toString());
        }

        // Config.h done
        configWriter.write("#endif\r\n");
        configWriter.close();

        // CMake
        FileWriter CMakeFile = new FileWriter(new File(buildDir + "/Compile.txt"));
        CMakeFile.write(CPPList + ")\r\n");
        CMakeFile.close();

        System.out.println("Code generated.");
    }

    void compile(int boardID, String targetDir, String inputSrc) {
        createDirectory(targetDir);
        switch (boardID) {
            case 1002: {
                exec(new String[]{ARDUINO_COMPILOR, "--pref", "build.path=" + targetDir, "--board", "arduino:avr:uno"
                        , "--verify", inputSrc, "2>&1"});
            }
        }
    }

    static public void main(String args[]) throws Exception {
        OneLinkCompiler compiler = new OneLinkCompiler(args[0]);
        compiler.debug = true;
        compiler.writeToFile(compiler.extractHardwareDemand().toString(), "Function.json");
//        System.out.println(compiler.extractHardwareDemand());
        compiler.exec(new String[]{"./main", "Function.json",
                "Hardware.json", "HardwareList.txt", "connection.jpg", "connection.txt"});
//        System.out.println(compiler.extractRecommandation("board", ""));
        int boardID = compiler.readBoardID("./HardwareList.txt");
        compiler.genCode(boardID, "Hardware.json", "filtered.cpp", "build", "../Lib");
        compiler.compile(boardID,"target","build/filtered.cpp");
//        System.out.println(boardID);

    }
}
