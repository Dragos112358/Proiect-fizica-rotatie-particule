import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

class ParticleSimulation extends JFrame {
    public ArrayList<Point> getParticle1Trail() {
        return particle1Trail;
    }

    public ArrayList<Point> getParticle2Trail() {
        return particle2Trail;
    }
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1000;
    private static final int PARTICLE_SIZE = 20;
    private static final int TRAIL_LENGTH = 50;
    public static int ORBIT_RADIUS = 150;
    private static final double GRAVITY_CONSTANT = 0.01;
    public static double rotationSpeed = 0.01;
    public static double vtimp = 0.01;
    public static double mass1 = Math.sqrt(10);
    public static double mass2 = Math.sqrt(10);
    public static double zoomFactor = 1.0; // Initial zoom factor
    private double angle = 0.0;

    public static ArrayList<Point> particle1Trail = new ArrayList<>();
    public static ArrayList<Point> particle2Trail = new ArrayList<>();
    private ArrayList<Point> parabolicPath = new ArrayList<>();
    private static SimulationPanel simulationPanel;
    public static SimulationPanel getSimulationPanel() {
        return simulationPanel;
    }
    private Point centerOfMass;
    public ParticleSimulation() {
        setTitle("Particle Attraction Simulation");
        setSize(WIDTH, HEIGHT);
        centerOfMass = new Point(WIDTH / 2, HEIGHT / 2); // Set initial center of mass
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);

        // Use BorderLayout for the content pane
        setLayout(new BorderLayout());

        // Create a panel for the slider
        SliderPanel sliderPanel = new SliderPanel();
        add(sliderPanel, BorderLayout.NORTH); // Add the slider panel to the top

        simulationPanel = new SimulationPanel();
        add(simulationPanel, BorderLayout.CENTER);

        Timer timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateParticles();
                updateParabolicPath();
                simulationPanel.repaint(); // Repaint the simulation panel
            }
        });
        timer.start();
    }

    private void updateParticles() {
        // Calculate initial angles for both particles
        double angle1 = angle;
        double angle2 = angle + Math.PI;

        // Calculate initial positions based on masses and desired distances
        int particle1X = (int) (WIDTH / 2 + (ORBIT_RADIUS * mass2 / (mass1 + mass2)) * Math.cos(angle1));
        int particle1Y = (int) (HEIGHT / 2 + (ORBIT_RADIUS * mass2 / (mass1 + mass2)) * Math.sin(angle1));

        int particle2X = (int) (WIDTH / 2 + (ORBIT_RADIUS * mass1 / (mass1 + mass2)) * Math.cos(angle2));
        int particle2Y = (int) (HEIGHT / 2 + (ORBIT_RADIUS * mass1 / (mass1 + mass2)) * Math.sin(angle2));

        // Update trails
        particle1Trail.add(new Point(particle1X, particle1Y));
        particle2Trail.add(new Point(particle2X, particle2Y));

        if (particle1Trail.size() > TRAIL_LENGTH) {
            particle1Trail.remove(0);
        }
        if (particle2Trail.size() > TRAIL_LENGTH) {
            particle2Trail.remove(0);
        }

        // Calculate rotation speed based on distance and masses
        double massProduct = mass1 * mass2;
        double distance = particle1Trail.get(particle1Trail.size() - 1).distance(particle2Trail.get(particle2Trail.size() - 1));
        rotationSpeed = massProduct / (1 + distance);

        angle += rotationSpeed * vtimp;
        centerOfMass.x = (particle1Trail.get(particle1Trail.size() - 1).x + particle2Trail.get(particle2Trail.size() - 1).x) / 2;
        centerOfMass.y = (particle1Trail.get(particle1Trail.size() - 1).y + particle2Trail.get(particle2Trail.size() - 1).y) / 2;

        // Adjust the translation and scaling for the new center of mass
        simulationPanel.setZoomCenter(centerOfMass);
    }

    private void updateParabolicPath() {
        // Calculate parabolic path between particles
        parabolicPath.clear();
        int numPoints = 50; // Increase the number of points for a smoother curve
        for (int i = 0; i <= numPoints; i++) {
            double t = (double) i / numPoints;
            int x = (int) ((1 - t) * particle1Trail.get(particle1Trail.size() - 1).x +
                    t * particle2Trail.get(particle2Trail.size() - 1).x);
            int y = (int) ((1 - t) * particle1Trail.get(particle1Trail.size() - 1).y +
                    t * particle2Trail.get(particle2Trail.size() - 1).y - GRAVITY_CONSTANT * t * t * HEIGHT);
            parabolicPath.add(new Point(x, y));
        }
    }

    class SimulationPanel extends JPanel {
        private boolean followParticle = false;
        private Point particleToFollow;
        public void setFollowParticle(ArrayList<Point> particleTrail) {
            followParticle = true;
            particleToFollow = particleTrail.get(particleTrail.size() - 1);
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Adjust the scaling transformation based on the zoom factor
            Graphics2D g2d = (Graphics2D) g.create();
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            if (!particle1Trail.isEmpty() && !particle2Trail.isEmpty()) {
                try {
                    centerX = (particle1Trail.get(particle1Trail.size() - 1).x + particle2Trail.get(particle2Trail.size() - 1).x) / 2;
                    centerY = (particle1Trail.get(particle1Trail.size() - 1).y + particle2Trail.get(particle2Trail.size() - 1).y) / 2;

                } catch (ArrayIndexOutOfBoundsException e) {
                }
            }
            int translateX = 0;
            int translateY = 0;
            if (followParticle && particleToFollow != null) {
                translateX = getWidth() / 2;
                translateY = getHeight() / 2;
            }
            // Adjust the translation and scaling
            g2d.translate(centerX , centerY);
            g2d.scale(zoomFactor, zoomFactor);
            g2d.translate(-centerX, -centerY);

            drawBackground(g2d);

            // Draw color gradient background to the buffer for each particle
            if (!particle1Trail.isEmpty()) {
                drawRadialGradient(g2d, particle1Trail.get(particle1Trail.size() - 1), Color.RED, Color.BLUE,true);
            }
            if (!particle2Trail.isEmpty()) {
                drawRadialGradient(g2d, particle2Trail.get(particle2Trail.size() - 1), Color.BLUE, Color.BLUE,false);
            }

            // Draw particle trails to the buffer
            g2d.setColor(Color.WHITE);
            drawTrail(g2d, particle1Trail);
            drawTrail(g2d, particle2Trail);

            // Draw particles to the buffer
            if (!particle1Trail.isEmpty()) {
                g2d.fillOval(
                        particle1Trail.get(particle1Trail.size() - 1).x - PARTICLE_SIZE / 2,
                        particle1Trail.get(particle1Trail.size() - 1).y - PARTICLE_SIZE / 2,
                        PARTICLE_SIZE,
                        PARTICLE_SIZE
                );
            }
            if (!particle2Trail.isEmpty()) {
                g2d.fillOval(
                        particle2Trail.get(particle2Trail.size() - 1).x - PARTICLE_SIZE / 2,
                        particle2Trail.get(particle2Trail.size() - 1).y - PARTICLE_SIZE / 2,
                        PARTICLE_SIZE,
                        PARTICLE_SIZE
                );
            }

            // Draw magnetic field lines
            if (!particle1Trail.isEmpty() && !particle2Trail.isEmpty()) {
                drawMagneticFieldLines(g2d, particle1Trail.get(particle1Trail.size() - 1),
                        particle2Trail.get(particle2Trail.size() - 1));
            }
            if (!particle1Trail.isEmpty() && !particle2Trail.isEmpty()) {
                drawBlackSignPlus(g2d, particle1Trail.get(particle1Trail.size() - 1));
                drawBlackSignMinus(g2d, particle2Trail.get(particle2Trail.size() - 1));
            }
            // Draw parabolic path
            drawParabolicPath(g2d, parabolicPath);

            // Draw curved lines between particles
            if (!particle1Trail.isEmpty() && !particle2Trail.isEmpty()) {
                for (int i = -500; i <= 500; i += 100) {
                    drawArcBetweenPoints(g2d, particle1Trail.get(particle1Trail.size() - 1),
                            particle2Trail.get(particle2Trail.size() - 1), i);
                }
            }

            // Add mouse listener outside of the painting loop
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    checkParticleClick(e.getPoint());
                }
            });

            g2d.dispose(); // Dispose of the Graphics2D object to release resources
        }
        private void drawBlackSignPlus(Graphics2D g, Point particle) {
            int signSize = 8; // Adjust the size of the sign
            int signX = particle.x - signSize / 2;
            int signY = particle.y - signSize / 2;

            g.setColor(Color.BLACK);
            Stroke originalStroke = g.getStroke();
            g.setStroke(new BasicStroke(3.0f));
            g.drawLine(particle.x - signSize, particle.y, particle.x + signSize, particle.y);
            g.drawLine(particle.x, particle.y - signSize, particle.x, particle.y + signSize);
            g.setStroke(originalStroke);
        }
        private void drawBlackSignMinus(Graphics2D g, Point particle) {
            int signSize = 8; // Adjust the size of the sign
            int signX = particle.x - signSize / 2;
            int signY = particle.y - signSize / 2;

            g.setColor(Color.BLACK);
            Stroke originalStroke = g.getStroke();
            g.setStroke(new BasicStroke(3.0f));
            g.drawLine(particle.x - signSize, particle.y, particle.x + signSize, particle.y);
            //g.drawLine(particle.x, particle.y - signSize, particle.x, particle.y + signSize);
            g.setStroke(originalStroke);
        }

        private void drawArrowhead(Graphics2D g2d, int x, int y, double dx, double dy) {
            double arrowSize = 10.0;
            double angle = Math.atan2(dy, dx);

            // Draw the arrowhead lines
            g2d.drawLine(x, y, (int) (x - arrowSize * Math.cos(angle - Math.PI / 6)), (int) (y - arrowSize * Math.sin(angle - Math.PI / 6)));
            g2d.drawLine(x, y, (int) (x - arrowSize * Math.cos(angle + Math.PI / 6)), (int) (y - arrowSize * Math.sin(angle + Math.PI / 6)));
        }
        private void checkParticleClick(Point clickPoint) {
            // Check if a particle is clicked, and set the particleToFollow accordingly
            if (!particle1Trail.isEmpty() && isPointInParticle(particle1Trail.get(particle1Trail.size() - 1), clickPoint)) {
                followParticle = true;
                particleToFollow = particle1Trail.get(particle1Trail.size() - 1);
            } else if (!particle2Trail.isEmpty() && isPointInParticle(particle2Trail.get(particle2Trail.size() - 1), clickPoint)) {
                followParticle = true;
                particleToFollow = particle2Trail.get(particle2Trail.size() - 1);
            } else {
                followParticle = false;
                particleToFollow = null;
            }

            // If following a particle, adjust the zoom and translation to focus on the selected particle
            if (followParticle) {
                // Calculate the translation needed to center the chosen particle
                int translateX = getWidth() / 2 - particleToFollow.x;
                int translateY = getHeight() / 2 - particleToFollow.y;

                // Adjust the translation by taking into account the current translation due to zoom
                translateX -= (getWidth() * (1.0 - zoomFactor) / 2);
                translateY -= (getHeight() * (1.0 - zoomFactor) / 2);

                // Adjust the translation
                Graphics2D g2d = (Graphics2D) getGraphics();
                g2d.translate(translateX, translateY);
                g2d.dispose(); // Dispose of the Graphics2D object to release resources
                repaint(); // Repaint the simulation panel
            }
        }

        private boolean isPointInParticle(Point particle, Point clickPoint) {
            // Check if the click point is within the bounds of the particle
            int particleRadius = PARTICLE_SIZE / 2;
            return clickPoint.distance(particle) <= particleRadius;
        }
        Point zoomCenter = new Point(WIDTH / 2, HEIGHT / 2);
        public void setZoomCenter(Point centerOfZoom) {
            this.zoomCenter = centerOfZoom;
        }
    }

    private void drawBackground(Graphics2D g) {
        // Create a BufferedImage for the background
        BufferedImage background = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bgGraphics = background.createGraphics();

        // Draw the blue background to the BufferedImage
        bgGraphics.setColor(Color.DARK_GRAY);
        bgGraphics.fillRect(0, 0, WIDTH, HEIGHT);

        bgGraphics.dispose();

        // Draw the BufferedImage to the main graphics context
        g.drawImage(background, 0, 0, null);
    }

    private void drawRadialGradient(Graphics2D g, Point particle, Color startColor, Color endColor, boolean isParticle1) {
        int radius = (int) (10 * (isParticle1 ? mass1 : mass2) + PARTICLE_SIZE);
        if (radius < 0)
            radius = -radius;
        if (radius == 0)
            radius = 20;
        int centerX = particle.x;
        int centerY = particle.y;

// Create a BufferedImage for the radial gradient
        BufferedImage gradientImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gradientGraphics = gradientImage.createGraphics();

// Draw the radial gradient to the BufferedImage
        RadialGradientPaint gradient = new RadialGradientPaint(
                centerX, centerY, radius,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{startColor, Color.YELLOW, Color.DARK_GRAY}
        );
        gradientGraphics.setPaint(gradient);
        gradientGraphics.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);

        gradientGraphics.dispose();

// Blend the gradient image with the existing image using SRC_OVER rule
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(gradientImage, 0, 0, null);
        g2d.dispose();
    }

    private void drawTrail(Graphics2D g, ArrayList<Point> trail) {
        for (int i = 0; i < trail.size() - 1; i++) {
            Point p1 = trail.get(i);
            Point p2 = trail.get(i + 1);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private void drawMagneticFieldLines(Graphics2D g, Point particle1, Point particle2) {
        g.setColor(Color.CYAN);

        // Draw magnetic field lines as lines connecting the two particles
        g.drawLine(particle1.x, particle1.y, particle2.x, particle2.y);
    }

    private void drawParabolicPath(Graphics2D g, ArrayList<Point> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            Point p1 = path.get(i);
            Point p2 = path.get(i + 1);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private void drawArcBetweenPoints(Graphics2D g, Point particle1, Point particle2, int x) {
        g.setColor(Color.GREEN);

        // Calculate control point for the arc of a circle
        int controlX = (particle1.x + particle2.x) / 2;
        int controlY = (particle1.y + particle2.y) / 2 - x;

        // Calculate the direction vector between the two particles
        int dx = particle2.x - particle1.x;
        int dy = particle2.y - particle1.y;

        // Create a Path2D to represent the arc
        Path2D path = new Path2D.Double();
        path.moveTo(particle1.x, particle1.y);
        path.quadTo(controlX, controlY, particle2.x, particle2.y);
        drawArrowhead(g, controlX, (controlY) + x/2 , dx, dy);

        // Draw the path (arc)
        g.draw(path);
    }

    private void drawArrowhead(Graphics2D g2d, int x, int y, double dx, double dy) {
        double arrowSize = 20.0;
        double angle = Math.atan2(dy, dx);

        // Draw the arrowhead lines
        g2d.drawLine(x, y, (int) (x - arrowSize * Math.cos(angle - Math.PI / 6)), (int) (y - arrowSize * Math.sin(angle - Math.PI / 6)));
        g2d.drawLine(x, y, (int) (x - arrowSize * Math.cos(angle + Math.PI / 6)), (int) (y - arrowSize * Math.sin(angle + Math.PI / 6)));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParticleSimulation simulation = new ParticleSimulation();
            simulation.setVisible(true);
        });
    }
}

class SliderPanel extends JPanel {
    private JButton followParticle1Button;
    private JButton followParticle2Button;
    private Point zoomCenter = new Point(WIDTH / 2, HEIGHT / 2);

    // ... (existing code)

    public void setZoomCenter(Point center) {
        this.zoomCenter = center;
    }
    public SliderPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT)); // Set the layout manager with LEFT alignment

        // Create a slider to control the rotation speed
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 50, 0);
        speedSlider.setMajorTickSpacing(10);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setPreferredSize(new Dimension(100,50));
        // Add a change listener to the slider
        speedSlider.addChangeListener(e -> {
            int value = speedSlider.getValue();
            ParticleSimulation.vtimp = value; // Set the rotation speed in ParticleSimulation
        });

        // Create a slider for modifying mass1
        JSlider mass1Slider = new JSlider(JSlider.HORIZONTAL, 1, 5, 3);

        mass1Slider.setMajorTickSpacing(1);
        mass1Slider.setPreferredSize(new Dimension(100,50));
        mass1Slider.setPaintTicks(true);
        mass1Slider.setPaintLabels(true);

        // Add a change listener to the mass1 slider
        mass1Slider.addChangeListener(e -> {
            int value = mass1Slider.getValue();
            ParticleSimulation.mass1 = value;
        });

        // Create a slider for modifying mass2
        JSlider mass2Slider = new JSlider(JSlider.HORIZONTAL, 1, 5, 3);
        mass2Slider.setPreferredSize(new Dimension(100,50));
        mass2Slider.setMajorTickSpacing(1);
        mass2Slider.setPaintTicks(true);
        mass2Slider.setPaintLabels(true);

        // Add a change listener to the mass2 slider
        mass2Slider.addChangeListener(e -> {
            int value = mass2Slider.getValue();
            ParticleSimulation.mass2 = value;
        });
        JSlider distanceSlider = new JSlider(JSlider.HORIZONTAL, 70, 800, 150);
        distanceSlider.setPreferredSize(new Dimension(400, 50));
        distanceSlider.setMajorTickSpacing(50);
        distanceSlider.setPaintTicks(true);
        distanceSlider.setPaintLabels(true);

        // Add a change listener to the distance slider
        distanceSlider.addChangeListener(e -> {
            int value = distanceSlider.getValue();
            ParticleSimulation.ORBIT_RADIUS = value; // Set the distance between particles in ParticleSimulation
        });
        // Create a slider for zooming
        JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 600, 100); // Adjust min, max, and default values accordingly
        zoomSlider.setMajorTickSpacing(25);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.setPreferredSize(new Dimension(500,50));

        // Add a change listener to the zoomSlider
        zoomSlider.addChangeListener(f -> {
            int value = zoomSlider.getValue();
            double zoomFactor = value / 100.0; // Adjust the scale factor based on the slider value

            // Calculate the center of zoom based on particle positions
            Point centerOfZoom = calculateCenterOfZoom();
            // Update zoom and center of zoom in ParticleSimulation
            ParticleSimulation.zoomFactor = zoomFactor;
            ParticleSimulation.getSimulationPanel().setZoomCenter(centerOfZoom);
            ParticleSimulation.getSimulationPanel().repaint();
        });
        // Update the initial center of mass based on the window size;
        // Add components to this panel
        add(new JLabel("Distanta"));
        add(distanceSlider);
        add(new JLabel("Viteza rotatie: "));
        add(speedSlider);
        add(new JLabel("Sarcina 1 "));
        add(mass1Slider);
        add(new JLabel("Sarcina 2: "));
        add(mass2Slider);
        add(new JLabel("Zoom: "));
        add(zoomSlider);
        followParticle1Button = new JButton("Follow Particle 1");
        followParticle1Button.addActionListener(e -> setFollowParticle(ParticleSimulation.particle1Trail));

        followParticle2Button = new JButton("Follow Particle 2");
        followParticle2Button.addActionListener(e -> setFollowParticle(ParticleSimulation.particle2Trail));
    }
    private Point calculateCenterOfZoom() {
        ParticleSimulation.SimulationPanel simulationPanel = ParticleSimulation.getSimulationPanel();

        if (!ParticleSimulation.particle1Trail.isEmpty() && !ParticleSimulation.particle2Trail.isEmpty()) {
            int centerX = (ParticleSimulation.particle1Trail.get(ParticleSimulation.particle1Trail.size() - 1).x +
                    ParticleSimulation.particle2Trail.get(ParticleSimulation.particle2Trail.size() - 1).x) / 2;
            int centerY = (ParticleSimulation.particle1Trail.get(ParticleSimulation.particle1Trail.size() - 1).y +
                    ParticleSimulation.particle2Trail.get(ParticleSimulation.particle2Trail.size() - 1).y) / 2;

            // Adjust the center based on the current translation due to zoom

            return new Point(centerX, centerY);
        } else {
            // Default to the center of the panel if particle positions are not available
            return new Point(simulationPanel.getWidth() / 2, simulationPanel.getHeight() / 2);
        }
    }


    private void setFollowParticle(ArrayList<Point> particleTrail) {
        ParticleSimulation.getSimulationPanel().setFollowParticle(particleTrail);
    }
}