import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { api } from './api'

beforeEach(()=>vi.stubGlobal('fetch',vi.fn()))
afterEach(()=>vi.unstubAllGlobals())
const response = (body, status=200) => ({ok:status<400,status,json:async()=>body})
it('loads all six datasets from the configured backend',async()=>{
  fetch.mockResolvedValue(response([]))
  const result=await api.load()
  expect(Object.keys(result)).toEqual(['products','suppliers','warehouses','stock','orders','summary'])
  expect(fetch.mock.calls.map(([url])=>new URL(url).pathname)).toEqual(['/api/products','/api/suppliers','/api/warehouses','/api/stock','/api/orders','/api/summary'])
})
it('posts the order as JSON to the orders route',async()=>{
  fetch.mockResolvedValue(response({id:1},201))
  const order={customerName:'QA',items:[{productId:1,warehouseId:1,quantity:2}]}
  await api.createOrder(order)
  expect(fetch).toHaveBeenCalledWith(expect.stringMatching(/\/api\/orders$/),expect.objectContaining({method:'POST',body:JSON.stringify(order),headers:{'Content-Type':'application/json'}}))
})
it('accepts a successful no-content deletion',async()=>{
  fetch.mockResolvedValue(response(null,204))
  expect(await api.remove('products',1)).toBeNull()
})
it('surfaces the backend error message to the interface',async()=>{
  fetch.mockResolvedValue(response({message:'Insufficient stock available'},400))
  await expect(api.adjustStock('deduct',{quantity:20})).rejects.toThrow('Insufficient stock available')
})
it('handles non-JSON server errors without masking the HTTP status',async()=>{
  fetch.mockResolvedValue({ok:false,status:503,json:async()=>{throw new Error('Not JSON')}})
  await expect(api.cancelOrder(1)).rejects.toThrow('Request failed with status 503')
})
