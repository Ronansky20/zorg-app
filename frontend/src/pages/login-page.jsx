import { useEffect, useState } from "react"
import { getAccounts, getConfig, login } from "@/api"
import { LoginForm } from "@/components/login-form"

export function LoginPage({ onLogin }) {
    const [mode, setMode] = useState(null)
    const [accounts, setAccounts] = useState([])

    useEffect(() => {
        getConfig().then(async (config) => {
            setMode(config.mode)
            if (config.mode === "json") setAccounts(await getAccounts())
        })
    }, [])

    async function handleSubmit(credentials) {
        onLogin(await login(credentials))
    }

    return (
        <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-background p-6 md:p-10">
            <div className="w-full max-w-sm">
                {mode && <LoginForm mode={mode} accounts={accounts} onSubmit={handleSubmit} />}
            </div>
        </div>
    )
}