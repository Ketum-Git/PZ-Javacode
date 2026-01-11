// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import fmod.SoundBuffer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class VoiceDebug extends JPanel {
    private static final int PREF_W = 400;
    private static final int PREF_H = 200;
    private static final int BORDER_GAP = 30;
    private static final Color LINE_CURRENT_COLOR = Color.blue;
    private static final Color LINE_LAST_COLOR = Color.red;
    private static final Color GRAPH_COLOR = Color.green;
    private static final Color GRAPH_POINT_COLOR = new Color(150, 50, 50, 180);
    private static final Stroke GRAPH_STROKE = new BasicStroke(3.0F);
    private static final int GRAPH_POINT_WIDTH = 12;
    private static final int Y_HATCH_CNT = 10;
    public List<Integer> scores;
    public int scoresMax;
    public String title;
    public int psize;
    public int last;
    public int current;
    private static VoiceDebug mainPanel;
    private static VoiceDebug mainPanel2;
    private static VoiceDebug mainPanel3;
    private static VoiceDebug mainPanel4;
    private static JFrame frame;

    public VoiceDebug(List<Integer> scores, String title) {
        this.scores = scores;
        this.title = title;
        this.psize = scores.size();
        this.last = 5;
        this.current = 8;
        this.scoresMax = 100;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double xScale = (this.getWidth() - 60.0) / (this.scores.size() - 1);
        double yScale = (this.getHeight() - 60.0) / (this.scoresMax - 1);
        int yShift = (int)((this.getHeight() - 60.0) / 2.0);
        int stepx = (int)(1.0 / xScale);
        if (stepx == 0) {
            stepx = 1;
        }

        List<Point> graphPoints = new ArrayList<>();

        for (int i = 0; i < this.scores.size(); i += stepx) {
            int x1 = (int)(i * xScale + 30.0);
            int y1 = (int)((this.scoresMax - this.scores.get(i)) * yScale + 30.0 - yShift);
            graphPoints.add(new Point(x1, y1));
        }

        g2.setColor(Color.black);
        g2.drawLine(30, this.getHeight() - 30, 30, 30);
        g2.drawLine(30, this.getHeight() - 30, this.getWidth() - 30, this.getHeight() - 30);

        for (int i = 0; i < 10; i++) {
            int x0 = 30;
            int x1 = 42;
            int y0 = this.getHeight() - ((i + 1) * (this.getHeight() - 60) / 10 + 30);
            g2.drawLine(30, y0, 42, y0);
        }

        Stroke oldStroke = g2.getStroke();
        g2.setColor(GRAPH_COLOR);
        g2.setStroke(GRAPH_STROKE);

        for (int i = 0; i < graphPoints.size() - 1; i++) {
            int x1 = graphPoints.get(i).x;
            int y1 = graphPoints.get(i).y;
            int x2 = graphPoints.get(i + 1).x;
            int y2 = graphPoints.get(i + 1).y;
            g2.drawLine(x1, y1, x2, y2);
        }

        double xScalePoints = (this.getWidth() - 60.0) / (this.psize - 1);
        g2.setColor(LINE_CURRENT_COLOR);
        int x_current = (int)(this.current * xScalePoints + 30.0);
        g2.drawLine(x_current, this.getHeight() - 30, x_current, 30);
        g2.drawString("Current", x_current, this.getHeight() - 30);
        g2.setColor(LINE_LAST_COLOR);
        int x_last = (int)(this.last * xScalePoints + 30.0);
        g2.drawLine(x_last, this.getHeight() - 30, x_last, 30);
        g2.drawString("Last", x_last, this.getHeight() - 30);
        g2.setColor(Color.black);
        g2.drawString(this.title, this.getWidth() / 2, 15);
        g2.drawString("Size: " + this.scores.size(), 30, 15);
        g2.drawString("Current/Write: " + this.current, 30, 30);
        g2.drawString("Last/Read: " + this.last, 30, 45);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 200);
    }

    public static void createAndShowGui() {
        List<Integer> playBufScores = new ArrayList<>();
        List<Integer> playBufScores100 = new ArrayList<>();
        List<Integer> FMODplayBufScores = new ArrayList<>();
        List<Integer> FMODplayBufScores100 = new ArrayList<>();
        mainPanel = new VoiceDebug(playBufScores, "SoundBuffer");
        mainPanel.scoresMax = 32000;
        mainPanel2 = new VoiceDebug(playBufScores100, "SoundBuffer - first 100 sample");
        mainPanel2.scoresMax = 32000;
        mainPanel3 = new VoiceDebug(FMODplayBufScores, "FMODSoundBuffer");
        mainPanel3.scoresMax = 32000;
        mainPanel4 = new VoiceDebug(FMODplayBufScores100, "FMODSoundBuffer - first 100 sample");
        mainPanel4.scoresMax = 32000;
        frame = new JFrame("DrawGraph");
        frame.setDefaultCloseOperation(3);
        frame.setLayout(new GridLayout(2, 2));
        frame.getContentPane().add(mainPanel);
        frame.getContentPane().add(mainPanel2);
        frame.getContentPane().add(mainPanel3);
        frame.getContentPane().add(mainPanel4);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    public static void updateGui(SoundBuffer playBuf, byte[] recBuf) {
        mainPanel.scores.clear();
        if (playBuf != null) {
            for (int i = 0; i < playBuf.buf().length; i++) {
                mainPanel.scores.add(Integer.valueOf(playBuf.buf()[i]));
            }

            mainPanel.current = playBuf.bufWrite;
            mainPanel.last = playBuf.bufRead;
            mainPanel.psize = playBuf.bufSize;
            mainPanel2.scores.clear();

            for (int i = 0; i < 100; i++) {
                mainPanel2.scores.add(Integer.valueOf(playBuf.buf()[i]));
            }
        }

        mainPanel3.scores.clear();
        mainPanel4.scores.clear();

        for (int i = 0; i < recBuf.length / 2; i += 2) {
            mainPanel4.scores.add(recBuf[i + 1] * 256 + recBuf[i]);
        }

        frame.repaint();
    }
}
