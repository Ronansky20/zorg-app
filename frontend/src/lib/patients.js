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

const BASE_COLUMNS = ["firstName", "lastName", "birthDate"]

const DASHBOARDS = {
    DOCTOR: {
        description: "These are the patients assigned to your care",
        columns: BASE_COLUMNS,
        stats: ["incomplete"],
    },
    PHARMACIST: {
        description: "Review the medication of the patients assigned to your care",
        columns: [...BASE_COLUMNS, "medications"],
        stats: ["onMedication"],
    },
    PHYSIOTHERAPIST: {
        description: "Track the weight and height of the patients assigned to your care",
        columns: [...BASE_COLUMNS, "weight", "height"],
        stats: ["incomplete"],
    },
    DENTIST: {
        description: "Check medication before treating the patients assigned to your care",
        columns: [...BASE_COLUMNS, "medications"],
        stats: ["onMedication", "incomplete"],
    },
}

export function dashboardFor(profession) {
    return DASHBOARDS[profession] ?? DASHBOARDS.DOCTOR
}

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

export function isOnMedication(patient) {
    return patient.medications?.length > 0
}

export function today() {
    return new Date().toLocaleDateString("en-CA")
}

export function validatePatient(values, canEdit) {
    const checks = {
        firstName: () => !values.firstName.trim() && "First name can't be empty",
        lastName: () => !values.lastName.trim() && "Last name can't be empty",
        birthDate: () => (!values.birthDate ? "Date of birth is required"
            : values.birthDate > today() && "A date of birth can't be in the future"),
        weight: () => !(Number(values.weight) > 0) && "Weight must be greater than 0",
        height: () => !(Number(values.height) > 0) && "Height must be greater than 0",
    }
    return Object.fromEntries(Object.entries(checks)
        .filter(([key]) => canEdit(key))
        .map(([key, check]) => [key, check()])
        .filter(([, message]) => message))
}

export function validateMedication({ name, dosage, frequency }) {
    const errors = {}
    if (!name) errors.name = "Pick a medication"
    if (!dosage.trim()) errors.dosage = "Dosage can't be empty"
    if (!frequency.trim()) errors.frequency = "Frequency can't be empty"
    return errors
}

export function medicationTypeLabel(config, name) {
    const type = config?.medications.find((medication) => medication.name === name)?.type
    return config?.medicationTypes.find((option) => option.id === type)?.label
}

export function professionLabel(profession) {
    return profession.charAt(0) + profession.slice(1).toLowerCase()
}
