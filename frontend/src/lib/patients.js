function formatDate(isoDate) {
    const [year, month, day] = isoDate.split("-")
    return `${day}-${month}-${year}`
}

export const PATIENT_COLUMNS = [
    { key: "firstName", label: "First name" },
    { key: "lastName", label: "Last Name", sortable: true },
    { key: "birthDate", label: "Date of Birth", format: formatDate },
    { key: "weight", label: "Weight", format: (kg) => `${kg} kg` },
    { key: "height", label: "Height", format: (m) => `${m} m` },
    { key: "address", label: "Address" },
    { key: "medications", label: "Medication", format: (list) => list.map((m) => m.name).join(", ") || "None" },
]

export const DEFAULT_COLUMNS = ["firstName", "lastName", "birthDate"]

export function formatField(column, value) {
    if (value === undefined || value === null || value === "") return "—"
    return column.format ? column.format(value) : value
}

export function fullName(patient) {
    return [patient.firstName, patient.lastName].filter(Boolean).join(" ")
}

export function matchesQuery(patient, query) {
    const words = query.trim().toLowerCase().split(/\s+/).filter(Boolean)
    const name = fullName(patient).toLowerCase()
    return words.every((word) => name.includes(word))
}

export function isIncomplete(patient, viewableFields) {
    return viewableFields
        .filter((key) => key !== "medications")
        .some((key) => {
            const value = patient[key]
            return value === undefined || value === null || String(value).trim() === "" || value === 0
        })
}

export function professionLabel(profession) {
    return profession.charAt(0) + profession.slice(1).toLowerCase()
}
