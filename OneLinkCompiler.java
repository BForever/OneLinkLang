
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.json.*;

import java.io.*;
import java.util.List;

public class OneLinkCompiler {
    CommonTokenStream tokens;
    ParseTree tree;
    ParseTreeWalker walker;
    boolean debug = false;

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

    public void exec(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateHardware(String username) {
        exec("sh ./main " + username + " Function.json");
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

    public void genCode(int boardID, String username, String uploadFileName,String hardwarePath,String configPath,
                        String cmakePath,String inputSrc,String outputSrc,String buildDir,String libDir) throws Exception {
        System.out.println("Start generate code...");
        // Read hardware.json
        JSONTokener tokener = new JSONTokener(new FileReader(new File(hardwarePath)));
        JSONObject hardware = new JSONObject(tokener);
        // Construct TL_Config.h
        String targetDirectory = "";
        FileWriter configWriter = new FileWriter(configPath, false);
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
                exec("cp "+libDir+"/Arduino/TinyLink/* "+buildDir);
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
                exec("cp ../Lib/" + Platform + "/TL_" + function.toString().toUpperCase() + '/' +
                        module.toString().toUpperCase() + "/* " + "../user_log/" + username +
                        "Information/temp" + "/sketch -r");
            }
        } catch (JSONException e) {
            System.out.println("No board information");
            e.printStackTrace();
        }
        /// Shield connection TODO
        try {
            JSONObject shieldConnection = (JSONObject) hardware.get("ShieldConnection");
        } catch (JSONException e) {
            System.out.println("No ShieldConnection information");
            e.printStackTrace();
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
                    exec("cp ../Lib/" + Platform + "/TL_" + function.toString().toUpperCase() + '/' +
                            module.toString().toUpperCase() + "/* " + "../user_log/" + username +
                            "Information/temp/sketch -r");


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
        FileWriter CMakeFile = new FileWriter(new File("../user_log/" + username + "Information/temp/" + uploadFileName +
                "/Compile.txt"));
        CMakeFile.write(CPPList + ")\r\n");
        CMakeFile.close();

        System.out.println("Code generated.");
    }

    static public void main(String args[]) throws Exception {
        OneLinkCompiler compiler = new OneLinkCompiler(args[0]);
//        compiler.debug=true;
//        compiler.writeToFile(compiler.extractHardwareDemand().toString(),"Function.json");
//        System.out.println(compiler.extractHardwareDemand());
//        compiler.exec("./main Function.json Hardware.json HardwareList.txt connection.jpg connection.txt");
//        System.out.println(compiler.extractRecommandation("board", ""));
        int boardID = compiler.readBoardID("./HardwareList.txt");
        System.out.println(boardID);




    }
}
