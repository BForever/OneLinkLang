import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;

import java.util.*;

public class OneLinkDebugger extends OneLinkParserBaseListener {
    public class MeasureRange{
        String dev;
        int upperBound;
        int lowerBound;
        String unit;

        MeasureRange(String dev,int lowerBound,int upperBound, String unit){
            this.dev = dev;
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
            this.unit = unit;
        }
    }
    List<MeasureRange>measureRangeList;
    Set<String> lib;
    Set<OneLinkParser.UnqualifiedidContext> req;
    TokenStreamRewriter rewriter;
    String filtered;
    OneLinkDebugger(TokenStream tokens){
        lib = new HashSet<String>();
        req = new HashSet<OneLinkParser.UnqualifiedidContext>();
        measureRangeList = new ArrayList<MeasureRange>();

        rewriter = new TokenStreamRewriter(tokens);

    }
//    @Override public void exitDeviceTranslationunit(OneLinkParser.DeviceTranslationunitContext ctx) {
//        filtered = rewriter.getText(ctx.getSourceInterval());
//    }

    @Override public void enterClassname(OneLinkParser.ClassnameContext ctx) {
        String name = ctx.getText();
        int index = name.indexOf("TL_");
        if(index==0){
            name = name.substring(3);
            lib.add(name);
        }
    }
    @Override public void enterUnqualifiedid(OneLinkParser.UnqualifiedidContext ctx) {
        String name = ctx.getText();
        int index = name.indexOf("TL_");
        if(index==0){
            name = name.substring(3);
            ParserRuleContext p;
            try{
                p  = ctx.getParent().getParent().getParent().getParent();
                if(p instanceof OneLinkParser.PostfixexpressionContext){
                    OneLinkParser.PostfixexpressionContext post = (OneLinkParser.PostfixexpressionContext)p;
                    if(post.idexpression().unqualifiedid().getText().equals("setMeasuringRange")){
                        p = p.getParent();
                        List<String> list = getTextFromInitializerlist(((OneLinkParser.PostfixexpressionContext)p).expressionlist().initializerlist());
                        if(list.size()>=3){
                            measureRangeList.add(new MeasureRange(name, Integer.parseInt(list.get(0)) ,Integer.parseInt(list.get(1)),list.get(2).substring(1,list.get(2).length()-1)));
                        }else {
                            System.err.println("setMeasuringRange function needs three parameters");
                        }
                    }else {
                        lib.add(name);
                    }
                }else {
                    lib.add(name);
                }

            }catch (NullPointerException e){
                System.err.println("invalid grammar: "+e.toString());
            }
        }
        if(name.equals("REQUIRE")){
            ParserRuleContext p;
            try {
                p = ctx.getParent().getParent().getParent().getParent().getParent();
                rewriter.delete(p.start.getTokenIndex(),p.stop.getTokenIndex()+1);
            }catch (NullPointerException e){
                System.err.println("invalid grammar: "+e.toString());
            }
            req.add(ctx);
        }
    }
    private List<String> getTextFromInitializerlist(OneLinkParser.InitializerlistContext list){
        List<String> result = new LinkedList<String>();

        while (list!=null){
            if(list.initializerclause()!=null){
                ((LinkedList<String>) result).addFirst(list.initializerclause().getText());
            }
            list = list.initializerlist();
        }

        return result;
    }

    /**
     * 采用动态规划的方法（字符串匹配相似度）
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
}
