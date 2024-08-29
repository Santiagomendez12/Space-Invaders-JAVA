import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class SpaceInvaders extends JPanel implements ActionListener, KeyListener {
    // board
    int tileSize = 32;
    int rows = 16;
    int columns = 16;
    int boardWidth = tileSize * columns;
    int boardHeight = tileSize * rows;

    // images
    Image shipImg;
    Image alienImg;
    Image alienCyanImg;
    Image alienMagentaImg;
    Image alienYellowImg;
    ArrayList<Image> alienImgArray;

    // game elements
    Block ship;
    ArrayList<Block> alienArray;
    ArrayList<Block> bulletArray;
    ArrayList<Block> alienBulletArray;

    // game stats
    int score = 0;
    int currentWave = 1;
    int currentLevel = 1;
    boolean gameOver = false;

    // ship properties
    int shipWidth = tileSize * 2;
    int shipHeight = tileSize;
    int shipX = (boardWidth - shipWidth) / 2;
    int shipY = boardHeight - shipHeight * 2;
    int shipVelocity = 5;

    // alien properties
    int alienWidth = tileSize * 3 / 2;  // Reducido de tileSize * 2
    int alienHeight = tileSize * 3 / 4;  // Reducido de tileSize
    int alienRows = 2;
    int alienColumns = 3;
    int alienCount = 0;
    int alienVelocityX = 1;
    double kamikazeProbability = 0.001;
    int kamikazeSpeed = 3;

    // bullet properties
    int bulletWidth = tileSize / 8;
    int bulletHeight = tileSize / 2;
    int bulletVelocityY = -10;

    // alien bullet properties
    int alienBulletSpeed = 3;
    double alienShootingProbability = 0.005;

    // movement flags
    boolean leftPressed = false;
    boolean rightPressed = false;

    Timer gameLoop;
    Random random = new Random();

    class Block {
        int x;
        int y;
        int width;
        int height;
        Image img;
        boolean alive = true;
        boolean used = false;
        
        Block(int x, int y, int width, int height, Image img) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.img = img;
        }
    }

    SpaceInvaders() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        shipImg = new ImageIcon(getClass().getResource("./ship.png")).getImage();
        alienImg = new ImageIcon(getClass().getResource("./alien.png")).getImage();
        alienCyanImg = new ImageIcon(getClass().getResource("./alien-cyan.png")).getImage();
        alienMagentaImg = new ImageIcon(getClass().getResource("./alien-magenta.png")).getImage();
        alienYellowImg = new ImageIcon(getClass().getResource("./alien-yellow.png")).getImage();

        alienImgArray = new ArrayList<Image>();
        alienImgArray.add(alienImg);
        alienImgArray.add(alienCyanImg);
        alienImgArray.add(alienMagentaImg);
        alienImgArray.add(alienYellowImg);

        ship = new Block(shipX, shipY, shipWidth, shipHeight, shipImg);
        alienArray = new ArrayList<Block>();
        bulletArray = new ArrayList<Block>();
        alienBulletArray = new ArrayList<Block>();

        gameLoop = new Timer(1000/60, this);
        createAliens();
        gameLoop.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // ship
        g.drawImage(ship.img, ship.x, ship.y, ship.width, ship.height, null);

        // aliens
        for (Block alien : alienArray) {
            if (alien.alive) {
                int alienY = 0;
                if (currentLevel >= 3 && alien.y > alienY + alienRows * (alienHeight + tileSize/4)) {
                    // Dibujar aliens en modo kamikaze con un color diferente
                    g.setColor(Color.RED);
                    g.fillRect(alien.x, alien.y, alien.width, alien.height);
                } else {
                    g.drawImage(alien.img, alien.x, alien.y, alien.width, alien.height, null);
                }
            }
        }

        // bullets
        g.setColor(Color.white);
        for (Block bullet : bulletArray) {
            if (!bullet.used) {
                g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
            }
        }

        // alien bullets
        g.setColor(Color.red);
        for (Block bullet : alienBulletArray) {
            g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
        }

        // score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Score: " + score, 10, 25);
        g.drawString("Level: " + currentLevel + " Wave: " + currentWave, boardWidth - 200, 25);

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("GAME OVER", boardWidth/2 - 100, boardHeight/2);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Press SPACE to restart", boardWidth/2 - 100, boardHeight/2 + 30);
        }
    }

    public void move() {
        // Move ship
        if (leftPressed && ship.x > 0) {
            ship.x -= shipVelocity;
        }
        if (rightPressed && ship.x + ship.width < boardWidth) {
            ship.x += shipVelocity;
        }

        // Move aliens
        for (Block alien : alienArray) {
            if (alien.alive) {
                if (currentLevel >= 3 && Math.random() < kamikazeProbability) {
                    // Iniciar ataque kamikaze
                    alien.y += kamikazeSpeed;
                } else {
                    alien.x += alienVelocityX;
                }

                if (alien.x + alien.width >= boardWidth || alien.x <= 0) {
                    alienVelocityX *= -1;
                    for (Block a : alienArray) {
                        a.y += alienHeight;
                    }
                }
                if (detectCollision(alien, ship)) {
                    gameOver = true;
                }

                if (alien.y + alien.height >= boardHeight) {
                    alien.alive = false;
                    alienCount--;
                }
            }
        }
        

        // Move bullets
        for (int i = bulletArray.size() - 1; i >= 0; i--) {
            Block bullet = bulletArray.get(i);
            bullet.y += bulletVelocityY;

            if (bullet.y < 0) {
                bulletArray.remove(i);
                continue;
            }

            for (Block alien : alienArray) {
                if (alien.alive && detectCollision(bullet, alien)) {
                    bullet.used = true;
                    alien.alive = false;
                    bulletArray.remove(i);
                    alienCount--;
                    score += 100;
                    break;
                }
            }
        }

        // Move alien bullets and check collisions
        for (int i = alienBulletArray.size() - 1; i >= 0; i--) {
            Block bullet = alienBulletArray.get(i);
            bullet.y += alienBulletSpeed;

            if (detectCollision(bullet, ship)) {
                gameOver = true;
                break;
            }

            if (bullet.y > boardHeight) {
                alienBulletArray.remove(i);
            }
        }

        // Aliens shoot
        if (currentLevel > 1) {
            for (Block alien : alienArray) {
                if (alien.alive && Math.random() < alienShootingProbability) {
                    Block bullet = new Block(
                        alien.x + alien.width / 2 - bulletWidth / 2,
                        alien.y + alien.height,
                        bulletWidth,
                        bulletHeight,
                        null
                    );
                    alienBulletArray.add(bullet);
                }
            }
        }

        // Next level
        if (alienCount == 0) {
            currentWave++;
            if (currentLevel == 1 && currentWave > 4) {
                currentLevel = 2;
                currentWave = 1;
                alienVelocityX = 2;
            } else if (currentLevel == 2 && currentWave > 6) {
                currentLevel = 3;
                currentWave = 1;
                alienVelocityX = 3;
            }
            
            if (currentWave == 1) {
                // Reiniciar configuración de aliens al inicio de cada nivel
                alienColumns = 3;
                alienRows = 2;
            } else {
                alienColumns = Math.min(alienColumns + 1, columns/2 - 2);
                alienRows = Math.min(alienRows + 1, rows - 6);
            }
            score += alienColumns * alienRows * 100;
            createAliens();
        }
    }	

    public void createAliens() {
        alienArray.clear();
        int startX = (boardWidth - (alienColumns * (alienWidth + tileSize/4))) / 2;
        for (int row = 0; row < alienRows; row++) {
            for (int col = 0; col < alienColumns; col++) {
                int x = startX + col * (alienWidth + tileSize/4);
                int y = row * (alienHeight + tileSize/4) + tileSize;
                Block alien = new Block(x, y, alienWidth, alienHeight, alienImgArray.get(random.nextInt(alienImgArray.size())));
                alienArray.add(alien);
            }
        }
        alienCount = alienArray.size();
    }

    public boolean detectCollision(Block a, Block b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            move();
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (key == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (key == KeyEvent.VK_SPACE) {
            if (!gameOver) {
                Block bullet = new Block(ship.x + ship.width/2 - bulletWidth/2, ship.y, bulletWidth, bulletHeight, null);
                bulletArray.add(bullet);
            } else {
                restartGame();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (key == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void restartGame() {
        ship.x = shipX;
        bulletArray.clear();
        alienBulletArray.clear();
        alienArray.clear();
        gameOver = false;
        score = 0;
        currentWave = 1;
        currentLevel = 1;
        alienColumns = 3;
        alienRows = 2;
        alienVelocityX = 1;
        createAliens();
        gameLoop.start();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Invaders");
        SpaceInvaders game = new SpaceInvaders();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}