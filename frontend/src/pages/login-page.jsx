import { useEffect, useState } from "react"
import { getAccounts, getConfig, login } from "@/api"
import { LoginForm } from "@/components/login-form"
import { Button } from "@/components/ui/button"
import { FieldError } from "@/components/ui/field"

async function loadOptions() {
    const { mode } = await getConfig()
    return { mode, accounts: mode === "json" ? await getAccounts() : [] }
}

export function LoginPage({ notice, onLogin }) {
    const [options, setOptions] = useState(null)
    const [loadError, setLoadError] = useState(null)
    const [attempt, setAttempt] = useState(0)

    useEffect(() => {
        loadOptions().then(setOptions).catch((e) => setLoadError(e.message))
    }, [attempt])

    function retry() {
        setLoadError(null)
        setAttempt((n) => n + 1)
    }

    async function handleSubmit(credentials) {
        onLogin(await login(credentials))
    }

    return (
        <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-background p-6 md:p-10">
            <div className="w-full max-w-xs">
                {loadError ? (
                    <div className="flex flex-col items-center gap-4 text-center">
                        <FieldError>{loadError}</FieldError>
                        <Button variant="outline" onPress={retry}>Try again</Button>
                    </div>
                ) : options && (
                    <LoginForm mode={options.mode} accounts={options.accounts} notice={notice} onSubmit={handleSubmit} />
                )}
            </div>
        </div>
    )
}
