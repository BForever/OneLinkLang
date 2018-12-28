
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.json.*;

import java.io.*;
import java.util.List;

public class OneLinkCompiler {
    CommonTokenStream tokens;
    ParseTree tree;
    ParseTreeWalker walker;
    boolean debug=false;

    void writeToFile(String contents,String fileName) {
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
        writeToFile(extractor.filtered,"filtered.cpp");

        JSONArray requirements = new JSONArray();

        for (OneLinkHardwareExtractor.MeasureRange m : extractor.measureRangeList) {
            extractor.lib.remove(m.dev);
            if(debug)System.out.println("required measureRange: [" + m.dev + "] from " + m.lowerBound + " " + m.unit + " to " +
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
            if(debug)System.out.println("required lib: " + str);
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
                if(debug)System.out.print("required devices:(");
                List<String> devices = OneLinkHardwareExtractor.getTextFromInitializerlist(ictx);

                for (String str : devices) {
                    if(debug)System.out.print(" " + str);
                    mandatory.put(str.substring(1, str.length() - 1));
                }

                if(debug)System.out.println(" )");
            }
        }
        if(!requirements.isEmpty())
            hardware.put("Requirements", requirements);
        if(!mandatory.isEmpty())
            hardware.put("Mandatory", mandatory);
        if(debug)System.out.println(" ");
        if(debug)System.out.println(hardware);

        return hardware;
    }

    public JSONObject extractRecommandation(String boardName, String outfile) {
        JSONObject hardware = extractHardwareDemand();
        JSONObject recommand = new JSONObject();
        recommand.put("Requirements", hardware.getJSONArray("Requirements"));
        recommand.put("Mandatory", boardName);
        return recommand;
    }

    public void generateHardware(String username){
        try{
            Process cmd = Runtime.getRuntime().exec("sh ./main "+username+" Function.json");
            cmd.waitFor();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    static public void main(String args[]) throws Exception {
        OneLinkCompiler compiler = new OneLinkCompiler(args[0]);
//        compiler.debug=true;
//        compiler.writeToFile(compiler.extractHardwareDemand().toString(),"../share/Function.json");
        System.out.println(compiler.extractHardwareDemand());
//        System.out.println(compiler.extractRecommandation("board", ""));

    }
}
