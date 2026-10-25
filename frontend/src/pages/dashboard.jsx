import { useEffect, useRef, useState } from "react"
import { getPatients } from "@/api"
import { dashboardFor, fullName, isIncomplete, isOnMedication } from "@/lib/patients"
import { PatientsTable } from "@/components/patients-table"
import { Button } from "@/components/ui/button"
import { FieldError } from "@/components/ui/field"
import { Item, ItemContent, ItemDescription, ItemTitle } from "@/components/ui/item"

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

const STATS = {
  incomplete: { title: "Incomplete records", matches: (patient, me) => isIncomplete(patient, me.viewableFields) },
  onMedication: { title: "On medication", matches: isOnMedication },
}

export function Dashboard({ me, focusRequest, query, onQueryChange, lastVisited, onOpen }) {
  const dashboard = dashboardFor(me.user.profession)
  const [patients, setPatients] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const patientsRef = useRef(null)
  const filterRef = useRef(null)
  const loaded = patients !== null
  const picking = focusRequest?.focus === "edit"

  useEffect(() => {
    getPatients().then(setPatients).catch((e) => setLoadError(e.message))
  }, [attempt])

  useEffect(() => {
    const focus = focusRequest?.focus
    if (focus === "my-patients") patientsRef.current?.scrollIntoView({ behavior: "smooth" })
    if (focus === "search" || focus === "edit") filterRef.current?.focus()
  }, [focusRequest, loaded])

  function retry() {
    setLoadError(null)
    setAttempt((n) => n + 1)
  }

  return (
    <>
      <div role="list" aria-label="Overview" className="flex flex-wrap gap-6">
        <StatItem title="Last Visited Patient" value={lastVisited ? fullName(lastVisited) : "—"} />
        <StatItem title="My patients" value={patients?.length ?? "—"} />
        {dashboard.stats.map((key) => (
          <StatItem key={key} title={STATS[key].title}
            value={patients?.filter((patient) => STATS[key].matches(patient, me)).length ?? "—"} />
        ))}
      </div>

      <section ref={patientsRef} aria-labelledby="patients-title"
        className="flex flex-col gap-1 rounded-xl border bg-card p-6 md:p-14">
        <div className="flex flex-col gap-2">
          <h1 id="patients-title" className="font-heading text-3xl font-semibold leading-9">
            {picking ? "Edit a patient" : "Your patients"}
          </h1>
          <p className="text-base text-muted-foreground">
            {picking ? "Choose the patient you want to edit" : dashboard.description}
          </p>
        </div>
        {loadError ? (
          <div className="flex flex-col items-start gap-4 py-4">
            <FieldError>{loadError}</FieldError>
            <Button variant="outline" onPress={retry}>Try again</Button>
          </div>
        ) : patients && (
          <PatientsTable patients={patients} viewableFields={me.viewableFields}
            defaultColumns={dashboard.columns} query={query}
            onQueryChange={onQueryChange} onOpen={onOpen} filterRef={filterRef} />
        )}
      </section>
    </>
  )
}
