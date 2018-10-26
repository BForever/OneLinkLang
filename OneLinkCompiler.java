import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.FileInputStream;


public class OneLinkCompiler {
    static public void main(String args[]) throws Exception{
        FileInputStream file = new FileInputStream(args[0]);
        ANTLRInputStream input = new ANTLRInputStream(file);
        OneLinkLexer lexer = new OneLinkLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OneLinkParser parser = new OneLinkParser(tokens);
        ParseTree tree = parser.compliationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new OneLinkListener(),tree);

    }
}
