package cz.vity.freerapid.plugins.services.multiupload;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author ntoskrnl
 */
public class MultiUploadSettingsPanel extends JPanel {
    private MultiUploadSettingsConfig config;

    private String[] serviceStrings;
    private JList serviceList;

    private final JLabel serviceLabel = new JLabel("Services in order of preference:");
    private final JScrollPane scrollPane = new JScrollPane();
    private final JButton moveUp = new JButton("Move up");
    private final JButton moveDown = new JButton("Move down");
    private final JCheckBox checkDownloadService = new JCheckBox("Check for file errors on download server");

    public MultiUploadSettingsPanel(MultiUploadServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        serviceStrings = config.getServices();
        serviceList = new JList(serviceStrings);
        scrollPane.setViewportView(serviceList);
        checkDownloadService.setSelected(config.getCheckDownloadService());

        moveUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveUpActionPerformed(e);
            }
        });

        moveDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveDownActionPerformed(e);
            }
        });

        checkDownloadService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setCheckDownloadService(checkDownloadService.isSelected());
            }
        });

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup()
                                .addComponent(serviceLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 105, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup()
                                        .addComponent(moveUp, GroupLayout.PREFERRED_SIZE, 105, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(moveDown, GroupLayout.PREFERRED_SIZE, 105, GroupLayout.PREFERRED_SIZE)))
                                .addComponent(checkDownloadService, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap()
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(serviceLabel, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup()
                                .addComponent(moveUp, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(moveDown, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(checkDownloadService, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap()
        );
    }

    private void moveUpActionPerformed(ActionEvent e) {
        final int selected = serviceList.getSelectedIndex();
        if ((selected < 0) || ((selected - 1) < 0))
            return;
        final String temp = serviceStrings[selected - 1];
        serviceStrings[selected - 1] = serviceStrings[selected];
        serviceStrings[selected] = temp;
        serviceList.setSelectedIndex(selected - 1);
    }

    private void moveDownActionPerformed(ActionEvent e) {
        final int selected = serviceList.getSelectedIndex();
        if ((selected < 0) || ((selected + 1) > (serviceStrings.length - 1)))
            return;
        final String temp = serviceStrings[selected + 1];
        serviceStrings[selected + 1] = serviceStrings[selected];
        serviceStrings[selected] = temp;
        serviceList.setSelectedIndex(selected + 1);
    }

}