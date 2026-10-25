import { useEffect, useState } from "react"
import { RiAddLine } from "@remixicon/react"
import { addPatient, getConfig, getUsers, updatePatient } from "@/api"
import { fullName, professionLabel, today, validatePatient } from "@/lib/patients"
import { FormField } from "@/components/form-field"
import { MedicationDialog } from "@/components/medication-dialog"
import { MedicationTable } from "@/components/medication-table"
import { Badge } from "@/components/ui/badge"
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage } from "@/components/ui/breadcrumb"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Field, FieldContent, FieldDescription, FieldError, FieldTitle } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Separator } from "@/components/ui/separator"

function toValues(patient) {
  return {
    firstName: patient?.firstName ?? "",
    lastName: patient?.lastName ?? "",
    birthDate: patient?.birthDate ?? "",
    weight: patient?.weight ? String(patient.weight) : "",
    height: patient?.height ? String(patient.height) : "",
    address: patient?.address ?? "",
  }
}

function FormSection({ title, description, children }) {
  return (
    <section className="grid gap-6 lg:grid-cols-[20rem_1fr] lg:gap-12">
      <div className="flex flex-col gap-1">
        <h2 className="text-base font-semibold">{title}</h2>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
      <div className="flex min-w-0 flex-col gap-6">{children}</div>
    </section>
  )
}

function UnitInput({ unit, ...props }) {
  return (
    <InputGroup>
      <InputGroupInput type="number" inputMode="decimal" min="0" {...props} />
      <InputGroupAddon align="inline-end">{unit}</InputGroupAddon>
    </InputGroup>
  )
}

export function PatientForm({ me, patient, onDone }) {
  const isNew = !patient
  const [values, setValues] = useState(() => toValues(patient))
  const [medications, setMedications] = useState(() => patient?.medications ?? [])
  const [careTeam, setCareTeam] = useState(() => new Set())
  const [errors, setErrors] = useState({})
  const [saveError, setSaveError] = useState(null)
  const [pending, setPending] = useState(false)
  const [config, setConfig] = useState(null)
  const [users, setUsers] = useState([])
  const [loadError, setLoadError] = useState(null)
  const [dialog, setDialog] = useState(null)

  const canView = (key) => me.viewableFields.includes(key)
  const canEdit = (key) => isNew || me.editableFields.includes(key)
  const name = isNew ? "the new patient" : fullName(patient)

  useEffect(() => {
    getConfig().then(setConfig).catch((e) => setLoadError(e.message))
    if (isNew) getUsers().then(setUsers).catch((e) => setLoadError(e.message))
  }, [isNew])

  function change(key, value) {
    setValues((current) => ({ ...current, [key]: value }))
    setErrors((current) => {
      const next = { ...current }
      delete next[key]
      return next
    })
  }

  function input(key) {
    return {
      id: key,
      value: values[key],
      disabled: !canEdit(key),
      "aria-invalid": Boolean(errors[key]),
      onChange: (event) => change(key, event.target.value),
    }
  }

  function toggleMember(id, selected) {
    setCareTeam((current) => {
      const next = new Set(current)
      if (selected) next.add(id)
      else next.delete(id)
      return next
    })
  }

  function saveMedication(medication) {
    setMedications((current) => dialog.index === undefined
      ? [...current, medication]
      : current.map((existing, index) => (index === dialog.index ? medication : existing)))
    setDialog(null)
  }

  function removeMedication(index) {
    setMedications((current) => current.filter((_, i) => i !== index))
    setDialog(null)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (pending) return
    const found = validatePatient(values, canEdit)
    setErrors(found)
    if (Object.keys(found).length > 0) return

    const fields = { ...values, weight: Number(values.weight), height: Number(values.height), medications }
    const editable = Object.fromEntries(Object.entries(fields).filter(([key]) => canEdit(key)))
    setPending(true)
    setSaveError(null)
    try {
      const saved = isNew
        ? await addPatient({ ...editable, careTeam: [...careTeam] })
        : await updatePatient(patient.id, editable)
      onDone(saved)
    } catch (e) {
      setSaveError(e.message)
      setPending(false)
    }
  }

  return (
    <>
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink onPress={() => onDone()}>Dashboard</BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbItem>
            <BreadcrumbPage>{isNew ? "Add patient" : "Edit patient"}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <form onSubmit={handleSubmit} noValidate aria-labelledby="patient-form-title"
        className="flex flex-col gap-8 rounded-xl border bg-card p-6 md:p-14">
        <div className="flex flex-col gap-2">
          <h1 id="patient-form-title" className="font-heading text-3xl font-semibold leading-9">
            {isNew ? "Add patient" : "Edit patient"}
          </h1>
          <p className="text-base text-muted-foreground">
            {isNew ? "Fill in the details of the new patient." : `Update the details of ${name}.`} Fields marked with * are required.
          </p>
        </div>

        <FormSection title="Personal details" description="Who the patient is and where they live.">
          <div className="grid gap-6 sm:grid-cols-2">
            <FormField id="firstName" label="First name" required disabled={!canEdit("firstName")} error={errors.firstName}>
              <Input placeholder="e.g. John" autoComplete="off" {...input("firstName")} />
            </FormField>
            <FormField id="lastName" label="Last name" required disabled={!canEdit("lastName")} error={errors.lastName}>
              <Input placeholder="e.g. Doe" autoComplete="off" {...input("lastName")} />
            </FormField>
          </div>
          {canView("birthDate") && (
            <div className="grid gap-6 sm:grid-cols-2">
              <FormField id="birthDate" label="Date of birth" required disabled={!canEdit("birthDate")}
                error={errors.birthDate} description="Can't be in the future">
                <Input type="date" max={today()} {...input("birthDate")} />
              </FormField>
            </div>
          )}
          {canView("address") && (
            <FormField id="address" label="Address" disabled={!canEdit("address")}>
              <Input placeholder="Street, house number, postcode and city" autoComplete="off" {...input("address")} />
            </FormField>
          )}
        </FormSection>

        {(canView("weight") || canView("height")) && (
          <>
            <Separator />
            <FormSection title="Measurements" description="Used to calculate the BMI.">
              <div className="grid gap-6 sm:grid-cols-2">
                {canView("weight") && (
                  <FormField id="weight" label="Weight" required disabled={!canEdit("weight")}
                    error={errors.weight} description="In kilograms, greater than 0">
                    <UnitInput unit="kg" step="0.1" placeholder="0.0" {...input("weight")} />
                  </FormField>
                )}
                {canView("height") && (
                  <FormField id="height" label="Height" required disabled={!canEdit("height")}
                    error={errors.height} description="In metres, greater than 0">
                    <UnitInput unit="m" step="0.01" placeholder="0.00" {...input("height")} />
                  </FormField>
                )}
              </div>
            </FormSection>
          </>
        )}

        <Separator />
        {isNew ? (
          <FormSection title="Care team" description="Who can view and edit this patient. You are added automatically.">
            <div className="grid gap-4 sm:grid-cols-2">
              {users.map((user) => {
                const isMe = user.id === me.user.id
                return (
                  <Field key={user.id} orientation="horizontal">
                    <Checkbox aria-labelledby={`member-${user.id}`} isSelected={isMe || careTeam.has(user.id)}
                      isDisabled={isMe} onChange={(selected) => toggleMember(user.id, selected)} />
                    <FieldContent>
                      <FieldTitle id={`member-${user.id}`}>{user.name}{isMe && " (you)"}</FieldTitle>
                      <FieldDescription>{professionLabel(user.profession)}</FieldDescription>
                    </FieldContent>
                  </Field>
                )
              })}
            </div>
          </FormSection>
        ) : (
          <FormSection title="Care team" description="The care team is set when the patient is added and can't be changed here.">
            <div className="flex flex-wrap gap-2">
              {patient.careTeam.map((member) => (
                <Badge key={member.id} variant="outline">
                  {member.name ?? member.id}{member.id === me.user.id && " (you)"}
                  {member.profession && ` · ${professionLabel(member.profession)}`}
                </Badge>
              ))}
            </div>
          </FormSection>
        )}

        {canView("medications") && (
          <>
            <Separator />
            <FormSection title="Medication" description="Medication the patient currently uses. Pick from the medication catalog.">
              <MedicationTable medications={medications} config={config}
                onEdit={canEdit("medications") ? (index) => setDialog({ index }) : undefined}
                onRemove={removeMedication} />
              {canEdit("medications") && (
                <Button variant="outline" className="self-start" isDisabled={!config} onPress={() => setDialog({})}>
                  <RiAddLine data-icon="inline-start" />
                  Add medication
                </Button>
              )}
            </FormSection>
          </>
        )}

        <Separator />
        {(loadError || saveError) && <FieldError>{saveError ?? loadError}</FieldError>}
        <div className="flex justify-end gap-2">
          <Button variant="outline" onPress={() => onDone()}>Cancel</Button>
          <Button type="submit" isDisabled={pending}>
            {pending ? "Saving…" : isNew ? "Add patient" : "Save changes"}
          </Button>
        </div>
      </form>

      {dialog && (
        <MedicationDialog medication={medications[dialog.index]} config={config} patientName={name}
          onSave={saveMedication} onRemove={() => removeMedication(dialog.index)} onClose={() => setDialog(null)} />
      )}
    </>
  )
}
