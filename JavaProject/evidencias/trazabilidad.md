# Cotejo de implementación y modelos

Se leyeron completamente los dos PDF suministrados y se inspeccionaron las figuras de clases y componentes. Los PDF permanecen intactos. El contenido de la solicitud pegada define el trabajo; las instrucciones académicas de los PDF se usan como requisitos de referencia.

| Elemento | Implementación | Evidencia |
|---|---|---|
| Diez entidades y Rol | Domain.java; atributos del diagrama original | clases.puml / clases.bmp |
| Venta contiene líneas inmutables | Constructor de Venta usa List.copyOf; Ventas valida 1..100 sin duplicados | RF04 de resultados.json |
| Producto tiene una Existencia | seed conserva saldos p1/p2; no hay API para crear inconsistencias | RF02 y prueba de invariante |
| Proveedores asociados | p1: pr1/pr2; p2: pr2 | RF03 reposición y proveedor incompatible |
| Usuario/Cliente asociados a Venta | vendedor derivado del token; cliente validado | RF04 y suplantación rechazada |
| Una Entrega por Venta | entregas indexadas por ventaId | Reintento, reinicio y concurrencia RF05 |
| Movimientos de compra y entrega | Bodega.movimiento, origenTipo/origenId | Implementación; saldos verificados por API |
| Interfaces del informe | Contracts.java, mismos nombres y operaciones | componentes.puml / componentes.bmp |
| Colaboraciones entre negocio | Constructores dependen de interfaces | Server.java y vista de colaboración |
| RF01 a RF05 | Cinco clases en business/ | 56 comprobaciones de integración |
| Dos clientes independientes | HttpClient Java y api.mjs Fetch | Cinco RF probados desde cada cliente |
| Persistencia y rollback | Store.transaction y save | Reinicio HTTP + 4 pruebas directas |
| Compilación Java 21 | compilar.sh ejecutado; compilar.bat revisado estáticamente | Compilación y arranque correctos en macOS |

Las diferencias menores están detalladas en README. No se afirma ejecución del script BAT en Windows, ni revisión visual de los formularios, ni certificación formal UML.
