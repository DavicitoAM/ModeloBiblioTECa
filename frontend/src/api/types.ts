export type PersonTypeCode =
  | 'STUDENT'
  | 'TEACHER'
  | 'STAFF'
  | 'VISITOR'

export interface PersonType {
  id: number
  code: PersonTypeCode | string
  name: string
  description: string | null
}

export interface Career {
  id: number
  code: string
  name: string
}

export interface StudentDetails {
  careerId: number | null
  careerCode: string | null
  careerName: string | null
  semester: number | null
  academicStatus: string
}

export interface Person {
  id: number
  type: {
    id: number
    code: PersonTypeCode | string
    name: string
  }
  institutionalIdentifier: string | null
  fullName: string
  email: string | null
  phone: string | null
  student: StudentDetails | null
  active: boolean
}

export interface VisitReason {
  id: number
  code: string
  name: string
  description: string | null
}

export interface AccessRecord {
  id: number
  person: {
    id: number
    typeCode: PersonTypeCode | string
    typeName: string
    institutionalIdentifier: string | null
    fullName: string
  }
  reason: {
    id: number
    code: string
    name: string
  } | null
  checkedInAt: string
  checkedOutAt: string | null
  destination: string | null
  notes: string | null
  source: string
  open: boolean
}

export interface CheckInPayload {
  personId: number
  visitReasonId?: number | null
  destination?: string | null
  notes?: string | null
}

export interface CreateVisitorPayload {
  firstName: string
  paternalSurname: string
  maternalSurname?: string | null
  email?: string | null
  phone?: string | null
}

export interface ApiProblem {
  detail?: string
  title?: string
  status?: number
  errors?: string[]
}
