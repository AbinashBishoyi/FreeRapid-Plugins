package cz.vity.freerapid.plugins.services.glumbouploads;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandlerA;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandlerB;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandlerC;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.List;
import java.util.LinkedList;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GlumboUploadsFileRunner extends XFileSharingRunner {
    
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new GlumboUploadsFileNameHandler());
        return fileNameHandlers;
    }
}