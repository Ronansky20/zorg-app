import { useMemo, useState } from "react"
import { RiArrowDownSLine } from "@remixicon/react"
import sortIcon from "@/assets/arrow-down-up.svg"
import { useConfig } from "@/hooks/use-config"
import { PATIENT_COLUMNS, canViewColumn, columnLabel, formatField, matchesQuery } from "@/lib/patients"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

const PAGE_SIZE = 10
const PAGE_WINDOW = 5

function compareBy(column, direction) {
  const sign = direction === "descending" ? -1 : 1
  return (a, b) => sign * String(a[column] ?? "").localeCompare(String(b[column] ?? ""), undefined, { sensitivity: "base" })
}

function pageWindow(page, pageCount) {
  const start = Math.max(1, Math.min(page - Math.floor(PAGE_WINDOW / 2), pageCount - PAGE_WINDOW + 1))
  return Array.from({ length: Math.min(PAGE_WINDOW, pageCount) }, (_, i) => start + i)
}

export function PatientsTable({ patients, viewableFields, defaultColumns, query, onQueryChange, onOpen, filterRef }) {
  const config = useConfig()
  const viewable = PATIENT_COLUMNS.filter((column) => canViewColumn(column, viewableFields))
  const [visible, setVisible] = useState(() => new Set(defaultColumns.filter((key) => viewable.some((column) => column.key === key))))
  const [sort, setSort] = useState({ column: "lastName", direction: "ascending" })
  const [page, setPage] = useState(1)

  const columns = viewable.filter((column) => visible.has(column.key))
  const rows = useMemo(
    () => patients.filter((patient) => matchesQuery(patient, query)).sort(compareBy(sort.column, sort.direction)),
    [patients, query, sort]
  )
  const pageCount = Math.max(1, Math.ceil(rows.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const pageRows = rows.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  function handleQueryChange(value) {
    setPage(1)
    onQueryChange(value)
  }

  return (
    <div className="flex w-full flex-col">
      <div className="flex items-center justify-between gap-4 py-4">
        <Input ref={filterRef} aria-label="Filter patients" placeholder="Filter patients" value={query}
          onChange={(event) => handleQueryChange(event.target.value)} className="max-w-80 bg-secondary" />
        <DropdownMenuTrigger>
          <Button variant="outline" className="bg-secondary">
            Columns
            <RiArrowDownSLine data-icon="inline-end" />
          </Button>
          <DropdownMenu placement="bottom end" className="w-44" items={viewable} selectionMode="multiple"
            selectedKeys={visible} onSelectionChange={setVisible} disallowEmptySelection>
            {(column) => <DropdownMenuItem id={column.key}>{columnLabel(config, column)}</DropdownMenuItem>}
          </DropdownMenu>
        </DropdownMenuTrigger>
      </div>

      <div className="overflow-hidden rounded-md border bg-accent">
        <Table aria-label="Your patients" className="table-fixed" style={{ minWidth: `${columns.length * 9}rem` }} sortDescriptor={sort} onSortChange={setSort}
          onRowAction={(id) => onOpen(patients.find((patient) => patient.id === id))}>
          <TableHeader columns={columns}>
            {(column) => (
              <TableHead id={column.key} isRowHeader={column.key === columns[0].key} allowsSorting={column.sortable}
                className={column.sortable ? "px-0 outline-none" : undefined}>
                {column.sortable ? (
                  <span className="inline-flex h-8 items-center gap-1 rounded-lg px-2.5 hover:bg-muted">
                    {columnLabel(config, column)}
                    <img src={sortIcon} alt="" width="16" height="16" />
                  </span>
                ) : columnLabel(config, column)}
              </TableHead>
            )}
          </TableHeader>
          <TableBody items={pageRows} dependencies={[columns]}
            renderEmptyState={() => (
              <p className="p-6 text-muted-foreground">
                {patients.length === 0 ? "No patients are assigned to you yet" : "No patients match your search"}
              </p>
            )}>
            {(patient) => (
              <TableRow id={patient.id} columns={columns} className="cursor-pointer">
                {(column) => <TableCell className="truncate">{formatField(column, patient[column.key])}</TableCell>}
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <nav aria-label="Pagination" className="flex items-center justify-between gap-4 py-4">
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" isDisabled={currentPage === 1} onPress={() => setPage(currentPage - 1)}>
            Previous
          </Button>
          <Button size="sm" isDisabled={currentPage === pageCount} onPress={() => setPage(currentPage + 1)}>
            Next
          </Button>
        </div>
        <div className="flex items-center gap-0.5">
          {pageWindow(currentPage, pageCount).map((number) => (
            <Button key={number} size="icon" variant={number === currentPage ? "outline" : "ghost"}
              aria-current={number === currentPage ? "page" : undefined} aria-label={`Page ${number}`}
              onPress={() => setPage(number)}>
              {number}
            </Button>
          ))}
        </div>
      </nav>
    </div>
  )
}
