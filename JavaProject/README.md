# PoliMarket · Java 21

Prototipo académico de la Unidad 2, **Reutilización de software**, del módulo Temas avanzados en diseño de software. Integra Recursos Humanos, Ventas, Bodega, Proveedores y Entregas mediante cinco componentes reutilizables. Una consola Java y una aplicación web JavaScript consumen la misma API HTTP; cada cliente permite ejecutar los cinco requisitos.

No requiere Maven, bibliotecas de ejecución, servicios externos ni base de datos. Los datos personales, empresas, direcciones y tokens son ficticios. **No es un sistema de producción.** Los PDF originales no se modificaron.

## 1. Inicio rápido

Abra una terminal en esta carpeta, la que contiene este README.

macOS / Linux:

```sh
sh compilar.sh
java -cp build/server polimarket.Server
```

Windows, desde CMD:

```bat
compilar.bat
java -cp build/server polimarket.Server
```

En PowerShell utilice `./compilar.bat`. Abra **http://127.0.0.1:8080**. Mensaje esperado:

```text
PoliMarket listo en http://127.0.0.1:8080 | Estado: data/polimarket.ser
```

Mantenga la terminal abierta. Detenga el servidor con **Ctrl+C**. Los clientes se ejecutan desde otra terminal o el navegador. El servidor escucha exclusivamente en `127.0.0.1`.

## 2. Requisitos previos

- JDK **21**, con `java` y `javac` en `PATH`. Instale una distribución de JDK 21 mediante su proveedor o gestor de paquetes habitual; un JRE sin compilador no basta. Configure `JAVA_HOME` hacia ese JDK y añada su directorio `bin` a `PATH` si el instalador no lo hizo.
- Verifique con `java -version` y `javac -version`: ambos deben indicar 21. Los scripts rechazan otros compiladores y usan `--release 21` y UTF-8.
- Navegador moderno con módulos ES, Fetch y `AbortSignal.timeout`.
- Opcional, solo para pruebas: Node.js **22 o posterior**. No necesita `npm install`.
- Opcional, solo para regenerar diagramas: PlantUML 1.2025.2 (JAR). La aplicación y las pruebas no lo utilizan.

El código y los scripts se prepararon para macOS, Linux y Windows. La ejecución comprobada en este entorno fue en macOS con JDK 21.0.9; no se afirma ejecución en los otros dos sistemas.

## 3. Arquitectura, estructura y requisitos

```text
sistema/src/polimarket/
  Domain.java                  Diez registros inmutables y Rol
  Store.java                   Estado, transacciones, bloqueo y persistencia
  Server.java                  Arranque e inyección por constructor
  AdaptadorHTTP.java           Rutas, formularios y respuestas
  Json.java                    Serialización JSON de salida
  BusinessException.java       Errores con estado HTTP
  contracts/Contracts.java     IRRHH, IBodega, IProveedores, IVentas, IEntregas
  business/                    Cinco componentes de negocio
clientes/java/ClienteConsola.java
clientes/web/                  index.html, style.css, api.mjs, app.mjs
pruebas/                       Integración HTTP y pruebas de Store
pruebas/ejecutar.sh / .bat      Compilación y ejecución de pruebas
compilar.sh / compilar.bat      Compilación de servidor y consola
build/                         Binarios generados; ignorados por Git
data/                          Estado local generado; ignorado por Git
diagramas/                     PlantUML, PNG, BMP y exportador Java
evidencias/                    Resultados de ejecución verificables
```

| RF | Funcionalidad | Negocio reutilizado | Clientes |
|---|---|---|---|
| RF-01 | Autorizar y revocar vendedores | RecursosHumanos / IRRHH | Web y consola |
| RF-02 | Consultar clientes, productos y existencias | Ventas, Bodega, RecursosHumanos | Web y consola |
| RF-03 | Consultar proveedores y registrar adquisiciones | Proveedores, Bodega, RecursosHumanos | Web y consola |
| RF-04 | Registrar venta y reservar unidades | Ventas, Bodega, RecursosHumanos | Web y consola |
| RF-05 | Consultar pendientes y despachar | Entregas, Ventas, Bodega, RecursosHumanos | Web y consola |

Ventas y Proveedores reciben `IBodega`; Entregas recibe `IVentas` e `IBodega`. Todos aplican autorización mediante `IRRHH`. `Server` construye e inyecta estas dependencias. Las operaciones internas `reservar`, `ingresar`, `sacar` y `obtener` exigen una transacción de Store; no tienen endpoint público. Cada coordinador valida el actor antes de usarlas. Los clientes no importan componentes del servidor ni leen el archivo de estado.

La fórmula `disponibles = fisicas - reservadas` está únicamente en `Existencia.disponibles()`, utilizada y expuesta por Bodega. Ningún cliente la recalcula. Una compra aumenta físicas; una venta aumenta reservadas; el despacho reduce ambas. Se mantiene `1000000 >= fisicas >= reservadas >= 0`.

## 4. Compilación

`compilar.sh` y `compilar.bat` compilan primero servidor y después consola, deteniéndose ante errores. No descargan dependencias. Las rutas admiten espacios.

Para compilar únicamente la consola, después de crear el directorio `build/client`:

```sh
javac --release 21 -encoding UTF-8 -d build/client clientes/java/ClienteConsola.java
```

No requiere el classpath del servidor. Los artefactos de compilación no forman parte del código fuente.

## 5. Identidades y uso web

| Token ficticio | Usuario | Rol | Estado inicial |
|---|---|---|---|
| `demo-rrhh` | rh1 | RRHH | Autorizado |
| `demo-vendedor` | v1 | VENDEDOR | **Inactivo** |
| `demo-bodega` | b1 | BODEGA | Autorizado |
| `demo-entregas` | e1 | ENTREGAS | Autorizado |

Existe también v2 autorizado en el catálogo de vendedores; no tiene un token adicional. El identificador y el rol del actor se obtienen del token en el servidor, nunca de campos suministrados por el cliente. Una revocación de v1 tiene efecto en su siguiente operación usando el mismo token.

En el navegador:

1. Seleccione **Vendedor v1** y pulse **Ver catálogo**: debe mostrar HTTP 403 al inicio.
2. Seleccione **Recursos Humanos**, consulte vendedores y autorice v1.
3. Seleccione **Vendedor v1** y consulte catálogo y existencias.
4. Seleccione **Responsable de Bodega**, consulte proveedores y registre la compra propuesta: pr1, p1, 3, COMPRA-DEMO-1.
5. Seleccione **Vendedor v1** y registre c1 con `p1:2,p2:1`.
6. El ID de venta se copia al formulario de despacho. Seleccione **Responsable de Entregas**, consulte pendientes y confirme el despacho.
7. Repita el despacho: debe conservar su ID y no volver a descontar. Consulte existencias.
8. Como RRHH, revoque v1 y compruebe que vuelve a recibir 403.

El panel de respuesta presenta código HTTP y JSON o un error. Todos los botones se deshabilitan mientras hay una solicitud. Los valores variables se muestran mediante `textContent`. Los roles permanecen seleccionables para demostrar que el servidor rechaza acciones incorrectas. No abra `index.html` como archivo local: use la URL del servidor.

## 6. Cliente de consola: comandos completos

Sintaxis: `ClienteConsola token GET|POST /api/ruta [campo=valor ...]`. Muestra `HTTP nnn` y JSON. Código de salida: 0 éxito, 2 rechazo HTTP, 1 error de argumentos o conexión. Conexión: máximo 5 segundos; solicitud: máximo 10 segundos.

Consultar y autorizar:

```sh
java -cp build/client ClienteConsola demo-rrhh GET /api/vendedores
java -cp build/client ClienteConsola demo-rrhh POST /api/autorizacion vendedor=v1 activo=true
```

Consultar catálogo, existencias y proveedores:

```sh
java -cp build/client ClienteConsola demo-vendedor GET /api/catalogo
java -cp build/client ClienteConsola demo-bodega GET /api/existencias
java -cp build/client ClienteConsola demo-bodega GET /api/proveedores
```

Comprar y vender:

```sh
java -cp build/client ClienteConsola demo-bodega POST /api/compras proveedor=pr1 producto=p1 cantidad=3 referencia=CONSOLA-1
java -cp build/client ClienteConsola demo-vendedor POST /api/ventas cliente=c1 lineas=p1:2,p2:1
```

Consultar pendientes y despachar. **Sustituya `ID_RECIBIDO` por el valor `id` de la venta recién creada**, no por el ID de compra:

```sh
java -cp build/client ClienteConsola demo-entregas GET /api/entregas/pendientes
java -cp build/client ClienteConsola demo-entregas POST /api/entregas venta=ID_RECIBIDO
java -cp build/client ClienteConsola demo-bodega GET /api/existencias
```

Revocar:

```sh
java -cp build/client ClienteConsola demo-rrhh POST /api/autorizacion vendedor=v1 activo=false
java -cp build/client ClienteConsola demo-vendedor GET /api/catalogo
```

El último comando debe devolver 403. Estos comandos también funcionan en CMD. En PowerShell puede entrecomillar `"lineas=p1:2,p2:1"` para evitar cualquier interpretación de la coma.

## 7. Demostración completa desde datos iniciales

Use un archivo nuevo o reinicie de forma segura los datos según la sección de persistencia. No ejecute consecutivamente los recorridos web y consola esperando otra vez los saldos iniciales: ambos comparten estado.

| Paso | Acción | p1: físicas / reservadas / disponibles | p2: físicas / reservadas / disponibles |
|---|---|---|---|
| Inicio | v1 inactivo; catálogo 403 | 10 / 0 / 10 | 5 / 0 / 5 |
| 1 | RRHH autoriza v1; catálogo 200 | 10 / 0 / 10 | 5 / 0 / 5 |
| 2 | Compra pr1, p1, cantidad 3, referencia CONSOLA-1 | 13 / 0 / 13 | 5 / 0 / 5 |
| 3 | Venta para c1: p1:2,p2:1 | 13 / 2 / 11 | 5 / 1 / 4 |
| 4 | Consultar pendientes y despachar esa venta | 11 / 0 / 11 | 4 / 0 / 4 |
| 5 | Repetir despacho | 11 / 0 / 11 | 4 / 0 / 4 |
| 6 | Reiniciar servidor y repetir despacho | 11 / 0 / 11 | 4 / 0 / 4 |

Los comandos de la sección anterior ejecutan este recorrido. Consulte existencias después de cada paso. El pendiente debe contener el cliente c1, su dirección ficticia y las dos líneas. Tras el despacho no debe figurar entre los pendientes.

## 8. API HTTP

Cabecera de identidad: `Authorization: Bearer demo-...`. POST usa `Content-Type: application/x-www-form-urlencoded`; las respuestas son JSON UTF-8. Los parámetros indicados son obligatorios; se rechazan campos adicionales y repetidos. Los GET no requieren parámetros.

| Método y ruta | Rol permitido | Campos POST | Respuesta 200 |
|---|---|---|---|
| GET `/api/vendedores` | RRHH | — | Lista de Usuario |
| POST `/api/autorizacion` | RRHH | `vendedor`, `activo=true\|false` | Usuario actualizado |
| GET `/api/catalogo` | VENDEDOR activo | — | `{clientes,productos,existencias}` |
| GET `/api/existencias` | VENDEDOR activo, BODEGA, ENTREGAS | — | Lista de Existencia con disponibles |
| GET `/api/proveedores` | BODEGA | — | Lista de `{producto,proveedores}` |
| POST `/api/compras` | BODEGA | `proveedor`, `producto`, `cantidad`, `referencia` | Compra |
| POST `/api/ventas` | VENDEDOR activo | `cliente`, `lineas` | Venta |
| GET `/api/entregas/pendientes` | ENTREGAS | — | Lista de `{venta,cliente}` |
| POST `/api/entregas` | ENTREGAS | `venta` | Entrega |

Esquemas y ejemplos de respuesta (los UUID y fechas reales se generan al operar):

```json
{"id":"v1","nombre":"Vendedor Uno demo","rol":"VENDEDOR","autorizado":true}
```

```json
{"productoId":"p1","fisicas":13,"reservadas":2,"disponibles":11}
```

```json
{"id":"UUID-COMPRA","proveedorId":"pr1","productoId":"p1","cantidad":3,"referencia":"CONSOLA-1"}
```

```json
{"id":"UUID-VENTA","vendedorId":"v1","clienteId":"c1","lineas":[{"productoId":"p1","cantidad":2},{"productoId":"p2","cantidad":1}]}
```

```json
{"id":"UUID-ENTREGA","ventaId":"UUID-VENTA","destinatario":"Cliente Uno ficticio","direccion":"Calle Demo 1, Ciudad Ficticia","fecha":"2026-11-16T12:00:00Z"}
```

El catálogo contiene listas de clientes (`id,nombre,direccion`), productos (`id,nombre,proveedores`) y existencias. Reposición devuelve cada producto junto con sus proveedores (`id,nombre`). Pendientes devuelve cada venta y el cliente completo. Despachar devuelve la dirección y destinatario conservados en la entrega; los productos permanecen asociados en la venta.

Ejemplo HTTP con curl (macOS/Linux; en Windows use `curl.exe`):

```sh
curl -i http://127.0.0.1:8080/api/autorizacion -H 'Authorization: Bearer demo-rrhh' -H 'Content-Type: application/x-www-form-urlencoded' --data 'vendedor=v1&activo=true'
```

Reglas de entrada:

- `cantidad`: entero entre 1 y 1000000; no admite decimales ni negativos.
- `referencia`: de 1 a 60 caracteres ASCII alfanuméricos, guion o guion bajo (`[A-Za-z0-9_-]{1,60}`). La clave idempotente es **proveedor + referencia**; con iguales producto y cantidad devuelve la compra anterior, con otros datos devuelve 409. Proveedores diferentes pueden tener la misma referencia.
- `lineas`: entre 1 y 100 elementos `producto:cantidad`, separados por comas, sin productos duplicados. No se aceptan reservas parciales.
- Compra: valida existencia de producto y proveedor, relación de suministro y capacidad final.
- Despacho: una entrega completa por venta. Repetir devuelve la misma entrega, incluso después de reiniciar.
- Venta: no es idempotente. Si la respuesta de red es incierta, consulte pendientes con ENTREGAS antes de repetir; no hay cancelaciones.

| HTTP | Significado |
|---|---|
| 400 | Formulario, cantidad, referencia, booleano o líneas inválidos |
| 401 | Identidad desconocida o cabecera ausente |
| 403 | Rol incorrecto o vendedor revocado/inactivo |
| 404 | Recurso o ruta inexistente |
| 409 | Stock, capacidad, suministro o referencia en conflicto |
| 500 | Fallo interno o persistencia; la transacción se revierte |

Error típico: `{"error":"Usuario inactivo o rol no autorizado","status":403}`. No se devuelven errores como respuestas exitosas. No hay CORS abierto; la web usa el mismo origen que la API.

## 9. Pruebas y evidencia real

macOS/Linux:

```sh
sh pruebas/ejecutar.sh
```

Windows CMD:

```bat
pruebas\ejecutar.bat
```

Las pruebas crean su propio directorio temporal y servidor en `127.0.0.1:18080`, que detienen al finalizar. No alteran `data/polimarket.ser`. El puerto 18080 debe estar libre. Un error termina con código distinto de cero y queda registrado. Se conservan los datos temporales para inspección; su ruta figura en el JSON.

La ejecución registrada aprobó **56 comprobaciones de integración + 4 de persistencia**:

- Permisos, autorización/revocación, matriz de roles y suplantación rechazada.
- Catálogo inicial, fórmula y consultas sin escritura.
- Compras válidas, reintentos, referencias contradictorias, relaciones de suministro, cantidades y límite de capacidad.
- Ventas, reservas, límites de líneas, duplicados, cantidades negativas/decimales y rollback multilínea.
- Pendientes, destinatario, despacho único y reintentos tras reiniciar.
- Dos ventas concurrentes compitiendo por stock; dos despachos simultáneos.
- Los cinco RF mediante el **api.mjs real** ejecutado en Node y mediante procesos independientes de **ClienteConsola**.
- Bloqueo de un segundo servidor, rechazo de tipos no permitidos al deserializar y rollback ante un fallo real de escritura provocado renombrando el directorio temporal.

Archivos: `evidencias/pruebas.txt`, `resultados.json`, `persistencia.txt`, `servidor-pruebas.log` y `diagramas.txt`. El JSON registra fecha UTC, plataforma, versión de Node y cada resultado. Las pruebas no dependen de las 34 comprobaciones afirmadas en el informe: estas evidencias proceden de la implementación entregada.

**Alcance visual:** la herramienta de navegador devolvió `The browser access host is unavailable`. No se automatizaron clics, formularios ni capturas de la web. Sí se ejecutó su módulo Fetch y se verificó que el servidor entrega HTML, CSS y ambos módulos. El recorrido manual de la sección 5 permite comprobar la interfaz; anote su resultado en `evidencias/revision-manual.md`. Tampoco se ejecutaron pruebas en Windows/Linux, caída eléctrica, carga prolongada o accesibilidad exhaustiva.

## 10. Persistencia y reinicio de datos

`data/polimarket.ser` guarda usuarios, clientes, productos, proveedores, inventario, compras, ventas, entregas y movimientos. Se crea con `Store.seed()` únicamente si no existe. El reinicio normal conserva todo el estado. No elimine datos para solucionar un error de arranque sin conservar una copia.

Cada operación pública de negocio usa el monitor de Store. Las escrituras conservan una copia del estado (registros y listas inmutables), ejecutan todas sus colaboraciones, escriben un temporal en el mismo directorio, fuerzan su contenido a disco y reemplazan el archivo mediante `ATOMIC_MOVE`. Ante validación o fallo de persistencia se restaura la copia. Si el sistema de archivos no admite reemplazo atómico, se rechaza la operación: no hay alternativa de escritura parcial. La prueba de fallo de escritura no simula ni demuestra un corte eléctrico ni todas las clases de fallo de almacenamiento.

El archivo `.lock` se mantiene bloqueado por el proceso para impedir dos escritores. Su mera existencia no significa que esté bloqueado: el bloqueo real lo administra el sistema operativo. La lectura aplica una lista de tipos permitidos y límites de tamaño, referencias y profundidad. No hay migraciones de formato. No cargue archivos de procedencia desconocida.

Para iniciar una demostración limpia sin perder datos, detenga **todos** los servidores y renombre `data` a un nombre de respaldo que todavía no exista:

```sh
# macOS/Linux, después de Ctrl+C
mv data data-respaldo-1
java -cp build/server polimarket.Server
```

```bat
rem Windows CMD, despues de Ctrl+C
ren data data-respaldo-1
java -cp build/server polimarket.Server
```

Alternativa sin mover nada: `java -cp build/server polimarket.Server data/otra-demo.ser`. Ese archivo tendrá datos iniciales si no existe. No borre un archivo de bloqueo mientras su servidor esté activo. Para restaurar un respaldo, detenga los procesos y devuelva el directorio guardado a `data`, conservando también el estado actual en otro nombre.

Opciones de arranque: `Server [archivoEstado] [puerto]`. Ejemplo de puerto alternativo:

```sh
java -cp build/server polimarket.Server data/polimarket.ser 8081
java -Dpolimarket.url=http://127.0.0.1:8081 -cp build/client ClienteConsola demo-rrhh GET /api/vendedores
```

La web correspondiente estará en `http://127.0.0.1:8081`. En el uso normal no se requieren argumentos.

## 11. Diagramas y correspondencia con el informe

`diagramas/clases.bmp` y `diagramas/componentes.bmp` son imágenes BMP RGB de 24 bits, reabiertas y verificadas con firma `BM`. Se revisaron visualmente los diagramas renderizados. Los PNG permiten previsualizarlos con menor tamaño de archivo. PlantUML editable: `clases.puml` y `componentes.puml`. Este último contiene las vistas de acceso y colaboración; el exportador las reúne en una sola imagen.

Para regenerarlos, coloque un JAR PlantUML 1.2025.2 en una ruta de su elección y ejecute desde la raíz (reemplace `RUTA/plantuml.jar`):

```sh
java -Djava.awt.headless=true -jar RUTA/plantuml.jar -tpng diagramas/clases.puml diagramas/componentes.puml
java -Djava.awt.headless=true diagramas/Exportar.java
```

El motor Smetana incluido evita instalar Graphviz. `Exportar.java` usa ImageIO del JDK, une las vistas y verifica los BMP. PlantUML es una herramienta de documentación opcional, no una dependencia de ejecución.

### Diferencias respecto al informe

- El texto del informe menciona nueve registros, pero su diagrama contiene diez. Se implementaron los **diez**: Usuario, Cliente, Venta, LineaVenta, Producto, Existencia, Proveedor, Compra, Entrega y Movimiento, más Rol.
- El nuevo UML explicita `Venta.lineas`, presente en el código y en la composición descrita por el informe. La multiplicidad `1..100` precisa su `1..*` conforme a RF-04; las salidas de Entrega se acotan de igual manera.
- La lógica HTTP está en `AdaptadorHTTP.java`; `Server.java` conserva arranque e inyección. Esto separa transporte y construcción sin cambiar los endpoints.
- El informe no da nombres y direcciones del seed en su texto; los incluidos aquí son explícitamente ficticios. Se conservan todos los identificadores, saldos, relaciones de suministro y tokens exigidos.
- Se precisa el ámbito proveedor + referencia de la idempotencia de compras, conforme a la adquisición identificada por referencia de proveedor. No existe idempotencia global entre proveedores diferentes.
- El número de pruebas se sustituye por los resultados reales de esta entrega. No se certifica formalmente UML 2.5 ni se atribuye ejecución a integrantes del grupo.

Cotejo: `evidencias/trazabilidad.md` relaciona modelo, contratos, dependencias, endpoints y pruebas. `componentes.csv` incluye la tabla de componentes; es una adaptación provisional porque no se recibió la tabla 1 oficial de la asignatura.

## 12. Solución de problemas

| Problema | Acción |
|---|---|
| Java no reconocido | Instale JDK 21, configure PATH y abra una nueva terminal. Compruebe `java -version` y `javac -version`. |
| JDK incorrecto | Haga que ambos comandos apunten al mismo JDK 21. No basta cambiar solo JAVA_HOME. |
| Error de compilación | Ejecute desde la carpeta del README, conserve la estructura y revise el primer error de javac. Los scripts se detienen si falla. |
| Puerto 8080 ocupado | Detenga el servidor anterior o use el puerto alternativo documentado. No mate procesos ajenos sin identificarlos. |
| Servidor no disponible | Revise el mensaje de arranque, URL, puerto y terminal del servidor. No use HTTPS ni `file://`. |
| HTTP 401 | Use exactamente un token de la tabla con cabecera Bearer. |
| HTTP 403 | Seleccione el rol indicado; para v1, autorícelo con RRHH. Cambiar un formulario no concede permisos. |
| HTTP 409 | Consulte existencias, relación de suministro y referencia. Un reintento de compra debe repetir todos sus datos. |
| Script sin permisos | Use `sh compilar.sh` o `sh pruebas/ejecutar.sh`; no necesitan bit ejecutable. En Windows use los BAT. |
| Estado bloqueado | Detenga el proceso que usa el mismo archivo; no elimine `.lock` para forzar acceso. |
| Estado incompatible | Conserve una copia y use un archivo nuevo. No hay migración automática de versiones. |
| Fallo de guardado o ATOMIC_MOVE | Compruebe permisos y espacio; use un directorio local que admita movimiento atómico. La operación fallida se revierte. |
| Node no reconocido | Solo se necesita para integración; instale Node 22+ o ejecute manualmente el recorrido. |
| Pruebas no arrancan | Libere 18080 y compruebe permisos de red local y `evidencias/servidor-pruebas.log`. |

## 13. Limitaciones y conclusiones

Los tokens son públicos y fijos, adecuados únicamente para demostración local. No hay contraseñas, TLS, gestión de usuarios, pagos, facturación, cancelaciones ni entregas parciales. Las transacciones serializadas y la serialización de objetos son apropiadas para datos pequeños; no sustituyen una base de datos ni garantizan recuperación de una caída eléctrica. El volumen serializado y las referencias se limitan al leer; no se ofrece crecimiento ilimitado.

La reutilización es observable: Bodega concentra los cambios de inventario para tres coordinadores, RecursosHumanos aplica una política común y ambos clientes consumen los mismos servicios. Separar físicas y reservadas permite mantener stock consistente y realizar una única salida por venta. Las pruebas ejercitan estas colaboraciones mediante la API, además de verificar fallos de persistencia de forma directa.

La entrega académica debe completar autores, grupo, tutor, participación individual y la plantilla oficial con datos del grupo. Esa información no se inventa ni se altera en los PDF originales.
