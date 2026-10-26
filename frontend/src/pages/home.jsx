import { useState } from "react"
import { AppLayout } from "@/components/app-layout"
import { Dashboard } from "@/pages/dashboard"
import { PatientForm } from "@/pages/patient-form"
import { SearchPage } from "@/pages/search"

export function Home({ me, onLogout, logoutError }) {
  const [view, setView] = useState({ page: "dashboard" })
  const [query, setQuery] = useState("")
  const [lastVisited, setLastVisited] = useState(null)
  const canEdit = me.actions.includes("EDIT_PATIENT")

  function navigate(id) {
    if (id === "add" || id === "search") setView({ page: id })
    else setView({ page: "dashboard", focus: id })
  }

  function search(value) {
    setQuery(value)
    if (view.page !== "search") setView({ page: "search" })
  }

  function openPatient(patient) {
    setLastVisited(patient)
    if (canEdit) setView({ page: "edit", patient })
  }

  function finishForm(saved) {
    if (saved && view.page === "edit") setLastVisited(saved)
    setView({ page: "dashboard" })
  }

  return (
    <AppLayout me={me} active={view.focus === "edit" ? "edit" : view.page} query={query} onQueryChange={search}
      onNavigate={navigate} onLogout={onLogout} logoutError={logoutError}>
      {view.page === "dashboard" ? (
        <Dashboard me={me} focusRequest={view} query={query} onQueryChange={setQuery}
          lastVisited={lastVisited} onOpen={openPatient} />
      ) : view.page === "search" ? (
        <SearchPage me={me} query={query} onQueryChange={setQuery} onOpen={openPatient}
          onVisit={setLastVisited} onBack={() => navigate("dashboard")} />
      ) : (
        <PatientForm key={view.patient?.id ?? "new"} me={me} patient={view.patient} onDone={finishForm} />
      )}
    </AppLayout>
  )
}
