export interface Student {
  id: number
  controlNumber: string
  fullName: string
  career: string
  semester: number | null
  academicStatus: string
}

export interface VisitReason {
  id: number
  code: string
  name: string
  description: string
}

export interface VisitResponse {
  id: number

  student: {
    controlNumber: string
    fullName: string
    career: string
    semester: number | null
  }

  reason: {
    id: number
    code: string
    name: string
  }

  checkedInAt: string
  source: string
}