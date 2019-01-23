import org.omg.CORBA.INTERNAL;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;


public class GUI extends JFrame  {

        Container content;

        public GUI() {
            super("Servers testing");

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Font font = new Font("Verdana", Font.PLAIN, 12);

            String[] items = {
                    "Single thread for every client",
                    "Thread read, singleThreadPoolExecutor write, ThreadPool",
                    "Selector read/write, ThreadPool"
            };

            content = getContentPane();
            content.setLayout(null);
//                getContentPane().setBackground(Color.magenta);

            JLabel label = new JLabel("Architecture type:");
            label.setBounds(20, 20, 140, 30);
            content.add(label);

            JComboBox comboBox = new JComboBox(items);
            comboBox.setFont(font);
            comboBox.setBounds(160, 20, 230, 30);
            content.add(comboBox);

            JLabel label2 = new JLabel("Requests number:");
            label2.setBounds(20, 70, 140, 30);
            content.add(label2);

            JTextField textField = new JTextField();
            textField.setBounds(160, 70, 50,30);
            content.add(textField);


            JLabel label3 = new JLabel("Variable parameter:");
            label3.setBounds(20, 140, 140, 20);
            content.add(label3);

            ButtonGroup variables = new ButtonGroup();
            JRadioButton choice1 = new JRadioButton("Number of elements");
            JRadioButton choice2 = new JRadioButton("Number of clients");
            JRadioButton choice3 = new JRadioButton("Messages frequency");

            choice1.setBounds(190,120,200,20);
            choice2.setBounds(190,140,200,20);
            choice3.setBounds(190,160,200,20);

            content.add(choice1);
            content.add(choice2);
            content.add(choice3);
            variables.add(choice1);
            variables.add(choice2);
            variables.add(choice3);

            JLabel arr = new JLabel("Number of elements:");
            arr.setBounds(8, 210, 150, 20);
            content.add(arr);

            JLabel clients = new JLabel("Number of clients:");
            clients.setBounds(8, 260, 150, 20);
            content.add(clients);

            JLabel delta = new JLabel("Messages frequency:");
            delta.setBounds(8, 310, 150, 20);
            content.add(delta);

            JLabel minMaxStep = new JLabel("min              max              step");
            Rectangle rec1 = new Rectangle(160, 200, 230, 20);
            minMaxStep.setBounds(rec1);
            content.add(minMaxStep);


            JLabel value1 = new JLabel("value:");
            Rectangle rec2 = new Rectangle(160, 250, 230, 20);
            value1.setBounds(rec2);
            content.add(value1);

            JLabel value2 = new JLabel("value:");
            Rectangle rec3 = new Rectangle(160, 300, 230, 20);
            value2.setBounds(rec3);
            content.add(value2);

            JTextField field1 = new JTextField();
            field1.setBounds(160, 220, 70,20);
            content.add(field1);
            JTextField field2 = new JTextField();
            field2.setBounds(241, 220, 70,20);
            content.add(field2);
            JTextField field3 = new JTextField();
            field3.setBounds(320, 220, 70,20);
            content.add(field3);

            JTextField field4 = new JTextField();
            field4.setBounds(320, 250, 70,20);
            content.add(field4);

            JTextField field5 = new JTextField();
            field5.setBounds(320, 300, 70,20);
            content.add(field5);

            choice1.addActionListener(actionEvent -> {
                minMaxStep.setBounds(rec1);
                value1.setBounds(rec2);
                value2.setBounds(rec3);
                field1.setBounds(160, 220, 70,20);
                field1.setText("");
                field2.setBounds(241, 220, 70,20);
                field2.setText("");
                field3.setBounds(320, 220, 70,20);
                field3.setText("");
                field4.setBounds(320, 250, 70,20);
                field4.setText("");
                field5.setBounds(320, 300, 70,20);
                field5.setText("");
//                content.repaint();
            });
            choice2.addActionListener(actionEvent -> {
                value1.setBounds(rec1);
                minMaxStep.setBounds(rec2);
                value2.setBounds(rec3);

                field1.setBounds(320, 200, 70,20);
                field1.setText("");
                field2.setBounds(160, 270, 70,20);
                field2.setText("");
                field3.setBounds(241, 270, 70,20);
                field3.setText("");
                field4.setBounds(320, 270, 70,20);
                field4.setText("");
                field5.setBounds(320, 300, 70,20);
                field5.setText("");

//                content.repaint();
            });
            choice3.addActionListener(actionEvent -> {
                value1.setBounds(rec1);
                value2.setBounds(rec2);
                minMaxStep.setBounds(rec3);

                field1.setBounds(320, 200, 70,20);
                field1.setText("");

                field2.setBounds(320, 250, 70,20);
                field2.setText("");

                field3.setBounds(160, 320, 70,20);
                field3.setText("");
                field4.setBounds(241, 320, 70,20);
                field4.setText("");
                field5.setBounds(320, 320, 70,20);
                field5.setText("");
//                content.repaint();
            });

            JLabel ip = new JLabel("Address:");
            ip.setBounds(20, 350, 80, 20);
            content.add(ip);

            JTextField ipAddr = new JTextField();
            ipAddr.setBounds(105, 350, 150, 20);
            content.add(ipAddr);

            JLabel port = new JLabel("Port:");
            port.setBounds(20, 375, 80, 20);
            content.add(port);

            JTextField portValue = new JTextField();
            portValue.setBounds(105, 375, 100, 20);
            content.add(portValue);



            JButton startButton = new JButton("Run");
            startButton.setFont(new Font("Verdana", Font.PLAIN, 16));
            startButton.setBounds(280, 360, 80, 30);
            content.add(startButton);

            Set<DrawCanvas> graphics = new HashSet<>();

            JLabel sortTime = new JLabel("Sorting time");
            sortTime.setBounds(550, 5, 100, 20);
            content.add(sortTime);

            JLabel readToWrite = new JLabel("Server read/write time");
            readToWrite.setBounds(520, 225, 200, 20);
            content.add(readToWrite);

            JLabel timeClient = new JLabel("Server read/write time");
            timeClient.setBounds(520, 445, 200, 20);
            content.add(timeClient);

            startButton.addActionListener(actionEvent -> {
                int architectureType = comboBox.getSelectedIndex();
                int queryPerClient = Integer.parseInt(textField.getText());
                boolean varNumElem = choice1.isSelected();
                boolean varCliNum = choice2.isSelected();
                boolean varDelta = choice3.isSelected();
                int numElem = Integer.parseInt(field1.getText());
                int cliNum = Integer.parseInt(varNumElem ? field4.getText() : field2.getText());
                int deltaTime = Integer.parseInt(varDelta ? field3.getText() : field5.getText());
                int variableParameter = varNumElem ? numElem : (varCliNum ? cliNum : deltaTime);
                int variableStep = Integer.parseInt(varNumElem ? field3.getText() : (varCliNum ? field4.getText() : field5.getText()));
                int variableLimit = Integer.parseInt(varNumElem ? field2.getText() : (varCliNum ? field3.getText() : field4.getText()));
                int[] valX = new int[(variableLimit - variableParameter) / variableStep + 1];

                int[] valSort = new int[(variableLimit - variableParameter) / variableStep + 1];
                int maxSort = -1;

                int[] valRead = new int[(variableLimit - variableParameter) / variableStep + 1];
                int maxRead = -1;

                int[] valClient = new int[(variableLimit - variableParameter) / variableStep + 1];
                int maxClient = -1;
                int counter = 0;

                for (int i = variableParameter; i <= variableLimit; i += variableStep) {
                    valX[counter] = i;
                    if (varNumElem) numElem = i;
                    if (varCliNum) cliNum = i;
                    if (varDelta) deltaTime = i;
                    int[] statistics = new int[3];
                    ClientManager cli = new ClientManager();
                    cli.run(ipAddr.getText(), Short.parseShort(portValue.getText()), architectureType, queryPerClient,
                            numElem, cliNum, deltaTime, statistics);
                    if (maxSort == -1)
                        maxSort = statistics[0];
                    else
                        maxSort = maxSort < statistics[0] ? statistics[0] : maxSort;
                    valSort[counter] = statistics[0];

                    if (maxRead == -1)
                        maxRead = statistics[1];
                    else
                        maxRead = maxRead < statistics[1] ? statistics[1] : maxRead;
                    valRead[counter] = statistics[1];

                    if (maxClient == -1)
                        maxClient = statistics[2];
                    else
                        maxClient = maxClient < statistics[2] ? statistics[2] : maxClient;
                    valClient[counter] = statistics[2];

                    counter++;
                }

                DrawCanvas canvas1 = new DrawCanvas(variableLimit, maxSort, valX, valSort);
                canvas1.setPreferredSize(new Dimension(400, 200));
                canvas1.setBounds(400, 10, 400, 201);
                content.add(canvas1);

                DrawCanvas canvas2 = new DrawCanvas(variableLimit, maxRead, valX, valRead);
                canvas2.setPreferredSize(new Dimension(400, 200));
                canvas2.setBounds(400, 230, 400, 201);
                content.add(canvas2);

                DrawCanvas canvas3 = new DrawCanvas(variableLimit, maxClient, valX, valClient);
                canvas3.setPreferredSize(new Dimension(400, 200));
                canvas3.setBounds(400, 450, 400, 201);
                content.add(canvas3);

                if (!graphics.isEmpty()) {
                    for (DrawCanvas canv : graphics) {
                        content.remove(canv);
                    }
                }
                graphics.add(canvas1);
                graphics.add(canvas2);
                graphics.add(canvas3);

                content.repaint();

            });

            setPreferredSize(new Dimension(800, 700));
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
            setResizable(false);
        }
    private class DrawCanvas extends JPanel {
        int maxX;
        int maxY;
        int[] xVals;
        int[] yVals;
        DrawCanvas(int maxX, int maxY, int[] xVals, int[] yVals) {
            this.maxX = maxX;
            this.maxY = maxY;
            this.xVals = xVals;
            this.yVals = yVals;
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
//            setBackground(Color.BLACK);
            g.setColor(Color.BLACK);
            g.drawLine(0, 200, 400, 200);
            g.drawLine(0, 0, 0, 200);
            g.setFont(new Font("Verdana", Font.PLAIN, 12));
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 50; i < 400; i += 50) g.drawLine(i, 0, i, 200);
            for (int i = 50; i < 200; i += 50) g.drawLine(0, i, 400, i);
            g.setColor(Color.DARK_GRAY);
            for (int i = 50; i < 400; i += 50) g.drawString("" + i * maxX / 400, i, 200);
            for (int i = 50; i < 200; i += 50) g.drawString("" + (maxY - i * maxY / 200), 0, i);
            g.setColor(Color.RED);
            if (maxX == 0) maxX = 1;
            if (maxY == 0) maxY = 1;
            for (int i = 0; i < xVals.length - 1; i++) {
                g.drawLine(xVals[i] * 400 / maxX, 200 - yVals[i] * 200 / maxY,
                          xVals[i + 1] * 400 / maxX, 200 - yVals[i + 1] * 200 / maxY);
            }
//            g.drawLine(0, 200, 100, 100);
//            g.drawLine(100, 100, 200, 50);
//            g.drawLine(200, 50, 300, 70);
//            g.drawLine(300, 70, 400, 0);


//            g.setColor(Color.DARK_GRAY);
//            g.drawString();

//            g.drawOval(150, 180, 10, 10);
//            g.drawRect(200, 210, 20, 30);
//            g.setColor(Color.RED);
//            g.fillOval(300, 310, 30, 50);
//            g.fillRect(400, 350, 60, 50);
//            g.setColor(Color.WHITE);
//            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
//            g.drawString("Testing custom drawing ...", 10, 20);
        }
    }

        public static void main(String[] args) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                JFrame.setDefaultLookAndFeelDecorated(true);
                new GUI();
            });
        }
}
