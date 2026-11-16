import { request } from './api.mjs';
const token = document.querySelector('#token');
const status = document.querySelector('#status');
const output = document.querySelector('#output');
async function run(method, path, params = {}) {
  const buttons = document.querySelectorAll('button');
  buttons.forEach(button => { button.disabled = true; });
  status.textContent = 'Procesando…'; status.dataset.kind = '';
  try {
    const result = await request(token.value, method, path, params);
    status.textContent = `HTTP ${result.status} · Operación completada`;
    status.dataset.kind = 'success'; output.textContent = JSON.stringify(result.body, null, 2);
    if (path === '/api/ventas') document.querySelector('#venta').value = result.body.id;
  } catch (error) {
    status.textContent = error.status ? `HTTP ${error.status} · Solicitud rechazada` : 'No se pudo conectar';
    status.dataset.kind = 'error'; output.textContent = error.message;
  } finally { buttons.forEach(button => { button.disabled = false; }); }
}
document.querySelectorAll('[data-get]').forEach(button => button.addEventListener('click', () => run('GET', button.dataset.get)));
document.querySelectorAll('form').forEach(form => form.addEventListener('submit', event => {
  event.preventDefault(); run('POST', form.dataset.path, Object.fromEntries(new FormData(form)));
}));
