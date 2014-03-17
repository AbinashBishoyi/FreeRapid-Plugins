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

    private String[] serviceStrings;
    private JList serviceList;

    private final JLabel serviceLabel = new JLabel("Services in order of preference:");
    private final JScrollPane scrollPane = new JScrollPane();
    private final JButton moveUp = new JButton("Move up");
    private final JButton moveDown = new JButton("Move down");

    public MultiUploadSettingsPanel(MultiUploadServiceImpl service) throws Exception {
        super(new BorderLayout());
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        serviceStrings = config.getServices();
        serviceList = new JList(serviceStrings);
        scrollPane.setViewportView(serviceList);

        moveUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveUpActionPerformed(e);
            }
        });

        moveDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveDownActionPerformed(e);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(serviceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(moveUp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(moveDown, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                        .addContainerGap()));

        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(serviceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup()
                                .addComponent(moveUp, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(moveDown, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }

    private void moveUpActionPerformed(java.awt.event.ActionEvent e) {
        final int selected = serviceList.getSelectedIndex();
        if ((selected < 0) || ((selected - 1) < 0))
            return;
        final String temp = serviceStrings[selected - 1];
        serviceStrings[selected - 1] = serviceStrings[selected];
        serviceStrings[selected] = temp;
        serviceList.setSelectedIndex(selected - 1);
    }

    private void moveDownActionPerformed(java.awt.event.ActionEvent e) {
        final int selected = serviceList.getSelectedIndex();
        if ((selected < 0) || ((selected + 1) > (serviceStrings.length - 1)))
            return;
        final String temp = serviceStrings[selected + 1];
        serviceStrings[selected + 1] = serviceStrings[selected];
        serviceStrings[selected] = temp;
        serviceList.setSelectedIndex(selected + 1);
    }
}