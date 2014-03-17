package cz.vity.freerapid.plugins.services.thefileupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class TheFileUploadFileRunner extends XFileSharingRunner {
    @Override
    protected void setLanguageCookie() throws Exception {
        if (fileURL.contains("efileuploading.com"))
            fileURL = fileURL.replace("efileuploading.com", "thefileupload.com");
        super.setLanguageCookie();
    }
}