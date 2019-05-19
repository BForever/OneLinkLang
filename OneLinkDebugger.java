import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class OneLinkDebugger extends OneLinkParserBaseListener {
    //    public class MeasureRange {
//        String dev;
//        int upperBound;
//        int lowerBound;
//        String unit;
//
//        MeasureRange(String dev, int lowerBound, int upperBound, String unit) {
//            this.dev = dev;
//            this.upperBound = upperBound;
//            this.lowerBound = lowerBound;
//            this.unit = unit;
//        }
//    }
    public class API_call {
        String className;
        String function;
        int line;
        int location;

        API_call(String className, String function, int line, int location) {
            this.className = className;
            this.function = function;
            this.line = line;
            this.location = location;
        }
    }
//
//    List<MeasureRange> measureRangeList;
//    Set<String> lib;
//    Set<OneLinkParser.UnqualifiedidContext> req;
//    TokenStreamRewriter rewriter;
//    String filtered;

    private Map<String, String[]> API_map;
    private boolean hasError;

    //    OneLinkDebugger(TokenStream tokens){
//        lib = new HashSet<String>();
//        req = new HashSet<OneLinkParser.UnqualifiedidContext>();
//        measureRangeList = new ArrayList<MeasureRange>();
//
//        rewriter = new TokenStreamRewriter(tokens);
    OneLinkDebugger(String docfile) {
        API_map = new HashMap<String, String[]>();
        hasError = false;
        try {
            Scanner scanner = new Scanner(new File(docfile));
            while (scanner.hasNext()) {
                String module = scanner.nextLine().substring(3);
                String[] functions = scanner.nextLine().split(" ");
                API_map.put(module, functions);
            }
            scanner.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        System.out.println("Start checking code...");

    }
    @Override public void exitDeviceTranslationunit(OneLinkParser.DeviceTranslationunitContext ctx) {
        if(!hasError){
            System.out.println("No API mismatch found, code checking done.");
        }
    }

//    @Override
//    public void enterClassname(OneLinkParser.ClassnameContext ctx) {
//        String name = ctx.getText();
//        int index = name.indexOf("TL_");
//        if (index == 0) {
//            name = name.substring(3);
//            ctx.start.getLine();
//            lib.add(name);
//        }
//    }

    @Override
    public void enterUnqualifiedid(OneLinkParser.UnqualifiedidContext ctx) {
        String name = ctx.getText();
        int index = name.indexOf("TL_");
        if (index == 0) {
            name = name.substring(3);

            // Class name error
            if (!API_map.containsKey(name)) {
                String[] keys = API_map.keySet().toArray(new String[0]);
                String similar = mostSimilar(name, keys);
                System.out.println("Line " + ctx.start.getLine() + ":" + ctx.start.getCharPositionInLine());
                System.out.println("API not match: 'TL_" + name + "'");
                System.out.println("You are probably referring to 'TL_" + similar + "'");
                hasError = true;
            } else {// Class name correct
                ParserRuleContext p;
                try {
                    p = ctx.getParent().getParent().getParent().getParent();
                    if (p instanceof OneLinkParser.PostfixexpressionContext) {
                        OneLinkParser.PostfixexpressionContext post = (OneLinkParser.PostfixexpressionContext) p;
                        OneLinkParser.UnqualifiedidContext q = post.idexpression().unqualifiedid();
                        String function = q.getText();
                        String[] functions = API_map.get(name);
                        if (!arrayContains(function, functions)) {
                            String similar = mostSimilar(function, functions);
                            System.out.println("Line " + q.start.getLine() + ":" + q.start.getCharPositionInLine());
                            System.out.println("API not match: 'TL_" + name + "." + function + "'");
                            System.out.println("You are probably referring to 'TL_" + name + "." + similar + "'");
                            hasError = true;
                        }
                    }
                } catch (NullPointerException e) {
                    System.err.println("invalid grammar: " + e.toString());
                }
            }
        }
    }

    private boolean arrayContains(String target, String[] list) {
        for (String item : list) {
            if (target.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getTextFromInitializerlist(OneLinkParser.InitializerlistContext list) {
        List<String> result = new LinkedList<String>();

        while (list != null) {
            if (list.initializerclause() != null) {
                ((LinkedList<String>) result).addFirst(list.initializerclause().getText());
            }
            list = list.initializerlist();
        }

        return result;
    }

    private String mostSimilar(String source, String[] targets) {
        int i = 0;
        int shortest = Integer.MAX_VALUE;
        int index = Integer.MAX_VALUE;
        for (String target : targets) {
            int distance = editDistance(source, target);
            if (distance < shortest) {
                shortest = distance;
                index = i;
            }
            i++;
        }
        if (shortest == Integer.MAX_VALUE) {
            return null;
        } else {
            return targets[index];
        }
    }

    /**
     * 采用动态规划的方法（字符串匹配相似度）
     *
     * @param source 源
     * @param target 要匹配的字符串
     * @return
     */
    private int editDistance(String source, String target) {
        char[] sources = source.toCharArray();
        char[] targets = target.toCharArray();
        int sourceLen = sources.length;
        int targetLen = targets.length;
        int[][] d = new int[sourceLen + 1][targetLen + 1];
        for (int i = 0; i <= sourceLen; i++) {
            d[i][0] = i;
        }
        for (int i = 0; i <= targetLen; i++) {
            d[0][i] = i;
        }

        for (int i = 1; i <= sourceLen; i++) {
            for (int j = 1; j <= targetLen; j++) {
                if (sources[i - 1] == targets[j - 1]) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    //插入
                    int insert = d[i][j - 1] + 1;
                    //删除
                    int delete = d[i - 1][j] + 1;
                    //替换
                    int replace = d[i - 1][j - 1] + 1;
                    d[i][j] = Math.min(insert, delete) > Math.min(delete, replace) ? Math.min(delete, replace) :
                            Math.min(insert, delete);
                }
            }
        }
        return d[sourceLen][targetLen];
    }

    public static void main(String[] args) {
        OneLinkDebugger oneLinkDebugger = new OneLinkDebugger("../share/API_list.txt");
        oneLinkDebugger.API_map.forEach((key, value) -> {
            System.out.println(key + "：");
            for (String fun : value) {
                System.out.println("\t" + fun);
            }
        });
    }
}
