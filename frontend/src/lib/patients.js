export function formatDate(isoDate) {
    const [year, month, day] = isoDate.split("-")
    return `${day}-${month}-${year}`
}

export const PATIENT_COLUMNS = [
    { key: "firstName" },
    { key: "lastName", sortable: true },
    { key: "birthDate", format: formatDate },
    { key: "weight", format: (kg) => `${kg} kg` },
    { key: "height", format: (m) => `${m} m` },
    { key: "bmi", label: "BMI", requires: ["weight", "height"], format: (value) => value.toFixed(1) },
    { key: "address" },
    { key: "medications", format: (list) => list.map((m) => m.name).join(", ") || "None" },
]

export function canViewColumn(column, viewableFields) {
    return (column.requires ?? [column.key]).every((key) => viewableFields.includes(key))
}

export function calculateBmi(weight, height) {
    if (!(weight > 0 && height > 0)) return null
    return Math.round((weight / (height * height)) * 10) / 10
}

export function bmiCategory(bmi) {
    if (bmi < 18.5) return "Underweight"
    if (bmi < 25) return "Healthy weight"
    if (bmi < 30) return "Overweight"
    return "Obese"
}

function labelFrom(options, id) {
    return options?.find((option) => option.id === id)?.label ?? id
}

export function fieldLabel(config, key) {
    return labelFrom(config?.fields, key)
}

export function columnLabel(config, column) {
    return column.label ?? fieldLabel(config, column.key)
}

export function actionLabel(config, action) {
    return labelFrom(config?.actions, action)
}

export function professionLabel(config, profession) {
    return labelFrom(config?.professions, profession)
}

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
        description: "Track the weight, height and BMI of the patients assigned to your care",
        columns: [...BASE_COLUMNS, "weight", "height", "bmi"],
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

export function queryWords(query) {
    return query.trim().toLowerCase().split(/\s+/).filter(Boolean)
}

function containsWords(text, words) {
    const lower = text.toLowerCase()
    return words.every((word) => lower.includes(word))
}

export function matchesQuery(patient, query) {
    return containsWords(fullName(patient), queryWords(query))
}

const SEARCH_FIELDS = [
    { key: "name", label: "Name", text: fullName },
    { key: "birthDate", text: (patient) => (patient.birthDate ? formatDate(patient.birthDate) : "") },
    { key: "address", text: (patient) => patient.address ?? "" },
    { key: "medications", text: (patient) => (patient.medications ?? []).map((m) => m.name).join(", ") },
]

export function searchFieldsFor(config, viewableFields) {
    return SEARCH_FIELDS
        .filter((field) => field.key === "name" || viewableFields.includes(field.key))
        .map((field) => ({ ...field, label: columnLabel(config, field) }))
}

export function searchPatients(patients, query, fields) {
    const words = queryWords(query)
    return patients
        .map((patient) => ({
            patient,
            field: words.length ? fields.find((field) => containsWords(field.text(patient), words)) : null,
        }))
        .filter((result) => !words.length || result.field)
        .sort((a, b) => (a.patient.lastName ?? "").localeCompare(b.patient.lastName ?? "", undefined, { sensitivity: "base" }))
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
