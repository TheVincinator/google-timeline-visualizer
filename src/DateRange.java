import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DateRange extends JPanel {

    JTextField fromTextField;

    private boolean allowPlaceholderText = false;

    JTextField toTextField;

    boolean buttonClicked = false;

    String fromOriginalText;

    String toOriginalText;

    public DateRange() {

    setOpaque(false);
    JLabel fromLabel = new JLabel("From: ");
    JLabel toLabel = new JLabel("To: ");

    fromTextField = new JTextField(10);
    toTextField = new JTextField(10);
    fromTextField.setText("Enter date here");
    toTextField.setText("Enter date here");

    setNoSpaceFilter(fromTextField);
    setNoSpaceFilter(toTextField);

    Color placeholderColor = Color.GRAY;
    Color inputColor = UIManager.getColor("TextField.foreground");

    fromTextField.setForeground(placeholderColor);
    toTextField.setForeground(placeholderColor);

    addFocusListener(fromTextField, placeholderColor, inputColor);
    addFocusListener(toTextField, placeholderColor, inputColor);

    add(fromLabel);
    add(fromTextField);
    add(toLabel);
    add(toTextField);
    }

    private void addFocusListener(JTextField textField, Color placeholderColor, Color inputColor) {
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
//                if (textField.getForeground() == Color.RED) {
//                    String currentText = originalText;
//                    while (currentText.equals(originalText)) {
//                        currentText = textField.getText();
//                    }
//                    textField.setForeground(inputColor);
//                }
                if (textField.getForeground() == Color.RED) {
                    textField.setForeground(inputColor);
                }
                if (textField.getText().equals("Enter date here")) {
                    textField.setText("");
                    textField.setForeground(inputColor);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (buttonClicked && textField == fromTextField && textField.getText().equals(fromOriginalText)) {
                    textField.setForeground(Color.RED);
                }
                if (buttonClicked && textField == toTextField && textField.getText().equals(toOriginalText)) {
                    textField.setForeground(Color.RED);
                }
                if (textField.getText().isEmpty()) {
                    allowPlaceholderText = true;
                    textField.setForeground(placeholderColor);
                    textField.setText("Enter date here");
                    allowPlaceholderText = false;
                }
            }
        });
    }

    private void setNoSpaceFilter(JTextField textField) {
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (allowPlaceholderText || (string != null && !string.contains(" "))) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (allowPlaceholderText || (text != null && !text.contains(" "))) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    public void setInvalidTextColor(JTextField textField) {
        textField.setForeground(Color.RED);
    }

    public void setOriginalText() {
        fromOriginalText = fromTextField.getText();
        toOriginalText = toTextField.getText();
    }

    public void isButtonClicked() {
        buttonClicked = true;
    }
}
