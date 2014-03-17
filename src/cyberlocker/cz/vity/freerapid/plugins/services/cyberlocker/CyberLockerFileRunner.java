package cz.vity.freerapid.plugins.services.cyberlocker;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class CyberLockerFileRunner extends XFileSharingRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        String content = getContentAsString();
        if (content.contains("down_direct")) {      // Removing registration form from within the download form
            final int start = content.indexOf("<Form method=\"POST\" onSubmit=\"return CheckForm(this)\">");
            final int end = content.indexOf("</Form>");
            content = content.substring(0, start) + content.substring(end);
        }
        final MethodBuilder methodBuilder = getMethodBuilder(content)
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL);
        if (!methodBuilder.getParameters().get("method_free").isEmpty()) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }

}