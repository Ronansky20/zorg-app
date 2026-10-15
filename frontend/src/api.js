async function request(path, { method = 'GET', body } = {}) {
    const headers = {}
    if (method !== 'GET') headers['X-Zorg-Client'] = 'web'
    if (body !== undefined) headers['Content-Type'] = 'application/json'

    const response = await fetch(`/api${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
    })
    if (response.status === 204) return null
    const data = await response.json()
    if (!response.ok) throw new Error(data.detail ?? response.statusText)
    return data
}

export const getMe = () => request('/me')
export const getConfig = () => request('/config')
export const login = (credentials) => request('/auth/login', { method: 'POST', body: credentials })
export const logout = () => request('/auth/logout', { method: 'POST' })

export const getPatients = () => request('/patients')
export const getAccounts = () => request('/auth/accounts')
export const getUsers = () => request('/users')
