# 📚 GUÍA COMPLETA: DESPLIEGUE EN KUBERNETES

**Fecha:** 23 de febrero de 2026  
**Objetivo:** Desplegar los 3 microservicios (User, Product, Order) en Kubernetes local

---

## 📋 TABLA DE CONTENIDOS

1. [Requisitos Previos](#parte-0-requisitos-previos)
2. [Construir Imágenes Docker](#parte-1-construir-las-imágenes-docker)
3. [Desplegar Bases de Datos](#parte-2-desplegar-bases-de-datos-postgresql)
4. [Despliegue en Kubernetes](#parte-3-despliegue-en-kubernetes-local)
5. [Verificar Despliegue](#parte-4-verificar-el-despliegue)
6. [Conectividad entre Servicios](#parte-5-verificar-conectividad-entre-servicios)
7. [Limpiar Recursos](#parte-6-limpiar-eliminar-todo)
8. [Despliegue en AWS EKS](#parte-7-despliegue-en-aws-eks-opcional)
9. [Debugging y Comandos Útiles](#parte-8-debugging-y-comandos-útiles)

---

## PARTE 0: REQUISITOS PREVIOS

### Herramientas Necesarias

Antes de comenzar, necesitas tener instalado:

```bash
# 1. Docker Desktop (incluye kubectl y un cluster K8s local)
# Descargar desde: https://www.docker.com/products/docker-desktop

# 2. Verificar que todo está instalado
docker --version          # Docker version 24.x o superior
kubectl version --client  # Kubernetes en tu máquina
```

### Habilitar Kubernetes en Docker Desktop

1. Abre **Docker Desktop**
2. Ve a **Settings** (Configuración)
3. Selecciona **Kubernetes** en el menú izquierdo
4. Marca la checkbox **"Enable Kubernetes"**
5. Espera a que inicie (2-3 minutos)
6. En terminal, verifica:

```bash
kubectl get nodes
# Deberías ver: docker-desktop    Ready    master
```

---

## PARTE 1: CONSTRUIR LAS IMÁGENES DOCKER

Necesitas crear imágenes Docker para los 3 servicios. Ejecuta estos comandos desde la raíz del proyecto:

### 1.1 Compilar los microservicios

Primero, compila cada servicio para generar el archivo JAR:

```bash
# User Service
cd user-service
mvn clean package -DskipTests

# Product Service
cd ../product-service
mvn clean package -DskipTests

# Order Service
cd ../order-service
mvn clean package -DskipTests

# Vuelve a la raíz
cd ..
```

### 1.2 Construir las imágenes Docker

```bash
# 1. Construir imagen del User Service
docker build -t user-service:1.0 ./user-service

# 2. Construir imagen del Product Service
docker build -t product-service:1.0 ./product-service

# 3. Construir imagen del Order Service
docker build -t order-service:1.0 ./order-service

# Verificar que se crearon
docker images | grep -E "user-service|product-service|order-service"
```

**Salida esperada:**
```
user-service       1.0       abc123def456   1 minute ago   ...
product-service    1.0       def456ghi789   1 minute ago   ...
order-service      1.0       ghi789jkl012   1 minute ago   ...
```

---

## PARTE 2: DESPLEGAR BASES DE DATOS (PostgreSQL)

Las bases de datos se ejecutarán en contenedores Docker en tu máquina local y serán accesibles desde Kubernetes a través de `host.docker.internal`.

### 2.1 Iniciar bases de datos con Docker Compose

```bash
# Desde la raíz del proyecto
docker-compose up -d
```

### 2.2 Verificar que están corriendo

```bash
docker-compose ps
```

**Salida esperada:**
```
NAME                COMMAND             STATUS
postgres-user       docker-entrypoint   Up 2 minutes
postgres-product    docker-entrypoint   Up 2 minutes
postgres-order      docker-entrypoint   Up 2 minutes
```

### 2.3 Bases de datos disponibles

| Servicio | Host | Puerto | Base de Datos | Usuario | Contraseña |
|----------|------|--------|---------------|---------|-----------|
| User Service | localhost | 5434 | userdb | postgres | postgres |
| Product Service | localhost | 5433 | productdb | postgres | postgres |
| Order Service | localhost | 5435 | orderdb | postgres | postgres |

---

## PARTE 3: DESPLIEGUE EN KUBERNETES LOCAL

Ahora despliegaremos los servicios en Kubernetes. Cada servicio tiene su propio namespace.

### 3.1 Desplegar User Service

```bash
# Crear el namespace (agrupa recursos)
kubectl apply -f user-service/k8s/00-namespace.yaml

# Crear ConfigMap (variables de entorno NO sensibles)
kubectl apply -f user-service/k8s/01-configmap.yaml

# Crear Secret (credenciales - sensibles)
kubectl apply -f user-service/k8s/02-secret.yaml

# Desplegar la aplicación (Deployment)
kubectl apply -f user-service/k8s/03-deployment.yaml

# Crear el servicio (NodePort para acceso local)
kubectl apply -f user-service/k8s/04-service.yaml

# Verificar que se creó todo
kubectl get pods -n user-service
kubectl get svc -n user-service
```

### 3.2 Desplegar Product Service

```bash
kubectl apply -f product-service/k8s/00-namespace.yaml
kubectl apply -f product-service/k8s/01-configmap.yaml
kubectl apply -f product-service/k8s/02-secret.yaml
kubectl apply -f product-service/k8s/03-deployment.yaml
kubectl apply -f product-service/k8s/04-service.yaml

# Verificar
kubectl get pods -n product-service
kubectl get svc -n product-service
```

### 3.3 Desplegar Order Service

```bash
kubectl apply -f order-service/k8s/00-namespace.yaml
kubectl apply -f order-service/k8s/01-configmap.yaml
kubectl apply -f order-service/k8s/02-secret.yaml
kubectl apply -f order-service/k8s/03-deployment.yaml
kubectl apply -f order-service/k8s/04-service.yaml

# Verificar
kubectl get pods -n order-service
kubectl get svc -n order-service
```

---

## PARTE 4: VERIFICAR EL DESPLIEGUE

### 4.1 Ver todos los pods

```bash
# Ver todos los pods en todos los namespaces
kubectl get pods -A

# Salida esperada:
# NAMESPACE          NAME                              READY   STATUS    RESTARTS   AGE
# user-service       user-service-abc123-xyz789        1/1     Running   0          2m
# product-service    product-service-def456-uvw012     1/1     Running   0          2m
# order-service      order-service-ghi789-rst345       1/1     Running   0          2m
```

### 4.2 Ver los servicios

```bash
# Ver todos los servicios
kubectl get svc -A

# Obtener el puerto asignado (NodePort)
kubectl get svc -n user-service
kubectl get svc -n product-service
kubectl get svc -n order-service
```

**Nota:** En la columna "PORT(S)" verás algo como `8081:30081/TCP`
- `8081` = puerto interno del contenedor
- `30081` = puerto NodePort (para acceso externo)

### 4.3 Ver logs de los pods

```bash
# Ver logs del User Service
kubectl logs -n user-service -l app=user-service

# Ver logs del Product Service
kubectl logs -n product-service -l app=product-service

# Ver logs del Order Service
kubectl logs -n order-service -l app=order-service

# Ver logs en tiempo real
kubectl logs -f -n user-service -l app=user-service
```

### 4.4 Acceder a las aplicaciones

Una vez que los pods están "Running", puedes acceder:

```bash
# User Service (generalmente NodePort 30081)
http://localhost:30081/swagger-ui.html
http://localhost:30081/api/users

# Product Service (generalmente NodePort 30082)
http://localhost:30082/swagger-ui.html
http://localhost:30082/api/products

# Order Service (generalmente NodePort 30083)
http://localhost:30083/swagger-ui.html
http://localhost:30083/api/orders
```

---

## PARTE 5: VERIFICAR CONECTIVIDAD ENTRE SERVICIOS

Los servicios deben comunicarse entre sí dentro del cluster.

### 5.1 Entrar a un pod para pruebas

```bash
# Obtener el nombre del pod
kubectl get pods -n user-service

# Acceder al bash del pod (reemplaza <pod-name>)
kubectl exec -it user-service-abc123-xyz789 -n user-service -- /bin/bash
```

### 5.2 Probar conectividad con otros servicios

Dentro del contenedor, ejecuta:

```bash
# Desde User Service, probar conectividad con Product Service
curl http://product-service:8082/api/products

# Desde Order Service, probar conectividad con Product Service
curl http://product-service:8082/api/products

# Los servicios se comunican usando:
# http://<service-name>.<namespace>.svc.cluster.local:<puerto>
# Ejemplo: http://product-service.product-service.svc.cluster.local:8082
```

### 5.3 Probar comunicación inter-namespace

```bash
# Desde un pod en order-service, llamar a product-service
curl http://product-service.product-service.svc.cluster.local:8082/api/products

# Formato:
# http://<service-name>.<namespace>.svc.cluster.local:<puerto>/<ruta>
```

---

## PARTE 6: LIMPIAR (ELIMINAR TODO)

### 6.1 Eliminar servicios de Kubernetes

```bash
# Eliminar namespaces (elimina todo dentro automaticamente)
kubectl delete namespace user-service
kubectl delete namespace product-service
kubectl delete namespace order-service

# Alternativamente, eliminar recurso por recurso:
# kubectl delete -f user-service/k8s/
# kubectl delete -f product-service/k8s/
# kubectl delete -f order-service/k8s/
```

### 6.2 Detener bases de datos

```bash
# Detener y eliminar contenedores
docker-compose down

# Si quieres preservar volúmenes (datos):
docker-compose down --volumes
```

### 6.3 Eliminar imágenes Docker (opcional)

```bash
docker rmi user-service:1.0 product-service:1.0 order-service:1.0
```

---

## PARTE 7: DESPLIEGUE EN AWS EKS (OPCIONAL)

Si quieres desplegar en la nube en lugar de localmente, sigue estos pasos:

### 7.1 Instalar herramientas

```bash
# Windows (con Chocolatey)
choco install awscli
choco install eksctl

# macOS
brew install awscli
brew install eksctl

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

### 7.2 Configurar AWS Credentials

```bash
# 1. Crear usuario IAM en AWS Console
# Ve a: https://console.aws.amazon.com/iam/home
# Crea un usuariocon permisos de administrador
# Guarda: Access Key ID y Secret Access Key

# 2. Configurar credenciales localmente
aws configure

# Te pedirá:
# AWS Access Key ID: [pega tu Access Key]
# AWS Secret Access Key: [pega tu Secret Access Key]
# Default region name: us-east-1
# Default output format: json
```

### 7.3 Crear cluster EKS

```bash
eksctl create cluster \
  --name microservicios-cluster \
  --version 1.28 \
  --region us-east-1 \
  --nodegroup-name workers \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4

# Espera 15-20 minutos a que se cree
```

### 7.4 Verificar conexión

```bash
# Verificar que kubectl está conectado a EKS
kubectl get nodes

# Deberías ver 2 nodos EC2
```

### 7.5 Desplegar en EKS

```bash
# Los mismos comandos de kubectl funcionan
kubectl apply -f user-service/k8s/
kubectl apply -f product-service/k8s/
kubectl apply -f order-service/k8s/

# Verificar
kubectl get pods -A
```

### 7.6 Acceder a los servicios en EKS

```bash
# Para servicios con LoadBalancer (en lugar de NodePort):
# AWS crea automáticamente un Load Balancer y te da una URL

kubectl get svc -A
# Busca en la columna "EXTERNAL-IP"

# Ejemplo: http://my-service-123.us-east-1.elb.amazonaws.com:8081
```

### 7.7 Limpiar cluster EKS

```bash
# Eliminar el cluster (cuidado, no se puede deshacer!)
eksctl delete cluster --name microservicios-cluster --region us-east-1

# Espera 10-15 minutos
```

---

## PARTE 8: DEBUGGING Y COMANDOS ÚTILES

### 8.1 Inspeccionar pods

```bash
# Ver descripción detallada de un pod
kubectl describe pod <pod-name> -n <namespace>

# Ejemplo:
kubectl describe pod user-service-abc123 -n user-service

# Ver eventos del cluster (problemas)
kubectl get events -A
kubectl get events -n user-service
```

### 8.2 Acceder shell del contenedor

```bash
# Entrar al bash de un contenedor
kubectl exec -it <pod-name> -n <namespace> -- /bin/bash

# Ejemplo:
kubectl exec -it user-service-abc123 -n user-service -- /bin/bash

# Una vez dentro, puedes:
# - Ver archivos: ls -la
# - Ver logs de la app: tail -f /var/log/app.log
# - Probar conectividad: curl http://producto-service:8082/api/products
```

### 8.3 Ver logs

```bash
# Ver logs recientes
kubectl logs <pod-name> -n <namespace>

# Ver últimas 100 líneas
kubectl logs <pod-name> -n <namespace> --tail=100

# Ver logs en tiempo real
kubectl logs -f <pod-name> -n <namespace>

# Ver logs de todos los pods de un label
kubectl logs -l app=user-service -n user-service --all-containers=true
```

### 8.4 Port forward (acceso local)

```bash
# Acceder a un servicio como si estuviera en localhost
kubectl port-forward svc/user-service 8081:8081 -n user-service

# Ahora puedes acceder: http://localhost:8081

# Con pod específico
kubectl port-forward pod/user-service-abc123 8081:8081 -n user-service
```

### 8.5 Ejecutar comandos en el contenedor

```bash
# Sin entrar al bash, ejecutar comando directo
kubectl exec <pod-name> -n <namespace> -- cat /etc/hostname

# Ejemplo: ver si la app está corriendo
kubectl exec user-service-abc123 -n user-service -- ps aux | grep java
```

### 8.6 Ver variables de entorno del pod

```bash
# Listar todas las variables de entorno
kubectl exec <pod-name> -n <namespace> -- env | sort

# Filtrar por una variable específica
kubectl exec <pod-name> -n <namespace> -- env | grep DB_
```

### 8.7 Verificar recursos (CPU, memoria)

```bash
# Ver uso de recursos de pods
kubectl top nodes
kubectl top pods -A

# Ver límites configurados
kubectl describe pod <pod-name> -n <namespace> | grep -A 5 "Limits\|Requests"
```

---

## TROUBLESHOOTING: PROBLEMAS COMUNES

### Problema: "ImagePullBackOff" o "ErrImagePull"

```bash
# Causa: Kubernetes no encuentra la imagen Docker
# Solución:

# 1. Verifica que compilaste la imagen
docker images | grep user-service

# 2. Verifica imagePullPolicy en el deployment
kubectl describe pod <pod-name> -n <namespace>

# 3. Asegúrate de que imagePullPolicy: Never está en el yaml
# (porque la imagen es local, no de un registry)
```

### Problema: "CrashLoopBackOff"

```bash
# Causa: El contenedor se inicia pero inmediatamente crashea
# Solución:

# 1. Ver los logs
kubectl logs <pod-name> -n <namespace>

# 2. Ver descripción del pod
kubectl describe pod <pod-name> -n <namespace>

# 3. Verificar que las bases de datos están corriendo
docker-compose ps

# 4. Verificar conectividad desde el pod
kubectl exec -it <pod-name> -n <namespace> -- /bin/bash
# Dentro: curl http://host.docker.internal:5434
```

### Problema: "Pending"

```bash
# Causa: Pod esperando recursos o volúmenes
# Solución:

# 1. Ver descripción completa
kubectl describe pod <pod-name> -n <namespace>

# 2. Ver eventos
kubectl get events -n <namespace>

# 3. Verificar que el nodo tiene recursos
kubectl top nodes
kubectl describe node docker-desktop
```

### Problema: No puedo acceder al servicio desde localhost

```bash
# Causa: Puerto NodePort no está correcto
# Solución:

# 1. Ver el puerto NodePort asignado
kubectl get svc -n <namespace>

# 2. Verificar que el pod está en Running
kubectl get pods -n <namespace>

# 3. Ver logs del pod
kubectl logs <pod-name> -n <namespace>

# 4. Probar port-forward como alternativa
kubectl port-forward svc/<service-name> 8081:8081 -n <namespace>
# Accede a: http://localhost:8081
```

---

## REFERENCIA RÁPIDA

### Estructura de archivos K8s

```
user-service/k8s/
├── 00-namespace.yaml      # Crear namespace
├── 01-configmap.yaml      # Variables de entorno
├── 02-secret.yaml         # Credenciales
├── 03-deployment.yaml     # Despliegue (pods)
└── 04-service.yaml        # Exposición de servicio
```

### Orden de despliegue recomendado

```
1. Compilar servicios        (mvn clean package)
2. Construir imágenes        (docker build)
3. Iniciar bases de datos    (docker-compose up)
4. Desplegar en K8s:
   a. User Service
   b. Product Service
   c. Order Service
5. Verificar pods están "Running"
6. Probar conectividad
```

### Puertos por defecto

| Servicio | Puerto Interno | NodePort |
|----------|---|---|
| User Service | 8081 | 30081 |
| Product Service | 8082 | 30082 |
| Order Service | 8083 | 30083 |

---

## RECURSOS ADICIONALES

- [Documentación oficial de Kubernetes](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)

---

**Última actualización:** 23 de febrero de 2026





-- Recompilar
cd order-service
mvn clean package -DskipTests

cd ..
docker build -t order-service:1.0 ./order-service

kubectl rollout restart deployment/order-service -n order-service






volver a entrar
docker-compose up -d
kubectl get pods -A