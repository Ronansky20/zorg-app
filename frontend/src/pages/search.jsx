import { useEffect, useMemo, useState } from "react"
import { GridList, GridListItem } from "react-aria-components"
import { RiArrowRightSLine, RiSearchLine, RiUserLine, RiUserSearchLine } from "@remixicon/react"
import { getPatients } from "@/api"
import { useConfig } from "@/hooks/use-config"
import { PATIENT_COLUMNS, canViewColumn, columnLabel, fieldLabel, formatDate, formatField, fullName, queryWords, searchFieldsFor, searchPatients } from "@/lib/patients"
import { CareTeamBadges } from "@/components/care-team-badges"
import { Badge } from "@/components/ui/badge"
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage } from "@/components/ui/breadcrumb"
import { Button } from "@/components/ui/button"
import { FieldError } from "@/components/ui/field"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Item, ItemActions, ItemContent, ItemDescription, ItemMedia, ItemTitle } from "@/components/ui/item"
import { Separator } from "@/components/ui/separator"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"

const ALL_FIELDS = { key: "all", label: "All fields" }
const PREVIEW_COLUMNS = PATIENT_COLUMNS.filter((column) => ["address", "weight", "height", "bmi"].includes(column.key))

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

function Highlight({ text, words }) {
  if (!words.length) return text
  const parts = text.split(new RegExp(`(${words.map(escapeRegExp).join("|")})`, "gi"))
  return (
    <span>
      {parts.map((part, index) => index % 2 === 1
        ? <mark key={index} className="bg-transparent font-semibold text-primary">{part}</mark>
        : part)}
    </span>
  )
}

function ResultRow({ patient, field, fields, words }) {
  const highlight = (key, text) => <Highlight text={text} words={field?.key === key ? words : []} />
  const details = fields
    .filter((f) => f.key !== "name" && (f.key !== "medications" || field?.key === "medications"))
    .map((f) => ({ key: f.key, text: f.text(patient) }))
    .filter((detail) => detail.text)

  return (
    <Item variant="outline" className="group-data-selected/row:bg-muted group-data-hovered/row:bg-muted/50">
      <ItemMedia variant="icon"><RiUserLine /></ItemMedia>
      <ItemContent className="min-w-0">
        <ItemTitle>{highlight("name", fullName(patient))}</ItemTitle>
        {details.length > 0 && (
          <ItemDescription className="line-clamp-1">
            {details.map((detail, index) => (
              <span key={detail.key}>
                {index > 0 && " · "}
                {detail.key === "birthDate" && "Born "}
                {highlight(detail.key, detail.text)}
              </span>
            ))}
          </ItemDescription>
        )}
      </ItemContent>
      <ItemActions className="text-muted-foreground">
        {field && <span className="hidden sm:inline">Matched on {field.label.toLowerCase()}</span>}
        <RiArrowRightSLine className="size-4" aria-hidden />
      </ItemActions>
    </Item>
  )
}

function PatientPreview({ me, patient, onOpen }) {
  const config = useConfig()
  const canView = (key) => me.viewableFields.includes(key)

  return (
    <aside aria-labelledby="preview-title" className="flex flex-col gap-4 self-start rounded-xl border bg-card p-6">
      <div className="flex flex-col gap-1">
        <h2 id="preview-title" className="text-base font-semibold">{fullName(patient)}</h2>
        {canView("birthDate") && patient.birthDate && (
          <p className="text-sm text-muted-foreground">
            Born {formatDate(patient.birthDate)}{patient.age !== undefined && ` · ${patient.age} years old`}
          </p>
        )}
      </div>
      <Separator />
      <dl className="grid grid-cols-1 gap-x-4 gap-y-1 text-sm sm:grid-cols-[7rem_minmax(0,1fr)] sm:gap-y-3">
        {PREVIEW_COLUMNS.filter((column) => canViewColumn(column, me.viewableFields)).map((column) => (
          <div key={column.key} className="contents">
            <dt className="text-muted-foreground">{columnLabel(config, column)}</dt>
            <dd className="mb-2 font-medium sm:mb-0">
              {formatField(column, patient[column.key])}
              {column.key === "bmi" && patient.bmiCategory && (
                <span className="font-normal text-muted-foreground"> · {patient.bmiCategory}</span>
              )}
            </dd>
          </div>
        ))}
        {canView("medications") && (
          <>
            <dt className="text-muted-foreground">{fieldLabel(config, "medications")}</dt>
            <dd className="mb-2 sm:mb-0">
              {patient.medications?.length ? (
                <div className="flex flex-wrap gap-2">
                  {patient.medications.map((medication, index) => (
                    <Badge key={index} variant="outline">
                      {[medication.name, medication.dosage, medication.frequency].filter(Boolean).join(" · ")}
                    </Badge>
                  ))}
                </div>
              ) : <span className="font-medium">None</span>}
            </dd>
          </>
        )}
        <dt className="text-muted-foreground">Care team</dt>
        <dd className="mb-2 sm:mb-0"><CareTeamBadges careTeam={patient.careTeam ?? []} me={me} /></dd>
      </dl>
      {me.actions.includes("EDIT_PATIENT") && (
        <>
          <Separator />
          <div className="flex justify-end">
            <Button size="sm" onPress={() => onOpen(patient)}>Edit patient</Button>
          </div>
        </>
      )}
    </aside>
  )
}

function NoResults({ query, field, onClear, onSearchAll }) {
  const quoted = `“${query.trim()}”`
  return (
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <div className="flex size-12 items-center justify-center rounded-full bg-muted">
        <RiUserSearchLine className="size-6" aria-hidden />
      </div>
      <div className="flex max-w-96 flex-col gap-2">
        <h2 className="text-base font-semibold">No patients found</h2>
        <p className="text-sm text-muted-foreground">
          {field.key === "all"
            ? `No patient in your care matches ${quoted}. Check the spelling.`
            : `No patient in your care has a ${field.label.toLowerCase()} matching ${quoted}. Check the spelling, or search in all fields.`}
        </p>
      </div>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" onPress={onClear}>Clear search</Button>
        {field.key !== "all" && <Button size="sm" onPress={onSearchAll}>Search all fields</Button>}
      </div>
    </div>
  )
}

export function SearchPage({ me, query, onQueryChange, onOpen, onVisit, onBack }) {
  const [patients, setPatients] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const [scope, setScope] = useState(ALL_FIELDS.key)
  const [selectedId, setSelectedId] = useState(null)
  const config = useConfig()
  const fields = useMemo(() => searchFieldsFor(config, me.viewableFields), [config, me.viewableFields])
  const field = fields.find((f) => f.key === scope) ?? ALL_FIELDS
  const words = queryWords(query)

  useEffect(() => {
    getPatients().then(setPatients).catch((e) => setLoadError(e.message))
  }, [attempt])

  const results = useMemo(
    () => (patients ? searchPatients(patients, query, field === ALL_FIELDS ? fields : [field]) : []),
    [patients, query, field, fields]
  )
  const selected = results.find((result) => result.patient.id === selectedId) ?? results[0]

  function retry() {
    setLoadError(null)
    setAttempt((n) => n + 1)
  }

  function select(keys) {
    const id = [...keys][0]
    setSelectedId(id)
    onVisit(patients.find((patient) => patient.id === id))
  }

  return (
    <>
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink onPress={onBack}>Dashboard</BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbItem>
            <BreadcrumbPage>Search patients</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <section aria-labelledby="search-title" className="flex flex-col gap-8 rounded-xl border bg-card p-6 md:p-14">
        <div className="flex flex-col gap-2">
          <h1 id="search-title" className="font-heading text-3xl font-semibold leading-9">Search patients</h1>
          <p className="text-base text-muted-foreground">
            Find a patient in your care by {fields.map((f) => f.label.toLowerCase()).join(", ").replace(/, ([^,]*)$/, " or $1")}.
          </p>
        </div>

        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-6">
          <InputGroup className="lg:flex-1">
            <InputGroupInput aria-label="Search patients" placeholder="Search" autoFocus value={query}
              onChange={(event) => onQueryChange(event.target.value)} />
            <InputGroupAddon>
              <RiSearchLine />
            </InputGroupAddon>
          </InputGroup>
          <div className="flex min-w-0 flex-wrap items-center gap-3">
            <span id="search-scope" className="text-sm font-medium text-muted-foreground">Search in</span>
            <ToggleGroup aria-labelledby="search-scope" variant="outline" size="sm" spacing={0} className="max-w-full overflow-x-auto"
              selectedKeys={[field.key]} disallowEmptySelection
              onSelectionChange={(keys) => setScope([...keys][0])}>
              {[ALL_FIELDS, ...fields].map((f) => <ToggleGroupItem key={f.key} id={f.key}>{f.label}</ToggleGroupItem>)}
            </ToggleGroup>
          </div>
        </div>

        <Separator />

        {loadError ? (
          <div className="flex flex-col items-start gap-4">
            <FieldError>{loadError}</FieldError>
            <Button variant="outline" onPress={retry}>Try again</Button>
          </div>
        ) : patients && (patients.length === 0 ? (
          <p className="py-16 text-center text-muted-foreground">No patients are assigned to you yet</p>
        ) : results.length === 0 ? (
          <NoResults query={query} field={field} onClear={() => onQueryChange("")} onSearchAll={() => setScope(ALL_FIELDS.key)} />
        ) : (
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_30rem]">
            <div className="flex min-w-0 flex-col gap-3">
              <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
                <p role="status" className="font-medium">
                  {words.length
                    ? `${results.length} ${results.length === 1 ? "patient matches" : "patients match"} “${query.trim()}”`
                    : `${results.length} ${results.length === 1 ? "patient" : "patients"} in your care`}
                </p>
                <p className="text-muted-foreground">Only patients in your care are searched</p>
              </div>
              <GridList aria-label="Search results" items={results.map((result) => ({ id: result.patient.id, ...result }))}
                selectionMode="single" selectionBehavior="replace" disallowEmptySelection
                selectedKeys={selected ? [selected.patient.id] : []} onSelectionChange={select}
                onAction={(id) => onOpen(patients.find((patient) => patient.id === id))}
                className="flex flex-col gap-2">
                {(result) => (
                  <GridListItem id={result.id} textValue={fullName(result.patient)}
                    className="group/row cursor-pointer rounded-lg outline-none data-focus-visible:ring-3 data-focus-visible:ring-ring/50">
                    <ResultRow patient={result.patient} field={result.field} fields={fields} words={words} />
                  </GridListItem>
                )}
              </GridList>
            </div>
            {selected && <PatientPreview me={me} patient={selected.patient} onOpen={onOpen} />}
          </div>
        ))}
      </section>
    </>
  )
}
