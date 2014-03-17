package cz.vity.freerapid.plugins.services.multiupload;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author ntoskrnl
 */
public class MultiUploadSettingsPanel extends JPanel {
    private MultiUploadSettingsConfig config;

    private final JCheckBox checkDownloadService = new JCheckBox("Check for file errors on download server");

    public MultiUploadSettingsPanel(MultiUploadServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        checkDownloadService.setSelected(config.getCheckDownloadService());

        checkDownloadService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setCheckDownloadService(checkDownloadService.isSelected());
            }
        });

        setLayout(new BorderLayout());
        add(checkDownloadService, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}