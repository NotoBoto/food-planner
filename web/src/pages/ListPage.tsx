import { ApiError } from "../api/errorHandling";
import { ErrorMessage } from "../components/ErrorMessage";

export function ListPage() {
  return (
    <>
      <h1 className="text-2xl font-semibold">Einkaufsliste</h1>
      <ErrorMessage
        error={
          new ApiError(404, {
            title: "Nicht gefunden",
            detail: "Kein Produkt mit der ID 42.",
          })
        }
      />
    </>
  )
}