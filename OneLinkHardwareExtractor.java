import org.antlr.v4.runtime.*;

import java.util.*;

public class OneLinkHardwareExtractor extends OneLinkParserBaseListener {
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
    private TokenStreamRewriter rewriter;
    String filtered;
    private CommonTokenStream tokens;
    OneLinkHardwareExtractor(CommonTokenStream tokens){
        lib = new HashSet<String>();
        req = new HashSet<OneLinkParser.UnqualifiedidContext>();
        measureRangeList = new ArrayList<MeasureRange>();

        this.tokens = tokens;

        rewriter = new TokenStreamRewriter(tokens);

    }


    @Override public void exitDeviceTranslationunit(OneLinkParser.DeviceTranslationunitContext ctx) {
        Token start = ctx.getStart();
        List<Token> defines = tokens.getHiddenTokensToLeft(start.getTokenIndex());

        for(Token define:defines){
            rewriter.insertBefore(ctx.start, define.getText());
        }

        filtered = rewriter.getText(ctx.getSourceInterval());
    }

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
        if(name.equalsIgnoreCase("REQUIRE")){
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
    static List<String> getTextFromInitializerlist(OneLinkParser.InitializerlistContext list){
        List<String> result = new LinkedList<String>();

        while (list!=null){
            if(list.initializerclause()!=null){
                ((LinkedList<String>) result).addFirst(list.initializerclause().getText());
            }
            list = list.initializerlist();
        }

        return result;
    }
}
