# Algoritmo-producto-consumidor
Este repositorio implementa un algoritmo clásico de sincronización de sistemas operativos conocido como el problema del productor-consumidor.

## Proyecto Productor-Consumidor
Este proyecto implementa el algoritmo clásico de Productor-Consumidor utilizando Java, con una interfaz gráfica para visualizar el proceso en tiempo real.

## Requisitos Previos
### Versión de Java
Este proyecto requiere Java 21. Para verificar su versión actual, ejecute:

```
java --version
```

Si la versión es correcta, verá un resultado similar a:

```
openjdk 21.0.3 2024-04-16 LTS
OpenJDK Runtime Environment Temurin-21.0.3+9 (build 21.0.3+9-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.3+9 (build 21.0.3+9-LTS, mixed mode, sharing)
```

Si no tiene la versión correcta, descargue e instale Java 21 desde el sitio oficial de Oracle o utilice OpenJDK.

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

- Un Productor genera datos (en este caso, lee números de un archivo) y los coloca en un buffer compartido.
- Varios Consumidores toman estos datos y los procesan según su tipo específico.

El principal desafío es la sincronización:

1. El productor no debe sobrescribir datos no consumidos.
2. Los consumidores no deben intentar consumir datos inexistentes.
3. Múltiples consumidores deben coordinar el acceso al buffer compartido.

## Características Específicas de esta Implementación
- Un único productor que lee números de un archivo.
- Tres tipos de consumidores:
  - Consumidor de números pares
  - Consumidor de números impares
  - Consumidor de números primos
- Un buffer compartido de tamaño finito.
- Cada consumidor muestra la suma acumulada de los números que va leyendo.
- Una interfaz gráfica que muestra el estado del sistema en tiempo real.
- Panel de control con opciones para pausar, reanudar y reiniciar la simulación.
- Panel de estadísticas que muestra información detallada del proceso.

## Cómo Ejecutar el Proyecto
### Paso 1: Compilar los archivos Java
```
javac src/GeneradorNumeros.java
javac src/ProductorConsumidor.java
```

### Paso 2: Generar el archivo de números (opcional)
Este paso es necesario la primera vez que ejecuta el programa o si desea generar nuevos números.

```
java -cp src GeneradorNumeros
```

Este comando generará un archivo `numeros.txt` con 100 números aleatorios que serán utilizados por el sistema Productor-Consumidor.

### Paso 3: Ejecutar el programa principal
```
java -cp src ProductorConsumidor
```

Este comando iniciará la simulación y mostrará la interfaz gráfica.

## Interfaz Gráfica
La interfaz gráfica está dividida en varias secciones principales:

### 1. Buffer Compartido (Parte Superior)
Muestra los números actualmente en el buffer, con colores según su tipo:

- Azul: Números pares
- Verde: Números impares
- Rojo: Números primos

Cada número se muestra en un panel individual con su clasificación.

### 2. Registro de Actividad (Parte Central)
Muestra un log de las actividades que realizan el productor y los consumidores en tiempo real, incluyendo:

- Qué números produce el productor
- Qué números consume cada consumidor
- Las sumas acumuladas
- Marca de tiempo para cada acción

### 3. Sumas Acumuladas (Parte Inferior)
Muestra las sumas acumuladas por cada tipo de consumidor, con paneles coloreados para identificar cada tipo.

### 4. Panel de Control (Parte Derecha)
Proporciona controles para:

- Pausar la simulación
- Reanudar una simulación pausada
- Reiniciar completamente la simulación

### 5. Panel de Estadísticas (Parte Derecha)
Muestra información detallada sobre el proceso:

- Total de números producidos
- Total de números consumidos
- Cantidad de pares, impares y primos consumidos
- Porcentaje de utilización del buffer

## Funcionamiento Interno
### Mecanismo de Concurrencia
El proyecto utiliza los siguientes mecanismos de Java para garantizar la sincronización:

- `BlockingQueue`: Para implementar el buffer compartido con control automático de capacidad.
- `synchronized`: Para garantizar acceso exclusivo a secciones críticas.
- `wait()` y `notify()/notifyAll()`: Para la comunicación entre hilos cuando el buffer está vacío o lleno.

### Detección de Tipos de Números
- Par: El número es divisible entre 2 (`numero % 2 == 0`).
- Impar: El número no es divisible entre 2 (`numero % 2 != 0`).
- Primo: El número solo es divisible por 1 y por sí mismo.

### Pausado y Reanudación
El sistema implementa un mecanismo que permite pausar toda la simulación y reanudarla posteriormente:

- Cuando se pausa, todos los hilos entran en espera usando `wait()`
- Al reanudar, se notifica a todos los hilos con `notifyAll()`

## Personalización
Puede personalizar varios aspectos del programa modificando las variables en la clase `ProductorConsumidor`:

- `tamanoBuffer`: El tamaño máximo del buffer compartido (por defecto: 10).
- `archivoNumeros`: La ruta del archivo con los números a procesar.
- `numConsumidores`: La cantidad de consumidores (debe ser múltiplo de 3).

En la clase `GeneradorNumeros`, puede modificar:

- `cantidadNumeros`: La cantidad de números aleatorios a generar.
- `minValor` y `maxValor`: El rango de los números aleatorios.

También puede ajustar los tiempos de espera (valores de `Thread.sleep()`) para que la simulación sea más rápida o más lenta.

## Solución de Problemas
### El programa no encuentra el archivo numeros.txt
Asegúrese de haber ejecutado primero `GeneradorNumeros` para crear el archivo, o cree manualmente un archivo de texto llamado `numeros.txt` con números separados por espacios o saltos de línea.

### Errores de compilación
Verifique que está utilizando Java 21 y que está ejecutando los comandos desde la carpeta raíz del proyecto.

### La interfaz gráfica no se muestra correctamente
Asegúrese de que su entorno de ejecución admite interfaces gráficas Swing. En entornos sin interfaz gráfica (como algunos servidores), el programa no podrá mostrar la ventana.

### Los íconos no aparecen
Los íconos requieren una carpeta `images` con las imágenes necesarias (`wait.png`, `producing.png`, `consuming.png`, `done.png`). Si estas imágenes no existen, el programa funcionará normalmente sin mostrar íconos.

### El botón Pausar no funciona correctamente
Verifique que está utilizando la versión más reciente del código que incluye la funcionalidad de control completa.

## Explicación del Código
### ProductorConsumidor.java
Contiene cinco clases principales:

- `Producto`: Representa un número y su clasificación (par, impar, primo).
- `Productor`: Lee números del archivo y los coloca en el buffer.
- `Consumidor`: Toma números específicos del buffer según su tipo asignado.
- `Animacion`: Maneja la interfaz gráfica para visualizar el proceso.
- `ProductorConsumidor`: Clase principal que orquesta todo el sistema.

### GeneradorNumeros.java
Una utilidad para generar un archivo de texto con números aleatorios para probar el sistema.

## Autores
Diego Rivas