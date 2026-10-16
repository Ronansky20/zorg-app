import { useEffect, useState } from 'react'
import { getMe, logout, watchSession } from '@/api'
import { Button } from '@/components/ui/button'
import { FieldError } from '@/components/ui/field'
import { LoginPage } from '@/pages/login-page'

function App() {
  const [me, setMe] = useState(undefined)
  const [logoutError, setLogoutError] = useState(null)
  const [notice, setNotice] = useState(null)

  useEffect(() => {
    getMe().then(setMe).catch(() => setMe(null))
  }, [])

  useEffect(() => {
    if (!me) return
    let timer
    function checkWhenExpired() {
      clearTimeout(timer)
      if (me.sessionTimeoutSeconds > 0) {
        timer = setTimeout(() => getMe().catch(() => {}), (me.sessionTimeoutSeconds + 5) * 1000)
      }
    }
    checkWhenExpired()
    const stopWatching = watchSession((active) => {
      if (active) return checkWhenExpired()
      setNotice('Your session has expired, please sign in again')
      setMe(null)
    })
    return () => {
      clearTimeout(timer)
      stopWatching()
    }
  }, [me])

  function handleLogin(user) {
    setNotice(null)
    setMe(user)
  }

  function handleLogout() {
    setLogoutError(null)
    logout().then(() => setMe(null)).catch((e) => setLogoutError(e.message))
  }

  if (me === undefined) return null
  if (me === null) return <LoginPage notice={notice} onLogin={handleLogin} />

  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4">
      <h1 className="font-heading text-2xl font-semibold">Welcome, {me.user.name}</h1>
      <Button variant="outline" onPress={handleLogout}>Log out</Button>
      {logoutError && <FieldError>{logoutError}</FieldError>}
    </main>
  )
}

export default App
