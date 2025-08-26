import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class FileUploadButton extends JPanel {

    private final JLabel filePathLabel;

    private File selectedFile;

    public File getSelectedFile() {
        return selectedFile;
    }

    public FileUploadButton() {
        setLayout(new FlowLayout());
        setOpaque(false);

        JButton uploadButton = new JButton("Upload File");
        filePathLabel = new JLabel("No file selected");

        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));

            int result = fileChooser.showOpenDialog(FileUploadButton.this);

            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                filePathLabel.setText("Selected file: " + selectedFile.getName());
            } else {
                filePathLabel.setText("Upload cancelled");
            }
        });

        add(uploadButton);
        add(filePathLabel);
    }
}