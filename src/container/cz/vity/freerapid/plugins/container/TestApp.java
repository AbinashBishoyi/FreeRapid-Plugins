package cz.vity.freerapid.plugins.container;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.dev.plugimpl.DevDialogSupport;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.utilities.LogUtils;
import org.jdesktop.application.Application;

import java.io.FileInputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public final class TestApp extends PluginDevApplication {
    private final static Logger logger = Logger.getLogger(TestApp.class.getName());

    private final static String FILE = "C:\\Users\\Administrator\\Desktop\\1.ccf";
    private final static String FILE2 = "C:\\Users\\Administrator\\Desktop\\2.rsdf";

    @Override
    protected void startup() {
        try {
            final ContainerPlugin plugin = new ContainerPluginImpl();
            plugin.setConnectionSettings(new ConnectionSettings());
            plugin.setDialogSupport(new DevDialogSupport(getContext()));

            List<FileInfo> list = plugin.read(new FileInputStream(FILE), FILE);
            logList(list);

            //plugin.write(list, new FileOutputStream(FILE2), FILE2);
            //list = plugin.read(new FileInputStream(FILE2), FILE2);
            //logList(list);
        } catch (final Exception e) {
            LogUtils.processException(logger, e);
        }
    }

    private void logList(final List<FileInfo> list) {
        final StringBuilder sb = new StringBuilder("URLs in list:");
        for (final FileInfo info : list) {
            sb.append('\n').append(info.getFileUrl().toString());
        }
        logger.info(sb.toString());
    }

    private void prepareList(final List<FileInfo> list) throws Exception {
        FileInfo file;
        file = new FileInfo(new URL("http://www.example.com/"));
        file.setFileName("test1");
        file.setDescription("test");
        file.setFileSize(123456);
        list.add(file);
        file = new FileInfo(new URL("http://wordrider.net/freerapid/"));
        file.setFileName("test2");
        list.add(file);
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}