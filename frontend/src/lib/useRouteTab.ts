import { useSearchParams } from 'react-router-dom'

/** A tab is part of navigation, including refresh, bookmarks and browser history. */
export function useRouteTab(defaultTab: string, allowed: string[]): [string, (tab: string) => void] {
  const [params, setParams] = useSearchParams()
  const requested = params.get('tab') || defaultTab
  const current = allowed.includes(requested) ? requested : defaultTab
  return [current, next => {
    const query = new URLSearchParams(params)
    if (next === defaultTab) query.delete('tab'); else query.set('tab', next)
    setParams(query)
  }]
}
