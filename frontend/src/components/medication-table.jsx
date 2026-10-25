import { medicationTypeLabel } from "@/lib/patients"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

export function MedicationTable({ medications, config, onEdit, onRemove }) {
  const editable = Boolean(onEdit)

  return (
    <div className="overflow-hidden rounded-md border bg-accent">
      <Table aria-label="Medication">
        <TableHeader>
          <TableHead isRowHeader>Medication</TableHead>
          <TableHead>Type</TableHead>
          <TableHead>Dosage</TableHead>
          <TableHead>Frequency</TableHead>
          {editable && <TableHead><span className="sr-only">Actions</span></TableHead>}
        </TableHeader>
        <TableBody renderEmptyState={() => <p className="p-6 text-muted-foreground">No medication added yet</p>}>
          {medications.map((medication, index) => (
            <TableRow key={index} id={index}>
              <TableCell>{medication.name}</TableCell>
              <TableCell>{medicationTypeLabel(config, medication.name) ?? "—"}</TableCell>
              <TableCell>{medication.dosage || "—"}</TableCell>
              <TableCell>{medication.frequency || "—"}</TableCell>
              {editable && (
                <TableCell className="text-right">
                  <Button variant="ghost" size="sm" aria-label={`Edit ${medication.name}`} onPress={() => onEdit(index)}>
                    Edit
                  </Button>
                  <Button variant="ghost" size="sm" aria-label={`Remove ${medication.name}`} onPress={() => onRemove(index)}>
                    Remove
                  </Button>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
