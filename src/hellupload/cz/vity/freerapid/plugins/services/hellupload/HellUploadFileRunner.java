package cz.vity.freerapid.plugins.services.hellupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HellUploadFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.remove(0);  //FileNameHandlerA
        return fileNameHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .setAction(fileURL);
        if ((methodBuilder.getParameters().get("method_free") != null) && (!methodBuilder.getParameters().get("method_free").isEmpty())) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }


}