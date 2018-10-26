public class OneLinkListener extends OneLinkParserBaseListener {
    @Override public void enterMobileAPP(OneLinkParser.MobileAPPContext ctx) {
        System.out.print(ctx.mobileAppBlocks().mobileProgramBlock().mobileTranslationunit().getText());
    }
}
