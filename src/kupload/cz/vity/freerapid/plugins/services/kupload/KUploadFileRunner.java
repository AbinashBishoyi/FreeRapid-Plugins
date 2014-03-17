package cz.vity.freerapid.plugins.services.kupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class KUploadFileRunner extends XFileSharingRunner {

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        setFileStreamContentTypes(new String[0], new String[]{"application/cgi"});
        super.checkNameAndSize();
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        final String action = URLDecoder.decode(URLDecoder.decode(method.getURI().toString(), "UTF-8"), "UTF-8"); //decode twice
        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(action).toGetMethod();
        httpMethod.removeRequestHeader("Accept-Encoding"); //disable gzip,deflate encoding
        return super.tryDownloadAndSaveFile(httpMethod);
    }
}