import { useState } from "react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"

export function LoginForm({
  mode,
  accounts = [],
  notice,
  onSubmit,
  className,
  ...props
}) {
  const [error, setError] = useState(null)
  const [pending, setPending] = useState(false)

  async function submit(credentials) {
    if (pending) return
    setPending(true)
    setError(null)

    try {
      await onSubmit(credentials)
    } catch (e) {
      setError(e.message)
      setPending(false)
    }
  }

  function handleSubmit(event) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    submit({ email: data.get("email"), password: data.get("password") })
  }

  return (
    <div className={cn("flex flex-col gap-6", className)} {...props}>
      <form onSubmit={handleSubmit}>
        <FieldGroup>
          <div className="flex flex-col items-center gap-2 text-center">
            <h1 className="text-7xl font-black leading-none tracking-tight text-foreground">
              Zorg<br />App
            </h1>
            {mode === "json" && <FieldDescription>Local mode: choose an account</FieldDescription>}
            {notice && <FieldDescription role="status">{notice}</FieldDescription>}
          </div>

          {mode === "firebase" ? (
            <>
              <Field>
                <FieldLabel htmlFor="email">Email</FieldLabel>
                <FieldDescription className="nth-last-2:mt-0">Enter your email</FieldDescription>
                <Input id="email" name="email" type="email" autoComplete="username" placeholder="john.doe@example.com" required />
              </Field>
              <Field>
                <FieldLabel htmlFor="password">Password</FieldLabel>
                <FieldDescription className="nth-last-2:mt-0">Enter your password</FieldDescription>
                <Input id="password" name="password" type="password" autoComplete="current-password" placeholder="••••••••" required />
              </Field>
              <Field>
                <Button type="submit" isDisabled={pending}>{pending ? "Signing in…" : "Login"}</Button>
              </Field>
            </>
          ) : (
            <Field className="grid gap-2">
              {accounts.length === 0 && <FieldDescription>No accounts found in users.json</FieldDescription>}
              {accounts.map((account) => (
                <Button key={account.id} type="button" variant="outline" isDisabled={pending}
                  onPress={() => submit({ userId: account.id })}>
                  {account.name} · {account.profession}
                </Button>
              ))}
            </Field>
          )}

          {error && <FieldError>{error}</FieldError>}
        </FieldGroup>
      </form>
    </div>
  );
}
