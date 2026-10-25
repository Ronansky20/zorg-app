import { useState } from "react"
import { medicationTypeLabel, validateMedication } from "@/lib/patients"
import { FormField } from "@/components/form-field"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogClose, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Field, FieldDescription, FieldGroup, FieldTitle } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { NativeSelect, NativeSelectOptGroup, NativeSelectOption } from "@/components/ui/native-select"

const EMPTY = { name: "", dosage: "", frequency: "" }

export function MedicationDialog({ medication, config, patientName, onSave, onRemove, onClose }) {
  const isNew = !medication
  const [values, setValues] = useState(() => ({ ...EMPTY, ...medication }))
  const [errors, setErrors] = useState({})
  const type = medicationTypeLabel(config, values.name)

  function change(key, value) {
    setValues((current) => ({ ...current, [key]: value }))
    setErrors((current) => {
      const next = { ...current }
      delete next[key]
      return next
    })
  }

  function handleSubmit(event) {
    event.preventDefault()
    const found = validateMedication(values)
    setErrors(found)
    if (Object.keys(found).length > 0) return
    onSave({ name: values.name, dosage: values.dosage.trim(), frequency: values.frequency.trim() })
  }

  return (
    <Dialog isOpen onOpenChange={(open) => !open && onClose()} className="sm:max-w-md">
      <form onSubmit={handleSubmit} noValidate className="grid gap-4">
        <DialogHeader>
          <DialogTitle>{isNew ? "Add medication" : "Edit medication"}</DialogTitle>
          <DialogDescription>
            {isNew ? `Add a medication to ${patientName}'s record.` : `Change the dosage or frequency for ${patientName}.`}
          </DialogDescription>
        </DialogHeader>

        <FieldGroup className="gap-4">
          <FormField id="medication-name" label="Medication" required error={errors.name}
            description="Pick from the catalog: pills, injections and ointments">
            <NativeSelect id="medication-name" className="w-full" value={values.name}
              aria-invalid={Boolean(errors.name)} onChange={(event) => change("name", event.target.value)}>
              <NativeSelectOption value="" disabled>Select a medication</NativeSelectOption>
              {config.medicationTypes.map((option) => (
                <NativeSelectOptGroup key={option.id} label={option.label}>
                  {config.medications.filter((item) => item.type === option.id).map((item) => (
                    <NativeSelectOption key={item.name} value={item.name}>{item.name}</NativeSelectOption>
                  ))}
                </NativeSelectOptGroup>
              ))}
            </NativeSelect>
          </FormField>

          <Field>
            <FieldTitle>Type</FieldTitle>
            {type ? <div><Badge variant="secondary">{type}</Badge></div> : <FieldDescription>Filled in when you pick a medication</FieldDescription>}
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <FormField id="medication-dosage" label="Dosage" required error={errors.dosage}>
              <Input id="medication-dosage" placeholder="e.g. 500 mg" value={values.dosage}
                aria-invalid={Boolean(errors.dosage)} onChange={(event) => change("dosage", event.target.value)} />
            </FormField>
            <FormField id="medication-frequency" label="Frequency" required error={errors.frequency}>
              <Input id="medication-frequency" placeholder="e.g. twice a day" value={values.frequency}
                aria-invalid={Boolean(errors.frequency)} onChange={(event) => change("frequency", event.target.value)} />
            </FormField>
          </div>
        </FieldGroup>

        <DialogFooter>
          {!isNew && <Button variant="destructive" className="sm:mr-auto" onPress={onRemove}>Remove</Button>}
          <DialogClose>Cancel</DialogClose>
          <Button type="submit">{isNew ? "Add medication" : "Save changes"}</Button>
        </DialogFooter>
      </form>
    </Dialog>
  )
}
