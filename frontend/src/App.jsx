import { useEffect, useState } from 'react'
import { getMe, logout } from '@/api'
import { Button } from '@/components/ui/button'
import { LoginPage } from '@/pages/login-page'

function App() {
  const [me, setMe] = useState(undefined)

  useEffect(() => {
    getMe().then(setMe).catch(() => setMe(null))
  }, [])

  if (me === undefined) return null
  if (me === null) return <LoginPage onLogin={setMe} />

  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4">
      <h1 className="font-heading text-2xl font-semibold">Welcome, {me.user.name}</h1>
      <Button variant="outline" onClick={() => logout().then(() => setMe(null))}>Log out</Button>
    </main>
  )
}

export default App
