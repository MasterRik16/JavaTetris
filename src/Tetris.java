import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Classic Tetris in a single file, using Swing.
 *
 * Compile: javac Tetris.java
 * Run:     java Tetris
 *
 * Controls:
 *   Left / Right  move
 *   Down          soft drop
 *   Up or X       rotate clockwise
 *   Z             rotate counter-clockwise
 *   Space         hard drop
 *   P             pause
 *   R             restart
 */
public class Tetris extends JPanel implements ActionListener, KeyListener {

    // ---- Board geometry ----
    static final int COLS = 10, ROWS = 20, CELL = 30, SIDE = 170;
    static final int W = COLS * CELL + SIDE, H = ROWS * CELL;

    // ---- Piece definitions (I, O, T, S, Z, J, L) ----
    static final int[][][] SHAPES = {
        { {0,0,0,0}, {1,1,1,1}, {0,0,0,0}, {0,0,0,0} },   // I
        { {1,1}, {1,1} },                                  // O
        { {0,1,0}, {1,1,1}, {0,0,0} },                     // T
        { {0,1,1}, {1,1,0}, {0,0,0} },                     // S
        { {1,1,0}, {0,1,1}, {0,0,0} },                     // Z
        { {1,0,0}, {1,1,1}, {0,0,0} },                     // J
        { {0,0,1}, {1,1,1}, {0,0,0} }                      // L
    };
    static final Color[] COLORS = {
        new Color(0x00C8E6), new Color(0xF0D000), new Color(0xA000F0),
        new Color(0x00D000), new Color(0xF00000), new Color(0x2040F0),
        new Color(0xF0A000)
    };

    static class Piece {
        int type, x, y;
        int[][] m;

        Piece(int type) {
            this.type = type;
            this.m = copy(SHAPES[type]);
            this.x = COLS / 2 - m.length / 2;
            this.y = (type == 0) ? -1 : 0;   // the I piece's filled row is row 1
        }
    }

    static int[][] copy(int[][] a) {
        int[][] r = new int[a.length][];
        for (int i = 0; i < a.length; i++) r[i] = a[i].clone();
        return r;
    }

    static int[][] rotateCW(int[][] m) {
        int n = m.length;
        int[][] r = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                r[i][j] = m[n - 1 - j][i];
        return r;
    }

    static int[][] rotateCCW(int[][] m) {
        int n = m.length;
        int[][] r = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                r[i][j] = m[j][n - 1 - i];
        return r;
    }

    // ---- Game state ----
    private final int[][] board = new int[ROWS][COLS];   // 0 = empty, else type + 1
    private final Deque<Integer> bag = new ArrayDeque<>();
    private Piece cur;
    private int nextType;
    private int score, lines, level;
    private boolean paused, gameOver;
    private final Timer timer;

    public Tetris() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(18, 18, 24));
        setFocusable(true);
        addKeyListener(this);
        timer = new Timer(800, this);
        newGame();
    }

    private void newGame() {
        for (int[] row : board) java.util.Arrays.fill(row, 0);
        bag.clear();
        score = 0; lines = 0; level = 1;
        paused = false; gameOver = false;
        nextType = pullFromBag();
        spawn();
        timer.setDelay(delayForLevel());
        timer.restart();
        repaint();
    }

    private int delayForLevel() {
        return Math.max(80, 800 - (level - 1) * 70);
    }

    // 7-bag randomizer: every piece appears once per bag of 7
    private int pullFromBag() {
        if (bag.isEmpty()) {
            List<Integer> l = new ArrayList<>();
            for (int i = 0; i < 7; i++) l.add(i);
            Collections.shuffle(l);
            bag.addAll(l);
        }
        return bag.poll();
    }

    private void spawn() {
        cur = new Piece(nextType);
        nextType = pullFromBag();
        if (collides(cur.m, cur.x, cur.y)) {
            gameOver = true;
            timer.stop();
        }
    }

    private boolean collides(int[][] m, int px, int py) {
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < m[r].length; c++) {
                if (m[r][c] == 0) continue;
                int bx = px + c, by = py + r;
                if (bx < 0 || bx >= COLS || by >= ROWS) return true;
                if (by >= 0 && board[by][bx] != 0) return true;
            }
        }
        return false;
    }

    private boolean movePiece(int dx, int dy) {
        if (collides(cur.m, cur.x + dx, cur.y + dy)) return false;
        cur.x += dx;
        cur.y += dy;
        return true;
    }

    private void rotate(boolean clockwise) {
        if (cur.type == 1) return;   // O piece doesn't rotate
        int[][] rotated = clockwise ? rotateCW(cur.m) : rotateCCW(cur.m);
        // Simple wall kicks: try shifting sideways / up if the rotation doesn't fit
        int[][] kicks = { {0,0}, {-1,0}, {1,0}, {-2,0}, {2,0}, {0,-1} };
        for (int[] k : kicks) {
            if (!collides(rotated, cur.x + k[0], cur.y + k[1])) {
                cur.m = rotated;
                cur.x += k[0];
                cur.y += k[1];
                return;
            }
        }
    }

    private void hardDrop() {
        int dropped = 0;
        while (movePiece(0, 1)) dropped++;
        score += dropped * 2;
        lockPiece();
    }

    private void lockPiece() {
        for (int r = 0; r < cur.m.length; r++) {
            for (int c = 0; c < cur.m[r].length; c++) {
                if (cur.m[r][c] == 0) continue;
                int bx = cur.x + c, by = cur.y + r;
                if (by < 0) {            // locked above the visible board
                    gameOver = true;
                    timer.stop();
                    repaint();
                    return;
                }
                board[by][bx] = cur.type + 1;
            }
        }
        clearLines();
        spawn();
    }

    private void clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] == 0) { full = false; break; }
            }
            if (full) {
                for (int rr = r; rr > 0; rr--) board[rr] = board[rr - 1].clone();
                board[0] = new int[COLS];
                cleared++;
                r++;   // re-check this row, since rows shifted down
            }
        }
        if (cleared > 0) {
            int[] points = { 0, 100, 300, 500, 800 };
            score += points[cleared] * level;
            lines += cleared;
            level = lines / 10 + 1;
            timer.setDelay(delayForLevel());
        }
    }

    // Gravity tick
    @Override
    public void actionPerformed(ActionEvent e) {
        if (paused || gameOver) return;
        if (!movePiece(0, 1)) lockPiece();
        repaint();
    }

    // ---- Input ----
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_R) { newGame(); return; }
        if (gameOver) return;
        if (k == KeyEvent.VK_P) {
            paused = !paused;
            repaint();
            return;
        }
        if (paused) return;

        switch (k) {
            case KeyEvent.VK_LEFT:  movePiece(-1, 0); break;
            case KeyEvent.VK_RIGHT: movePiece(1, 0);  break;
            case KeyEvent.VK_DOWN:
                if (movePiece(0, 1)) score += 1; else lockPiece();
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_X:     rotate(true);  break;
            case KeyEvent.VK_Z:     rotate(false); break;
            case KeyEvent.VK_SPACE: hardDrop();    break;
            default: return;
        }
        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // ---- Rendering ----
    private void drawCell(Graphics2D g, int px, int py, Color col, int size) {
        g.setColor(col);
        g.fillRect(px, py, size, size);
        g.setColor(col.brighter());
        g.fillRect(px, py, size, 3);
        g.fillRect(px, py, 3, size);
        g.setColor(col.darker().darker());
        g.fillRect(px, py + size - 3, size, 3);
        g.fillRect(px + size - 3, py, 3, size);
    }

    private int ghostY() {
        int gy = cur.y;
        while (!collides(cur.m, cur.x, gy + 1)) gy++;
        return gy;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Playfield background + grid
        g.setColor(new Color(10, 10, 16));
        g.fillRect(0, 0, COLS * CELL, ROWS * CELL);
        g.setColor(new Color(30, 30, 42));
        for (int c = 0; c <= COLS; c++) g.drawLine(c * CELL, 0, c * CELL, ROWS * CELL);
        for (int r = 0; r <= ROWS; r++) g.drawLine(0, r * CELL, COLS * CELL, r * CELL);

        // Locked blocks
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (board[r][c] != 0)
                    drawCell(g, c * CELL, r * CELL, COLORS[board[r][c] - 1], CELL);

        if (!gameOver) {
            // Ghost piece
            int gy = ghostY();
            g.setColor(new Color(255, 255, 255, 50));
            for (int r = 0; r < cur.m.length; r++)
                for (int c = 0; c < cur.m[r].length; c++)
                    if (cur.m[r][c] != 0 && gy + r >= 0)
                        g.fillRect((cur.x + c) * CELL + 1, (gy + r) * CELL + 1, CELL - 2, CELL - 2);

            // Current piece
            for (int r = 0; r < cur.m.length; r++)
                for (int c = 0; c < cur.m[r].length; c++)
                    if (cur.m[r][c] != 0 && cur.y + r >= 0)
                        drawCell(g, (cur.x + c) * CELL, (cur.y + r) * CELL, COLORS[cur.type], CELL);
        }

        // Side panel
        int sx = COLS * CELL + 20;
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("NEXT", sx, 30);

        int[][] nm = SHAPES[nextType];
        int mini = 22;
        for (int r = 0; r < nm.length; r++)
            for (int c = 0; c < nm[r].length; c++)
                if (nm[r][c] != 0)
                    drawCell(g, sx + c * mini, 45 + r * mini, COLORS[nextType], mini);

        g.setColor(Color.WHITE);
        g.drawString("SCORE", sx, 180);
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.drawString(String.valueOf(score), sx, 202);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("LINES", sx, 240);
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.drawString(String.valueOf(lines), sx, 262);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("LEVEL", sx, 300);
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.drawString(String.valueOf(level), sx, 322);

        g.setColor(new Color(160, 160, 175));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String[] help = {
            "\u2190 \u2192   move", "\u2193      soft drop", "\u2191 / X  rotate",
            "Z      rotate back", "Space  hard drop", "P      pause", "R      restart"
        };
        for (int i = 0; i < help.length; i++) g.drawString(help[i], sx, 370 + i * 18);

        // Overlays
        if (paused) overlay(g, "PAUSED", "Press P to resume");
        if (gameOver) overlay(g, "GAME OVER", "Press R to restart");
    }

    private void overlay(Graphics2D g, String title, String sub) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, COLS * CELL, ROWS * CELL);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (COLS * CELL - fm.stringWidth(title)) / 2, ROWS * CELL / 2 - 10);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        fm = g.getFontMetrics();
        g.drawString(sub, (COLS * CELL - fm.stringWidth(sub)) / 2, ROWS * CELL / 2 + 20);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tetris");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new Tetris());
            f.pack();
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
