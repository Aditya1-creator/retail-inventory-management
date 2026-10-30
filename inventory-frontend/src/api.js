const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8082/api').replace(/\/$/, '')

async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (response.status === 204) return null
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const message = body?.message || body?.error || `Request failed with status ${response.status}`
    throw new Error(message)
  }
  return body
}

export const api = {
  load: async () => {
    const [products, suppliers, warehouses, stock, orders, summary] = await Promise.all([
      request('/products'), request('/suppliers'), request('/warehouses'),
      request('/stock'), request('/orders'), request('/summary'),
    ])
    return { products, suppliers, warehouses, stock, orders, summary }
  },
  create: (resource, payload) => request(`/${resource}`, { method: 'POST', body: JSON.stringify(payload) }),
  update: (resource, id, payload) => request(`/${resource}/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  remove: (resource, id) => request(`/${resource}/${id}`, { method: 'DELETE' }),
  setProductArchived: (id, archived) => request(`/products/${id}/${archived?'archive':'restore'}`, { method: 'PUT' }),
  adjustStock: (kind, payload) => request(`/stock/${kind}`, { method: 'POST', body: JSON.stringify(payload) }),
  createOrder: payload => request('/orders', { method: 'POST', body: JSON.stringify(payload) }),
  cancelOrder: id => request(`/orders/${id}/cancel`, { method: 'PUT' }),
}
