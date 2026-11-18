import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Nach jedem Test die gezeichnete Seite wieder abräumen.
afterEach(() => cleanup())
