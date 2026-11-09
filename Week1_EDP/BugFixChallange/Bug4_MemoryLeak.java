import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Bug4_MemoryLeak extends JFrame {

    private List<JButton> createdButtons = new ArrayList<>();
    private JPanel buttonPanel;

    public Bug4_MemoryLeak() {
        setTitle("Bug 4 - Fixed (No Memory Leak)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        buttonPanel = new JPanel(new FlowLayout());

        JButton addButton = new JButton("Add Button");
        addButton.addActionListener(e -> {
            JButton newButton = new JButton("Button " + createdButtons.size());
            createdButtons.add(newButton);
            buttonPanel.add(newButton);
            buttonPanel.revalidate();
            buttonPanel.repaint();
        });

        JButton removeButton = new JButton("Remove All");
        removeButton.addActionListener(e -> {
            buttonPanel.removeAll();
            createdButtons.clear();   // FIX: also drop the references so the buttons can be garbage collected
            buttonPanel.revalidate();
            buttonPanel.repaint();
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(addButton);
        controlPanel.add(removeButton);

        add(buttonPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        setSize(500, 300);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Bug4_MemoryLeak().setVisible(true);
        });
    }
}