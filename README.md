# DistributedPatterns - Almacenamiento Distribuido de Clave-Valor con Arquitectura Resiliente

## Descripción General

DistributedPatterns es un sistema distribuido resiliente que implementa una estructura de datos replicada para almacenar pares clave-valor en múltiples nodos. Este proyecto incluye una configuración tolerante a fallos donde cada nodo en la red mantiene un estado sincronizado. Cuando nuevos nodos se unen o vuelven al clúster, reciben el estado completo actual, asegurando la consistencia entre nodos. Una interfaz de cliente web construida con JavaScript asíncrono permite la interacción en tiempo real, habilitando a los clientes para registrar y ver sus mensajes con marcas de tiempo. El sistema incorpora un balanceador de carga y un descubrimiento de servicios para gestionar dinámicamente la disponibilidad de los nodos.

---

## Arquitectura

### Componentes Clave

1. **Clúster Principal (`ChatCluster`)**: 
   - Almacena pares clave-valor de mensajes con marcas de tiempo en un `HashMap`.
   - Implementa `getState` y `setState` para transferir el estado actual cuando nodos se unen o vuelven al clúster.
   - El `HashMap` de cada nodo se replica automáticamente en el clúster, asegurando tolerancia a fallos.

2. **Canal de Heartbeat (`HeartbeatChannel`)**:
   - Dedicado al seguimiento de nodos activos mediante mensajes de heartbeat (latidos).
   - Cada nodo envía un mensaje de "heartbeat" cada segundo, permitiendo a otros nodos monitorear su actividad.
   - Si un nodo deja de enviar heartbeats, se considera inactivo. Cuando vuelve, recibe el estado actual.


### Proceso de Flujo

1. **Solicitudes del Cliente**: Los clientes envían mensajes de forma asíncrona a través de la interfaz web.
2. **Balanceador de Carga**: El balanceador de carga basado en Spring distribuye las solicitudes de los clientes entre los nodos de backend.
3. **Nodos de Backend**: Cada nodo de backend forma parte de `ChatCluster`, donde almacena mensajes en un `HashMap` replicado y mantiene sincronizados los datos entre nodos.
4. **Monitoreo de Heartbeat**: El `HeartbeatChannel` rastrea la salud de los nodos, marcando los nodos como inactivos si no reciben un heartbeat.

---

## Proceso de Configuración

### Prerrequisitos

- **Java 17**
- **Apache Maven**
- **NetBeans (opcional)** - Recomendado si usas NetBeans para desarrollo.
- **Docker (opcional)** - Si planeas desplegar el sistema en contenedores.

### Clonar el Repositorio

```bash
git clone https://github.com/SebasBarrera/DISTRIBUTED-ARCHITECTURE.git
cd DistributedPatterns
```
### Compilar el Proyecto
```bash
mvn clean install
```
## Pasos de Despliegue

### 1. Despliegue Local

#### Ejecutar la Aplicación Localmente

```
mvn exec:java -Dexec.mainClass="com.eci.aygo.lab2.distributedpatterns.SimpleChat"
```

Repite el comando anterior en varias terminales para simular múltiples nodos uniéndose al clúster.

### 2. Despliegue en Contenedores con Docker

#### Dockerfile

```
FROM openjdk:17-jdk
COPY target/DistributedPatterns-1.0-SNAPSHOT.jar /DistributedPatterns.jar
ENTRYPOINT ["java", "-jar", "/DistributedPatterns.jar"]
```

#### Construcción de la Imagen Docker

```
docker build -t distributedpatterns .
```

#### Ejecutar el Contenedor Docker

```
docker run -d --name nodo1 distributedpatterns
docker run -d --name nodo2 distributedpatterns
```

### 3. Despliegue en Kubernetes (Opcional)

Crea un archivo de despliegue en Kubernetes (`distributedpatterns-deployment.yaml`):

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: distributedpatterns
spec:
  replicas: 3
  selector:
    matchLabels:
      app: distributedpatterns
  template:
    metadata:
      labels:
        app: distributedpatterns
    spec:
      containers:
      - name: distributedpatterns
        image: distributedpatterns:latest
        ports:
        - containerPort: 8080
```

Aplica el despliegue:

```
kubectl apply -f distributedpatterns-deployment.yaml
```

---

## Pruebas de la Aplicación

### 1. Pruebas Iniciales con un Solo Nodo

Para verificar la configuración inicial, inicia una sola instancia de la aplicación:

```
mvn exec:java -Dexec.mainClass="com.eci.aygo.lab2.distributedpatterns.SimpleChat"
```

- Envía mensajes y observa los logs para asegurar que se almacenan en el `HashMap` con marcas de tiempo.
- Prueba la funcionalidad de `getState` y `setState` añadiendo y eliminando nodos.

### 2. Pruebas con Múltiples Nodos

Inicia múltiples nodos para probar la replicación y transferencia de estado:

1. Ejecuta varias instancias (o contenedores si usas Docker) y confirma que los mensajes se replican.
2. Prueba la tolerancia a fallos deteniendo un nodo y reincorporándolo, asegurando que recupere el último estado.

### 3. Pruebas del Mecanismo de Heartbeat

- Observa los mensajes de heartbeat cada segundo en los logs.
- Verifica que los nodos detecten la actividad de otros nodos y marquen los que falten.


---

## Logs y Solución de Problemas

### Logs de Ejemplo

Observa los logs en tiempo real para monitorear la actividad del clúster. A continuación, un ejemplo de logs típicos:

```
INFO: local_addr: DESKTOP-OED72RM-12345, cluster=ChatCluster, physical address=192.168.0.10:57899
** view: [DESKTOP-OED72RM-12345|3] (2) [DESKTOP-OED72RM-12345, DESKTOP-OED72RM-67890]
Member: DESKTOP-OED72RM-12345
Member: DESKTOP-OED72RM-67890
INFO: Received message: [timestamp] User1: ¡Hola, mundo!
```

### Advertencias Comunes

- **JGRP000012: discarded message from different cluster**: Ocurre cuando mensajes de clústeres diferentes (e.g., `HeartbeatChannel` vs. `ChatCluster`) se reciben. Asegúrate de que cada nodo envía mensajes solo a su clúster designado.
- **JGRP000011: dropped message batch from non-member**: Indica un mensaje de un nodo no miembro. Verifica si algún nodo tiene versiones o configuraciones inconsistentes.

---

## Recursos Adicionales

- **Documentación de JGroups**: [Documentación Oficial de JGroups](https://www.jgroups.org/)
- **Framework Spring**: [Documentación de Spring](https://spring.io/projects/spring-framework)
- **Documentación de Docker**: [Documentación Oficial de Docker](https://docs.docker.com/)
- **Documentación de Kubernetes**: [Documentación Oficial de Kubernetes](https://kubernetes.io/docs/)

---
