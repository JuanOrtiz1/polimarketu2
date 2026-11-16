# Verificación final

- Backend y consola compilados con javac 21.0.9 y `--release 21`.
- Arranque observado en `http://127.0.0.1:8080` con estado temporal.
- GET /api/existencias en 8080 devolvió HTTP 200 con p1: físicas 10, reservadas 0, disponibles 10; p2: físicas 5, reservadas 0, disponibles 5.
- Suite completa ejecutada en puerto aislado 18080: 56/56 integración y 4/4 persistencia aprobadas. La suite fue repetida después de mejorar el registro de fallos de arranque.
- `node --check` aprobado para api.mjs y app.mjs; `sh -n` aprobado para los dos scripts POSIX.
- Comandos de consola equivalentes a los del README ejercitados mediante procesos Java independientes en la suite. El UUID de despacho se obtiene de la venta real.
- Scripts BAT revisados estáticamente; no ejecutados en Windows. El listado de fuentes usa rutas entre comillas y separadores `/` para el archivo de argumentos javac.
- BMP generados con ImageIO, reabiertos y firma BM verificada; dimensiones en diagramas.txt. Revisión visual de ambas representaciones renderizadas completada.
- Documentación y diferencias de trazabilidad registradas en README.md y trazabilidad.md.
- No hay servidor de prueba pendiente tras la suite; el servidor temporal 8080 tampoco figura entre los procesos al cierre.
- Revisión visual de la aplicación web pendiente por indisponibilidad del navegador automatizado; recorrido manual en revision-manual.md.
