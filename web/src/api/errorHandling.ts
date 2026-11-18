import { z } from 'zod'

// So sieht ein Fehler von Spring aus (ProblemDetail). Bei Validierungsfehlern (400) steht in
// errors je Feldname, was nicht stimmt.
const problemSchema = z.object({
  title: z.string().optional(),
  status: z.number().optional(),
  detail: z.string().optional(),
  errors: z.record(z.string(), z.string()).optional(),
})

export type ProblemDetail = z.infer<typeof problemSchema>

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.title ?? `HTTP ${status}`)
    this.status = status
    this.problem = problem
  }
}

// Alles, was kein ProblemDetail ist, ergibt null.
export function parseProblem(body: unknown): ProblemDetail | null {
  const result = problemSchema.safeParse(body)
  return result.success ? result.data : null
}
