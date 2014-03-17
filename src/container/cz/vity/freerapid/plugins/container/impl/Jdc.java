package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerException;
import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.utilities.LogUtils;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class Jdc implements ContainerFormat {
    private final static Logger logger = Logger.getLogger(Jdc.class.getName());

    public static String[] getSupportedFiles() {
        return new String[]{"jdc"};
    }

    private final ContainerPlugin plugin;

    public Jdc(final ContainerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FileInfo> read(final InputStream is) throws Exception {
        final String userInput = plugin.getDialogSupport().askForPassword("JDC container");
        if (userInput == null) {
            throw new ContainerException("Password required");
        }
        final byte[] key = DigestUtils.md5(userInput);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
        final ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new HexInputStream(new CipherInputStream(is, cipher))));
        final Object o = ois.readObject();
        ois.close();
        if (o == null || !(o instanceof List)) {
            throw ContainerException.fileIsCorrupt();
        }
        return toInfoList((List<FilePackage>) o);
    }

    private static List<FileInfo> toInfoList(final List<FilePackage> srcList) {
        final List<FileInfo> list = new LinkedList<FileInfo>();
        for (final FilePackage filePackage : srcList) {
            final String comment = filePackage.comment;
            for (final DownloadLink downloadLink : filePackage.downloadLinkList) {
                if (downloadLink.urlDownload != null) {
                    try {
                        final FileInfo info = new FileInfo(new URL(downloadLink.urlDownload));
                        if (downloadLink.forcedFileName != null) {
                            info.setFileName(downloadLink.forcedFileName);
                        } else if (downloadLink.finalFileName != null) {
                            info.setFileName(downloadLink.finalFileName);
                        } else if (downloadLink.name != null) {
                            info.setFileName(downloadLink.name);
                        }
                        if (downloadLink.downloadMax > 0) {
                            info.setFileSize(downloadLink.downloadMax);
                        }
                        info.setDescription(comment);
                        list.add(info);
                    } catch (MalformedURLException e) {
                        LogUtils.processException(logger, e);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public void write(final List<FileInfo> files, final OutputStream os) throws Exception {
        final String userInput = plugin.getDialogSupport().askForPassword("JDC container");
        if (userInput == null) {
            throw new ContainerException("Password required");
        }
        final byte[] key = DigestUtils.md5(userInput);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
        final ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new HexOutputStream(new CipherOutputStream(os, cipher))));
        oos.writeObject(toFilePackageList(files));
        oos.close();
    }

    private static ArrayList<FilePackage> toFilePackageList(final List<FileInfo> srcList) {
        final ArrayList<FilePackage> list = new ArrayList<FilePackage>(1);
        final FilePackage filePackage = new FilePackage();
        filePackage.downloadLinkList = new ArrayList<DownloadLink>(srcList.size());
        for (final FileInfo info : srcList) {
            final DownloadLink downloadLink = new DownloadLink();
            downloadLink.urlDownload = info.getFileUrl().toString();
            if (info.getFileName() != null) {
                downloadLink.finalFileName = info.getFileName();
                downloadLink.name = info.getFileName();
            }
            if (info.getFileSize() > 0) {
                downloadLink.downloadMax = info.getFileSize();
            }
            filePackage.downloadLinkList.add(downloadLink);
        }
        list.add(filePackage);
        return list;
    }

}
