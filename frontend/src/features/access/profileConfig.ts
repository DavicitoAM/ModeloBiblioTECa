import type { AppIconName } from '../../components/AppIcon'
import type { PersonTypeCode } from '../../api/types'

export interface InstitutionalProfileConfig {
  code: Exclude<PersonTypeCode, 'VISITOR'>
  label: string
  singular: string
  description: string
  identifierLabel: string
  identifierPlaceholder: string
  icon: AppIconName
}

export const institutionalProfiles: Record<
  'estudiante' | 'docente' | 'personal',
  InstitutionalProfileConfig
> = {
  estudiante: {
    code: 'STUDENT',
    label: 'Estudiante',
    singular: 'estudiante',
    description: 'Ingresa tu número de control para identificarte.',
    identifierLabel: 'Número de control',
    identifierPlaceholder: 'Ej. 22123456',
    icon: 'student',
  },
  docente: {
    code: 'TEACHER',
    label: 'Docente',
    singular: 'docente',
    description: 'Ingresa tu clave o número institucional.',
    identifierLabel: 'Clave institucional',
    identifierPlaceholder: 'Ej. DOC-001',
    icon: 'teacher',
  },
  personal: {
    code: 'STAFF',
    label: 'Personal',
    singular: 'personal',
    description: 'Ingresa la clave asignada por la institución.',
    identifierLabel: 'Clave institucional',
    identifierPlaceholder: 'Ej. ADM-001',
    icon: 'staff',
  },
}
