import type { ReactNode } from 'react'

export type AppIconName =
  | 'student'
  | 'teacher'
  | 'staff'
  | 'visitor'
  | 'arrow'
  | 'login'
  | 'logout'
  | 'book'
  | 'space'
  | 'loan'
  | 'home'
  | 'records'
  | 'people'
  | 'settings'
  | 'search'
  | 'check'
  | 'clock'
  | 'shield'

interface Props {
  name: AppIconName
  size?: number
}

const paths: Record<AppIconName, ReactNode> = {
  student: <>
    <path d="M4 8.5 12 4l8 4.5-8 4.5-8-4.5Z" />
    <path d="M7 10.2V15c0 1.8 2.2 3 5 3s5-1.2 5-3v-4.8" />
    <path d="M20 9v5" />
  </>,
  teacher: <>
    <circle cx="9" cy="7" r="3" />
    <path d="M3.5 20v-2.5A4.5 4.5 0 0 1 8 13h2a4.5 4.5 0 0 1 4.5 4.5V20" />
    <path d="M15 5h6v8h-4" />
    <path d="m17.5 9 1.2 1.2L21 7.8" />
  </>,
  staff: <>
    <circle cx="12" cy="7" r="3" />
    <path d="M5 20v-2a7 7 0 0 1 14 0v2" />
    <path d="M9.5 13.5 12 16l2.5-2.5" />
  </>,
  visitor: <>
    <circle cx="9" cy="7" r="3" />
    <path d="M3 20v-2.5A4.5 4.5 0 0 1 7.5 13h3A4.5 4.5 0 0 1 15 17.5V20" />
    <path d="M17 8h4" />
    <path d="m19 6 2 2-2 2" />
  </>,
  arrow: <path d="M5 12h14m-5-5 5 5-5 5" />,
  login: <>
    <path d="M10 5H5v14h5" />
    <path d="M14 8l4 4-4 4" />
    <path d="M18 12H9" />
  </>,
  logout: <>
    <path d="M14 5h5v14h-5" />
    <path d="m10 8-4 4 4 4" />
    <path d="M6 12h9" />
  </>,
  book: <>
    <path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H20v16H7.5A3.5 3.5 0 0 0 4 21.5v-16Z" />
    <path d="M4 18.5A3.5 3.5 0 0 1 7.5 15H20" />
  </>,
  space: <>
    <path d="M4 19V7l8-4 8 4v12" />
    <path d="M8 19v-6h8v6" />
    <path d="M3 19h18" />
  </>,
  loan: <>
    <path d="M5 6h10v12H5z" />
    <path d="M9 4h10v12" />
    <path d="m9 10 2 2 4-4" />
  </>,
  home: <>
    <path d="m3 11 9-8 9 8" />
    <path d="M5 10v10h14V10" />
    <path d="M9 20v-6h6v6" />
  </>,
  records: <>
    <path d="M6 4h12v16H6z" />
    <path d="M9 8h6M9 12h6M9 16h4" />
  </>,
  people: <>
    <circle cx="9" cy="8" r="3" />
    <circle cx="17" cy="9" r="2.5" />
    <path d="M3 20v-2a6 6 0 0 1 12 0v2" />
    <path d="M15 15a5 5 0 0 1 6 5" />
  </>,
  settings: <>
    <circle cx="12" cy="12" r="3" />
    <path d="M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.4-2.4 1A7 7 0 0 0 15 6l-.3-2.6h-4L10.5 6A7 7 0 0 0 9 7.1l-2.4-1-2 3.4 2 1.5a7 7 0 0 0 0 2l-2 1.5 2 3.4 2.4-1A7 7 0 0 0 10.5 18l.3 2.6h4L15 18a7 7 0 0 0 1.5-1.1l2.4 1 2-3.4-2-1.5c.1-.3.1-.7.1-1Z" />
  </>,
  search: <>
    <circle cx="11" cy="11" r="6" />
    <path d="m16 16 4 4" />
  </>,
  check: <path d="m5 12 4 4L19 6" />,
  clock: <>
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3 2" />
  </>,
  shield: <>
    <path d="M12 3 5 6v5c0 4.5 2.7 8 7 10 4.3-2 7-5.5 7-10V6l-7-3Z" />
    <path d="m9 12 2 2 4-4" />
  </>,
}

export default function AppIcon({ name, size = 24 }: Props) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  )
}
