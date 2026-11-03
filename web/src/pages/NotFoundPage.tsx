import { Link } from 'react-router'

export function NotFoundPage() {
    return (
        <div className="flex flex-col items-center justify-center h-screen">
            <h1 className="text-4xl font-bold mb-4">404 - Seite nicht gefunden</h1>
            <p className="text-lg mb-8">
                )Die angeforderte Seite existiert nicht. Bitte überprüfen Sie die URL oder kehren Sie zur Startseite zurück.
            </p>
            <Link to="/" className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">
                Zur Startseite
            </Link>
        </div>
    )
}