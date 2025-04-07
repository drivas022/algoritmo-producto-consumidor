import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;

/**
 * Esta clase representa un número con su clasificación
 */
class Producto {
    private int valor;
    private boolean esPar;
    private boolean esImpar;
    private boolean esPrimo;

    public Producto(int valor) {
        this.valor = valor;
        this.esPar = valor % 2 == 0;
        this.esImpar = valor % 2 != 0;
        this.esPrimo = esPrimo(valor);
    }

    // Método para verificar si un número es primo
    private boolean esPrimo(int numero) {
        if (numero <= 1) return false;
        if (numero <= 3) return true;
        if (numero % 2 == 0 || numero % 3 == 0) return false;
        
        int i = 5;
        while (i * i <= numero) {
            if (numero % i == 0 || numero % (i + 2) == 0) return false;
            i += 6;
        }
        return true;
    }

    public int getValor() {
        return valor;
    }

    public boolean esPar() {
        return esPar;
    }

    public boolean esImpar() {
        return esImpar;
    }

    public boolean esPrimo() {
        return esPrimo;
    }

    @Override
    public String toString() {
        return Integer.toString(valor);
    }
}

/**
 * Clase que representa un productor que lee números de un archivo
 */
class Productor implements Runnable {
    private final BlockingQueue<Producto> buffer;
    private final String archivo;
    private final Animacion animacion;
    private volatile boolean ejecutando = true;

    public Productor(BlockingQueue<Producto> buffer, String archivo, Animacion animacion) {
        this.buffer = buffer;
        this.archivo = archivo;
        this.animacion = animacion;
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null && ejecutando) {
                String[] numeros = linea.split("\\s+");
                for (String numStr : numeros) {
                    try {
                        int num = Integer.parseInt(numStr.trim());
                        Producto producto = new Producto(num);
                        buffer.put(producto); // BlockingQueue.put() bloqueará si el buffer está lleno
                        animacion.actualizar("Productor produjo: " + producto.getValor());
                        animacion.actualizarBuffer(new ArrayList<>(buffer));
                        Thread.sleep(500); // Pausa para visualizar
                    } catch (NumberFormatException e) {
                        System.err.println("Error al parsear número: " + numStr);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            animacion.actualizar("Productor ha terminado de leer el archivo");
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        } finally {
            System.out.println("Productor terminó");
        }
    }

    public void detener() {
        ejecutando = false;
    }
}

/**
 * Clase que representa un consumidor que toma números del buffer según su tipo
 */
class Consumidor implements Runnable {
    private final BlockingQueue<Producto> buffer;
    private final String tipo;
    private final int id;
    private final Animacion animacion;
    private volatile boolean ejecutando = true;
    private int suma = 0;

    public Consumidor(BlockingQueue<Producto> buffer, String tipo, int id, Animacion animacion) {
        this.buffer = buffer;
        this.tipo = tipo;
        this.id = id;
        this.animacion = animacion;
    }

    @Override
    public void run() {
        try {
            while (ejecutando) {
                Producto producto = null;
                
                // Buscar un producto adecuado para este consumidor
                synchronized (buffer) {
                    for (Producto p : buffer) {
                        if ((tipo.equals("par") && p.esPar()) ||
                            (tipo.equals("impar") && p.esImpar()) ||
                            (tipo.equals("primo") && p.esPrimo())) {
                            producto = p;
                            buffer.remove(p);
                            break;
                        }
                    }
                    
                    if (producto == null) {
                        // No hay productos adecuados, esperar
                        buffer.wait(100);
                        continue;
                    }
                }
                
                // Procesar el producto
                suma += producto.getValor();
                animacion.actualizar("Consumidor " + id + " (" + tipo + ") consumió: " + producto.getValor() + ", Suma: " + suma);
                animacion.actualizarSuma(id, suma);
                animacion.actualizarBuffer(new ArrayList<>(buffer));
                Thread.sleep(800); // Pausa para visualizar
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Consumidor " + id + " (" + tipo + ") terminó con suma: " + suma);
        }
    }

    public void detener() {
        ejecutando = false;
    }
    
    public int getSuma() {
        return suma;
    }
}

/**
 * Clase para la animación gráfica
 */
class Animacion {
    private JFrame frame;
    private JTextArea logArea;
    private JPanel bufferPanel;
    private JLabel[] sumaLabels;
    
    public Animacion(int numConsumidores) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Problema Productor-Consumidor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLayout(new BorderLayout());
            
            // Panel superior para el buffer
            bufferPanel = new JPanel();
            bufferPanel.setBorder(BorderFactory.createTitledBorder("Buffer"));
            frame.add(bufferPanel, BorderLayout.NORTH);
            
            // Panel central para los logs
            logArea = new JTextArea(20, 60);
            logArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(logArea);
            frame.add(scrollPane, BorderLayout.CENTER);
            
            // Panel inferior para las sumas
            JPanel sumasPanel = new JPanel(new GridLayout(1, numConsumidores));
            sumasPanel.setBorder(BorderFactory.createTitledBorder("Sumas Acumuladas"));
            sumaLabels = new JLabel[numConsumidores];
            
            for (int i = 0; i < numConsumidores; i++) {
                String tipo = "";
                if (i % 3 == 0) tipo = "par";
                else if (i % 3 == 1) tipo = "impar";
                else tipo = "primo";
                
                sumaLabels[i] = new JLabel("Consumidor " + i + " (" + tipo + "): 0");
                sumasPanel.add(sumaLabels[i]);
            }
            
            frame.add(sumasPanel, BorderLayout.SOUTH);
            frame.setVisible(true);
        });
    }
    
    public void actualizar(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(mensaje + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void actualizarSuma(int idConsumidor, int suma) {
        SwingUtilities.invokeLater(() -> {
            String tipo = "";
            if (idConsumidor % 3 == 0) tipo = "par";
            else if (idConsumidor % 3 == 1) tipo = "impar";
            else tipo = "primo";
            
            sumaLabels[idConsumidor].setText("Consumidor " + idConsumidor + " (" + tipo + "): " + suma);
        });
    }
    
    public void actualizarBuffer(List<Producto> productos) {
        SwingUtilities.invokeLater(() -> {
            bufferPanel.removeAll();
            for (Producto p : productos) {
                JLabel label = new JLabel(p.toString());
                
                // Asignar color según el tipo de número
                if (p.esPrimo()) {
                    label.setForeground(Color.RED);
                } else if (p.esPar()) {
                    label.setForeground(Color.BLUE);
                } else {
                    label.setForeground(Color.GREEN);
                }
                
                bufferPanel.add(label);
            }
            bufferPanel.revalidate();
            bufferPanel.repaint();
        });
    }
}

/**
 * Clase principal que orquesta todo el sistema
 */
public class ProductorConsumidor {
    public static void main(String[] args) {
        // Configuración
        int tamanoBuffer = 10;
        String archivoNumeros = "numeros.txt";
        int numConsumidores = 3; // Múltiplo de 3 como especifica el problema
        
        // Crear buffer compartido
        BlockingQueue<Producto> buffer = new LinkedBlockingQueue<>(tamanoBuffer);
        
        // Crear la animación
        Animacion animacion = new Animacion(numConsumidores);
        
        // Crear productor
        Productor productor = new Productor(buffer, archivoNumeros, animacion);
        Thread threadProductor = new Thread(productor);
        
        // Crear consumidores
        List<Consumidor> consumidores = new ArrayList<>();
        List<Thread> threadsConsumidores = new ArrayList<>();
        
        for (int i = 0; i < numConsumidores; i++) {
            String tipo;
            if (i % 3 == 0) tipo = "par";
            else if (i % 3 == 1) tipo = "impar";
            else tipo = "primo";
            
            Consumidor consumidor = new Consumidor(buffer, tipo, i, animacion);
            Thread threadConsumidor = new Thread(consumidor);
            
            consumidores.add(consumidor);
            threadsConsumidores.add(threadConsumidor);
        }
        
        // Iniciar todos los hilos
        threadProductor.start();
        for (Thread t : threadsConsumidores) {
            t.start();
        }
        
        // Configurar apagado adecuado
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Deteniendo todos los hilos...");
            productor.detener();
            for (Consumidor c : consumidores) {
                c.detener();
            }
            
            try {
                threadProductor.join(1000);
                for (Thread t : threadsConsumidores) {
                    t.join(1000);
                }
            } catch (InterruptedException e) {
                System.err.println("Error al detener hilos: " + e.getMessage());
            }
        }));
    }
}