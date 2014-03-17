package cz.vity.freerapid.plugins.services.embedupload;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * @author tong2shot
 */
public class EmbedUploadSettingsPanel extends JPanel {
    private EmbedUploadSettingsConfig config;

    private final JCheckBox checkDownloadService = new JCheckBox("Queue all links");

    public EmbedUploadSettingsPanel(EmbedUploadServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        checkDownloadService.setSelected(config.isQueueAllLinks());
        checkDownloadService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQueueAllLinks(checkDownloadService.isSelected());
            }
        });
        setLayout(new BorderLayout());
        add(checkDownloadService, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
