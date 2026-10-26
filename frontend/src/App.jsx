import { useEffect, useState } from 'react'
import { getConfig, getMe, logout, watchSession } from '@/api'
import { ConfigContext } from '@/hooks/use-config'
import { Home } from '@/pages/home'
import { LoginPage } from '@/pages/login-page'

function App() {
  const [me, setMe] = useState(undefined)
  const [config, setConfig] = useState(undefined)
  const [logoutError, setLogoutError] = useState(null)
  const [notice, setNotice] = useState(null)

  useEffect(() => {
    getMe().then(setMe).catch(() => setMe(null))
    getConfig().then(setConfig).catch(() => setConfig(null))
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

  if (me === undefined || config === undefined) return null
  if (me === null) return <LoginPage notice={notice} onLogin={handleLogin} />
  return (
    <ConfigContext value={config}>
      <Home me={me} onLogout={handleLogout} logoutError={logoutError} />
    </ConfigContext>
  )
}

export default App
