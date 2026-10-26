const sessionListeners = new Set()

export function watchSession(listener) {
    sessionListeners.add(listener)
    return () => sessionListeners.delete(listener)
}

async function request(path, { method = 'GET', body } = {}) {
    const headers = {}
    if (method !== 'GET') headers['X-Zorg-Client'] = 'web'
    if (body !== undefined) headers['Content-Type'] = 'application/json'

    let response
    try {
        response = await fetch(`/api${path}`, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        })
    } catch {
        throw new Error("Can't reach the server, check your connection and try again")
    }
    if (path !== '/auth/login') sessionListeners.forEach((listener) => listener(response.status !== 401))
    if (response.status === 204) return null
    const data = await response.json().catch(() => null)
    if (!response.ok) throw new Error(data?.detail ?? `The server returned an error (${response.status}), try again later`)
    return data
}

export const getMe = () => request('/me')
export const getConfig = () => request('/config')
export const login = (credentials) => request('/auth/login', { method: 'POST', body: credentials })
export const logout = () => request('/auth/logout', { method: 'POST' })

export const getPatients = () => request('/patients')
export const getPatient = (id) => request(`/patients/${encodeURIComponent(id)}`)
export const addPatient = (patient) => request('/patients', { method: 'POST', body: patient })
export const updatePatient = (id, changes) => request(`/patients/${id}`, { method: 'PATCH', body: changes })
export const getAccounts = () => request('/auth/accounts')
export const getUsers = (profession) => request(profession ? `/users?profession=${encodeURIComponent(profession)}` : '/users')
