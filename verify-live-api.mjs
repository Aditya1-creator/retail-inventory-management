import assert from 'node:assert/strict'

const base=(process.env.INVENTORY_API_URL || 'http://localhost:8082/api').replace(/\/$/, '')
const results=[]
async function request(method,path,body,status=200) {
  const response=await fetch(base+path,{method,headers:{'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(15000)})
  const text=await response.text()
  assert.equal(response.status,status,`${method} ${path}: ${text}`)
  return text?JSON.parse(text):null
}
function passed(name){results.push(name);console.log('PASS '+name)}
for(const path of ['/products','/suppliers','/warehouses','/stock','/orders','/summary'])await request('GET',path)
passed('All six frontend data endpoints respond')
const tag='REVIEW-'+Date.now()
const supplier=await request('POST','/suppliers',{name:tag,email:'review@example.test',phone:''},201)
const warehouse=await request('POST','/warehouses',{name:tag,location:'Isolated review records'},201)
const product=await request('POST','/products',{sku:tag,name:'Review verification product',category:'Verification',price:125.50,reorderLevel:5,supplierId:supplier.id},201)
passed('Supplier, warehouse and decimal-price product creation')
const item=quantity=>({productId:product.id,warehouseId:warehouse.id,quantity})
const quantity=async()=> (await request('GET','/stock')).find(s=>s.productId===product.id&&s.warehouseId===warehouse.id)?.quantity
await request('POST','/stock/add',item(10));assert.equal(await quantity(),10)
await request('POST','/stock/deduct',item(11),400);assert.equal(await quantity(),10)
passed('Stock receipt and insufficient-stock safeguard')
let order=await request('POST','/orders',{customerName:tag,items:[item(3)]},201)
assert.equal(order.totalAmount,376.50);assert.equal(await quantity(),7)
await request('PUT',`/orders/${order.id}/cancel`);assert.equal(await quantity(),10)
await request('PUT',`/orders/${order.id}/cancel`,undefined,409);assert.equal(await quantity(),10)
passed('Order totals, stock deduction, restoration and repeated cancellation')
await request('POST','/orders',{customerName:tag,items:[item(3),item(99)]},400);assert.equal(await quantity(),10)
passed('Multi-item failure rolls back earlier deductions')
for(let round=1;round<=3;round++) {
  const competing=await Promise.all([1,2].map(async()=>{
    const r=await fetch(base+'/orders',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({customerName:tag,items:[item(8)]}),signal:AbortSignal.timeout(15000)})
    return {status:r.status,body:await r.json()}
  }))
  assert.deepEqual(competing.map(r=>r.status).sort(),[201,400],JSON.stringify(competing))
  assert.equal(await quantity(),2)
  const winner=competing.find(r=>r.status===201).body
  const cancellations=await Promise.all([1,2].map(async()=>{
    const r=await fetch(base+`/orders/${winner.id}/cancel`,{method:'PUT',signal:AbortSignal.timeout(15000)})
    return r.status
  }))
  assert.deepEqual(cancellations.sort(),[200,409]);assert.equal(await quantity(),10)
  passed(`Concurrent orders and cancellations round ${round}`)
}
const cors=await fetch(base+'/products',{method:'OPTIONS',headers:{Origin:'http://127.0.0.1:5173','Access-Control-Request-Method':'POST'}})
assert.equal(cors.status,200);assert.equal(cors.headers.get('access-control-allow-origin'),'http://127.0.0.1:5173')
passed('Frontend origin accepted by CORS')
console.log(JSON.stringify({passed:results.length,tag,productId:product.id,warehouseId:warehouse.id,supplierId:supplier.id,finalQuantity:await quantity(),results},null,2))
