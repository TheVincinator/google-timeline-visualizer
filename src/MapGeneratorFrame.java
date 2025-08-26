import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class MapGeneratorFrame extends JFrame {

    MapGeneratorFrame() {
        setTitle("Create a map!");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(500, 500);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(240, 248, 255));

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JLabel startLabel = new JLabel("🌎 Google Timeline Visualizer", SwingConstants.CENTER);
        startLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        startLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        startLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0)); // Top padding
        add(startLabel);

        // Vertical spacing
        add(Box.createRigidArea(new Dimension(0, 100)));

        // File upload button (custom)
        FileUploadButton fileUploadButton = new FileUploadButton();
        fileUploadButton.setMaximumSize(new Dimension(400, 40));
        fileUploadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(fileUploadButton);

        // More vertical spacing
        //add(Box.createRigidArea(new Dimension(0, 100)));

        // Date range text field
        JLabel dateLabel = new JLabel("Enter date range (e.g., 2019-01-01 to 2019-03-31):", SwingConstants.CENTER);
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dateLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        DateRange dateRange = new DateRange();
        dateRange.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(dateLabel);
        add(dateRange);

        // More vertical spacing
        //add(Box.createRigidArea(new Dimension(0, 100)));

        // Rename File
        JCheckBox renameFileCheckbox = new JCheckBox("Create custom file name?");
        renameFileCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        renameFileCheckbox.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(renameFileCheckbox);

        // CSV Checkbox
        JCheckBox csvExportCheckbox = new JCheckBox("Export .csv file with coordinates");
        csvExportCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        csvExportCheckbox.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(csvExportCheckbox);

        // Create map button
        JButton startButton = new JButton("Create map!");
        startButton.setPreferredSize(new Dimension(150, 50));
        startButton.setMaximumSize(new Dimension(200, 50)); // max width
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(startButton);

        startButton.addActionListener(e -> {
            File file = fileUploadButton.getSelectedFile();

            String fromDateText = dateRange.fromTextField.getText();
            String toDateText = dateRange.toTextField.getText();

            if (file == null) {
                JOptionPane.showMessageDialog(this, "Please upload a file first.");
                return;
            } else if (fromDateText.equals("Enter date here") || toDateText.equals("Enter date here")) {
                JOptionPane.showMessageDialog(this, "Date(s) cannot be empty!");
                return;
            } else if (!(isValidDate(fromDateText) && isValidDate(toDateText))) {
                JOptionPane.showMessageDialog(this, "Incorrect date formats!");
                if (!(isValidDate(fromDateText))) {
                    dateRange.setInvalidTextColor(dateRange.fromTextField);
                }
                if (!(isValidDate(toDateText))) {
                    dateRange.setInvalidTextColor(dateRange.toTextField);
                }
                dateRange.isButtonClicked();
                dateRange.setOriginalText();
                return;
            }

            String fileNameInput = "";
            if (renameFileCheckbox.isSelected()) {
                while (fileNameInput.isEmpty()) {
                    fileNameInput = JOptionPane.showInputDialog(null, "Name the file:", "Name your map!", JOptionPane.QUESTION_MESSAGE);

                    if (fileNameInput.isEmpty()) {
                        return;
                    } else if (fileNameInput.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(null, "The field cannot be empty.");
                        fileNameInput = "";
                    }
                }
            }

            boolean checkboxSelected = csvExportCheckbox.isSelected();
            runPythonScriptWithProgressDialog(file, fromDateText, toDateText, checkboxSelected, fileNameInput);
        });

//        ImageIcon image = new ImageIcon("logo.png");
//        setIconImage(image.getImage());
    }

    private static boolean isValidDate(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                .withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalDate.parse(dateStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public void runPythonScriptWithProgressDialog(File file, String fromDateText, String toDateText, boolean checkboxSelected, String fileNameInput) {
        JDialog progressDialog = new JDialog((Frame) null, "Please wait", true);
        JLabel label = new JLabel("Starting map generation...", SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        progressDialog.add(label);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setResizable(false);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String pythonScriptPath = "app.py";
                String filePath = file.getAbsolutePath();

                List<String> command = new ArrayList<>();
                command.add("python3");
                command.add(pythonScriptPath);
                command.add(filePath);
                command.add(fromDateText);
                command.add(toDateText);
                command.add(String.valueOf(checkboxSelected));
                command.add(fileNameInput);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Read output from Python and publish to SwingWorker
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        publish(line); // send to process() method
                    }
                }

                process.waitFor();
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                // Update progress label with the latest output from Python
                label.setText("<html>" + String.join("<br>", chunks) + "</html>");
            }

            @Override
            protected void done() {
                progressDialog.dispose();

                try {
                    // Determine HTML file path
                    File outputDir = new File("maps");
                    File htmlFile;
                    String safeName = fileNameInput.isEmpty() ? "filtered_map" : fileNameInput;
                    htmlFile = new File(outputDir, safeName + ".html");

                    if (htmlFile.exists()) {
                        Desktop.getDesktop().browse(htmlFile.toURI());
                    } else {
                        JOptionPane.showMessageDialog(null, "Map file not found: " + htmlFile.getAbsolutePath());
                    }

                    // Optionally show CSV filename if exported
                    if (checkboxSelected) {
                        File[] csvFiles = outputDir.listFiles((dir, name) -> name.startsWith(safeName) && name.endsWith(".csv"));
                        if (csvFiles != null && csvFiles.length > 0) {
                            File latestCsv = csvFiles[csvFiles.length - 1]; // last one by array order
                            JOptionPane.showMessageDialog(null, "CSV exported: " + latestCsv.getName());
                        }
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "An error occurred while opening the map file.");
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MapGeneratorFrame program = new MapGeneratorFrame();
            program.setVisible(true);
        });
    }
}
