import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor, cleanup } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'
import { api } from './api'

vi.mock('./api', () => ({ api: {
  load: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn(),
  adjustStock: vi.fn(), createOrder: vi.fn(), cancelOrder: vi.fn(), setProductArchived: vi.fn(),
} }))
const fixture = () => ({
  products: [{ id: 1, sku: 'MOUSE-01', name: 'Mouse', price: 125.50, reorderLevel: 5, supplierId: 1, supplierName: 'Tech' }],
  suppliers: [{ id: 1, name: 'Tech', email: 'qa@example.test', phone: '123' }],
  warehouses: [{ id: 1, name: 'Main', location: 'Delhi' }],
  stock: [{ id: 1, productId: 1, warehouseId: 1, productName: 'Mouse', productSku: 'MOUSE-01', warehouseName: 'Main', quantity: 10 }],
  orders: [{ id: 1, customerName: 'QA', totalAmount: 376.50, status: 'CONFIRMED', items: [{ quantity: 3 }] }], summary: {},
})
beforeEach(() => {
  vi.resetAllMocks()
  api.load.mockResolvedValue(fixture())
  vi.spyOn(window, 'confirm').mockReturnValue(true)
})
afterEach(() => { cleanup(); vi.restoreAllMocks() })
async function open(section) {
  const user=userEvent.setup()
  render(<App />)
  await screen.findByText('Welcome back.')
  if (section) await user.click(screen.getByRole('button', {name:section, exact:true}))
  return user
}
describe('Inventory user workflows', () => {
  it('archives a product, reveals archived records and restores it', async () => {
    const data=fixture()
    api.load.mockImplementation(async()=>structuredClone(data))
    api.setProductArchived.mockImplementation(async(id,archived)=>{data.products[0].archived=archived})
    const user=await open('Products')
    await user.click(screen.getByRole('button',{name:'Archive',exact:true}))
    expect(await screen.findByText('Product archived. Order history preserved.')).toBeTruthy()
    expect(screen.queryByText('Mouse')).toBeNull()
    await user.click(screen.getByLabelText('Include archived products'))
    expect(await screen.findByText('Mouse')).toBeTruthy()
    await user.click(screen.getByRole('button',{name:'Restore',exact:true}))
    expect(await screen.findByText('Product restored.')).toBeTruthy()
    expect(screen.getByText('Active')).toBeTruthy()
    expect(api.setProductArchived).toHaveBeenNthCalledWith(1,1,true)
    expect(api.setProductArchived).toHaveBeenNthCalledWith(2,1,false)
  })
  it('does not offer archived products for new orders', async () => {
    const data=fixture();data.products[0].archived=true
    api.load.mockResolvedValue(data)
    const user=await open('Orders')
    await user.click(screen.getByRole('button',{name:'+ Create order'}))
    expect(screen.queryByRole('option',{name:'Mouse'})).toBeNull()
  })
  it('loads the dashboard using the API and shows live stock', async () => {
    await open()
    expect(api.load).toHaveBeenCalledTimes(1)
    expect(screen.getByText('Connected to backend')).toBeTruthy()
    expect(screen.getByText('Recent orders')).toBeTruthy()
    expect(screen.getByText('₹376.50')).toBeTruthy()
  })
  it('accepts decimal prices and a product without optional fields', async () => {
    const user=await open('Products')
    await user.click(screen.getByRole('button',{name:'+ Add Product'}))
    await user.type(screen.getByLabelText('SKU'),'DECIMAL-01')
    await user.type(screen.getByLabelText('Product name'),'Decimal item')
    await user.clear(screen.getByLabelText('Price'))
    await user.type(screen.getByLabelText('Price'),'19.99')
    expect(screen.getByLabelText('Price').validity.valid).toBe(true)
    await user.click(screen.getByRole('button',{name:'Save changes'}))
    await waitFor(()=>expect(api.create).toHaveBeenCalledWith('products',expect.objectContaining({price:19.99,supplierId:null})))
  })
  it('allows a supplier with only its required name', async () => {
    const user=await open('Suppliers')
    await user.click(screen.getByRole('button',{name:'+ Add Supplier'}))
    await user.type(screen.getByLabelText('Supplier name'),'New supplier')
    await user.click(screen.getByRole('button',{name:'Save changes'}))
    await waitFor(()=>expect(api.create).toHaveBeenCalledWith('suppliers',expect.objectContaining({name:'New supplier',email:null,phone:null})))
  })
  it('submits stock adjustments with numeric identifiers and quantity', async () => {
    const user=await open('Inventory')
    await user.click(screen.getByRole('button',{name:'+ Add stock'}))
    await user.selectOptions(screen.getByLabelText('Product'),'1')
    await user.selectOptions(screen.getByLabelText('Warehouse'),'1')
    await user.clear(screen.getByLabelText('Quantity'))
    await user.type(screen.getByLabelText('Quantity'),'4')
    await user.click(screen.getByRole('button',{name:'Save changes'}))
    await waitFor(()=>expect(api.adjustStock).toHaveBeenCalledWith('add',{productId:1,warehouseId:1,quantity:4}))
  })
  it('submits an order with the selected item', async () => {
    const user=await open('Orders')
    await user.click(screen.getByRole('button',{name:'+ Create order'}))
    await user.type(screen.getByLabelText('Customer name'),'Customer')
    await user.selectOptions(screen.getByLabelText('Product'),'1')
    await user.selectOptions(screen.getByLabelText('Warehouse'),'1')
    await user.click(screen.getByRole('button',{name:'Create order',exact:true}))
    await waitFor(()=>expect(api.createOrder).toHaveBeenCalledWith({customerName:'Customer',items:[{productId:1,warehouseId:1,quantity:1}]}))
  })
  it('shows cancellation errors without an unhandled rejection', async () => {
    api.cancelOrder.mockRejectedValue(new Error('Order is already cancelled'))
    const user=await open('Orders')
    await user.click(screen.getByRole('button',{name:'Cancel',exact:true}))
    expect(await screen.findByText('Order is already cancelled')).toBeTruthy()
  })
  it('shows referenced-record deletion errors without crashing', async () => {
    api.remove.mockRejectedValue(new Error('Record is referenced by stock'))
    const user=await open('Products')
    await user.click(screen.getByRole('button',{name:'Delete'}))
    expect(await screen.findByText('Record is referenced by stock')).toBeTruthy()
  })
  it('displays a useful backend-offline message', async () => {
    api.load.mockRejectedValue(new Error('Failed to fetch'))
    render(<App />)
    expect(await screen.findByText('Could not load backend data: Failed to fetch')).toBeTruthy()
  })
})
