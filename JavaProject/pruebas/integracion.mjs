import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import { mkdtemp, writeFile, mkdir, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { request } from '../clientes/web/api.mjs';

const base = 'http://127.0.0.1:18080';
const directory = await mkdtemp(join(tmpdir(), 'polimarket-test-'));
const state = join(directory, 'estado.ser');
const results = [];
let server;
let logs = '';
async function test(name, fn) {
  try { await fn(); results.push({ name, passed: true }); console.log(`OK ${name}`); }
  catch (error) { results.push({ name, passed: false, error: error.stack }); console.error(`FALLO ${name}: ${error.message}`); throw error; }
}
const call = (token, method, path, data) => request(`demo-${token}`, method, `/api/${path}`, data, base);
async function rejected(status, token, method, path, data) {
  await assert.rejects(call(token, method, path, data), e => e.status === status);
}
async function inventory() { return (await call('bodega', 'GET', 'existencias')).body; }
async function start() {
  server = spawn('java', ['-cp', 'build/server', 'polimarket.Server', state, '18080']);
  server.stdout.on('data', data => { logs += data; }); server.stderr.on('data', data => { logs += data; });
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Servidor no inició: '+logs)), 10000);
    server.once('error', reject);
    server.once('exit', code => { clearTimeout(timer); reject(new Error(`Servidor terminó ${code}: ${logs}`)); });
    server.stdout.on('data', data => { if (data.toString().includes('PoliMarket listo')) { clearTimeout(timer); resolve(); } });
  });
}
async function stop() {
  if (!server || server.exitCode !== null) return;
  await new Promise(resolve => { server.once('exit', resolve); server.kill('SIGTERM'); });
}
function cli(token, method, path, params = {}, status = 200) {
  const result = spawnSync('java', [`-Dpolimarket.url=${base}`, '-cp', 'build/client', 'ClienteConsola', `demo-${token}`, method, `/api/${path}`, ...Object.entries(params).map(([k,v]) => `${k}=${v}`)], { encoding:'utf8', timeout:15000 });
  assert.equal(result.status, status >= 400 ? 2 : 0, result.stderr);
  const [http, ...body] = result.stdout.trim().split('\n'); assert.equal(http, `HTTP ${status}`); return JSON.parse(body.join('\n'));
}
let sale, delivery;
try {
  await start();
  await test('HTTP: recursos web reales', async () => {
    for (const path of ['/', '/style.css', '/api.mjs', '/app.mjs']) { const r=await fetch(base+path); assert.equal(r.status,200); assert.ok((await r.text()).length>100); }
  });
  await test('Seguridad: token desconocido 401', () => rejected(401,'incorrecto','GET','catalogo'));
  await test('RF01: vendedor inicialmente inactivo 403', () => rejected(403,'vendedor','GET','catalogo'));
  await test('RF01: BODEGA no autoriza', () => rejected(403,'bodega','POST','autorizacion',{vendedor:'v1',activo:'true'}));
  await test('RF01: vendedores iniciales', async () => {const r=await call('rrhh','GET','vendedores'); assert.equal(r.body.length,2); assert.equal(r.body.find(x=>x.id==='v1').autorizado,false);});
  await test('RF01: booleano explícito', () => rejected(400,'rrhh','POST','autorizacion',{vendedor:'v1',activo:'yes'}));
  await test('RF01: autorizar vendedor', async () => assert.equal((await call('rrhh','POST','autorizacion',{vendedor:'v1',activo:'true'})).body.autorizado,true));
  await test('RF02: catálogo y asociaciones iniciales', async () => { const r=(await call('vendedor','GET','catalogo')).body; assert.deepEqual(r.clientes.map(x=>x.id),['c1','c2']); assert.deepEqual(r.productos[0].proveedores,['pr1','pr2']); });
  await test('RF02: existencias iniciales y fórmula', async () => assert.deepEqual(await inventory(),[{productoId:'p1',fisicas:10,reservadas:0,disponibles:10},{productoId:'p2',fisicas:5,reservadas:0,disponibles:5}]));
  await test('RF02: consulta no modifica estado', async () => { const a=await readFile(state); await inventory(); assert.deepEqual(await readFile(state),a); });
  await test('RF01: revocación con mismo token', async () => {await call('rrhh','POST','autorizacion',{vendedor:'v1',activo:'false'}); await rejected(403,'vendedor','GET','catalogo'); await rejected(403,'vendedor','GET','existencias'); await rejected(403,'vendedor','POST','ventas',{cliente:'c1',lineas:'p1:1'}); await call('rrhh','POST','autorizacion',{vendedor:'v1',activo:'true'});});
  await test('RF03: reposición por producto', async () => { const r=(await call('bodega','GET','proveedores')).body; assert.deepEqual(r.map(x=>x.proveedores.map(p=>p.id)),[['pr1','pr2'],['pr2']]); });
  const purchase={proveedor:'pr1',producto:'p1',cantidad:'3',referencia:'PRUEBA-1'}; let purchaseId;
  await test('RF03: compra válida mediante api.mjs', async () => {purchaseId=(await call('bodega','POST','compras',purchase)).body.id; assert.equal((await inventory())[0].fisicas,13);});
  await test('RF03: compra idempotente', async () => {assert.equal((await call('bodega','POST','compras',purchase)).body.id,purchaseId); assert.equal((await inventory())[0].fisicas,13);});
  await test('RF03: referencia contradictoria 409', () => rejected(409,'bodega','POST','compras',{...purchase,cantidad:'4'}));
  await test('RF03: proveedor incompatible 409 y rollback', async () => {const before=await readFile(state); await rejected(409,'bodega','POST','compras',{...purchase,producto:'p2',referencia:'INCOMPATIBLE'}); assert.deepEqual(await readFile(state),before);});
  for (const cantidad of ['0','-1','1.5','1000001']) await test(`RF03: cantidad inválida ${cantidad}`, () => rejected(400,'bodega','POST','compras',{...purchase,cantidad}));
  await test('RF03: referencia inválida', () => rejected(400,'bodega','POST','compras',{...purchase,referencia:'no permitida!'}));
  await test('RF03: proveedor inexistente', () => rejected(404,'bodega','POST','compras',{...purchase,proveedor:'no'}));
  await test('RF03: producto inexistente', () => rejected(404,'bodega','POST','compras',{...purchase,producto:'no'}));
  await test('RF03: capacidad máxima y rollback', async () => {const before=await readFile(state); await rejected(409,'bodega','POST','compras',{...purchase,cantidad:'1000000',referencia:'LIMITE'}); assert.deepEqual(await readFile(state),before);});
  await test('RF04: venta válida y vendedor derivado del token', async () => {sale=(await call('vendedor','POST','ventas',{cliente:'c1',lineas:'p1:2,p2:1'})).body; assert.equal(sale.vendedorId,'v1'); assert.equal(sale.lineas.length,2);});
  await test('RF04: reserva sin salida física', async () => assert.deepEqual((await inventory())[0],{productoId:'p1',fisicas:13,reservadas:2,disponibles:11}));
  await test('RF04: cliente inexistente', () => rejected(404,'vendedor','POST','ventas',{cliente:'no',lineas:'p1:1'}));
  for (const [name,lineas,status] of [['insuficiente','p1:99',409],['duplicado','p1:1,p1:2',400],['negativa','p1:-1',400],['decimal','p1:1.5',400],['vacía','',400],['más de 100',Array(101).fill('p1:1').join(','),400],['producto inexistente','p1:1,no:1',404],['segunda línea sin stock','p1:1,p2:99',409]]) {
    await test(`RF04: ${name}, sin cambios parciales`, async () => {const before=await readFile(state); await rejected(status,'vendedor','POST','ventas',{cliente:'c1',lineas}); assert.deepEqual(await readFile(state),before);});
  }
  await test('Seguridad: no se puede suplantar vendedor', () => rejected(400,'vendedor','POST','ventas',{cliente:'c1',lineas:'p1:1',vendedor:'v2'}));
  await test('RF05: destinatario, dirección y líneas pendientes', async () => {const p=(await call('entregas','GET','entregas/pendientes')).body[0]; assert.equal(p.venta.id,sale.id); assert.equal(p.cliente.id,'c1'); assert.equal(p.cliente.direccion,'Calle Demo 1, Ciudad Ficticia'); assert.deepEqual(p.venta.lineas,sale.lineas);});
  await test('RF05: solo ENTREGAS despacha', () => rejected(403,'vendedor','POST','entregas',{venta:sale.id}));
  await test('RF05: venta inexistente', () => rejected(404,'entregas','POST','entregas',{venta:'no'}));
  await test('RF05: despacho mediante api.mjs', async () => {delivery=(await call('entregas','POST','entregas',{venta:sale.id})).body; assert.equal(delivery.destinatario,'Cliente Uno ficticio'); assert.match(delivery.fecha,/Z$/);});
  await test('RF05: salida de físicas y reservas', async () => assert.deepEqual((await inventory())[0],{productoId:'p1',fisicas:11,reservadas:0,disponibles:11}));
  await test('RF05: reintento conserva ID e inventario', async () => {const before=await inventory(); assert.equal((await call('entregas','POST','entregas',{venta:sale.id})).body.id,delivery.id); assert.deepEqual(await inventory(),before);});
  await test('RF05: venta despachada no está pendiente', async () => assert.deepEqual((await call('entregas','GET','entregas/pendientes')).body,[]));
  await test('Persistencia: inventario, compra y entrega tras reiniciar', async () => {const before=await inventory(); await stop(); await start(); assert.deepEqual(await inventory(),before); assert.equal((await call('entregas','POST','entregas',{venta:sale.id})).body.id,delivery.id); assert.equal((await call('bodega','POST','compras',purchase)).body.id,purchaseId);});
  let concurrentSale;
  await test('Concurrencia: solo una de dos ventas de 8 tiene stock', async () => {const r=await Promise.allSettled([1,2].map(()=>call('vendedor','POST','ventas',{cliente:'c2',lineas:'p1:8'}))); assert.equal(r.filter(x=>x.status==='fulfilled').length,1); assert.equal(r.find(x=>x.status==='rejected').reason.status,409); concurrentSale=r.find(x=>x.status==='fulfilled').value.body; assert.equal((await inventory())[0].reservadas,8);});
  await test('Concurrencia: dos despachos, una sola salida', async () => {const r=await Promise.all([1,2].map(()=>call('entregas','POST','entregas',{venta:concurrentSale.id}))); assert.equal(r[0].body.id,r[1].body.id); assert.deepEqual((await inventory())[0],{productoId:'p1',fisicas:3,reservadas:0,disponibles:3});});
  await test('Cliente Java: vendedores y autorización RF01', async () => {assert.equal(cli('rrhh','GET','vendedores').length,2); assert.equal(cli('rrhh','POST','autorizacion',{vendedor:'v1',activo:'true'}).autorizado,true);});
  await test('Cliente Java: catálogo y existencias RF02', async () => {assert.equal(cli('vendedor','GET','catalogo').clientes.length,2); assert.equal(cli('bodega','GET','existencias')[0].fisicas,3);});
  await test('Cliente Java: proveedores y compras RF03', async () => {assert.equal(cli('bodega','GET','proveedores').length,2); assert.equal(cli('bodega','POST','compras',{...purchase,referencia:'JAVA-1'}).cantidad,3);});
  let javaSale;
  await test('Cliente Java: venta RF04', async () => {javaSale=cli('vendedor','POST','ventas',{cliente:'c1',lineas:'p1:1'}); assert.equal(javaSale.vendedorId,'v1');});
  await test('Cliente Java: pendientes y despacho RF05', async () => {assert.equal(cli('entregas','GET','entregas/pendientes')[0].venta.id,javaSale.id); assert.equal(cli('entregas','POST','entregas',{venta:javaSale.id}).ventaId,javaSale.id);});
  await test('Cliente Java: error HTTP y código de salida 2', async () => {assert.equal(cli('no','GET','catalogo',{},401).status,401);});
  await test('HTTP: tipos, campos duplicados y codificación inválida', async () => {
    for(const body of ['cliente=c1&cliente=c2&lineas=p1%3A1','cliente=%XX&lineas=p1%3A1']) {const r=await fetch(base+'/api/ventas',{method:'POST',headers:{Authorization:'Bearer demo-vendedor','Content-Type':'application/x-www-form-urlencoded'},body}); assert.equal(r.status,400);}
    assert.equal((await fetch(base+'/api/ventas',{method:'POST',body:'{}'})).status,400);
  });
  await test('Seguridad: matriz de roles en endpoints', async () => {await rejected(403,'rrhh','GET','catalogo'); await rejected(403,'vendedor','GET','proveedores'); await rejected(403,'bodega','GET','entregas/pendientes'); await rejected(403,'entregas','POST','compras',purchase);});
  await test('Persistencia: segundo proceso no abre el mismo estado', async () => {const r=spawnSync('java',['-cp','build/server','polimarket.Server',state,'18081'],{encoding:'utf8',timeout:10000}); assert.notEqual(r.status,0); assert.match(r.stderr,/bloqueado/);});
  await test('Invariante final', async () => {for(const e of await inventory()){assert.ok(e.fisicas>=e.reservadas && e.reservadas>=0); assert.equal(e.disponibles,e.fisicas-e.reservadas);}});
} catch (error) {
  if (!results.some(result => !result.passed)) {
    results.push({ name: 'Infraestructura de pruebas', passed: false, error: error.stack });
    console.error(error.message);
  }
  process.exitCode=1;
}
finally {
  await stop(); await mkdir('evidencias',{recursive:true});
  const report={fecha:new Date().toISOString(),plataforma:process.platform,node:process.version,estadoTemporal:state,total:results.length,aprobadas:results.filter(x=>x.passed).length,resultados:results};
  await writeFile('evidencias/resultados.json',JSON.stringify(report,null,2)+'\n');
  await writeFile('evidencias/pruebas.txt',`${report.fecha}\n${report.plataforma} ${report.node}\n`+results.map(x=>`${x.passed?'OK':'FALLO'} ${x.name}${x.error?'\n'+x.error:''}`).join('\n')+`\n${report.aprobadas}/${report.total} aprobadas\n`);
  await writeFile('evidencias/servidor-pruebas.log',logs);
  console.log(`${report.aprobadas}/${report.total} aprobadas`);
}
