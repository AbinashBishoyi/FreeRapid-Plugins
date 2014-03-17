package cz.vity.freerapid.plugins.services.ok2upload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OK2UploadFileRunner extends XFileSharingRunner {

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<a id=\"iddirect_link\" href\\s*=\\s*\"(.*)\">");
        return downloadLinkRegexes;
    }

}