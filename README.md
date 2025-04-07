# Algoritmo-producto-consumidor
Este repositorio implementa un algoritmo clásico de sincronización de sistemas operativos conocido como el problema del productor-consumidor. 


# Proyecto Productor-Consumidor

Este proyecto implementa el algoritmo clásico de Productor-Consumidor utilizando Java, con una interfaz gráfica para visualizar el proceso en tiempo real.

## Requisitos Previos

### Versión de Java

Este proyecto requiere Java 21. Para verificar su versión actual, ejecute:

```bash
java --version
```

Si la versión es correcta, verá un resultado similar a:

```bash
openjdk 21 2023-09-19
OpenJDK Runtime Environment (build 21+35-2513)
OpenJDK 64-Bit Server VM (build 21+35-2513, mixed mode, sharing)
```

Si no tiene la versión correcta, descargue e instale Java 21 desde [el sitio oficial de Oracle](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) o utilice [OpenJDK](https://jdk.java.net/21/).

## Estructura del Proyecto

```
ALGORITMO-PRODUCTO-CONSUMIDOR/
├── src/
│   ├── GeneradorNumeros.java      # Genera archivo con números aleatorios
│   └── ProductorConsumidor.java   # Implementación principal
├── numeros.txt                    # Archivo con los números a procesar
└── README.md                      # Este archivo
```

## Descripción del Problema

El problema Productor-Consumidor es un clásico en programación concurrente donde:

1. Un **Productor** genera datos (en este caso, lee números de un archivo) y los coloca en un buffer compartido.
2. Varios **Consumidores** toman estos datos y los procesan según su tipo específico.

El principal desafío es la sincronización:
- El productor no debe sobrescribir datos no consumidos.
- Los consumidores no deben intentar consumir datos inexistentes.
- Múltiples consumidores deben coordinar el acceso al buffer compartido.

### Características Específicas de esta Implementación

- Un único productor que lee números de un archivo.
- Tres tipos de consumidores:
  - Consumidor de números **pares**
  - Consumidor de números **impares**
  - Consumidor de números **primos**
- Un buffer compartido de tamaño finito.
- Cada consumidor muestra la suma acumulada de los números que va leyendo.
- Una interfaz gráfica que muestra el estado del sistema en tiempo real.

## Cómo Ejecutar el Proyecto

### Paso 1: Compilar los archivos Java

```bash
javac src/GeneradorNumeros.java
javac src/ProductorConsumidor.java
```

### Paso 2: Generar el archivo de números (opcional)

Este paso es necesario la primera vez que ejecuta el programa o si desea generar nuevos números.

```bash
java -cp src GeneradorNumeros
```

Este comando generará un archivo `numeros.txt` con 100 números aleatorios que serán utilizados por el sistema Productor-Consumidor.

### Paso 3: Ejecutar el programa principal

```bash
java -cp src ProductorConsumidor
```

Este comando iniciará la simulación y mostrará la interfaz gráfica.

## Interfaz Gráfica

La interfaz gráfica está dividida en tres secciones principales:

### 1. Buffer Compartido (Parte Superior)

Muestra los números actualmente en el buffer, con colores según su tipo:
- **Azul**: Números pares
- **Verde**: Números impares
- **Rojo**: Números primos

### 2. Registro de Actividad (Parte Central)

Muestra un log de las actividades que realizan el productor y los consumidores en tiempo real, incluyendo:
- Qué números produce el productor
- Qué números consume cada consumidor
- Las sumas acumuladas

### 3. Sumas Acumuladas (Parte Inferior)

Muestra las sumas acumuladas por cada tipo de consumidor.

## Funcionamiento Interno

### Mecanismo de Concurrencia

El proyecto utiliza los siguientes mecanismos de Java para garantizar la sincronización:

- `BlockingQueue`: Para implementar el buffer compartido con control automático de capacidad.
- `synchronized`: Para garantizar acceso exclusivo a secciones críticas.
- `wait()` y `notify()`: Para la comunicación entre hilos cuando el buffer está vacío o lleno.

### Detección de Tipos de Números

- **Par**: El número es divisible entre 2 (`numero % 2 == 0`).
- **Impar**: El número no es divisible entre 2 (`numero % 2 != 0`).
- **Primo**: El número solo es divisible por 1 y por sí mismo.

## Personalización

Puede personalizar varios aspectos del programa modificando las variables en la clase `ProductorConsumidor`:

- `tamanoBuffer`: El tamaño máximo del buffer compartido (por defecto: 10).
- `archivoNumeros`: La ruta del archivo con los números a procesar.
- `numConsumidores`: La cantidad de consumidores (debe ser múltiplo de 3).

En la clase `GeneradorNumeros`, puede modificar:
- `cantidadNumeros`: La cantidad de números aleatorios a generar.
- `minValor` y `maxValor`: El rango de los números aleatorios.

## Solución de Problemas

### El programa no encuentra el archivo `numeros.txt`

Asegúrese de haber ejecutado primero `GeneradorNumeros` para crear el archivo, o cree manualmente un archivo de texto llamado `numeros.txt` con números separados por espacios o saltos de línea.

### Errores de compilación

Verifique que está utilizando Java 21 y que está ejecutando los comandos desde la carpeta raíz del proyecto.

### La interfaz gráfica no se muestra correctamente

Asegúrese de que su entorno de ejecución admite interfaces gráficas Swing. En entornos sin interfaz gráfica (como algunos servidores), el programa no podrá mostrar la ventana.

## Explicación del Código

### `ProductorConsumidor.java`

Contiene cinco clases principales:

1. **Producto**: Representa un número y su clasificación (par, impar, primo).
2. **Productor**: Lee números del archivo y los coloca en el buffer.
3. **Consumidor**: Toma números específicos del buffer según su tipo asignado.
4. **Animacion**: Maneja la interfaz gráfica para visualizar el proceso.
5. **ProductorConsumidor**: Clase principal que orquesta todo el sistema.

### `GeneradorNumeros.java`

Una utilidad para generar un archivo de texto con números aleatorios para probar el sistema.

## Autores

Diego Rivas
