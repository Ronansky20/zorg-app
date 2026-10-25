import { useEffect, useRef, useState } from "react"
import { getPatients } from "@/api"
import { fullName, isIncomplete } from "@/lib/patients"
import { AppSidebar } from "@/components/app-sidebar"
import { PatientsTable } from "@/components/patients-table"
import { Button } from "@/components/ui/button"
import { FieldError } from "@/components/ui/field"
import { Item, ItemContent, ItemDescription, ItemTitle } from "@/components/ui/item"
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"

function StatItem({ title, value }) {
  return (
    <Item variant="outline" role="listitem" className="w-auto min-w-44 flex-nowrap bg-secondary">
      <ItemContent>
        <ItemTitle className="text-card-foreground">{title}</ItemTitle>
        <ItemDescription className="text-base font-medium">{value}</ItemDescription>
      </ItemContent>
    </Item>
  )
}

export function DoctorDashboard({ me, onLogout, logoutError }) {
  const [patients, setPatients] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [query, setQuery] = useState("")
  const [lastVisited, setLastVisited] = useState(null)
  const patientsRef = useRef(null)
  const filterRef = useRef(null)

  useEffect(() => {
    getPatients().then(setPatients).catch((e) => setLoadError(e.message))
  }, [attempt])

  function retry() {
    setLoadError(null)
    setAttempt((n) => n + 1)
  }

  function handleNavigate(id) {
    if (id === "my-patients") patientsRef.current?.scrollIntoView({ behavior: "smooth" })
    if (id === "search") filterRef.current?.focus()
  }

  const incomplete = patients?.filter((patient) => isIncomplete(patient, me.viewableFields)).length

  return (
    <SidebarProvider>
      <AppSidebar me={me} active="dashboard" query={query} onQueryChange={setQuery}
        onNavigate={handleNavigate} onLogout={onLogout} logoutError={logoutError} />
      <SidebarInset className="gap-8 bg-muted p-4 md:p-9">
        <SidebarTrigger className="md:hidden" />

        <div role="list" aria-label="Overview" className="flex flex-wrap gap-6">
          <StatItem title="Last Visited Patient" value={lastVisited ? fullName(lastVisited) : "—"} />
          <StatItem title="My patients" value={patients?.length ?? "—"} />
          <StatItem title="Incomplete records" value={incomplete ?? "—"} />
        </div>

        <section ref={patientsRef} aria-labelledby="patients-title"
          className="flex flex-col gap-1 rounded-xl border bg-card p-6 md:p-14">
          <div className="flex flex-col gap-2">
            <h1 id="patients-title" className="font-heading text-3xl font-semibold leading-9">Your patients</h1>
            <p className="text-base text-muted-foreground">These are the patients assigned to your care</p>
          </div>
          {loadError ? (
            <div className="flex flex-col items-start gap-4 py-4">
              <FieldError>{loadError}</FieldError>
              <Button variant="outline" onPress={retry}>Try again</Button>
            </div>
          ) : patients && (
            <PatientsTable patients={patients} viewableFields={me.viewableFields} query={query}
              onQueryChange={setQuery} onOpen={setLastVisited} filterRef={filterRef} />
          )}
        </section>
      </SidebarInset>
    </SidebarProvider>
  )
}
