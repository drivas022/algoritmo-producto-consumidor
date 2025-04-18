import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import javax.swing.*;
import javax.swing.border.TitledBorder;

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
 * Buffer compartido utilizando una lista enlazada y semáforos para sincronización
 */
class BufferCompartido {
    private final LinkedList<Producto> buffer;
    private final int capacidad;
    
    // Semáforos para control de concurrencia
    private final Semaphore mutex;     // Controla el acceso a la sección crítica
    private final Semaphore empty;     // Controla espacios disponibles en el buffer
    private final Semaphore full;      // Controla elementos disponibles en el buffer
    
    // Semáforos adicionales para los tipos específicos de consumidores
    private final Semaphore semPares;  // Para números pares
    private final Semaphore semImpares; // Para números impares
    private final Semaphore semPrimos; // Para números primos
    
    public BufferCompartido(int capacidad) {
        this.capacidad = capacidad;
        this.buffer = new LinkedList<>();
        
        // Inicializar semáforos
        this.mutex = new Semaphore(1, true); // Semáforo binario (mutex)
        this.empty = new Semaphore(capacidad, true); // Inicialmente, todos los espacios están vacíos
        this.full = new Semaphore(0, true); // Inicialmente, no hay elementos
        
        // Inicializar semáforos para tipos específicos
        this.semPares = new Semaphore(0, true);
        this.semImpares = new Semaphore(0, true);
        this.semPrimos = new Semaphore(0, true);
    }
    
    /**
     * Añade un producto al buffer
     */
    public void poner(Producto producto) throws InterruptedException {
        empty.acquire();  // Esperar si no hay espacio disponible
        mutex.acquire();  // Entrar en la sección crítica
        
        try {
            buffer.add(producto); // Añadir el producto al buffer
            
            // Señalizar a los consumidores específicos según el tipo de número
            if (producto.esPar()) {
                semPares.release();
            }
            if (producto.esImpar()) {
                semImpares.release();
            }
            if (producto.esPrimo()) {
                semPrimos.release();
            }
        } finally {
            mutex.release();  // Salir de la sección crítica
            full.release();   // Señalizar que hay un elemento más en el buffer
        }
    }
    
    /**
     * Obtiene un producto del buffer según el tipo especificado
     */
    public Producto tomar(String tipo) throws InterruptedException {
        Producto producto = null;
        
        // Esperar por el tipo específico de número
        switch (tipo) {
            case "par":
                semPares.acquire();
                break;
            case "impar":
                semImpares.acquire();
                break;
            case "primo":
                semPrimos.acquire();
                break;
        }
        
        full.acquire();  // Esperar si el buffer está vacío
        mutex.acquire(); // Entrar en la sección crítica
        
        try {
            // Buscar un producto del tipo correcto
            int index = -1;
            for (int i = 0; i < buffer.size(); i++) {
                Producto p = buffer.get(i);
                if ((tipo.equals("par") && p.esPar()) ||
                    (tipo.equals("impar") && p.esImpar()) ||
                    (tipo.equals("primo") && p.esPrimo())) {
                    index = i;
                    break;
                }
            }
            
            if (index != -1) {
                producto = buffer.remove(index);
            }
        } finally {
            mutex.release(); // Salir de la sección crítica
            empty.release(); // Señalizar que hay un espacio más en el buffer
        }
        
        return producto;
    }
    
    /**
     * Obtiene una copia de los elementos actuales del buffer (para visualización)
     */
    public List<Producto> getElementos() {
        List<Producto> copia = new ArrayList<>();
        try {
            mutex.acquire();
            copia.addAll(buffer);
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return copia;
    }
    
    /**
     * Devuelve el tamaño actual del buffer
     */
    public int getTamano() {
        try {
            mutex.acquire();
            int tamano = buffer.size();
            mutex.release();
            return tamano;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    /**
     * Limpia el buffer (usado para reiniciar)
     */
    public void limpiar() {
        try {
            mutex.acquire();
            
            // Vaciar el buffer
            int tamanoActual = buffer.size();
            buffer.clear();
            
            // Reiniciar los semáforos
            // Drenar los permisos existentes
            drainPermits(full);
            drainPermits(semPares);
            drainPermits(semImpares);
            drainPermits(semPrimos);
            
            // Reiniciar el semáforo empty
            drainPermits(empty);
            empty.release(capacidad);
            
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Método auxiliar para drenar todos los permisos de un semáforo
     */
    private void drainPermits(Semaphore semaphore) {
        semaphore.drainPermits();
    }
}

/**
 * Clase que representa un productor que lee números de un archivo
 */
class Productor implements Runnable {
    private final BufferCompartido buffer;
    private final String archivo;
    private final Animacion animacion;
    private volatile boolean ejecutando = true;

    public Productor(BufferCompartido buffer, String archivo, Animacion animacion) {
        this.buffer = buffer;
        this.archivo = archivo;
        this.animacion = animacion;
    }

    @Override
    public void run() {
        // Verificar que el archivo existe antes de intentar leerlo
        File file = new File(archivo);
        if (!file.exists() || !file.canRead()) {
            String mensaje = "Error: No se puede leer el archivo " + archivo;
            System.err.println(mensaje);
            animacion.actualizar(mensaje);
            animacion.actualizar("Por favor, ejecute primero GeneradorNumeros.java");
            return;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null && ejecutando) {
                String[] numeros = linea.split("\\s+");
                for (String numStr : numeros) {
                    try {
                        // Verificar si la animación está pausada
                        if (animacion.estaPausado()) continue;
                        
                        int num = Integer.parseInt(numStr.trim());
                        Producto producto = new Producto(num);
                        
                        // Usar el buffer con semáforos para añadir el producto
                        buffer.poner(producto);
                        
                        animacion.actualizar("Productor produjo: " + producto.getValor());
                        animacion.actualizarBuffer(buffer.getElementos());
                        
                        // Actualizar estadísticas
                        animacion.actualizarEstadisticas("producido", producto.getValor());
                        animacion.actualizarUtilizacionBuffer(buffer.getTamano());
                        
                        Thread.sleep(animacion.getDelayProductor()); // Usar el delay dinámico
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
    private final BufferCompartido buffer;
    private final String tipo;
    private final int id;
    private final Animacion animacion;
    private volatile boolean ejecutando = true;
    private int suma = 0;

    public Consumidor(BufferCompartido buffer, String tipo, int id, Animacion animacion) {
        this.buffer = buffer;
        this.tipo = tipo;
        this.id = id;
        this.animacion = animacion;
    }

    @Override
    public void run() {
        try {
            while (ejecutando) {
                // Verificar si la animación está pausada
                if (animacion.estaPausado()) continue;
                
                // Tomar un producto del buffer usando semáforos
                Producto producto = buffer.tomar(tipo);
                
                if (producto != null) {
                    suma += producto.getValor();
                    animacion.actualizar("Consumidor " + id + " (" + tipo + ") consumió: " + producto.getValor() + ", Suma: " + suma);
                    animacion.actualizarSuma(id, suma);
                    animacion.actualizarBuffer(buffer.getElementos());
                    
                    // Actualizar estadísticas
                    animacion.actualizarEstadisticas("consumido", producto.getValor());
                    animacion.actualizarUtilizacionBuffer(buffer.getTamano());
                    
                    Thread.sleep(animacion.getDelayConsumidor()); // Usar el delay dinámico
                }
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
    private JLabel estadoProductor;
    private JLabel[] estadoConsumidores;
    private Font fuenteTitulos = new Font("Arial", Font.BOLD, 14);
    private Font fuenteNormal = new Font("Arial", Font.PLAIN, 12);
    
    // Nuevos atributos para el panel de control
    private JButton btnPausar;
    private JButton btnReanudar;
    private JButton btnReiniciar;
    private volatile boolean pausado = false;
    
    // Atributos para control de velocidad
    private JSlider sliderVelocidad;
    private JLabel lblVelocidad;
    private volatile int delayProductor = 500;  // valor por defecto en ms
    private volatile int delayConsumidor = 800; // valor por defecto en ms
    
    // Atributos para estadísticas
    private Map<String, JLabel> estadisticas = new HashMap<>();
    private int totalProducidos = 0;
    private int totalConsumidos = 0;
    private int paresConsumidos = 0;
    private int imparesConsumidos = 0;
    private int primosConsumidos = 0;
    private int capacidadBuffer = 10;
    
    public Animacion(int numConsumidores) {
        SwingUtilities.invokeLater(() -> {
            // Configurar el estilo del Look and Feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Error al configurar Look and Feel: " + e.getMessage());
            }
            
            // Crear la ventana principal
            frame = new JFrame("Simulación Productor-Consumidor con Semáforos");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 700); // Aumentado tamaño para acomodar panel de control
            frame.setLayout(new BorderLayout(10, 10));
            frame.getContentPane().setBackground(new Color(240, 240, 240));
            
            // Panel de información
            JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Panel de estado
            JPanel estadoPanel = new JPanel(new GridLayout(1 + numConsumidores, 1, 5, 5));
            estadoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1, true),
                "Estado de Procesos",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                fuenteTitulos
            ));
            
            // Estado del productor
            estadoProductor = new JLabel("Productor: Esperando");
            estadoProductor.setFont(fuenteNormal);
            try {
                estadoProductor.setIcon(new ImageIcon(getClass().getResource("/images/wait.png")));
            } catch (Exception e) {
                // Si no encuentra el ícono, continuar sin él
            }
            estadoPanel.add(estadoProductor);
            
            // Estado de los consumidores
            estadoConsumidores = new JLabel[numConsumidores];
            for (int i = 0; i < numConsumidores; i++) {
                String tipo = "";
                if (i % 3 == 0) tipo = "par";
                else if (i % 3 == 1) tipo = "impar";
                else tipo = "primo";
                
                estadoConsumidores[i] = new JLabel("Consumidor " + i + " (" + tipo + "): Esperando");
                estadoConsumidores[i].setFont(fuenteNormal);
                try {
                    estadoConsumidores[i].setIcon(new ImageIcon(getClass().getResource("/images/wait.png")));
                } catch (Exception e) {
                    // Si no encuentra el ícono, continuar sin él
                }
                estadoPanel.add(estadoConsumidores[i]);
            }
            
            infoPanel.add(estadoPanel, BorderLayout.NORTH);
            
            // Panel superior para el buffer
            JPanel bufferContainer = new JPanel(new BorderLayout());
            JLabel bufferTitle = new JLabel("Buffer compartido", SwingConstants.CENTER);
            bufferTitle.setFont(fuenteTitulos);
            bufferContainer.add(bufferTitle, BorderLayout.NORTH);
            
            bufferPanel = new JPanel();
            bufferPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
            bufferPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true)
            ));
            bufferPanel.setBackground(Color.WHITE);
            
            bufferContainer.add(bufferPanel, BorderLayout.CENTER);
            frame.add(bufferContainer, BorderLayout.NORTH);
            
            // Panel central para los logs
            JLabel logTitle = new JLabel("Registro de actividad", SwingConstants.CENTER);
            logTitle.setFont(fuenteTitulos);
            
            logArea = new JTextArea(15, 60);
            logArea.setEditable(false);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setBackground(new Color(250, 250, 250));
            logArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            JScrollPane scrollPane = new JScrollPane(logArea);
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
            
            JPanel logPanel = new JPanel(new BorderLayout(5, 5));
            logPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            logPanel.add(logTitle, BorderLayout.NORTH);
            logPanel.add(scrollPane, BorderLayout.CENTER);
            
            frame.add(logPanel, BorderLayout.CENTER);
            
            // Panel inferior para las sumas
            JPanel sumasContainer = new JPanel(new BorderLayout(5, 5));
            JLabel sumasTitle = new JLabel("Sumas Acumuladas", SwingConstants.CENTER);
            sumasTitle.setFont(fuenteTitulos);
            sumasContainer.add(sumasTitle, BorderLayout.NORTH);
            
            JPanel sumasPanel = new JPanel(new GridLayout(1, numConsumidores, 15, 5));
            sumasPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            sumaLabels = new JLabel[numConsumidores];
            
            for (int i = 0; i < numConsumidores; i++) {
                String tipo = "";
                Color color = Color.BLACK;
                
                if (i % 3 == 0) {
                    tipo = "Pares";
                    color = new Color(0, 102, 204); // Azul
                } else if (i % 3 == 1) {
                    tipo = "Impares";
                    color = new Color(0, 153, 0); // Verde
                } else {
                    tipo = "Primos";
                    color = new Color(204, 0, 0); // Rojo
                }
                
                JPanel consumidorPanel = new JPanel(new BorderLayout(5, 5));
                consumidorPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(color, 2, true),
                    "Consumidor " + i + " (" + tipo + ")",
                    TitledBorder.CENTER,
                    TitledBorder.TOP,
                    new Font("Arial", Font.BOLD, 12),
                    color
                ));
                
                sumaLabels[i] = new JLabel("0", SwingConstants.CENTER);
                sumaLabels[i].setFont(new Font("Arial", Font.BOLD, 24));
                sumaLabels[i].setForeground(color);
                
                consumidorPanel.add(sumaLabels[i], BorderLayout.CENTER);
                sumasPanel.add(consumidorPanel);
            }
            
            sumasContainer.add(sumasPanel, BorderLayout.CENTER);
            frame.add(sumasContainer, BorderLayout.SOUTH);
            
            // Crear panel de control
            crearPanelControl();
            
            // Mostrar ventana
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    private void crearPanelControl() {
        JPanel controlPanel = new JPanel(new GridLayout(5, 1, 5, 10)); // Aumentado para incluir slider
        controlPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1, true),
            "Control",
            TitledBorder.CENTER,
            TitledBorder.TOP,
            fuenteTitulos
        ));
        
        btnPausar = new JButton("Pausar");
        btnReanudar = new JButton("Reanudar");
        btnReiniciar = new JButton("Reiniciar");
        
        btnPausar.setFont(fuenteNormal);
        btnReanudar.setFont(fuenteNormal);
        btnReiniciar.setFont(fuenteNormal);
        
        btnReanudar.setEnabled(false);
        
        btnPausar.addActionListener(e -> {
            pausado = true;
            btnPausar.setEnabled(false);
            btnReanudar.setEnabled(true);
            logArea.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] Simulación pausada\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        
        btnReanudar.addActionListener(e -> {
            pausado = false;
            btnPausar.setEnabled(true);
            btnReanudar.setEnabled(false);
            logArea.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] Simulación reanudada\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
            synchronized (this) {
                this.notifyAll(); // Notificar a todos los hilos que estén esperando
            }
        });
        
        // Botón de reinicio
        btnReiniciar.addActionListener(e -> {
            logArea.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] Reiniciando simulación...\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
            
            // La funcionalidad real de reinicio se implementa en la clase principal
            ProductorConsumidorSemaforos.reiniciarSimulacion();
        });
        
        // Añadir control de velocidad
        JPanel velocidadPanel = new JPanel(new BorderLayout(5, 5));
        lblVelocidad = new JLabel("Velocidad: Normal", SwingConstants.CENTER);
        lblVelocidad.setFont(fuenteNormal);
        
        sliderVelocidad = new JSlider(JSlider.HORIZONTAL, 1, 5, 3);
        sliderVelocidad.setMajorTickSpacing(1);
        sliderVelocidad.setPaintTicks(true);
        sliderVelocidad.setPaintLabels(true);
        sliderVelocidad.setSnapToTicks(true);
        
        // Etiquetas para el slider
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(1, new JLabel("Muy lento"));
        labelTable.put(2, new JLabel("Lento"));
        labelTable.put(3, new JLabel("Normal"));
        labelTable.put(4, new JLabel("Rápido"));
        labelTable.put(5, new JLabel("Muy rápido"));
        sliderVelocidad.setLabelTable(labelTable);
        
        sliderVelocidad.addChangeListener(e -> {
            int valor = sliderVelocidad.getValue();
            switch(valor) {
                case 1: // Muy lento
                    delayProductor = 2000;
                    delayConsumidor = 3000;
                    lblVelocidad.setText("Velocidad: Muy lenta");
                    break;
                case 2: // Lento
                    delayProductor = 1000;
                    delayConsumidor = 1500;
                    lblVelocidad.setText("Velocidad: Lenta");
                    break;
                case 3: // Normal
                    delayProductor = 500;
                    delayConsumidor = 800;
                    lblVelocidad.setText("Velocidad: Normal");
                    break;
                case 4: // Rápido
                    delayProductor = 200;
                    delayConsumidor = 300;
                    lblVelocidad.setText("Velocidad: Rápida");
                    break;
                case 5: // Muy rápido
                    delayProductor = 50;
                    delayConsumidor = 100;
                    lblVelocidad.setText("Velocidad: Muy rápida");
                    break;
            }
        });
        
        velocidadPanel.add(lblVelocidad, BorderLayout.NORTH);
        velocidadPanel.add(sliderVelocidad, BorderLayout.CENTER);
        
        // Añadir componentes al panel
        controlPanel.add(btnPausar);
        controlPanel.add(btnReanudar);
        controlPanel.add(btnReiniciar);
        controlPanel.add(velocidadPanel);
        
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(controlPanel, BorderLayout.NORTH);
        
        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1, true),
            "Estadísticas",
            TitledBorder.CENTER,
            TitledBorder.TOP,
            fuenteTitulos
        ));
        
        // Inicializar contadores
        JLabel lblTotalProducidos = new JLabel("Total producidos: 0", SwingConstants.LEFT);
        JLabel lblTotalConsumidos = new JLabel("Total consumidos: 0", SwingConstants.LEFT);
        JLabel lblParesConsumidos = new JLabel("Pares consumidos: 0", SwingConstants.LEFT);
        JLabel lblImparesConsumidos = new JLabel("Impares consumidos: 0", SwingConstants.LEFT);
        JLabel lblPrimosConsumidos = new JLabel("Primos consumidos: 0", SwingConstants.LEFT);
        JLabel lblBufferUtilizacion = new JLabel("Buffer utilización: 0%", SwingConstants.LEFT);
        
        lblTotalProducidos.setFont(fuenteNormal);
        lblTotalConsumidos.setFont(fuenteNormal);
        lblParesConsumidos.setFont(fuenteNormal);
        lblImparesConsumidos.setFont(fuenteNormal);
        lblPrimosConsumidos.setFont(fuenteNormal);
        lblBufferUtilizacion.setFont(fuenteNormal);
        
        statsPanel.add(lblTotalProducidos);
        statsPanel.add(lblTotalConsumidos);
        statsPanel.add(lblParesConsumidos);
        statsPanel.add(lblImparesConsumidos);
        statsPanel.add(lblPrimosConsumidos);
        statsPanel.add(lblBufferUtilizacion);
        
        // Guardar referencias para actualizar estos valores
        estadisticas.put("totalProducidos", lblTotalProducidos);
        estadisticas.put("totalConsumidos", lblTotalConsumidos);
        estadisticas.put("paresConsumidos", lblParesConsumidos);
        estadisticas.put("imparesConsumidos", lblImparesConsumidos);
        estadisticas.put("primosConsumidos", lblPrimosConsumidos);
        estadisticas.put("bufferUtilizacion", lblBufferUtilizacion);
        
        eastPanel.add(statsPanel, BorderLayout.CENTER);
        
        // Agregar panel al este de la ventana principal
        frame.add(eastPanel, BorderLayout.EAST);
    }
    
    public synchronized boolean estaPausado() throws InterruptedException {
        while (pausado) {
            wait();
        }
        return false;
    }
    
    public int getDelayProductor() {
        return delayProductor;
    }

    public int getDelayConsumidor() {
        return delayConsumidor;
    }
    
    public void actualizar(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            // Añadir timestamp al mensaje
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            logArea.append("[" + timestamp + "] " + mensaje + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
            
            // Actualizar estado
            if (mensaje.startsWith("Productor produjo")) {
                estadoProductor.setText("Productor: Produciendo");
                try {
                    estadoProductor.setIcon(new ImageIcon(getClass().getResource("/images/producing.png")));
                } catch (Exception e) {
                    // Si no encuentra el ícono, continuar sin él
                }
            } else if (mensaje.startsWith("Productor ha terminado")) {
                estadoProductor.setText("Productor: Finalizado");
                try {
                    estadoProductor.setIcon(new ImageIcon(getClass().getResource("/images/done.png")));
                } catch (Exception e) {
                    // Si no encuentra el ícono, continuar sin él
                }
            }
            
            for (int i = 0; i < estadoConsumidores.length; i++) {
                if (mensaje.startsWith("Consumidor " + i + " ")) {
                    estadoConsumidores[i].setText("Consumidor " + i + ": Consumiendo");
                    try {
                        estadoConsumidores[i].setIcon(new ImageIcon(getClass().getResource("/images/consuming.png")));
                    } catch (Exception e) {
                        // Si no encuentra el ícono, continuar sin él
                    }
                }
            }
        });
    }
    
    public void actualizarEstadisticas(String tipo, int valor) {
        SwingUtilities.invokeLater(() -> {
            if (tipo.equals("producido")) {
                totalProducidos++;
                estadisticas.get("totalProducidos").setText("Total producidos: " + totalProducidos);
            } else if (tipo.equals("consumido")) {
                totalConsumidos++;
                estadisticas.get("totalConsumidos").setText("Total consumidos: " + totalConsumidos);
                
                if (valor % 2 == 0) {
                    paresConsumidos++;
                    estadisticas.get("paresConsumidos").setText("Pares consumidos: " + paresConsumidos);
                }
                
                if (valor % 2 != 0) {
                    imparesConsumidos++;
                    estadisticas.get("imparesConsumidos").setText("Impares consumidos: " + imparesConsumidos);
                }
                
                // Verificar si es primo
                boolean esPrimo = true;
                if (valor <= 1) esPrimo = false;
                else if (valor <= 3) esPrimo = true;
                else if (valor % 2 == 0 || valor % 3 == 0) esPrimo = false;
                else {
                    int i = 5;
                    while (i * i <= valor) {
                        if (valor % i == 0 || valor % (i + 2) == 0) {
                            esPrimo = false;
                            break;
                        }
                        i += 6;
                    }
                }
                
                if (esPrimo) {
                    primosConsumidos++;
                    estadisticas.get("primosConsumidos").setText("Primos consumidos: " + primosConsumidos);
                }
            }
        });
    }
    
    public void actualizarUtilizacionBuffer(int tamanoActual) {
        SwingUtilities.invokeLater(() -> {
            int porcentaje = (tamanoActual * 100) / capacidadBuffer;
            estadisticas.get("bufferUtilizacion").setText("Buffer utilización: " + porcentaje + "%");
        });
    }
    
    public void actualizarSuma(int idConsumidor, int suma) {
        SwingUtilities.invokeLater(() -> {
            sumaLabels[idConsumidor].setText(String.valueOf(suma));
        });
    }
    
    public void actualizarBuffer(List<Producto> productos) {
        SwingUtilities.invokeLater(() -> {
            bufferPanel.removeAll();
            
            // Si no hay productos, mostrar mensaje
            if (productos.isEmpty()) {
                JLabel emptyLabel = new JLabel("Buffer vacío");
                emptyLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                emptyLabel.setForeground(Color.GRAY);
                bufferPanel.add(emptyLabel);
            } else {
                // Mostrar cada producto como un elemento visual
                for (Producto p : productos) {
                    JPanel productoPanel = new JPanel();
                    productoPanel.setPreferredSize(new Dimension(50, 50));
                    productoPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
                    productoPanel.setLayout(new BorderLayout());
                    
                    JLabel valorLabel = new JLabel(String.valueOf(p.getValor()), SwingConstants.CENTER);
                    valorLabel.setFont(new Font("Arial", Font.BOLD, 16));
                    
                    // Asignar color según el tipo de número
                    Color backgroundColor;
                    if (p.esPrimo()) {
                        backgroundColor = new Color(255, 200, 200); // Rojo claro
                        valorLabel.setForeground(new Color(204, 0, 0));
                    } else if (p.esPar()) {
                        backgroundColor = new Color(200, 220, 255); // Azul claro
                        valorLabel.setForeground(new Color(0, 102, 204));
                    } else {
                        backgroundColor = new Color(200, 255, 200); // Verde claro
                        valorLabel.setForeground(new Color(0, 153, 0));
                    }
                    
                    productoPanel.setBackground(backgroundColor);
                    productoPanel.add(valorLabel, BorderLayout.CENTER);
                    
                    // Añadir etiqueta pequeña para indicar el tipo
                    JLabel tipoLabel = new JLabel("", SwingConstants.CENTER);
                    tipoLabel.setFont(new Font("Arial", Font.PLAIN, 10));   
                    
                    if (p.esPrimo()) {
                        tipoLabel.setText("Primo");
                    } else if (p.esPar()) {
                        tipoLabel.setText("Par");
                    } else {
                        tipoLabel.setText("Impar");
                    }
                    
                    productoPanel.add(tipoLabel, BorderLayout.SOUTH);
                    bufferPanel.add(productoPanel);
                }
            }
            
            bufferPanel.revalidate();
            bufferPanel.repaint();
        });
    }
}

/**
 * Clase principal que orquesta todo el sistema
 */
public class ProductorConsumidorSemaforos {
    // Variables de clase para permitir reinicio
    private static BufferCompartido buffer;
    private static Animacion animacion;
    private static Productor productor;
    private static Thread threadProductor;
    private static List<Consumidor> consumidores;
    private static List<Thread> threadsConsumidores;
    private static String archivoNumeros;
    private static int numConsumidores;
    
    public static void main(String[] args) {
        // Configuración
        int tamanoBuffer = 10;
        archivoNumeros = "numeros.txt";
        numConsumidores = 3; // Múltiplo de 3 como especifica el problema
        
        // Crear buffer compartido
        buffer = new BufferCompartido(tamanoBuffer);
        
        // Crear la animación
        animacion = new Animacion(numConsumidores);
        
        // Crear productor
        productor = new Productor(buffer, archivoNumeros, animacion);
        threadProductor = new Thread(productor);
        
        // Crear consumidores
        consumidores = new ArrayList<>();
        threadsConsumidores = new ArrayList<>();
        
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
    
    public static void reiniciarSimulacion() {
        // Este método sería llamado por el botón de reinicio
        // Detener hilos actuales
        productor.detener();
        for (Consumidor c : consumidores) {
            c.detener();
        }
        
        try {
            // Esperar a que terminen
            threadProductor.join(1000);
            for (Thread t : threadsConsumidores) {
                t.join(1000);
            }
            
            // Limpiar buffer
            buffer.limpiar();
            
            // Crear nuevos hilos
            productor = new Productor(buffer, archivoNumeros, animacion);
            threadProductor = new Thread(productor);
            
            consumidores.clear();
            threadsConsumidores.clear();
            
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
            
            // Iniciar nuevos hilos
            threadProductor.start();
            for (Thread t : threadsConsumidores) {
                t.start();
            }
            
        } catch (InterruptedException e) {
            System.err.println("Error al reiniciar simulación: " + e.getMessage());
        }
    }
}