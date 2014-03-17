package cz.vity.freerapid.plugins.services.hulkload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HulkLoadFileRunner extends XFileSharingRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder(getContentAsString() + "</Form>")   //# one page was missing a tag
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL);
        if (!methodBuilder.getParameters().get("method_free").isEmpty()) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }
}