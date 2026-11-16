# Estado de revisión de la interfaz

No ejecutada visualmente en esta sesión. La herramienta de navegador devolvió:

`The browser access host is unavailable`

Esto no afecta las 56 pruebas HTTP, que sí ejecutan api.mjs real mediante Fetch en Node y el cliente Java en procesos independientes. No se presentan como pruebas visuales.

## Recorrido pendiente para el evaluador

- [ ] Abrir http://127.0.0.1:8080 después del arranque y comprobar distribución en escritorio y pantalla estrecha.
- [ ] Elegir vendedor inactivo y observar HTTP 403.
- [ ] Elegir RRHH y autorizar v1.
- [ ] Consultar catálogo como v1 y verificar los clientes y productos.
- [ ] Consultar proveedores y comprar como BODEGA.
- [ ] Registrar venta como v1; verificar que su ID aparece en el formulario de despacho.
- [ ] Consultar pendientes y despachar como ENTREGAS; repetir sin doble descuento.
- [ ] Revocar v1 y verificar rechazo con la misma identidad.
- [ ] Verificar deshabilitación de botones durante solicitudes y recuperación tras error.
- [ ] Recorrer con teclado, comprobar foco y legibilidad de resultados.

Fecha y responsable de la revisión: por completar después de ejecutarla.
