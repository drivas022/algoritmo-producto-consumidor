import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Esta clase permite generar un archivo con números aleatorios para probar
 * la aplicación Productor-Consumidor
 * Versión modificada: un número por línea
 */
public class GeneradorNumeros {
    public static void main(String[] args) {
        String archivo = "numeros.txt";
        int cantidadNumeros = 100;
        int minValor = 1;
        int maxValor = 1000;
        
        generarArchivoNumeros(archivo, cantidadNumeros, minValor, maxValor);
    }
    
    /**
     * Genera un archivo de texto con números aleatorios, un número por línea
     * @param nombreArchivo Nombre del archivo a generar
     * @param cantidad Cantidad de números a generar
     * @param min Valor mínimo
     * @param max Valor máximo
     */
    public static void generarArchivoNumeros(String nombreArchivo, int cantidad, int min, int max) {
        Random random = new Random();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(nombreArchivo))) {
            for (int i = 0; i < cantidad; i++) {
                int numero = random.nextInt(max - min + 1) + min;
                writer.println(numero); // Escribir un número por línea
            }
            
            System.out.println("Archivo " + nombreArchivo + " generado con exito");
            
        } catch (IOException e) {
            System.err.println("Error al generar el archivo: " + e.getMessage());
        }
    }
}