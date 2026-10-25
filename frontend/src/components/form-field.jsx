import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field"

export function FormField({ id, label, required, disabled, description, error, children }) {
  return (
    <Field data-invalid={error ? true : undefined} data-disabled={disabled || undefined}>
      <FieldLabel htmlFor={id}>
        {label}
        {required && <span aria-hidden="true" className="text-destructive">*</span>}
      </FieldLabel>
      {children}
      {error ? <FieldError>{error}</FieldError> : description && <FieldDescription>{description}</FieldDescription>}
    </Field>
  )
}
