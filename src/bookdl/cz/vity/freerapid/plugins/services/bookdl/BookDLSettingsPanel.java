package cz.vity.freerapid.plugins.services.bookdl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author CrazyCoder
 */
public class BookDLSettingsPanel extends JPanel {
    private BookDLSettingsConfig config;

    public BookDLSettingsPanel(BookDLServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JCheckBox downloadPDF = new JCheckBox("Download PDF", config.isDownloadPDF());
        final JCheckBox downloadEPUB = new JCheckBox("Download EPUB", config.isDownloadEPUB());
        final JCheckBox downloadMOBI = new JCheckBox("Download MOBI", config.isDownloadMOBI());

        downloadPDF.setAlignmentX(Component.LEFT_ALIGNMENT);
        downloadEPUB.setAlignmentX(Component.LEFT_ALIGNMENT);
        downloadMOBI.setAlignmentX(Component.LEFT_ALIGNMENT);

        downloadPDF.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDownloadPDF(downloadPDF.isSelected());
            }
        });
        downloadEPUB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDownloadEPUB(downloadEPUB.isSelected());
            }
        });
        downloadMOBI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDownloadMOBI(downloadMOBI.isSelected());
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(downloadPDF);
        add(downloadEPUB);
        add(downloadMOBI);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
